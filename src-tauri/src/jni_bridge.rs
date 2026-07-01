//! JNI bridge between the Kotlin `LocationBridge` / `LocationService` and
//! the Rust core. Only compiled on Android targets.

#![cfg(target_os = "android")]

use jni::objects::JClass;
use jni::sys::{jdouble, jfloat, jlong};
use jni::{JNIEnv, JavaVM};
use std::sync::OnceLock;

use tauri::{AppHandle, Manager};

use crate::db::Db;
use crate::metrics::TrackPoint;
use crate::session::SessionStore;

/// Cached Tauri AppHandle so JNI callbacks (which arrive on the Kotlin service
/// thread, not a Tauri thread) can access state and emit events.
static APP_HANDLE: OnceLock<AppHandle> = OnceLock::new();
static JVM: OnceLock<JavaVM> = OnceLock::new();

pub fn init(app: AppHandle) {
    let _ = APP_HANDLE.set(app);
}

pub fn store_jvm(env: &JNIEnv) {
    if let Ok(vm) = env.get_java_vm() {
        let _ = JVM.set(vm);
    }
}

const BRIDGE_CLASS: &str = "com/didit/app/LocationBridge";

/// Call a no-arg static `void` method on `LocationBridge` (e.g. startService).
pub fn call_bridge_static_void(_app: &AppHandle, name: &str) -> Result<(), String> {
    let jvm = JVM.get().ok_or("JVM not yet cached")?;
    let mut env = jvm.attach_current_thread().map_err(|e| e.to_string())?;
    env.call_static_method(BRIDGE_CLASS, name, "()V", &[])
        .map_err(|e| e.to_string())?;
    Ok(())
}

pub fn installation_updated_at_ms() -> Result<Option<i64>, String> {
    let jvm = JVM.get().ok_or("JVM not yet cached")?;
    let mut env = jvm.attach_current_thread().map_err(|e| e.to_string())?;
    let ts = env
        .call_static_method(BRIDGE_CLASS, "installationUpdatedAtMs", "()J", &[])
        .map_err(|e| e.to_string())?
        .j()
        .map_err(|e| e.to_string())?;
    Ok((ts > 0).then_some(ts))
}

// ─────────────────────────────────────────────────────────────────────────────
// JNI entry points — these are called by Kotlin's LocationBridge.
// Their fully qualified names must match the Kotlin package + class + method.
// ─────────────────────────────────────────────────────────────────────────────

/// Called once during app init from Kotlin so we can cache the JavaVM.
#[no_mangle]
pub extern "system" fn Java_com_didit_app_LocationBridge_nativeInit(env: JNIEnv, _class: JClass) {
    store_jvm(&env);
    log::info!("JNI: nativeInit — JVM cached");
}

/// Called by the foreground service for each GPS sample.
#[no_mangle]
pub extern "system" fn Java_com_didit_app_LocationBridge_onLocation(
    _env: JNIEnv,
    _class: JClass,
    lat: jdouble,
    lng: jdouble,
    altitude: jdouble,
    accuracy: jfloat,
    timestamp_ms: jlong,
) {
    let Some(app) = APP_HANDLE.get() else { return };
    let store = app.state::<SessionStore>();
    let db = app.state::<Db>();

    let point = TrackPoint {
        timestamp_ms,
        lat,
        lng,
        altitude_m: if altitude.is_finite() {
            Some(altitude)
        } else {
            None
        },
        accuracy_m: Some(accuracy as f32),
        paused: false,
    };
    store.push_point(&db, point);
}

/// Called when the Kotlin service stops (user OS-killed it, etc).
#[no_mangle]
pub extern "system" fn Java_com_didit_app_LocationBridge_onServiceStopped(
    _env: JNIEnv,
    _class: JClass,
) {
    let Some(app) = APP_HANDLE.get() else { return };
    let db = app.state::<Db>();
    let store = app.state::<SessionStore>();
    store.flush_remaining(&db);
    log::info!("JNI: service stopped — flushed");
}

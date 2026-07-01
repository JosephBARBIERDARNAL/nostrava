//! JNI bridge between Kotlin's `LocationBridge`/`LocationService` and the
//! Rust core: streaming GPS callbacks in, foreground-service control out.
//! The request/response session + gym API lives in `jni_api.rs`.

use jni::objects::{JClass, JString};
use jni::sys::{jdouble, jfloat, jlong};
use jni::{JNIEnv, JavaVM};
use std::sync::OnceLock;

use crate::metrics::TrackPoint;
use crate::AppState;

/// Cached JavaVM so calls arriving on the Kotlin service thread (not the
/// thread that ran `nativeInit`) can still attach and call back into Kotlin.
static JVM: OnceLock<JavaVM> = OnceLock::new();

fn store_jvm(env: &JNIEnv) {
    if let Ok(vm) = env.get_java_vm() {
        let _ = JVM.set(vm);
    }
}

const BRIDGE_CLASS: &str = "com/didit/app/LocationBridge";

/// Call a no-arg static `void` method on `LocationBridge` (e.g. startService).
pub fn call_bridge_static_void(name: &str) -> Result<(), String> {
    let jvm = JVM.get().ok_or("JVM not yet cached")?;
    let mut env = jvm.attach_current_thread().map_err(|e| e.to_string())?;
    env.call_static_method(BRIDGE_CLASS, name, "()V", &[])
        .map_err(|e| e.to_string())?;
    Ok(())
}

// ─────────────────────────────────────────────────────────────────────────
// JNI entry points — called by Kotlin's LocationBridge. Fully qualified
// names must match the Kotlin package + class + method exactly.
// ─────────────────────────────────────────────────────────────────────────

/// Called once at process start from `LocationBridge.attach()`: caches the
/// JavaVM and opens the SQLite database at the path Kotlin supplies
/// (`context.filesDir/didit.db`).
#[no_mangle]
pub extern "system" fn Java_com_didit_app_LocationBridge_nativeInit(
    mut env: JNIEnv,
    _class: JClass,
    db_path: JString,
) {
    #[cfg(target_os = "android")]
    android_logger::init_once(android_logger::Config::default().with_max_level(log::LevelFilter::Info));

    store_jvm(&env);

    let path: String = env
        .get_string(&db_path)
        .expect("db_path must be valid UTF-8")
        .into();
    crate::init(std::path::Path::new(&path));
    log::info!("JNI: nativeInit — JVM cached, db opened at {path}");
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
    let state = AppState::get();
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
    state.store.push_point(&state.db, point);
}

/// Called when the Kotlin service stops (user OS-killed it, etc).
#[no_mangle]
pub extern "system" fn Java_com_didit_app_LocationBridge_onServiceStopped(
    _env: JNIEnv,
    _class: JClass,
) {
    let state = AppState::get();
    state.store.flush_remaining(&state.db);
    log::info!("JNI: service stopped — flushed");
}

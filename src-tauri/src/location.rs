//! Platform abstraction for starting/stopping the location source.
//! On Android, calls into the Kotlin `LocationBridge` via JNI to start/stop
//! the foreground service. On desktop, this is a no-op — the simulator
//! command `dev_push_point` is the location source there.

use tauri::AppHandle;

#[cfg(target_os = "android")]
pub fn start(app: &AppHandle) {
    if let Err(e) = crate::jni_bridge::call_bridge_static_void(app, "startService") {
        log::error!("LocationBridge.startService failed: {e}");
    }
}

#[cfg(target_os = "android")]
pub fn stop(app: &AppHandle) {
    if let Err(e) = crate::jni_bridge::call_bridge_static_void(app, "stopService") {
        log::error!("LocationBridge.stopService failed: {e}");
    }
}

#[cfg(not(target_os = "android"))]
pub fn start(_app: &AppHandle) {
    log::info!("location::start (desktop no-op — use dev_push_point)");
}

#[cfg(not(target_os = "android"))]
pub fn stop(_app: &AppHandle) {
    log::info!("location::stop (desktop no-op)");
}

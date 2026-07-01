//! Starts/stops the Android foreground location service via JNI, by calling
//! static methods on Kotlin's `LocationBridge`.

pub fn start() {
    if let Err(e) = crate::jni_bridge::call_bridge_static_void("startService") {
        log::error!("LocationBridge.startService failed: {e}");
    }
}

pub fn stop() {
    if let Err(e) = crate::jni_bridge::call_bridge_static_void("stopService") {
        log::error!("LocationBridge.stopService failed: {e}");
    }
}

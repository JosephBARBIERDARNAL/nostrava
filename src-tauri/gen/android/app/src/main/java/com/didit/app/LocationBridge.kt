package com.didit.app

import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * The Java↔Rust seam. The Rust side calls [startService] / [stopService] via
 * JNI to control the foreground service; the service calls [onLocation] /
 * [onServiceStopped] back into Rust whenever new data is available.
 *
 * `init { System.loadLibrary("didit_lib") }` is what pulls the Rust
 * cdylib into the JVM. The symbol names must match the `Java_…` exports
 * declared in `src-tauri/src/jni_bridge.rs`.
 */
object LocationBridge {
    private const val TAG = "LocationBridge"

    private var appContext: Context? = null

    init {
        System.loadLibrary("didit_lib")
    }

    /** Call once from MainActivity.onCreate so Rust can cache the JavaVM. */
    fun attach(ctx: Context) {
        appContext = ctx.applicationContext
        nativeInit()
    }

    @JvmStatic
    fun startService() {
        val ctx = appContext ?: return
        val intent = Intent(ctx, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    @JvmStatic
    fun stopService() {
        val ctx = appContext ?: return
        ctx.stopService(Intent(ctx, LocationService::class.java))
    }

    @JvmStatic
    fun installationUpdatedAtMs(): Long {
        val ctx = appContext ?: return 0L
        return ctx.packageManager.getPackageInfo(ctx.packageName, 0).lastUpdateTime
    }

    // ─── JNI bindings (implemented in Rust) ──────────────────────────────
    @JvmStatic external fun nativeInit()
    @JvmStatic external fun onLocation(
        lat: Double, lng: Double, altitude: Double,
        accuracy: Float, timestampMs: Long,
    )
    @JvmStatic external fun onServiceStopped()
}

package com.didit.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.didit.app.nav.DiditNavGraph
import com.didit.app.ui.theme.DiditTheme
import java.io.File
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    private val foregroundLocationPerms = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // osmdroid needs its own on-disk tile cache and a distinct user agent
        // (required by the OSM tile usage policy) configured before any MapView
        // is created.
        Configuration.getInstance().apply {
            osmdroidBasePath = File(applicationContext.cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
            userAgentValue = applicationContext.packageName
        }

        LocationBridge.attach(applicationContext)
        requestForegroundLocationIfMissing()

        setContent {
            DiditTheme {
                DiditNavGraph()
            }
        }
    }

    private fun requestForegroundLocationIfMissing() {
        val toRequest = mutableListOf<String>()
        for (perm in foregroundLocationPerms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                toRequest += perm
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            toRequest += Manifest.permission.POST_NOTIFICATIONS
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), REQ_FG_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // After foreground location is granted, prompt for "Allow all the time" so the run can
        // continue with the screen off. Android shows this as a separate full-screen dialog.
        if (requestCode == REQ_FG_LOCATION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fineIdx = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)
            val fineGranted = fineIdx >= 0 && grantResults[fineIdx] == PackageManager.PERMISSION_GRANTED
            val bgGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            if (fineGranted && !bgGranted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQ_BG_LOCATION,
                )
            }
        }
    }

    companion object {
        private const val REQ_FG_LOCATION = 1001
        private const val REQ_BG_LOCATION = 1002
    }
}

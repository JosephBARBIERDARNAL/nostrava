// Paste this block at the end of MainActivity.onCreate() (after super.onCreate(...)).
// Also add the imports below.

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private val LOCATION_PERMS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

// inside onCreate, after super.onCreate(savedInstanceState):

LocationBridge.attach(applicationContext)

val needFine = LOCATION_PERMS.any {
    ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
}
val needNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

val toRequest = mutableListOf<String>()
if (needFine) toRequest += LOCATION_PERMS
if (needNotif) toRequest += Manifest.permission.POST_NOTIFICATIONS
if (toRequest.isNotEmpty()) {
    ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 1001)
}

// After the user has granted FINE_LOCATION + COARSE_LOCATION, Android
// requires a *second* explicit request for ACCESS_BACKGROUND_LOCATION on
// Android 10+. Trigger this from the first session start, not at app launch,
// since Android shows it as a separate "Allow all the time" screen.
//
// Implement onRequestPermissionsResult() to call:
//   ActivityCompat.requestPermissions(
//     this,
//     arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
//     1002,
//   )
// once foreground location has been granted.

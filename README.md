# didit

A personal, **local-only** sport (run, bike and gym) tracker for Android. Zero account, cloud, or analytics.

<img src="image.png" alt="Description" width="200">

<br>

Native Kotlin/Jetpack Compose app (`android/`) backed by a Rust core (`rust/`) for
session state, SQLite persistence, and GPS/elevation math, called over JNI.
Android only — no desktop, no web.

## Putting it on your phone

1. On the phone: Settings → About phone → tap _Build number_ 7× to unlock Developer Options → enable _USB debugging_.
2. Plug it in, accept the RSA prompt.
3. `just install` — builds the APK (Rust cdylib + Kotlin/Compose) and `adb install`s it.
4. First launch: grant location ("Allow all the time" when prompted on the second screen) and notification permissions.

To iterate directly from Android Studio, open the `android/` folder as the project root.

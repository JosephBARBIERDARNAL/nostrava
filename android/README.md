# Android glue files

These are the Kotlin + manifest pieces that Tauri's Android generator doesn't
write for us. They wire a foreground service + FusedLocationProvider to the
Rust core via JNI.

## After running `tauri android init`

Tauri generates `src-tauri/gen/android/` with a Gradle project + a default
`MainActivity.kt` + `AndroidManifest.xml`. Merge the following:

1. **Copy Kotlin sources** into the generated source tree (package
   `com.didit.app`):

   ```
   cp android/kotlin/*.kt \
      src-tauri/gen/android/app/src/main/java/com/didit/app/
   ```

2. **Merge `AndroidManifest.xml`**: add the `<uses-permission>` lines and the
   `<service>` declaration from `android/AndroidManifest.snippet.xml` into the
   generated manifest. The permissions go above `<application>`; the service
   goes inside `<application>`.

3. **Add Gradle dependency** for FusedLocationProvider in
   `src-tauri/gen/android/app/build.gradle.kts`:

   ```kotlin
   dependencies {
       implementation("com.google.android.gms:play-services-location:21.3.0")
       // ...existing deps...
   }
   ```

4. **Edit `MainActivity.kt`** to add runtime-permission requests on `onCreate`.
   See `android/MainActivity.onCreate.snippet.kt` for the block to paste in.

After that, `tauri android dev` or `tauri android build --apk` will pick up
everything automatically.

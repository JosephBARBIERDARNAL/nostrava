# Nostrava

A personal, **local-only** running tracker for Android. Strava-style features
(timer, pause, distance, pace, splits, history) without any account, cloud, or
analytics. Sideloaded — never goes near the Play Store.

Built with **Tauri 2** (Rust core) + **React/shadcn** (UI) + a small **Kotlin
foreground service** for GPS.

## What it does

- Start / pause / stop a running session
- Computes distance (Haversine + GPS-jump filter), avg pace, per-km splits,
  elevation gain/loss
- Subtracts paused intervals from moving time
- History view with Week / Month / Year buckets
- All data stays on the phone in a SQLite DB at the app's private data dir

## Architecture

```
React + shadcn UI   (system WebView, UI only)
        │
        ▼  Tauri invoke / events
Rust core           (commands, session state, SQLite, metrics)
        │
        ▼  JNI
Kotlin glue         (Foreground Service + FusedLocationProvider)
```


## Repo layout

```
src/                   React frontend (UI only)
src-tauri/             Rust core + Tauri config
  src/metrics.rs       pure math (unit-tested)
  src/db.rs            rusqlite layer
  src/session.rs       active session state
  src/commands.rs      Tauri commands
  src/location.rs      cross-platform location start/stop
  src/jni_bridge.rs    Android-only JNI entry points
android/               Kotlin glue + manifest snippets (merged after init)
justfile               install / dev / build recipes
```

## Development

### Desktop dev loop (fast iteration on UI)

```bash
just dev          # runs `tauri dev`; window opens, hot reload on save
just test         # runs the Rust metrics unit tests
just build-web    # frontend typecheck + bundle
```

Desktop builds use a no-op `location::start/stop`. To feed fake GPS samples
during development, call `dev_push_point` from the browser console:

```js
await window.__TAURI__.core.invoke("dev_push_point", {
  lat: 48.8566,
  lng: 2.3522,
  altitudeM: 35,
  accuracyM: 5,
});
```

### Android — one-time setup

1. **Install Android Studio** (or just the command-line SDK + NDK).
   Make sure `ANDROID_HOME`, `NDK_HOME`, and `adb` are on your `PATH`:

   ```bash
   export ANDROID_HOME=$HOME/Library/Android/sdk
   export NDK_HOME=$ANDROID_HOME/ndk/<version>
   export PATH=$ANDROID_HOME/platform-tools:$PATH
   ```

2. **Add the Rust Android targets**:

   ```bash
   rustup target add aarch64-linux-android armv7-linux-androideabi \
                     i686-linux-android x86_64-linux-android
   ```

3. **Scaffold the Android project**:

   ```bash
   just android-init
   ```

   This runs `tauri android init` and prints the merge steps. Follow
   `android/README.md` to drop the Kotlin sources, manifest entries, and
   Gradle dependency into the generated `src-tauri/gen/android/` project.

4. **(Optional) Generate a release keystore** for stable upgrade signing:

   ```bash
   just keystore
   ```

   The default debug keystore at `~/.android/debug.keystore` works fine for
   personal use; only set up a release keystore if you want APKs to upgrade
   cleanly across reinstalls without prior uninstallation.

### Putting it on your phone

1. On the phone: Settings → About phone → tap _Build number_ 7× to unlock
   Developer Options → enable _USB debugging_.
2. Plug it in, accept the RSA prompt.
3. `just install` — builds the APK and `adb install`s it.
4. First launch: grant location ("Allow all the time" when prompted on the
   second screen) and notification permissions.

To iterate with live UI reload:

```bash
just android-dev
```

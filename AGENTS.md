# Repository Guidelines

## Project Structure & Module Organization

didit is a local-only run/bike/gym tracker: a native Kotlin/Jetpack Compose Android app backed by a Rust core, talking over JNI. There is no web frontend, no Tauri, no desktop target — Android only.

- `android/`: the real, hand-maintained Gradle project (open this folder directly in Android Studio). Kotlin sources live under `android/app/src/main/java/com/didit/app/`: `MainActivity.kt`, `LocationBridge.kt`/`LocationService.kt` (foreground GPS service + JNI seam), `bridge/NativeBridge.kt` (JNI wrapper for the session/gym API), `model/` (data classes mirroring the Rust JSON payloads), `nav/` (Navigation-Compose graph), `ui/theme/`, `ui/components/`, `ui/screens/` (one file per screen).
- `rust/`: the core logic crate (`didit-core`, builds a `cdylib`). `metrics.rs` (pure GPS/elevation/pace math, unit-tested), `session.rs` (in-memory active-session state machine), `db.rs` (SQLite via `rusqlite`), `location.rs` (starts/stops the Android foreground service via JNI), `jni_bridge.rs` (Kotlin→Rust location callbacks + service lifecycle), `jni_api.rs` (JNI wrappers for session/gym CRUD and live-state queries).

## Build, Test, and Development Commands

- `just build`: assemble a debug APK (compiles the Rust cdylib for arm64-v8a + armeabi-v7a via the `rust-android-gradle` plugin, then Kotlin/Compose).
- `just install`: build + install the debug APK on a connected/USB-debugging device.
- `just release`: assemble a release APK (configure signing in `android/app/build.gradle.kts` first — see `just keystore`).
- `just test`: run Rust unit tests (`cd rust && cargo test --lib`) — the metrics/session/db logic has no Android dependency and runs on the host.
- `just logcat`: tail this app's logcat output (there's no dev console anymore; `log::info!`/`log::error!` in Rust go through `android_logger`).

## Coding Style & Naming Conventions

Kotlin: standard Kotlin style, PascalCase for composables and classes (`ActivityTrackScreen.kt`), camelCase for functions/properties. Screens are one file per screen under `ui/screens/`; shared visual primitives (buttons, cards, tabs, stat tiles) live under `ui/components/` prefixed `Didit*`. `NativeBridge` is the only place that should call JNI `external fun`s directly — screens and components call it, never the raw JNI functions.

Rust: `rustfmt`, snake_case module/function names. Keep device-independent calculations in `metrics.rs` where they can stay unit-tested. `jni_bridge.rs` and `jni_api.rs` should stay thin marshalling layers — business logic belongs in `session.rs`/`db.rs`/`metrics.rs`.

## Testing Guidelines

Automated coverage is Rust-focused (`cargo test --lib` in `rust/`, metrics/session edge cases via `#[cfg(test)]`). There's no Kotlin test suite yet — verify UI/navigation/permission-flow changes manually on a real device (screen-off tracking, background/kill during a run, and the two-stage location permission prompts all need a real device, not an emulator).

## Commit & Pull Request Guidelines

Prefer descriptive, imperative commit messages (`Fix pause duration calculation`, not `push all`). Pull requests should describe the user-visible change, list commands run, and include screenshots or device notes for UI, navigation, or permission-flow changes.

## Security & Configuration Tips

Keep the app local-only: avoid adding analytics, accounts, or cloud sync without explicit discussion. Do not commit signing keys, generated keystores, or personal Android SDK paths (`android/local.properties` is gitignored). Use `ANDROID_HOME`/`NDK_HOME`/`ADB` env vars for local setup.

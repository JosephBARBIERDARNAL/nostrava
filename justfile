set shell := ["bash", "-cu"]

# Default: list recipes
default:
    @just --list

# Run the pure-Rust unit tests (metrics math etc).
test:
    cd rust && cargo test --lib

# Build a debug APK (compiles the Rust cdylib for arm64-v8a + armeabi-v7a, then Kotlin/Compose).
build:
    cd android && ./gradlew assembleDebug

# Build + install the debug APK on the currently-connected device via adb.
install:
    cd android && ./gradlew installDebug

# Build a release APK. Configure release signing in android/app/build.gradle.kts first.
release:
    cd android && ./gradlew assembleRelease

# Tail this app's logcat output — there's no Tauri dev console anymore, so this
# (plus `log::info!`/`log::error!` in Rust via android_logger) is how you see

# what's happening on-device.
logcat:
    ADB="${ADB:-${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}/platform-tools/adb}"; \
     if [ ! -x "$ADB" ]; then ADB="$(command -v adb || true)"; fi; \
     "$ADB" logcat | grep -i didit

# Generate a self-signed release keystore at ~/.didit/release.keystore.

# Run once, then point android/app/build.gradle.kts's `signingConfig` at it.
keystore:
    @mkdir -p ~/.didit
    @if [ -f ~/.didit/release.keystore ]; then \
        echo "Keystore already exists at ~/.didit/release.keystore"; \
    else \
        keytool -genkey -v \
          -keystore ~/.didit/release.keystore \
          -alias didit -keyalg RSA -keysize 4096 -validity 10950 \
          -storetype PKCS12; \
    fi

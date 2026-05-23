set shell := ["bash", "-cu"]

# Default: list recipes
default:
    @just --list

# Desktop dev — opens a 420×820 window with hot-reloading UI.
# Use this for fast iteration on screens; GPS is faked via the dev-push-point
# command (or you can just script it in the browser console).
dev:
    npm run tauri:dev

# Frontend-only typecheck + build (no Rust).
build-web:
    npm run build

# Run the pure-Rust unit tests (metrics math etc).
test:
    cd src-tauri && cargo test --lib

# One-time: scaffold the gen/android/ Gradle project.
# Requires ANDROID_HOME + NDK_HOME exported (and the SDK + NDK installed).
android-init:
    npm run tauri -- android init
    @echo
    @echo "→ Now merge files from ./android/ into src-tauri/gen/android/."
    @echo "  See android/README.md for the steps."

# Live-reload on a phone connected via USB (USB debugging on, RSA prompt accepted).
android-dev:
    npm run tauri -- android dev

# Build a release APK. Configure release signing before installing this one.
android-apk:
    npm run tauri -- android build --apk

# Build a debug-signed APK for sideloading on a modern Android phone.
android-debug-apk:
    npm run tauri -- android build --debug --apk --target aarch64

# Build + install on the currently-connected device via adb.
install:
    just android-debug-apk
    @APK="src-tauri/gen/android/app/build/outputs/apk/universal/debug/app-universal-debug.apk"; \
     ADB="${ADB:-${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}/platform-tools/adb}"; \
     if [ ! -x "$ADB" ]; then ADB="$(command -v adb || true)"; fi; \
     if [ ! -f "$APK" ]; then echo "APK not found: $APK"; exit 1; fi; \
     if [ -z "$ADB" ]; then echo "adb not found. Add Android platform-tools to PATH or set ADB=/path/to/adb."; exit 1; fi; \
     echo "Installing $APK …"; \
     "$ADB" install -r "$APK"

# Generate a self-signed release keystore at ~/.nostrava/release.keystore.
# Run once, then point tauri.conf.json's `android.signingConfig` at it.
keystore:
    @mkdir -p ~/.nostrava
    @if [ -f ~/.nostrava/release.keystore ]; then \
        echo "Keystore already exists at ~/.nostrava/release.keystore"; \
    else \
        keytool -genkey -v \
          -keystore ~/.nostrava/release.keystore \
          -alias nostrava -keyalg RSA -keysize 4096 -validity 10950 \
          -storetype PKCS12; \
    fi

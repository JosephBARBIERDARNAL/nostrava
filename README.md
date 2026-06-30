# Nostrava

A personal, **local-only** running tracker for Android. Strava-style features
(timer, pause, distance, pace, splits, history) without any account, cloud, or
analytics.

Built with **Tauri 2** (Rust core) + **React/shadcn** (UI) + a small **Kotlin foreground service** for GPS.


## Putting it on your phone

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

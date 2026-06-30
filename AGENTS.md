# Repository Guidelines

## Project Structure & Module Organization

Nostrava is a local-only running tracker built with Tauri 2, React, and Rust. The React/Vite frontend lives in `src/`: pages are in `src/pages`, reusable UI is in `src/components`, helpers are in `src/lib`, and global styles are in `src/styles/globals.css`. The Rust core lives in `src-tauri/src`, with metrics, database, session, command, location, and JNI modules split by responsibility. Android Kotlin and manifest snippets are kept in `android/` and merged into `src-tauri/gen/android/` after scaffolding.

## Build, Test, and Development Commands

- `just dev`: run `npm run tauri:dev` for desktop development with hot reload.
- `just build-web`: run TypeScript build checks and produce the Vite bundle.
- `just test`: run Rust library tests in `src-tauri`.
- `just android-init`: scaffold Android, then follow `android/README.md`.
- `just android-dev`: run the app on a connected Android device with live reload.
- `just install`: build a debug APK and install it with `adb`.

Use `npm run preview` only to inspect the built frontend bundle.

## Coding Style & Naming Conventions

Frontend code uses TypeScript, React function components, named exports, two-space indentation, and import aliases such as `@/lib/api`. Keep component filenames in PascalCase (`Track.tsx`, `SplitsTable.tsx`) and utility files short lowercase names (`format.ts`, `cn.ts`). Prefer Tailwind classes and existing shadcn-style primitives from `src/components/ui`.

Rust code follows `rustfmt`, snake_case module/function names, and small modules organized around app responsibilities. Keep device-independent calculations in `metrics.rs` where they can stay unit-tested.

## Testing Guidelines

The current automated test coverage is Rust-focused. Add unit tests beside Rust logic with `#[cfg(test)]`, especially for metrics, session behavior, and filtering edge cases. Run `just test` before changes that touch `src-tauri/src`. Run `just build-web` for frontend type safety. There is no configured frontend test runner yet, so document manual UI checks in PRs that change screens or navigation.

## Commit & Pull Request Guidelines

Recent commits use short, imperative summaries such as `update todo` or `push all`. Prefer more descriptive messages like `Fix pause duration calculation`. Pull requests should describe the user-visible change, list commands run, link relevant issues, and include screenshots or device notes for UI, Android, or permission-flow changes.

## Security & Configuration Tips

Keep the app local-only: avoid adding analytics, accounts, or cloud sync without explicit discussion. Do not commit signing keys, generated keystores, or personal Android SDK paths. Use environment variables such as `ANDROID_HOME`, `NDK_HOME`, or `ADB` for local setup.

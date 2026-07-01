//! didit Rust core — Tauri builder, state, commands, and the live-metrics
//! ticker thread.

mod commands;
mod db;
mod location;
mod metrics;
mod session;

#[cfg(target_os = "android")]
mod jni_bridge;

use std::time::Duration;
use tauri::Manager;

use crate::session::SessionStore;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .manage(SessionStore::new())
        .setup(|app| {
            // Open SQLite in the app's local data directory.
            let data_dir = app
                .path()
                .app_local_data_dir()
                .expect("app local data dir resolvable");
            std::fs::create_dir_all(&data_dir).ok();
            let db_path = data_dir.join("didit.db");
            let db = db::open(&db_path).expect("open sqlite");
            app.manage(db);

            // On Android, hand the AppHandle to the JNI module so it can route
            // location callbacks back into Rust state.
            #[cfg(target_os = "android")]
            crate::jni_bridge::init(app.handle().clone());

            // Spawn a ticker that emits live metrics ~1 Hz while a session is active.
            let handle = app.handle().clone();
            std::thread::spawn(move || loop {
                std::thread::sleep(Duration::from_millis(1000));
                crate::commands::emit_live_metrics(&handle);
            });

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            commands::current_state,
            commands::installation_updated_at_ms,
            commands::installation_installed_at_ms,
            commands::live_metrics,
            commands::live_points,
            commands::active_activity,
            commands::start_session,
            commands::pause_session,
            commands::resume_session,
            commands::stop_session,
            commands::list_recent,
            commands::list_range,
            commands::get_session,
            commands::delete_session,
            commands::dev_push_point,
            commands::create_gym_session,
            commands::list_recent_gym,
            commands::list_gym_range,
            commands::get_gym_session,
            commands::delete_gym_session,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

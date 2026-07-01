//! didit core — session state machine, SQLite persistence, and GPS/elevation
//! metrics. No UI code lives here; the UI is a native Kotlin/Compose Android
//! app in `android/` that talks to this crate exclusively over JNI (see
//! `jni_bridge.rs` for the streaming GPS/service-lifecycle callbacks and
//! `jni_api.rs` for the request/response session + gym API).

pub mod db;
pub mod jni_api;
pub mod jni_bridge;
pub mod location;
pub mod metrics;
pub mod session;

use std::path::Path;
use std::sync::OnceLock;

use crate::db::Db;
use crate::session::SessionStore;

/// Process-wide state, initialized once from `LocationBridge.nativeInit` at
/// app start and read by every JNI entry point thereafter.
pub struct AppState {
    pub db: Db,
    pub store: SessionStore,
}

static APP_STATE: OnceLock<AppState> = OnceLock::new();

impl AppState {
    pub fn get() -> &'static AppState {
        APP_STATE
            .get()
            .expect("AppState not initialized — nativeInit must run before any other JNI call")
    }
}

/// Opens the SQLite database at `db_path` and installs the process-wide
/// state. Called exactly once, from `jni_bridge::Java_..._nativeInit`.
pub fn init(db_path: &Path) {
    let db = db::open(db_path).expect("open sqlite");
    let _ = APP_STATE.set(AppState {
        db,
        store: SessionStore::new(),
    });
}

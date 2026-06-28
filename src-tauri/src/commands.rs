//! Tauri command handlers — the IPC surface the React app calls.

use chrono::{Datelike, Local, TimeZone};
use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter, Manager, State};

use crate::db::{Db, SessionDetail, SessionRow};
use crate::location;
use crate::metrics::{finalize, SessionTotals, TrackPoint};
use crate::session::{ActiveSession, LiveMetrics, SessionState, SessionStore};

fn now_ms() -> i64 {
    chrono::Utc::now().timestamp_millis()
}

#[derive(Debug, thiserror::Error)]
pub enum CmdError {
    #[error(transparent)]
    Db(#[from] crate::db::DbError),
    #[error("invalid state: {0}")]
    State(&'static str),
}

impl Serialize for CmdError {
    fn serialize<S: serde::Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
        s.serialize_str(&self.to_string())
    }
}

#[tauri::command]
pub fn current_state(store: State<SessionStore>) -> SessionState {
    store.snapshot_state()
}

#[tauri::command]
pub fn live_metrics(store: State<SessionStore>) -> Option<LiveMetrics> {
    store.live_metrics(now_ms())
}

#[tauri::command]
pub fn live_points(store: State<SessionStore>) -> Option<Vec<TrackPoint>> {
    store.live_points()
}

#[tauri::command]
pub fn start_session(
    app: AppHandle,
    db: State<Db>,
    store: State<SessionStore>,
) -> Result<i64, CmdError> {
    let mut guard = store.inner.lock();
    if guard.is_some() {
        return Err(CmdError::State("a session is already active"));
    }
    let started_at_ms = now_ms();
    let id = db.create_session(started_at_ms)?;
    *guard = Some(ActiveSession {
        id,
        started_at_ms,
        state: SessionState::Running,
        points: Vec::new(),
        pauses: Vec::new(),
        unflushed_from: 0,
    });
    drop(guard);
    location::start(&app);
    Ok(id)
}

#[tauri::command]
pub fn pause_session(db: State<Db>, store: State<SessionStore>) -> Result<(), CmdError> {
    let mut guard = store.inner.lock();
    let Some(s) = guard.as_mut() else {
        return Err(CmdError::State("no active session"));
    };
    if s.state != SessionState::Running {
        return Err(CmdError::State("session is not running"));
    }
    let ts = now_ms();
    s.state = SessionState::Paused;
    s.pauses.push(crate::metrics::PauseInterval {
        paused_at_ms: ts,
        resumed_at_ms: None,
    });
    let id = s.id;
    drop(guard);
    db.open_pause(id, ts)?;
    Ok(())
}

#[tauri::command]
pub fn resume_session(db: State<Db>, store: State<SessionStore>) -> Result<(), CmdError> {
    let mut guard = store.inner.lock();
    let Some(s) = guard.as_mut() else {
        return Err(CmdError::State("no active session"));
    };
    if s.state != SessionState::Paused {
        return Err(CmdError::State("session is not paused"));
    }
    let ts = now_ms();
    s.state = SessionState::Running;
    if let Some(last) = s.pauses.last_mut() {
        last.resumed_at_ms = Some(ts);
    }
    let id = s.id;
    drop(guard);
    db.close_pause(id, ts)?;
    Ok(())
}

#[tauri::command]
pub fn stop_session(
    app: AppHandle,
    db: State<Db>,
    store: State<SessionStore>,
) -> Result<SessionDetail, CmdError> {
    let ended_at_ms = now_ms();

    let (id, started_at_ms, points, mut pauses) = {
        let mut guard = store.inner.lock();
        let Some(s) = guard.take() else {
            return Err(CmdError::State("no active session"));
        };
        (s.id, s.started_at_ms, s.points, s.pauses)
    };

    // Close any open pause.
    if let Some(last) = pauses.last_mut() {
        if last.resumed_at_ms.is_none() {
            last.resumed_at_ms = Some(ended_at_ms);
            db.close_pause(id, ended_at_ms)?;
        }
    }

    // Flush any unsaved points (we kept the in-memory copy authoritative; persist all of them).
    // The store has already been taken so we write everything here.
    db.append_points(id, &points)?;

    let totals: SessionTotals = finalize(&points, &pauses, started_at_ms, ended_at_ms);
    db.finalize_session(id, ended_at_ms, &totals)?;

    location::stop(&app);

    let detail = db
        .get_session_detail(id)?
        .ok_or(CmdError::State("session vanished"))?;
    Ok(detail)
}

#[tauri::command]
pub fn list_recent(db: State<Db>, limit: u32) -> Result<Vec<SessionRow>, CmdError> {
    Ok(db.list_recent(limit)?)
}

#[derive(Debug, Clone, Copy, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum Range {
    Week,
    Month,
    Year,
}

#[derive(Debug, Serialize)]
pub struct HistoryBucket {
    pub from_ms: i64,
    pub to_ms: i64,
    pub label: String,
    pub sessions: Vec<SessionRow>,
    pub total_distance_m: f64,
    pub total_moving_duration_ms: i64,
    pub session_count: u32,
}

#[tauri::command]
pub fn list_range(db: State<Db>, range: Range, anchor_ms: i64) -> Result<HistoryBucket, CmdError> {
    let anchor = Local
        .timestamp_millis_opt(anchor_ms)
        .single()
        .unwrap_or_else(Local::now);
    let (from, to, label) = match range {
        Range::Week => {
            let weekday = anchor.weekday().num_days_from_monday() as i64;
            let monday = (anchor.date_naive() - chrono::Duration::days(weekday))
                .and_hms_opt(0, 0, 0)
                .unwrap();
            let from = Local.from_local_datetime(&monday).single().unwrap();
            let to = from + chrono::Duration::days(7);
            let label = format!("Week of {}", from.format("%b %d"));
            (from, to, label)
        }
        Range::Month => {
            let first = chrono::NaiveDate::from_ymd_opt(anchor.year(), anchor.month(), 1)
                .unwrap()
                .and_hms_opt(0, 0, 0)
                .unwrap();
            let from = Local.from_local_datetime(&first).single().unwrap();
            let next_month = if anchor.month() == 12 {
                chrono::NaiveDate::from_ymd_opt(anchor.year() + 1, 1, 1).unwrap()
            } else {
                chrono::NaiveDate::from_ymd_opt(anchor.year(), anchor.month() + 1, 1).unwrap()
            };
            let to = Local
                .from_local_datetime(&next_month.and_hms_opt(0, 0, 0).unwrap())
                .single()
                .unwrap();
            let label = from.format("%B %Y").to_string();
            (from, to, label)
        }
        Range::Year => {
            let from = Local
                .from_local_datetime(
                    &chrono::NaiveDate::from_ymd_opt(anchor.year(), 1, 1)
                        .unwrap()
                        .and_hms_opt(0, 0, 0)
                        .unwrap(),
                )
                .single()
                .unwrap();
            let to = Local
                .from_local_datetime(
                    &chrono::NaiveDate::from_ymd_opt(anchor.year() + 1, 1, 1)
                        .unwrap()
                        .and_hms_opt(0, 0, 0)
                        .unwrap(),
                )
                .single()
                .unwrap();
            let label = anchor.year().to_string();
            (from, to, label)
        }
    };

    let sessions = db.list_sessions_in_range(from.timestamp_millis(), to.timestamp_millis())?;
    let total_distance_m: f64 = sessions.iter().filter_map(|s| s.total_distance_m).sum();
    let total_moving_duration_ms: i64 = sessions.iter().filter_map(|s| s.moving_duration_ms).sum();
    Ok(HistoryBucket {
        from_ms: from.timestamp_millis(),
        to_ms: to.timestamp_millis(),
        label,
        session_count: sessions.len() as u32,
        sessions,
        total_distance_m,
        total_moving_duration_ms,
    })
}

#[tauri::command]
pub fn get_session(db: State<Db>, id: i64) -> Result<Option<SessionDetail>, CmdError> {
    Ok(db.get_session_detail(id)?)
}

#[tauri::command]
pub fn delete_session(db: State<Db>, id: i64) -> Result<(), CmdError> {
    db.delete_session(id)?;
    Ok(())
}

/// Desktop-only escape hatch: simulate a GPS sample for testing without a phone.
/// On Android we route through the foreground service instead.
#[tauri::command]
pub fn dev_push_point(
    db: State<Db>,
    store: State<SessionStore>,
    lat: f64,
    lng: f64,
    altitude_m: Option<f64>,
    accuracy_m: Option<f32>,
) -> Result<(), CmdError> {
    let point = TrackPoint {
        timestamp_ms: now_ms(),
        lat,
        lng,
        altitude_m,
        accuracy_m: accuracy_m.or(Some(5.0)),
        paused: false,
    };
    store.push_point(&db, point);
    Ok(())
}

/// Called by the live-metrics ticker thread to push updates to the frontend.
pub fn emit_live_metrics(app: &AppHandle) {
    let store = app.state::<SessionStore>();
    if let Some(metrics) = store.live_metrics(now_ms()) {
        let _ = app.emit("metrics", metrics);
    }
}

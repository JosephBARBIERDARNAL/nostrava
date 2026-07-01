//! JNI wrappers for the request/response API surface (session + gym CRUD,
//! live-state queries). GPS streaming and service-lifecycle callbacks live in
//! `jni_bridge.rs`; this file is purely a thin JSON-over-JNI translation
//! layer — all business logic already lives in `session.rs`/`db.rs`.
//!
//! Every JNI function returns a JSON string. Calls that can fail return a
//! `{"ok":true,"value":...}` / `{"ok":false,"error":"..."}` envelope; calls
//! that can't fail (they just report current state) return a plain JSON
//! value with no envelope. Kotlin's `NativeBridge` decodes both uniformly.

use chrono::{Datelike, Local, TimeZone};
use jni::objects::{JClass, JString};
use jni::sys::{jlong, jstring};
use jni::JNIEnv;
use serde::Serialize;

use crate::db::{GymSessionRow, SessionDetail, SessionRow};
use crate::location;
use crate::metrics::finalize;
use crate::session::{ActiveSession, ActivityKind, SessionState};
use crate::AppState;

fn now_ms() -> i64 {
    chrono::Utc::now().timestamp_millis()
}

#[derive(Debug, thiserror::Error)]
enum CmdError {
    #[error(transparent)]
    Db(#[from] crate::db::DbError),
    #[error("invalid state: {0}")]
    State(&'static str),
}

fn parse_activity(s: &str) -> Result<ActivityKind, CmdError> {
    match s {
        "running" => Ok(ActivityKind::Running),
        "biking" => Ok(ActivityKind::Biking),
        _ => Err(CmdError::State("invalid activity")),
    }
}

#[derive(Clone, Copy)]
enum Range {
    Week,
    Month,
    Year,
}

fn parse_range(s: &str) -> Result<Range, CmdError> {
    match s {
        "week" => Ok(Range::Week),
        "month" => Ok(Range::Month),
        "year" => Ok(Range::Year),
        _ => Err(CmdError::State("invalid range")),
    }
}

#[derive(Serialize)]
struct HistoryBucket {
    from_ms: i64,
    to_ms: i64,
    label: String,
    sessions: Vec<SessionRow>,
    total_distance_m: f64,
    total_moving_duration_ms: i64,
    session_count: u32,
}

#[derive(Serialize)]
struct GymHistoryBucket {
    from_ms: i64,
    to_ms: i64,
    label: String,
    sessions: Vec<GymSessionRow>,
    session_count: u32,
}

/// Compute the [from, to) bounds and display label for a week/month/year
/// bucket anchored at `anchor_ms`. Shared by running/biking and gym history.
fn range_bounds(range: Range, anchor_ms: i64) -> (chrono::DateTime<Local>, chrono::DateTime<Local>, String) {
    let anchor = Local
        .timestamp_millis_opt(anchor_ms)
        .single()
        .unwrap_or_else(Local::now);
    match range {
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
    }
}

// ── JSON marshalling helpers ────────────────────────────────────────────

#[derive(Serialize)]
struct Envelope<T: Serialize> {
    ok: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    value: Option<T>,
    #[serde(skip_serializing_if = "Option::is_none")]
    error: Option<String>,
}

fn to_jni_json_result<T: Serialize>(env: &mut JNIEnv, result: Result<T, CmdError>) -> jstring {
    let envelope = match result {
        Ok(value) => Envelope {
            ok: true,
            value: Some(value),
            error: None,
        },
        Err(e) => Envelope {
            ok: false,
            value: None,
            error: Some(e.to_string()),
        },
    };
    to_jni_json(env, &envelope)
}

fn to_jni_json<T: Serialize>(env: &mut JNIEnv, value: &T) -> jstring {
    let json = serde_json::to_string(value)
        .unwrap_or_else(|_| "{\"ok\":false,\"error\":\"serialize failure\"}".to_string());
    env.new_string(json).expect("new_string").into_raw()
}

fn get_str(env: &mut JNIEnv, s: &JString) -> String {
    env.get_string(s).expect("valid UTF-8 string arg").into()
}

// ── Session lifecycle ────────────────────────────────────────────────────

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_startSession(
    mut env: JNIEnv,
    _class: JClass,
    activity: JString,
) -> jstring {
    let activity_str = get_str(&mut env, &activity);
    let result: Result<i64, CmdError> = (|| {
        let activity = parse_activity(&activity_str)?;
        let state = AppState::get();
        let mut guard = state.store.inner.lock();
        if guard.is_some() {
            return Err(CmdError::State("a session is already active"));
        }
        let started_at_ms = now_ms();
        let id = state.db.create_session(started_at_ms, activity.as_str())?;
        *guard = Some(ActiveSession {
            id,
            started_at_ms,
            activity,
            state: SessionState::Running,
            points: Vec::new(),
            pauses: Vec::new(),
            unflushed_from: 0,
        });
        drop(guard);
        location::start();
        Ok(id)
    })();
    to_jni_json_result(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_pauseSession(mut env: JNIEnv, _class: JClass) -> jstring {
    let state = AppState::get();
    let result: Result<(), CmdError> = (|| {
        let mut guard = state.store.inner.lock();
        let s = guard.as_mut().ok_or(CmdError::State("no active session"))?;
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
        state.db.open_pause(id, ts)?;
        Ok(())
    })();
    to_jni_json_result(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_resumeSession(mut env: JNIEnv, _class: JClass) -> jstring {
    let state = AppState::get();
    let result: Result<(), CmdError> = (|| {
        let mut guard = state.store.inner.lock();
        let s = guard.as_mut().ok_or(CmdError::State("no active session"))?;
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
        state.db.close_pause(id, ts)?;
        Ok(())
    })();
    to_jni_json_result(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_stopSession(mut env: JNIEnv, _class: JClass) -> jstring {
    let state = AppState::get();
    let result: Result<SessionDetail, CmdError> = (|| {
        let ended_at_ms = now_ms();
        let (id, started_at_ms, points, mut pauses) = {
            let mut guard = state.store.inner.lock();
            let s = guard.take().ok_or(CmdError::State("no active session"))?;
            (s.id, s.started_at_ms, s.points, s.pauses)
        };
        if let Some(last) = pauses.last_mut() {
            if last.resumed_at_ms.is_none() {
                last.resumed_at_ms = Some(ended_at_ms);
                state.db.close_pause(id, ended_at_ms)?;
            }
        }
        state.db.append_points(id, &points)?;
        let totals = finalize(&points, &pauses, started_at_ms, ended_at_ms);
        state.db.finalize_session(id, ended_at_ms, &totals)?;
        location::stop();
        state
            .db
            .get_session_detail(id)?
            .ok_or(CmdError::State("session vanished"))
    })();
    to_jni_json_result(&mut env, result)
}

// ── Session queries ───────────────────────────────────────────────────────

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_listRecent(
    mut env: JNIEnv,
    _class: JClass,
    limit: jlong,
    activity: JString,
) -> jstring {
    let activity_str = get_str(&mut env, &activity);
    let result: Result<Vec<SessionRow>, CmdError> = (|| {
        let activity = parse_activity(&activity_str)?;
        Ok(AppState::get().db.list_recent(limit as u32, activity.as_str())?)
    })();
    to_jni_json_result(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_listRange(
    mut env: JNIEnv,
    _class: JClass,
    range: JString,
    anchor_ms: jlong,
    activity: JString,
) -> jstring {
    let range_str = get_str(&mut env, &range);
    let activity_str = get_str(&mut env, &activity);
    let result: Result<HistoryBucket, CmdError> = (|| {
        let range = parse_range(&range_str)?;
        let activity = parse_activity(&activity_str)?;
        let (from, to, label) = range_bounds(range, anchor_ms);
        let sessions =
            AppState::get()
                .db
                .list_sessions_in_range(from.timestamp_millis(), to.timestamp_millis(), activity.as_str())?;
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
    })();
    to_jni_json_result(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_getSession(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
) -> jstring {
    let result: Result<Option<SessionDetail>, CmdError> = AppState::get().db.get_session_detail(id).map_err(CmdError::from);
    to_jni_json_result(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_deleteSession(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
) -> jstring {
    let result: Result<(), CmdError> = AppState::get().db.delete_session(id).map_err(CmdError::from);
    to_jni_json_result(&mut env, result)
}

// ── Gym CRUD ──────────────────────────────────────────────────────────────

/// `exercises_json` is a JSON-encoded `string[]` — Kotlin serializes the
/// selected exercise names before calling, so this JNI call stays
/// single-string-argument simple instead of marshalling a Java string array.
#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_createGymSession(
    mut env: JNIEnv,
    _class: JClass,
    exercises_json: JString,
) -> jstring {
    let raw = get_str(&mut env, &exercises_json);
    let result: Result<i64, CmdError> = (|| {
        let exercises: Vec<String> =
            serde_json::from_str(&raw).map_err(|_| CmdError::State("invalid exercises payload"))?;
        Ok(AppState::get().db.create_gym_session(now_ms(), &exercises)?)
    })();
    to_jni_json_result(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_listRecentGym(
    mut env: JNIEnv,
    _class: JClass,
    limit: jlong,
) -> jstring {
    let result: Result<Vec<GymSessionRow>, CmdError> = AppState::get().db.list_recent_gym(limit as u32).map_err(CmdError::from);
    to_jni_json_result(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_listGymRange(
    mut env: JNIEnv,
    _class: JClass,
    range: JString,
    anchor_ms: jlong,
) -> jstring {
    let range_str = get_str(&mut env, &range);
    let result: Result<GymHistoryBucket, CmdError> = (|| {
        let range = parse_range(&range_str)?;
        let (from, to, label) = range_bounds(range, anchor_ms);
        let sessions = AppState::get()
            .db
            .list_gym_sessions_in_range(from.timestamp_millis(), to.timestamp_millis())?;
        Ok(GymHistoryBucket {
            from_ms: from.timestamp_millis(),
            to_ms: to.timestamp_millis(),
            label,
            session_count: sessions.len() as u32,
            sessions,
        })
    })();
    to_jni_json_result(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_getGymSession(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
) -> jstring {
    let result: Result<Option<GymSessionRow>, CmdError> = AppState::get().db.get_gym_session(id).map_err(CmdError::from);
    to_jni_json_result(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_deleteGymSession(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
) -> jstring {
    let result: Result<(), CmdError> = AppState::get().db.delete_gym_session(id).map_err(CmdError::from);
    to_jni_json_result(&mut env, result)
}

// ── Live state (no envelope — these can't fail, they just report state) ───

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_currentState(mut env: JNIEnv, _class: JClass) -> jstring {
    let state = AppState::get().store.snapshot_state();
    to_jni_json(&mut env, &state)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_activeActivity(mut env: JNIEnv, _class: JClass) -> jstring {
    let activity = AppState::get().store.active_activity();
    to_jni_json(&mut env, &activity)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_liveMetrics(mut env: JNIEnv, _class: JClass) -> jstring {
    let metrics = AppState::get().store.live_metrics(now_ms());
    to_jni_json(&mut env, &metrics)
}

#[no_mangle]
pub extern "system" fn Java_com_didit_app_NativeBridge_livePoints(mut env: JNIEnv, _class: JClass) -> jstring {
    let points = AppState::get().store.live_points();
    to_jni_json(&mut env, &points)
}

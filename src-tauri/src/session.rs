//! In-memory active-session state. The Kotlin/JNI bridge (on Android) or the
//! desktop simulator command both feed points into here via `push_point`.

use std::sync::Arc;

use parking_lot::Mutex;
use serde::Serialize;

use crate::db::Db;
use crate::metrics::{cumulative_distance_m, pauses_total_ms, PauseInterval, TrackPoint};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum SessionState {
    Idle,
    Running,
    Paused,
}

#[derive(Debug)]
pub struct ActiveSession {
    pub id: i64,
    pub started_at_ms: i64,
    pub state: SessionState,
    pub points: Vec<TrackPoint>,
    pub pauses: Vec<PauseInterval>,
    /// Points appended since the last DB flush.
    pub unflushed_from: usize,
}

#[derive(Debug, Clone, Serialize)]
pub struct LiveMetrics {
    pub session_id: i64,
    pub state: SessionState,
    pub started_at_ms: i64,
    pub now_ms: i64,
    pub total_distance_m: f64,
    /// Elapsed wall-clock minus accumulated pause time.
    pub moving_duration_ms: i64,
    /// Average pace so far (s/km), or 0 if no real distance yet.
    pub avg_pace_s_per_km: f64,
}

#[derive(Clone)]
pub struct SessionStore {
    pub inner: Arc<Mutex<Option<ActiveSession>>>,
}

impl SessionStore {
    pub fn new() -> Self {
        Self {
            inner: Arc::new(Mutex::new(None)),
        }
    }

    pub fn snapshot_state(&self) -> SessionState {
        self.inner
            .lock()
            .as_ref()
            .map_or(SessionState::Idle, |s| s.state)
    }

    /// Compute live metrics for the running session, or None if idle.
    pub fn live_metrics(&self, now_ms: i64) -> Option<LiveMetrics> {
        let guard = self.inner.lock();
        let s = guard.as_ref()?;
        let cum = cumulative_distance_m(&s.points);
        let total_distance_m = cum.last().copied().unwrap_or(0.0);
        let paused_ms = pauses_total_ms(&s.pauses, now_ms);
        let moving_duration_ms = (now_ms - s.started_at_ms - paused_ms).max(0);
        let avg_pace_s_per_km = if total_distance_m > 1.0 {
            (moving_duration_ms as f64 / 1000.0) / (total_distance_m / 1000.0)
        } else {
            0.0
        };
        Some(LiveMetrics {
            session_id: s.id,
            state: s.state,
            started_at_ms: s.started_at_ms,
            now_ms,
            total_distance_m,
            moving_duration_ms,
            avg_pace_s_per_km,
        })
    }

    /// Return the active track points for the frontend live map.
    pub fn live_points(&self) -> Option<Vec<TrackPoint>> {
        let guard = self.inner.lock();
        guard.as_ref().map(|s| s.points.clone())
    }

    /// Append a location update from whatever source (JNI or simulator).
    pub fn push_point(&self, db: &Db, mut point: TrackPoint) {
        let mut guard = self.inner.lock();
        let Some(s) = guard.as_mut() else { return };
        point.paused = s.state == SessionState::Paused;
        s.points.push(point);

        // Flush every ~10 points or every ~10 seconds so we don't lose data if killed.
        let should_flush = s.points.len() - s.unflushed_from >= 10;
        if should_flush {
            let new_pts: Vec<TrackPoint> = s.points[s.unflushed_from..].to_vec();
            s.unflushed_from = s.points.len();
            // Best-effort persist; failure shouldn't lose in-memory state.
            let _ = db.append_points(s.id, &new_pts);
        }
    }

    /// Persist any active-session points that have not yet been flushed.
    pub fn flush_remaining(&self, db: &Db) {
        let mut guard = self.inner.lock();
        let Some(s) = guard.as_mut() else { return };
        if s.unflushed_from >= s.points.len() {
            return;
        }

        let new_pts: Vec<TrackPoint> = s.points[s.unflushed_from..].to_vec();
        if db.append_points(s.id, &new_pts).is_ok() {
            s.unflushed_from = s.points.len();
        }
    }
}

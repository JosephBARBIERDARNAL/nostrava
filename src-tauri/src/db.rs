//! SQLite persistence (rusqlite, bundled SQLite).

use std::path::Path;
use std::sync::Mutex;

use rusqlite::{params, Connection, OptionalExtension};

use crate::metrics::{PauseInterval, SessionTotals, Split, TrackPoint};

pub struct Db(pub Mutex<Connection>);

#[derive(Debug, thiserror::Error)]
pub enum DbError {
    #[error("sqlite: {0}")]
    Sqlite(#[from] rusqlite::Error),
}

pub fn open(path: &Path) -> Result<Db, DbError> {
    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent).ok();
    }
    let conn = Connection::open(path)?;
    conn.execute_batch(SCHEMA_SQL)?;
    Ok(Db(Mutex::new(conn)))
}

const SCHEMA_SQL: &str = r#"
PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS sessions (
    id                    INTEGER PRIMARY KEY,
    started_at_ms         INTEGER NOT NULL,
    ended_at_ms           INTEGER,
    total_distance_m      REAL,
    moving_duration_ms    INTEGER,
    total_duration_ms     INTEGER,
    avg_pace_s_per_km     REAL,
    elevation_gain_m      REAL,
    elevation_loss_m      REAL
);

CREATE TABLE IF NOT EXISTS track_points (
    session_id     INTEGER NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    timestamp_ms   INTEGER NOT NULL,
    lat            REAL NOT NULL,
    lng            REAL NOT NULL,
    altitude_m     REAL,
    accuracy_m     REAL,
    paused         INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_points_session ON track_points(session_id, timestamp_ms);

CREATE TABLE IF NOT EXISTS splits (
    session_id          INTEGER NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    km_index            INTEGER NOT NULL,
    duration_ms         INTEGER NOT NULL,
    elevation_gain_m    REAL
);

CREATE TABLE IF NOT EXISTS pause_intervals (
    session_id      INTEGER NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    paused_at_ms    INTEGER NOT NULL,
    resumed_at_ms   INTEGER
);
"#;

#[derive(Debug, Clone, serde::Serialize)]
pub struct SessionRow {
    pub id: i64,
    pub started_at_ms: i64,
    pub ended_at_ms: Option<i64>,
    pub total_distance_m: Option<f64>,
    pub moving_duration_ms: Option<i64>,
    pub total_duration_ms: Option<i64>,
    pub avg_pace_s_per_km: Option<f64>,
    pub elevation_gain_m: Option<f64>,
    pub elevation_loss_m: Option<f64>,
}

#[derive(Debug, Clone, serde::Serialize)]
pub struct SessionDetail {
    pub session: SessionRow,
    pub points: Vec<TrackPoint>,
    pub splits: Vec<Split>,
    pub pauses: Vec<PauseInterval>,
}

impl Db {
    pub fn create_session(&self, started_at_ms: i64) -> Result<i64, DbError> {
        let conn = self.0.lock().unwrap();
        conn.execute(
            "INSERT INTO sessions (started_at_ms) VALUES (?1)",
            params![started_at_ms],
        )?;
        Ok(conn.last_insert_rowid())
    }

    pub fn append_points(&self, session_id: i64, points: &[TrackPoint]) -> Result<(), DbError> {
        let mut conn = self.0.lock().unwrap();
        let tx = conn.transaction()?;
        {
            let mut stmt = tx.prepare(
                "INSERT INTO track_points (session_id, timestamp_ms, lat, lng, altitude_m, accuracy_m, paused)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
            )?;
            for p in points {
                stmt.execute(params![
                    session_id,
                    p.timestamp_ms,
                    p.lat,
                    p.lng,
                    p.altitude_m,
                    p.accuracy_m,
                    p.paused as i32
                ])?;
            }
        }
        tx.commit()?;
        Ok(())
    }

    pub fn open_pause(&self, session_id: i64, paused_at_ms: i64) -> Result<(), DbError> {
        let conn = self.0.lock().unwrap();
        conn.execute(
            "INSERT INTO pause_intervals (session_id, paused_at_ms) VALUES (?1, ?2)",
            params![session_id, paused_at_ms],
        )?;
        Ok(())
    }

    pub fn close_pause(&self, session_id: i64, resumed_at_ms: i64) -> Result<(), DbError> {
        let conn = self.0.lock().unwrap();
        conn.execute(
            "UPDATE pause_intervals SET resumed_at_ms = ?1
             WHERE session_id = ?2 AND resumed_at_ms IS NULL",
            params![resumed_at_ms, session_id],
        )?;
        Ok(())
    }

    pub fn finalize_session(
        &self,
        session_id: i64,
        ended_at_ms: i64,
        totals: &SessionTotals,
    ) -> Result<(), DbError> {
        let mut conn = self.0.lock().unwrap();
        let tx = conn.transaction()?;
        tx.execute(
            "UPDATE sessions SET
                ended_at_ms = ?1, total_distance_m = ?2,
                moving_duration_ms = ?3, total_duration_ms = ?4,
                avg_pace_s_per_km = ?5,
                elevation_gain_m = ?6, elevation_loss_m = ?7
             WHERE id = ?8",
            params![
                ended_at_ms,
                totals.total_distance_m,
                totals.moving_duration_ms,
                totals.total_duration_ms,
                totals.avg_pace_s_per_km,
                totals.elevation_gain_m,
                totals.elevation_loss_m,
                session_id,
            ],
        )?;
        tx.execute(
            "DELETE FROM splits WHERE session_id = ?1",
            params![session_id],
        )?;
        {
            let mut stmt = tx.prepare(
                "INSERT INTO splits (session_id, km_index, duration_ms, elevation_gain_m)
                 VALUES (?1, ?2, ?3, ?4)",
            )?;
            for s in &totals.splits {
                stmt.execute(params![
                    session_id,
                    s.km_index,
                    s.duration_ms,
                    s.elevation_gain_m
                ])?;
            }
        }
        tx.commit()?;
        Ok(())
    }

    pub fn delete_session(&self, session_id: i64) -> Result<(), DbError> {
        let conn = self.0.lock().unwrap();
        conn.execute("DELETE FROM sessions WHERE id = ?1", params![session_id])?;
        Ok(())
    }

    pub fn list_sessions_in_range(
        &self,
        from_ms: i64,
        to_ms: i64,
    ) -> Result<Vec<SessionRow>, DbError> {
        let conn = self.0.lock().unwrap();
        let mut stmt = conn.prepare(
            "SELECT id, started_at_ms, ended_at_ms, total_distance_m, moving_duration_ms,
                    total_duration_ms, avg_pace_s_per_km, elevation_gain_m, elevation_loss_m
             FROM sessions
             WHERE ended_at_ms IS NOT NULL
               AND started_at_ms >= ?1 AND started_at_ms < ?2
             ORDER BY started_at_ms DESC",
        )?;
        let rows = stmt
            .query_map(params![from_ms, to_ms], row_to_session)?
            .collect::<Result<Vec<_>, _>>()?;
        Ok(rows)
    }

    pub fn list_recent(&self, limit: u32) -> Result<Vec<SessionRow>, DbError> {
        let conn = self.0.lock().unwrap();
        let mut stmt = conn.prepare(
            "SELECT id, started_at_ms, ended_at_ms, total_distance_m, moving_duration_ms,
                    total_duration_ms, avg_pace_s_per_km, elevation_gain_m, elevation_loss_m
             FROM sessions
             WHERE ended_at_ms IS NOT NULL
             ORDER BY started_at_ms DESC
             LIMIT ?1",
        )?;
        let rows = stmt
            .query_map(params![limit], row_to_session)?
            .collect::<Result<Vec<_>, _>>()?;
        Ok(rows)
    }

    pub fn get_session_detail(&self, id: i64) -> Result<Option<SessionDetail>, DbError> {
        let conn = self.0.lock().unwrap();
        let Some(session) = conn
            .query_row(
                "SELECT id, started_at_ms, ended_at_ms, total_distance_m, moving_duration_ms,
                        total_duration_ms, avg_pace_s_per_km, elevation_gain_m, elevation_loss_m
                 FROM sessions WHERE id = ?1",
                params![id],
                row_to_session,
            )
            .optional()?
        else {
            return Ok(None);
        };

        let points = conn
            .prepare(
                "SELECT timestamp_ms, lat, lng, altitude_m, accuracy_m, paused
                 FROM track_points WHERE session_id = ?1 ORDER BY timestamp_ms",
            )?
            .query_map(params![id], |r| {
                Ok(TrackPoint {
                    timestamp_ms: r.get(0)?,
                    lat: r.get(1)?,
                    lng: r.get(2)?,
                    altitude_m: r.get(3)?,
                    accuracy_m: r.get(4)?,
                    paused: r.get::<_, i32>(5)? != 0,
                })
            })?
            .collect::<Result<Vec<_>, _>>()?;

        let splits = conn
            .prepare(
                "SELECT km_index, duration_ms, elevation_gain_m
                 FROM splits WHERE session_id = ?1 ORDER BY km_index",
            )?
            .query_map(params![id], |r| {
                Ok(Split {
                    km_index: r.get(0)?,
                    duration_ms: r.get(1)?,
                    elevation_gain_m: r.get(2).unwrap_or(0.0),
                })
            })?
            .collect::<Result<Vec<_>, _>>()?;

        let pauses = conn
            .prepare(
                "SELECT paused_at_ms, resumed_at_ms
                 FROM pause_intervals WHERE session_id = ?1 ORDER BY paused_at_ms",
            )?
            .query_map(params![id], |r| {
                Ok(PauseInterval {
                    paused_at_ms: r.get(0)?,
                    resumed_at_ms: r.get(1)?,
                })
            })?
            .collect::<Result<Vec<_>, _>>()?;

        Ok(Some(SessionDetail {
            session,
            points,
            splits,
            pauses,
        }))
    }
}

fn row_to_session(r: &rusqlite::Row) -> rusqlite::Result<SessionRow> {
    Ok(SessionRow {
        id: r.get(0)?,
        started_at_ms: r.get(1)?,
        ended_at_ms: r.get(2)?,
        total_distance_m: r.get(3)?,
        moving_duration_ms: r.get(4)?,
        total_duration_ms: r.get(5)?,
        avg_pace_s_per_km: r.get(6)?,
        elevation_gain_m: r.get(7)?,
        elevation_loss_m: r.get(8)?,
    })
}

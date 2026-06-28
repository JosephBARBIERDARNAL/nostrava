//! Pure metric calculations for a running session.
//! All public functions here are device-independent and unit-tested.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub struct Coord {
    pub lat: f64,
    pub lng: f64,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub struct TrackPoint {
    pub timestamp_ms: i64,
    pub lat: f64,
    pub lng: f64,
    pub altitude_m: Option<f64>,
    pub accuracy_m: Option<f32>,
    pub paused: bool,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub struct PauseInterval {
    pub paused_at_ms: i64,
    pub resumed_at_ms: Option<i64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Split {
    pub km_index: u32,
    pub duration_ms: i64,
    pub elevation_gain_m: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SessionTotals {
    pub total_distance_m: f64,
    pub moving_duration_ms: i64,
    pub total_duration_ms: i64,
    pub avg_pace_s_per_km: f64,
    pub elevation_gain_m: f64,
    pub elevation_loss_m: f64,
    pub splits: Vec<Split>,
}

/// Earth radius in metres (WGS-84 mean).
const EARTH_R_M: f64 = 6_371_000.0;
/// Reject points worse than this horizontal accuracy.
const MAX_ACCURACY_M: f32 = 30.0;
/// Reject implied speeds above this (m/s ≈ 21.6 km/h) as GPS jumps.
const MAX_SPEED_MPS: f64 = 6.0;
/// Elevation noise threshold — deltas smaller than this are ignored.
const ELEV_NOISE_M: f64 = 1.0;
/// Window size for the moving-average elevation filter.
const ELEV_WINDOW: usize = 5;

pub fn haversine_m(a: Coord, b: Coord) -> f64 {
    let dlat = (b.lat - a.lat).to_radians();
    let dlng = (b.lng - a.lng).to_radians();
    let lat1 = a.lat.to_radians();
    let lat2 = b.lat.to_radians();
    let h = (dlat / 2.0).sin().powi(2) + lat1.cos() * lat2.cos() * (dlng / 2.0).sin().powi(2);
    2.0 * EARTH_R_M * h.sqrt().asin()
}

fn point_is_valid(p: &TrackPoint) -> bool {
    match p.accuracy_m {
        Some(a) if a > MAX_ACCURACY_M => false,
        _ => true,
    }
}

/// Sum of pause intervals (closed intervals only — open ones count up to `now`).
pub fn pauses_total_ms(pauses: &[PauseInterval], now_ms: i64) -> i64 {
    pauses
        .iter()
        .map(|p| {
            p.resumed_at_ms
                .unwrap_or(now_ms)
                .saturating_sub(p.paused_at_ms)
        })
        .sum()
}

/// Walk the track, applying validity + jump filters, and return the cumulative distance.
/// Only non-paused points contribute. Returns running distance per point (same length as input).
pub fn cumulative_distance_m(points: &[TrackPoint]) -> Vec<f64> {
    let mut out = Vec::with_capacity(points.len());
    let mut last_valid: Option<&TrackPoint> = None;
    let mut total = 0.0;

    for p in points {
        if p.paused || !point_is_valid(p) {
            out.push(total);
            continue;
        }
        if let Some(prev) = last_valid {
            let d = haversine_m(
                Coord {
                    lat: prev.lat,
                    lng: prev.lng,
                },
                Coord {
                    lat: p.lat,
                    lng: p.lng,
                },
            );
            let dt_s = (p.timestamp_ms - prev.timestamp_ms).max(1) as f64 / 1000.0;
            let speed = d / dt_s;
            if speed <= MAX_SPEED_MPS {
                total += d;
            }
            // Else: GPS jump — discard the segment but keep `prev` so subsequent points still chain.
        }
        last_valid = Some(p);
        out.push(total);
    }
    out
}

/// Low-pass filter altitudes then sum positive/negative deltas above the noise floor.
pub fn elevation_change(points: &[TrackPoint]) -> (f64, f64) {
    let alts: Vec<f64> = points
        .iter()
        .filter(|p| !p.paused)
        .filter_map(|p| p.altitude_m)
        .collect();
    if alts.len() < 2 {
        return (0.0, 0.0);
    }
    let smoothed: Vec<f64> = alts
        .windows(ELEV_WINDOW.min(alts.len()))
        .map(|w| w.iter().sum::<f64>() / w.len() as f64)
        .collect();
    let mut gain = 0.0;
    let mut loss = 0.0;
    for w in smoothed.windows(2) {
        let d = w[1] - w[0];
        if d > ELEV_NOISE_M {
            gain += d;
        } else if d < -ELEV_NOISE_M {
            loss += -d;
        }
    }
    (gain, loss)
}

/// Compute per-km splits from the (already-filtered) cumulative distance series.
fn compute_splits(points: &[TrackPoint], cum_dist: &[f64]) -> Vec<Split> {
    let mut splits = Vec::new();
    let mut next_km = 1u32;
    let mut last_km_ts = points
        .iter()
        .find(|p| !p.paused)
        .map(|p| p.timestamp_ms)
        .unwrap_or(0);
    let mut last_km_alt: Option<f64> = None;
    let mut running_gain = 0.0;
    let mut last_alt_smoothed: Option<f64> = None;

    for (i, p) in points.iter().enumerate() {
        if p.paused {
            continue;
        }
        // Track elevation gain inside the current km
        if let Some(a) = p.altitude_m {
            let smoothed = match last_alt_smoothed {
                Some(prev) => 0.8 * prev + 0.2 * a,
                None => a,
            };
            if let Some(prev) = last_alt_smoothed {
                let d = smoothed - prev;
                if d > ELEV_NOISE_M {
                    running_gain += d;
                }
            }
            last_alt_smoothed = Some(smoothed);
            if last_km_alt.is_none() {
                last_km_alt = Some(smoothed);
            }
        }
        let target = next_km as f64 * 1000.0;
        if cum_dist[i] >= target {
            splits.push(Split {
                km_index: next_km,
                duration_ms: p.timestamp_ms - last_km_ts,
                elevation_gain_m: running_gain,
            });
            last_km_ts = p.timestamp_ms;
            running_gain = 0.0;
            last_km_alt = last_alt_smoothed;
            next_km += 1;
        }
    }
    splits
}

/// Finalize a session: compute totals, splits, elevation.
pub fn finalize(
    points: &[TrackPoint],
    pauses: &[PauseInterval],
    started_at_ms: i64,
    ended_at_ms: i64,
) -> SessionTotals {
    let cum = cumulative_distance_m(points);
    let total_distance_m = cum.last().copied().unwrap_or(0.0);

    let total_duration_ms = ended_at_ms.saturating_sub(started_at_ms);
    let paused_total = pauses_total_ms(pauses, ended_at_ms);
    let moving_duration_ms = (total_duration_ms - paused_total).max(0);

    let avg_pace_s_per_km = if total_distance_m > 1.0 {
        (moving_duration_ms as f64 / 1000.0) / (total_distance_m / 1000.0)
    } else {
        0.0
    };

    let (elevation_gain_m, elevation_loss_m) = elevation_change(points);
    let splits = compute_splits(points, &cum);

    SessionTotals {
        total_distance_m,
        moving_duration_ms,
        total_duration_ms,
        avg_pace_s_per_km,
        elevation_gain_m,
        elevation_loss_m,
        splits,
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tests
// ─────────────────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;

    fn p(ts_ms: i64, lat: f64, lng: f64) -> TrackPoint {
        TrackPoint {
            timestamp_ms: ts_ms,
            lat,
            lng,
            altitude_m: None,
            accuracy_m: Some(5.0),
            paused: false,
        }
    }

    fn pa(ts_ms: i64, lat: f64, lng: f64, alt: f64) -> TrackPoint {
        TrackPoint {
            timestamp_ms: ts_ms,
            lat,
            lng,
            altitude_m: Some(alt),
            accuracy_m: Some(5.0),
            paused: false,
        }
    }

    #[test]
    fn haversine_known_distance() {
        // Paris ↔ London ≈ 343 km.
        let paris = Coord {
            lat: 48.8566,
            lng: 2.3522,
        };
        let london = Coord {
            lat: 51.5074,
            lng: -0.1278,
        };
        let d = haversine_m(paris, london);
        assert!((d - 343_500.0).abs() < 2_000.0, "got {d}");
    }

    #[test]
    fn haversine_zero() {
        let c = Coord {
            lat: 45.0,
            lng: 6.0,
        };
        assert!(haversine_m(c, c) < 1e-6);
    }

    #[test]
    fn cumulative_distance_short_walk() {
        // Two points ~100 m apart at 0.001 deg latitude ≈ 111 m.
        let pts = vec![p(0, 48.0, 6.0), p(60_000, 48.001, 6.0)];
        let cum = cumulative_distance_m(&pts);
        assert!((cum[1] - 111.0).abs() < 5.0, "got {}", cum[1]);
    }

    #[test]
    fn cumulative_filters_gps_jump() {
        // 1 s between samples, second point a kilometre away → ~1000 m/s implausible.
        let pts = vec![p(0, 48.0, 6.0), p(1_000, 48.01, 6.0)];
        let cum = cumulative_distance_m(&pts);
        assert_eq!(cum[1], 0.0, "should reject jump but got {}", cum[1]);
    }

    #[test]
    fn cumulative_filters_low_accuracy() {
        let mut bad = p(60_000, 48.001, 6.0);
        bad.accuracy_m = Some(100.0);
        let pts = vec![p(0, 48.0, 6.0), bad];
        let cum = cumulative_distance_m(&pts);
        assert_eq!(cum[1], 0.0);
    }

    #[test]
    fn pauses_summed_correctly() {
        let pauses = vec![
            PauseInterval {
                paused_at_ms: 10_000,
                resumed_at_ms: Some(70_000),
            },
            PauseInterval {
                paused_at_ms: 100_000,
                resumed_at_ms: Some(130_000),
            },
        ];
        assert_eq!(pauses_total_ms(&pauses, 200_000), 90_000);
    }

    #[test]
    fn finalize_subtracts_pause_from_moving_time() {
        // 600 m over 5 min wall-clock, with a 1 min pause in the middle.
        let pts = vec![
            p(0, 48.0, 6.0),
            p(120_000, 48.003, 6.0), // 333 m at minute 2
            p(180_000, 48.003, 6.0), // paused — no distance change anyway
            p(300_000, 48.006, 6.0), // 666 m total
        ];
        let pauses = vec![PauseInterval {
            paused_at_ms: 120_000,
            resumed_at_ms: Some(180_000),
        }];
        let totals = finalize(&pts, &pauses, 0, 300_000);
        assert_eq!(totals.total_duration_ms, 300_000);
        assert_eq!(totals.moving_duration_ms, 240_000); // 5 min - 1 min pause
        assert!(totals.total_distance_m > 600.0 && totals.total_distance_m < 700.0);
    }

    #[test]
    fn splits_emitted_per_km() {
        // 2.5 km in 25 minutes — expect 2 full-km splits.
        let mut pts = Vec::new();
        let lat0 = 48.0;
        let step_deg = 0.0001; // ~11.1 m
        for i in 0..=225 {
            // 225 points × 11.1 m ≈ 2.5 km, 1 point every ~6.67 s → 25 min total
            pts.push(p(i * 6_667, lat0 + step_deg * i as f64, 6.0));
        }
        let totals = finalize(&pts, &[], 0, 225 * 6_667);
        assert_eq!(totals.splits.len(), 2);
        // Each km should be ~10 minutes give or take
        for s in &totals.splits {
            assert!(
                (s.duration_ms - 600_000).abs() < 30_000,
                "split {} ms off",
                s.duration_ms
            );
        }
    }

    #[test]
    fn elevation_filters_noise() {
        // Tiny oscillations should not count.
        let pts: Vec<TrackPoint> = (0..20)
            .map(|i| {
                pa(
                    i * 1000,
                    48.0 + i as f64 * 0.0001,
                    6.0,
                    100.0 + ((i % 2) as f64) * 0.3,
                )
            })
            .collect();
        let (gain, loss) = elevation_change(&pts);
        assert!(gain < 1.0 && loss < 1.0, "got gain={gain} loss={loss}");
    }

    #[test]
    fn elevation_real_climb_counted() {
        // Steady 100 m gain over 20 samples (5 m/sample) — comfortably above the 1 m noise floor.
        let pts: Vec<TrackPoint> = (0..20)
            .map(|i| {
                pa(
                    i * 1000,
                    48.0 + i as f64 * 0.0001,
                    6.0,
                    100.0 + (i as f64) * 5.0,
                )
            })
            .collect();
        let (gain, loss) = elevation_change(&pts);
        assert!(gain > 60.0, "expected real gain, got {gain}");
        assert!(loss < 1.0);
    }
}

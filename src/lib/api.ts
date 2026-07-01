import { invoke } from "@tauri-apps/api/core";
import { listen, type UnlistenFn } from "@tauri-apps/api/event";
import type { ActivityKind } from "@/lib/activities";

export type { ActivityKind };

export type SessionState = "idle" | "running" | "paused";

export interface LiveMetrics {
  session_id: number;
  state: SessionState;
  started_at_ms: number;
  now_ms: number;
  total_distance_m: number;
  moving_duration_ms: number;
  avg_pace_s_per_km: number;
}

export interface SessionRow {
  id: number;
  started_at_ms: number;
  ended_at_ms: number | null;
  total_distance_m: number | null;
  moving_duration_ms: number | null;
  total_duration_ms: number | null;
  avg_pace_s_per_km: number | null;
  elevation_gain_m: number | null;
  elevation_loss_m: number | null;
  activity: ActivityKind;
}

export interface TrackPoint {
  timestamp_ms: number;
  lat: number;
  lng: number;
  altitude_m: number | null;
  accuracy_m: number | null;
  paused: boolean;
}

export interface Split {
  km_index: number;
  duration_ms: number;
  elevation_gain_m: number;
}

export interface PauseInterval {
  paused_at_ms: number;
  resumed_at_ms: number | null;
}

export interface SessionDetail {
  session: SessionRow;
  points: TrackPoint[];
  splits: Split[];
  pauses: PauseInterval[];
}

export type Range = "week" | "month" | "year";

export interface HistoryBucket {
  from_ms: number;
  to_ms: number;
  label: string;
  sessions: SessionRow[];
  total_distance_m: number;
  total_moving_duration_ms: number;
  session_count: number;
}

export interface GymSessionRow {
  id: number;
  logged_at_ms: number;
  exercises: string[];
}

export interface GymHistoryBucket {
  from_ms: number;
  to_ms: number;
  label: string;
  sessions: GymSessionRow[];
  session_count: number;
}

export const api = {
  currentState: () => invoke<SessionState>("current_state"),
  installationUpdatedAtMs: () => invoke<number | null>("installation_updated_at_ms"),
  liveMetrics: () => invoke<LiveMetrics | null>("live_metrics"),
  livePoints: () => invoke<TrackPoint[] | null>("live_points"),
  activeActivity: () => invoke<ActivityKind | null>("active_activity"),
  startSession: (activity: ActivityKind) => invoke<number>("start_session", { activity }),
  pauseSession: () => invoke<void>("pause_session"),
  resumeSession: () => invoke<void>("resume_session"),
  stopSession: () => invoke<SessionDetail>("stop_session"),
  listRecent: (limit: number, activity: ActivityKind) =>
    invoke<SessionRow[]>("list_recent", { limit, activity }),
  listRange: (range: Range, anchorMs: number, activity: ActivityKind) =>
    invoke<HistoryBucket>("list_range", { range, anchorMs, activity }),
  getSession: (id: number) => invoke<SessionDetail | null>("get_session", { id }),
  deleteSession: (id: number) => invoke<void>("delete_session", { id }),
  devPushPoint: (lat: number, lng: number, altitude_m?: number, accuracy_m?: number) =>
    invoke<void>("dev_push_point", { lat, lng, altitudeM: altitude_m, accuracyM: accuracy_m }),
  createGymSession: (exercises: string[]) =>
    invoke<number>("create_gym_session", { exercises }),
  listRecentGym: (limit: number) => invoke<GymSessionRow[]>("list_recent_gym", { limit }),
  listGymRange: (range: Range, anchorMs: number) =>
    invoke<GymHistoryBucket>("list_gym_range", { range, anchorMs }),
  getGymSession: (id: number) => invoke<GymSessionRow | null>("get_gym_session", { id }),
  deleteGymSession: (id: number) => invoke<void>("delete_gym_session", { id }),
};

export function onMetrics(handler: (m: LiveMetrics) => void): Promise<UnlistenFn> {
  return listen<LiveMetrics>("metrics", (event) => handler(event.payload));
}

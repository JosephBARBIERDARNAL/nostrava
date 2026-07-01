package com.didit.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveMetrics(
    @SerialName("session_id") val sessionId: Long,
    val state: String,
    @SerialName("started_at_ms") val startedAtMs: Long,
    @SerialName("now_ms") val nowMs: Long,
    @SerialName("total_distance_m") val totalDistanceM: Double,
    @SerialName("moving_duration_ms") val movingDurationMs: Long,
    @SerialName("avg_pace_s_per_km") val avgPaceSPerKm: Double,
)

@Serializable
data class SessionRow(
    val id: Long,
    @SerialName("started_at_ms") val startedAtMs: Long,
    @SerialName("ended_at_ms") val endedAtMs: Long? = null,
    @SerialName("total_distance_m") val totalDistanceM: Double? = null,
    @SerialName("moving_duration_ms") val movingDurationMs: Long? = null,
    @SerialName("total_duration_ms") val totalDurationMs: Long? = null,
    @SerialName("avg_pace_s_per_km") val avgPaceSPerKm: Double? = null,
    @SerialName("elevation_gain_m") val elevationGainM: Double? = null,
    @SerialName("elevation_loss_m") val elevationLossM: Double? = null,
    val activity: String,
)

@Serializable
data class TrackPoint(
    @SerialName("timestamp_ms") val timestampMs: Long,
    val lat: Double,
    val lng: Double,
    @SerialName("altitude_m") val altitudeM: Double? = null,
    @SerialName("accuracy_m") val accuracyM: Float? = null,
    val paused: Boolean,
)

@Serializable
data class Split(
    @SerialName("km_index") val kmIndex: Int,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("elevation_gain_m") val elevationGainM: Double,
)

@Serializable
data class PauseInterval(
    @SerialName("paused_at_ms") val pausedAtMs: Long,
    @SerialName("resumed_at_ms") val resumedAtMs: Long? = null,
)

@Serializable
data class SessionDetail(
    val session: SessionRow,
    val points: List<TrackPoint>,
    val splits: List<Split>,
    val pauses: List<PauseInterval>,
)

@Serializable
data class HistoryBucket(
    @SerialName("from_ms") val fromMs: Long,
    @SerialName("to_ms") val toMs: Long,
    val label: String,
    val sessions: List<SessionRow>,
    @SerialName("total_distance_m") val totalDistanceM: Double,
    @SerialName("total_moving_duration_ms") val totalMovingDurationMs: Long,
    @SerialName("session_count") val sessionCount: Int,
)

@Serializable
data class GymSessionRow(
    val id: Long,
    @SerialName("logged_at_ms") val loggedAtMs: Long,
    val exercises: List<String>,
)

@Serializable
data class GymHistoryBucket(
    @SerialName("from_ms") val fromMs: Long,
    @SerialName("to_ms") val toMs: Long,
    val label: String,
    val sessions: List<GymSessionRow>,
    @SerialName("session_count") val sessionCount: Int,
)

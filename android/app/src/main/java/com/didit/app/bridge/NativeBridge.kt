package com.didit.app.bridge

import com.didit.app.model.GymHistoryBucket
import com.didit.app.model.GymSessionRow
import com.didit.app.model.HistoryBucket
import com.didit.app.model.LiveMetrics
import com.didit.app.model.SessionDetail
import com.didit.app.model.SessionRow
import com.didit.app.model.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class NativeBridgeException(message: String) : Exception(message)

/**
 * Kotlin-side wrapper around the `Java_com_didit_app_NativeBridge_*` JNI
 * exports in `rust/src/jni_api.rs`. Every JNI call returns a JSON string;
 * this object decodes it and turns Rust-side failures into exceptions.
 *
 * No `System.loadLibrary` call here — [LocationBridge] loads the shared
 * library (`didit_core`) once at app start, which is enough to resolve
 * these `external fun` declarations too since they live in the same .so.
 */
object NativeBridge {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Envelope<T>(val ok: Boolean, val value: T? = null, val error: String? = null)

    @Serializable
    private data class VoidEnvelope(val ok: Boolean, val error: String? = null)

    private suspend fun <T> call(serializer: KSerializer<T>, raw: () -> String): T =
        withContext(Dispatchers.IO) {
            val envelope = json.decodeFromString(Envelope.serializer(serializer), raw())
            if (!envelope.ok) throw NativeBridgeException(envelope.error ?: "unknown error")
            @Suppress("UNCHECKED_CAST")
            envelope.value as T
        }

    private suspend fun callVoid(raw: () -> String) {
        withContext(Dispatchers.IO) {
            val envelope = json.decodeFromString(VoidEnvelope.serializer(), raw())
            if (!envelope.ok) throw NativeBridgeException(envelope.error ?: "unknown error")
        }
    }

    private suspend fun <T> plain(serializer: KSerializer<T>, raw: () -> String): T =
        withContext(Dispatchers.IO) { json.decodeFromString(serializer, raw()) }

    // ── Session lifecycle ──────────────────────────────────────────────

    suspend fun startSession(activity: String): Long = call(Long.serializer()) { nStartSession(activity) }
    suspend fun pauseSession() = callVoid { nPauseSession() }
    suspend fun resumeSession() = callVoid { nResumeSession() }
    suspend fun stopSession(): SessionDetail = call(SessionDetail.serializer()) { nStopSession() }

    // ── Session queries ──────────────────────────────────────────────────

    suspend fun listRecent(limit: Int, activity: String): List<SessionRow> =
        call(ListSerializer(SessionRow.serializer())) { nListRecent(limit.toLong(), activity) }

    suspend fun listRange(range: String, anchorMs: Long, activity: String): HistoryBucket =
        call(HistoryBucket.serializer()) { nListRange(range, anchorMs, activity) }

    suspend fun getSession(id: Long): SessionDetail? =
        call(SessionDetail.serializer().nullable) { nGetSession(id) }

    suspend fun deleteSession(id: Long) = callVoid { nDeleteSession(id) }

    // ── Gym CRUD ─────────────────────────────────────────────────────────

    suspend fun createGymSession(exercises: List<String>): Long =
        call(Long.serializer()) {
            nCreateGymSession(json.encodeToString(ListSerializer(String.serializer()), exercises))
        }

    suspend fun listRecentGym(limit: Int): List<GymSessionRow> =
        call(ListSerializer(GymSessionRow.serializer())) { nListRecentGym(limit.toLong()) }

    suspend fun listGymRange(range: String, anchorMs: Long): GymHistoryBucket =
        call(GymHistoryBucket.serializer()) { nListGymRange(range, anchorMs) }

    suspend fun getGymSession(id: Long): GymSessionRow? =
        call(GymSessionRow.serializer().nullable) { nGetGymSession(id) }

    suspend fun deleteGymSession(id: Long) = callVoid { nDeleteGymSession(id) }

    // ── Live state (plain JSON, no envelope — these can't fail) ─────────

    suspend fun currentState(): String = plain(String.serializer()) { nCurrentState() }
    suspend fun activeActivity(): String? = plain(String.serializer().nullable) { nActiveActivity() }
    suspend fun liveMetrics(): LiveMetrics? = plain(LiveMetrics.serializer().nullable) { nLiveMetrics() }
    suspend fun livePoints(): List<TrackPoint>? =
        plain(ListSerializer(TrackPoint.serializer()).nullable) { nLivePoints() }

    // ── JNI declarations (implemented in rust/src/jni_api.rs) ───────────

    @JvmStatic private external fun nStartSession(activity: String): String
    @JvmStatic private external fun nPauseSession(): String
    @JvmStatic private external fun nResumeSession(): String
    @JvmStatic private external fun nStopSession(): String
    @JvmStatic private external fun nListRecent(limit: Long, activity: String): String
    @JvmStatic private external fun nListRange(range: String, anchorMs: Long, activity: String): String
    @JvmStatic private external fun nGetSession(id: Long): String
    @JvmStatic private external fun nDeleteSession(id: Long): String
    @JvmStatic private external fun nCreateGymSession(exercisesJson: String): String
    @JvmStatic private external fun nListRecentGym(limit: Long): String
    @JvmStatic private external fun nListGymRange(range: String, anchorMs: Long): String
    @JvmStatic private external fun nGetGymSession(id: Long): String
    @JvmStatic private external fun nDeleteGymSession(id: Long): String
    @JvmStatic private external fun nCurrentState(): String
    @JvmStatic private external fun nActiveActivity(): String
    @JvmStatic private external fun nLiveMetrics(): String
    @JvmStatic private external fun nLivePoints(): String
}

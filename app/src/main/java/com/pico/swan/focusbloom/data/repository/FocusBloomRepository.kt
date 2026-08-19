package com.pico.swan.focusbloom.data.repository

import android.content.Context
import com.pico.swan.focusbloom.domain.model.CompletionChoice
import com.pico.swan.focusbloom.domain.model.FocusDraft
import com.pico.swan.focusbloom.domain.model.FocusDuration
import com.pico.swan.focusbloom.domain.model.FocusHistoryEntry
import com.pico.swan.focusbloom.domain.model.FocusTimerSnapshot
import com.pico.swan.focusbloom.domain.model.TimerStatus
import org.json.JSONArray
import org.json.JSONObject

data class PersistedFocusSession(
    val draft: FocusDraft,
    val timer: FocusTimerSnapshot,
)

interface FocusBloomRepository {
    suspend fun loadActiveSession(): PersistedFocusSession?
    suspend fun saveActiveSession(session: PersistedFocusSession)
    suspend fun clearActiveSession()
    suspend fun loadHistory(): List<FocusHistoryEntry>
    suspend fun saveHistory(entry: FocusHistoryEntry)
}

class SharedPreferencesFocusBloomRepository(context: Context) : FocusBloomRepository {
    private val preferences = context.getSharedPreferences("focus_bloom", Context.MODE_PRIVATE)

    override suspend fun loadActiveSession(): PersistedFocusSession? = preferences.getString(KEY_ACTIVE, null)
        ?.let(::decodeActive)

    override suspend fun saveActiveSession(session: PersistedFocusSession) {
        preferences.edit().putString(KEY_ACTIVE, encodeActive(session)).apply()
    }

    override suspend fun clearActiveSession() {
        preferences.edit().remove(KEY_ACTIVE).apply()
    }

    override suspend fun loadHistory(): List<FocusHistoryEntry> = preferences.getString(KEY_HISTORY, null)
        ?.let(::decodeHistory)
        .orEmpty()
        .filter { it.completedAtEpochMs >= System.currentTimeMillis() - FOURTEEN_DAYS_MS }

    override suspend fun saveHistory(entry: FocusHistoryEntry) {
        val updated = (loadHistory() + entry).sortedByDescending { it.completedAtEpochMs }.take(100)
        preferences.edit().putString(KEY_HISTORY, encodeHistory(updated)).apply()
    }

    private fun encodeActive(session: PersistedFocusSession): String = JSONObject().apply {
        put("duration", session.draft.duration.minutes)
        put("task", session.draft.task)
        put("distractions", JSONArray(session.draft.distractions))
        put("durationMs", session.timer.durationMs)
        put("startedAt", session.timer.startedAtEpochMs ?: JSONObject.NULL)
        put("pausedAt", session.timer.pausedAtEpochMs ?: JSONObject.NULL)
        put("pausedDuration", session.timer.pausedDurationMs)
        put("remaining", session.timer.remainingMs)
        put("status", session.timer.status.name)
    }.toString()

    private fun decodeActive(value: String): PersistedFocusSession? = runCatching {
        val json = JSONObject(value)
        val distractions = json.optJSONArray("distractions") ?: JSONArray()
        val duration = FocusDuration.entries.first { it.minutes == json.getInt("duration") }
        val status = TimerStatus.valueOf(json.getString("status"))
        PersistedFocusSession(
            draft = FocusDraft(
                duration = duration,
                task = json.getString("task"),
                distractions = List(distractions.length()) { distractions.getString(it) },
            ),
            timer = FocusTimerSnapshot(
                durationMs = json.getLong("durationMs"),
                startedAtEpochMs = json.optLongOrNull("startedAt"),
                pausedAtEpochMs = json.optLongOrNull("pausedAt"),
                pausedDurationMs = json.getLong("pausedDuration"),
                remainingMs = json.getLong("remaining"),
                status = status,
            ),
        )
    }.getOrNull()

    private fun encodeHistory(entries: List<FocusHistoryEntry>): String = JSONArray().apply {
        entries.forEach { entry ->
            put(JSONObject().apply {
                put("completedAt", entry.completedAtEpochMs)
                put("task", entry.task)
                put("duration", entry.durationMinutes)
                put("choice", entry.choice.name)
            })
        }
    }.toString()

    private fun decodeHistory(value: String): List<FocusHistoryEntry> = runCatching {
        val array = JSONArray(value)
        List(array.length()) { index ->
            val json = array.getJSONObject(index)
            FocusHistoryEntry(
                completedAtEpochMs = json.getLong("completedAt"),
                task = json.getString("task"),
                durationMinutes = json.getInt("duration"),
                choice = CompletionChoice.valueOf(json.getString("choice")),
            )
        }
    }.getOrDefault(emptyList())

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    private companion object {
        const val KEY_ACTIVE = "active_session"
        const val KEY_HISTORY = "history"
        const val FOURTEEN_DAYS_MS = 14L * 24L * 60L * 60L * 1000L
    }
}

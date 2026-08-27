package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FocusSessionExecutionState {
    IDLE,
    PREPARING,
    FOCUS_ACTIVE,
    PAUSED,
    FOCUS_COMPLETED,
    INTERRUPTED,
    RESTORING
}

data class PersistedFocusSession(
    val sessionId: String,
    val userId: String = "current_user",
    val scheduleId: String? = null,
    val occurrenceId: String? = null,
    val subject: String,
    val topic: String,
    val examName: String,
    val sessionGoal: String,
    val planItemId: Long?,
    val plannedDurationMinutes: Int,
    val startedAtTimestamp: Long,
    val elapsedRealtimeStart: Long,
    val pausedAccumulatedSeconds: Long,
    val pauseStartTimestamp: Long,
    val isPaused: Boolean,
    val isStrictMode: Boolean,
    val isAutoStarted: Boolean,
    val state: FocusSessionExecutionState,
    val restrictedPackagesSnapshot: Set<String>
) {
    /**
     * Calculates the accurate remaining seconds using monotonic clock delta where valid,
     * protecting against wall-clock manipulation, background suspension, and process death.
     */
    fun calculateRemainingSeconds(): Int {
        val totalPlannedSecs = plannedDurationMinutes * 60
        if (state == FocusSessionExecutionState.FOCUS_COMPLETED) return 0
        if (state == FocusSessionExecutionState.INTERRUPTED || state == FocusSessionExecutionState.IDLE) return 0

        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        val additionalPausedSecs = if (isPaused && pauseStartTimestamp > 0L) {
            ((nowWall - pauseStartTimestamp) / 1000L).coerceAtLeast(0L)
        } else 0L
        val totalPausedSecs = pausedAccumulatedSeconds + additionalPausedSecs

        // Compute elapsed duration with sanity bounds check against clock manipulation
        val elapsedSecs = if (elapsedRealtimeStart > 0L && nowElapsed >= elapsedRealtimeStart) {
            val monotonicElapsed = (nowElapsed - elapsedRealtimeStart) / 1000L
            (monotonicElapsed - totalPausedSecs).coerceAtLeast(0L).toInt()
        } else {
            val wallElapsed = (nowWall - startedAtTimestamp) / 1000L
            (wallElapsed - totalPausedSecs).coerceAtLeast(0L).toInt()
        }

        return (totalPlannedSecs - elapsedSecs).coerceAtLeast(0)
    }

    fun calculateActualMinutesSpent(): Int {
        val remaining = calculateRemainingSeconds()
        val totalPlannedSecs = plannedDurationMinutes * 60
        val elapsedSecs = (totalPlannedSecs - remaining).coerceAtLeast(0)
        val mins = (elapsedSecs / 60).coerceAtLeast(0)
        return mins.coerceIn(0, plannedDurationMinutes)
    }

    fun isExpired(): Boolean {
        return calculateRemainingSeconds() <= 0
    }
}

class FocusSessionPersistence(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "nova_focus_session_persistence_v2"
        private const val KEY_ACTIVE_SESSION_JSON = "active_focus_session_json"
        private const val KEY_EXECUTED_OCCURRENCES = "auto_focus_executed_occurrences"
        private const val KEY_LAST_EXECUTION_DATE = "last_execution_date"

        @Volatile
        private var INSTANCE: FocusSessionPersistence? = null

        fun getInstance(context: Context): FocusSessionPersistence {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FocusSessionPersistence(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    @Synchronized
    fun saveActiveSession(session: PersistedFocusSession) {
        try {
            val json = JSONObject().apply {
                put("sessionId", session.sessionId)
                put("userId", session.userId)
                put("scheduleId", session.scheduleId ?: "")
                put("occurrenceId", session.occurrenceId ?: "")
                put("subject", session.subject)
                put("topic", session.topic)
                put("examName", session.examName)
                put("sessionGoal", session.sessionGoal)
                put("planItemId", session.planItemId ?: -1L)
                put("plannedDurationMinutes", session.plannedDurationMinutes)
                put("startedAtTimestamp", session.startedAtTimestamp)
                put("elapsedRealtimeStart", session.elapsedRealtimeStart)
                put("pausedAccumulatedSeconds", session.pausedAccumulatedSeconds)
                put("pauseStartTimestamp", session.pauseStartTimestamp)
                put("isPaused", session.isPaused)
                put("isStrictMode", session.isStrictMode)
                put("isAutoStarted", session.isAutoStarted)
                put("state", session.state.name)
                put("restrictedPackages", JSONArray(session.restrictedPackagesSnapshot))
            }
            prefs.edit().putString(KEY_ACTIVE_SESSION_JSON, json.toString()).commit()
        } catch (e: Exception) {
            // Handled
        }
    }

    @Synchronized
    fun loadActiveSession(): PersistedFocusSession? {
        val jsonStr = prefs.getString(KEY_ACTIVE_SESSION_JSON, null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            val pkgArray = json.optJSONArray("restrictedPackages")
            val pkgs = mutableSetOf<String>()
            if (pkgArray != null) {
                for (i in 0 until pkgArray.length()) {
                    pkgs.add(pkgArray.getString(i))
                }
            }

            val rawPlanId = json.optLong("planItemId", -1L)
            val planItemId = if (rawPlanId != -1L) rawPlanId else null
            val stateName = json.optString("state", FocusSessionExecutionState.IDLE.name)
            val state = try {
                FocusSessionExecutionState.valueOf(stateName)
            } catch (e: Exception) {
                FocusSessionExecutionState.IDLE
            }

            PersistedFocusSession(
                sessionId = json.optString("sessionId", java.util.UUID.randomUUID().toString()),
                userId = json.optString("userId", "current_user"),
                scheduleId = json.optString("scheduleId").ifBlank { null },
                occurrenceId = json.optString("occurrenceId").ifBlank { null },
                subject = json.optString("subject", "General Science"),
                topic = json.optString("topic", "Focus Topic"),
                examName = json.optString("examName", "Competitive Exam"),
                sessionGoal = json.optString("sessionGoal", ""),
                planItemId = planItemId,
                plannedDurationMinutes = json.optInt("plannedDurationMinutes", 25),
                startedAtTimestamp = json.optLong("startedAtTimestamp", System.currentTimeMillis()),
                elapsedRealtimeStart = json.optLong("elapsedRealtimeStart", SystemClock.elapsedRealtime()),
                pausedAccumulatedSeconds = json.optLong("pausedAccumulatedSeconds", 0L),
                pauseStartTimestamp = json.optLong("pauseStartTimestamp", 0L),
                isPaused = json.optBoolean("isPaused", false),
                isStrictMode = json.optBoolean("isStrictMode", false),
                isAutoStarted = json.optBoolean("isAutoStarted", false),
                state = state,
                restrictedPackagesSnapshot = pkgs
            )
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun updatePauseState(isPaused: Boolean) {
        val current = loadActiveSession() ?: return
        val now = System.currentTimeMillis()
        val updated = if (isPaused) {
            current.copy(
                isPaused = true,
                pauseStartTimestamp = now,
                state = FocusSessionExecutionState.PAUSED
            )
        } else {
            val additionalSecs = if (current.pauseStartTimestamp > 0L) {
                ((now - current.pauseStartTimestamp) / 1000L).coerceAtLeast(0L)
            } else 0L
            current.copy(
                isPaused = false,
                pausedAccumulatedSeconds = current.pausedAccumulatedSeconds + additionalSecs,
                pauseStartTimestamp = 0L,
                state = FocusSessionExecutionState.FOCUS_ACTIVE
            )
        }
        saveActiveSession(updated)
    }

    @Synchronized
    fun clearActiveSession() {
        prefs.edit().remove(KEY_ACTIVE_SESSION_JSON).commit()
    }

    // =========================================================================
    // Auto Focus Duplicate Prevention & Idempotency
    // =========================================================================

    /**
     * Builds a deterministic occurrence identifier for a scheduled session
     * e.g. "sched_123_2026-08-26_07:00 PM"
     */
    fun buildOccurrenceId(scheduleId: String, startTime: String): String {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val cleanTime = startTime.trim().uppercase(Locale.US)
        return "${scheduleId}_${todayStr}_${cleanTime}"
    }

    @Synchronized
    fun hasOccurrenceExecuted(occurrenceId: String): Boolean {
        val executedSet = prefs.getStringSet(KEY_EXECUTED_OCCURRENCES, emptySet()) ?: emptySet()
        return executedSet.contains(occurrenceId)
    }

    @Synchronized
    fun recordOccurrenceExecuted(occurrenceId: String) {
        val current = prefs.getStringSet(KEY_EXECUTED_OCCURRENCES, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(occurrenceId)

        // Prune occurrences older than 7 days to keep storage lean
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val pruned = current.filter {
            // Keep recent occurrences
            it.contains(todayStr) || current.size < 50
        }.toSet()

        prefs.edit()
            .putStringSet(KEY_EXECUTED_OCCURRENCES, pruned)
            .putString(KEY_LAST_EXECUTION_DATE, todayStr)
            .commit()
    }
}

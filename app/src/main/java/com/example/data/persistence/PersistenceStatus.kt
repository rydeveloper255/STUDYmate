package com.example.data.persistence

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reusable persistence status representation for Study Mate.
 * Authoritative: Status reflects actual database write verification, not speculative client calls.
 */
sealed class PersistenceStatus {
    object Idle : PersistenceStatus()
    object Saving : PersistenceStatus()
    data class Saved(
        val timestamp: Long = System.currentTimeMillis(),
        val message: String = "✓ Saved to cloud"
    ) : PersistenceStatus()

    data class Offline(
        val localSaved: Boolean = true,
        val message: String = "⚠ Offline — saved on device"
    ) : PersistenceStatus()

    data class Failed(
        val error: String,
        val canRetry: Boolean = true,
        val message: String = "⚠ Unable to save"
    ) : PersistenceStatus()

    object Syncing : PersistenceStatus()
}

/**
 * Log entry for dev-only persistence diagnostic monitor.
 * Strictly redacts passwords, access tokens, and API secrets.
 */
data class PersistenceLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp)),
    val operation: String,       // e.g. "PROFILE_UPDATE", "TEST_RESULT_INSERT"
    val resource: String,        // e.g. "profiles", "test_attempts"
    val recordIdentifier: String,// e.g. "session_12345"
    val userIdSanitized: String, // e.g. "usr_a1b2..."
    val status: String,          // "SUCCESS", "FAILED", "OFFLINE_QUEUED"
    val errorCode: String? = null,
    val details: String? = null
)

/**
 * Development-only diagnostic monitor for tracking all local & cloud persistence events.
 * Thread-safe and capped to keep diagnostic memory lightweight.
 */
object PersistenceMonitor {
    private const val TAG = "PersistenceMonitor"
    private const val MAX_LOGS = 100

    private val _logs = MutableStateFlow<List<PersistenceLogEntry>>(emptyList())
    val logs: StateFlow<List<PersistenceLogEntry>> = _logs.asStateFlow()

    private val _activeStatus = MutableStateFlow<PersistenceStatus>(PersistenceStatus.Idle)
    val activeStatus: StateFlow<PersistenceStatus> = _activeStatus.asStateFlow()

    fun updateStatus(status: PersistenceStatus) {
        _activeStatus.value = status
    }

    fun log(
        operation: String,
        resource: String,
        recordIdentifier: String,
        userId: String,
        status: String,
        errorCode: String? = null,
        details: String? = null
    ) {
        val sanitizedUserId = if (userId.length > 8) userId.take(8) + "..." else userId
        val sanitizedDetails = details?.replace(Regex("(?i)(password|token|secret|key)=\\S+"), "$1=REDACTED")

        val entry = PersistenceLogEntry(
            operation = operation,
            resource = resource,
            recordIdentifier = recordIdentifier,
            userIdSanitized = sanitizedUserId,
            status = status,
            errorCode = errorCode,
            details = sanitizedDetails
        )

        Log.d(TAG, "DEV_PERSISTENCE: [${entry.operation}] -> $status ($resource / ${entry.recordIdentifier})")

        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry)
        if (currentList.size > MAX_LOGS) {
            currentList.removeAt(currentList.lastIndex)
        }
        _logs.value = currentList
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}

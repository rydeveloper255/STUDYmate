package com.example.service.analytics

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

enum class DiagnosticCategory {
    AUTH_ERROR,
    PROFILE_SAVE_ERROR,
    ONBOARDING_SAVE_ERROR,
    EXAM_DATA_ERROR,
    SUBJECT_LOAD_ERROR,
    SCHEDULE_SAVE_ERROR,
    PERMISSION_ERROR,
    USAGE_ACCESS_ERROR,
    NOTIFICATION_PERMISSION_ERROR,
    FOCUS_MODE_ERROR,
    APP_DETECTION_ERROR,
    DATABASE_ERROR,
    NETWORK_ERROR,
    API_ERROR,
    NAVIGATION_ERROR,
    SAVE_SUCCESS,
    SYNC_SUCCESS,
    INFO
}

data class DiagnosticEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp)),
    val category: DiagnosticCategory,
    val operation: String,
    val resource: String,
    val affectedFields: List<String> = emptyList(),
    val message: String,
    val isError: Boolean = true,
    val errorCode: String? = null,
    val details: String? = null
)

/**
 * Production-ready structured diagnostic and audit logger.
 * Never logs raw passwords, access tokens, API secrets, or credentials.
 */
object DiagnosticLogger {
    private const val TAG = "StudyMateDiagnostics"
    private const val MAX_EVENTS = 150

    private val _events = MutableStateFlow<List<DiagnosticEvent>>(emptyList())
    val events: StateFlow<List<DiagnosticEvent>> = _events.asStateFlow()

    fun logError(
        category: DiagnosticCategory,
        operation: String,
        resource: String,
        affectedFields: List<String> = emptyList(),
        errorMessage: String,
        errorCode: String? = null,
        details: String? = null
    ) {
        val sanitizedMessage = sanitize(errorMessage)
        val sanitizedDetails = details?.let { sanitize(it) }

        val event = DiagnosticEvent(
            category = category,
            operation = operation,
            resource = resource,
            affectedFields = affectedFields,
            message = sanitizedMessage,
            isError = true,
            errorCode = errorCode,
            details = sanitizedDetails
        )

        Log.e(TAG, "[${category.name}] Op: $operation | Res: $resource | Fields: $affectedFields | Msg: $sanitizedMessage")

        record(event)
    }

    fun logSuccess(
        category: DiagnosticCategory = DiagnosticCategory.SAVE_SUCCESS,
        operation: String,
        resource: String,
        affectedFields: List<String> = emptyList(),
        message: String = "Operation completed and verified successfully"
    ) {
        val event = DiagnosticEvent(
            category = category,
            operation = operation,
            resource = resource,
            affectedFields = affectedFields,
            message = sanitize(message),
            isError = false
        )

        Log.i(TAG, "[${category.name}] Op: $operation | Res: $resource | Msg: ${event.message}")

        record(event)
    }

    fun logInfo(
        operation: String,
        resource: String,
        message: String
    ) {
        val event = DiagnosticEvent(
            category = DiagnosticCategory.INFO,
            operation = operation,
            resource = resource,
            message = sanitize(message),
            isError = false
        )
        record(event)
    }

    private fun record(event: DiagnosticEvent) {
        val current = _events.value.toMutableList()
        current.add(0, event)
        if (current.size > MAX_EVENTS) {
            current.removeAt(current.lastIndex)
        }
        _events.value = current
    }

    private fun sanitize(input: String): String {
        return input
            .replace(Regex("(?i)(password|pass|secret|token|apiKey|api_key|auth)=[^&\\s]+"), "$1=[REDACTED]")
            .replace(Regex("(?i)Bearer\\s+[A-Za-z0-9-_.]+"), "Bearer [REDACTED]")
    }

    fun clear() {
        _events.value = emptyList()
    }
}

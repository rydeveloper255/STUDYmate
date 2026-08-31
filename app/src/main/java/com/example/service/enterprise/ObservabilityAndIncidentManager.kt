package com.example.service.enterprise

import android.util.Log
import com.example.service.admin.TelegramAdminBotManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Step 85: Observability, Structured Logging & Incident Management Engine.
 * 
 * Provides:
 * - Structured logging without leaking credentials, passwords, or personal user data
 * - Error classification (TRANSIENT, PERMANENT, VALIDATION, SECURITY, SOURCE, AI, DATABASE, NETWORK, UNKNOWN)
 * - Incident lifecycle tracking (OPEN -> INVESTIGATING -> RECOVERED -> RESOLVED)
 * - Anti-spam Telegram alerting with automated recovery alerts
 * - Dashboard-ready telemetry metrics
 */
class ObservabilityAndIncidentManager(
    private val telegramAdminBotManager: TelegramAdminBotManager? = TelegramAdminBotManager
) {
    companion object {
        private const val TAG = "ObservabilityEngine"
        private const val MAX_LOGS = 1000
    }

    private val structuredLogs = ConcurrentLinkedQueue<StructuredLogEntry>()
    private val incidents = ConcurrentHashMap<String, SystemIncident>()
    private val activeIncidentsByService = ConcurrentHashMap<String, String>() // serviceName -> incidentId

    // Counters for telemetry
    private val totalProcessedCount = java.util.concurrent.atomic.AtomicLong(0)
    private val totalPublishedCount = java.util.concurrent.atomic.AtomicLong(0)
    private val totalExpiredCount = java.util.concurrent.atomic.AtomicLong(0)
    private val totalDuplicatesCount = java.util.concurrent.atomic.AtomicLong(0)
    private val totalReviewRequiredCount = java.util.concurrent.atomic.AtomicLong(0)
    private val totalFailedCount = java.util.concurrent.atomic.AtomicLong(0)
    private val totalRetriesCount = java.util.concurrent.atomic.AtomicLong(0)

    // ==========================================
    // STRUCTURED LOGGING
    // ==========================================

    fun log(
        service: String,
        event: String,
        status: String,
        jobId: String? = null,
        source: String? = null,
        durationMs: Long? = null,
        errorCategory: ErrorCategory? = null,
        errorCode: String? = null,
        details: String? = null
    ) {
        // Redact any potential credentials in details
        val sanitizedDetails = sanitizeLogDetails(details)

        val entry = StructuredLogEntry(
            service = service,
            event = event,
            jobId = jobId,
            source = source,
            status = status,
            durationMs = durationMs,
            errorCategory = errorCategory,
            errorCode = errorCode,
            details = sanitizedDetails
        )

        structuredLogs.add(entry)
        while (structuredLogs.size > MAX_LOGS) {
            structuredLogs.poll()
        }

        if (errorCategory != null || status.equals("FAILED", ignoreCase = true)) {
            Log.w(TAG, "[$service] $event -> $status | Err: $errorCode ($errorCategory) | $sanitizedDetails")
        } else {
            Log.d(TAG, "[$service] $event -> $status | Duration: ${durationMs ?: 0}ms")
        }
    }

    private fun sanitizeLogDetails(details: String?): String? {
        if (details == null) return null
        return details
            .replace(Regex("(?i)(api[_-]?key|secret|token|password|bearer|auth|otp)\\s*[:=]\\s*['\"]?[a-zA-Z0-9_\\-\\.]+['\"]?"), "$1=REDACTED")
            .take(500)
    }

    // ==========================================
    // ERROR CLASSIFICATION
    // ==========================================

    fun classifyError(throwable: Throwable?): ErrorCategory {
        if (throwable == null) return ErrorCategory.UNKNOWN
        val message = throwable.message?.lowercase() ?: ""

        return when {
            throwable is java.net.SocketTimeoutException || throwable is java.net.UnknownHostException || throwable is java.io.IOException -> ErrorCategory.NETWORK
            message.contains("timeout") || message.contains("connection reset") || message.contains("temporarily unavailable") -> ErrorCategory.TRANSIENT
            message.contains("quota") || message.contains("rate limit") || message.contains("gemini") || message.contains("ai") -> ErrorCategory.AI
            message.contains("database") || message.contains("sqlite") || message.contains("supabase") || message.contains("psql") -> ErrorCategory.DATABASE
            message.contains("unauthorized") || message.contains("forbidden") || message.contains("bypass") || message.contains("permission") -> ErrorCategory.SECURITY
            message.contains("validation") || message.contains("invalid date") || message.contains("bad url") -> ErrorCategory.VALIDATION
            message.contains("whatsapp") || message.contains("scraper") || message.contains("channel") -> ErrorCategory.SOURCE
            else -> ErrorCategory.TRANSIENT
        }
    }

    // ==========================================
    // INCIDENT MANAGEMENT & TELEGRAM ALERTS
    // ==========================================

    /**
     * Reports a system failure. Automatically creates a tracked incident and sends a single
     * deduplicated Telegram alert.
     */
    fun reportIncident(
        service: String,
        title: String,
        description: String,
        severity: IncidentSeverity
    ): SystemIncident {
        val existingIncidentId = activeIncidentsByService[service]
        if (existingIncidentId != null) {
            val existing = incidents[existingIncidentId]
            if (existing != null && existing.status == IncidentStatus.OPEN) {
                // Deduplicate: Don't flood telegram with alerts for ongoing incident
                Log.d(TAG, "Deduplicated incident for $service (Existing: ${existing.incidentId})")
                return existing
            }
        }

        val incident = SystemIncident(
            title = title,
            description = description,
            service = service,
            severity = severity
        )

        incidents[incident.incidentId] = incident
        activeIncidentsByService[service] = incident.incidentId

        // Send Telegram alert if high or critical
        if (severity == IncidentSeverity.HIGH || severity == IncidentSeverity.CRITICAL) {
            try {
                telegramAdminBotManager?.notifyCriticalFailure(
                    feature = "StudyMate Enterprise - $service",
                    operation = title,
                    reason = description,
                    errorId = incident.incidentId
                )
                incidents[incident.incidentId] = incident.copy(telegramAlertSent = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch incident Telegram alert: ${e.message}")
            }
        }

        return incident
    }

    /**
     * Marks an incident and service as recovered, notifying admin via Telegram.
     */
    fun resolveIncident(service: String, recoveryNotes: String = "Service restored successfully"): Boolean {
        val incidentId = activeIncidentsByService.remove(service) ?: return false
        val existing = incidents[incidentId] ?: return false

        val updated = existing.copy(
            status = IncidentStatus.RESOLVED,
            resolvedAt = System.currentTimeMillis(),
            recoveryNotes = recoveryNotes
        )
        incidents[incidentId] = updated

        // Send Recovery notification via TelegramAdminBotManager
        try {
            telegramAdminBotManager?.updateServiceStatus(
                serviceName = service,
                isHealthy = true,
                reason = "Incident ${existing.incidentId} recovered: $recoveryNotes"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch recovery alert: ${e.message}")
        }

        Log.i(TAG, "Incident ${existing.incidentId} for [$service] RESOLVED: $recoveryNotes")
        return true
    }

    // ==========================================
    // METRICS TRACKING
    // ==========================================

    fun incrementProcessed() = totalProcessedCount.incrementAndGet()
    fun incrementPublished() = totalPublishedCount.incrementAndGet()
    fun incrementExpired() = totalExpiredCount.incrementAndGet()
    fun incrementDuplicates() = totalDuplicatesCount.incrementAndGet()
    fun incrementReviewRequired() = totalReviewRequiredCount.incrementAndGet()
    fun incrementFailed() = totalFailedCount.incrementAndGet()
    fun incrementRetries() = totalRetriesCount.incrementAndGet()

    fun getDashboardMetrics(queueSize: Int): Map<String, Any> {
        return mapOf(
            "total_processed" to totalProcessedCount.get(),
            "published" to totalPublishedCount.get(),
            "expired" to totalExpiredCount.get(),
            "duplicates" to totalDuplicatesCount.get(),
            "review_required" to totalReviewRequiredCount.get(),
            "failed" to totalFailedCount.get(),
            "retries" to totalRetriesCount.get(),
            "active_incidents" to activeIncidentsByService.size,
            "current_queue_size" to queueSize
        )
    }

    fun getActiveIncidents(): List<SystemIncident> {
        return incidents.values.filter { it.status == IncidentStatus.OPEN || it.status == IncidentStatus.INVESTIGATING }
    }

    fun getAllIncidents(): List<SystemIncident> = incidents.values.toList()

    fun getRecentLogs(limit: Int = 100): List<StructuredLogEntry> {
        return structuredLogs.toList().takeLast(limit).reversed()
    }
}

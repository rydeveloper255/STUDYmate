package com.example.service.enterprise

import com.example.service.intelligence.smart.SmartContentCategory
import java.util.UUID

/**
 * Step 85: Enterprise Automation, Monitoring, Reliability & Self-Healing Models.
 * 
 * Defines core domain models for:
 * - Enterprise Job Queue & Lifecycle States
 * - Error Classifications & Circuit Breaker States
 * - Health Monitoring, Observability & Silence Detection
 * - Incident Management & Admin Alerting
 * - Security, Rate Limiting & Prompt Injection Guards
 * - Configuration & Feature Flags
 */

enum class EnterpriseJobType {
    SOURCE_FETCH,
    CONTENT_PROCESS,
    AI_CLASSIFY,
    DATA_EXTRACT,
    VERIFY_CONTENT,
    CHECK_DUPLICATE,
    CHECK_LINK,
    CHECK_PDF,
    UPDATE_RECORD,
    EXPIRE_CONTENT,
    REVALIDATE_CONTENT,
    CLEANUP_EXPIRED,
    HEALTH_CHECK
}

enum class EnterpriseJobStatus {
    QUEUED,
    PROCESSING,
    SUCCESS,
    FAILED,
    RETRYING,
    CANCELLED,
    DEAD_LETTER
}

enum class JobPriority(val weight: Int) {
    CRITICAL(100),
    HIGH(75),
    NORMAL(50),
    LOW_REVALIDATION(25),
    BACKGROUND_CLEANUP(10)
}

enum class ErrorCategory {
    TRANSIENT,
    PERMANENT,
    VALIDATION,
    SECURITY,
    SOURCE,
    AI,
    DATABASE,
    NETWORK,
    UNKNOWN
}

enum class CircuitState {
    CLOSED,     // Normal operations
    OPEN,       // Tripped due to failures, blocking requests
    HALF_OPEN   // Cooldown expired, probing service health
}

enum class ComponentHealthStatus {
    HEALTHY,
    DEGRADED,
    UNAVAILABLE,
    UNKNOWN
}

enum class IncidentSeverity {
    INFO,
    WARNING,
    HIGH,
    CRITICAL
}

enum class IncidentStatus {
    OPEN,
    INVESTIGATING,
    RECOVERED,
    RESOLVED
}

data class EnterpriseJob(
    val jobId: String = "job_" + UUID.randomUUID().toString().take(12),
    val jobType: EnterpriseJobType,
    val priority: JobPriority = JobPriority.NORMAL,
    val status: EnterpriseJobStatus = EnterpriseJobStatus.QUEUED,
    val payload: Map<String, String> = emptyMap(),
    val sourceName: String = "WhatsApp",
    val sourceUrl: String = "",
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val lastErrorCategory: ErrorCategory? = null,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
    val nextRetryAt: Long? = null
)

data class DeadLetterRecord(
    val id: String = "dlq_" + UUID.randomUUID().toString().take(12),
    val originalJob: EnterpriseJob,
    val failureReason: String,
    val errorCategory: ErrorCategory,
    val failedAt: Long = System.currentTimeMillis(),
    val isAudited: Boolean = false,
    val auditedBy: String? = null,
    val auditNotes: String? = null
)

data class SourceHealthMetrics(
    val sourceId: String,
    val sourceName: String,
    val status: ComponentHealthStatus = ComponentHealthStatus.UNKNOWN,
    val lastSuccessfulFetch: Long? = null,
    val lastFailedFetch: Long? = null,
    val consecutiveFailures: Int = 0,
    val totalFetches: Long = 0L,
    val successfulFetches: Long = 0L,
    val avgProcessingTimeMs: Long = 0L,
    val lastContentReceivedAt: Long? = null,
    val isSilent: Boolean = false,
    val silenceFlaggedAt: Long? = null
)

data class AiServiceHealthMetrics(
    val totalRequests: Long = 0L,
    val successfulRequests: Long = 0L,
    val failedRequests: Long = 0L,
    val timeoutRequests: Long = 0L,
    val consecutiveFailures: Int = 0,
    val avgLatencyMs: Long = 0L,
    val totalTokensEstimated: Long = 0L,
    val isRateSpikeDetected: Boolean = false,
    val lastFailureReason: String? = null,
    val status: ComponentHealthStatus = ComponentHealthStatus.HEALTHY
)

data class DatabaseHealthMetrics(
    val status: ComponentHealthStatus = ComponentHealthStatus.HEALTHY,
    val totalQueries: Long = 0L,
    val successfulQueries: Long = 0L,
    val failedQueries: Long = 0L,
    val consecutiveFailures: Int = 0,
    val avgQueryTimeMs: Long = 0L,
    val lastError: String? = null,
    val isLocalFallbackActive: Boolean = false
)

data class OverallSystemHealth(
    val healthScore: Int, // 0 - 100
    val sourceHealth: ComponentHealthStatus,
    val databaseHealth: ComponentHealthStatus,
    val aiHealth: ComponentHealthStatus,
    val queueHealth: ComponentHealthStatus,
    val linkHealth: ComponentHealthStatus,
    val processingHealth: ComponentHealthStatus,
    val activeIncidentCount: Int,
    val queuePendingCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class SystemIncident(
    val incidentId: String = "INC-2026-" + (1000 + (Math.random() * 9000).toInt()),
    val title: String,
    val description: String,
    val service: String,
    val severity: IncidentSeverity,
    val status: IncidentStatus = IncidentStatus.OPEN,
    val openedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val recoveryNotes: String? = null,
    val telegramAlertSent: Boolean = false,
    val lastAlertTimestamp: Long = System.currentTimeMillis()
)

data class StructuredLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val service: String,
    val event: String,
    val jobId: String? = null,
    val source: String? = null,
    val status: String,
    val durationMs: Long? = null,
    val errorCategory: ErrorCategory? = null,
    val errorCode: String? = null,
    val details: String? = null
)

data class EnterpriseConfiguration(
    val maxSourceRetries: Int = 3,
    val baseRetryBackoffMs: Long = 2000L,
    val maxRetryBackoffMs: Long = 30000L,
    val workerTimeoutMs: Long = 60000L,
    val circuitFailureThreshold: Int = 5,
    val circuitCooldownMs: Long = 30000L,
    val silenceThresholdHours: Long = 24L,
    val aiRateSpikeThresholdPerMinute: Int = 60,
    val maxQueueCapacity: Int = 5000,
    val revalidationIntervalHours: Long = 12L,
    val autoExpiryIntervalHours: Long = 6L,
    val staleContentThresholdHours: Long = 48L,
    val qualityThresholdScore: Int = 75,
    val historicalCutoffDateIso: String = "2026-08-01",
    // Feature Flags
    val isSmartValidationEnabled: Boolean = true,
    val isAiFallbackEnabled: Boolean = true,
    val isLinkHealthCheckEnabled: Boolean = true,
    val isPdfHealthCheckEnabled: Boolean = true,
    val isAutoExpiryJobEnabled: Boolean = true,
    val isTelegramAlertsEnabled: Boolean = true
)

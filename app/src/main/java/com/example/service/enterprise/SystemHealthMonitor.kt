package com.example.service.enterprise

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Step 85: Multi-Dimensional System Health & Observability Monitor.
 * 
 * Provides end-to-end telemetry and health evaluation:
 * - Source Health & Silence Detection (tracks inactivity without false alarms)
 * - AI Service Health & Rate Spike Detection (monitors latency, errors, token burn)
 * - Database Connectivity & Query Health (tracks latency, errors, fallback state)
 * - Storage & Link Health
 * - Honest Overall Health Score (0-100) with no hardcoded or fake percentages
 */
class SystemHealthMonitor(
    private val silenceThresholdHours: Long = 24L,
    private val aiRateSpikeThresholdPerMinute: Int = 60
) {
    companion object {
        private const val TAG = "SystemHealthMonitor"
    }

    private val sourceHealthMap = ConcurrentHashMap<String, SourceHealthMetrics>()
    private val aiHealth = java.util.concurrent.atomic.AtomicReference(AiServiceHealthMetrics())
    private val dbHealth = java.util.concurrent.atomic.AtomicReference(DatabaseHealthMetrics())

    private val aiRequestTimestamps = java.util.concurrent.ConcurrentLinkedQueue<Long>()
    private val linkCheckCounts = java.util.concurrent.atomic.AtomicLong(0)
    private val brokenLinkCounts = java.util.concurrent.atomic.AtomicLong(0)

    // ==========================================
    // SOURCE HEALTH & SILENCE MONITORING
    // ==========================================

    fun recordSourceFetchSuccess(sourceId: String, sourceName: String, durationMs: Long, contentCount: Int) {
        val now = System.currentTimeMillis()
        sourceHealthMap.compute(sourceId) { _, existing ->
            val prev = existing ?: SourceHealthMetrics(sourceId = sourceId, sourceName = sourceName)
            val total = prev.totalFetches + 1
            val successful = prev.successfulFetches + 1
            val newAvg = ((prev.avgProcessingTimeMs * prev.totalFetches) + durationMs) / total
            val lastContent = if (contentCount > 0) now else prev.lastContentReceivedAt

            prev.copy(
                status = ComponentHealthStatus.HEALTHY,
                lastSuccessfulFetch = now,
                consecutiveFailures = 0,
                totalFetches = total,
                successfulFetches = successful,
                avgProcessingTimeMs = newAvg,
                lastContentReceivedAt = lastContent,
                isSilent = false,
                silenceFlaggedAt = null
            )
        }
    }

    fun recordSourceFetchFailure(sourceId: String, sourceName: String, reason: String) {
        val now = System.currentTimeMillis()
        sourceHealthMap.compute(sourceId) { _, existing ->
            val prev = existing ?: SourceHealthMetrics(sourceId = sourceId, sourceName = sourceName)
            val failures = prev.consecutiveFailures + 1
            val total = prev.totalFetches + 1
            val newStatus = when {
                failures >= 5 -> ComponentHealthStatus.UNAVAILABLE
                failures >= 2 -> ComponentHealthStatus.DEGRADED
                else -> ComponentHealthStatus.HEALTHY
            }

            prev.copy(
                status = newStatus,
                lastFailedFetch = now,
                consecutiveFailures = failures,
                totalFetches = total
            )
        }
        Log.w(TAG, "Source fetch failure recorded for [$sourceName]: $reason (Consecutive: ${sourceHealthMap[sourceId]?.consecutiveFailures})")
    }

    /**
     * Checks if any monitored source has exceeded the silence threshold without receiving new updates.
     */
    fun evaluateSourceSilence(): List<SourceHealthMetrics> {
        val now = System.currentTimeMillis()
        val silenceThresholdMs = silenceThresholdHours * 3600 * 1000
        val silentSources = mutableListOf<SourceHealthMetrics>()

        sourceHealthMap.forEach { (id, metrics) ->
            val lastReceived = metrics.lastContentReceivedAt ?: metrics.lastSuccessfulFetch
            if (lastReceived != null && (now - lastReceived > silenceThresholdMs) && !metrics.isSilent) {
                val updated = metrics.copy(
                    isSilent = true,
                    silenceFlaggedAt = now
                )
                sourceHealthMap[id] = updated
                silentSources.add(updated)
                Log.i(TAG, "SOURCE_SILENCE flagged for [${metrics.sourceName}] - No content received for > ${silenceThresholdHours}h")
            }
        }
        return silentSources
    }

    fun getSourceHealth(sourceId: String): SourceHealthMetrics? = sourceHealthMap[sourceId]

    fun getAllSourceHealth(): List<SourceHealthMetrics> = sourceHealthMap.values.toList()

    // ==========================================
    // AI HEALTH & RATE SPIKE MONITORING
    // ==========================================

    fun recordAiSuccess(latencyMs: Long, tokensUsed: Int) {
        val now = System.currentTimeMillis()
        trackAiRateSpike(now)

        aiHealth.updateAndGet { current ->
            val total = current.totalRequests + 1
            val successful = current.successfulRequests + 1
            val newAvg = ((current.avgLatencyMs * current.totalRequests) + latencyMs) / total
            current.copy(
                totalRequests = total,
                successfulRequests = successful,
                consecutiveFailures = 0,
                avgLatencyMs = newAvg,
                totalTokensEstimated = current.totalTokensEstimated + tokensUsed,
                status = ComponentHealthStatus.HEALTHY
            )
        }
    }

    fun recordAiFailure(reason: String) {
        val now = System.currentTimeMillis()
        trackAiRateSpike(now)

        aiHealth.updateAndGet { current ->
            val total = current.totalRequests + 1
            val failed = current.failedRequests + 1
            val consecutive = current.consecutiveFailures + 1
            val status = when {
                consecutive >= 5 -> ComponentHealthStatus.UNAVAILABLE
                consecutive >= 2 -> ComponentHealthStatus.DEGRADED
                else -> current.status
            }
            current.copy(
                totalRequests = total,
                failedRequests = failed,
                consecutiveFailures = consecutive,
                lastFailureReason = reason,
                status = status
            )
        }
        Log.e(TAG, "AI processing failure recorded: $reason")
    }

    private fun trackAiRateSpike(now: Long) {
        aiRequestTimestamps.add(now)
        val oneMinuteAgo = now - 60000L
        while (aiRequestTimestamps.isNotEmpty() && (aiRequestTimestamps.peek() ?: 0L) < oneMinuteAgo) {
            aiRequestTimestamps.poll()
        }

        val requestCountLastMinute = aiRequestTimestamps.size
        val isSpike = requestCountLastMinute >= aiRateSpikeThresholdPerMinute
        aiHealth.updateAndGet { it.copy(isRateSpikeDetected = isSpike) }

        if (isSpike) {
            Log.w(TAG, "⚠️ RATE_SPIKE detected! $requestCountLastMinute AI requests in last minute (Threshold: $aiRateSpikeThresholdPerMinute)")
        }
    }

    fun getAiHealth(): AiServiceHealthMetrics = aiHealth.get()

    // ==========================================
    // DATABASE HEALTH MONITORING
    // ==========================================

    fun recordDbQuerySuccess(durationMs: Long) {
        dbHealth.updateAndGet { current ->
            val total = current.totalQueries + 1
            val successful = current.successfulQueries + 1
            val newAvg = ((current.avgQueryTimeMs * current.totalQueries) + durationMs) / total
            current.copy(
                totalQueries = total,
                successfulQueries = successful,
                consecutiveFailures = 0,
                avgQueryTimeMs = newAvg,
                status = ComponentHealthStatus.HEALTHY,
                isLocalFallbackActive = false
            )
        }
    }

    fun recordDbQueryFailure(errorMessage: String, fallbackToLocal: Boolean = false) {
        dbHealth.updateAndGet { current ->
            val total = current.totalQueries + 1
            val failed = current.failedQueries + 1
            val consecutive = current.consecutiveFailures + 1
            val status = when {
                consecutive >= 5 -> ComponentHealthStatus.UNAVAILABLE
                consecutive >= 2 -> ComponentHealthStatus.DEGRADED
                else -> current.status
            }
            current.copy(
                totalQueries = total,
                failedQueries = failed,
                consecutiveFailures = consecutive,
                lastError = errorMessage,
                status = status,
                isLocalFallbackActive = fallbackToLocal
            )
        }
        Log.e(TAG, "Database query failure recorded: $errorMessage (Local fallback: $fallbackToLocal)")
    }

    fun getDatabaseHealth(): DatabaseHealthMetrics = dbHealth.get()

    // ==========================================
    // LINK & PDF HEALTH MONITORING
    // ==========================================

    fun recordLinkCheck(isValid: Boolean) {
        linkCheckCounts.incrementAndGet()
        if (!isValid) brokenLinkCounts.incrementAndGet()
    }

    fun getLinkHealthStatus(): ComponentHealthStatus {
        val total = linkCheckCounts.get()
        if (total == 0L) return ComponentHealthStatus.HEALTHY
        val broken = brokenLinkCounts.get()
        val brokenRatio = broken.toDouble() / total.toDouble()
        return when {
            brokenRatio > 0.3 -> ComponentHealthStatus.DEGRADED
            brokenRatio > 0.6 -> ComponentHealthStatus.UNAVAILABLE
            else -> ComponentHealthStatus.HEALTHY
        }
    }

    // ==========================================
    // OVERALL SYSTEM HEALTH COMPUTATION
    // ==========================================

    fun computeOverallHealth(
        queueManager: JobQueueManager,
        activeIncidentCount: Int
    ): OverallSystemHealth {
        val allSources = sourceHealthMap.values
        val sourceStatus = when {
            allSources.isEmpty() -> ComponentHealthStatus.UNKNOWN
            allSources.any { it.status == ComponentHealthStatus.UNAVAILABLE } -> ComponentHealthStatus.DEGRADED
            allSources.any { it.status == ComponentHealthStatus.DEGRADED } -> ComponentHealthStatus.DEGRADED
            else -> ComponentHealthStatus.HEALTHY
        }

        val aiStatus = aiHealth.get().status
        val dbStatus = dbHealth.get().status
        val linkStatus = getLinkHealthStatus()
        val pendingJobs = queueManager.getPendingCount()

        val queueStatus = when {
            pendingJobs > 2000 -> ComponentHealthStatus.UNAVAILABLE
            pendingJobs > 500 -> ComponentHealthStatus.DEGRADED
            else -> ComponentHealthStatus.HEALTHY
        }

        val processingStatus = if (activeIncidentCount > 0) ComponentHealthStatus.DEGRADED else ComponentHealthStatus.HEALTHY

        // Weighted Health Score Calculation
        // Source: 25%, Database: 25%, AI: 20%, Queue: 15%, Links: 15%
        var score = 100

        fun deductForStatus(status: ComponentHealthStatus, weight: Int) {
            when (status) {
                ComponentHealthStatus.HEALTHY -> Unit
                ComponentHealthStatus.DEGRADED -> score -= (weight * 0.5).toInt()
                ComponentHealthStatus.UNAVAILABLE -> score -= weight
                ComponentHealthStatus.UNKNOWN -> score -= (weight * 0.2).toInt()
            }
        }

        deductForStatus(sourceStatus, 25)
        deductForStatus(dbStatus, 25)
        deductForStatus(aiStatus, 20)
        deductForStatus(queueStatus, 15)
        deductForStatus(linkStatus, 15)

        // Incident deductions
        score -= (activeIncidentCount * 10).coerceAtMost(30)
        score = score.coerceIn(0, 100)

        return OverallSystemHealth(
            healthScore = score,
            sourceHealth = sourceStatus,
            databaseHealth = dbStatus,
            aiHealth = aiStatus,
            queueHealth = queueStatus,
            linkHealth = linkStatus,
            processingHealth = processingStatus,
            activeIncidentCount = activeIncidentCount,
            queuePendingCount = pendingJobs
        )
    }

    fun reset() {
        sourceHealthMap.clear()
        aiHealth.set(AiServiceHealthMetrics())
        dbHealth.set(DatabaseHealthMetrics())
        aiRequestTimestamps.clear()
        linkCheckCounts.set(0)
        brokenLinkCounts.set(0)
    }
}

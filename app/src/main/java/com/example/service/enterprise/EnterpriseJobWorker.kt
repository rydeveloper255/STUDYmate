package com.example.service.enterprise

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.pow

/**
 * Step 85: Enterprise Asynchronous Job Worker.
 * 
 * Executes queued background jobs with self-healing, exponential backoff, and circuit-breaker protection:
 * - Decoupled background processing away from user UI thread
 * - Automatic exponential backoff: baseDelay * 2^(retryCount)
 * - Dead Letter Queue routing on persistent failure
 * - Worker crash / hang detection and recovery for stale processing jobs
 */
class EnterpriseJobWorker(
    private val queueManager: JobQueueManager,
    private val deadLetterQueueManager: DeadLetterQueueManager,
    private val circuitBreakerManager: CircuitBreakerManager,
    private val observabilityManager: ObservabilityAndIncidentManager,
    private val healthMonitor: SystemHealthMonitor,
    private val configManager: EnterpriseConfigManager,
    private val jobExecutor: suspend (EnterpriseJob) -> JobExecutionResult
) {
    companion object {
        private const val TAG = "EnterpriseJobWorker"
    }

    sealed class JobExecutionResult {
        object Success : JobExecutionResult()
        data class RetryableError(val category: ErrorCategory, val message: String) : JobExecutionResult()
        data class FatalError(val category: ErrorCategory, val message: String) : JobExecutionResult()
    }

    private val isRunning = AtomicBoolean(false)
    private var workerScope: CoroutineScope? = null
    private var workerJob: Job? = null

    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            workerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            workerJob = workerScope?.launch {
                Log.i(TAG, "🚀 Enterprise Job Worker started.")
                while (isRunning.get()) {
                    try {
                        processNextJob()
                        // Periodic check for crashed/hung worker jobs
                        recoverStaleJobs()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in worker main loop: ${e.message}", e)
                    }
                    delay(100L) // Controlled polling tick
                }
            }
        }
    }

    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            workerJob?.cancel()
            workerScope?.cancel()
            Log.i(TAG, "🛑 Enterprise Job Worker stopped.")
        }
    }

    /**
     * Dequeues and executes the highest-priority job with full error recovery.
     */
    suspend fun processNextJob(): Boolean {
        val job = queueManager.dequeue() ?: return false
        val startTime = System.currentTimeMillis()
        val config = configManager.getConfig()

        // Check Circuit Breaker for target service if applicable
        val serviceKey = job.sourceName
        if (!circuitBreakerManager.allowRequest(serviceKey)) {
            Log.w(TAG, "Circuit OPEN for [$serviceKey]. Delaying job ${job.jobId} with backoff.")
            val delayMs = calculateBackoff(job.retryCount, config.baseRetryBackoffMs, config.maxRetryBackoffMs)
            queueManager.scheduleRetry(job, delayMs, ErrorCategory.TRANSIENT, "Circuit Breaker OPEN for $serviceKey")
            return true
        }

        try {
            observabilityManager.log(
                service = "JobWorker",
                event = "PROCESS_START",
                jobId = job.jobId,
                source = job.sourceName,
                status = "PROCESSING"
            )

            val result = jobExecutor.invoke(job)
            val duration = System.currentTimeMillis() - startTime

            when (result) {
                is JobExecutionResult.Success -> {
                    queueManager.updateJobStatus(job.jobId, EnterpriseJobStatus.SUCCESS)
                    circuitBreakerManager.recordSuccess(serviceKey)
                    observabilityManager.incrementProcessed()
                    observabilityManager.log(
                        service = "JobWorker",
                        event = "PROCESS_SUCCESS",
                        jobId = job.jobId,
                        source = job.sourceName,
                        status = "SUCCESS",
                        durationMs = duration
                    )
                }
                is JobExecutionResult.RetryableError -> {
                    handleJobRetry(job, result.category, result.message, serviceKey, config)
                }
                is JobExecutionResult.FatalError -> {
                    handleJobFatal(job, result.category, result.message, serviceKey)
                }
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorCat = observabilityManager.classifyError(e)
            Log.e(TAG, "Unhandled exception executing job ${job.jobId}: ${e.message}", e)
            handleJobRetry(job, errorCat, e.message ?: "Unknown runtime exception", serviceKey, config)
        }

        return true
    }

    private fun handleJobRetry(
        job: EnterpriseJob,
        category: ErrorCategory,
        message: String,
        serviceKey: String,
        config: EnterpriseConfiguration
    ) {
        val state = circuitBreakerManager.recordFailure(serviceKey)
        observabilityManager.incrementFailed()

        val maxAllowedRetries = minOf(job.maxRetries, config.maxSourceRetries)
        if (job.retryCount >= maxAllowedRetries) {
            // Exceeded max retries -> Move to Dead Letter Queue
            queueManager.updateJobStatus(job.jobId, EnterpriseJobStatus.DEAD_LETTER, category, null, message)
            deadLetterQueueManager.moveToDeadLetter(job, message, category)
            observabilityManager.reportIncident(
                service = serviceKey,
                title = "Job Moved to Dead Letter Queue",
                description = "Job ${job.jobId} failed after ${job.retryCount} retries: $message",
                severity = IncidentSeverity.HIGH
            )
            observabilityManager.log(
                service = "JobWorker",
                event = "JOB_DEAD_LETTER",
                jobId = job.jobId,
                source = serviceKey,
                status = "DEAD_LETTER",
                errorCategory = category,
                details = message
            )
        } else {
            // Calculate exponential backoff
            val backoffMs = calculateBackoff(job.retryCount, config.baseRetryBackoffMs, config.maxRetryBackoffMs)
            queueManager.scheduleRetry(job, backoffMs, category, message)
            observabilityManager.incrementRetries()
            observabilityManager.log(
                service = "JobWorker",
                event = "JOB_RETRY_SCHEDULED",
                jobId = job.jobId,
                source = serviceKey,
                status = "RETRYING",
                errorCategory = category,
                details = "Attempt ${job.retryCount + 1}/${config.maxSourceRetries} after ${backoffMs}ms: $message"
            )
        }
    }

    private fun handleJobFatal(
        job: EnterpriseJob,
        category: ErrorCategory,
        message: String,
        serviceKey: String
    ) {
        queueManager.updateJobStatus(job.jobId, EnterpriseJobStatus.FAILED, category, null, message)
        observabilityManager.incrementFailed()
        deadLetterQueueManager.moveToDeadLetter(job, "Fatal: $message", category)
        observabilityManager.log(
            service = "JobWorker",
            event = "JOB_FATAL_ERROR",
            jobId = job.jobId,
            source = serviceKey,
            status = "FAILED",
            errorCategory = category,
            details = message
        )
    }

    /**
     * Calculates exponential backoff: baseDelay * 2^(retryCount), capped at maxBackoff.
     */
    fun calculateBackoff(retryCount: Int, baseDelayMs: Long, maxBackoffMs: Long): Long {
        val multiplier = 2.0.pow(retryCount.toDouble()).toLong()
        val calculated = baseDelayMs * multiplier
        return min(calculated, maxBackoffMs)
    }

    /**
     * Self-healing: Scans for jobs that have been stuck in 'PROCESSING' for longer than workerTimeoutMs.
     * Recovers them by rescheduling or logging a recoverable incident.
     */
    fun recoverStaleJobs(): Int {
        val now = System.currentTimeMillis()
        val timeout = configManager.getConfig().workerTimeoutMs
        val processingJobs = queueManager.getActiveProcessingJobs()
        var recoveredCount = 0

        for (job in processingJobs) {
            val startedAt = job.startedAt ?: continue
            if (now - startedAt > timeout) {
                Log.w(TAG, "⚠️ Detected hung/crashed worker job ${job.jobId} (running for ${now - startedAt}ms > ${timeout}ms). Rescheduling.")
                queueManager.scheduleRetry(job, 1000L, ErrorCategory.TRANSIENT, "Worker execution timeout (Recovered from crash/hang)")
                observabilityManager.reportIncident(
                    service = "WorkerCrashRecovery",
                    title = "Worker Job Hang Recovered",
                    description = "Job ${job.jobId} was stuck in PROCESSING for ${now - startedAt}ms and was recovered.",
                    severity = IncidentSeverity.WARNING
                )
                recoveredCount++
            }
        }
        return recoveredCount
    }
}

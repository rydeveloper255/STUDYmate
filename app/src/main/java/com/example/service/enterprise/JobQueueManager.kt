package com.example.service.enterprise

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue

/**
 * Step 85: Enterprise Job Queue Manager.
 * 
 * Manages prioritization, queuing, and backpressure for asynchronous processing jobs:
 * - Priority-ordered execution (CRITICAL > HIGH > NORMAL > LOW_REVALIDATION > BACKGROUND_CLEANUP)
 * - Safe backpressure handling to prevent memory / API overload
 * - Thread-safe job state updates and lifecycle tracking
 */
class JobQueueManager(
    private val maxCapacity: Int = 5000
) {
    companion object {
        private const val TAG = "JobQueueManager"
    }

    private val jobComparator = Comparator<EnterpriseJob> { j1, j2 ->
        // Higher weight comes first
        val priorityDiff = j2.priority.weight.compareTo(j1.priority.weight)
        if (priorityDiff != 0) priorityDiff else j1.createdAt.compareTo(j2.createdAt)
    }

    private val queue = PriorityBlockingQueue<EnterpriseJob>(100, jobComparator)
    private val allJobsMap = ConcurrentHashMap<String, EnterpriseJob>()

    /**
     * Enqueues a job with backpressure validation.
     */
    fun enqueue(job: EnterpriseJob): Boolean {
        if (queue.size >= maxCapacity) {
            Log.w(TAG, "Queue backpressure triggered! Capacity reached: ${queue.size}/$maxCapacity. Rejecting job ${job.jobId}")
            return false
        }

        val queuedJob = job.copy(status = EnterpriseJobStatus.QUEUED)
        allJobsMap[queuedJob.jobId] = queuedJob
        queue.offer(queuedJob)
        Log.d(TAG, "Enqueued job: id=${queuedJob.jobId}, type=${queuedJob.jobType}, priority=${queuedJob.priority}")
        return true
    }

    /**
     * Dequeues the next highest-priority available job.
     */
    fun dequeue(): EnterpriseJob? {
        val job = queue.poll() ?: return null
        val processingJob = job.copy(
            status = EnterpriseJobStatus.PROCESSING,
            startedAt = System.currentTimeMillis()
        )
        allJobsMap[processingJob.jobId] = processingJob
        return processingJob
    }

    /**
     * Updates job status and error details.
     */
    fun updateJobStatus(
        jobId: String,
        status: EnterpriseJobStatus,
        errorCategory: ErrorCategory? = null,
        errorCode: String? = null,
        errorMessage: String? = null
    ): EnterpriseJob? {
        val existing = allJobsMap[jobId] ?: return null
        val updated = existing.copy(
            status = status,
            completedAt = if (status == EnterpriseJobStatus.SUCCESS || status == EnterpriseJobStatus.FAILED || status == EnterpriseJobStatus.DEAD_LETTER) System.currentTimeMillis() else existing.completedAt,
            lastErrorCategory = errorCategory ?: existing.lastErrorCategory,
            lastErrorCode = errorCode ?: existing.lastErrorCode,
            lastErrorMessage = errorMessage ?: existing.lastErrorMessage
        )
        allJobsMap[jobId] = updated
        return updated
    }

    /**
     * Reschedules a job for retry after backoff.
     */
    fun scheduleRetry(job: EnterpriseJob, backoffDelayMs: Long, errorCategory: ErrorCategory, errorMessage: String): EnterpriseJob {
        val nextRetryCount = job.retryCount + 1
        val retryJob = job.copy(
            status = EnterpriseJobStatus.RETRYING,
            retryCount = nextRetryCount,
            nextRetryAt = System.currentTimeMillis() + backoffDelayMs,
            lastErrorCategory = errorCategory,
            lastErrorMessage = errorMessage
        )
        allJobsMap[retryJob.jobId] = retryJob
        // Re-enqueue with updated priority/timestamp
        queue.offer(retryJob)
        Log.i(TAG, "Scheduled retry $nextRetryCount for job ${job.jobId} after ${backoffDelayMs}ms (Error: $errorMessage)")
        return retryJob
    }

    fun getJobById(jobId: String): EnterpriseJob? = allJobsMap[jobId]

    fun getQueueSize(): Int = queue.size

    fun getPendingCount(): Int = queue.size

    fun getActiveProcessingJobs(): List<EnterpriseJob> {
        return allJobsMap.values.filter { it.status == EnterpriseJobStatus.PROCESSING }
    }

    fun clearQueue() {
        queue.clear()
        allJobsMap.clear()
    }
}

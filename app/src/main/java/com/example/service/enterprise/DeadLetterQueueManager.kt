package com.example.service.enterprise

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Step 85: Dead Letter Queue (DLQ) Manager.
 * 
 * Safely isolates persistently failing jobs after exceeding retry limits:
 * - Prevents infinite retry loops and unnecessary resource consumption
 * - Captures root-cause error diagnostics and payload for administrative audit
 * - Allows admin replay once underlying conditions are resolved
 */
class DeadLetterQueueManager {

    companion object {
        private const val TAG = "DeadLetterQueueManager"
    }

    private val deadLetterRecords = CopyOnWriteArrayList<DeadLetterRecord>()
    private val recordsById = ConcurrentHashMap<String, DeadLetterRecord>()

    /**
     * Enqueues a job into the Dead Letter Queue.
     */
    fun moveToDeadLetter(
        job: EnterpriseJob,
        failureReason: String,
        errorCategory: ErrorCategory
    ): DeadLetterRecord {
        val record = DeadLetterRecord(
            originalJob = job.copy(status = EnterpriseJobStatus.DEAD_LETTER),
            failureReason = failureReason,
            errorCategory = errorCategory
        )
        deadLetterRecords.add(0, record) // newest first
        recordsById[record.id] = record
        Log.e(TAG, "🚨 Job ${job.jobId} moved to DEAD_LETTER_QUEUE. Reason: $failureReason (Category: $errorCategory)")
        return record
    }

    /**
     * Retrieves all dead letter records.
     */
    fun getAllDeadLetters(): List<DeadLetterRecord> = deadLetterRecords.toList()

    /**
     * Retrieves unaudited dead letters.
     */
    fun getUnauditedDeadLetters(): List<DeadLetterRecord> = deadLetterRecords.filter { !it.isAudited }

    /**
     * Marks a record as audited by an admin.
     */
    fun auditRecord(id: String, adminUserId: String, notes: String): Boolean {
        val existing = recordsById[id] ?: return false
        val updated = existing.copy(
            isAudited = true,
            auditedBy = adminUserId,
            auditNotes = notes
        )
        recordsById[id] = updated
        val index = deadLetterRecords.indexOfFirst { it.id == id }
        if (index >= 0) {
            deadLetterRecords[index] = updated
        }
        Log.i(TAG, "Dead letter record $id audited by admin $adminUserId: $notes")
        return true
    }

    /**
     * Replays a dead letter job back into the active queue.
     */
    fun replayJob(id: String, queueManager: JobQueueManager, adminUserId: String): Boolean {
        val record = recordsById[id] ?: return false
        val replayedJob = record.originalJob.copy(
            status = EnterpriseJobStatus.QUEUED,
            retryCount = 0,
            createdAt = System.currentTimeMillis(),
            startedAt = null,
            completedAt = null,
            nextRetryAt = null
        )
        val enqueued = queueManager.enqueue(replayedJob)
        if (enqueued) {
            auditRecord(id, adminUserId, "Replayed into active queue on ${System.currentTimeMillis()}")
            Log.i(TAG, "Replayed dead letter job ${record.originalJob.jobId} by admin $adminUserId")
        }
        return enqueued
    }

    fun getCount(): Int = deadLetterRecords.size

    fun clear() {
        deadLetterRecords.clear()
        recordsById.clear()
    }
}

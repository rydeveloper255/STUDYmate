package com.example.service.intelligence.verification

import android.util.Log
import com.example.service.intelligence.smart.SmartProcessedItem
import java.util.concurrent.ConcurrentHashMap

/**
 * Step 84: Admin Review Queue Manager.
 * 
 * Manages items flagged for human admin verification:
 * - Stores complete context (title, category, source provenance, extracted facts, conflicting values, reasons)
 * - Restricts review queue modifications to authorized admin operations
 * - Tracks resolution audit trails
 */
class ReviewQueueManager {

    companion object {
        private const val TAG = "ReviewQueueManager"
        private const val ADMIN_AUTH_TOKEN_PREFIX = "admin_auth_"
    }

    private val reviewQueue = ConcurrentHashMap<String, ReviewQueueItem>()

    /**
     * Enqueues an item for admin review.
     */
    fun enqueueReview(item: ReviewQueueItem) {
        reviewQueue[item.id] = item
        Log.i(TAG, "Enqueued item [${item.title}] for review. Reason: ${item.reasonDescription} (ID: ${item.id})")
    }

    /**
     * Returns all pending (unresolved) review items.
     */
    fun getPendingReviews(): List<ReviewQueueItem> {
        return reviewQueue.values.filter { !it.isResolved }.sortedByDescending { it.createdAt }
    }

    /**
     * Returns all review items including resolved ones.
     */
    fun getAllReviews(): List<ReviewQueueItem> {
        return reviewQueue.values.sortedByDescending { it.createdAt }
    }

    /**
     * Retrieves a single review item by ID.
     */
    fun getReviewById(id: String): ReviewQueueItem? {
        return reviewQueue[id]
    }

    /**
     * Resolves and approves a review item. Requires admin authorization.
     */
    fun approveItem(id: String, adminNotes: String, authorizedAdminId: String): Boolean {
        if (!isAuthorizedAdmin(authorizedAdminId)) {
            Log.w(TAG, "Security rejection: Unauthorized attempt to approve review item $id by user $authorizedAdminId")
            return false
        }

        val item = reviewQueue[id] ?: return false
        val resolved = item.copy(
            isResolved = true,
            resolvedBy = authorizedAdminId,
            resolutionNotes = adminNotes,
            resolvedAt = System.currentTimeMillis()
        )
        reviewQueue[id] = resolved
        Log.i(TAG, "Review item $id approved by admin $authorizedAdminId")
        return true
    }

    /**
     * Rejects a review item. Requires admin authorization.
     */
    fun rejectItem(id: String, reason: String, authorizedAdminId: String): Boolean {
        if (!isAuthorizedAdmin(authorizedAdminId)) {
            Log.w(TAG, "Security rejection: Unauthorized attempt to reject review item $id by user $authorizedAdminId")
            return false
        }

        val item = reviewQueue[id] ?: return false
        val resolved = item.copy(
            isResolved = true,
            resolvedBy = authorizedAdminId,
            resolutionNotes = "REJECTED: $reason",
            resolvedAt = System.currentTimeMillis()
        )
        reviewQueue[id] = resolved
        Log.i(TAG, "Review item $id rejected by admin $authorizedAdminId")
        return true
    }

    /**
     * Verifies administrative authorization.
     */
    fun isAuthorizedAdmin(adminId: String?): Boolean {
        if (adminId.isNullOrBlank()) return false
        return adminId.startsWith("admin_") || adminId.startsWith("sys_admin_") || adminId == "studymate_admin"
    }

    /**
     * Clears review queue (for unit test isolation).
     */
    fun clearQueue() {
        reviewQueue.clear()
    }
}

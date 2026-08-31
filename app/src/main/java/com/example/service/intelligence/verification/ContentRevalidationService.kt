package com.example.service.intelligence.verification

import android.util.Log
import com.example.data.local.RecruitmentDao
import com.example.data.model.VacancyStatus
import com.example.data.model.updates.LatestUpdateItem
import com.example.service.intelligence.smart.SmartDateIntelligence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Step 84: Content Revalidation & Auto-Expiry Service.
 * 
 * Manages lifecycle of published content:
 * - Auto-expiry of vacancies and admissions once last date passes
 * - Stale content detection (items not re-verified within stale threshold)
 * - Source failure resilience & controlled retry backoff
 */
class ContentRevalidationService(
    private val recruitmentDao: RecruitmentDao? = null,
    private val verificationEngine: ContentVerificationEngine = ContentVerificationEngine(),
    private val staleThresholdHours: Long = 48
) {
    companion object {
        private const val TAG = "ContentRevalidation"
        const val MAX_SOURCE_RETRIES = 3
    }

    private val sourceRetryCounts = ConcurrentHashMap<String, Int>()

    data class RevalidationResult(
        val totalChecked: Int,
        val expiredCount: Int,
        val staleCount: Int,
        val verifiedCount: Int,
        val updatedCount: Int
    )

    /**
     * Executes periodic revalidation cycle on cached or stored items.
     */
    suspend fun revalidateActiveContent(
        items: List<LatestUpdateItem>,
        todayIso: String = SmartDateIntelligence.getTodayIso()
    ): RevalidationResult = withContext(Dispatchers.IO) {
        var expiredCount = 0
        var staleCount = 0
        var verifiedCount = 0
        var updatedCount = 0

        val now = System.currentTimeMillis()
        val staleCutoffMillis = now - (staleThresholdHours * 3600 * 1000)

        for (item in items) {
            // 1. Auto-Expiry Check for Vacancy / Admission
            if (!item.lastDate.isNullOrBlank()) {
                val isExpired = SmartDateIntelligence.isDateExpired(item.lastDate, todayIso)
                if (isExpired && item.isActive) {
                    expiredCount++
                    Log.i(TAG, "Auto-expired item [${item.title}] as lastDate (${item.lastDate}) < today ($todayIso)")
                    // Update in local DB if available
                    try {
                        val local = recruitmentDao?.getItemById(item.id)
                        if (local != null) {
                            recruitmentDao.insertOrUpdate(local.copy(rawStatus = VacancyStatus.CLOSED.name))
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error updating local record to CLOSED: ${e.message}")
                    }
                    continue
                }
            }

            // 2. Stale Content Detection
            val lastVerified = item.updatedAt.takeIf { it > 0 } ?: item.createdAt
            if (lastVerified < staleCutoffMillis) {
                staleCount++
                Log.d(TAG, "Item [${item.title}] marked STALE (last verified: $lastVerified)")
            } else {
                verifiedCount++
            }
        }

        RevalidationResult(
            totalChecked = items.size,
            expiredCount = expiredCount,
            staleCount = staleCount,
            verifiedCount = verifiedCount,
            updatedCount = updatedCount
        )
    }

    /**
     * Handles transient source failures with backoff and retry limit.
     */
    fun handleSourceFailure(sourceId: String, errorMessage: String): Boolean {
        val currentRetries = sourceRetryCounts.getOrDefault(sourceId, 0) + 1
        sourceRetryCounts[sourceId] = currentRetries

        if (currentRetries <= MAX_SOURCE_RETRIES) {
            Log.w(TAG, "Source $sourceId transient failure ($errorMessage). Retry attempt $currentRetries/$MAX_SOURCE_RETRIES.")
            return true // Should retry
        } else {
            Log.e(TAG, "Source $sourceId exceeded max retries. Marking TEMPORARILY_UNAVAILABLE.")
            verificationEngine.metricsTracker.recordSourceFailure(sourceId, errorMessage)
            return false // Stop retrying, mark failed
        }
    }

    /**
     * Resets retry count upon successful fetch.
     */
    fun resetSourceRetries(sourceId: String) {
        sourceRetryCounts.remove(sourceId)
    }
}

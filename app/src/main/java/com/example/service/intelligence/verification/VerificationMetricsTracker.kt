package com.example.service.intelligence.verification

import android.util.Log
import com.example.service.admin.TelegramAdminBotManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Step 84: Quality & Verification Metrics Tracker.
 * 
 * Tracks real-time verification metrics and provides filtered, high-value
 * alerts for Telegram Admin notifications without alert spam.
 */
class VerificationMetricsTracker(
    private val telegramAdminBotManager: TelegramAdminBotManager? = null
) {
    companion object {
        private const val TAG = "VerificationMetrics"
    }

    private val _totalProcessed = AtomicInteger(0)
    private val _published = AtomicInteger(0)
    private val _expired = AtomicInteger(0)
    private val _duplicate = AtomicInteger(0)
    private val _updated = AtomicInteger(0)
    private val _reviewRequired = AtomicInteger(0)
    private val _failed = AtomicInteger(0)
    private val _dataConflicts = AtomicInteger(0)
    private val _categoryConflicts = AtomicInteger(0)
    private val _dateConflicts = AtomicInteger(0)
    private val _sourceFailures = AtomicInteger(0)

    private val _metricsState = MutableStateFlow(VerificationMetrics())
    val metricsState: StateFlow<VerificationMetrics> = _metricsState.asStateFlow()

    fun recordProcessed(status: VerificationStatus) {
        _totalProcessed.incrementAndGet()
        when (status) {
            VerificationStatus.PUBLISHED -> _published.incrementAndGet()
            VerificationStatus.EXPIRED -> _expired.incrementAndGet()
            VerificationStatus.DUPLICATE -> _duplicate.incrementAndGet()
            VerificationStatus.UPDATED -> _updated.incrementAndGet()
            VerificationStatus.REVIEW_REQUIRED -> _reviewRequired.incrementAndGet()
            VerificationStatus.DATA_CONFLICT -> {
                _dataConflicts.incrementAndGet()
                _reviewRequired.incrementAndGet()
            }
            VerificationStatus.CATEGORY_CONFLICT -> {
                _categoryConflicts.incrementAndGet()
                _reviewRequired.incrementAndGet()
            }
            VerificationStatus.DATE_CONFLICT -> {
                _dateConflicts.incrementAndGet()
                _reviewRequired.incrementAndGet()
            }
            VerificationStatus.TEMPORARILY_UNAVAILABLE -> {
                _sourceFailures.incrementAndGet()
            }
            VerificationStatus.FAILED, VerificationStatus.REJECTED -> _failed.incrementAndGet()
            else -> {}
        }
        updateState()
    }

    fun recordSourceFailure(sourceName: String, error: String) {
        _sourceFailures.incrementAndGet()
        updateState()
        // Send high-value alert to Telegram
        sendHighValueAlert("CRITICAL SOURCE FAILURE", "Source: $sourceName\nError: $error")
    }

    fun recordCriticalConflict(title: String, conflictDescription: String) {
        _dataConflicts.incrementAndGet()
        updateState()
        sendHighValueAlert("DATA CONFLICT FLAGGED", "Item: $title\nDetails: $conflictDescription")
    }

    private fun updateState() {
        _metricsState.value = VerificationMetrics(
            totalProcessed = _totalProcessed.get(),
            publishedCount = _published.get(),
            expiredCount = _expired.get(),
            duplicateCount = _duplicate.get(),
            updatedCount = _updated.get(),
            reviewRequiredCount = _reviewRequired.get(),
            failedCount = _failed.get(),
            dataConflictsCount = _dataConflicts.get(),
            categoryConflictsCount = _categoryConflicts.get(),
            dateConflictsCount = _dateConflicts.get(),
            sourceFailuresCount = _sourceFailures.get(),
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }

    private fun sendHighValueAlert(title: String, body: String) {
        try {
            telegramAdminBotManager?.notifySecurityAlert(
                title = "[STUDYMATE QUALITY ALERT] $title",
                details = body,
                severity = "HIGH"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to route alert to Telegram: ${e.message}")
        }
    }

    fun getMetrics(): VerificationMetrics = _metricsState.value

    fun resetMetrics() {
        _totalProcessed.set(0)
        _published.set(0)
        _expired.set(0)
        _duplicate.set(0)
        _updated.set(0)
        _reviewRequired.set(0)
        _failed.set(0)
        _dataConflicts.set(0)
        _categoryConflicts.set(0)
        _dateConflicts.set(0)
        _sourceFailures.set(0)
        updateState()
    }
}

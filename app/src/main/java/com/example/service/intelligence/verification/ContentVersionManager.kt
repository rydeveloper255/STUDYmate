package com.example.service.intelligence.verification

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Step 84: Content Versioning & Correction Notice Manager.
 * 
 * Maintains an immutable audit trail of record updates, date extensions,
 * and official correction notices:
 * - Version number tracking per recruitment record
 * - Field-level diffs (last_date, exam_date, application_url, status)
 * - Correction notice linking to existing records
 */
class ContentVersionManager {

    companion object {
        private const val TAG = "ContentVersionManager"
    }

    private val versionHistory = ConcurrentHashMap<String, MutableList<ContentVersionRecord>>()

    /**
     * Records a new version update for a record.
     */
    fun recordVersion(
        recordId: String,
        fieldName: String,
        previousValue: String?,
        newValue: String?,
        changeSource: String,
        changeSummary: String
    ): ContentVersionRecord {
        val historyList = versionHistory.computeIfAbsent(recordId) { mutableListOf() }
        val nextVersionNumber = historyList.size + 1

        val record = ContentVersionRecord(
            recordId = recordId,
            versionNumber = nextVersionNumber,
            fieldName = fieldName,
            previousValue = previousValue,
            newValue = newValue,
            changeSource = changeSource,
            changeSummary = changeSummary,
            changedAt = System.currentTimeMillis()
        )

        historyList.add(record)
        Log.i(TAG, "Recorded version $nextVersionNumber for record $recordId: $fieldName changed from '$previousValue' to '$newValue' ($changeSummary)")
        return record
    }

    /**
     * Returns full version history for a given record.
     */
    fun getVersionHistory(recordId: String): List<ContentVersionRecord> {
        return versionHistory[recordId]?.toList() ?: emptyList()
    }

    /**
     * Checks if a post is an official correction notice for a recruitment.
     */
    fun isCorrectionNotice(text: String, title: String): Boolean {
        val combined = "$title $text".lowercase()
        return combined.contains("correction notice") ||
               combined.contains("corrigendum") ||
               combined.contains("शुद्धिपत्र") ||
               combined.contains("संशोधन सूचना") ||
               combined.contains("amendment notification")
    }

    /**
     * Clears history (for testing).
     */
    fun clearHistory() {
        versionHistory.clear()
    }
}

package com.example.service.intelligence.smart

import com.example.data.model.updates.LatestUpdateItem
import java.security.MessageDigest

/**
 * Step 83: Smart Multi-Level Duplicate & Update Detector.
 * 
 * Evaluates candidate content against existing records across 5 distinct levels:
 * - Level 1: Exact source message ID match
 * - Level 2: Exact Official / Apply / PDF URL match
 * - Level 3: Normalized Title + Organization match
 * - Level 4: Title + Organization + Key Event Date match
 * - Level 5: SHA-256 Content Fingerprint match
 * 
 * Crucially distinguishes EXACT DUPLICATES (discard/skip without redundant write)
 * from UPDATED CONTENT (e.g., Last Date Extended, Exam Date announced, new PDF link).
 */
object SmartDuplicateDetector {

    /**
     * Checks if new item matches any existing record, and if it represents a meaningful update.
     */
    fun evaluate(
        newItem: SmartProcessedItem,
        existingRecords: List<LatestUpdateItem>
    ): SmartDuplicateResult {
        if (existingRecords.isEmpty()) {
            return SmartDuplicateResult(isDuplicate = false, isUpdate = false)
        }

        // Level 1: Exact Source Message ID
        if (!newItem.sourceMessageId.isNullOrBlank()) {
            val match = existingRecords.find { it.externalId == newItem.sourceMessageId }
            if (match != null) {
                return analyzeChanges(newItem, match, SmartDuplicateLevel.EXACT_MESSAGE_ID)
            }
        }

        // Level 2: Exact Canonical / Official / Apply URL
        val newPrimaryUrl = newItem.extractedData.primaryUrl
        if (!newPrimaryUrl.isNullOrBlank()) {
            val match = existingRecords.find { 
                it.applyUrl == newPrimaryUrl || it.sourceUrl == newPrimaryUrl || it.downloadUrl == newPrimaryUrl 
            }
            if (match != null) {
                return analyzeChanges(newItem, match, SmartDuplicateLevel.EXACT_URL)
            }
        }

        // Level 3: Normalized Title + Organization Match
        val newNormTitle = newItem.normalizedTitle
        val newOrg = newItem.extractedData.organization
        if (newNormTitle.isNotBlank() && !newOrg.isNullOrBlank()) {
            val match = existingRecords.find { existing ->
                val existingNormTitle = SmartTitleNormalizer.normalizeTitle(existing.title)
                val orgMatch = existing.organization.equals(newOrg, ignoreCase = true) ||
                               existing.examName.equals(newOrg, ignoreCase = true)
                orgMatch && (existingNormTitle == newNormTitle || SmartTitleNormalizer.calculateTitleSimilarity(existing.title, newItem.extractedData.title) >= 0.85f)
            }
            if (match != null) {
                return analyzeChanges(newItem, match, SmartDuplicateLevel.NORMALIZED_TITLE_ORG)
            }
        }

        // Level 4: Title + Important Date
        val newKeyDate = newItem.lastDate ?: newItem.examDate ?: newItem.postDate
        if (newNormTitle.isNotBlank() && newKeyDate != null) {
            val match = existingRecords.find { existing ->
                val existingDate = existing.lastDate ?: existing.examDate ?: existing.publishedDate
                val existingNormTitle = SmartTitleNormalizer.normalizeTitle(existing.title)
                existingDate == newKeyDate && SmartTitleNormalizer.calculateTitleSimilarity(existing.title, newItem.extractedData.title) >= 0.80f
            }
            if (match != null) {
                return analyzeChanges(newItem, match, SmartDuplicateLevel.TITLE_ORG_DATE)
            }
        }

        // Level 5: Content Hash
        if (newItem.contentHash.isNotBlank()) {
            val match = existingRecords.find { it.contentHash == newItem.contentHash }
            if (match != null) {
                return SmartDuplicateResult(
                    isDuplicate = true,
                    isUpdate = false,
                    duplicateLevel = SmartDuplicateLevel.CONTENT_HASH,
                    existingRecordId = match.id
                )
            }
        }

        return SmartDuplicateResult(isDuplicate = false, isUpdate = false)
    }

    /**
     * Determines whether the matching record is an exact duplicate or contains updated fields.
     */
    private fun analyzeChanges(
        newItem: SmartProcessedItem,
        existing: LatestUpdateItem,
        level: SmartDuplicateLevel
    ): SmartDuplicateResult {
        val updatedFields = mutableListOf<String>()

        // Check for Last Date changes (e.g. extension)
        if (!newItem.lastDate.isNullOrBlank() && newItem.lastDate != existing.lastDate) {
            updatedFields.add("last_date (from ${existing.lastDate ?: "none"} to ${newItem.lastDate})")
        }

        // Check for Exam Date announcement/change
        if (!newItem.examDate.isNullOrBlank() && newItem.examDate != existing.examDate) {
            updatedFields.add("exam_date (from ${existing.examDate ?: "none"} to ${newItem.examDate})")
        }

        // Check for Apply / Download URL addition
        val newApply = (newItem.extractedData as? SmartExtractedData.Vacancy)?.data?.applyUrl
        if (!newApply.isNullOrBlank() && newApply != existing.applyUrl) {
            updatedFields.add("apply_url")
        }

        val newPdf = newItem.pdfUrl
        if (!newPdf.isNullOrBlank() && newPdf != existing.downloadUrl) {
            updatedFields.add("download_url")
        }

        return if (updatedFields.isNotEmpty()) {
            SmartDuplicateResult(
                isDuplicate = true,
                isUpdate = true,
                duplicateLevel = level,
                existingRecordId = existing.id,
                updatedFields = updatedFields,
                updateSummary = "Updated fields: ${updatedFields.joinToString(", ")}"
            )
        } else {
            SmartDuplicateResult(
                isDuplicate = true,
                isUpdate = false,
                duplicateLevel = level,
                existingRecordId = existing.id
            )
        }
    }

    /**
     * Computes deterministic SHA-256 fingerprint from canonical attributes.
     */
    fun computeContentHash(
        title: String,
        category: String,
        org: String?,
        date: String?,
        primaryUrl: String?
    ): String {
        val raw = "${title.trim().lowercase()}|${category.trim().lowercase()}|${org?.trim()?.lowercase() ?: ""}|${date ?: ""}|${primaryUrl ?: ""}"
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(raw.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            raw.hashCode().toString()
        }
    }
}

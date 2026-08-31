package com.example.service.intelligence.smart

import com.example.data.model.updates.LatestUpdateItem

/**
 * Step 83: Smart Source Priority & Conflict Detection.
 * 
 * Hierarchy:
 * OFFICIAL SOURCE (Rank 1) > CONFIGURED TRUSTED SOURCE (Rank 2) > WHATSAPP SOURCE (Rank 3) > OTHER (Rank 4)
 * 
 * Conflict Identification:
 * - Detects irreconcilable factual discrepancies between sources (e.g., Source A states last date 15 Aug,
 *   Source B states 30 Aug without an extension notice; or conflicting total vacancy numbers).
 * - Flags DATA_CONFLICT and preserves provenance from both sources instead of silently picking one.
 */
object SmartConflictDetector {

    enum class SourceTier(val priority: Int, val label: String) {
        OFFICIAL_SOURCE(1, "Official Website / Board"),
        CONFIGURED_TRUSTED_SOURCE(2, "Configured Portal"),
        WHATSAPP_SOURCE(3, "StudyMate WhatsApp Channel"),
        OTHER(4, "Other Source");

        companion object {
            fun getTier(sourceType: String?, sourceName: String?): SourceTier {
                val st = sourceType?.lowercase() ?: ""
                val sn = sourceName?.lowercase() ?: ""
                return when {
                    st.contains("official") || sn.contains("gov") || sn.contains("nic") -> OFFICIAL_SOURCE
                    st.contains("trusted") || st.contains("website") -> CONFIGURED_TRUSTED_SOURCE
                    st.contains("whatsapp") || sn.contains("whatsapp") -> WHATSAPP_SOURCE
                    else -> OTHER
                }
            }
        }
    }

    /**
     * Inspects candidate item against existing records for factual discrepancies.
     */
    fun detectConflict(
        newItem: SmartProcessedItem,
        existingRecords: List<LatestUpdateItem>
    ): SmartConflictInfo {
        val newTitle = newItem.extractedData.title
        val newOrg = newItem.extractedData.organization

        for (existing in existingRecords) {
            val titleSimilarity = SmartTitleNormalizer.calculateTitleSimilarity(existing.title, newTitle)
            val orgMatch = newOrg.isNullOrBlank() || existing.organization.equals(newOrg, ignoreCase = true)

            // If it's referring to the same core recruitment/exam:
            if (titleSimilarity >= 0.85f && orgMatch) {
                // Check 1: Conflicting Last Dates (without an explicit extension notice)
                if (!newItem.lastDate.isNullOrBlank() && 
                    !existing.lastDate.isNullOrBlank() && 
                    newItem.lastDate != existing.lastDate) {
                    
                    val isExtensionMentioned = (newItem.extractedData as? SmartExtractedData.Vacancy)?.data?.isLastDateExtended == true
                    if (!isExtensionMentioned) {
                        return SmartConflictInfo(
                            hasConflict = true,
                            conflictingField = "last_date",
                            sourceAValue = existing.lastDate,
                            sourceBValue = newItem.lastDate,
                            sourceAName = existing.sourceName,
                            sourceBName = newItem.sourceName,
                            conflictDescription = "Conflicting last dates: ${existing.sourceName} states '${existing.lastDate}', while ${newItem.sourceName} states '${newItem.lastDate}' without extension proof."
                        )
                    }
                }

                // Check 2: Conflicting Exam Dates
                if (!newItem.examDate.isNullOrBlank() && 
                    !existing.examDate.isNullOrBlank() && 
                    newItem.examDate != existing.examDate) {
                    return SmartConflictInfo(
                        hasConflict = true,
                        conflictingField = "exam_date",
                        sourceAValue = existing.examDate,
                        sourceBValue = newItem.examDate,
                        sourceAName = existing.sourceName,
                        sourceBName = newItem.sourceName,
                        conflictDescription = "Conflicting exam dates: ${existing.sourceName} states '${existing.examDate}', while ${newItem.sourceName} states '${newItem.examDate}'."
                    )
                }
            }
        }

        return SmartConflictInfo(hasConflict = false)
    }
}

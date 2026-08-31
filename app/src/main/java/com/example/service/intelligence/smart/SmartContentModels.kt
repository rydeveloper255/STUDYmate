package com.example.service.intelligence.smart

import com.example.data.model.updates.UpdateCategory
import java.util.UUID

/**
 * Step 83: Smart Content Intelligence Layer Domain Models.
 * 
 * Core structural representations for understanding, classifying, extracting,
 * validating, scoring, and deduplicating recruitment, results, admit cards, answer keys,
 * and admissions from WhatsApp Channel and existing website sources.
 */

enum class SmartContentCategory(val key: String, val displayName: String, val updateCategory: UpdateCategory) {
    VACANCY("vacancy", "Vacancy", UpdateCategory.VACANCY),
    RESULT("result", "Result", UpdateCategory.RESULT),
    ADMIT_CARD("admit_card", "Admit Card", UpdateCategory.ADMIT_CARD),
    ANSWER_KEY("answer_key", "Answer Key", UpdateCategory.ANSWER_KEY),
    ADMISSION("admission", "Admission", UpdateCategory.ADMISSION),
    OTHER("other", "Other", UpdateCategory.VACANCY);

    companion object {
        fun fromKey(key: String?): SmartContentCategory {
            if (key == null) return OTHER
            val clean = key.trim().lowercase()
            return entries.find { it.key == clean } ?: OTHER
        }
    }
}

enum class SmartConfidenceLevel {
    HIGH,     // >= 0.85: Eligible for automatic processing
    MEDIUM,   // 0.60 .. 0.84: Additional validation required
    LOW       // < 0.60: Routed to REVIEW_REQUIRED
}

data class SmartConfidenceScore(
    val score: Float,
    val level: SmartConfidenceLevel,
    val signals: List<String> = emptyList(),
    val reason: String = ""
)

enum class SmartLinkType(val label: String) {
    OFFICIAL("Official Website"),
    APPLY("Apply Online"),
    RESULT("Result & Scorecard"),
    ADMIT_CARD("Admit Card"),
    ANSWER_KEY("Answer Key"),
    ADMISSION("Admission / Counseling"),
    PDF("Official Notification PDF"),
    OTHER("Direct Link")
}

data class SmartLink(
    val url: String,
    val linkType: SmartLinkType,
    val displayText: String,
    val isVerifiedPdf: Boolean = false
)

/**
 * Category-Specific Structured Extraction Models (Zero Fake Data Invariant: missing = null)
 */

data class SmartVacancyData(
    val title: String,
    val organization: String? = null,
    val postName: String? = null,
    val vacancyCount: Int? = null,
    val qualification: String? = null,
    val ageLimit: String? = null,
    val applicationStartDate: String? = null,
    val lastDate: String? = null,
    val examDate: String? = null,
    val applicationFee: String? = null,
    val officialNotificationUrl: String? = null,
    val applyUrl: String? = null,
    val sourceUrl: String? = null,
    val sourcePostDate: String? = null,
    val isLastDateExtended: Boolean = false,
    val originalLastDate: String? = null
)

data class SmartResultData(
    val title: String,
    val organization: String? = null,
    val examName: String? = null,
    val resultDate: String? = null,
    val examDate: String? = null,
    val resultUrl: String? = null,
    val officialUrl: String? = null,
    val sourceUrl: String? = null,
    val sourcePostDate: String? = null
)

data class SmartAdmitCardData(
    val title: String,
    val organization: String? = null,
    val examName: String? = null,
    val examDate: String? = null,
    val admitCardReleaseDate: String? = null,
    val downloadUrl: String? = null,
    val officialUrl: String? = null,
    val sourceUrl: String? = null,
    val sourcePostDate: String? = null
)

data class SmartAnswerKeyData(
    val title: String,
    val organization: String? = null,
    val examName: String? = null,
    val answerKeyDate: String? = null,
    val examDate: String? = null,
    val answerKeyUrl: String? = null,
    val officialUrl: String? = null,
    val sourceUrl: String? = null,
    val sourcePostDate: String? = null
)

data class SmartAdmissionData(
    val title: String,
    val institution: String? = null,
    val course: String? = null,
    val qualification: String? = null,
    val applicationStartDate: String? = null,
    val lastDate: String? = null,
    val admissionDate: String? = null,
    val applicationUrl: String? = null,
    val officialUrl: String? = null,
    val sourceUrl: String? = null,
    val sourcePostDate: String? = null
)

data class SmartOtherData(
    val title: String,
    val organization: String? = null,
    val noticeDate: String? = null,
    val officialUrl: String? = null,
    val sourceUrl: String? = null,
    val sourcePostDate: String? = null
)

sealed class SmartExtractedData {
    data class Vacancy(val data: SmartVacancyData) : SmartExtractedData()
    data class Result(val data: SmartResultData) : SmartExtractedData()
    data class AdmitCard(val data: SmartAdmitCardData) : SmartExtractedData()
    data class AnswerKey(val data: SmartAnswerKeyData) : SmartExtractedData()
    data class Admission(val data: SmartAdmissionData) : SmartExtractedData()
    data class Other(val data: SmartOtherData) : SmartExtractedData()

    val title: String
        get() = when (this) {
            is Vacancy -> data.title
            is Result -> data.title
            is AdmitCard -> data.title
            is AnswerKey -> data.title
            is Admission -> data.title
            is Other -> data.title
        }

    val organization: String?
        get() = when (this) {
            is Vacancy -> data.organization
            is Result -> data.organization
            is AdmitCard -> data.organization
            is AnswerKey -> data.organization
            is Admission -> data.institution
            is Other -> data.organization
        }

    val primaryUrl: String?
        get() = when (this) {
            is Vacancy -> data.applyUrl ?: data.officialNotificationUrl
            is Result -> data.resultUrl ?: data.officialUrl
            is AdmitCard -> data.downloadUrl ?: data.officialUrl
            is AnswerKey -> data.answerKeyUrl ?: data.officialUrl
            is Admission -> data.applicationUrl ?: data.officialUrl
            is Other -> data.officialUrl
        }
}

/**
 * Quality Score Breakdown (0 to 100)
 */
data class SmartQualityScore(
    val totalScore: Int,
    val titleScore: Int,
    val categoryConfidenceScore: Int,
    val sourceDateScore: Int,
    val importantDatesScore: Int,
    val verifiedLinksScore: Int,
    val organizationScore: Int,
    val integrityBonus: Int,
    val isEligibleForAutoPublish: Boolean,
    val penaltyReasons: List<String> = emptyList()
)

enum class SmartDuplicateLevel {
    NONE,
    EXACT_MESSAGE_ID,
    EXACT_URL,
    NORMALIZED_TITLE_ORG,
    TITLE_ORG_DATE,
    CONTENT_HASH
}

data class SmartDuplicateResult(
    val isDuplicate: Boolean,
    val isUpdate: Boolean,
    val duplicateLevel: SmartDuplicateLevel = SmartDuplicateLevel.NONE,
    val existingRecordId: String? = null,
    val updatedFields: List<String> = emptyList(),
    val updateSummary: String? = null
)

data class SmartConflictInfo(
    val hasConflict: Boolean,
    val conflictingField: String? = null,
    val sourceAValue: String? = null,
    val sourceBValue: String? = null,
    val sourceAName: String? = null,
    val sourceBName: String? = null,
    val conflictDescription: String? = null
)

enum class SmartProcessingStatus {
    PROCESSING,
    PUBLISHED,
    EXPIRED,
    UPDATED,
    DUPLICATE,
    REVIEW_REQUIRED,
    DATA_CONFLICT,
    CATEGORY_CONFLICT,
    DATE_CONFLICT,
    IGNORED_BEFORE_CUTOFF,
    DATE_UNAVAILABLE,
    SOURCE_UNAVAILABLE,
    FAILED
}

enum class SmartPipelineStage {
    SOURCE_RECEIVED,
    DATE_FILTER_CHECKED,
    TEXT_CLEANED,
    CONTENT_DETECTED,
    CATEGORY_CLASSIFIED,
    FIELDS_EXTRACTED,
    DATE_NORMALIZED_VALIDATED,
    EXPIRY_CHECKED,
    DUPLICATE_CHECKED,
    QUALITY_SCORED,
    DATABASE_SAVED,
    PUBLISHED
}

data class SmartPipelineStageLog(
    val stage: SmartPipelineStage,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: Map<String, String> = emptyMap()
)

data class SmartProcessedItem(
    val id: String = UUID.randomUUID().toString(),
    val sourceMessageId: String? = null,
    val rawContent: String,
    val cleanedContent: String,
    val category: SmartContentCategory,
    val confidence: SmartConfidenceScore,
    val extractedData: SmartExtractedData,
    val links: List<SmartLink> = emptyList(),
    val pdfUrl: String? = null,
    val normalizedTitle: String,
    val qualityScore: SmartQualityScore,
    val duplicateResult: SmartDuplicateResult,
    val conflictInfo: SmartConflictInfo? = null,
    val aiSummary: String,
    val sourceUrl: String,
    val sourceName: String,
    val sourceType: String,
    val postDate: String? = null,
    val lastDate: String? = null,
    val examDate: String? = null,
    val isExpired: Boolean = false,
    val isActive: Boolean = true,
    val status: SmartProcessingStatus,
    val stageLogs: List<SmartPipelineStageLog> = emptyList(),
    val contentHash: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

package com.example.data.model.content

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Supported content categories for StudyMate automated collection.
 */
enum class ContentCategory(val displayName: String, val badge: String) {
    VACANCY("Vacancy", "🟢"),
    RESULT("Result", "🏆"),
    ADMIT_CARD("Admit Card", "🎫"),
    ANSWER_KEY("Answer Key", "🔑"),
    ADMISSION("Admission", "🎓"),
    CURRENT_AFFAIRS("Current Affairs", "📰"),
    CURRENT_AFFAIRS_PDF("Weekly CA PDF", "📑"),
    IMPORTANT_UPDATE("Important Update", "⚡"),
    EXTERNAL_SOURCE("External Source (Ref)", "🔗")
}

/**
 * Type of content source.
 */
enum class SourceType {
    WEB_SCRAPER,
    PDF_DIRECTORY,
    RSS_FEED,
    OFFICIAL_PORTAL,
    REFERENCE_ONLY,
    WHATSAPP_CHANNEL
}

/**
 * Health / operational status of a content source.
 */
enum class SourceStatus {
    ACTIVE,
    UNHEALTHY,
    DISABLED,
    RATE_LIMITED
}

/**
 * Real-time computed status intelligence based on source-backed verified dates.
 */
enum class ContentStatusIntelligence(val label: String, val badgeColorHex: String, val hindiLabel: String) {
    NEW("New", "#10B981", "नया"),
    UPDATED("Updated", "#3B82F6", "अपडेट हुआ"),
    OPEN("Open", "#10B981", "आवेदन चालू"),
    CLOSING_SOON("Closing Soon", "#F97316", "जल्द समाप्त"),
    CLOSED("Closed", "#6B7280", "समाप्त"),
    EXAM_SOON("Exam Soon", "#8B5CF6", "परीक्षा निकट"),
    ADMIT_CARD_OUT("Admit Card Out", "#06B6D4", "एडमिट कार्ड जारी"),
    RESULT_OUT("Result Out", "#10B981", "रिजल्ट घोषित"),
    ANSWER_KEY_OUT("Answer Key Out", "#F59E0B", "उत्तर कुंजी जारी")
}

/**
 * Confidence & Review processing status for the AI verification layer.
 */
enum class ContentProcessingStatus {
    AUTO_APPROVED,
    REVIEW_REQUIRED,
    REJECTED,
    PENDING
}

/**
 * Standard lifecycle states for every content piece.
 */
enum class ContentLifecycleStage {
    DISCOVERED,
    COLLECTED,
    NORMALIZED,
    AI_PROCESSED,
    VALIDATED,
    APPROVED,
    PUBLISHED,
    MONITORED
}

/**
 * Result of smart difference & duplicate detection.
 */
enum class DetectionResultType {
    NEW,
    UPDATED,
    UNCHANGED,
    DUPLICATE
}

/**
 * Central configuration model for a content source.
 */
@Entity(tableName = "content_sources")
data class ContentSourceConfig(
    @PrimaryKey val sourceId: String,
    val sourceName: String,
    val sourceUrl: String,
    val category: ContentCategory,
    val enabled: Boolean = true,
    val priority: Int = 1,
    val lastCheckedAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val lastError: String? = null,
    val sourceType: SourceType = SourceType.WEB_SCRAPER,
    val language: String = "Hindi",
    val description: String = "",
    val checkIntervalMinutes: Long = 180L,
    val status: SourceStatus = SourceStatus.ACTIVE,
    val consecutiveFailures: Int = 0,
    val itemsDiscoveredCount: Int = 0
)

/**
 * Content Source Reference Entity mapping a canonical content_item to multiple source URLs.
 */
@Entity(tableName = "content_source_references")
data class ContentSourceReferenceEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val contentId: String,
    val sourceId: String,
    val sourceUrl: String,
    val isPrimary: Boolean = false,
    val discoveredAt: Long = System.currentTimeMillis()
)

/**
 * Normalized content item collected from any source.
 */
data class CollectedContentItem(
    val id: String,
    val title: String,
    val originalTitle: String,
    val category: ContentCategory,
    val sourceName: String,
    val sourceUrl: String,
    val canonicalUrl: String,
    val publishedDate: String = "",
    val dateRange: String? = null,
    val organization: String? = null,
    val postName: String? = null,
    val lastDate: String? = null,
    val examDate: String? = null,
    val eligibility: String? = null,
    val totalPosts: String? = null,
    val applicationFee: String? = null,
    val ageLimit: String? = null,
    val selectionProcess: String? = null,
    val officialLink: String? = null,
    val pdfUrl: String? = null,
    val summary: String = "",
    val contentFingerprint: String = "",
    val isVerified: Boolean = true,
    val isTelegramPublished: Boolean = false,
    val telegramMessageId: Long? = null,
    val detectedAt: Long = System.currentTimeMillis(),
    val storagePath: String? = null,
    val fileSize: String? = null,
    val rawSnippet: String? = null,
    val processingStatus: ContentProcessingStatus = ContentProcessingStatus.AUTO_APPROVED,
    val statusIntelligence: ContentStatusIntelligence = ContentStatusIntelligence.NEW,
    val additionalSources: List<String> = emptyList(),
    val isUpdated: Boolean = false,
    val updateHistoryNote: String = ""
)

/**
 * Model specifically representing a detected Weekly Current Affairs Hindi PDF.
 */
data class WeeklyCurrentAffairsPdf(
    val id: String,
    val title: String,
    val description: String = "साप्ताहिक करेंट अफेयर्स पीडीएफ संग्रह (UPSC, SSC, Railway, Banking, State Exams)",
    val dateRange: String = "",
    val category: ContentCategory = ContentCategory.CURRENT_AFFAIRS_PDF,
    val language: String = "Hindi",
    val sourceName: String = "GK Now Hindi",
    val sourcePageUrl: String = "https://gknow.in/hi/weekly-current-affairs-pdf-in-hindi/",
    val pdfSourceUrl: String = "",
    val storagePath: String? = null,
    val pdfPublicUrl: String? = null,
    val publishedAt: String = "",
    val detectedAt: Long = System.currentTimeMillis(),
    val fileSize: String? = null,
    val contentHash: String = "",
    val isAvailable: Boolean = true,
    val status: String = "AVAILABLE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Raw Source Record Room Entity (Stored before AI processing for full traceability).
 */
@Entity(tableName = "raw_source_records")
data class RawSourceRecordEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val sourceName: String,
    val sourceUrl: String,
    val discoveredUrl: String,
    val rawTitle: String,
    val rawText: String,
    val discoveredAt: Long = System.currentTimeMillis(),
    val publishedAt: String = "",
    val updatedAt: String = "",
    val contentHash: String,
    val contentType: String,
    val language: String = "Hindi",
    val categoryHint: String = ""
)

/**
 * Content Version Entity (Tracks historical field changes like last date extended, exam date shifted, etc.).
 */
@Entity(tableName = "content_versions")
data class ContentVersionEntity(
    @PrimaryKey val versionId: String,
    val contentId: String,
    val versionNumber: Int,
    val changedFieldsJson: String, // e.g. {"applicationLastDate": {"old": "10 Sep", "new": "15 Sep"}}
    val changeSummary: String,
    val recordedAt: Long = System.currentTimeMillis(),
    val sourceAttribution: String = ""
)

/**
 * Review Queue Entity (Holds items requiring human review before publication).
 */
@Entity(tableName = "content_review_queue")
data class ReviewQueueItemEntity(
    @PrimaryKey val id: String,
    val contentId: String,
    val title: String,
    val category: String,
    val sourceName: String,
    val originalLink: String,
    val extractedFieldsJson: String,
    val aiSummary: String,
    val detectedChanges: String,
    val reviewReason: String,
    val status: String = "REVIEW_REQUIRED", // "REVIEW_REQUIRED", "APPROVED", "REJECTED", "EDITED"
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewerNotes: String = ""
)

/**
 * AI Processing Log Entity for auditing AI cost, performance, and optimization.
 */
@Entity(tableName = "ai_processing_logs")
data class AiProcessingLogEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val contentId: String,
    val title: String,
    val processingType: String, // e.g. "EXTRACTION", "NORMALIZATION", "CHANGE_DETECTION"
    val modelName: String = "gemini-2.5-flash",
    val tokensUsed: Int = 0,
    val durationMs: Long = 0L,
    val status: String = "SUCCESS", // "SUCCESS", "SKIPPED_UNCHANGED", "FAILED"
    val inputFingerprint: String = "",
    val detectedChanges: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Telegram Publication Entity for tracking dispatched updates and preventing duplicate broadcasts.
 */
@Entity(tableName = "telegram_publications")
data class TelegramPublicationEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val contentId: String,
    val title: String,
    val category: String,
    val chatId: String,
    val messageId: Long? = null,
    val publishedAt: Long = System.currentTimeMillis(),
    val status: String = "PUBLISHED", // "PUBLISHED", "FAILED", "SUPPRESSED"
    val contentHash: String = ""
)

/**
 * Persistent Job Log Entity for 3-hour automated and manual runs.
 */
@Entity(tableName = "content_collection_jobs")
data class ContentCollectionJobLogEntity(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val startedAt: Long,
    val completedAt: Long,
    val status: String, // "SUCCESS", "PARTIAL_SUCCESS", "FAILED"
    val sourcesChecked: Int,
    val newItems: Int,
    val updatedItems: Int,
    val duplicateItems: Int,
    val failedSources: Int,
    val telegramPosts: Int,
    val pdfsDetected: Int,
    val aiProcessed: Int,
    val reviewRequired: Int,
    val errorsJson: String = "[]",
    val summary: String = ""
)

/**
 * In-memory execution log for UI presentation.
 */
data class ContentCollectionJobLog(
    val id: Long = System.currentTimeMillis(),
    val startedAt: Long,
    val completedAt: Long,
    val status: String, // "SUCCESS", "PARTIAL_SUCCESS", "FAILED"
    val sourcesChecked: Int,
    val newItems: Int,
    val updatedItems: Int = 0,
    val duplicateItems: Int,
    val failedSources: Int,
    val telegramPosts: Int,
    val pdfsDetected: Int,
    val aiProcessed: Int = 0,
    val reviewRequired: Int = 0,
    val errors: List<String> = emptyList(),
    val summary: String = ""
)

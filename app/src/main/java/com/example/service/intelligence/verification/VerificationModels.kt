package com.example.service.intelligence.verification

import com.example.service.intelligence.smart.SmartConfidenceScore
import com.example.service.intelligence.smart.SmartContentCategory
import com.example.service.intelligence.smart.SmartExtractedData
import com.example.service.intelligence.smart.SmartLink
import com.example.service.intelligence.smart.SmartQualityScore
import java.util.UUID

/**
 * Step 84: Advanced Content Verification & Quality Control Domain Models.
 * 
 * Defines comprehensive models for two-layer validation, provenance tracking,
 * link health, review queues, versioning, revalidation, and quality metrics.
 */

enum class VerificationStatus {
    VERIFIED,
    PUBLISHED,
    REVIEW_REQUIRED,
    CATEGORY_CONFLICT,
    DATE_CONFLICT,
    DATA_CONFLICT,
    EXPIRED,
    UPDATED,
    DUPLICATE,
    STALE,
    TEMPORARILY_UNAVAILABLE,
    REJECTED,
    FAILED,
    UNKNOWN_EXPIRY
}

enum class ValidationLayer {
    DETERMINISTIC,  // Layer 1: Date format, calendar bounds, safe URL schemes, required fields, constraints
    INTELLIGENT     // Layer 2: Category meaning, vacancy verification, cross-source conflict, hallucination check
}

enum class ReviewReason(val description: String) {
    CATEGORY_CONFLICT("Category assigned does not match source content signals"),
    DATE_CONFLICT("Chronological or factual date conflict detected (e.g. start date > last date)"),
    IMPOSSIBLE_DATE("Invalid calendar date detected (e.g. 32 August, 31 February)"),
    LINK_UNCERTAIN("URL destination domain is uncertain or belongs to unknown host"),
    LINK_UNSAFE("URL scheme is dangerous or blocked (e.g. javascript:, data:, intent:)"),
    LOW_CONFIDENCE("Overall classification or extraction confidence is below safe threshold"),
    MISSING_CRITICAL_DATA("Mandatory required field for this category is missing"),
    DUPLICATE_UNCERTAIN("Ambiguous duplicate or unresolved update state"),
    SOURCE_UNAVAILABLE("Source connection or endpoint is temporarily unavailable"),
    INVALID_CONTENT("Malformed, blank, or unrecognizable content structure"),
    POTENTIAL_HALLUCINATION("Extracted critical fact cannot be verified in original source text"),
    ORGANIZATION_UNCERTAIN("Organization name could not be reliably established from source"),
    UNKNOWN_EXPIRY("Vacancy/Admission has no last date and cannot be verified active")
}

enum class LinkHealthStatus {
    ACTIVE,
    BROKEN,
    REDIRECTED,
    UNAVAILABLE,
    BLOCKED_SCHEME,
    UNKNOWN
}

data class LinkVerificationResult(
    val originalUrl: String,
    val finalUrl: String,
    val status: LinkHealthStatus,
    val isHttps: Boolean,
    val domain: String?,
    val isOfficialCandidate: Boolean,
    val isSafeScheme: Boolean,
    val riskNotes: List<String> = emptyList()
)

data class ContentProvenance(
    val sourceType: String,
    val sourceName: String,
    val sourceUrl: String,
    val sourceMessageId: String? = null,
    val sourcePostDate: String? = null,
    val fetchedAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long = System.currentTimeMillis(),
    val verificationCount: Int = 1
)

data class ReviewQueueItem(
    val id: String = UUID.randomUUID().toString(),
    val recordId: String,
    val title: String,
    val category: SmartContentCategory,
    val sourceName: String,
    val sourceUrl: String,
    val sourcePostDate: String?,
    val reasons: List<ReviewReason>,
    val reasonDescription: String,
    val rawContent: String,
    val extractedValues: Map<String, String>,
    val conflictingValues: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null,
    val resolvedAt: Long? = null
)

data class ContentVersionRecord(
    val id: String = UUID.randomUUID().toString(),
    val recordId: String,
    val versionNumber: Int,
    val fieldName: String,
    val previousValue: String?,
    val newValue: String?,
    val changeSource: String,
    val changeSummary: String,
    val changedAt: Long = System.currentTimeMillis()
)

data class LayerValidationResult(
    val layer: ValidationLayer,
    val passed: Boolean,
    val issues: List<String> = emptyList(),
    val flaggedReasons: List<ReviewReason> = emptyList()
)

data class VerificationReport(
    val id: String,
    val overallStatus: VerificationStatus,
    val isEligibleForAutoPublish: Boolean,
    val layer1Result: LayerValidationResult,
    val layer2Result: LayerValidationResult,
    val linkVerificationResults: List<LinkVerificationResult> = emptyList(),
    val qualityScore: SmartQualityScore,
    val provenance: ContentProvenance,
    val reviewReasons: List<ReviewReason> = emptyList(),
    val notes: List<String> = emptyList(),
    val verifiedAt: Long = System.currentTimeMillis()
)

data class VerificationMetrics(
    val totalProcessed: Int = 0,
    val publishedCount: Int = 0,
    val expiredCount: Int = 0,
    val duplicateCount: Int = 0,
    val updatedCount: Int = 0,
    val reviewRequiredCount: Int = 0,
    val failedCount: Int = 0,
    val dataConflictsCount: Int = 0,
    val categoryConflictsCount: Int = 0,
    val dateConflictsCount: Int = 0,
    val sourceFailuresCount: Int = 0,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

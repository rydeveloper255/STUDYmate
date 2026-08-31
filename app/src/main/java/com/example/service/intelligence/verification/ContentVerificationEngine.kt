package com.example.service.intelligence.verification

import android.util.Log
import com.example.data.model.updates.LatestUpdateItem
import com.example.service.intelligence.smart.*
import java.util.UUID

/**
 * Step 84: Advanced Content Verification & Quality Control Engine.
 * 
 * Central verification engine implementing:
 * FETCH -> UNDERSTAND -> EXTRACT -> VERIFY -> VALIDATE -> QUALITY CHECK -> PUBLISH
 * 
 * Guarantees:
 * - No fake or guessed data
 * - Two-layer validation (Deterministic + Intelligent)
 * - Anti-hallucination source text grounding
 * - Admin review queue routing for ambiguous/conflicting data
 * - Strict auto-publish gating
 */
class ContentVerificationEngine(
    val reviewQueueManager: ReviewQueueManager = ReviewQueueManager(),
    val versionManager: ContentVersionManager = ContentVersionManager(),
    val metricsTracker: VerificationMetricsTracker = VerificationMetricsTracker()
) {
    companion object {
        private const val TAG = "ContentVerificationEng"
    }

    /**
     * Executes end-to-end verification and quality control on an incoming item.
     */
    fun verify(
        rawContent: String,
        cleanedContent: String,
        category: SmartContentCategory,
        confidence: SmartConfidenceScore,
        extractedData: SmartExtractedData,
        normalizedTitle: String,
        links: List<SmartLink>,
        sourceUrl: String,
        sourceName: String,
        sourceType: String,
        sourcePostDate: String?,
        lastDate: String?,
        examDate: String?,
        sourceMessageId: String? = null,
        existingRecords: List<LatestUpdateItem> = emptyList()
    ): VerificationReport {
        val recordId = sourceMessageId?.let { "wa_$it" } ?: UUID.randomUUID().toString()
        val allIssues = mutableListOf<String>()
        val allReviewReasons = mutableListOf<ReviewReason>()
        val notes = mutableListOf<String>()

        // 1. LAYER 1: Deterministic Validation
        val layer1 = DeterministicValidator.validate(
            title = extractedData.title,
            category = category,
            extractedData = extractedData,
            sourcePostDate = sourcePostDate,
            lastDate = lastDate,
            examDate = examDate,
            links = links,
            sourceMessageId = sourceMessageId
        )

        val layer1Result = LayerValidationResult(
            layer = ValidationLayer.DETERMINISTIC,
            passed = layer1.isValid,
            issues = layer1.issues,
            flaggedReasons = layer1.flaggedReasons
        )
        allIssues.addAll(layer1.issues)
        allReviewReasons.addAll(layer1.flaggedReasons)

        // 2. LAYER 2: Intelligent & Contextual Validation
        val layer2 = IntelligentValidator.validate(
            rawText = rawContent,
            cleanedText = cleanedContent,
            category = category,
            extractedData = extractedData,
            sourcePostDate = sourcePostDate,
            lastDate = lastDate,
            examDate = examDate
        )

        val layer2Result = LayerValidationResult(
            layer = ValidationLayer.INTELLIGENT,
            passed = layer2.passed,
            issues = layer2.issues,
            flaggedReasons = layer2.flaggedReasons
        )
        allIssues.addAll(layer2.issues)
        allReviewReasons.addAll(layer2.flaggedReasons)

        // 3. Link Health & Safety Verification
        val linkResults = links.map { LinkHealthVerifier.verifyLink(it.url, sourceUrl) }
        for (linkRes in linkResults) {
            if (!linkRes.isSafeScheme) {
                allReviewReasons.add(ReviewReason.LINK_UNSAFE)
                allIssues.add("Blocked unsafe link scheme in URL: ${linkRes.originalUrl}")
            } else if (linkRes.status == LinkHealthStatus.BROKEN) {
                allReviewReasons.add(ReviewReason.LINK_UNCERTAIN)
                allIssues.add("Malformed link syntax: ${linkRes.originalUrl}")
            }
        }

        // 4. Cross-Source Conflict Detection
        val tempItem = SmartProcessedItem(
            id = recordId,
            rawContent = rawContent,
            cleanedContent = cleanedContent,
            category = category,
            confidence = confidence,
            extractedData = layer2.sanitizedExtractedData,
            normalizedTitle = normalizedTitle,
            qualityScore = SmartQualityScore(100, 15, 25, 15, 15, 15, 10, 5, true),
            duplicateResult = SmartDuplicateResult(false, false),
            aiSummary = "",
            sourceUrl = sourceUrl,
            sourceName = sourceName,
            sourceType = sourceType,
            postDate = sourcePostDate,
            lastDate = layer2.sanitizedLastDate,
            examDate = layer2.sanitizedExamDate,
            status = SmartProcessingStatus.PROCESSING
        )
        val conflictInfo = SmartConflictDetector.detectConflict(tempItem, existingRecords)
        if (conflictInfo.hasConflict) {
            allReviewReasons.add(ReviewReason.DATE_CONFLICT)
            allIssues.add("Cross-source data conflict: ${conflictInfo.conflictDescription}")
            metricsTracker.recordCriticalConflict(extractedData.title, conflictInfo.conflictDescription ?: "Conflict")
        }

        // 5. Quality Score Calculation
        val qualityScore = SmartQualityScorer.calculateScore(
            title = extractedData.title,
            confidence = confidence,
            postDate = sourcePostDate,
            lastDate = layer2.sanitizedLastDate,
            examDate = layer2.sanitizedExamDate,
            organization = layer2.sanitizedExtractedData.organization,
            links = links,
            hasConflict = conflictInfo.hasConflict,
            hasInvalidDate = layer1.flaggedReasons.contains(ReviewReason.IMPOSSIBLE_DATE)
        )

        // 6. Publication Decision & Status Evaluation
        val isExpired = layer1.isExpired
        val hasCategoryConflict = allReviewReasons.contains(ReviewReason.CATEGORY_CONFLICT)
        val hasDateConflict = allReviewReasons.contains(ReviewReason.DATE_CONFLICT) || layer2.flaggedReasons.contains(ReviewReason.DATE_CONFLICT)
        val hasImpossibleDate = allReviewReasons.contains(ReviewReason.IMPOSSIBLE_DATE)
        val hasHallucination = allReviewReasons.contains(ReviewReason.POTENTIAL_HALLUCINATION)
        val hasUnsafeLink = allReviewReasons.contains(ReviewReason.LINK_UNSAFE)
        val hasMissingCritical = allReviewReasons.contains(ReviewReason.MISSING_CRITICAL_DATA) || allReviewReasons.contains(ReviewReason.ORGANIZATION_UNCERTAIN)
        val isCutoffEligible = layer1.isCutoffEligible

        val overallStatus = when {
            !isCutoffEligible -> VerificationStatus.REJECTED
            conflictInfo.hasConflict -> VerificationStatus.DATA_CONFLICT
            hasCategoryConflict -> VerificationStatus.CATEGORY_CONFLICT
            hasDateConflict -> VerificationStatus.DATE_CONFLICT
            hasImpossibleDate -> VerificationStatus.REVIEW_REQUIRED
            hasUnsafeLink -> VerificationStatus.REVIEW_REQUIRED
            hasHallucination -> VerificationStatus.REVIEW_REQUIRED
            hasMissingCritical -> VerificationStatus.REVIEW_REQUIRED
            allReviewReasons.contains(ReviewReason.UNKNOWN_EXPIRY) -> VerificationStatus.UNKNOWN_EXPIRY
            !qualityScore.isEligibleForAutoPublish -> VerificationStatus.REVIEW_REQUIRED
            isExpired -> VerificationStatus.EXPIRED
            else -> VerificationStatus.VERIFIED
        }

        val canAutoPublish = overallStatus == VerificationStatus.VERIFIED

        val provenance = ContentProvenance(
            sourceType = sourceType,
            sourceName = sourceName,
            sourceUrl = sourceUrl,
            sourceMessageId = sourceMessageId,
            sourcePostDate = sourcePostDate
        )

        val report = VerificationReport(
            id = recordId,
            overallStatus = overallStatus,
            isEligibleForAutoPublish = canAutoPublish,
            layer1Result = layer1Result,
            layer2Result = layer2Result,
            linkVerificationResults = linkResults,
            qualityScore = qualityScore,
            provenance = provenance,
            reviewReasons = allReviewReasons.distinct(),
            notes = allIssues
        )

        // If review required or conflict, automatically enqueue in admin review queue
        if (!canAutoPublish && overallStatus != VerificationStatus.EXPIRED && overallStatus != VerificationStatus.REJECTED) {
            val extractedMap = mutableMapOf(
                "title" to extractedData.title,
                "organization" to (extractedData.organization ?: "null"),
                "post_date" to (sourcePostDate ?: "null"),
                "last_date" to (layer2.sanitizedLastDate ?: "null"),
                "exam_date" to (layer2.sanitizedExamDate ?: "null")
            )
            val conflictMap = if (conflictInfo.hasConflict) {
                mapOf(
                    "conflicting_field" to (conflictInfo.conflictingField ?: ""),
                    "source_a_value" to (conflictInfo.sourceAValue ?: ""),
                    "source_b_value" to (conflictInfo.sourceBValue ?: "")
                )
            } else emptyMap()

            val reviewItem = ReviewQueueItem(
                recordId = recordId,
                title = extractedData.title,
                category = category,
                sourceName = sourceName,
                sourceUrl = sourceUrl,
                sourcePostDate = sourcePostDate,
                reasons = allReviewReasons.distinct(),
                reasonDescription = allIssues.joinToString("; ").ifBlank { overallStatus.name },
                rawContent = rawContent,
                extractedValues = extractedMap,
                conflictingValues = conflictMap
            )
            reviewQueueManager.enqueueReview(reviewItem)
        }

        metricsTracker.recordProcessed(overallStatus)
        Log.i(TAG, "Completed verification for [${extractedData.title}]: Status=${overallStatus.name}, AutoPublish=$canAutoPublish, Quality=${qualityScore.totalScore}/100")

        return report
    }
}

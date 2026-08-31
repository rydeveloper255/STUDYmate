package com.example.service.intelligence.smart

import android.util.Log
import com.example.data.local.RecruitmentDao
import com.example.data.model.RecruitmentContentType
import com.example.data.model.RecruitmentEntity
import com.example.data.model.VacancyStatus
import com.example.data.model.updates.LatestUpdateItem
import com.example.data.remote.GeminiRepository
import com.example.data.remote.supabase.LatestUpdatesRepository
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseResult
import com.example.service.content.whatsapp.WhatsAppSourceConfig
import com.example.service.intelligence.verification.ContentVerificationEngine
import com.example.service.intelligence.verification.VerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

/**
 * Step 83 & Step 84: Smart Content Intelligence & Advanced Verification Pipeline.
 * 
 * Implements the complete intelligent ingestion, verification & processing pipeline:
 * 1. SOURCE: Ingest raw post/text from WhatsApp or Website
 * 2. RAW CONTENT: Archival preservation
 * 3. DATE FILTER: Cutoff validation (>= 2026-08-01)
 * 4. TEXT CLEANING: Strip noise, tracking URLs, formatting
 * 5. CONTENT DETECTION: Title, structure, signals identification
 * 6. CATEGORY CLASSIFICATION: Context-based with confidence score
 * 7. IMPORTANT DATA EXTRACTION: Category-specific fields (Zero fake data)
 * 8. TWO-LAYER VERIFICATION: Deterministic + Contextual Semantic Validation
 * 9. DUPLICATE CHECK: 5-level duplicate & update detection
 * 10. EXPIRY CHECK: Last date vs today in IST
 * 11. QUALITY SCORE: 0-100 score gating auto-publish
 * 12. SUPABASE / APP: Database-first idempotent storage
 */
class SmartContentIntelligencePipeline(
    private val supabaseClient: SupabaseClient = SupabaseClient.instance,
    private val recruitmentDao: RecruitmentDao? = null,
    private val geminiRepository: GeminiRepository? = null,
    val verificationEngine: ContentVerificationEngine = ContentVerificationEngine()
) {
    companion object {
        private const val TAG = "SmartIntelligencePipe"
        const val TABLE_RAW_SOURCE_CONTENT = "raw_source_content"
        const val TABLE_LATEST_UPDATES = LatestUpdatesRepository.TABLE_LATEST_UPDATES
    }

    /**
     * Executes the smart intelligence pipeline on a single raw input post.
     */
    suspend fun processContent(
        rawText: String,
        sourceMessageId: String? = null,
        sourceUrl: String = WhatsAppSourceConfig.configuredChannelUrl,
        sourceName: String = WhatsAppSourceConfig.SOURCE_NAME,
        sourceType: String = WhatsAppSourceConfig.SOURCE_TYPE,
        sourcePostDate: String? = null,
        existingRecords: List<LatestUpdateItem> = emptyList(),
        forceLocalOnly: Boolean = false
    ): SmartProcessedItem = withContext(Dispatchers.IO) {
        val stageLogs = mutableListOf<SmartPipelineStageLog>()
        val itemId = sourceMessageId?.let { "wa_$it" } ?: UUID.randomUUID().toString()

        fun log(stage: SmartPipelineStage, msg: String, details: Map<String, String> = emptyMap()) {
            stageLogs.add(SmartPipelineStageLog(stage, msg, System.currentTimeMillis(), details))
            Log.d(TAG, "[${stage.name}] $msg")
        }

        // Stage 1: SOURCE RECEIVED
        log(SmartPipelineStage.SOURCE_RECEIVED, "Received source item from $sourceName (msg_id: ${sourceMessageId ?: "none"})")

        // Stage 2: RAW CONTENT PRESERVATION
        val preservedRawText = rawText

        // Stage 3: DATE FILTER CHECK
        val cutoffCheck = SmartDateIntelligence.isEligibleByCutoff(sourcePostDate)
        if (cutoffCheck == false) {
            log(SmartPipelineStage.DATE_FILTER_CHECKED, "Post date ($sourcePostDate) is strictly before historical cutoff 2026-08-01. Dropped.")
            return@withContext buildTerminalItem(
                id = itemId,
                sourceMessageId = sourceMessageId,
                raw = preservedRawText,
                cleaned = preservedRawText,
                category = SmartContentCategory.OTHER,
                status = SmartProcessingStatus.IGNORED_BEFORE_CUTOFF,
                sourceUrl = sourceUrl,
                sourceName = sourceName,
                sourceType = sourceType,
                postDate = sourcePostDate,
                logs = stageLogs
            )
        } else if (cutoffCheck == null && sourcePostDate == null && rawText.isBlank()) {
            log(SmartPipelineStage.DATE_FILTER_CHECKED, "Post date is unavailable and content is blank. Marked as REVIEW_REQUIRED.")
            return@withContext buildTerminalItem(
                id = itemId,
                sourceMessageId = sourceMessageId,
                raw = preservedRawText,
                cleaned = preservedRawText,
                category = SmartContentCategory.OTHER,
                status = SmartProcessingStatus.DATE_UNAVAILABLE,
                sourceUrl = sourceUrl,
                sourceName = sourceName,
                sourceType = sourceType,
                postDate = null,
                logs = stageLogs
            )
        }

        // Stage 4: TEXT CLEANING
        val cleanedText = SmartContentCleaner.cleanContent(preservedRawText)
        log(SmartPipelineStage.TEXT_CLEANED, "Cleaned text: reduced ${preservedRawText.length} chars to ${cleanedText.length} chars.")

        // Stage 5: CONTENT DETECTION (Title & Lines)
        val lines = cleanedText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val rawTitle = lines.firstOrNull() ?: "Official Update"
        val normalizedTitle = SmartTitleNormalizer.normalizeTitle(rawTitle)
        log(SmartPipelineStage.CONTENT_DETECTED, "Detected Title: '$rawTitle' | Normalized: '$normalizedTitle'")

        if (cleanedText.isBlank()) {
            log(SmartPipelineStage.FIELDS_EXTRACTED, "Empty content body. Route to REVIEW_REQUIRED.")
            return@withContext buildTerminalItem(
                id = itemId,
                sourceMessageId = sourceMessageId,
                raw = preservedRawText,
                cleaned = cleanedText,
                category = SmartContentCategory.OTHER,
                status = SmartProcessingStatus.REVIEW_REQUIRED,
                sourceUrl = sourceUrl,
                sourceName = sourceName,
                sourceType = sourceType,
                postDate = sourcePostDate,
                logs = stageLogs
            )
        }

        // Stage 6: CATEGORY CLASSIFICATION (Context-based + Confidence)
        val classification = SmartCategoryClassifier.classify(rawTitle, cleanedText)
        log(
            SmartPipelineStage.CATEGORY_CLASSIFIED,
            "Classified as ${classification.category.name} with confidence ${classification.confidence.score} (${classification.confidence.level.name})"
        )

        // Stage 7: IMPORTANT DATA EXTRACTION (Category-specific + Links)
        val links = SmartLinkClassifier.extractAndClassifyLinks(cleanedText, classification.category)
        val dates = SmartDateIntelligence.extractAllDates(cleanedText, sourcePostDate)
        val pdfUrl = links.firstOrNull { it.isVerifiedPdf }?.url

        val extractedData = SmartCategoryDataExtractor.extractData(
            title = rawTitle,
            text = cleanedText,
            category = classification.category,
            sourceUrl = sourceUrl,
            sourcePostDate = dates.startDate ?: sourcePostDate,
            links = links,
            dates = dates
        )
        log(SmartPipelineStage.FIELDS_EXTRACTED, "Extracted data: Org='${extractedData.organization}', PrimaryUrl='${extractedData.primaryUrl}'")

        // Stage 8: TWO-LAYER VERIFICATION & QUALITY CONTROL (Step 84 Engine)
        val canonicalPostDate = dates.startDate ?: sourcePostDate
        val rawLastDate = dates.lastDate
        val rawExamDate = dates.examDate

        val verificationReport = verificationEngine.verify(
            rawContent = preservedRawText,
            cleanedContent = cleanedText,
            category = classification.category,
            confidence = classification.confidence,
            extractedData = extractedData,
            normalizedTitle = normalizedTitle,
            links = links,
            sourceUrl = sourceUrl,
            sourceName = sourceName,
            sourceType = sourceType,
            sourcePostDate = canonicalPostDate,
            lastDate = rawLastDate,
            examDate = rawExamDate,
            sourceMessageId = sourceMessageId,
            existingRecords = existingRecords
        )

        val canonicalLastDate = if (verificationReport.layer2Result.passed || verificationReport.overallStatus == VerificationStatus.EXPIRED) {
            rawLastDate
        } else {
            // Anti-hallucination / invalid date safety
            null
        }
        val canonicalExamDate = rawExamDate

        log(SmartPipelineStage.DATE_NORMALIZED_VALIDATED, "Verification Report: Status=${verificationReport.overallStatus.name}, Issues=${verificationReport.notes}")

        // Stage 9: EXPIRY CHECK
        val isExpired = verificationReport.overallStatus == VerificationStatus.EXPIRED
        log(SmartPipelineStage.EXPIRY_CHECKED, "Expiry status: isExpired=$isExpired (lastDate=$canonicalLastDate)")

        // Stage 10: QUALITY SCORE
        val qualityScore = verificationReport.qualityScore
        log(SmartPipelineStage.QUALITY_SCORED, "Quality Score: ${qualityScore.totalScore}/100 (Eligible: ${qualityScore.isEligibleForAutoPublish})")

        // Stage 11: DUPLICATE CHECK & UPDATE RESOLUTION
        val contentHash = SmartDuplicateDetector.computeContentHash(
            title = rawTitle,
            category = classification.category.key,
            org = extractedData.organization,
            date = canonicalLastDate ?: canonicalPostDate,
            primaryUrl = extractedData.primaryUrl
        )

        val candidateStatus = when (verificationReport.overallStatus) {
            VerificationStatus.CATEGORY_CONFLICT -> SmartProcessingStatus.CATEGORY_CONFLICT
            VerificationStatus.DATE_CONFLICT -> SmartProcessingStatus.DATE_CONFLICT
            VerificationStatus.DATA_CONFLICT -> SmartProcessingStatus.DATA_CONFLICT
            VerificationStatus.REVIEW_REQUIRED -> SmartProcessingStatus.REVIEW_REQUIRED
            VerificationStatus.UNKNOWN_EXPIRY -> SmartProcessingStatus.REVIEW_REQUIRED
            VerificationStatus.REJECTED -> SmartProcessingStatus.REVIEW_REQUIRED
            VerificationStatus.EXPIRED -> SmartProcessingStatus.EXPIRED
            VerificationStatus.TEMPORARILY_UNAVAILABLE -> SmartProcessingStatus.SOURCE_UNAVAILABLE
            VerificationStatus.FAILED -> SmartProcessingStatus.FAILED
            else -> if (verificationReport.isEligibleForAutoPublish) SmartProcessingStatus.PUBLISHED else SmartProcessingStatus.REVIEW_REQUIRED
        }

        val candidateItem = SmartProcessedItem(
            id = itemId,
            sourceMessageId = sourceMessageId,
            rawContent = preservedRawText,
            cleanedContent = cleanedText,
            category = classification.category,
            confidence = classification.confidence,
            extractedData = extractedData,
            links = links,
            pdfUrl = pdfUrl,
            normalizedTitle = normalizedTitle,
            qualityScore = qualityScore,
            duplicateResult = SmartDuplicateResult(isDuplicate = false, isUpdate = false),
            conflictInfo = if (candidateStatus == SmartProcessingStatus.DATA_CONFLICT || candidateStatus == SmartProcessingStatus.DATE_CONFLICT) {
                SmartConflictDetector.detectConflict(
                    newItem = buildTemporaryItem(
                        id = itemId,
                        title = rawTitle,
                        normTitle = normalizedTitle,
                        category = classification.category,
                        data = extractedData,
                        lastDate = canonicalLastDate,
                        examDate = canonicalExamDate,
                        sourceName = sourceName,
                        sourceUrl = sourceUrl,
                        sourceType = sourceType
                    ),
                    existingRecords = existingRecords
                ).takeIf { it.hasConflict }
            } else null,
            aiSummary = SmartSummaryGenerator.generateSummary(classification.category, extractedData, canonicalLastDate, canonicalExamDate),
            sourceUrl = sourceUrl,
            sourceName = sourceName,
            sourceType = sourceType,
            postDate = canonicalPostDate,
            lastDate = canonicalLastDate,
            examDate = canonicalExamDate,
            isExpired = isExpired,
            isActive = !isExpired && candidateStatus == SmartProcessingStatus.PUBLISHED,
            status = candidateStatus,
            stageLogs = stageLogs,
            contentHash = contentHash
        )

        val duplicateResult = SmartDuplicateDetector.evaluate(candidateItem, existingRecords)
        log(SmartPipelineStage.DUPLICATE_CHECKED, "Duplicate check: isDup=${duplicateResult.isDuplicate}, isUpdate=${duplicateResult.isUpdate}, level=${duplicateResult.duplicateLevel.name}")

        val finalItem = if (duplicateResult.isUpdate) {
            // Track version update
            duplicateResult.existingRecordId?.let { existingId ->
                verificationEngine.versionManager.recordVersion(
                    recordId = existingId,
                    fieldName = "last_date",
                    previousValue = null,
                    newValue = canonicalLastDate,
                    changeSource = sourceName,
                    changeSummary = "Updated notification received from $sourceName"
                )
            }
            candidateItem.copy(
                id = duplicateResult.existingRecordId ?: itemId,
                duplicateResult = duplicateResult,
                status = SmartProcessingStatus.UPDATED
            )
        } else if (duplicateResult.isDuplicate) {
            candidateItem.copy(
                id = duplicateResult.existingRecordId ?: itemId,
                duplicateResult = duplicateResult,
                status = SmartProcessingStatus.DUPLICATE
            )
        } else {
            candidateItem.copy(duplicateResult = duplicateResult)
        }

        // Stage 12: DATABASE PERSISTENCE (Idempotent Supabase & Room)
        if (!forceLocalOnly && finalItem.status != SmartProcessingStatus.DUPLICATE) {
            persistToSupabaseAndRoom(finalItem, isUpdate = duplicateResult.isUpdate)
            log(SmartPipelineStage.DATABASE_SAVED, "Persisted item status=${finalItem.status.name} to Database.")
        }

        finalItem
    }

    private suspend fun persistToSupabaseAndRoom(item: SmartProcessedItem, isUpdate: Boolean) {
        // 1. Save Raw Content Record
        try {
            val rawPayload = JSONObject().apply {
                put("source_type", item.sourceType)
                put("source_name", item.sourceName)
                put("source_url", item.sourceUrl)
                put("source_message_id", item.sourceMessageId)
                put("source_post_date", item.postDate)
                put("raw_text", item.rawContent)
                put("cleaned_text", item.cleanedContent)
                put("content_hash", item.contentHash)
                put("quality_score", item.qualityScore.totalScore)
                put("confidence_score", item.confidence.score.toDouble())
                put("category", item.category.key)
                put("status", item.status.name)
            }
            supabaseClient.from(TABLE_RAW_SOURCE_CONTENT).insert(rawPayload.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Raw source persistence skipped/error: ${e.message}")
        }

        // 2. Save Structured Record to Room Database
        try {
            val cType = when (item.category) {
                SmartContentCategory.VACANCY -> RecruitmentContentType.VACANCY.name
                SmartContentCategory.ADMIT_CARD -> RecruitmentContentType.ADMIT_CARD.name
                SmartContentCategory.RESULT -> RecruitmentContentType.RESULT.name
                SmartContentCategory.ANSWER_KEY -> RecruitmentContentType.ANSWER_KEY.name
                SmartContentCategory.ADMISSION -> RecruitmentContentType.NOTIFICATION.name
                SmartContentCategory.OTHER -> RecruitmentContentType.NOTIFICATION.name
            }

            val entity = RecruitmentEntity(
                id = item.id,
                title = item.extractedData.title,
                organization = item.extractedData.organization ?: "Government Organization",
                postName = (item.extractedData as? SmartExtractedData.Vacancy)?.data?.postName ?: item.extractedData.title,
                contentType = cType,
                rawStatus = if (item.isExpired) VacancyStatus.CLOSED.name else VacancyStatus.OPEN.name,
                totalVacancies = (item.extractedData as? SmartExtractedData.Vacancy)?.data?.vacancyCount,
                applicationStartDate = item.postDate,
                applicationLastDate = item.lastDate,
                examDate = item.examDate,
                feeDetails = (item.extractedData as? SmartExtractedData.Vacancy)?.data?.applicationFee ?: "Not specified",
                educationalQualification = (item.extractedData as? SmartExtractedData.Vacancy)?.data?.qualification ?: "Not specified",
                ageRelaxation = (item.extractedData as? SmartExtractedData.Vacancy)?.data?.ageLimit ?: "Not specified",
                selectionProcess = emptyList(),
                sourceUrl = item.sourceUrl,
                officialSourceUrl = item.pdfUrl ?: item.extractedData.primaryUrl ?: item.sourceUrl,
                applicationUrl = item.extractedData.primaryUrl ?: item.sourceUrl,
                officialPdfUrl = item.pdfUrl ?: "",
                summaryEn = item.aiSummary,
                summaryHi = item.aiSummary,
                whatShouldIDo = emptyList(),
                isVerified = true,
                fetchedAt = item.createdAt,
                lastVerifiedAt = item.updatedAt,
                contentHash = item.contentHash
            )
            recruitmentDao?.insertOrUpdate(entity)
        } catch (e: Exception) {
            Log.w(TAG, "Room persistence error: ${e.message}")
        }

        // 3. Save Structured Record to Supabase
        try {
            val payload = JSONObject().apply {
                put("id", item.id)
                put("update_type", item.category.key)
                put("title", item.extractedData.title)
                put("short_description", item.aiSummary)
                put("full_content", item.cleanedContent)
                put("organization", item.extractedData.organization ?: "")
                put("published_date", item.postDate)
                put("start_date", item.postDate)
                put("last_date", item.lastDate)
                put("exam_date", item.examDate)
                put("source_url", item.sourceUrl)
                put("apply_url", item.extractedData.primaryUrl ?: "")
                put("download_url", item.pdfUrl ?: "")
                put("source_name", item.sourceName)
                put("source_type", item.sourceType)
                put("external_id", item.sourceMessageId)
                put("content_hash", item.contentHash)
                put("is_active", item.isActive)

                val meta = JSONObject().apply {
                    put("quality_score", item.qualityScore.totalScore)
                    put("confidence_score", item.confidence.score.toDouble())
                    put("ai_summary", item.aiSummary)
                    put("is_expired", item.isExpired)
                }
                put("metadata", meta)
            }

            if (isUpdate) {
                supabaseClient.from(TABLE_LATEST_UPDATES).update(
                    queryParams = mapOf("id" to "eq.${item.id}"),
                    jsonBody = payload.toString()
                )
            } else {
                supabaseClient.from(TABLE_LATEST_UPDATES).insert(
                    jsonBody = payload.toString()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase latest_updates save error: ${e.message}")
        }
    }

    private fun buildTerminalItem(
        id: String,
        sourceMessageId: String?,
        raw: String,
        cleaned: String,
        category: SmartContentCategory,
        status: SmartProcessingStatus,
        sourceUrl: String,
        sourceName: String,
        sourceType: String,
        postDate: String?,
        logs: List<SmartPipelineStageLog>
    ): SmartProcessedItem {
        return SmartProcessedItem(
            id = id,
            sourceMessageId = sourceMessageId,
            rawContent = raw,
            cleanedContent = cleaned,
            category = category,
            confidence = SmartConfidenceScore(0.0f, SmartConfidenceLevel.LOW),
            extractedData = SmartExtractedData.Other(SmartOtherData(title = "Unprocessed Item", sourceUrl = sourceUrl, sourcePostDate = postDate)),
            normalizedTitle = "",
            qualityScore = SmartQualityScore(0, 0, 0, 0, 0, 0, 0, 0, false),
            duplicateResult = SmartDuplicateResult(isDuplicate = false, isUpdate = false),
            aiSummary = "",
            sourceUrl = sourceUrl,
            sourceName = sourceName,
            sourceType = sourceType,
            postDate = postDate,
            isActive = false,
            status = status,
            stageLogs = logs
        )
    }

    private fun buildTemporaryItem(
        id: String,
        title: String,
        normTitle: String,
        category: SmartContentCategory,
        data: SmartExtractedData,
        lastDate: String?,
        examDate: String?,
        sourceName: String,
        sourceUrl: String,
        sourceType: String
    ): SmartProcessedItem {
        return SmartProcessedItem(
            id = id,
            rawContent = "",
            cleanedContent = "",
            category = category,
            confidence = SmartConfidenceScore(1.0f, SmartConfidenceLevel.HIGH),
            extractedData = data,
            normalizedTitle = normTitle,
            qualityScore = SmartQualityScore(100, 15, 25, 15, 15, 15, 10, 5, true),
            duplicateResult = SmartDuplicateResult(false, false),
            aiSummary = "",
            sourceUrl = sourceUrl,
            sourceName = sourceName,
            sourceType = sourceType,
            lastDate = lastDate,
            examDate = examDate,
            isActive = true,
            status = SmartProcessingStatus.PROCESSING
        )
    }
}

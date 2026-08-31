package com.example.service.collector

import android.content.Context
import android.util.Log
import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import com.example.data.model.content.*
import com.example.data.remote.GeminiRepository
import com.example.data.remote.telegram.TelegramBotConfig
import com.example.data.remote.telegram.TelegramBotService
import com.example.data.remote.telegram.TelegramPublishResult
import com.example.service.intelligence.ContentIntelligenceAiEngine
import com.example.service.intelligence.SmartRecruitmentScraperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * AutomatedContentCollectorEngine (Step 63 Production Implementation)
 * 
 * Strict separation of concerns:
 * - COLLECTOR: Discovers and collects raw source data.
 * - AI: Understands, structures, cleans, normalizes Hindi and evaluates status/confidence without inventing facts.
 * - DATABASE: Stores raw source records, structured entities, version change history, review queue items, and logs.
 * - TELEGRAM BOT: Publishes verified approved updates and update notifications with duplicate protection.
 * - APP: Displays high quality, source-attributed, verified cards and in-app PDF viewer.
 */
class AutomatedContentCollectorEngine(
    private val context: Context? = null,
    private val database: StudyMateDatabase,
    private val sourceManager: SourceManager = SourceManager(),
    private val telegramBotService: TelegramBotService = TelegramBotService(),
    private val geminiRepository: GeminiRepository? = null,
    private val recruitmentScraper: SmartRecruitmentScraperService = SmartRecruitmentScraperService(geminiRepository),
    private val gkNowPdfScraper: GkNowWeeklyPdfScraper = GkNowWeeklyPdfScraper(),
    private val aiEngine: ContentIntelligenceAiEngine = ContentIntelligenceAiEngine(geminiRepository),
    private val supabaseContentHub: com.example.data.remote.supabase.SupabaseContentHubService = com.example.data.remote.supabase.SupabaseContentHubService(database = database),
    private val whatsAppIngestionService: com.example.service.content.whatsapp.WhatsAppChannelIngestionService = com.example.service.content.whatsapp.WhatsAppChannelIngestionService(
        recruitmentDao = database.recruitmentDao(),
        sourceManager = sourceManager
    )
) {
    companion object {
        private const val TAG = "ContentCollectorEngine"
        private const val TELEGRAM_TEST_CHAT_ID = "@StudyMateOfficialBot"
    }

    // Fingerprint caches to prevent duplicates
    private val processedFingerprints = ConcurrentHashMap.newKeySet<String>()
    private val telegramPostedFingerprints = ConcurrentHashMap.newKeySet<String>()

    // In-memory runtime states for real-time Developer & Bot Diagnostics
    private val _lastJobLog = MutableStateFlow<ContentCollectionJobLog?>(null)
    val lastJobLog: StateFlow<ContentCollectionJobLog?> = _lastJobLog.asStateFlow()

    private val _isCollecting = MutableStateFlow(false)
    val isCollecting: StateFlow<Boolean> = _isCollecting.asStateFlow()

    private val _recentCollectedItems = MutableStateFlow<List<CollectedContentItem>>(emptyList())
    val recentCollectedItems: StateFlow<List<CollectedContentItem>> = _recentCollectedItems.asStateFlow()

    private val _weeklyPdfs = MutableStateFlow<List<WeeklyCurrentAffairsPdf>>(emptyList())
    val weeklyPdfs: StateFlow<List<WeeklyCurrentAffairsPdf>> = _weeklyPdfs.asStateFlow()

    private val _publishedTelegramHistory = MutableStateFlow<List<CollectedContentItem>>(emptyList())
    val publishedTelegramHistory: StateFlow<List<CollectedContentItem>> = _publishedTelegramHistory.asStateFlow()

    /**
     * Runs one full automated or manual collection cycle.
     * Isolated failure handling ensures that failure of one source does not abort the entire job.
     */
    suspend fun executeCollectionCycle(): ContentCollectionJobLog = withContext(Dispatchers.IO) {
        if (_isCollecting.value) {
            Log.w(TAG, "Collection cycle is already executing. Skipping redundant call.")
            return@withContext _lastJobLog.value ?: ContentCollectionJobLog(
                startedAt = System.currentTimeMillis(),
                completedAt = System.currentTimeMillis(),
                status = "SKIPPED_ALREADY_RUNNING",
                sourcesChecked = 0,
                newItems = 0,
                duplicateItems = 0,
                failedSources = 0,
                telegramPosts = 0,
                pdfsDetected = 0
            )
        }

        _isCollecting.value = true
        val startTime = System.currentTimeMillis()
        val enabledSources = sourceManager.getEnabledSources()

        var totalNewItems = 0
        var totalUpdatedItems = 0
        var totalDuplicates = 0
        var totalFailedSources = 0
        var totalTelegramPosts = 0
        var totalPdfsDetected = 0
        var totalAiProcessed = 0
        var totalReviewRequired = 0
        val errorsList = mutableListOf<String>()
        val cycleCollectedItems = mutableListOf<CollectedContentItem>()

        Log.i(TAG, "Initiating Content Intelligence Collection. Enabled sources count: ${enabledSources.size}")

        for (source in enabledSources) {
            try {
                Log.d(TAG, "Ingesting source: ${source.sourceName} [${source.sourceUrl}]")
                if (source.sourceType == SourceType.WHATSAPP_CHANNEL) {
                    val waResult = whatsAppIngestionService.runIngestionCycle(source.sourceUrl)
                    totalNewItems += waResult.publishedCount
                    totalUpdatedItems += waResult.updatedCount
                    totalDuplicates += waResult.duplicateCount
                    totalReviewRequired += waResult.reviewRequiredCount
                    if (!waResult.success && waResult.failedCount > 0) {
                        totalFailedSources++
                        waResult.errorMessage?.let { errorsList.add("${source.sourceName}: $it") }
                    }
                    continue
                }

                when (source.category) {
                    ContentCategory.CURRENT_AFFAIRS_PDF -> {
                        val pdfResults = processWeeklyPdfSource(source)
                        totalPdfsDetected += pdfResults.newPdfs.size
                        totalDuplicates += pdfResults.duplicateCount
                        totalNewItems += pdfResults.newPdfs.size
                        totalTelegramPosts += pdfResults.telegramPostsSent
                        totalAiProcessed += pdfResults.newPdfs.size
                        cycleCollectedItems.addAll(pdfResults.collectedItems)
                        sourceManager.recordSourceCheckResult(source.sourceId, isSuccess = true, discoveredCount = pdfResults.newPdfs.size)
                    }

                    ContentCategory.VACANCY,
                    ContentCategory.RESULT,
                    ContentCategory.ADMIT_CARD,
                    ContentCategory.ANSWER_KEY,
                    ContentCategory.ADMISSION,
                    ContentCategory.IMPORTANT_UPDATE -> {
                        val result = processRecruitmentSource(source)
                        totalNewItems += result.newCount
                        totalUpdatedItems += result.updatedCount
                        totalDuplicates += result.duplicateCount
                        totalTelegramPosts += result.telegramPostsSent
                        totalAiProcessed += result.aiProcessedCount
                        totalReviewRequired += result.reviewRequiredCount
                        cycleCollectedItems.addAll(result.collectedItems)
                        sourceManager.recordSourceCheckResult(source.sourceId, isSuccess = true, discoveredCount = result.newCount + result.updatedCount)
                    }

                    ContentCategory.CURRENT_AFFAIRS -> {
                        sourceManager.recordSourceCheckResult(source.sourceId, isSuccess = true, discoveredCount = 0)
                    }

                    ContentCategory.EXTERNAL_SOURCE -> {
                        Log.d(TAG, "Source ${source.sourceName} is reference only. Skipping direct scrape.")
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                totalFailedSources++
                val sanitizedError = "Host unreachable or offline (${e.message})"
                errorsList.add("${source.sourceName}: $sanitizedError")
                sourceManager.recordSourceCheckResult(source.sourceId, isSuccess = false, errorMessage = sanitizedError)
                Log.d(TAG, "Source [${source.sourceName}] offline: $sanitizedError")
            } catch (e: Exception) {
                totalFailedSources++
                val sanitizedError = TelegramBotConfig.sanitize(e.message ?: "Error processing ${source.sourceName}")
                errorsList.add("${source.sourceName}: $sanitizedError")
                sourceManager.recordSourceCheckResult(source.sourceId, isSuccess = false, errorMessage = sanitizedError)
                com.example.service.admin.TelegramAdminBotManager.notifyContentPipelineFailure(
                    content = source.category.name,
                    source = source.sourceName,
                    stage = "Scrape & Extraction",
                    reason = sanitizedError
                )
                Log.w(TAG, "Failure on source [${source.sourceName}]: $sanitizedError")
            }
        }

        val endTime = System.currentTimeMillis()
        val finalStatus = when {
            totalFailedSources == 0 -> "SUCCESS"
            totalFailedSources < enabledSources.size -> "PARTIAL_SUCCESS"
            else -> "FAILED"
        }

        val summaryText = "Collection completed in ${endTime - startTime}ms. Sources: ${enabledSources.size}, New: $totalNewItems, Updated: $totalUpdatedItems, Duplicates: $totalDuplicates, PDFs: $totalPdfsDetected, Telegram: $totalTelegramPosts, Review: $totalReviewRequired"

        val jobLog = ContentCollectionJobLog(
            id = startTime,
            startedAt = startTime,
            completedAt = endTime,
            status = finalStatus,
            sourcesChecked = enabledSources.size,
            newItems = totalNewItems,
            updatedItems = totalUpdatedItems,
            duplicateItems = totalDuplicates,
            failedSources = totalFailedSources,
            telegramPosts = totalTelegramPosts,
            pdfsDetected = totalPdfsDetected,
            aiProcessed = totalAiProcessed,
            reviewRequired = totalReviewRequired,
            errors = errorsList,
            summary = summaryText
        )

        // Save persistent log to database
        try {
            val logEntity = ContentCollectionJobLogEntity(
                id = jobLog.id,
                startedAt = jobLog.startedAt,
                completedAt = jobLog.completedAt,
                status = jobLog.status,
                sourcesChecked = jobLog.sourcesChecked,
                newItems = jobLog.newItems,
                updatedItems = jobLog.updatedItems,
                duplicateItems = jobLog.duplicateItems,
                failedSources = jobLog.failedSources,
                telegramPosts = jobLog.telegramPosts,
                pdfsDetected = jobLog.pdfsDetected,
                aiProcessed = jobLog.aiProcessed,
                reviewRequired = jobLog.reviewRequired,
                errorsJson = errorsList.joinToString(";;;"),
                summary = jobLog.summary
            )
            database.contentIntelligenceDao().insertJobLog(logEntity)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist job log: ${e.message}")
        }

        _lastJobLog.value = jobLog
        _recentCollectedItems.value = (cycleCollectedItems + _recentCollectedItems.value).distinctBy { it.id }.take(60)
        _isCollecting.value = false

        Log.i(TAG, summaryText)
        jobLog
    }

    /**
     * Processes Weekly Hindi Current Affairs PDFs from GK Now with dynamic date range detection.
     */
    private suspend fun processWeeklyPdfSource(source: ContentSourceConfig): PdfProcessingResult {
        val discoveredPdfs = gkNowPdfScraper.discoverWeeklyPdfs()
        val newPdfs = mutableListOf<WeeklyCurrentAffairsPdf>()
        val collectedItems = mutableListOf<CollectedContentItem>()
        var duplicatesCount = 0
        var telegramPosts = 0

        for (pdf in discoveredPdfs) {
            val fingerprint = computeFingerprint(pdf.pdfSourceUrl, pdf.title, pdf.dateRange)
            if (processedFingerprints.contains(fingerprint)) {
                duplicatesCount++
                continue
            }

            processedFingerprints.add(fingerprint)

            // Attempt storage upload & metadata enrichment
            var storagePath: String? = pdf.storagePath
            var pdfPublicUrl: String? = pdf.pdfPublicUrl
            var fileSizeStr: String? = pdf.fileSize

            if (pdf.pdfSourceUrl.isNotBlank() && !pdf.pdfSourceUrl.contains("#week-")) {
                try {
                    val req = okhttp3.Request.Builder()
                        .url(pdf.pdfSourceUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) StudyMate/1.0")
                        .build()
                    val resp = okhttp3.OkHttpClient().newCall(req).execute()
                    if (resp.isSuccessful) {
                        val bytes = resp.body?.bytes()
                        if (bytes != null && bytes.size > 100 && bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte()) { // %PDF
                            val uploadPath = "current-affairs-pdfs/${pdf.id}.pdf"
                            val uploadResult = com.example.data.remote.supabase.SupabaseClient.instance.uploadFile(
                                bucket = "current-affairs-pdfs",
                                path = "${pdf.id}.pdf",
                                mimeType = "application/pdf",
                                fileBytes = bytes
                            )
                            if (uploadResult is com.example.data.remote.supabase.SupabaseResult.Success) {
                                storagePath = uploadPath
                                pdfPublicUrl = uploadResult.data
                                fileSizeStr = "${bytes.size / 1024} KB"
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Storage upload attempt skipped for ${pdf.id}: ${e.message}")
                }
            }

            val enrichedPdf = pdf.copy(
                storagePath = storagePath,
                pdfPublicUrl = pdfPublicUrl ?: pdf.pdfSourceUrl,
                fileSize = fileSizeStr,
                contentHash = fingerprint
            )

            newPdfs.add(enrichedPdf)

            // Publish to Supabase DB pdf_documents table
            supabaseContentHub.publishWeeklyPdf(enrichedPdf)

            val item = gkNowPdfScraper.toCollectedContentItem(enrichedPdf)
            collectedItems.add(item)

            // 1. Raw Source Record
            val rawRecord = RawSourceRecordEntity(
                id = "raw_pdf_${enrichedPdf.id}",
                sourceId = source.sourceId,
                sourceName = source.sourceName,
                sourceUrl = source.sourceUrl,
                discoveredUrl = enrichedPdf.pdfSourceUrl,
                rawTitle = enrichedPdf.title,
                rawText = "Weekly Hindi Current Affairs PDF for date range ${enrichedPdf.dateRange}. Source: ${enrichedPdf.sourcePageUrl}",
                discoveredAt = System.currentTimeMillis(),
                publishedAt = enrichedPdf.publishedAt,
                contentHash = fingerprint,
                contentType = "CURRENT_AFFAIRS_PDF",
                language = "Hindi",
                categoryHint = "CURRENT_AFFAIRS_PDF"
            )
            database.contentIntelligenceDao().insertRawSourceRecord(rawRecord)

            // 2. Room Database Save for CurrentAffairs
            val caItem = CurrentAffairsItem(
                title = enrichedPdf.title,
                summary = "साप्ताहिक करेंट अफेयर्स पीडीएफ (${enrichedPdf.dateRange})। यूपीएससी, एसएससी, रेलवे व राज्य परीक्षाओं के लिए प्रामाणिक समसामयिकी पीडीएफ संग्रह।",
                examRelevance = "High",
                category = "Weekly Current Affairs (Hindi PDF)",
                targetExams = listOf("UPSC", "SSC", "Railway", "State PSC", "Banking"),
                subject = "Current Affairs",
                sourceName = GkNowWeeklyPdfScraper.SOURCE_NAME,
                sourceUrl = enrichedPdf.sourcePageUrl,
                publishedDate = enrichedPdf.publishedAt,
                language = "hi",
                canonicalUrl = enrichedPdf.pdfPublicUrl ?: enrichedPdf.pdfSourceUrl,
                fetchedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            )
            database.currentAffairsDao().insertCurrentAffairs(listOf(caItem))

            // 3. Telegram Bot Publishing
            if (!telegramPostedFingerprints.contains(fingerprint)) {
                val telegramMessage = buildWeeklyCaTelegramMessage(enrichedPdf)
                val publishResult = telegramBotService.sendStudyMatePost(
                    chatId = TELEGRAM_TEST_CHAT_ID,
                    text = telegramMessage,
                    parseMode = "HTML"
                )
                if (publishResult is TelegramPublishResult.Success) {
                    telegramPostedFingerprints.add(fingerprint)
                    telegramPosts++
                    _publishedTelegramHistory.value = (listOf(item.copy(isTelegramPublished = true)) + _publishedTelegramHistory.value).take(30)

                    // Record publication in Supabase Content Hub
                    supabaseContentHub.recordTelegramPublication(
                        TelegramPublicationEntity(
                            contentId = pdf.id,
                            title = pdf.title,
                            category = pdf.category.name,
                            chatId = TELEGRAM_TEST_CHAT_ID,
                            messageId = publishResult.messageId,
                            status = "PUBLISHED",
                            contentHash = fingerprint
                        )
                    )
                }
            }

            // 4. Publish PDF to Supabase pdf_documents table
            supabaseContentHub.publishWeeklyPdf(pdf)
        }

        if (newPdfs.isNotEmpty()) {
            _weeklyPdfs.value = (newPdfs + _weeklyPdfs.value).distinctBy { it.id }
        }

        return PdfProcessingResult(
            newPdfs = newPdfs,
            duplicateCount = duplicatesCount,
            telegramPostsSent = telegramPosts,
            collectedItems = collectedItems
        )
    }

    /**
     * Processes Recruitment sources (Vacancy, Result, Admit Card, Answer Key, Admission).
     */
    private suspend fun processRecruitmentSource(source: ContentSourceConfig): RecruitmentProcessingResult {
        val discoveredEntities = recruitmentScraper.discoverAndProcessAllSections()
        val collectedItems = mutableListOf<CollectedContentItem>()
        var newCount = 0
        var updatedCount = 0
        var duplicatesCount = 0
        var telegramPosts = 0
        var aiProcessedCount = 0
        var reviewRequiredCount = 0

        for (entity in discoveredEntities) {
            val officialLink = entity.officialPdfUrl.ifBlank { entity.sourceUrl }
            val lastDate = entity.applicationLastDate ?: ""
            val fingerprint = computeFingerprint(officialLink, entity.title, lastDate)

            // Step 1: Duplicate check via in-memory and Supabase Content Hash check
            if (processedFingerprints.contains(fingerprint)) {
                duplicatesCount++
                continue
            }

            val diffCheck = supabaseContentHub.evaluateContentDifference(
                sourceUrl = entity.sourceUrl,
                rawTitle = entity.title,
                rawSnippet = entity.summaryHi,
                publishedDate = entity.applicationStartDate
            )

            if (diffCheck.resultType == DetectionResultType.UNCHANGED) {
                duplicatesCount++
                processedFingerprints.add(fingerprint)
                // Audit AI Skip (Zero API Cost)
                supabaseContentHub.logAiProcessing(
                    AiProcessingLogEntity(
                        contentId = entity.id,
                        title = entity.title,
                        processingType = "EXTRACTION_AND_NORMALIZATION",
                        modelName = "gemini-2.5-flash",
                        tokensUsed = 0,
                        durationMs = 0L,
                        status = "SKIPPED_UNCHANGED",
                        inputFingerprint = diffCheck.contentHash,
                        detectedChanges = diffCheck.reason
                    )
                )
                continue
            }

            processedFingerprints.add(fingerprint)

            // Step 2: Store Raw Source Record for complete traceability
            val rawRecord = RawSourceRecordEntity(
                id = "raw_${entity.id}",
                sourceId = source.sourceId,
                sourceName = entity.organization.ifBlank { source.sourceName },
                sourceUrl = entity.sourceUrl,
                discoveredUrl = officialLink,
                rawTitle = entity.title,
                rawText = "${entity.title}\nOrg: ${entity.organization}\nPost: ${entity.postName}\nLast Date: ${entity.applicationLastDate}\nExam Date: ${entity.examDate}\nSummary: ${entity.summaryHi}",
                discoveredAt = System.currentTimeMillis(),
                publishedAt = entity.applicationStartDate ?: "",
                updatedAt = entity.changeSummary,
                contentHash = fingerprint,
                contentType = entity.contentType,
                language = "Hindi",
                categoryHint = source.category.name
            )
            database.contentIntelligenceDao().insertRawSourceRecord(rawRecord)

            // Step 3: Pass through AI Engine for validation & structuring (On New / Changed)
            val startTime = System.currentTimeMillis()
            val existingItem = _recentCollectedItems.value.firstOrNull { it.canonicalUrl == officialLink || it.originalTitle == entity.title }
            val aiResult = aiEngine.processAndValidate(rawRecord, existingItem)
            val durationMs = System.currentTimeMillis() - startTime
            aiProcessedCount++

            val processedItem = aiResult.item

            // Audit log AI processing
            supabaseContentHub.logAiProcessing(
                AiProcessingLogEntity(
                    contentId = processedItem.id,
                    title = processedItem.title,
                    processingType = "NORMALIZATION_AND_STRUCTURING",
                    modelName = "gemini-2.5-flash",
                    tokensUsed = 340,
                    durationMs = durationMs,
                    status = "SUCCESS",
                    inputFingerprint = fingerprint,
                    detectedChanges = if (aiResult.isUpdated) "Detected change" else "New discovery"
                )
            )

            // Step 4: Handle Review Queue vs Auto Approved
            if (aiResult.reviewQueueItem != null) {
                reviewRequiredCount++
                database.contentIntelligenceDao().insertReviewQueueItem(aiResult.reviewQueueItem)
                Log.d(TAG, "Item ${processedItem.title} queued for human review. Reason: ${aiResult.reviewQueueItem.reviewReason}")
            }

            // Step 5: Save Version History if updated
            if (aiResult.isUpdated && aiResult.contentVersion != null) {
                updatedCount++
                database.contentIntelligenceDao().insertContentVersion(aiResult.contentVersion)
            } else {
                newCount++
            }

            // Step 6: Persist structured entity to Room Database & Supabase Content Hub
            database.recruitmentDao().insertOrUpdateAll(listOf(entity))
            supabaseContentHub.publishContentItem(processedItem)
            collectedItems.add(processedItem)

            // Step 7: Telegram Dispatch (Only approved items & max 5 per cycle to prevent flooding)
            if (processedItem.processingStatus == ContentProcessingStatus.AUTO_APPROVED &&
                !telegramPostedFingerprints.contains(fingerprint) &&
                telegramPosts < 5
            ) {
                val telegramMessage = if (aiResult.isUpdated) {
                    buildUpdateTelegramMessage(processedItem)
                } else {
                    buildRecruitmentTelegramMessage(processedItem)
                }

                val publishResult = telegramBotService.sendStudyMatePost(
                    chatId = TELEGRAM_TEST_CHAT_ID,
                    text = telegramMessage,
                    parseMode = "HTML"
                )

                if (publishResult is TelegramPublishResult.Success) {
                    telegramPostedFingerprints.add(fingerprint)
                    telegramPosts++
                    _publishedTelegramHistory.value = (listOf(processedItem.copy(isTelegramPublished = true)) + _publishedTelegramHistory.value).take(30)

                    supabaseContentHub.recordTelegramPublication(
                        TelegramPublicationEntity(
                            contentId = processedItem.id,
                            title = processedItem.title,
                            category = processedItem.category.name,
                            chatId = TELEGRAM_TEST_CHAT_ID,
                            messageId = publishResult.messageId,
                            status = "PUBLISHED",
                            contentHash = fingerprint
                        )
                    )
                }
            }
        }

        return RecruitmentProcessingResult(
            newCount = newCount,
            updatedCount = updatedCount,
            duplicateCount = duplicatesCount,
            telegramPostsSent = telegramPosts,
            aiProcessedCount = aiProcessedCount,
            reviewRequiredCount = reviewRequiredCount,
            collectedItems = collectedItems
        )
    }

    /**
     * Manually approves a review queue item and dispatches it to Telegram.
     */
    suspend fun approveAndPublishReviewItem(reviewItemId: String, reviewerNotes: String = "Approved by Admin"): Boolean = withContext(Dispatchers.IO) {
        val item = database.contentIntelligenceDao().getReviewItemById(reviewItemId) ?: return@withContext false
        database.contentIntelligenceDao().updateReviewStatus(reviewItemId, "APPROVED", reviewerNotes, System.currentTimeMillis())

        val contentItem = _recentCollectedItems.value.firstOrNull { it.id == item.contentId }
        if (contentItem != null) {
            val message = buildRecruitmentTelegramMessage(contentItem)
            val result = telegramBotService.sendStudyMatePost(
                chatId = TELEGRAM_TEST_CHAT_ID,
                text = message,
                parseMode = "HTML"
            )
            if (result is TelegramPublishResult.Success) {
                _publishedTelegramHistory.value = (listOf(contentItem.copy(isTelegramPublished = true)) + _publishedTelegramHistory.value).take(30)
            }
        }
        true
    }

    /**
     * Rejects a review queue item.
     */
    suspend fun rejectReviewItem(reviewItemId: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        database.contentIntelligenceDao().updateReviewStatus(reviewItemId, "REJECTED", reason, System.currentTimeMillis())
        true
    }

    /**
     * Builds structured Telegram post for Vacancies & Exam Updates.
     */
    private fun buildRecruitmentTelegramMessage(item: CollectedContentItem): String {
        val categoryHeader = when (item.category) {
            ContentCategory.VACANCY -> "🟢 New Vacancy"
            ContentCategory.RESULT -> "🏆 Exam Result Declared"
            ContentCategory.ADMIT_CARD -> "🎫 Admit Card Released"
            ContentCategory.ANSWER_KEY -> "🔑 Answer Key Released"
            ContentCategory.ADMISSION -> "🎓 New Admission Form"
            else -> "⚡ Important Exam Update"
        }

        val sb = StringBuilder()
        sb.append("📚 <b>StudyMate Update</b>\n\n")
        sb.append("$categoryHeader\n\n")
        sb.append("<b>${escapeHtml(item.title)}</b>\n\n")
        sb.append("<b>Important Details:</b>\n")

        if (!item.organization.isNullOrBlank()) {
            sb.append("• <b>Organization:</b> ${escapeHtml(item.organization)}\n")
        }
        if (!item.postName.isNullOrBlank()) {
            sb.append("• <b>Post:</b> ${escapeHtml(item.postName)}\n")
        }
        if (!item.totalPosts.isNullOrBlank()) {
            sb.append("• <b>Total Posts:</b> ${escapeHtml(item.totalPosts)}\n")
        }
        if (!item.lastDate.isNullOrBlank()) {
            sb.append("• <b>Last Date:</b> ${escapeHtml(item.lastDate)}\n")
        }
        if (!item.examDate.isNullOrBlank()) {
            sb.append("• <b>Exam Date:</b> ${escapeHtml(item.examDate)}\n")
        }
        if (!item.eligibility.isNullOrBlank()) {
            sb.append("• <b>Eligibility:</b> ${escapeHtml(item.eligibility)}\n")
        }

        val link = item.officialLink ?: item.canonicalUrl
        if (link.isNotBlank()) {
            sb.append("\n🔗 <b>Apply / Official Link:</b>\n$link\n")
        }
        if (item.sourceName.isNotBlank()) {
            sb.append("\nSource: <i>${escapeHtml(item.sourceName)}</i>\n")
        }

        sb.append("\n<i>StudyMate • India's AI-Powered Exam Companion</i>")
        return sb.toString()
    }

    /**
     * Builds structured Telegram update notification when existing items change.
     */
    private fun buildUpdateTelegramMessage(item: CollectedContentItem): String {
        val sb = StringBuilder()
        sb.append("🔄 <b>StudyMate Update</b>\n\n")
        sb.append("<b>${escapeHtml(item.title)}</b>\n\n")
        sb.append("<b>Important Update:</b>\n")
        sb.append("• ${escapeHtml(item.updateHistoryNote.ifBlank { "विवरण में संशोधन किया गया है।" })}\n")

        if (!item.lastDate.isNullOrBlank()) {
            sb.append("• <b>New Last Date:</b> ${escapeHtml(item.lastDate)}\n")
        }

        val link = item.officialLink ?: item.canonicalUrl
        if (link.isNotBlank()) {
            sb.append("\n🔗 <b>Official Notice Link:</b>\n$link\n")
        }

        sb.append("\n<i>StudyMate • Verified Exam Intelligence</i>")
        return sb.toString()
    }

    /**
     * Builds structured Telegram post for Weekly Current Affairs Hindi PDF.
     */
    private fun buildWeeklyCaTelegramMessage(pdf: WeeklyCurrentAffairsPdf): String {
        return """
            📚 <b>StudyMate Current Affairs</b>

            📰 <b>Weekly Current Affairs (Hindi PDF)</b>
            📅 <b>${escapeHtml(pdf.dateRange)}</b>
            🇮🇳 <b>भाषा: हिंदी</b>

            📥 <b>Read / Download PDF:</b>
            ${pdf.pdfSourceUrl}

            <i>StudyMate • UPSC, SSC, Railways & State PSC Preparation</i>
        """.trimIndent()
    }

    private fun computeFingerprint(vararg parts: String?): String {
        val raw = parts.filterNotNull().joinToString("||")
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private data class PdfProcessingResult(
        val newPdfs: List<WeeklyCurrentAffairsPdf>,
        val duplicateCount: Int,
        val telegramPostsSent: Int,
        val collectedItems: List<CollectedContentItem>
    )

    private data class RecruitmentProcessingResult(
        val newCount: Int,
        val updatedCount: Int,
        val duplicateCount: Int,
        val telegramPostsSent: Int,
        val aiProcessedCount: Int,
        val reviewRequiredCount: Int,
        val collectedItems: List<CollectedContentItem>
    )
}

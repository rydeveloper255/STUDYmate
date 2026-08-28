package com.example.data.remote.supabase

import android.util.Log
import com.example.data.local.ContentIntelligenceDao
import com.example.data.local.RecruitmentDao
import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import com.example.data.model.content.*
import com.example.data.persistence.PersistenceMonitor
import com.example.data.persistence.PersistenceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * SupabaseContentHubService (Step 64 Implementation)
 *
 * Establishes Supabase as the SINGLE SOURCE OF TRUTH for all collected & processed educational/exam content:
 * - Content Hub Table: `content_items`, `source_items`, `content_versions`, `ai_processing_logs`, `pdf_documents`, `telegram_publications`, `content_review_queue`
 * - AI Cost Optimization: Verifies fingerprints against Supabase & Room. If content hash is unchanged, skips AI calls.
 * - Client Fast Content Delivery: StudyMate UI directly reads approved records from Supabase / Room without triggering scraping or AI.
 * - Complete Observability: Audits pipeline metrics, AI skip rates, and Telegram dispatch histories.
 */
class SupabaseContentHubService(
    private val supabaseClient: SupabaseClient = SupabaseClient.instance,
    private val database: StudyMateDatabase? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "SupabaseContentHub"
        const val TABLE_CONTENT_SOURCES = "content_sources"
        const val TABLE_SOURCE_ITEMS = "source_items"
        const val TABLE_CONTENT_ITEMS = "content_items"
        const val TABLE_CONTENT_VERSIONS = "content_versions"
        const val TABLE_PDF_DOCUMENTS = "pdf_documents"
        const val TABLE_PROCESSING_JOBS = "processing_jobs"
        const val TABLE_AI_LOGS = "ai_processing_logs"
        const val TABLE_REVIEW_QUEUE = "content_review_queue"
        const val TABLE_TELEGRAM_PUBS = "telegram_publications"
        const val TABLE_SOURCE_REFERENCES = "content_source_references"
    }

    private val contentDao: ContentIntelligenceDao? = database?.contentIntelligenceDao()
    private val recruitmentDao: RecruitmentDao? = database?.recruitmentDao()

    private val _hubSyncStatus = MutableStateFlow<SupabaseSyncStatus>(SupabaseSyncStatus.Idle)
    val hubSyncStatus: StateFlow<SupabaseSyncStatus> = _hubSyncStatus.asStateFlow()

    private val _aiOptimizationStats = MutableStateFlow(AiOptimizationStats())
    val aiOptimizationStats: StateFlow<AiOptimizationStats> = _aiOptimizationStats.asStateFlow()

    data class AiOptimizationStats(
        val totalDiscovered: Int = 0,
        val aiCallsExecuted: Int = 0,
        val aiCallsSavedUnchanged: Int = 0,
        val totalTokensUsed: Int = 0,
        val lastSyncTimestamp: Long = System.currentTimeMillis(),
        val telegramDispatches: Int = 0,
        val activeSourcesCount: Int = 0
    ) {
        val aiSavingsPercentage: Int
            get() {
                val total = aiCallsExecuted + aiCallsSavedUnchanged
                return if (total > 0) ((aiCallsSavedUnchanged.toFloat() / total) * 100).toInt() else 100
            }
    }

    init {
        refreshOptimizationStats()
    }

    fun refreshOptimizationStats() {
        coroutineScope.launch {
            try {
                val skipped = contentDao?.getSkippedAiCallsCount() ?: 0
                val success = contentDao?.getSuccessfulAiCallsCount() ?: 0
                _aiOptimizationStats.value = _aiOptimizationStats.value.copy(
                    aiCallsExecuted = success,
                    aiCallsSavedUnchanged = skipped,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh stats: ${e.message}")
            }
        }
    }

    // =========================================================================
    // 1. FAST CLIENT CONTENT DELIVERY (READ FROM SUPABASE / ROOM)
    // =========================================================================

    /**
     * Fetches approved content from Supabase single source of truth.
     * Caches in local Room database for fast offline-first display.
     * DOES NOT invoke AI or external scraping.
     */
    suspend fun fetchApprovedContent(
        category: String? = null,
        state: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<RecruitmentEntity>> = withContext(Dispatchers.IO) {
        try {
            _hubSyncStatus.value = SupabaseSyncStatus.Syncing
            PersistenceMonitor.updateStatus(PersistenceStatus.Syncing)

            val queryParams = mutableMapOf(
                "select" to "*",
                "order" to "detected_at.desc",
                "limit" to limit.toString(),
                "offset" to offset.toString()
            )

            // Read only approved items
            queryParams["processing_status"] = "in.(AUTO_APPROVED,APPROVED)"

            if (!category.isNullOrBlank() && category != "ALL" && category != "All India") {
                queryParams["category"] = "eq.$category"
            }

            if (!state.isNullOrBlank() && state != "All India") {
                queryParams["state"] = "eq.$state"
            }

            val result = supabaseClient.from(TABLE_CONTENT_ITEMS).select(queryParams)

            if (result is SupabaseResult.Success) {
                val jsonArray = JSONArray(result.data)
                val items = mutableListOf<RecruitmentEntity>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val entity = parseJsonToRecruitmentEntity(obj)
                    items.add(entity)
                }

                if (items.isNotEmpty()) {
                    recruitmentDao?.insertOrUpdateAll(items)
                }

                _hubSyncStatus.value = SupabaseSyncStatus.Success("Loaded ${items.size} verified items from Content Hub")
                PersistenceMonitor.updateStatus(PersistenceStatus.Saved(message = "✓ Content Hub in sync"))
                Result.success(items)
            } else {
                // Fallback gracefully to Room local database
                val localItems = recruitmentDao?.getAllOnce() ?: emptyList()
                _hubSyncStatus.value = SupabaseSyncStatus.Success("Serving ${localItems.size} local cached items")
                PersistenceMonitor.updateStatus(PersistenceStatus.Offline(message = "Offline — using local database"))
                Result.success(localItems)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching from Supabase Content Hub: ${e.message}")
            val localItems = recruitmentDao?.getAllOnce() ?: emptyList()
            _hubSyncStatus.value = SupabaseSyncStatus.Error("Error connecting to Supabase: ${e.message}", e)
            Result.success(localItems)
        }
    }

    /**
     * Fetches Weekly Current Affairs PDFs from Supabase `pdf_documents`.
     */
    suspend fun fetchWeeklyPdfs(limit: Int = 30): List<WeeklyCurrentAffairsPdf> = withContext(Dispatchers.IO) {
        try {
            val result = supabaseClient.from(TABLE_PDF_DOCUMENTS).select(
                mapOf("select" to "*", "order" to "created_at.desc", "limit" to limit.toString())
            )
            if (result is SupabaseResult.Success) {
                val array = JSONArray(result.data)
                val list = mutableListOf<WeeklyCurrentAffairsPdf>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val storagePath = if (obj.has("pdf_storage_path")) obj.optString("pdf_storage_path", null) else obj.optString("storage_path", null)
                    val pdfPublicUrl = if (obj.has("pdf_public_url")) obj.optString("pdf_public_url", null) else if (!storagePath.isNullOrBlank()) supabaseClient.getPublicUrl("current-affairs-pdfs", storagePath) else null
                    
                    list.add(
                        WeeklyCurrentAffairsPdf(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", "Weekly Current Affairs PDF"),
                            description = obj.optString("description", "साप्ताहिक करेंट अफेयर्स पीडीएफ संग्रह"),
                            dateRange = if (obj.has("date_range")) obj.optString("date_range", "") else obj.optString("published_date", ""),
                            category = ContentCategory.CURRENT_AFFAIRS_PDF,
                            language = obj.optString("language", "Hindi"),
                            sourceName = obj.optString("source_name", "GK Now Hindi"),
                            sourcePageUrl = if (obj.has("source_url")) obj.optString("source_url", "") else obj.optString("source_page_url", "https://gknow.in/hi/weekly-current-affairs-pdf-in-hindi/"),
                            pdfSourceUrl = obj.optString("pdf_source_url", ""),
                            storagePath = storagePath,
                            pdfPublicUrl = pdfPublicUrl,
                            publishedAt = if (obj.has("published_date")) obj.optString("published_date", "") else obj.optString("published_at", ""),
                            detectedAt = if (obj.has("fetched_at")) obj.optLong("fetched_at", System.currentTimeMillis()) else obj.optLong("detected_at", System.currentTimeMillis()),
                            fileSize = obj.optString("file_size", null),
                            contentHash = obj.optString("content_hash", ""),
                            isAvailable = obj.optBoolean("is_available", true),
                            status = obj.optString("status", "AVAILABLE"),
                            createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updated_at", System.currentTimeMillis())
                        )
                    )
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading pdf_documents: ${e.message}")
            emptyList()
        }
    }

    // =========================================================================
    // 2. AI COST OPTIMIZATION: HASH DEDUPLICATION & CHANGE DETECTION
    // =========================================================================

    /**
     * Checks if content is truly new, updated, or unchanged.
     * Returns `DetectionResultType.UNCHANGED` if the content hash matches an existing record.
     */
    suspend fun evaluateContentDifference(
        sourceUrl: String,
        rawTitle: String,
        rawSnippet: String,
        publishedDate: String?
    ): ContentDifferenceCheck = withContext(Dispatchers.IO) {
        val rawHash = computeSha256(sourceUrl, rawTitle, rawSnippet, publishedDate ?: "")

        // 1. Check local Room database
        val existingRecord = contentDao?.getRawRecordByHash(rawHash)
        if (existingRecord != null) {
            return@withContext ContentDifferenceCheck(
                resultType = DetectionResultType.UNCHANGED,
                contentHash = rawHash,
                existingRecord = existingRecord,
                reason = "Exact content hash match found in database (AI skipped)"
            )
        }

        // 2. Check by canonical source URL for updates
        val existingByUrl = contentDao?.getRawRecordById("raw_${computeSha256(sourceUrl)}")
        if (existingByUrl != null && existingByUrl.contentHash != rawHash) {
            return@withContext ContentDifferenceCheck(
                resultType = DetectionResultType.UPDATED,
                contentHash = rawHash,
                existingRecord = existingByUrl,
                reason = "Existing URL modified — new content version detected"
            )
        }

        ContentDifferenceCheck(
            resultType = DetectionResultType.NEW,
            contentHash = rawHash,
            existingRecord = null,
            reason = "New content discovered"
        )
    }

    data class ContentDifferenceCheck(
        val resultType: DetectionResultType,
        val contentHash: String,
        val existingRecord: RawSourceRecordEntity?,
        val reason: String
    )

    // =========================================================================
    // 3. PIPELINE SYNC: PERSISTENCE TO SUPABASE HUB
    // =========================================================================

    /**
     * Publishes normalized, validated, approved content item to Supabase `content_items` & Room.
     */
    suspend fun publishContentItem(item: CollectedContentItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

            val itemJson = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("original_title", item.originalTitle)
                put("category", item.category.name)
                put("source_name", item.sourceName)
                put("source_url", item.sourceUrl)
                put("canonical_url", item.canonicalUrl)
                put("published_date", item.publishedDate)
                put("date_range", item.dateRange ?: "")
                put("organization", item.organization ?: "")
                put("post_name", item.postName ?: "")
                put("last_date", item.lastDate ?: "")
                put("exam_date", item.examDate ?: "")
                put("eligibility", item.eligibility ?: "")
                put("total_posts", item.totalPosts ?: "")
                put("application_fee", item.applicationFee ?: "")
                put("age_limit", item.ageLimit ?: "")
                put("selection_process", item.selectionProcess ?: "")
                put("official_link", item.officialLink ?: "")
                put("pdf_url", item.pdfUrl ?: "")
                put("summary", item.summary)
                put("content_fingerprint", item.contentFingerprint)
                put("is_verified", item.isVerified)
                put("is_telegram_published", item.isTelegramPublished)
                put("telegram_message_id", item.telegramMessageId ?: JSONObject.NULL)
                put("detected_at", item.detectedAt)
                put("status_intelligence", item.statusIntelligence.name)
                put("processing_status", item.processingStatus.name)
                put("is_updated", item.isUpdated)
                put("update_history_note", item.updateHistoryNote)
                put("state", "All India")
                put("updated_at", isoNow)
            }

            // 1. Supabase Upsert
            supabaseClient.from(TABLE_CONTENT_ITEMS).upsert(
                jsonBody = itemJson.toString(),
                onConflict = "id"
            )

            // 2. Mirror into Room
            val entity = parseCollectedItemToRecruitmentEntity(item)
            recruitmentDao?.insertOrUpdate(entity)

            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed publishing content item to Supabase: ${e.message}")
            false
        }
    }

    /**
     * Publishes Weekly Current Affairs PDF to Supabase `pdf_documents`.
     */
    suspend fun publishWeeklyPdf(pdf: WeeklyCurrentAffairsPdf): Boolean = withContext(Dispatchers.IO) {
        try {
            val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

            val pdfJson = JSONObject().apply {
                put("id", pdf.id)
                put("title", pdf.title)
                put("description", pdf.description)
                put("category", pdf.category.name)
                put("language", pdf.language)
                put("source_name", pdf.sourceName)
                put("source_url", pdf.sourcePageUrl)
                put("source_page_url", pdf.sourcePageUrl)
                put("pdf_source_url", pdf.pdfSourceUrl)
                put("pdf_storage_path", pdf.storagePath ?: JSONObject.NULL)
                put("storage_path", pdf.storagePath ?: JSONObject.NULL)
                put("pdf_public_url", pdf.pdfPublicUrl ?: JSONObject.NULL)
                put("published_date", pdf.publishedAt.ifBlank { pdf.dateRange })
                put("published_at", pdf.publishedAt.ifBlank { pdf.dateRange })
                put("date_range", pdf.dateRange)
                put("fetched_at", pdf.detectedAt)
                put("detected_at", pdf.detectedAt)
                put("file_size", pdf.fileSize ?: JSONObject.NULL)
                put("content_hash", pdf.contentHash)
                put("is_available", pdf.isAvailable)
                put("status", pdf.status)
                put("created_at", pdf.createdAt)
                put("updated_at", isoNow)
            }

            supabaseClient.from(TABLE_PDF_DOCUMENTS).upsert(
                jsonBody = pdfJson.toString(),
                onConflict = "id"
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed publishing PDF to Supabase: ${e.message}")
            false
        }
    }

    /**
     * Records AI Processing Log to Supabase `ai_processing_logs` and Room.
     * Complies with zero-credential policy (No API keys or sensitive data logged).
     */
    suspend fun logAiProcessing(log: AiProcessingLogEntity) = withContext(Dispatchers.IO) {
        try {
            contentDao?.insertAiProcessingLog(log)

            val logJson = JSONObject().apply {
                put("id", log.id)
                put("content_id", log.contentId)
                put("title", log.title)
                put("processing_type", log.processingType)
                put("model_name", log.modelName)
                put("tokens_used", log.tokensUsed)
                put("duration_ms", log.durationMs)
                put("status", log.status)
                put("input_fingerprint", log.inputFingerprint)
                put("detected_changes", log.detectedChanges)
                put("created_at", log.createdAt)
            }

            supabaseClient.from(TABLE_AI_LOGS).insert(
                jsonBody = logJson.toString(),
                returnRepresentation = false
            )
            refreshOptimizationStats()
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing AI log to Supabase: ${e.message}")
        }
    }

    /**
     * Records Telegram Publication in Supabase `telegram_publications` & Room for idempotency.
     */
    suspend fun recordTelegramPublication(pub: TelegramPublicationEntity) = withContext(Dispatchers.IO) {
        try {
            contentDao?.insertTelegramPublication(pub)

            val pubJson = JSONObject().apply {
                put("id", pub.id)
                put("content_id", pub.contentId)
                put("title", pub.title)
                put("category", pub.category)
                put("chat_id", pub.chatId)
                put("message_id", pub.messageId ?: JSONObject.NULL)
                put("published_at", pub.publishedAt)
                put("status", pub.status)
                put("content_hash", pub.contentHash)
            }

            supabaseClient.from(TABLE_TELEGRAM_PUBS).upsert(
                jsonBody = pubJson.toString(),
                onConflict = "id"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing Telegram publication record: ${e.message}")
        }
    }

    /**
     * Updates source health in Supabase `content_sources` table and Room.
     */
    suspend fun updateSourceStatus(source: ContentSourceConfig) = withContext(Dispatchers.IO) {
        try {
            contentDao?.insertContentSource(source)
            val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

            val sourceJson = JSONObject().apply {
                put("source_id", source.sourceId)
                put("source_name", source.sourceName)
                put("source_url", source.sourceUrl)
                put("category", source.category.name)
                put("source_type", source.sourceType.name)
                put("enabled", source.enabled)
                put("priority", source.priority)
                put("last_checked_at", source.lastCheckedAt ?: JSONObject.NULL)
                put("last_success_at", source.lastSuccessAt ?: JSONObject.NULL)
                put("last_error", source.lastError ?: JSONObject.NULL)
                put("consecutive_failures", source.consecutiveFailures)
                put("items_discovered_count", source.itemsDiscoveredCount)
                put("status", source.status.name)
                put("updated_at", isoNow)
            }

            supabaseClient.from(TABLE_CONTENT_SOURCES).upsert(
                jsonBody = sourceJson.toString(),
                onConflict = "source_id"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed updating source status in Supabase: ${e.message}")
        }
    }

    /**
     * Publishes raw discovered item to Supabase `source_items` table before AI normalization.
     */
    suspend fun publishRawSourceItem(record: RawSourceRecordEntity) = withContext(Dispatchers.IO) {
        try {
            contentDao?.insertRawSourceRecord(record)
            val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

            val json = JSONObject().apply {
                put("id", record.id)
                put("source_id", record.sourceId)
                put("source_name", record.sourceName)
                put("source_url", record.sourceUrl)
                put("canonical_url", record.discoveredUrl)
                put("title", record.rawTitle)
                put("raw_content", record.rawText)
                put("content_type", record.contentType)
                put("language", record.language)
                put("category_hint", record.categoryHint)
                put("content_hash", record.contentHash)
                put("published_at", record.publishedAt)
                put("discovered_at", record.discoveredAt)
                put("updated_at", isoNow)
            }

            supabaseClient.from(TABLE_SOURCE_ITEMS).upsert(
                jsonBody = json.toString(),
                onConflict = "id"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed publishing raw source item to Supabase: ${e.message}")
        }
    }

    /**
     * Publishes content version history to Supabase `content_versions` table.
     */
    suspend fun publishContentVersion(version: ContentVersionEntity) = withContext(Dispatchers.IO) {
        try {
            contentDao?.insertContentVersion(version)
            val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

            val json = JSONObject().apply {
                put("id", version.versionId)
                put("content_id", version.contentId)
                put("version_number", version.versionNumber)
                put("changed_fields_json", version.changedFieldsJson)
                put("change_summary", version.changeSummary)
                put("recorded_at", version.recordedAt)
                put("source_attribution", version.sourceAttribution)
                put("created_at", isoNow)
            }

            supabaseClient.from(TABLE_CONTENT_VERSIONS).upsert(
                jsonBody = json.toString(),
                onConflict = "id"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed publishing content version to Supabase: ${e.message}")
        }
    }

    /**
     * Logs 3-hour / manual collection job executions to Supabase `processing_jobs` table.
     */
    suspend fun logProcessingJob(job: ContentCollectionJobLogEntity) = withContext(Dispatchers.IO) {
        try {
            contentDao?.insertJobLog(job)
            val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

            val json = JSONObject().apply {
                put("id", job.id)
                put("started_at", job.startedAt)
                put("completed_at", job.completedAt)
                put("status", job.status)
                put("sources_checked", job.sourcesChecked)
                put("new_items", job.newItems)
                put("updated_items", job.updatedItems)
                put("duplicate_items", job.duplicateItems)
                put("failed_sources", job.failedSources)
                put("telegram_posts", job.telegramPosts)
                put("pdfs_detected", job.pdfsDetected)
                put("ai_processed", job.aiProcessed)
                put("review_required", job.reviewRequired)
                put("summary", job.summary)
                put("errors_json", job.errorsJson)
                put("created_at", isoNow)
            }

            supabaseClient.from(TABLE_PROCESSING_JOBS).upsert(
                jsonBody = json.toString(),
                onConflict = "id"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed logging processing job to Supabase: ${e.message}")
        }
    }

    /**
     * Maps multiple external source URLs to a single canonical `content_item` in `content_source_references`.
     */
    suspend fun publishSourceReference(ref: ContentSourceReferenceEntity) = withContext(Dispatchers.IO) {
        try {
            contentDao?.insertSourceReference(ref)

            val json = JSONObject().apply {
                put("id", ref.id)
                put("content_id", ref.contentId)
                put("source_id", ref.sourceId)
                put("source_url", ref.sourceUrl)
                put("is_primary", ref.isPrimary)
                put("discovered_at", ref.discoveredAt)
            }

            supabaseClient.from(TABLE_SOURCE_REFERENCES).upsert(
                jsonBody = json.toString(),
                onConflict = "id"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed publishing content source reference: ${e.message}")
        }
    }

    /**
     * Publishes an item requiring human review to Supabase `content_review_queue`.
     */
    suspend fun publishReviewQueueItem(item: ReviewQueueItemEntity) = withContext(Dispatchers.IO) {
        try {
            contentDao?.insertReviewQueueItem(item)

            val json = JSONObject().apply {
                put("id", item.id)
                put("content_id", item.contentId)
                put("title", item.title)
                put("category", item.category)
                put("source_name", item.sourceName)
                put("original_link", item.originalLink)
                put("extracted_fields_json", item.extractedFieldsJson)
                put("ai_summary", item.aiSummary)
                put("detected_changes", item.detectedChanges)
                put("review_reason", item.reviewReason)
                put("status", item.status)
                put("created_at", item.createdAt)
            }

            supabaseClient.from(TABLE_REVIEW_QUEUE).upsert(
                jsonBody = json.toString(),
                onConflict = "id"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed publishing to review queue: ${e.message}")
        }
    }

    /**
     * Fetches a single canonical content item by ID without re-executing AI or web scraping.
     */
    suspend fun fetchContentItemById(id: String): Result<RecruitmentEntity?> = withContext(Dispatchers.IO) {
        try {
            val local = recruitmentDao?.getItemById(id)
            if (local != null) {
                return@withContext Result.success(local)
            }

            val queryParams = mapOf(
                "select" to "*",
                "id" to "eq.$id",
                "limit" to "1"
            )
            val result = supabaseClient.from(TABLE_CONTENT_ITEMS).select(queryParams)
            if (result is SupabaseResult.Success) {
                val jsonArray = JSONArray(result.data)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val entity = parseJsonToRecruitmentEntity(obj)
                    recruitmentDao?.insertOrUpdate(entity)
                    Result.success(entity)
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching content item by ID: ${e.message}")
            Result.success(recruitmentDao?.getItemById(id))
        }
    }

    // =========================================================================
    // 4. PARSING & HELPERS
    // =========================================================================

    private fun parseJsonToRecruitmentEntity(obj: JSONObject): RecruitmentEntity {
        val categoryStr = obj.optString("category", "VACANCY")
        val categoryEnum = runCatching { ContentCategory.valueOf(categoryStr) }.getOrDefault(ContentCategory.VACANCY)
        val contentType = when (categoryEnum) {
            ContentCategory.VACANCY -> RecruitmentContentType.VACANCY.name
            ContentCategory.RESULT -> RecruitmentContentType.RESULT.name
            ContentCategory.ADMIT_CARD -> RecruitmentContentType.ADMIT_CARD.name
            ContentCategory.ANSWER_KEY -> RecruitmentContentType.ANSWER_KEY.name
            ContentCategory.ADMISSION -> RecruitmentContentType.NOTIFICATION.name
            else -> RecruitmentContentType.EXAM_UPDATE.name
        }

        val totalPostsStr = obj.optString("total_posts", "")
        val totalPostsInt = totalPostsStr.filter { it.isDigit() }.toIntOrNull()

        return RecruitmentEntity(
            id = obj.optString("id", UUID.randomUUID().toString()),
            title = obj.optString("title", "Exam Update"),
            organization = obj.optString("organization", "Government Recruitment Board"),
            postName = obj.optString("post_name", "Various Posts"),
            examCategory = RecruitmentCategory.OTHER.name,
            state = obj.optString("state", "All India"),
            contentType = contentType,
            rawStatus = VacancyStatus.OPEN.name,
            totalVacancies = totalPostsInt,
            applicationStartDate = obj.optString("published_date", null).takeIf { !it.isNullOrBlank() },
            applicationLastDate = obj.optString("last_date", null).takeIf { !it.isNullOrBlank() },
            examDate = obj.optString("exam_date", null).takeIf { !it.isNullOrBlank() },
            feeDetails = obj.optString("application_fee", "Not specified"),
            salary = "Not specified",
            ageRelaxation = obj.optString("age_limit", "Not specified"),
            educationalQualification = obj.optString("eligibility", "Not specified"),
            selectionProcess = listOf(obj.optString("selection_process", "Written Exam & Interview")),
            officialPdfUrl = obj.optString("pdf_url", ""),
            officialSourceUrl = obj.optString("official_link", ""),
            applicationUrl = obj.optString("official_link", ""),
            sourceUrl = obj.optString("source_url", ""),
            summaryEn = obj.optString("summary", ""),
            summaryHi = obj.optString("summary", ""),
            changeSummary = obj.optString("update_history_note", ""),
            contentHash = obj.optString("content_fingerprint", "")
        )
    }

    private fun parseCollectedItemToRecruitmentEntity(item: CollectedContentItem): RecruitmentEntity {
        val contentType = when (item.category) {
            ContentCategory.VACANCY -> RecruitmentContentType.VACANCY.name
            ContentCategory.RESULT -> RecruitmentContentType.RESULT.name
            ContentCategory.ADMIT_CARD -> RecruitmentContentType.ADMIT_CARD.name
            ContentCategory.ANSWER_KEY -> RecruitmentContentType.ANSWER_KEY.name
            ContentCategory.ADMISSION -> RecruitmentContentType.NOTIFICATION.name
            else -> RecruitmentContentType.EXAM_UPDATE.name
        }

        val totalPostsInt = item.totalPosts?.filter { it.isDigit() }?.toIntOrNull()

        return RecruitmentEntity(
            id = item.id,
            title = item.title,
            organization = item.organization ?: "Government Recruitment Board",
            postName = item.postName ?: item.title,
            examCategory = RecruitmentCategory.OTHER.name,
            state = "All India",
            contentType = contentType,
            rawStatus = VacancyStatus.OPEN.name,
            totalVacancies = totalPostsInt,
            applicationStartDate = item.publishedDate.takeIf { it.isNotBlank() },
            applicationLastDate = item.lastDate?.takeIf { it.isNotBlank() },
            examDate = item.examDate?.takeIf { it.isNotBlank() },
            feeDetails = item.applicationFee ?: "Not specified",
            salary = "Not specified",
            ageRelaxation = item.ageLimit ?: "Not specified",
            educationalQualification = item.eligibility ?: "Not specified",
            selectionProcess = listOf(item.selectionProcess ?: "Written Exam / Interview"),
            officialPdfUrl = item.pdfUrl ?: "",
            officialSourceUrl = item.officialLink ?: "",
            applicationUrl = item.officialLink ?: item.canonicalUrl,
            sourceUrl = item.sourceUrl,
            summaryEn = item.summary,
            summaryHi = item.summary,
            changeSummary = item.updateHistoryNote,
            contentHash = item.contentFingerprint
        )
    }

    private fun computeSha256(vararg values: String): String {
        val input = values.joinToString("::")
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}

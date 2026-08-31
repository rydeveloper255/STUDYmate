package com.example.service.content.whatsapp

import android.util.Log
import com.example.data.local.RecruitmentDao
import com.example.data.local.StudyMateDatabase
import com.example.data.model.RecruitmentEntity
import com.example.data.model.VacancyStatus
import com.example.data.model.updates.LatestUpdateItem
import com.example.data.remote.supabase.LatestUpdatesRepository
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseResult
import com.example.service.collector.SourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * Step 82: Ingestion Service for StudyMate WhatsApp Channel content.
 * 
 * Pipeline:
 * WhatsApp Channel -> Source Ingestion -> Raw Content -> Basic Validation ->
 * Date Cutoff (>= 2026-08-01) -> Duplicate Check -> Category Processing -> Supabase -> StudyMate App
 */
class WhatsAppChannelIngestionService(
    private val fetcher: WhatsAppChannelFetcher = WhatsAppChannelFetcher(),
    private val supabaseClient: SupabaseClient = SupabaseClient.instance,
    private val recruitmentDao: RecruitmentDao? = null,
    private val sourceManager: SourceManager? = null
) {

    companion object {
        private const val TAG = "WhatsAppIngestion"
        const val TABLE_RAW_SOURCE_CONTENT = "raw_source_content"
        const val TABLE_LATEST_UPDATES = LatestUpdatesRepository.TABLE_LATEST_UPDATES
        private val IST_TIMEZONE = TimeZone.getTimeZone("Asia/Kolkata")
    }

    private val processedHashes = ConcurrentHashMap.newKeySet<String>()
    private val inMemoryRecords = ConcurrentHashMap<String, LatestUpdateItem>()
    private val _recentLogs = MutableStateFlow<List<WhatsAppIngestionLog>>(emptyList())
    val recentLogs: StateFlow<List<WhatsAppIngestionLog>> = _recentLogs.asStateFlow()

    data class IngestionCycleResult(
        val success: Boolean,
        val status: WhatsAppIngestionStatus,
        val totalRawPosts: Int,
        val publishedCount: Int,
        val updatedCount: Int,
        val duplicateCount: Int,
        val ignoredBeforeCutoffCount: Int,
        val expiredCount: Int,
        val reviewRequiredCount: Int,
        val failedCount: Int,
        val processedItems: List<WhatsAppProcessedContent>,
        val errorMessage: String? = null
    )

    /**
     * Executes automated ingestion cycle from configured WhatsApp Channel.
     */
    suspend fun runIngestionCycle(
        channelUrl: String = WhatsAppSourceConfig.configuredChannelUrl
    ): IngestionCycleResult = withContext(Dispatchers.IO) {
        logStage(WhatsAppIngestionStage.FETCH_STARTED, "Connecting to WhatsApp source: $channelUrl")

        val fetchResult = fetcher.fetchChannelPosts(channelUrl)
        when (fetchResult) {
            is WhatsAppChannelFetcher.FetchResult.Unavailable -> {
                logStage(
                    WhatsAppIngestionStage.FETCH_FAILED,
                    "Source unavailable: ${fetchResult.reason}"
                )
                sourceManager?.recordSourceCheckResult(
                    sourceId = WhatsAppSourceConfig.SOURCE_ID,
                    isSuccess = false,
                    errorMessage = fetchResult.reason
                )
                IngestionCycleResult(
                    success = false,
                    status = WhatsAppIngestionStatus.SOURCE_UNAVAILABLE,
                    totalRawPosts = 0,
                    publishedCount = 0,
                    updatedCount = 0,
                    duplicateCount = 0,
                    ignoredBeforeCutoffCount = 0,
                    expiredCount = 0,
                    reviewRequiredCount = 0,
                    failedCount = 1,
                    processedItems = emptyList(),
                    errorMessage = fetchResult.reason
                )
            }

            is WhatsAppChannelFetcher.FetchResult.Failed -> {
                logStage(
                    WhatsAppIngestionStage.FAILED,
                    "Fetch error: ${fetchResult.error}"
                )
                sourceManager?.recordSourceCheckResult(
                    sourceId = WhatsAppSourceConfig.SOURCE_ID,
                    isSuccess = false,
                    errorMessage = fetchResult.error
                )
                IngestionCycleResult(
                    success = false,
                    status = WhatsAppIngestionStatus.FAILED,
                    totalRawPosts = 0,
                    publishedCount = 0,
                    updatedCount = 0,
                    duplicateCount = 0,
                    ignoredBeforeCutoffCount = 0,
                    expiredCount = 0,
                    reviewRequiredCount = 0,
                    failedCount = 1,
                    processedItems = emptyList(),
                    errorMessage = fetchResult.error
                )
            }

            is WhatsAppChannelFetcher.FetchResult.Success -> {
                logStage(
                    WhatsAppIngestionStage.FETCH_SUCCESS,
                    "Fetched ${fetchResult.posts.size} raw items. ${fetchResult.message}"
                )
                sourceManager?.recordSourceCheckResult(
                    sourceId = WhatsAppSourceConfig.SOURCE_ID,
                    isSuccess = true,
                    discoveredCount = fetchResult.posts.size
                )
                processRawPosts(fetchResult.posts)
            }
        }
    }

    /**
     * Core processing pipeline on a list of raw posts (used by real fetcher and unit test suite).
     */
    suspend fun processRawPosts(rawPosts: List<WhatsAppRawPost>): IngestionCycleResult = withContext(Dispatchers.IO) {
        var publishedCount = 0
        var updatedCount = 0
        var duplicateCount = 0
        var ignoredBeforeCutoff = 0
        var expiredCount = 0
        var reviewRequiredCount = 0
        var failedCount = 0
        val processedList = mutableListOf<WhatsAppProcessedContent>()

        val todayIso = getTodayIso()

        for (rawPost in rawPosts) {
            try {
                logStage(
                    WhatsAppIngestionStage.PROCESSING_STARTED,
                    "Processing post msg_id: ${rawPost.sourceMessageId ?: "unassigned"}"
                )

                // 1. Basic Content Extraction
                val extracted = WhatsAppContentExtractor.extractContent(rawPost)

                // 2. Validate Title
                if (extracted.title.isBlank() || extracted.title.equals("Official Update", ignoreCase = true) && rawPost.rawText.isBlank()) {
                    logStage(WhatsAppIngestionStage.VALIDATION_RESULT, "Missing or blank title. Route to REVIEW_REQUIRED.")
                    reviewRequiredCount++
                    continue
                }

                // 3. Date Cutoff Check (1 August 2026 Rule)
                val postDate = extracted.postDate ?: rawPost.sourcePostDate
                val cutoffStatus = WhatsAppSourceConfig.isEligibleByCutoff(postDate)

                if (cutoffStatus == false) {
                    logStage(
                        WhatsAppIngestionStage.VALIDATION_RESULT,
                        "Post date ($postDate) is before cutoff 2026-08-01. Ignored."
                    )
                    ignoredBeforeCutoff++
                    continue
                } else if (cutoffStatus == null) {
                    // Date unavailable: mark as DATE_UNAVAILABLE / REVIEW_REQUIRED
                    logStage(
                        WhatsAppIngestionStage.VALIDATION_RESULT,
                        "Post date is unavailable. Marked as REVIEW_REQUIRED."
                    )
                    val dateUnavailableItem = extracted.copy(
                        status = WhatsAppIngestionStatus.DATE_UNAVAILABLE,
                        isActive = false
                    )
                    persistRawRecord(rawPost, dateUnavailableItem)
                    reviewRequiredCount++
                    continue
                }

                logStage(
                    WhatsAppIngestionStage.CLASSIFICATION_RESULT,
                    "Category: ${extracted.category.name}, Org: ${extracted.organization ?: "Unknown"}"
                )

                // 4. Vacancy Expiry Validation
                var finalStatus = WhatsAppIngestionStatus.PUBLISHED
                var isActive = true

                if (extracted.category == WhatsAppCategory.VACANCY && !extracted.lastDate.isNullOrBlank()) {
                    val isExpired = isDateBeforeToday(extracted.lastDate, todayIso)
                    if (isExpired) {
                        finalStatus = WhatsAppIngestionStatus.EXPIRED
                        isActive = false
                        expiredCount++
                        logStage(
                            WhatsAppIngestionStage.VALIDATION_RESULT,
                            "Vacancy [${extracted.title}] last date (${extracted.lastDate}) has passed. Marked as EXPIRED (hidden from active feed)."
                        )
                    }
                }

                val processedItem = extracted.copy(
                    postDate = postDate,
                    status = finalStatus,
                    isActive = isActive
                )

                // 5. Deduplication & Updated Post Handling
                val isExisting = checkExistingRecord(processedItem)
                if (isExisting != null) {
                    if (isItemUpdated(isExisting, processedItem)) {
                        // Updated post: update existing record with new details (e.g. date extension)
                        val updatedItem = processedItem.copy(
                            id = isExisting.id,
                            updatedAt = System.currentTimeMillis()
                        )
                        saveOrUpdateRecord(rawPost, updatedItem, isUpdate = true)
                        updatedCount++
                        processedList.add(updatedItem)
                        logStage(
                            WhatsAppIngestionStage.SAVED,
                            "Updated existing record [${updatedItem.title}] with latest dates/details."
                        )
                    } else {
                        duplicateCount++
                        processedHashes.add(processedItem.contentHash)
                        logStage(
                            WhatsAppIngestionStage.DUPLICATE_DETECTED,
                            "Unchanged duplicate detected for hash: ${processedItem.contentHash.take(10)}"
                        )
                    }
                    continue
                }

                // 6. Save new validated record
                saveOrUpdateRecord(rawPost, processedItem, isUpdate = false)
                processedHashes.add(processedItem.contentHash)
                processedList.add(processedItem)

                if (processedItem.status == WhatsAppIngestionStatus.PUBLISHED) {
                    publishedCount++
                    logStage(
                        WhatsAppIngestionStage.PUBLISHED,
                        "Published ${processedItem.category.name}: ${processedItem.title}"
                    )
                } else if (processedItem.status == WhatsAppIngestionStatus.EXPIRED) {
                    logStage(
                        WhatsAppIngestionStage.SAVED,
                        "Saved expired record ${processedItem.category.name}: ${processedItem.title}"
                    )
                }
            } catch (e: Exception) {
                failedCount++
                logStage(
                    WhatsAppIngestionStage.FAILED,
                    "Error processing post: ${e.message}"
                )
            }
        }

        IngestionCycleResult(
            success = failedCount == 0,
            status = if (failedCount == 0) WhatsAppIngestionStatus.PUBLISHED else WhatsAppIngestionStatus.FAILED,
            totalRawPosts = rawPosts.size,
            publishedCount = publishedCount,
            updatedCount = updatedCount,
            duplicateCount = duplicateCount,
            ignoredBeforeCutoffCount = ignoredBeforeCutoff,
            expiredCount = expiredCount,
            reviewRequiredCount = reviewRequiredCount,
            failedCount = failedCount,
            processedItems = processedList
        )
    }

    private suspend fun checkExistingRecord(item: WhatsAppProcessedContent): LatestUpdateItem? {
        // In-memory lookup by contentHash, id, or external message ID
        val memCached = inMemoryRecords[item.contentHash]
            ?: inMemoryRecords[item.id]
            ?: item.sourceMessageId?.let { inMemoryRecords[it] }
        if (memCached != null) {
            return memCached
        }

        if (processedHashes.contains(item.contentHash)) {
            // Memory check with Room fallback
            val local = recruitmentDao?.getItemById(item.id)
            if (local != null) return LatestUpdateItem.fromRecruitmentEntity(local)
        }

        // Local DB check by ID or contentHash
        val local = recruitmentDao?.getItemById(item.id)
        if (local != null) {
            return LatestUpdateItem.fromRecruitmentEntity(local)
        }

        // Supabase lookup by content_hash or external_id
        try {
            val queryParams = mutableMapOf<String, String>()
            if (!item.sourceMessageId.isNullOrBlank()) {
                queryParams["external_id"] = "eq.${item.sourceMessageId}"
            } else {
                queryParams["content_hash"] = "eq.${item.contentHash}"
            }
            queryParams["limit"] = "1"

            val res = supabaseClient.from(TABLE_LATEST_UPDATES).select(queryParams)
            if (res is SupabaseResult.Success) {
                val array = org.json.JSONArray(res.data)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val parsed = parseSupabaseObjToItem(obj)
                    inMemoryRecords[parsed.id] = parsed
                    parsed.externalId?.let { inMemoryRecords[it] = parsed }
                    parsed.contentHash?.let { inMemoryRecords[it] = parsed }
                    return parsed
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking existing record in Supabase: ${e.message}")
        }
        return null
    }

    private fun isItemUpdated(existing: LatestUpdateItem, newContent: WhatsAppProcessedContent): Boolean {
        if (existing.lastDate != newContent.lastDate && !newContent.lastDate.isNullOrBlank()) {
            return true
        }
        if (existing.examDate != newContent.examDate && !newContent.examDate.isNullOrBlank()) {
            return true
        }
        if (existing.applyUrl != newContent.applyUrl && !newContent.applyUrl.isNullOrBlank()) {
            return true
        }
        if (existing.downloadUrl != newContent.pdfUrl && !newContent.pdfUrl.isNullOrBlank()) {
            return true
        }
        return false
    }

    private suspend fun saveOrUpdateRecord(
        rawPost: WhatsAppRawPost,
        content: WhatsAppProcessedContent,
        isUpdate: Boolean
    ) {
        // Cache in memory
        val latestItem = content.toLatestUpdateItem()
        inMemoryRecords[latestItem.id] = latestItem
        inMemoryRecords[content.contentHash] = latestItem
        if (!content.sourceMessageId.isNullOrBlank()) {
            inMemoryRecords[content.sourceMessageId] = latestItem
        }

        // 1. Save Raw Content record
        persistRawRecord(rawPost, content)

        // 2. Save Structured record to Local Room DB
        try {
            recruitmentDao?.insertOrUpdate(content.toRecruitmentEntity())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist to Room: ${e.message}")
        }

        // 3. Save Structured record to Supabase
        try {
            val latestItem = content.toLatestUpdateItem()
            val payload = JSONObject().apply {
                put("id", latestItem.id)
                put("update_type", latestItem.updateType)
                put("title", latestItem.title)
                put("short_description", latestItem.shortDescription)
                put("full_content", latestItem.fullContent)
                put("organization", latestItem.organization)
                put("exam_name", latestItem.examName)
                put("post_name", latestItem.postName)
                put("published_date", latestItem.publishedDate)
                put("start_date", latestItem.startDate)
                put("last_date", latestItem.lastDate)
                put("exam_date", latestItem.examDate)
                put("source_url", latestItem.sourceUrl)
                put("apply_url", latestItem.applyUrl)
                put("download_url", latestItem.downloadUrl)
                put("source_name", latestItem.sourceName)
                put("source_type", latestItem.sourceType)
                put("external_id", latestItem.externalId)
                put("content_hash", latestItem.contentHash)
                put("is_active", latestItem.isActive)

                val metaJson = JSONObject()
                latestItem.metadata.forEach { (k, v) -> metaJson.put(k, v) }
                put("metadata", metaJson)
            }

            if (isUpdate) {
                supabaseClient.from(TABLE_LATEST_UPDATES).update(
                    queryParams = mapOf("id" to "eq.${content.id}"),
                    jsonBody = payload.toString()
                )
            } else {
                supabaseClient.from(TABLE_LATEST_UPDATES).insert(
                    jsonBody = payload.toString()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist structured item to Supabase: ${e.message}")
        }
    }

    private suspend fun persistRawRecord(rawPost: WhatsAppRawPost, content: WhatsAppProcessedContent) {
        try {
            val rawPayload = JSONObject().apply {
                put("source_type", rawPost.sourceType)
                put("source_name", WhatsAppSourceConfig.SOURCE_NAME)
                put("source_url", rawPost.sourceUrl)
                put("source_message_id", rawPost.sourceMessageId)
                put("source_post_date", content.postDate)
                put("raw_text", rawPost.rawText)
                put("attachment_reference", rawPost.attachmentReference)
                put("source_timestamp", rawPost.sourceTimestamp)
                put("content_hash", content.contentHash)
                put("status", content.status.name)
            }

            supabaseClient.from(TABLE_RAW_SOURCE_CONTENT).insert(rawPayload.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Raw source persistence in Supabase skipped/failed: ${e.message}")
        }
    }

    private fun parseSupabaseObjToItem(obj: JSONObject): LatestUpdateItem {
        return LatestUpdateItem(
            id = obj.optString("id"),
            updateType = obj.optString("update_type", "vacancy"),
            title = obj.optString("title", ""),
            shortDescription = obj.optString("short_description", ""),
            fullContent = obj.optString("full_content", ""),
            organization = obj.optString("organization", ""),
            examName = obj.optString("exam_name", ""),
            postName = obj.optString("post_name", ""),
            publishedDate = obj.optString("published_date", null),
            startDate = obj.optString("start_date", null),
            lastDate = obj.optString("last_date", null),
            examDate = obj.optString("exam_date", null),
            sourceUrl = obj.optString("source_url", ""),
            applyUrl = obj.optString("apply_url", ""),
            downloadUrl = obj.optString("download_url", ""),
            sourceName = obj.optString("source_name", WhatsAppSourceConfig.SOURCE_NAME),
            sourceType = obj.optString("source_type", WhatsAppSourceConfig.SOURCE_TYPE),
            externalId = obj.optString("external_id", null),
            contentHash = obj.optString("content_hash", null),
            isActive = obj.optBoolean("is_active", true)
        )
    }

    private fun isDateBeforeToday(dateStr: String, todayIso: String): Boolean {
        val parsed = WhatsAppSourceConfig.parseDateSafely(dateStr) ?: return false
        val today = WhatsAppSourceConfig.parseDateSafely(todayIso) ?: return false
        return parsed.before(today)
    }

    private fun getTodayIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = IST_TIMEZONE
        }.format(Date())
    }

    private fun logStage(stage: WhatsAppIngestionStage, message: String, details: Map<String, String> = emptyMap()) {
        val log = WhatsAppIngestionLog(
            stage = stage,
            message = message,
            timestamp = System.currentTimeMillis(),
            details = details
        )
        Log.i(TAG, "[${stage.name}] $message")
        val current = _recentLogs.value
        _recentLogs.value = (listOf(log) + current).take(100)
    }
}

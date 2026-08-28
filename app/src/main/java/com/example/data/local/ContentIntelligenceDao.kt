package com.example.data.local

import androidx.room.*
import com.example.data.model.content.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentIntelligenceDao {

    // --- Raw Source Records ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawSourceRecord(record: RawSourceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawSourceRecords(records: List<RawSourceRecordEntity>)

    @Query("SELECT * FROM raw_source_records WHERE id = :id LIMIT 1")
    suspend fun getRawRecordById(id: String): RawSourceRecordEntity?

    @Query("SELECT * FROM raw_source_records WHERE contentHash = :hash LIMIT 1")
    suspend fun getRawRecordByHash(hash: String): RawSourceRecordEntity?

    @Query("SELECT * FROM raw_source_records ORDER BY discoveredAt DESC LIMIT :limit")
    fun getRecentRawRecords(limit: Int = 50): Flow<List<RawSourceRecordEntity>>

    // --- Content Versioning / Change History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentVersion(version: ContentVersionEntity)

    @Query("SELECT * FROM content_versions WHERE contentId = :contentId ORDER BY versionNumber DESC")
    fun getContentVersions(contentId: String): Flow<List<ContentVersionEntity>>

    @Query("SELECT * FROM content_versions WHERE contentId = :contentId ORDER BY versionNumber DESC")
    suspend fun getContentVersionsOnce(contentId: String): List<ContentVersionEntity>

    @Query("SELECT COUNT(*) FROM content_versions WHERE contentId = :contentId")
    suspend fun getVersionCount(contentId: String): Int

    // --- Review Queue ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewQueueItem(item: ReviewQueueItemEntity)

    @Query("SELECT * FROM content_review_queue WHERE status = 'REVIEW_REQUIRED' ORDER BY createdAt DESC")
    fun getPendingReviewItems(): Flow<List<ReviewQueueItemEntity>>

    @Query("SELECT * FROM content_review_queue ORDER BY createdAt DESC LIMIT 50")
    fun getAllReviewItems(): Flow<List<ReviewQueueItemEntity>>

    @Query("SELECT * FROM content_review_queue WHERE id = :id LIMIT 1")
    suspend fun getReviewItemById(id: String): ReviewQueueItemEntity?

    @Query("UPDATE content_review_queue SET status = :status, reviewerNotes = :notes, reviewedAt = :reviewedAt WHERE id = :id")
    suspend fun updateReviewStatus(id: String, status: String, notes: String, reviewedAt: Long)

    @Query("DELETE FROM content_review_queue WHERE id = :id")
    suspend fun deleteReviewItem(id: String)

    // --- Persistent Job Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobLog(log: ContentCollectionJobLogEntity)

    @Query("SELECT * FROM content_collection_jobs ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentJobLogs(limit: Int = 20): Flow<List<ContentCollectionJobLogEntity>>

    // --- AI Processing Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiProcessingLog(log: AiProcessingLogEntity)

    @Query("SELECT * FROM ai_processing_logs ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentAiLogs(limit: Int = 50): Flow<List<AiProcessingLogEntity>>

    @Query("SELECT * FROM ai_processing_logs WHERE contentId = :contentId ORDER BY createdAt DESC")
    suspend fun getAiLogsForContent(contentId: String): List<AiProcessingLogEntity>

    @Query("SELECT COUNT(*) FROM ai_processing_logs WHERE status = 'SKIPPED_UNCHANGED'")
    suspend fun getSkippedAiCallsCount(): Int

    @Query("SELECT COUNT(*) FROM ai_processing_logs WHERE status = 'SUCCESS'")
    suspend fun getSuccessfulAiCallsCount(): Int

    // --- Telegram Publications ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelegramPublication(pub: TelegramPublicationEntity)

    @Query("SELECT * FROM telegram_publications WHERE contentHash = :hash LIMIT 1")
    suspend fun getTelegramPublicationByHash(hash: String): TelegramPublicationEntity?

    @Query("SELECT * FROM telegram_publications ORDER BY publishedAt DESC LIMIT :limit")
    fun getRecentTelegramPublications(limit: Int = 30): Flow<List<TelegramPublicationEntity>>

    // --- Content Sources Configuration ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentSource(source: ContentSourceConfig)

    @Query("SELECT * FROM content_sources WHERE sourceId = :sourceId LIMIT 1")
    suspend fun getContentSourceById(sourceId: String): ContentSourceConfig?

    @Query("SELECT * FROM content_sources WHERE enabled = 1")
    suspend fun getEnabledContentSources(): List<ContentSourceConfig>

    @Query("SELECT * FROM content_sources")
    fun getAllContentSources(): Flow<List<ContentSourceConfig>>

    // --- Content Source References ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSourceReference(ref: ContentSourceReferenceEntity)

    @Query("SELECT * FROM content_source_references WHERE contentId = :contentId")
    suspend fun getSourceReferencesForContent(contentId: String): List<ContentSourceReferenceEntity>
}

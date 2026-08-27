package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ResourceBookmarkEntity
import com.example.data.model.StudyResourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateResource(resource: StudyResourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResources(resources: List<StudyResourceEntity>)

    @Query("SELECT * FROM study_resources WHERE status = 'ACTIVE' ORDER BY updatedAt DESC")
    fun getAllResources(): Flow<List<StudyResourceEntity>>

    @Query("SELECT * FROM study_resources WHERE status = 'ACTIVE' ORDER BY updatedAt DESC")
    suspend fun getAllResourcesOnce(): List<StudyResourceEntity>

    @Query("SELECT * FROM study_resources WHERE resourceId = :resourceId LIMIT 1")
    suspend fun getResourceById(resourceId: String): StudyResourceEntity?

    @Query("SELECT * FROM study_resources WHERE isSaved = 1 ORDER BY updatedAt DESC")
    fun getSavedResources(): Flow<List<StudyResourceEntity>>

    @Query("SELECT * FROM study_resources WHERE isDownloadedOffline = 1 ORDER BY updatedAt DESC")
    fun getOfflineResources(): Flow<List<StudyResourceEntity>>

    @Query("UPDATE study_resources SET readingStatus = :readingStatus, lastViewedPage = :lastPage, totalPages = CASE WHEN :totalPages > 0 THEN :totalPages ELSE totalPages END, updatedAt = :timestamp WHERE resourceId = :resourceId")
    suspend fun updateReadingProgress(resourceId: String, readingStatus: String, lastPage: Int, totalPages: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE study_resources SET isSaved = :isSaved, updatedAt = :timestamp WHERE resourceId = :resourceId")
    suspend fun updateSavedState(resourceId: String, isSaved: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE study_resources SET isDownloadedOffline = :isDownloaded, offlineFilePath = :filePath, fileSizeBytes = :fileSize WHERE resourceId = :resourceId")
    suspend fun updateOfflineStatus(resourceId: String, isDownloaded: Boolean, filePath: String, fileSize: Long)

    @Query("UPDATE study_resources SET aiSummaryCache = :summaryCache WHERE resourceId = :resourceId")
    suspend fun updateAiSummary(resourceId: String, summaryCache: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: ResourceBookmarkEntity)

    @Query("SELECT * FROM resource_bookmarks WHERE resourceId = :resourceId ORDER BY pageNumber ASC")
    suspend fun getBookmarksForResource(resourceId: String): List<ResourceBookmarkEntity>

    @Query("DELETE FROM study_resources WHERE resourceId = :resourceId")
    suspend fun deleteResource(resourceId: String)

    @Query("SELECT COUNT(*) FROM study_resources")
    suspend fun getResourceCount(): Int
}

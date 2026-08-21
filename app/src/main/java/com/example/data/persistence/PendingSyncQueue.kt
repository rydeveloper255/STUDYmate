package com.example.data.persistence

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pending_sync_queue")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val tableName: String,
    val recordId: String,
    val action: String, // "UPSERT", "INSERT", "DELETE"
    val jsonPayload: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)

@Dao
interface PendingSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: PendingSyncEntity): Long

    @Query("SELECT * FROM pending_sync_queue ORDER BY createdAtMillis ASC")
    suspend fun getAllPendingOnce(): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync_queue ORDER BY createdAtMillis ASC")
    fun getAllPendingFlow(): Flow<List<PendingSyncEntity>>

    @Query("DELETE FROM pending_sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync_queue WHERE tableName = :tableName AND recordId = :recordId")
    suspend fun deleteByRecord(tableName: String, recordId: String)

    @Query("SELECT COUNT(*) FROM pending_sync_queue")
    fun getPendingCountFlow(): Flow<Int>

    @Query("DELETE FROM pending_sync_queue")
    suspend fun clearAll()
}

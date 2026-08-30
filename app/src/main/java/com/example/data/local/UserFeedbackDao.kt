package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UserFeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFeedback(feedback: UserFeedbackEntity)

    @Query("SELECT * FROM user_feedback ORDER BY createdAtMillis DESC")
    fun getAllFeedbackFlow(): Flow<List<UserFeedbackEntity>>

    @Query("SELECT * FROM user_feedback ORDER BY createdAtMillis DESC")
    suspend fun getAllFeedbackOnce(): List<UserFeedbackEntity>

    @Query("SELECT * FROM user_feedback WHERE syncState = 'PENDING' ORDER BY createdAtMillis ASC")
    suspend fun getPendingSyncFeedback(): List<UserFeedbackEntity>

    @Query("SELECT * FROM user_feedback WHERE feedbackId = :id LIMIT 1")
    suspend fun getFeedbackById(id: String): UserFeedbackEntity?

    @Query("UPDATE user_feedback SET status = :status WHERE feedbackId = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE user_feedback SET syncState = :syncState WHERE feedbackId = :id")
    suspend fun updateSyncState(id: String, syncState: String)

    @Query("SELECT COUNT(*) FROM user_feedback")
    suspend fun getFeedbackCount(): Int

    @Query("SELECT COUNT(*) FROM user_feedback WHERE isHighPriority = 1 AND status != 'RESOLVED'")
    suspend fun getActiveHighPriorityCount(): Int
}

package com.example.data.local

import androidx.room.*
import com.example.data.model.StudyScheduleItem
import com.example.data.model.StudyScheduleLog
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyScheduleDao {
    @Query("SELECT * FROM study_schedule_items ORDER BY createdAt DESC")
    fun getAllScheduleItemsFlow(): Flow<List<StudyScheduleItem>>

    @Query("SELECT * FROM study_schedule_items ORDER BY createdAt DESC")
    suspend fun getAllScheduleItems(): List<StudyScheduleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateScheduleItem(item: StudyScheduleItem)

    @Delete
    suspend fun deleteScheduleItem(item: StudyScheduleItem)

    @Query("DELETE FROM study_schedule_items WHERE id = :id")
    suspend fun deleteScheduleItemById(id: String)

    @Query("SELECT * FROM study_schedule_logs ORDER BY scheduledDateMillis DESC")
    fun getAllScheduleLogsFlow(): Flow<List<StudyScheduleLog>>

    @Query("SELECT * FROM study_schedule_logs ORDER BY scheduledDateMillis DESC")
    suspend fun getAllScheduleLogs(): List<StudyScheduleLog>

    @Query("SELECT * FROM study_schedule_logs WHERE scheduledDateMillis >= :startMillis AND scheduledDateMillis <= :endMillis")
    suspend fun getScheduleLogsForDateRange(startMillis: Long, endMillis: Long): List<StudyScheduleLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateScheduleLog(log: StudyScheduleLog)

    @Query("UPDATE study_schedule_logs SET status = :status WHERE id = :id")
    suspend fun updateScheduleLogStatus(id: String, status: String)
}

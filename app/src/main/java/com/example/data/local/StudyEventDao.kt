package com.example.data.local

import androidx.room.*
import com.example.data.model.StudyEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyEventDao {
    @Query("SELECT * FROM study_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<StudyEventEntity>>

    @Query("SELECT * FROM study_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getEventsSince(sinceTimestamp: Long): List<StudyEventEntity>

    @Query("SELECT * FROM study_events WHERE eventType = :eventType ORDER BY timestamp DESC")
    fun getEventsByType(eventType: String): Flow<List<StudyEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: StudyEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<StudyEventEntity>)

    @Query("DELETE FROM study_events")
    suspend fun clearAllEvents()
}

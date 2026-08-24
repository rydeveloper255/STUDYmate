package com.example.data.local

import androidx.room.*
import com.example.data.model.RecruitmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecruitmentDao {

    @Query("SELECT * FROM recruitment_items ORDER BY fetchedAt DESC")
    fun getAllRecruitmentItems(): Flow<List<RecruitmentEntity>>

    @Query("SELECT * FROM recruitment_items WHERE contentType = :contentType ORDER BY fetchedAt DESC")
    fun getItemsByContentType(contentType: String): Flow<List<RecruitmentEntity>>

    @Query("SELECT * FROM recruitment_items WHERE isSaved = 1 ORDER BY fetchedAt DESC")
    fun getSavedItems(): Flow<List<RecruitmentEntity>>

    @Query("SELECT * FROM recruitment_items WHERE applicationStatus != 'NONE' ORDER BY fetchedAt DESC")
    fun getTrackedApplications(): Flow<List<RecruitmentEntity>>

    @Query("SELECT * FROM recruitment_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): RecruitmentEntity?

    @Query("SELECT * FROM recruitment_items")
    suspend fun getAllOnce(): List<RecruitmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: RecruitmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(items: List<RecruitmentEntity>)

    @Query("UPDATE recruitment_items SET isSaved = :isSaved WHERE id = :id")
    suspend fun setSaved(id: String, isSaved: Boolean)

    @Query("UPDATE recruitment_items SET hasDeadlineReminder = :hasReminder, reminderDaysBefore = :daysBefore WHERE id = :id")
    suspend fun setDeadlineReminder(id: String, hasReminder: Boolean, daysBefore: Int)

    @Query("UPDATE recruitment_items SET applicationStatus = :status, userApplicationNumber = :appNo, userRollNumber = :rollNo, userAppliedPost = :post, userNotes = :notes WHERE id = :id")
    suspend fun updateApplicationStatus(id: String, status: String, appNo: String, rollNo: String, post: String, notes: String)

    @Query("UPDATE recruitment_items SET documentsReadyList = :docsReady WHERE id = :id")
    suspend fun updateDocumentsReadyList(id: String, docsReady: List<String>)

    @Query("UPDATE recruitment_items SET checklistCheckedList = :checkedList WHERE id = :id")
    suspend fun updateChecklistCheckedList(id: String, checkedList: List<String>)

    @Query("DELETE FROM recruitment_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recruitment_items")
    suspend fun clearAll()
}

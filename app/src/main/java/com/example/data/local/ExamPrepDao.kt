package com.example.data.local

import androidx.room.*
import com.example.data.model.ExamGoalEntity
import com.example.data.model.SyllabusTopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamPrepDao {

    // --- EXAM GOALS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamGoal(goal: ExamGoalEntity)

    @Update
    suspend fun updateExamGoal(goal: ExamGoalEntity)

    @Query("DELETE FROM exam_goals WHERE examId = :examId")
    suspend fun deleteExamGoal(examId: String)

    @Query("SELECT * FROM exam_goals WHERE examId = :examId LIMIT 1")
    suspend fun getExamGoalById(examId: String): ExamGoalEntity?

    @Query("SELECT * FROM exam_goals ORDER BY CASE priority WHEN 'PRIMARY' THEN 1 WHEN 'SECONDARY' THEN 2 ELSE 3 END, createdAt DESC")
    fun getAllExamGoalsFlow(): Flow<List<ExamGoalEntity>>

    @Query("SELECT * FROM exam_goals ORDER BY CASE priority WHEN 'PRIMARY' THEN 1 WHEN 'SECONDARY' THEN 2 ELSE 3 END, createdAt DESC")
    suspend fun getAllExamGoalsOnce(): List<ExamGoalEntity>

    @Query("SELECT * FROM exam_goals WHERE status = 'ACTIVE' ORDER BY CASE priority WHEN 'PRIMARY' THEN 1 WHEN 'SECONDARY' THEN 2 ELSE 3 END LIMIT 1")
    fun getPrimaryExamGoalFlow(): Flow<ExamGoalEntity?>

    @Query("SELECT * FROM exam_goals WHERE status = 'ACTIVE' ORDER BY CASE priority WHEN 'PRIMARY' THEN 1 WHEN 'SECONDARY' THEN 2 ELSE 3 END LIMIT 1")
    suspend fun getPrimaryExamGoalOnce(): ExamGoalEntity?

    // --- SYLLABUS TOPICS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyllabusTopics(topics: List<SyllabusTopicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyllabusTopic(topic: SyllabusTopicEntity)

    @Update
    suspend fun updateSyllabusTopic(topic: SyllabusTopicEntity)

    @Query("DELETE FROM syllabus_topics WHERE topicId = :topicId")
    suspend fun deleteSyllabusTopic(topicId: String)

    @Query("DELETE FROM syllabus_topics WHERE examId = :examId")
    suspend fun deleteSyllabusTopicsForExam(examId: String)

    @Query("SELECT * FROM syllabus_topics WHERE examId = :examId ORDER BY subjectName ASC, orderIndex ASC")
    fun getSyllabusTopicsForExamFlow(examId: String): Flow<List<SyllabusTopicEntity>>

    @Query("SELECT * FROM syllabus_topics WHERE examId = :examId ORDER BY subjectName ASC, orderIndex ASC")
    suspend fun getSyllabusTopicsForExamOnce(examId: String): List<SyllabusTopicEntity>

    @Query("SELECT * FROM syllabus_topics WHERE examId = :examId AND subjectName = :subjectName ORDER BY orderIndex ASC")
    suspend fun getSyllabusTopicsForSubjectOnce(examId: String, subjectName: String): List<SyllabusTopicEntity>

    @Query("SELECT * FROM syllabus_topics WHERE topicId = :topicId LIMIT 1")
    suspend fun getTopicById(topicId: String): SyllabusTopicEntity?

    @Query("UPDATE syllabus_topics SET status = :status WHERE topicId = :topicId")
    suspend fun updateTopicStatus(topicId: String, status: String)

    @Query("UPDATE syllabus_topics SET revisionStatus = :revisionStatus, nextRevisionDueMillis = :nextDueMillis WHERE topicId = :topicId")
    suspend fun updateTopicRevisionState(topicId: String, revisionStatus: String, nextDueMillis: Long)

    @Query("SELECT * FROM syllabus_topics WHERE examId = :examId AND (revisionStatus = 'REVISION_PENDING' OR revisionStatus = 'REVISION_SCHEDULED' OR status = 'REVIEW_REQUIRED') ORDER BY nextRevisionDueMillis ASC")
    suspend fun getPendingRevisionTopicsOnce(examId: String): List<SyllabusTopicEntity>

    @Query("DELETE FROM exam_goals")
    suspend fun clearAllExamGoals()

    @Query("DELETE FROM syllabus_topics")
    suspend fun clearAllSyllabusTopics()
}

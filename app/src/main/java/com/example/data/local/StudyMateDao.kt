package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET xp = xp + :xpDelta, totalFocusMinutes = totalFocusMinutes + :focusMinutesDelta, streakDays = :newStreak")
    suspend fun updateXpAndStats(xpDelta: Int, focusMinutesDelta: Int, newStreak: Int)

    @Query("DELETE FROM user_profile")
    suspend fun clearUser()
}

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plan_items ORDER BY isCompleted ASC, scheduledDateMillis ASC, id ASC")
    fun getAllPlanItems(): Flow<List<StudyPlanItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItem(item: StudyPlanItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItems(items: List<StudyPlanItem>)

    @Update
    suspend fun updatePlanItem(item: StudyPlanItem)

    @Query("UPDATE study_plan_items SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setItemCompleted(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM study_plan_items WHERE id = :id")
    suspend fun deletePlanItem(id: Long)

    @Query("DELETE FROM study_plan_items")
    suspend fun clearAllPlanItems()
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllFocusSessions(): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSession): Long

    @Query("SELECT SUM(actualMinutesSpent) FROM focus_sessions")
    fun getTotalFocusMinutes(): Flow<Int?>
}

@Dao
interface MockTestDao {
    @Query("SELECT * FROM mock_test_attempts ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<MockTestAttempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: MockTestAttempt): Long

    @Query("DELETE FROM mock_test_attempts WHERE id = :id")
    suspend fun deleteAttempt(id: Long)
}

@Dao
interface UserQuestionMaterialDao {
    @Query("SELECT * FROM user_question_materials ORDER BY timestamp DESC")
    fun getAllMaterials(): Flow<List<UserQuestionMaterial>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: UserQuestionMaterial): Long

    @Query("DELETE FROM user_question_materials WHERE id = :id")
    suspend fun deleteMaterial(id: Long)
}

@Dao
interface MistakeDao {
    @Query("SELECT * FROM mistakes ORDER BY isMastered ASC, timestamp DESC")
    fun getAllMistakes(): Flow<List<MistakeItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(mistake: MistakeItem): Long

    @Query("UPDATE mistakes SET isMastered = :isMastered WHERE id = :id")
    suspend fun updateMastered(id: Long, isMastered: Boolean)

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteMistake(id: Long)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY status ASC, nextReviewDate ASC, id DESC")
    fun getAllFlashcards(): Flow<List<FlashcardItem>>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :currentTime OR status = 'REVISE_NOW' ORDER BY nextReviewDate ASC")
    fun getDueFlashcards(currentTime: Long): Flow<List<FlashcardItem>>

    @Query("SELECT * FROM flashcards WHERE subject = :subject ORDER BY nextReviewDate ASC")
    fun getFlashcardsBySubject(subject: String): Flow<List<FlashcardItem>>

    @Query("SELECT * FROM flashcards WHERE status = :category ORDER BY nextReviewDate ASC")
    fun getFlashcardsByCategory(category: RevisionCategory): Flow<List<FlashcardItem>>

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun getFlashcardById(id: Long): FlashcardItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(cards: List<FlashcardItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(card: FlashcardItem): Long

    @Update
    suspend fun updateFlashcard(card: FlashcardItem)

    @Query("UPDATE flashcards SET status = :status, confidence = :confidence, reviewCount = reviewCount + 1, lastReviewed = :lastReviewed WHERE id = :id")
    suspend fun updateReviewResult(id: Long, status: RevisionCategory, confidence: Int, lastReviewed: Long)

    @Query("UPDATE flashcards SET status = :status, confidence = :confidence, intervalDays = :intervalDays, easeFactor = :easeFactor, repetitions = :repetitions, nextReviewDate = :nextReviewDate, reviewCount = reviewCount + 1, lastReviewed = :lastReviewed WHERE id = :id")
    suspend fun updateSpacedReview(
        id: Long,
        status: RevisionCategory,
        confidence: Int,
        intervalDays: Int,
        easeFactor: Float,
        repetitions: Int,
        nextReviewDate: Long,
        lastReviewed: Long
    )

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcard(id: Long)

    @Query("DELETE FROM flashcards")
    suspend fun clearAllFlashcards()
}

@Dao
interface NovaMemoryDao {
    @Query("SELECT * FROM nova_memory ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<NovaMemoryItem>>

    @Query("SELECT * FROM nova_memory WHERE isEnabled = 1 ORDER BY timestamp DESC")
    fun getActiveMemories(): Flow<List<NovaMemoryItem>>

    @Query("SELECT * FROM nova_memory WHERE isEnabled = 1 ORDER BY timestamp DESC")
    suspend fun getActiveMemoriesOnce(): List<NovaMemoryItem>

    @Query("SELECT * FROM nova_memory WHERE category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategory(category: NovaMemoryCategory): Flow<List<NovaMemoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: NovaMemoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<NovaMemoryItem>)

    @Update
    suspend fun updateMemory(memory: NovaMemoryItem)

    @Query("UPDATE nova_memory SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleMemoryEnabled(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM nova_memory WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM nova_memory")
    suspend fun clearAllMemories()
}

@Dao
interface NovaReminderDao {
    @Query("SELECT * FROM nova_reminders ORDER BY timeMillis ASC")
    fun getAllReminders(): Flow<List<NovaReminderItem>>

    @Query("SELECT * FROM nova_reminders WHERE isCompleted = 0 ORDER BY timeMillis ASC")
    fun getPendingReminders(): Flow<List<NovaReminderItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: NovaReminderItem): Long

    @Update
    suspend fun updateReminder(reminder: NovaReminderItem)

    @Query("UPDATE nova_reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM nova_reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    @Query("DELETE FROM nova_reminders")
    suspend fun clearAllReminders()
}

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun getAllVoiceNotes(): Flow<List<VoiceNoteItem>>

    @Query("SELECT * FROM voice_notes WHERE id = :id LIMIT 1")
    suspend fun getVoiceNoteById(id: Long): VoiceNoteItem?

    @Query("SELECT * FROM voice_notes WHERE isBookmarked = 1 ORDER BY createdAt DESC")
    fun getBookmarkedVoiceNotes(): Flow<List<VoiceNoteItem>>

    @Query("SELECT * FROM voice_notes WHERE subject = :subject ORDER BY createdAt DESC")
    fun getVoiceNotesBySubject(subject: String): Flow<List<VoiceNoteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceNote(note: VoiceNoteItem): Long

    @Update
    suspend fun updateVoiceNote(note: VoiceNoteItem)

    @Query("UPDATE voice_notes SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun toggleBookmark(id: Long, isBookmarked: Boolean)

    @Query("UPDATE voice_notes SET isTranscribing = :isTranscribing, transcription = :transcription, summary = :summary, keyPoints = :keyPoints, extractedReminders = :reminders, title = :title WHERE id = :id")
    suspend fun updateTranscriptionResult(
        id: Long,
        isTranscribing: Boolean,
        transcription: String,
        summary: String,
        keyPoints: List<String>,
        reminders: List<String>,
        title: String
    )

    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteVoiceNote(id: Long)

    @Query("DELETE FROM voice_notes")
    suspend fun clearAllVoiceNotes()
}

@Dao
interface SmartNoteDao {
    @Query("SELECT * FROM smart_notes ORDER BY createdAt DESC")
    fun getAllSmartNotes(): Flow<List<SmartNoteItem>>

    @Query("SELECT * FROM smart_notes WHERE isBookmarked = 1 ORDER BY createdAt DESC")
    fun getBookmarkedNotes(): Flow<List<SmartNoteItem>>

    @Query("SELECT * FROM smart_notes WHERE subject = :subject ORDER BY createdAt DESC")
    fun getSmartNotesBySubject(subject: String): Flow<List<SmartNoteItem>>

    @Query("SELECT * FROM smart_notes WHERE id = :id LIMIT 1")
    suspend fun getSmartNoteById(id: Long): SmartNoteItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmartNote(note: SmartNoteItem): Long

    @Update
    suspend fun updateSmartNote(note: SmartNoteItem)

    @Query("UPDATE smart_notes SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun toggleBookmark(id: Long, isBookmarked: Boolean)

    @Query("UPDATE smart_notes SET isRevised = :isRevised WHERE id = :id")
    suspend fun toggleRevised(id: Long, isRevised: Boolean)

    @Query("DELETE FROM smart_notes WHERE id = :id")
    suspend fun deleteSmartNote(id: Long)

    @Query("DELETE FROM smart_notes")
    suspend fun clearAllSmartNotes()
}

@Dao
interface CurrentAffairsDao {
    @Query("SELECT * FROM current_affairs ORDER BY createdAt DESC")
    fun getAllCurrentAffairs(): Flow<List<CurrentAffairsItem>>

    @Query("SELECT * FROM current_affairs WHERE category = :category ORDER BY createdAt DESC")
    fun getByCategory(category: String): Flow<List<CurrentAffairsItem>>

    @Query("SELECT * FROM current_affairs WHERE isSavedForRevision = 1 ORDER BY createdAt DESC")
    fun getSavedForRevision(): Flow<List<CurrentAffairsItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentAffairs(items: List<CurrentAffairsItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CurrentAffairsItem): Long

    @Query("UPDATE current_affairs SET isSavedForRevision = :isSaved WHERE id = :id")
    suspend fun toggleSavedForRevision(id: Long, isSaved: Boolean)

    @Query("DELETE FROM current_affairs WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM current_affairs")
    suspend fun clearAll()
}

@Dao
interface ExamUpdateDao {
    @Query("SELECT * FROM exam_updates ORDER BY createdAt DESC")
    fun getAllExamUpdates(): Flow<List<ExamUpdateItem>>

    @Query("SELECT * FROM exam_updates WHERE examName = :examName ORDER BY createdAt DESC")
    fun getUpdatesByExam(examName: String): Flow<List<ExamUpdateItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamUpdates(items: List<ExamUpdateItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdate(item: ExamUpdateItem): Long

    @Query("UPDATE exam_updates SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM exam_updates WHERE id = :id")
    suspend fun deleteUpdate(id: Long)
}

@Dao
interface ExamObjectiveDao {
    @Query("SELECT * FROM exam_objectives ORDER BY status ASC, examDateMillis ASC")
    fun getAllExamObjectives(): Flow<List<ExamObjective>>

    @Query("SELECT * FROM exam_objectives WHERE status = 'ACTIVE' LIMIT 1")
    fun getActiveExamObjective(): Flow<ExamObjective?>

    @Query("SELECT * FROM exam_objectives WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveExamObjectiveOnce(): ExamObjective?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjective(objective: ExamObjective): Long

    @Update
    suspend fun updateObjective(objective: ExamObjective)

    @Query("UPDATE exam_objectives SET status = CASE WHEN id = :id THEN 'ACTIVE' ELSE 'PAUSED' END")
    suspend fun setActiveObjective(id: Long)

    @Query("DELETE FROM exam_objectives WHERE id = :id")
    suspend fun deleteObjective(id: Long)
}

@Dao
interface TopicMasteryDao {
    @Query("SELECT * FROM topic_masteries ORDER BY masteryScore ASC, lastTestedMillis ASC")
    fun getAllTopicMasteries(): Flow<List<TopicMastery>>

    @Query("SELECT * FROM topic_masteries WHERE subject = :subject ORDER BY masteryScore ASC")
    fun getTopicMasteriesBySubject(subject: String): Flow<List<TopicMastery>>

    @Query("SELECT * FROM topic_masteries WHERE masteryScore < :threshold ORDER BY masteryScore ASC")
    fun getWeakTopics(threshold: Int = 65): Flow<List<TopicMastery>>

    @Query("SELECT * FROM topic_masteries WHERE masteryScore >= :threshold ORDER BY masteryScore DESC")
    fun getMasteredTopics(threshold: Int = 85): Flow<List<TopicMastery>>

    @Query("SELECT * FROM topic_masteries WHERE subject = :subject AND topic = :topic LIMIT 1")
    suspend fun getTopicMasteryOnce(subject: String, topic: String): TopicMastery?

    @Query("SELECT * FROM topic_masteries WHERE subject = :subject AND topic = :topic LIMIT 1")
    fun getTopicMasteryFlow(subject: String, topic: String): Flow<TopicMastery?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTopicMastery(topicMastery: TopicMastery): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopicMasteries(items: List<TopicMastery>)

    @Query("DELETE FROM topic_masteries WHERE id = :id")
    suspend fun deleteTopicMastery(id: Long)

    @Query("DELETE FROM topic_masteries")
    suspend fun clearAll()
}

@Dao
interface StudentSessionHistoryDao {
    @Query("SELECT * FROM student_session_history ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudentSessionHistory>>

    @Query("SELECT * FROM student_session_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 30): Flow<List<StudentSessionHistory>>

    @Query("SELECT * FROM student_session_history WHERE sessionType = :sessionType ORDER BY timestamp DESC")
    fun getSessionsByType(sessionType: String): Flow<List<StudentSessionHistory>>

    @Query("SELECT * FROM student_session_history WHERE subject = :subject ORDER BY timestamp DESC")
    fun getSessionsBySubject(subject: String): Flow<List<StudentSessionHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudentSessionHistory): Long

    @Query("SELECT SUM(actualMinutesSpent) FROM student_session_history")
    fun getTotalSessionMinutes(): Flow<Int?>

    @Query("DELETE FROM student_session_history WHERE id = :id")
    suspend fun deleteSession(id: Long)
}

@Dao
interface IntelligenceSnapshotDao {
    @Query("SELECT * FROM intelligence_snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<IntelligenceSnapshot>>

    @Query("SELECT * FROM intelligence_snapshots ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSnapshot(): Flow<IntelligenceSnapshot?>

    @Query("SELECT * FROM intelligence_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshotOnce(): IntelligenceSnapshot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: IntelligenceSnapshot): Long

    @Query("DELETE FROM intelligence_snapshots")
    suspend fun clearAll()
}

@Dao
interface ExamCatalogDao {
    @Query("SELECT * FROM exams ORDER BY isPopular DESC, name ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE category = :category ORDER BY name ASC")
    fun getExamsByCategory(category: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :examId LIMIT 1")
    suspend fun getExamById(examId: String): ExamEntity?

    @Query("SELECT * FROM exams WHERE id = :examId LIMIT 1")
    fun getExamByIdFlow(examId: String): Flow<ExamEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    // Subjects
    @Query("SELECT * FROM exam_subjects WHERE examId = :examId ORDER BY isOfficial DESC, name ASC")
    fun getSubjectsForExam(examId: String): Flow<List<ExamSubjectEntity>>

    @Query("SELECT * FROM exam_subjects WHERE examId = :examId ORDER BY isOfficial DESC, name ASC")
    suspend fun getSubjectsForExamOnce(examId: String): List<ExamSubjectEntity>

    @Query("SELECT * FROM exam_subjects WHERE id = :subjectId LIMIT 1")
    suspend fun getSubjectById(subjectId: String): ExamSubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<ExamSubjectEntity>)

    // Chapters
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex ASC")
    fun getChaptersForSubject(subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE examId = :examId ORDER BY orderIndex ASC")
    fun getChaptersForExam(examId: String): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    // Topics
    @Query("SELECT * FROM topics WHERE chapterId = :chapterId ORDER BY orderIndex ASC")
    fun getTopicsForChapter(chapterId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY orderIndex ASC")
    fun getTopicsForSubject(subjectId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE examId = :examId ORDER BY isHighYield DESC, orderIndex ASC")
    fun getTopicsForExam(examId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE examId = :examId AND isHighYield = 1")
    fun getHighYieldTopicsForExam(examId: String): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Query("SELECT COUNT(*) FROM exams")
    suspend fun getExamsCount(): Int
}





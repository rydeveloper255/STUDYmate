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

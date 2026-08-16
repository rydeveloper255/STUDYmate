package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StudyRepository(
    private val database: StudyMateDatabase
) {
    val userProfile: Flow<UserProfile?> = database.userDao().getUserProfile()
    val allPlanItems: Flow<List<StudyPlanItem>> = database.studyPlanDao().getAllPlanItems()
    val allFocusSessions: Flow<List<FocusSession>> = database.focusDao().getAllFocusSessions()
    val allMockTestAttempts: Flow<List<MockTestAttempt>> = database.mockTestDao().getAllAttempts()
    val allMistakes: Flow<List<MistakeItem>> = database.mistakeDao().getAllMistakes()
    val allFlashcards: Flow<List<FlashcardItem>> = database.flashcardDao().getAllFlashcards()

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        database.userDao().insertOrUpdateUserProfile(profile)
    }

    suspend fun addStudyPlanItem(item: StudyPlanItem): Long = withContext(Dispatchers.IO) {
        database.studyPlanDao().insertPlanItem(item)
    }

    suspend fun replaceStudyPlan(items: List<StudyPlanItem>) = withContext(Dispatchers.IO) {
        database.studyPlanDao().clearAllPlanItems()
        database.studyPlanDao().insertPlanItems(items)
    }

    suspend fun togglePlanItemCompletion(id: Long, completed: Boolean, xpReward: Int = 30) = withContext(Dispatchers.IO) {
        database.studyPlanDao().setItemCompleted(id, completed)
        if (completed) {
            val user = database.userDao().getUserProfileOnce()
            if (user != null) {
                val newXp = user.xp + xpReward
                val newLevel = (newXp / 250) + 1
                database.userDao().insertOrUpdateUserProfile(user.copy(xp = newXp, level = newLevel))
            }
        }
    }

    suspend fun deletePlanItem(id: Long) = withContext(Dispatchers.IO) {
        database.studyPlanDao().deletePlanItem(id)
    }

    suspend fun recordFocusSession(
        subject: String,
        topic: String,
        durationMinutes: Int,
        actualMinutesSpent: Int
    ): FocusSession = withContext(Dispatchers.IO) {
        val earnedXp = (actualMinutesSpent * 2).coerceAtLeast(20)
        val session = FocusSession(
            subject = subject,
            topic = topic,
            durationMinutes = durationMinutes,
            actualMinutesSpent = actualMinutesSpent,
            xpEarned = earnedXp
        )
        database.focusDao().insertFocusSession(session)

        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            val newFocusMins = user.totalFocusMinutes + actualMinutesSpent
            val newXp = user.xp + earnedXp
            val newLevel = (newXp / 250) + 1
            database.userDao().insertOrUpdateUserProfile(
                user.copy(
                    totalFocusMinutes = newFocusMins,
                    xp = newXp,
                    level = newLevel
                )
            )
        }
        session
    }

    suspend fun recordMockTestAttempt(
        title: String,
        subject: String,
        score: Int,
        totalQuestions: Int,
        timeSpentSeconds: Int,
        weakTopics: List<String>,
        strongTopics: List<String>,
        aiRecommendation: String
    ): MockTestAttempt = withContext(Dispatchers.IO) {
        val accuracy = if (totalQuestions > 0) (score.toFloat() / totalQuestions) * 100f else 0f
        val attempt = MockTestAttempt(
            title = title,
            subject = subject,
            score = score,
            totalQuestions = totalQuestions,
            accuracyPercent = accuracy,
            timeSpentSeconds = timeSpentSeconds,
            weakTopics = weakTopics,
            strongTopics = strongTopics,
            aiRecommendation = aiRecommendation
        )
        database.mockTestDao().insertAttempt(attempt)

        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            val newQuestions = user.totalQuestionsSolved + totalQuestions
            val earnedXp = score * 15 + 20
            val newXp = user.xp + earnedXp
            val newLevel = (newXp / 250) + 1
            database.userDao().insertOrUpdateUserProfile(
                user.copy(
                    totalQuestionsSolved = newQuestions,
                    xp = newXp,
                    level = newLevel
                )
            )
        }
        attempt
    }

    suspend fun recordMistake(
        questionText: String,
        studentAnswer: String,
        correctAnswer: String,
        subject: String,
        topic: String,
        explanation: String
    ) = withContext(Dispatchers.IO) {
        val item = MistakeItem(
            questionText = questionText,
            studentAnswer = studentAnswer,
            correctAnswer = correctAnswer,
            subject = subject,
            topic = topic,
            explanation = explanation
        )
        database.mistakeDao().insertMistake(item)
    }

    suspend fun markMistakeMastered(id: Long, mastered: Boolean) = withContext(Dispatchers.IO) {
        database.mistakeDao().updateMastered(id, mastered)
        if (mastered) {
            val user = database.userDao().getUserProfileOnce()
            if (user != null) {
                database.userDao().insertOrUpdateUserProfile(user.copy(xp = user.xp + 25))
            }
        }
    }

    suspend fun addFlashcard(
        subject: String,
        topic: String,
        front: String,
        back: String,
        hint: String = "",
        difficulty: String = "Medium",
        sourceDocTitle: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val card = FlashcardItem(
            subject = subject,
            topic = topic,
            front = front,
            back = back,
            hint = hint,
            difficulty = difficulty,
            status = RevisionCategory.REVISE_NOW,
            confidence = 2,
            reviewCount = 0,
            lastReviewed = now,
            intervalDays = 1,
            easeFactor = 2.5f,
            repetitions = 0,
            nextReviewDate = now,
            sourceDocTitle = sourceDocTitle,
            createdAt = now
        )
        val id = database.flashcardDao().insertFlashcard(card)
        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            database.userDao().insertOrUpdateUserProfile(user.copy(xp = user.xp + 20))
        }
        id
    }

    suspend fun insertFlashcard(card: FlashcardItem): Long = withContext(Dispatchers.IO) {
        val id = database.flashcardDao().insertFlashcard(card)
        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            database.userDao().insertOrUpdateUserProfile(user.copy(xp = user.xp + 20))
        }
        id
    }

    suspend fun updateFlashcard(card: FlashcardItem) = withContext(Dispatchers.IO) {
        database.flashcardDao().updateFlashcard(card)
    }

    suspend fun deleteFlashcard(id: Long) = withContext(Dispatchers.IO) {
        database.flashcardDao().deleteFlashcard(id)
    }

    /**
     * Spaced Repetition Engine (SM-2 Inspired):
     * Rating Quality:
     * 1 = Again / Failed Recall (Need immediate practice today)
     * 2 = Hard (Struggled, review in 2 days)
     * 3 = Good (Recalled with reasonable effort, standard interval increase)
     * 5 = Easy / Mastered (Instant recall, accelerated interval expansion)
     */
    suspend fun recordSpacedFlashcardReview(id: Long, ratingQuality: Int) = withContext(Dispatchers.IO) {
        val card = database.flashcardDao().getFlashcardById(id) ?: return@withContext
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        var newReps = card.repetitions
        var newIntervalDays = card.intervalDays
        var newEaseFactor = card.easeFactor
        var newStatus = card.status
        val newConfidence = ratingQuality.coerceIn(1, 5)

        when (ratingQuality) {
            1 -> { // Again / Forgot
                newReps = 0
                newIntervalDays = 1
                newEaseFactor = (newEaseFactor - 0.2f).coerceAtLeast(1.3f)
                newStatus = RevisionCategory.REVISE_NOW
            }
            2 -> { // Hard
                newReps = (newReps + 1).coerceAtMost(2)
                newIntervalDays = 2
                newEaseFactor = (newEaseFactor - 0.15f).coerceAtLeast(1.3f)
                newStatus = RevisionCategory.PRACTICE_SOON
            }
            3 -> { // Good
                newReps += 1
                newIntervalDays = when (newReps) {
                    1 -> 1
                    2 -> 3
                    else -> (newIntervalDays * newEaseFactor).toInt().coerceAtLeast(newIntervalDays + 1)
                }
                newEaseFactor = (newEaseFactor + 0.05f).coerceAtMost(3.0f)
                newStatus = if (newIntervalDays >= 7) RevisionCategory.STRONG else RevisionCategory.PRACTICE_SOON
            }
            else -> { // 4 or 5: Easy / Mastered
                newReps += 1
                newIntervalDays = when (newReps) {
                    1 -> 3
                    2 -> 7
                    else -> (newIntervalDays * newEaseFactor * 1.35f).toInt().coerceAtLeast(newIntervalDays + 2)
                }
                newEaseFactor = (newEaseFactor + 0.15f).coerceAtMost(3.2f)
                newStatus = RevisionCategory.STRONG
            }
        }

        val nextReview = now + (newIntervalDays * dayMillis)

        database.flashcardDao().updateSpacedReview(
            id = id,
            status = newStatus,
            confidence = newConfidence,
            intervalDays = newIntervalDays,
            easeFactor = newEaseFactor,
            repetitions = newReps,
            nextReviewDate = nextReview,
            lastReviewed = now
        )

        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            val xpGain = when (ratingQuality) {
                1 -> 10
                2 -> 15
                3 -> 25
                else -> 35
            }
            val newXp = user.xp + xpGain
            val newLevel = (newXp / 250) + 1
            database.userDao().insertOrUpdateUserProfile(user.copy(xp = newXp, level = newLevel))
        }
    }

    suspend fun recordFlashcardReview(id: Long, status: RevisionCategory, confidence: Int) = withContext(Dispatchers.IO) {
        val rating = when (status) {
            RevisionCategory.REVISE_NOW -> 1
            RevisionCategory.PRACTICE_SOON -> 3
            RevisionCategory.STRONG -> 5
        }
        recordSpacedFlashcardReview(id, rating)
    }

    suspend fun insertFlashcardList(cards: List<FlashcardItem>) = withContext(Dispatchers.IO) {
        database.flashcardDao().insertFlashcards(cards)
        val user = database.userDao().getUserProfileOnce()
        if (user != null && cards.isNotEmpty()) {
            val earnedXp = cards.size * 15
            val newXp = user.xp + earnedXp
            val newLevel = (newXp / 250) + 1
            database.userDao().insertOrUpdateUserProfile(user.copy(xp = newXp, level = newLevel))
        }
    }

    suspend fun populateInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        // If flashcards empty, add initial curated flashcards for smart revision
        database.flashcardDao().insertFlashcards(
            listOf(
                FlashcardItem(
                    subject = "Physics",
                    topic = "Electromagnetism",
                    front = "What is Lenz's Law and what conservation principle is it based on?",
                    back = "Lenz's law states that the direction of induced EMF opposes the change in magnetic flux that produces it. It is a direct consequence of the Conservation of Energy.",
                    hint = "Think about energy conservation in closed conducting loops.",
                    difficulty = "Medium",
                    status = RevisionCategory.REVISE_NOW,
                    confidence = 2,
                    intervalDays = 1,
                    easeFactor = 2.5f,
                    repetitions = 0,
                    nextReviewDate = now,
                    sourceDocTitle = "NCERT Physics Class 12",
                    createdAt = now
                ),
                FlashcardItem(
                    subject = "Mathematics",
                    topic = "Calculus",
                    front = "State the Fundamental Theorem of Calculus (Part 1).",
                    back = "If f is continuous on [a, b] and F(x) = ∫[a to x] f(t)dt, then F'(x) = f(x) for all x in (a, b).",
                    hint = "Differentiating an integral of a continuous function yields the integrand.",
                    difficulty = "Hard",
                    status = RevisionCategory.PRACTICE_SOON,
                    confidence = 3,
                    intervalDays = 3,
                    easeFactor = 2.5f,
                    repetitions = 1,
                    nextReviewDate = now + (2 * 24 * 3600 * 1000L),
                    sourceDocTitle = "Calculus Essentials",
                    createdAt = now
                ),
                FlashcardItem(
                    subject = "Chemistry",
                    topic = "Thermodynamics",
                    front = "What determines the spontaneity of a reaction at constant T and P?",
                    back = "Gibbs Free Energy change: ΔG = ΔH - TΔS. Spontaneous when ΔG < 0.",
                    hint = "Look at the sign of ΔG.",
                    difficulty = "Medium",
                    status = RevisionCategory.STRONG,
                    confidence = 5,
                    intervalDays = 7,
                    easeFactor = 2.7f,
                    repetitions = 3,
                    nextReviewDate = now + (7 * 24 * 3600 * 1000L),
                    sourceDocTitle = "Physical Chemistry Core",
                    createdAt = now
                ),
                FlashcardItem(
                    subject = "Biology",
                    topic = "Genetics",
                    front = "What is Mendel's Law of Independent Assortment?",
                    back = "Alleles of two (or more) different genes get sorted into gametes independently of one another during meiosis.",
                    hint = "Applies to genes located on different chromosomes or far apart on the same chromosome.",
                    difficulty = "Easy",
                    status = RevisionCategory.PRACTICE_SOON,
                    confidence = 3,
                    intervalDays = 2,
                    easeFactor = 2.5f,
                    repetitions = 1,
                    nextReviewDate = now + (1 * 24 * 3600 * 1000L),
                    sourceDocTitle = "Principles of Inheritance",
                    createdAt = now
                )
            )
        )
    }
}

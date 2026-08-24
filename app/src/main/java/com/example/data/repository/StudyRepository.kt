package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.supabase.SupabaseSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StudyRepository(
    private val database: StudyMateDatabase,
    val syncService: SupabaseSyncService? = null
) {
    val intelligenceRepository = StudyMateIntelligenceRepository(database, syncService)
    val smartLearningEngine = com.example.service.intelligence.SmartLearningEngine(
        database = database,
        geminiRepository = com.example.data.remote.GeminiRepository()
    )

    val userProfile: Flow<UserProfile?> = database.userDao().getUserProfile()
    val userStudyPreferences: Flow<UserStudyPreferences?> = database.userStudyPreferencesDao().getUserPreferences()
    val allPlanItems: Flow<List<StudyPlanItem>> = database.studyPlanDao().getAllPlanItems()
    val allFocusSessions: Flow<List<FocusSession>> = database.focusDao().getAllFocusSessions()
    val allMockTestAttempts: Flow<List<MockTestAttempt>> = database.mockTestDao().getAllAttempts()
    val allMistakes: Flow<List<MistakeItem>> = database.mistakeDao().getAllMistakes()
    val allFlashcards: Flow<List<FlashcardItem>> = database.flashcardDao().getAllFlashcards()
    val allUserQuestionMaterials: Flow<List<UserQuestionMaterial>> = database.userQuestionMaterialDao().getAllMaterials()
    val allNovaMemories: Flow<List<NovaMemoryItem>> = database.novaMemoryDao().getAllMemories()
    val activeNovaMemories: Flow<List<NovaMemoryItem>> = database.novaMemoryDao().getActiveMemories()
    val allNovaReminders: Flow<List<NovaReminderItem>> = database.novaReminderDao().getAllReminders()
    val pendingNovaReminders: Flow<List<NovaReminderItem>> = database.novaReminderDao().getPendingReminders()
    val allVoiceNotes: Flow<List<VoiceNoteItem>> = database.voiceNoteDao().getAllVoiceNotes()
    val bookmarkedVoiceNotes: Flow<List<VoiceNoteItem>> = database.voiceNoteDao().getBookmarkedVoiceNotes()
    val allSmartNotes: Flow<List<SmartNoteItem>> = database.smartNoteDao().getAllSmartNotes()
    val bookmarkedSmartNotes: Flow<List<SmartNoteItem>> = database.smartNoteDao().getBookmarkedNotes()
    val allCurrentAffairs: Flow<List<CurrentAffairsItem>> = database.currentAffairsDao().getAllCurrentAffairs()
    val savedCurrentAffairs: Flow<List<CurrentAffairsItem>> = database.currentAffairsDao().getSavedForRevision()
    val allExamUpdates: Flow<List<ExamUpdateItem>> = database.examUpdateDao().getAllExamUpdates()
    val allLearningBookmarks: Flow<List<UserLearningBookmark>> = database.userLearningBookmarkDao().getAllBookmarks()


    // Intelligence flows
    val allExamObjectives: Flow<List<ExamObjective>> = intelligenceRepository.allExamObjectives
    val activeExamObjective: Flow<ExamObjective?> = intelligenceRepository.activeExamObjective
    val allTopicMasteries: Flow<List<TopicMastery>> = intelligenceRepository.allTopicMasteries
    val weakTopicMasteries: Flow<List<TopicMastery>> = intelligenceRepository.weakTopics
    val studentSessionHistory: Flow<List<StudentSessionHistory>> = intelligenceRepository.allSessionHistory
    val latestIntelligenceSnapshot: Flow<IntelligenceSnapshot?> = intelligenceRepository.latestSnapshot

    suspend fun saveSmartNote(note: SmartNoteItem): Long = withContext(Dispatchers.IO) {
        val id = database.smartNoteDao().insertSmartNote(note)
        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            val newXp = user.xp + 25
            val newLevel = (newXp / 250) + 1
            val updatedUser = user.copy(xp = newXp, level = newLevel)
            database.userDao().insertOrUpdateUserProfile(updatedUser)
            syncService?.syncUserProfile(updatedUser)
        }
        syncService?.syncSmartNote(note.copy(id = id))
        id
    }

    suspend fun toggleSmartNoteBookmark(id: Long, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        database.smartNoteDao().toggleBookmark(id, isBookmarked)
    }

    suspend fun toggleSmartNoteRevised(id: Long, isRevised: Boolean) = withContext(Dispatchers.IO) {
        database.smartNoteDao().toggleRevised(id, isRevised)
    }

    suspend fun updateSmartNote(note: SmartNoteItem) = withContext(Dispatchers.IO) {
        database.smartNoteDao().updateSmartNote(note)
        syncService?.syncSmartNote(note)
    }

    suspend fun deleteSmartNote(id: Long) = withContext(Dispatchers.IO) {
        database.smartNoteDao().deleteSmartNote(id)
        syncService?.deleteSmartNote(id)
    }

    suspend fun saveCurrentAffairsList(items: List<CurrentAffairsItem>) = withContext(Dispatchers.IO) {
        database.currentAffairsDao().insertCurrentAffairs(items)
    }

    suspend fun toggleCurrentAffairsSaved(id: Long, isSaved: Boolean) = withContext(Dispatchers.IO) {
        database.currentAffairsDao().toggleSavedForRevision(id, isSaved)
    }

    suspend fun saveExamUpdatesList(items: List<ExamUpdateItem>) = withContext(Dispatchers.IO) {
        database.examUpdateDao().insertExamUpdates(items)
    }

    suspend fun markExamUpdateRead(id: Long) = withContext(Dispatchers.IO) {
        database.examUpdateDao().markAsRead(id)
    }

    suspend fun saveVoiceNote(note: VoiceNoteItem): Long = withContext(Dispatchers.IO) {
        database.voiceNoteDao().insertVoiceNote(note)
    }

    suspend fun updateVoiceNote(note: VoiceNoteItem) = withContext(Dispatchers.IO) {
        database.voiceNoteDao().updateVoiceNote(note)
    }

    suspend fun toggleVoiceNoteBookmark(id: Long, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        database.voiceNoteDao().toggleBookmark(id, isBookmarked)
    }

    suspend fun updateVoiceNoteTranscription(
        id: Long,
        isTranscribing: Boolean,
        transcription: String,
        summary: String,
        keyPoints: List<String>,
        reminders: List<String>,
        title: String
    ) = withContext(Dispatchers.IO) {
        database.voiceNoteDao().updateTranscriptionResult(
            id = id,
            isTranscribing = isTranscribing,
            transcription = transcription,
            summary = summary,
            keyPoints = keyPoints,
            reminders = reminders,
            title = title
        )
    }

    suspend fun deleteVoiceNote(id: Long) = withContext(Dispatchers.IO) {
        database.voiceNoteDao().deleteVoiceNote(id)
    }

    suspend fun getVoiceNoteById(id: Long): VoiceNoteItem? = withContext(Dispatchers.IO) {
        database.voiceNoteDao().getVoiceNoteById(id)
    }

    suspend fun saveNovaMemory(memory: NovaMemoryItem): Long = withContext(Dispatchers.IO) {
        val id = database.novaMemoryDao().insertMemory(memory)
        syncService?.syncNovaMemory(memory.copy(id = id))
        id
    }

    suspend fun saveNovaMemories(memories: List<NovaMemoryItem>) = withContext(Dispatchers.IO) {
        database.novaMemoryDao().insertMemories(memories)
        memories.forEach { syncService?.syncNovaMemory(it) }
    }

    suspend fun updateNovaMemory(memory: NovaMemoryItem) = withContext(Dispatchers.IO) {
        database.novaMemoryDao().updateMemory(memory)
        syncService?.syncNovaMemory(memory)
    }

    suspend fun toggleNovaMemory(id: Long, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        database.novaMemoryDao().toggleMemoryEnabled(id, isEnabled)
    }

    suspend fun deleteNovaMemory(id: Long) = withContext(Dispatchers.IO) {
        database.novaMemoryDao().deleteMemory(id)
        syncService?.deleteNovaMemory(id)
    }

    suspend fun clearAllNovaMemories() = withContext(Dispatchers.IO) {
        database.novaMemoryDao().clearAllMemories()
    }

    suspend fun getActiveMemoriesOnce(): List<NovaMemoryItem> = withContext(Dispatchers.IO) {
        database.novaMemoryDao().getActiveMemoriesOnce()
    }

    suspend fun addNovaReminder(reminder: NovaReminderItem): Long = withContext(Dispatchers.IO) {
        database.novaReminderDao().insertReminder(reminder)
    }

    suspend fun setNovaReminderCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        database.novaReminderDao().setCompleted(id, completed)
    }

    suspend fun deleteNovaReminder(id: Long) = withContext(Dispatchers.IO) {
        database.novaReminderDao().deleteReminder(id)
    }

    suspend fun clearAllNovaReminders() = withContext(Dispatchers.IO) {
        database.novaReminderDao().clearAllReminders()
    }

    suspend fun populateInitialNovaMemoriesIfEmpty(userProfile: UserProfile) = withContext(Dispatchers.IO) {
        val existing = database.novaMemoryDao().getActiveMemoriesOnce()
        if (existing.isEmpty()) {
            val initial = listOf(
                NovaMemoryItem(
                    category = NovaMemoryCategory.ACADEMIC,
                    key = "Target Exam",
                    value = "${userProfile.examName} (${userProfile.grade})",
                    source = "Onboarding"
                ),
                NovaMemoryItem(
                    category = NovaMemoryCategory.ACADEMIC,
                    key = "Subjects of Focus",
                    value = userProfile.subjects.joinToString(", "),
                    source = "Onboarding"
                ),
                NovaMemoryItem(
                    category = NovaMemoryCategory.WEAK_AREAS,
                    key = "Weak Areas",
                    value = if (userProfile.weakTopics.isNotEmpty()) userProfile.weakTopics.joinToString(", ") else "Rotational Motion, Organic Synthesis",
                    source = "Self Assessment"
                ),
                NovaMemoryItem(
                    category = NovaMemoryCategory.STUDY_PREFERENCES,
                    key = "Daily Target",
                    value = "${userProfile.dailyTargetMinutes / 60}h ${userProfile.dailyTargetMinutes % 60}m per day (${userProfile.preferredStudyTime} slots)",
                    source = "Preferences"
                ),
                NovaMemoryItem(
                    category = NovaMemoryCategory.GOALS,
                    key = "Milestone Goal",
                    value = userProfile.targetScore.ifBlank { "Top 500 AIR / 99th percentile" },
                    source = "Goals"
                ),
                NovaMemoryItem(
                    category = NovaMemoryCategory.CONVERSATION,
                    key = "Preferred Title",
                    value = "Boss",
                    source = "NOVA Persona"
                )
            )
            database.novaMemoryDao().insertMemories(initial)
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        database.userDao().insertOrUpdateUserProfile(profile)
        syncService?.syncUserProfile(profile)
    }

    suspend fun saveUserPreferences(preferences: UserStudyPreferences) = withContext(Dispatchers.IO) {
        database.userStudyPreferencesDao().saveUserPreferences(preferences)
    }

    suspend fun getUserPreferencesSync(userId: String = "current_user"): UserStudyPreferences {
        return withContext(Dispatchers.IO) {
            database.userStudyPreferencesDao().getUserPreferencesSync(userId) ?: UserStudyPreferences(userId = userId)
        }
    }

    suspend fun clearAllUserStudyData(): Unit = withContext(Dispatchers.IO) {
        database.studyPlanDao().clearAllPlanItems()
        database.mockTestDao().clearAllAttempts()
        database.mistakeDao().clearAllMistakes()
        database.focusDao().clearAllFocusSessions()
        database.userLearningBookmarkDao().clearAllBookmarks()
        database.userQuestionMaterialDao().clearAllMaterials()
        database.studentSessionHistoryDao().clearAllSessionHistory()
        database.topicMasteryDao().clearAll()
        database.novaMemoryDao().clearAllMemories()
        database.novaReminderDao().clearAllReminders()
        database.voiceNoteDao().clearAllVoiceNotes()
        database.smartNoteDao().clearAllSmartNotes()
        database.questionHistoryDao().clearAllHistory()
        database.pendingSyncDao().clearAll()
    }

    suspend fun addStudyPlanItem(item: StudyPlanItem): Long = withContext(Dispatchers.IO) {
        val id = database.studyPlanDao().insertPlanItem(item)
        val saved = item.copy(id = id)
        syncService?.syncStudyPlanItem(saved)
        id
    }

    suspend fun updateStudyPlanItem(item: StudyPlanItem) = withContext(Dispatchers.IO) {
        database.studyPlanDao().insertPlanItem(item)
        syncService?.syncStudyPlanItem(item)
    }

    suspend fun replaceStudyPlan(items: List<StudyPlanItem>) = withContext(Dispatchers.IO) {
        database.studyPlanDao().clearAllPlanItems()
        database.studyPlanDao().insertPlanItems(items)
        items.forEach { syncService?.syncStudyPlanItem(it) }
    }

    suspend fun togglePlanItemCompletion(id: Long, completed: Boolean, xpReward: Int = 30) = withContext(Dispatchers.IO) {
        database.studyPlanDao().setItemCompleted(id, completed)
        if (completed) {
            val user = database.userDao().getUserProfileOnce()
            if (user != null) {
                val newXp = user.xp + xpReward
                val newLevel = (newXp / 250) + 1
                val updatedUser = user.copy(xp = newXp, level = newLevel)
                database.userDao().insertOrUpdateUserProfile(updatedUser)
                syncService?.syncUserProfile(updatedUser)
            }
        }
    }

    suspend fun deletePlanItem(id: Long) = withContext(Dispatchers.IO) {
        database.studyPlanDao().deletePlanItem(id)
        syncService?.deleteStudyPlanItem(id)
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
        val id = database.focusDao().insertFocusSession(session)
        val savedSession = session.copy(id = id)
        syncService?.syncFocusSession(savedSession)

        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            val newFocusMins = user.totalFocusMinutes + actualMinutesSpent
            val newXp = user.xp + earnedXp
            val newLevel = (newXp / 250) + 1
            val updatedUser = user.copy(
                totalFocusMinutes = newFocusMins,
                xp = newXp,
                level = newLevel
            )
            database.userDao().insertOrUpdateUserProfile(updatedUser)
            syncService?.syncUserProfile(updatedUser)
        }
        savedSession
    }

    suspend fun recordMockTestAttempt(
        title: String,
        subject: String,
        score: Int,
        totalQuestions: Int,
        timeSpentSeconds: Int,
        weakTopics: List<String> = emptyList(),
        strongTopics: List<String> = emptyList(),
        aiRecommendation: String = "",
        examName: String = "JEE / NEET / Board Exam",
        topic: String = "All Topics",
        difficulty: String = "Medium",
        correctCount: Int = 0,
        incorrectCount: Int = 0,
        skippedCount: Int = 0,
        avgTimePerQuestionSeconds: Float = 0f,
        markingScheme: String = "+4 / -1 (Standard)",
        totalTimeAllowedSeconds: Int = 600
    ): MockTestAttempt = withContext(Dispatchers.IO) {
        val accuracy = if (totalQuestions > 0) (correctCount.toFloat() / totalQuestions) * 100f else 0f
        val attempt = MockTestAttempt(
            title = title,
            subject = subject,
            score = score,
            totalQuestions = totalQuestions,
            accuracyPercent = accuracy,
            timeSpentSeconds = timeSpentSeconds,
            weakTopics = weakTopics,
            strongTopics = strongTopics,
            aiRecommendation = aiRecommendation,
            examName = examName,
            topic = topic,
            difficulty = difficulty,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            skippedCount = skippedCount,
            avgTimePerQuestionSeconds = avgTimePerQuestionSeconds,
            markingScheme = markingScheme,
            totalTimeAllowedSeconds = totalTimeAllowedSeconds
        )
        val id = database.mockTestDao().insertAttempt(attempt)
        val savedAttempt = attempt.copy(id = id)
        syncService?.syncMockTestAttempt(savedAttempt)

        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            val newQuestions = user.totalQuestionsSolved + totalQuestions
            val earnedXp = (correctCount * 15 + 20).coerceAtLeast(10)
            val newXp = user.xp + earnedXp
            val newLevel = (newXp / 250) + 1
            val updatedUser = user.copy(
                totalQuestionsSolved = newQuestions,
                xp = newXp,
                level = newLevel
            )
            database.userDao().insertOrUpdateUserProfile(updatedUser)
            syncService?.syncUserProfile(updatedUser)
        }
        savedAttempt
    }

    suspend fun deleteMockTestAttempt(id: Long): Unit = withContext(Dispatchers.IO) {
        database.mockTestDao().deleteAttempt(id)
        syncService?.deleteMockTestAttempt(id)
    }

    suspend fun saveUserQuestionMaterial(
        title: String,
        exam: String,
        subject: String,
        topic: String,
        rawText: String
    ): Long = withContext(Dispatchers.IO) {
        // Approximate count of questions based on markers like "Q", "1.", etc. or paragraphs
        val estCount = rawText.split(Regex("(?i)(question|Q[0-9]|\\n[0-9]+\\.)")).size.coerceAtLeast(1)
        val material = UserQuestionMaterial(
            title = title,
            exam = exam,
            subject = subject,
            topic = topic,
            rawText = rawText,
            questionCount = estCount,
            timestamp = System.currentTimeMillis()
        )
        val id = database.userQuestionMaterialDao().insertMaterial(material)
        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            val updatedUser = user.copy(xp = user.xp + 25)
            database.userDao().insertOrUpdateUserProfile(updatedUser)
            syncService?.syncUserProfile(updatedUser)
        }
        id
    }

    suspend fun deleteUserQuestionMaterial(id: Long): Unit = withContext(Dispatchers.IO) {
        database.userQuestionMaterialDao().deleteMaterial(id)
    }

    suspend fun recordMistake(
        questionText: String,
        studentAnswer: String,
        correctAnswer: String,
        subject: String,
        topic: String,
        explanation: String
    ): Unit = withContext(Dispatchers.IO) {
        val item = MistakeItem(
            questionText = questionText,
            studentAnswer = studentAnswer,
            correctAnswer = correctAnswer,
            subject = subject,
            topic = topic,
            explanation = explanation
        )
        val id = database.mistakeDao().insertMistake(item)
        syncService?.syncMistake(item.copy(id = id))
    }

    suspend fun markMistakeMastered(id: Long, mastered: Boolean): Unit = withContext(Dispatchers.IO) {
        database.mistakeDao().updateMastered(id, mastered)
        if (mastered) {
            val user = database.userDao().getUserProfileOnce()
            if (user != null) {
                val updatedUser = user.copy(xp = user.xp + 25)
                database.userDao().insertOrUpdateUserProfile(updatedUser)
                syncService?.syncUserProfile(updatedUser)
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
        val savedCard = card.copy(id = id)
        syncService?.syncFlashcard(savedCard)

        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            val updatedUser = user.copy(xp = user.xp + 20)
            database.userDao().insertOrUpdateUserProfile(updatedUser)
            syncService?.syncUserProfile(updatedUser)
        }
        id
    }

    suspend fun insertFlashcard(card: FlashcardItem): Long = withContext(Dispatchers.IO) {
        val id = database.flashcardDao().insertFlashcard(card)
        val savedCard = card.copy(id = id)
        syncService?.syncFlashcard(savedCard)

        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            val updatedUser = user.copy(xp = user.xp + 20)
            database.userDao().insertOrUpdateUserProfile(updatedUser)
            syncService?.syncUserProfile(updatedUser)
        }
        id
    }

    suspend fun insertFlashcards(cards: List<FlashcardItem>) = withContext(Dispatchers.IO) {
        cards.forEach { card ->
            val id = database.flashcardDao().insertFlashcard(card)
            syncService?.syncFlashcard(card.copy(id = id))
        }
        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            val updatedUser = user.copy(xp = user.xp + (cards.size * 15))
            database.userDao().insertOrUpdateUserProfile(updatedUser)
            syncService?.syncUserProfile(updatedUser)
        }
    }

    suspend fun updateFlashcard(card: FlashcardItem) = withContext(Dispatchers.IO) {
        database.flashcardDao().updateFlashcard(card)
        syncService?.syncFlashcard(card)
    }

    suspend fun deleteFlashcard(id: Long) = withContext(Dispatchers.IO) {
        database.flashcardDao().deleteFlashcard(id)
        syncService?.deleteFlashcard(id)
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

        val updatedCard = card.copy(
            status = newStatus,
            confidence = newConfidence,
            intervalDays = newIntervalDays,
            easeFactor = newEaseFactor,
            repetitions = newReps,
            nextReviewDate = nextReview,
            lastReviewed = now
        )
        syncService?.syncFlashcard(updatedCard)

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
            val updatedUser = user.copy(xp = newXp, level = newLevel)
            database.userDao().insertOrUpdateUserProfile(updatedUser)
            syncService?.syncUserProfile(updatedUser)
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
        cards.forEach { card ->
            val id = database.flashcardDao().insertFlashcard(card)
            syncService?.syncFlashcard(card.copy(id = id))
        }
        val user = database.userDao().getUserProfileOnce()
        if (user != null && cards.isNotEmpty()) {
            val earnedXp = cards.size * 15
            val newXp = user.xp + earnedXp
            val newLevel = (newXp / 250) + 1
            val updatedUser = user.copy(xp = newXp, level = newLevel)
            database.userDao().insertOrUpdateUserProfile(updatedUser)
            syncService?.syncUserProfile(updatedUser)
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

        // Seed initial Smart Notes
        database.smartNoteDao().insertSmartNote(
            SmartNoteItem(
                title = "Thermodynamics Core Formulas & Laws",
                subject = "Physics / Chemistry",
                topic = "Thermodynamics",
                contentMarkdown = "## Key Thermodynamic Laws\n\n1. **First Law:** $\\Delta U = Q - W$\n2. **Second Law:** Heat cannot spontaneously flow from colder to hotter body without work.\n3. **Carnot Engine Efficiency:** $\\eta = 1 - \\frac{T_C}{T_H}$",
                keyPoints = listOf(
                    "ΔU is a state function; Q and W are path dependent.",
                    "For isothermal process of ideal gas, ΔU = 0, so Q = W.",
                    "For adiabatic process, Q = 0, so ΔU = -W."
                ),
                formulas = listOf(
                    "\\Delta U = n C_v \\Delta T",
                    "W_{iso} = n R T \\ln(V_f / V_i)",
                    "P V^\\gamma = \\text{constant}"
                ),
                importantFacts = listOf(
                    "Carnot cycle consists of two reversible isotherms and two reversible adiabatics.",
                    "Entropy of an isolated system always increases in an irreversible process."
                ),
                sourceTitle = "NCERT Physics & Chemistry",
                sourceUrl = "https://ncert.nic.in",
                isBookmarked = true,
                createdAt = now
            )
        )

        // Seed initial Current Affairs
        database.currentAffairsDao().insertCurrentAffairs(
            listOf(
                CurrentAffairsItem(
                    title = "ISRO Gaganyaan Mission: Key Milestones & Test Vehicle Abort Flight",
                    summary = "ISRO completed critical crew escape and parachute deployment tests for the Gaganyaan mission. The mission aims to demonstrate human spaceflight capability to Low Earth Orbit with a crew of 3 members.",
                    examRelevance = "High Yield for UPSC CSE (Science & Tech GS-3) and SSC CGL General Awareness.",
                    category = "Science & Tech",
                    targetExams = listOf("UPSC", "SSC", "State PSC", "General"),
                    sourceName = "ISRO Official / PIB India",
                    sourceUrl = "https://isro.gov.in",
                    publishedDate = "Recent Update",
                    isSavedForRevision = true,
                    createdAt = now
                ),
                CurrentAffairsItem(
                    title = "RBI Monetary Policy Committee (MPC) Review & Inflation Targeting",
                    summary = "The Reserve Bank of India maintained repo rate stances focusing on aligning headline CPI inflation with the 4% target on a durable basis while supporting sustainable economic growth.",
                    examRelevance = "Important for Economy (GS-3), Banking Exams, and Current Affairs.",
                    category = "Economy",
                    targetExams = listOf("UPSC", "SSC", "Banking", "General"),
                    sourceName = "Reserve Bank of India",
                    sourceUrl = "https://rbi.org.in",
                    publishedDate = "Current Fiscal",
                    isSavedForRevision = false,
                    createdAt = now
                )
            )
        )

        // Seed initial Exam Updates
        database.examUpdateDao().insertExamUpdates(
            listOf(
                ExamUpdateItem(
                    examName = "JEE Main & Advanced",
                    title = "NTA Exam Calendar & Candidate Information Bulletin",
                    noticeType = "Official Notice",
                    summary = "National Testing Agency (NTA) released notification regarding session eligibility, application windows, and revised exam centers.",
                    officialLink = "https://jeemain.nta.ac.in",
                    publishDate = "Official Release",
                    isVerifiedOfficial = true,
                    createdAt = now
                ),
                ExamUpdateItem(
                    examName = "NEET UG",
                    title = "NTA NEET UG Syllabus Clarifications & Tie-Breaking Rules",
                    noticeType = "Syllabus Update",
                    summary = "NMC and NTA confirmed the standardized syllabus matching NCERT rationalized content for Physics, Chemistry, and Biology.",
                    officialLink = "https://neet.nta.online",
                    publishDate = "Official Release",
                    isVerifiedOfficial = true,
                    createdAt = now
                ),
                ExamUpdateItem(
                    examName = "UPSC CSE",
                    title = "UPSC Annual Examination Schedule & Notification",
                    noticeType = "Exam Date",
                    summary = "Union Public Service Commission announced Civil Services Preliminary & Mains examination calendar.",
                    officialLink = "https://upsc.gov.in",
                    publishDate = "Official Release",
                    isVerifiedOfficial = true,
                    createdAt = now
                )
            )
        )

        // Seed initial intelligence engine masteries & exam objectives
        val user = database.userDao().getUserProfileOnce()
        if (user != null) {
            intelligenceRepository.seedInitialIntelligenceIfEmpty(user)
        }
    }

    // --- Intelligence Engine Access & Delegation Methods ---

    suspend fun saveExamObjective(objective: ExamObjective): Long =
        intelligenceRepository.saveExamObjective(objective)

    suspend fun updateExamObjective(objective: ExamObjective) =
        intelligenceRepository.updateExamObjective(objective)

    suspend fun setActiveExamObjective(id: Long) =
        intelligenceRepository.setActiveExamObjective(id)

    suspend fun recordTopicPerformance(
        subject: String,
        topic: String,
        questionsAttempted: Int,
        correctCount: Int,
        difficulty: String = "Medium",
        weakSpots: List<String> = emptyList()
    ): TopicMastery = intelligenceRepository.recordTopicPerformance(
        subject = subject,
        topic = topic,
        questionsAttempted = questionsAttempted,
        correctCount = correctCount,
        difficulty = difficulty,
        weakSpots = weakSpots
    )

    suspend fun saveTopicMastery(topicMastery: TopicMastery): Long =
        intelligenceRepository.saveTopicMastery(topicMastery)

    suspend fun recordStudentSessionHistory(
        sessionType: String,
        subject: String,
        topic: String,
        durationMinutes: Int,
        actualMinutesSpent: Int,
        xpEarned: Int,
        accuracyPercent: Float? = null,
        questionsAttempted: Int = 0,
        productivityRating: Int = 4,
        notesSummary: String = ""
    ): StudentSessionHistory = intelligenceRepository.recordStudySession(
        sessionType = sessionType,
        subject = subject,
        topic = topic,
        durationMinutes = durationMinutes,
        actualMinutesSpent = actualMinutesSpent,
        xpEarned = xpEarned,
        accuracyPercent = accuracyPercent,
        questionsAttempted = questionsAttempted,
        productivityRating = productivityRating,
        notesSummary = notesSummary
    )

    suspend fun generateIntelligenceSnapshot(): IntelligenceSnapshot =
        intelligenceRepository.generateAndPersistSnapshot()

    suspend fun setUserManualOverride(examId: String, subject: String, topic: String, override: String) =
        intelligenceRepository.setUserManualOverride(examId, subject, topic, override)

    suspend fun resetUserExamPreparationData(examId: String) =
        intelligenceRepository.resetUserExamPreparationData(examId)

    val allQuestionHistory: Flow<List<QuestionHistoryEntity>> = database.questionHistoryDao().getAllHistory()
    val allQuestionQualityReports: Flow<List<QuestionQualityReportEntity>> = database.questionQualityReportDao().getAllReports()

    suspend fun recordQuestionAttemptHistory(
        questionId: String,
        examId: String,
        subject: String,
        topic: String,
        isCorrect: Boolean,
        isSkipped: Boolean,
        responseTimeSecs: Int
    ) = withContext(Dispatchers.IO) {
        val existing = database.questionHistoryDao().getHistoryForQuestion(questionId)
        val attemptCount = (existing?.attemptCount ?: 0) + 1
        val correctCount = (existing?.correctCount ?: 0) + (if (isCorrect) 1 else 0)
        val incorrectCount = (existing?.incorrectCount ?: 0) + (if (!isCorrect && !isSkipped) 1 else 0)
        val resultStr = if (isSkipped) "SKIPPED" else if (isCorrect) "CORRECT" else "INCORRECT"

        val updated = QuestionHistoryEntity(
            id = "current_user_$questionId",
            userId = "current_user",
            questionId = questionId,
            examId = examId,
            subject = subject,
            topic = topic,
            attemptCount = attemptCount,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            lastAttemptedAt = System.currentTimeMillis(),
            lastResult = resultStr,
            lastResponseTimeSecs = responseTimeSecs
        )
        database.questionHistoryDao().insertOrUpdateHistory(updated)
    }

    suspend fun reportQuestionQuality(
        questionId: String,
        examId: String,
        reason: String,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        val report = QuestionQualityReportEntity(
            userId = "current_user",
            questionId = questionId,
            examId = examId,
            reason = reason,
            notes = notes,
            status = "UNDER_REVIEW"
        )
        database.questionQualityReportDao().insertReport(report)
    }

    suspend fun getFlaggedQuestionIds(): List<String> = withContext(Dispatchers.IO) {
        database.questionQualityReportDao().getFlaggedQuestionIds()
    }
}

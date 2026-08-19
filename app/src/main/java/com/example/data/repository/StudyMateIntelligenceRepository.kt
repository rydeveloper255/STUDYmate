package com.example.data.repository

import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import com.example.service.intelligence.StudyMateIntelligenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Room-based Master Intelligence Repository.
 * Models student context: exam objectives, session history, and topic mastery levels.
 * Follows the repository pattern and exposes reactive Flow streams and transactional updates.
 */
class StudyMateIntelligenceRepository(
    private val database: StudyMateDatabase,
    private val syncService: com.example.data.remote.supabase.SupabaseSyncService? = null
) {
    private val examObjectiveDao = database.examObjectiveDao()
    private val topicMasteryDao = database.topicMasteryDao()
    private val sessionHistoryDao = database.studentSessionHistoryDao()
    private val snapshotDao = database.intelligenceSnapshotDao()
    private val userDao = database.userDao()
    private val planDao = database.studyPlanDao()
    private val focusDao = database.focusDao()
    private val mockDao = database.mockTestDao()
    private val mistakeDao = database.mistakeDao()
    private val flashcardDao = database.flashcardDao()

    // 1. Reactive Streams
    val allExamObjectives: Flow<List<ExamObjective>> = examObjectiveDao.getAllExamObjectives()
    val activeExamObjective: Flow<ExamObjective?> = examObjectiveDao.getActiveExamObjective()

    val allTopicMasteries: Flow<List<TopicMastery>> = topicMasteryDao.getAllTopicMasteries()
    val weakTopics: Flow<List<TopicMastery>> = topicMasteryDao.getWeakTopics(threshold = 65)
    val masteredTopics: Flow<List<TopicMastery>> = topicMasteryDao.getMasteredTopics(threshold = 85)

    val allSessionHistory: Flow<List<StudentSessionHistory>> = sessionHistoryDao.getAllSessions()
    val recentSessions: Flow<List<StudentSessionHistory>> = sessionHistoryDao.getRecentSessions(limit = 20)
    val latestSnapshot: Flow<IntelligenceSnapshot?> = snapshotDao.getLatestSnapshot()

    fun getTopicMasteriesBySubject(subject: String): Flow<List<TopicMastery>> {
        return topicMasteryDao.getTopicMasteriesBySubject(subject)
    }

    fun getSessionsByType(sessionType: String): Flow<List<StudentSessionHistory>> {
        return sessionHistoryDao.getSessionsByType(sessionType)
    }

    fun getSessionsBySubject(subject: String): Flow<List<StudentSessionHistory>> {
        return sessionHistoryDao.getSessionsBySubject(subject)
    }

    /**
     * Unified Student Master Context Flow combining profile, plans, focus, tests, mistakes, flashcards, and topic masteries.
     */
    val studentMasterContext: Flow<StudentMasterContext?> = combine(
        userDao.getUserProfile(),
        planDao.getAllPlanItems(),
        focusDao.getAllFocusSessions(),
        mockDao.getAllAttempts(),
        mistakeDao.getAllMistakes(),
        flashcardDao.getAllFlashcards()
    ) { args: Array<Any?> ->
        val profile = args[0] as? UserProfile
        val plans = (args[1] as? List<*>)?.filterIsInstance<StudyPlanItem>() ?: emptyList()
        val focus = (args[2] as? List<*>)?.filterIsInstance<FocusSession>() ?: emptyList()
        val mockAttempts = (args[3] as? List<*>)?.filterIsInstance<MockTestAttempt>() ?: emptyList()
        val mistakes = (args[4] as? List<*>)?.filterIsInstance<MistakeItem>() ?: emptyList()
        val cards = (args[5] as? List<*>)?.filterIsInstance<FlashcardItem>() ?: emptyList()

        if (profile == null) null
        else {
            StudyMateIntelligenceEngine.buildMasterContext(
                profile = profile,
                plans = plans,
                focusSessions = focus,
                mockAttempts = mockAttempts,
                mistakes = mistakes,
                flashcards = cards
            )
        }
    }.flowOn(Dispatchers.Default)

    // 2. Exam Objectives Operations
    suspend fun saveExamObjective(objective: ExamObjective): Long = withContext(Dispatchers.IO) {
        val id = examObjectiveDao.insertObjective(objective)
        if (objective.status == "ACTIVE") {
            examObjectiveDao.setActiveObjective(id)
            // Sync user profile exam metadata
            val user = userDao.getUserProfileOnce()
            if (user != null) {
                val updatedUser = user.copy(
                    examName = objective.examName,
                    examDateMillis = objective.examDateMillis
                )
                userDao.insertOrUpdateUserProfile(updatedUser)
                syncService?.syncUserProfile(updatedUser)
            }
        }
        syncService?.syncExamObjective(objective.copy(id = id))
        id
    }

    suspend fun updateExamObjective(objective: ExamObjective) = withContext(Dispatchers.IO) {
        examObjectiveDao.updateObjective(objective)
        syncService?.syncExamObjective(objective)
    }

    suspend fun setActiveExamObjective(id: Long) = withContext(Dispatchers.IO) {
        examObjectiveDao.setActiveObjective(id)
    }

    suspend fun deleteExamObjective(id: Long) = withContext(Dispatchers.IO) {
        examObjectiveDao.deleteObjective(id)
    }

    // 3. Topic Mastery Operations & Adaptive Tracking
    suspend fun recordTopicPerformance(
        subject: String,
        topic: String,
        questionsAttempted: Int,
        correctCount: Int,
        difficulty: String = "Medium",
        weakSpots: List<String> = emptyList()
    ): TopicMastery = withContext(Dispatchers.IO) {
        val existing = topicMasteryDao.getTopicMasteryOnce(subject, topic)
        val now = System.currentTimeMillis()

        val totalAttempted = (existing?.totalQuestionsAttempted ?: 0) + questionsAttempted
        val totalCorrect = (existing?.correctQuestionsCount ?: 0) + correctCount
        val totalIncorrect = totalAttempted - totalCorrect
        val accuracy = if (totalAttempted > 0) (totalCorrect.toFloat() / totalAttempted) * 100f else 60f

        val easyCount = (existing?.easySolved ?: 0) + if (difficulty.equals("Easy", ignoreCase = true)) correctCount else 0
        val medCount = (existing?.medSolved ?: 0) + if (difficulty.equals("Medium", ignoreCase = true)) correctCount else 0
        val hardCount = (existing?.hardSolved ?: 0) + if (difficulty.equals("Hard", ignoreCase = true)) correctCount else 0

        // Weighted Mastery Calculation
        val rawMastery = (accuracy * 0.6f + (hardCount * 5 + medCount * 2 + easyCount * 1).coerceAtMost(40)).roundToInt().coerceIn(5, 100)

        val level = when {
            rawMastery >= 85 -> "MASTERED"
            rawMastery >= 70 -> "PROFICIENT"
            rawMastery >= 45 -> "DEVELOPING"
            else -> "NOVICE"
        }

        // Adaptive review interval based on mastery level
        val reviewIntervalDays = when (level) {
            "MASTERED" -> 7
            "PROFICIENT" -> 4
            "DEVELOPING" -> 2
            else -> 1
        }
        val nextReviewMillis = now + (reviewIntervalDays * 24L * 60 * 60 * 1000)

        val combinedWeakSpots = ((existing?.weakSpots ?: emptyList()) + weakSpots).distinct().takeLast(5)

        val updated = TopicMastery(
            id = existing?.id ?: 0,
            subject = subject,
            topic = topic,
            masteryScore = rawMastery,
            accuracyPercent = accuracy,
            totalQuestionsAttempted = totalAttempted,
            correctQuestionsCount = totalCorrect,
            incorrectQuestionsCount = totalIncorrect,
            easySolved = easyCount,
            medSolved = medCount,
            hardSolved = hardCount,
            retentionDecayRate = if (rawMastery > 75) 0.8f else 1.2f,
            lastTestedMillis = now,
            recommendedReviewDateMillis = nextReviewMillis,
            masteryLevel = level,
            weakSpots = combinedWeakSpots,
            updatedAt = now
        )

        topicMasteryDao.insertOrUpdateTopicMastery(updated)
        syncService?.syncTopicMastery(updated)
        updated
    }

    suspend fun saveTopicMastery(topicMastery: TopicMastery): Long = withContext(Dispatchers.IO) {
        val id = topicMasteryDao.insertOrUpdateTopicMastery(topicMastery)
        syncService?.syncTopicMastery(topicMastery.copy(id = id))
        id
    }

    suspend fun saveTopicMasteries(items: List<TopicMastery>) = withContext(Dispatchers.IO) {
        topicMasteryDao.insertTopicMasteries(items)
        items.forEach { syncService?.syncTopicMastery(it) }
    }

    suspend fun deleteTopicMastery(id: Long) = withContext(Dispatchers.IO) {
        topicMasteryDao.deleteTopicMastery(id)
    }

    // 4. Student Session History Operations
    suspend fun recordStudySession(
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
    ): StudentSessionHistory = withContext(Dispatchers.IO) {
        val session = StudentSessionHistory(
            sessionType = sessionType,
            subject = subject,
            topic = topic,
            durationMinutes = durationMinutes,
            actualMinutesSpent = actualMinutesSpent,
            xpEarned = xpEarned,
            accuracyPercent = accuracyPercent,
            questionsAttempted = questionsAttempted,
            productivityRating = productivityRating,
            notesSummary = notesSummary,
            timestamp = System.currentTimeMillis()
        )
        val id = sessionHistoryDao.insertSession(session)
        val savedSession = session.copy(id = id)
        syncService?.syncStudySessionHistory(savedSession)

        // Award XP and increment total focus time
        val user = userDao.getUserProfileOnce()
        if (user != null) {
            val newXp = user.xp + xpEarned
            val newFocus = user.totalFocusMinutes + actualMinutesSpent
            val newLevel = (newXp / 250) + 1
            val updatedUser = user.copy(
                xp = newXp,
                totalFocusMinutes = newFocus,
                level = newLevel
            )
            userDao.insertOrUpdateUserProfile(updatedUser)
            syncService?.syncUserProfile(updatedUser)
        }
        savedSession
    }

    suspend fun deleteSessionHistory(id: Long) = withContext(Dispatchers.IO) {
        sessionHistoryDao.deleteSession(id)
    }

    // 5. Intelligence Snapshot & Analysis
    suspend fun generateAndPersistSnapshot(): IntelligenceSnapshot = withContext(Dispatchers.IO) {
        val user = userDao.getUserProfileOnce()
        val now = System.currentTimeMillis()
        val daysRemaining = if (user != null) {
            ((user.examDateMillis - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(0).toInt()
        } else 60

        val masteries = topicMasteryDao.getAllTopicMasteries().firstOrNull() ?: emptyList()
        val avgMastery = if (masteries.isNotEmpty()) {
            masteries.map { it.masteryScore }.average().roundToInt()
        } else 65

        val weakCount = masteries.count { it.masteryScore < 65 }
        val masteredCount = masteries.count { it.masteryScore >= 85 }
        val syllabusProgress = if (masteries.isNotEmpty()) {
            ((masteredCount.toFloat() / masteries.size) * 100).roundToInt()
        } else 35

        val plans = planDao.getAllPlanItems().firstOrNull() ?: emptyList()
        val focusSessions = focusDao.getAllFocusSessions().firstOrNull() ?: emptyList()
        val mockAttempts = mockDao.getAllAttempts().firstOrNull() ?: emptyList()
        val mistakes = mistakeDao.getAllMistakes().firstOrNull() ?: emptyList()
        val flashcards = flashcardDao.getAllFlashcards().firstOrNull() ?: emptyList()

        val nextBestAction = if (user != null) {
            StudyMateIntelligenceEngine.calculateNextBestAction(
                profile = user,
                plans = plans,
                mockAttempts = mockAttempts,
                mistakes = mistakes,
                flashcards = flashcards
            )
        } else {
            NextBestAction(
                title = "Revise Physics Core Formulas",
                subject = "Physics",
                topic = "Mechanics",
                durationMinutes = 25,
                actionType = NextBestActionType.SPACED_REVISION,
                urgencyTag = "High Priority 🔥",
                reason = "Target high-yield formula recall.",
                whyThisHelpful = "Increases retention before mock tests."
            )
        }

        val goalRadar = if (user != null) {
            StudyMateIntelligenceEngine.calculateGoalRadar(
                profile = user,
                plans = plans,
                focusSessions = focusSessions,
                mockAttempts = mockAttempts
            )
        } else null

        val readinessIndex = (avgMastery * 0.5f + (syllabusProgress * 0.3f) + ((goalRadar?.weeklyHoursCompleted ?: 10f) * 2f)).coerceIn(10f, 99f)

        val snapshot = IntelligenceSnapshot(
            timestamp = now,
            examDaysRemaining = daysRemaining,
            overallMasteryScore = avgMastery,
            syllabusCompletionPercent = syllabusProgress,
            readinessIndex = readinessIndex,
            topRecommendedActionTitle = nextBestAction.title,
            topRecommendedSubject = nextBestAction.subject,
            topRecommendedTopic = nextBestAction.topic,
            pacingStatus = goalRadar?.studyPaceStatus ?: "On Track 🚀",
            insightsSummary = goalRadar?.calmAdvice ?: "Maintain steady focus sessions and regular active recall.",
            weakTopicsCount = weakCount,
            masteredTopicsCount = masteredCount
        )

        snapshotDao.insertSnapshot(snapshot)
        syncService?.syncIntelligenceSnapshot(snapshot)
        snapshot
    }

    /**
     * Seeds initial baseline topic masteries and exam objective if empty.
     */
    suspend fun seedInitialIntelligenceIfEmpty(profile: UserProfile) = withContext(Dispatchers.IO) {
        val existingMasteries = topicMasteryDao.getAllTopicMasteries().firstOrNull() ?: emptyList()
        if (existingMasteries.isEmpty()) {
            val initial = listOf(
                TopicMastery(
                    subject = "Physics",
                    topic = "Newton's Laws & Friction",
                    masteryScore = 78,
                    accuracyPercent = 80f,
                    totalQuestionsAttempted = 25,
                    correctQuestionsCount = 20,
                    incorrectQuestionsCount = 5,
                    easySolved = 10,
                    medSolved = 8,
                    hardSolved = 2,
                    masteryLevel = "PROFICIENT"
                ),
                TopicMastery(
                    subject = "Physics",
                    topic = "Rotational Dynamics",
                    masteryScore = 48,
                    accuracyPercent = 52f,
                    totalQuestionsAttempted = 30,
                    correctQuestionsCount = 15,
                    incorrectQuestionsCount = 15,
                    easySolved = 8,
                    medSolved = 5,
                    hardSolved = 2,
                    masteryLevel = "DEVELOPING",
                    weakSpots = listOf("Moment of inertia for non-uniform rods", "Angular momentum conservation in collisions")
                ),
                TopicMastery(
                    subject = "Chemistry",
                    topic = "Chemical Equilibrium & Le Chatelier",
                    masteryScore = 86,
                    accuracyPercent = 88f,
                    totalQuestionsAttempted = 28,
                    correctQuestionsCount = 25,
                    incorrectQuestionsCount = 3,
                    easySolved = 12,
                    medSolved = 10,
                    hardSolved = 3,
                    masteryLevel = "MASTERED"
                ),
                TopicMastery(
                    subject = "Chemistry",
                    topic = "Organic Reaction Mechanisms",
                    masteryScore = 55,
                    accuracyPercent = 58f,
                    totalQuestionsAttempted = 35,
                    correctQuestionsCount = 20,
                    incorrectQuestionsCount = 15,
                    easySolved = 9,
                    medSolved = 8,
                    hardSolved = 3,
                    masteryLevel = "DEVELOPING",
                    weakSpots = listOf("Electrophilic addition regioselectivity", "Carbocation rearrangement")
                ),
                TopicMastery(
                    subject = "Mathematics",
                    topic = "Differential Calculus & Maxima/Minima",
                    masteryScore = 82,
                    accuracyPercent = 84f,
                    totalQuestionsAttempted = 40,
                    correctQuestionsCount = 34,
                    incorrectQuestionsCount = 6,
                    easySolved = 15,
                    medSolved = 14,
                    hardSolved = 5,
                    masteryLevel = "PROFICIENT"
                ),
                TopicMastery(
                    subject = "Mathematics",
                    topic = "Integral Calculus & Definite Integrals",
                    masteryScore = 60,
                    accuracyPercent = 62f,
                    totalQuestionsAttempted = 32,
                    correctQuestionsCount = 20,
                    incorrectQuestionsCount = 12,
                    easySolved = 10,
                    medSolved = 8,
                    hardSolved = 2,
                    masteryLevel = "DEVELOPING",
                    weakSpots = listOf("Integration by parts cyclic integrals", "Properties of definite integrals with modulus")
                )
            )
            topicMasteryDao.insertTopicMasteries(initial)
        }

        val existingObjectives = examObjectiveDao.getAllExamObjectives().firstOrNull() ?: emptyList()
        if (existingObjectives.isEmpty()) {
            val objective = ExamObjective(
                examName = profile.examName,
                targetScoreOrRank = "Top 500 AIR",
                examDateMillis = profile.examDateMillis,
                category = "Competitive Exam",
                targetWeeklyStudyHours = 28f,
                totalSyllabusTopicsCount = 120,
                completedSyllabusTopicsCount = 45,
                prioritySubjects = profile.subjects.ifEmpty { listOf("Physics", "Mathematics", "Chemistry") },
                status = "ACTIVE"
            )
            examObjectiveDao.insertObjective(objective)
        }
    }
}

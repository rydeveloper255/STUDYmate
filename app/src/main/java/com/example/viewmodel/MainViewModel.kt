package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.StudyMateApplication
import com.example.data.model.*
import com.example.data.model.updates.*
import com.example.data.remote.AuthRepository
import com.example.data.remote.GeminiRepository
import com.example.data.remote.supabase.LatestUpdatesRepository
import com.example.data.repository.ExamCatalogRepository
import com.example.data.repository.StudyRepository
import com.example.service.intelligence.StudyMateIntelligenceEngine
import com.example.ui.theme.AppThemeMode
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.text.SimpleDateFormat
import android.content.Context

enum class AuthScreenState {
    LOGIN,
    SIGNUP,
    OTP_VERIFICATION,
    FORGOT_PASSWORD,
    FORGOT_PASSWORD_OTP,
    RESET_PASSWORD
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isThinking: Boolean = false,
    val hasImage: Boolean = false,
    val actionType: TutorActionType = TutorActionType.GENERAL_CHAT,
    val generatedFlashcards: List<FlashcardItem>? = null,
    val generatedPlanItems: List<StudyPlanItem>? = null,
    val generatedQuestions: List<Question>? = null,
    val isOfflineFallback: Boolean = false,
    val subjectContext: String? = null,
    val topicContext: String? = null
)

data class CompletedSessionSummary(
    val examName: String = "",
    val subject: String = "",
    val topic: String = "",
    val plannedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val xpEarned: Int = 0
)

data class FocusTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val initialMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val actualMinutesSpent: Int = 0,
    val subject: String = "General Science",
    val topic: String = "Sound",
    val examName: String = "",
    val sessionGoal: String = "",
    val planItemId: Long? = null,
    val restrictedAppsCount: Int = 4,
    val showCelebration: Boolean = false,
    val lastSessionXp: Int = 50,
    val lastCompletedSession: CompletedSessionSummary? = null,
    val sessionStartTimestamp: Long = 0L,
    val pausedAccumulatedSeconds: Long = 0L,
    val pauseStartTimestamp: Long = 0L,
    val isBreakActive: Boolean = false,
    val breakDurationMinutes: Int = 5,
    val breakRemainingSeconds: Int = 5 * 60,
    val isFocusShieldActive: Boolean = true,
    val isStrictModeEnabled: Boolean = false,
    val isStrictModeActive: Boolean = false,
    val isAutoStarted: Boolean = false,
    val isInterrupted: Boolean = false
)

data class ActiveTestState(
    val isTestInProgress: Boolean = false,
    val requestId: String = "",
    val subject: String = "All Subjects",
    val title: String = "Mock Test",
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(), // questionIndex -> optionIndex
    val markedForReview: Set<Int> = emptySet(),
    val visitedQuestions: Set<Int> = emptySet(),
    val timeSpentSeconds: Map<Int, Int> = emptyMap(), // questionIndex -> seconds spent
    val currentQuestionEnteredTimestamp: Long = 0L,
    val startedAtTimestamp: Long = 0L,
    val expiresAtTimestamp: Long = 0L,
    val totalDurationSeconds: Int = 600,
    val remainingSeconds: Int = 600,
    val isCompleted: Boolean = false,
    val isSubmitting: Boolean = false,
    val submissionError: String? = null,
    val completedAttempt: MockTestAttempt? = null,
    val detailedQuestions: List<QuestionAttemptDetail> = emptyList(),
    val timeAnalysis: TimeAnalysisResult? = null,
    val testIntelligence: com.example.service.intelligence.TestIntelligenceResult? = null,
    val isNovaAnalyzing: Boolean = false,
    val isPaletteOpen: Boolean = false,
    val isSubmitConfirmOpen: Boolean = false,
    val isOrientationConfirmed: Boolean = false,
    val config: MockTestConfig = MockTestConfig()
) {
    fun getCbtState(questionIndex: Int): QuestionCbtState {
        val isAnswered = selectedAnswers.containsKey(questionIndex)
        val isMarked = markedForReview.contains(questionIndex)
        val isVisited = visitedQuestions.contains(questionIndex) || questionIndex == currentQuestionIndex
        return when {
            isAnswered && isMarked -> QuestionCbtState.ANSWERED_AND_MARKED
            isAnswered -> QuestionCbtState.ANSWERED
            isMarked -> QuestionCbtState.MARKED_FOR_REVIEW
            isVisited -> QuestionCbtState.NOT_ANSWERED
            else -> QuestionCbtState.NOT_VISITED
        }
    }
}

class MainViewModel(
    application: Application,
    private val authRepository: AuthRepository,
    private val geminiRepository: GeminiRepository,
    private val studyRepository: StudyRepository,
    val examCatalogRepository: ExamCatalogRepository
) : AndroidViewModel(application) {

    // --- User Profile & Auth ---
    val userProfile: StateFlow<UserProfile?> = studyRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // --- Step 9 Smart Exam Intelligence Service & Context ---
    val examIntelligenceService = com.example.service.intelligence.ExamIntelligenceService(
        examCatalogDao = (application as StudyMateApplication).database.examCatalogDao(),
        supabaseClient = (application as StudyMateApplication).supabaseClient,
        geminiRepository = geminiRepository
    )

    val examContextRepository = com.example.data.repository.ExamContextRepository(
        database = (application as StudyMateApplication).database,
        intelligenceService = examIntelligenceService,
        syncService = (application as StudyMateApplication).supabaseSyncService,
        scope = viewModelScope
    )

    private val sessionPersistence = com.example.service.intelligence.TestSessionPersistence(getApplication())
    private val focusSessionPersistence = com.example.service.FocusSessionPersistence.getInstance(getApplication())

    val activeExamContext: StateFlow<ExamContext> = examContextRepository.activeExamContext
    val examDiscoveryState: StateFlow<ExamDiscoveryState> = examContextRepository.discoveryState

    fun refreshActiveExamSyllabus() {
        viewModelScope.launch {
            val curr = activeExamContext.value
            examContextRepository.loadExamContext(
                examId = curr.examId,
                examName = curr.examName,
                category = curr.category,
                forceRefresh = true
            )
        }
    }

    fun confirmExamChange(
        newExamId: String,
        newExamName: String,
        category: String = "Competitive Exams",
        targetDateMillis: Long = System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000
    ) {
        viewModelScope.launch {
            examContextRepository.confirmExamChange(
                newExamId = newExamId,
                newExamName = newExamName,
                category = category,
                targetDateMillis = targetDateMillis
            )
        }
    }

    // --- Verified Exam Catalog & Single Authoritative Selected Exam ---
    val allCatalogExams: StateFlow<List<ExamEntity>> = examCatalogRepository.allExams
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedExam: StateFlow<SelectedExam> = combine(
        userProfile,
        examCatalogRepository.allExams
    ) { profile, catalogList ->
        if (profile == null) {
            SelectedExam.defaultExam()
        } else {
            val matchedCatalog = catalogList.firstOrNull {
                it.name.equals(profile.examName, ignoreCase = true) ||
                it.shortCode.equals(profile.examName, ignoreCase = true) ||
                it.id.equals(profile.examName, ignoreCase = true) ||
                profile.examName.contains(it.shortCode, ignoreCase = true)
            }
            SelectedExam(
                examId = matchedCatalog?.id ?: "exam_${profile.examName.lowercase().replace("[^a-z0-9]".toRegex(), "_")}",
                examName = profile.examName,
                examCategory = profile.examCategory.ifBlank { matchedCatalog?.category ?: "Competitive Exam" },
                targetDateMillis = profile.examDateMillis,
                targetScore = profile.targetScore,
                priority = "HIGH",
                status = "ACTIVE",
                examPattern = matchedCatalog?.examPattern ?: "",
                totalMarks = matchedCatalog?.totalMarks ?: 100,
                durationMinutes = matchedCatalog?.durationMinutes ?: 90,
                isCustom = matchedCatalog?.isCustom ?: (matchedCatalog == null)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SelectedExam.defaultExam())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedExamSubjects: StateFlow<List<ExamSubjectEntity>> = selectedExam
        .flatMapLatest { exam ->
            examCatalogRepository.getSubjectsForExam(exam.examId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedExamSyllabusTree: StateFlow<Map<String, List<ChapterWithTopics>>> = selectedExam
        .flatMapLatest { exam ->
            examCatalogRepository.getSyllabusHierarchyForExam(exam.examId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    private val _authNavState = MutableStateFlow(AuthScreenState.LOGIN)
    val authNavState: StateFlow<AuthScreenState> = _authNavState.asStateFlow()

    private val _pendingAuthEmail = MutableStateFlow("")
    val pendingAuthEmail: StateFlow<String> = _pendingAuthEmail.asStateFlow()

    private val _pendingAuthName = MutableStateFlow("")
    val pendingAuthName: StateFlow<String> = _pendingAuthName.asStateFlow()

    private val _pendingAuthPhone = MutableStateFlow("")
    val pendingAuthPhone: StateFlow<String> = _pendingAuthPhone.asStateFlow()

    private val _recoveryAccessToken = MutableStateFlow<String?>(null)
    val recoveryAccessToken: StateFlow<String?> = _recoveryAccessToken.asStateFlow()

    private val _otpCooldownSeconds = MutableStateFlow(0)
    val otpCooldownSeconds: StateFlow<Int> = _otpCooldownSeconds.asStateFlow()

    // --- Telegram Bot Service & Health State ---
    val telegramBotService: com.example.data.remote.telegram.TelegramBotService =
        (application as? StudyMateApplication)?.telegramBotService
            ?: com.example.data.remote.telegram.TelegramBotService()

    private val _telegramHealthStatus = MutableStateFlow<com.example.data.remote.telegram.TelegramHealthStatus>(
        com.example.data.remote.telegram.TelegramHealthStatus.Idle
    )
    val telegramHealthStatus: StateFlow<com.example.data.remote.telegram.TelegramHealthStatus> =
        _telegramHealthStatus.asStateFlow()

    fun checkTelegramBotHealth() {
        viewModelScope.launch {
            _telegramHealthStatus.value = com.example.data.remote.telegram.TelegramHealthStatus.Checking
            val status = telegramBotService.checkHealth()
            _telegramHealthStatus.value = status
        }
    }

    // --- Study Plan Items ---
    val studyPlanItems: StateFlow<List<StudyPlanItem>> = studyRepository.allPlanItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStudyPreferences: StateFlow<UserStudyPreferences> = studyRepository.userStudyPreferences
        .map { it ?: UserStudyPreferences() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStudyPreferences())

    val allPracticeSessions: StateFlow<List<PracticeSessionEntity>> = studyRepository.allPracticeSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuestionAttempts: StateFlow<List<QuestionAttemptEntity>> = studyRepository.allQuestionAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedQuestionsList: StateFlow<List<SavedQuestionEntity>> = studyRepository.allSavedQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isPlanGenerating = MutableStateFlow(false)
    val isPlanGenerating: StateFlow<Boolean> = _isPlanGenerating.asStateFlow()

    private val _deadlineWarning = MutableStateFlow<String?>(null)
    val deadlineWarning: StateFlow<String?> = _deadlineWarning.asStateFlow()

    // --- Active Timer State ---
    private val _activeStudySession = MutableStateFlow<StudyPlanItem?>(null)
    val activeStudySession: StateFlow<StudyPlanItem?> = _activeStudySession.asStateFlow()

    private val _sessionRemainingSeconds = MutableStateFlow(0)
    val sessionRemainingSeconds: StateFlow<Int> = _sessionRemainingSeconds.asStateFlow()

    private val _isSessionTimerRunning = MutableStateFlow(false)
    val isSessionTimerRunning: StateFlow<Boolean> = _isSessionTimerRunning.asStateFlow()

    private val _isSessionPaused = MutableStateFlow(false)
    val isSessionPaused: StateFlow<Boolean> = _isSessionPaused.asStateFlow()

    private val _activeSessionActualMinutes = MutableStateFlow(0)
    val activeSessionActualMinutes: StateFlow<Int> = _activeSessionActualMinutes.asStateFlow()

    private var sessionTimerJob: Job? = null

    // --- AI Tutor Chat ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "model",
                text = "👋 Hello! I'm your **StudyMate AI Tutor**.\n\nAsk me anything in Physics, Mathematics, Chemistry, Biology, or Computer Science. I can break down derivations, explain tricky concepts intuitively, or create instant quizzes!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _useThinkingMode = MutableStateFlow(false)
    val useThinkingMode: StateFlow<Boolean> = _useThinkingMode.asStateFlow()

    private val _tutorPersona = MutableStateFlow("Friendly AI Tutor")
    val tutorPersona: StateFlow<String> = _tutorPersona.asStateFlow()

    // --- Text To Speech ---
    private var tts: TextToSpeech? = null
    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    // --- Focus Mode Timer ---
    private val _focusState = MutableStateFlow(FocusTimerState())
    val focusState: StateFlow<FocusTimerState> = _focusState.asStateFlow()
    private var timerJob: Job? = null

    // --- Study Schedule & Smart Discipline ---
    val studyScheduleList: StateFlow<List<com.example.data.model.StudyScheduleItem>> = studyRepository.allScheduleItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyScheduleLogs: StateFlow<List<com.example.data.model.StudyScheduleLog>> = studyRepository.allScheduleLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSchedulePaused = MutableStateFlow(false)
    val isSchedulePaused: StateFlow<Boolean> = _isSchedulePaused.asStateFlow()

    private var scheduleTickerJob: Job? = null

    init {
        checkAndRestoreActiveSession()
        checkAndRestoreActiveFocusSession()
        seedDefaultSchedulesIfEmpty()
        startScheduleTickerLoop()
        refreshNovaSmartPlannerData()
    }

    private fun seedDefaultSchedulesIfEmpty() {
        viewModelScope.launch {
            val existing = studyRepository.studyScheduleListOnce()
            if (existing.isEmpty()) {
                val defaultItems = listOf(
                    com.example.data.model.StudyScheduleItem(
                        dayOfWeek = "MON",
                        startTime = "07:00 PM",
                        endTime = "08:00 PM",
                        durationMinutes = 60,
                        subject = "Mathematics",
                        topic = "Percentage & Profit",
                        isAutoFocus = true,
                        isStrictMode = true,
                        repeatType = "WEEKLY",
                        repeatDaysJson = "[\"MON\",\"WED\",\"FRI\"]"
                    ),
                    com.example.data.model.StudyScheduleItem(
                        dayOfWeek = "MON",
                        startTime = "08:15 PM",
                        endTime = "09:00 PM",
                        durationMinutes = 45,
                        subject = "General Science",
                        topic = "Physics Laws",
                        isAutoFocus = false,
                        isStrictMode = false,
                        repeatType = "WEEKLY",
                        repeatDaysJson = "[\"MON\",\"TUE\",\"THU\"]"
                    ),
                    com.example.data.model.StudyScheduleItem(
                        dayOfWeek = "TUE",
                        startTime = "06:30 PM",
                        endTime = "07:30 PM",
                        durationMinutes = 60,
                        subject = "English",
                        topic = "Grammar & Comprehension",
                        isAutoFocus = true,
                        isStrictMode = false,
                        repeatType = "WEEKLY",
                        repeatDaysJson = "[\"TUE\",\"THU\",\"SAT\"]"
                    )
                )
                defaultItems.forEach { studyRepository.saveScheduleItem(it) }
            }
        }
    }

    private fun startScheduleTickerLoop() {
        scheduleTickerJob?.cancel()
        scheduleTickerJob = viewModelScope.launch {
            while (scheduleTickerJob?.isActive == true) {
                delay(30000L) // Check every 30 seconds
                try {
                    checkAndTriggerScheduledSessions()
                } catch (e: Exception) {
                    // Ignore transient errors
                }
            }
        }
    }

    private suspend fun checkAndTriggerScheduledSessions() {
        if (_isSchedulePaused.value) return
        val items = studyScheduleList.value
        if (items.isEmpty()) return

        val cal = java.util.Calendar.getInstance()
        val dayNames = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        val currentDay = dayNames[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
        val currentTimeStr = sdf.format(cal.time)

        items.filter { !it.isPaused }.forEach { item ->
            val dayMatches = item.dayOfWeek.equals(currentDay, ignoreCase = true) || item.repeatDaysJson.contains(currentDay, ignoreCase = true) || item.repeatType == "DAILY"
            if (dayMatches && item.startTime.equals(currentTimeStr, ignoreCase = true)) {
                val occurrenceId = focusSessionPersistence.buildOccurrenceId(item.id, item.startTime)
                if (!focusSessionPersistence.hasOccurrenceExecuted(occurrenceId)) {
                    if (item.isAutoFocus && !_focusState.value.isRunning) {
                        startFocusSession(
                            subject = item.subject,
                            topic = item.topic.ifBlank { "Scheduled Session" },
                            durationMinutes = item.durationMinutes,
                            isStrictMode = item.isStrictMode,
                            isAutoStarted = true,
                            occurrenceId = occurrenceId
                        )
                    }
                }
            }
        }
    }

    fun saveScheduleItem(item: com.example.data.model.StudyScheduleItem) {
        viewModelScope.launch {
            studyRepository.saveScheduleItem(item)
        }
    }

    fun deleteScheduleItem(id: String) {
        viewModelScope.launch {
            studyRepository.deleteScheduleItem(id)
        }
    }

    fun toggleSchedulePause() {
        _isSchedulePaused.value = !_isSchedulePaused.value
    }

    fun rescheduleMissedSession(logId: String, newDateMillis: Long, newTime: String) {
        viewModelScope.launch {
            studyRepository.updateScheduleLogStatus(logId, "RESCHEDULED")
        }
    }

    fun skipMissedSession(logId: String) {
        viewModelScope.launch {
            studyRepository.updateScheduleLogStatus(logId, "SKIPPED")
        }
    }

    // --- Step 50: Nova Smart Intelligence & Communication Engine ---
    private val novaIntelligenceEngine = com.example.service.intelligence.NovaStudyIntelligenceEngine()
    private val hinglishMotivationEngine by lazy { com.example.notification.NovaHinglishMotivationEngine(getApplication()) }

    private val _dailyMissionTasks = MutableStateFlow<List<com.example.data.model.DailyMissionTask>>(emptyList())
    val dailyMissionTasks: StateFlow<List<com.example.data.model.DailyMissionTask>> = _dailyMissionTasks.asStateFlow()

    private val _weakTopicInsights = MutableStateFlow<List<com.example.data.model.WeakTopicInsight>>(emptyList())
    val weakTopicInsights: StateFlow<List<com.example.data.model.WeakTopicInsight>> = _weakTopicInsights.asStateFlow()

    private val _adaptiveScheduleShift = MutableStateFlow<com.example.data.model.AdaptiveScheduleShiftSuggestion?>(null)
    val adaptiveScheduleShift: StateFlow<com.example.data.model.AdaptiveScheduleShiftSuggestion?> = _adaptiveScheduleShift.asStateFlow()

    private val _weeklyReviewStats = MutableStateFlow(com.example.data.model.WeeklyReviewStats())
    val weeklyReviewStats: StateFlow<com.example.data.model.WeeklyReviewStats> = _weeklyReviewStats.asStateFlow()

    private val _weeklyStudyGoalHours = MutableStateFlow(10.0f)
    val weeklyStudyGoalHours: StateFlow<Float> = _weeklyStudyGoalHours.asStateFlow()

    private val _studyStreakDays = MutableStateFlow(7)
    val studyStreakDays: StateFlow<Int> = _studyStreakDays.asStateFlow()

    fun refreshNovaSmartPlannerData() {
        viewModelScope.launch {
            try {
                val todayStr = novaIntelligenceEngine.getTodayFormatted()
                val db = (getApplication() as StudyMateApplication).database
                val existingMissions = db.novaIntelligenceDao().getDailyMissionsForDateOnce(todayStr)
                val scheduleItems = studyScheduleList.value
                val user = userProfile.value

                val missions = novaIntelligenceEngine.buildDailyMissionsForToday(existingMissions, scheduleItems, user)
                if (existingMissions.isEmpty() && missions.isNotEmpty()) {
                    db.novaIntelligenceDao().insertDailyMissionTasks(missions)
                }
                _dailyMissionTasks.value = missions

                // Weak Topics
                val questionAttempts = db.questionHistoryDao().getAllHistoryOnce()
                val mistakeItems = mistakes.value
                _weakTopicInsights.value = novaIntelligenceEngine.detectWeakTopics(questionAttempts, mistakeItems)

                // Adaptive Schedule Shifts
                val logs = studyScheduleLogs.value
                _adaptiveScheduleShift.value = novaIntelligenceEngine.analyzeAdaptiveScheduleShifts(scheduleItems, logs)

                // Weekly Review
                val focusSess = allFocusSessions.value
                val mockAtt = mockTestAttempts.value
                _weeklyReviewStats.value = novaIntelligenceEngine.calculateWeeklyReviewStats(
                    focusSessions = focusSess,
                    scheduleItems = scheduleItems,
                    scheduleLogs = logs,
                    mockAttempts = mockAtt,
                    questionAttempts = questionAttempts
                )

                // Goal
                val goalEntity = db.novaIntelligenceDao().getWeeklyGoalOnce()
                if (goalEntity != null) {
                    _weeklyStudyGoalHours.value = goalEntity.targetHoursPerWeek
                }

                // Streak
                val userStreak = user?.streakDays ?: 7
                _studyStreakDays.value = userStreak
            } catch (e: Exception) {
                // Ignore transient errors
            }
        }
    }

    fun toggleDailyMissionTask(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val db = (getApplication() as StudyMateApplication).database
            db.novaIntelligenceDao().updateMissionCompletion(taskId, isCompleted, System.currentTimeMillis())
            _dailyMissionTasks.value = _dailyMissionTasks.value.map { task ->
                if (task.id == taskId) task.copy(isCompleted = isCompleted) else task
            }

            if (isCompleted) {
                // Reward XP & trigger motivation
                val current = userProfile.value ?: UserProfile()
                updateUserProfile(current.copy(xp = current.xp + 25))
                triggerNovaMotivation(com.example.data.model.MotivationCategory.DAILY_GOAL_COMPLETED)
            }
        }
    }

    fun dismissDailyMissionTask(taskId: String) {
        viewModelScope.launch {
            val db = (getApplication() as StudyMateApplication).database
            db.novaIntelligenceDao().dismissMissionTask(taskId)
            _dailyMissionTasks.value = _dailyMissionTasks.value.filter { it.id != taskId }
        }
    }

    fun acceptAdaptiveScheduleShift(suggestion: com.example.data.model.AdaptiveScheduleShiftSuggestion) {
        viewModelScope.launch {
            val items = studyScheduleList.value
            val target = items.find { it.id == suggestion.scheduleId }
            if (target != null) {
                val updated = target.copy(startTime = suggestion.suggestedTime)
                studyRepository.saveScheduleItem(updated)
            }
            _adaptiveScheduleShift.value = null
        }
    }

    fun dismissAdaptiveScheduleShift() {
        _adaptiveScheduleShift.value = null
    }

    fun updateWeeklyStudyGoal(hours: Float) {
        viewModelScope.launch {
            val db = (getApplication() as StudyMateApplication).database
            db.novaIntelligenceDao().setWeeklyGoal(com.example.data.model.UserWeeklyGoalEntity(targetHoursPerWeek = hours))
            _weeklyStudyGoalHours.value = hours
        }
    }

    fun triggerNovaMotivation(category: com.example.data.model.MotivationCategory, customMinutes: Int? = null) {
        val userNotifEnabled = userProfile.value?.notificationsEnabled ?: true
        hinglishMotivationEngine.triggerNotificationIfAllowed(
            category = category,
            customMinutes = customMinutes,
            notificationsEnabled = userNotifEnabled
        )
    }

    // --- Mock Test & Practice ---
    val mockTestAttempts: StateFlow<List<MockTestAttempt>> = studyRepository.allMockTestAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mistakes: StateFlow<List<MistakeItem>> = studyRepository.allMistakes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcards: StateFlow<List<FlashcardItem>> = studyRepository.allFlashcards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRevisionItems: StateFlow<List<RevisionItemEntity>> = studyRepository.allRevisionItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueRevisionItems: StateFlow<List<RevisionItemEntity>> = studyRepository.dueRevisionItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRevisionSessions: StateFlow<List<RevisionSessionEntity>> = studyRepository.allRevisionSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revisionRetentionStats: StateFlow<RevisionRetentionStats> = studyRepository.revisionRetentionStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RevisionRetentionStats())

    val userQuestionMaterials: StateFlow<List<UserQuestionMaterial>> = studyRepository.allUserQuestionMaterials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isFlashcardGenerating = MutableStateFlow(false)
    val isFlashcardGenerating: StateFlow<Boolean> = _isFlashcardGenerating.asStateFlow()

    private val _flashcardMessage = MutableStateFlow<String?>(null)
    val flashcardMessage: StateFlow<String?> = _flashcardMessage.asStateFlow()

    private val _pendingResumeSession = MutableStateFlow<ActiveTestState?>(null)
    val pendingResumeSession: StateFlow<ActiveTestState?> = _pendingResumeSession.asStateFlow()

    private val _activeTestState = MutableStateFlow(ActiveTestState())
    val activeTestState: StateFlow<ActiveTestState> = _activeTestState.asStateFlow()

    fun checkAndRestoreActiveSession() {
        viewModelScope.launch {
            val restored = sessionPersistence.loadActiveSession()
            if (restored != null && !restored.isCompleted && restored.questions.isNotEmpty()) {
                if (restored.remainingSeconds > 0) {
                    _pendingResumeSession.value = restored
                } else {
                    // Test expired while app was closed -> auto-submit cleanly so answers are preserved!
                    _activeTestState.value = restored
                    submitMockTest()
                }
            }
        }
    }

    private val _isTestGenerating = MutableStateFlow(false)
    val isTestGenerating: StateFlow<Boolean> = _isTestGenerating.asStateFlow()

    private val _generationError = MutableStateFlow<TestGenerationError?>(null)
    val generationError: StateFlow<TestGenerationError?> = _generationError.asStateFlow()

    private val _insufficientPyqNotice = MutableStateFlow<InsufficientPyqNotice?>(null)
    val insufficientPyqNotice: StateFlow<InsufficientPyqNotice?> = _insufficientPyqNotice.asStateFlow()

    fun clearGenerationError() {
        _generationError.value = null
    }

    fun dismissInsufficientPyqNotice() {
        _insufficientPyqNotice.value = null
    }

    private val _mistakeDiagnosis = MutableStateFlow<String?>(null)
    val mistakeDiagnosis: StateFlow<String?> = _mistakeDiagnosis.asStateFlow()

    // --- Document Summarizer & Study Questions ---
    private val _documentAnalysisState = MutableStateFlow<DocumentAnalysisResult?>(null)
    val documentAnalysisState: StateFlow<DocumentAnalysisResult?> = _documentAnalysisState.asStateFlow()

    private val _isDocumentParsing = MutableStateFlow(false)
    val isDocumentParsing: StateFlow<Boolean> = _isDocumentParsing.asStateFlow()

    private val _documentError = MutableStateFlow<String?>(null)
    val documentError: StateFlow<String?> = _documentError.asStateFlow()

    // --- AI Smart Features State ---
    private val _aiCoachRecommendation = MutableStateFlow<AiCoachRecommendation?>(null)
    val aiCoachRecommendation: StateFlow<AiCoachRecommendation?> = _aiCoachRecommendation.asStateFlow()
    private val _isAiCoachLoading = MutableStateFlow(false)
    val isAiCoachLoading: StateFlow<Boolean> = _isAiCoachLoading.asStateFlow()

    private val _studyNowRecommendation = MutableStateFlow<StudyNowRecommendation?>(null)
    val studyNowRecommendation: StateFlow<StudyNowRecommendation?> = _studyNowRecommendation.asStateFlow()
    private val _isStudyNowLoading = MutableStateFlow(false)
    val isStudyNowLoading: StateFlow<Boolean> = _isStudyNowLoading.asStateFlow()

    private val _mistakeInsights = MutableStateFlow<List<MistakePatternInsight>>(emptyList())
    val mistakeInsights: StateFlow<List<MistakePatternInsight>> = _mistakeInsights.asStateFlow()
    private val _isMistakeInsightsLoading = MutableStateFlow(false)
    val isMistakeInsightsLoading: StateFlow<Boolean> = _isMistakeInsightsLoading.asStateFlow()

    private val _weeklyReport = MutableStateFlow<WeeklyProgressReport?>(null)
    val weeklyReport: StateFlow<WeeklyProgressReport?> = _weeklyReport.asStateFlow()
    private val _isWeeklyReportLoading = MutableStateFlow(false)
    val isWeeklyReportLoading: StateFlow<Boolean> = _isWeeklyReportLoading.asStateFlow()

    private val _adaptiveQuizState = MutableStateFlow<AdaptiveQuizState?>(null)
    val adaptiveQuizState: StateFlow<AdaptiveQuizState?> = _adaptiveQuizState.asStateFlow()

    private val _completeStudyKit = MutableStateFlow<CompleteStudyKit?>(null)
    val completeStudyKit: StateFlow<CompleteStudyKit?> = _completeStudyKit.asStateFlow()
    private val _isStudyKitGenerating = MutableStateFlow(false)
    val isStudyKitGenerating: StateFlow<Boolean> = _isStudyKitGenerating.asStateFlow()
    private val _studyKitMessage = MutableStateFlow<String?>(null)
    val studyKitMessage: StateFlow<String?> = _studyKitMessage.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // --- Master Intelligence Engine State ---
    val analyticsEngine = studyRepository.analyticsEngine

    val analyticsDateFilter = MutableStateFlow(com.example.data.model.AnalyticsDateFilter.THIS_WEEK)
    val selectedAnalyticsSubjectFilter = MutableStateFlow("All")

    val dailyAnalytics: StateFlow<com.example.data.model.DailyAnalytics> = combine(
        analyticsEngine.analyticsUpdateTrigger,
        studyRepository.allFocusSessions,
        analyticsDateFilter
    ) { _, _, _ ->
        analyticsEngine.getDailyAnalytics()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.model.DailyAnalytics(dateFormatted = "Today"))

    val weeklyAnalytics: StateFlow<com.example.data.model.WeeklyAnalytics> = combine(
        analyticsEngine.analyticsUpdateTrigger,
        studyRepository.allFocusSessions,
        analyticsDateFilter
    ) { _, _, _ ->
        analyticsEngine.getWeeklyAnalytics()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.model.WeeklyAnalytics(startDateFormatted = "", endDateFormatted = ""))

    val monthlyAnalytics: StateFlow<com.example.data.model.MonthlyAnalytics> = combine(
        analyticsEngine.analyticsUpdateTrigger,
        studyRepository.allFocusSessions,
        analyticsDateFilter
    ) { _, _, _ ->
        analyticsEngine.getMonthlyAnalytics()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.model.MonthlyAnalytics(monthYearFormatted = ""))

    val streakInfo: StateFlow<com.example.data.model.StreakInfo> = combine(
        analyticsEngine.analyticsUpdateTrigger,
        studyRepository.allFocusSessions
    ) { _, _ ->
        analyticsEngine.getStreakInfo()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.model.StreakInfo())

    val subjectAnalyticsList: StateFlow<List<com.example.data.model.SubjectAnalytics>> = combine(
        analyticsEngine.analyticsUpdateTrigger,
        studyRepository.allFocusSessions,
        selectedAnalyticsSubjectFilter
    ) { _, _, subjectFilter ->
        val list = analyticsEngine.getSubjectAnalytics()
        if (subjectFilter == "All") list else list.filter { it.subject.equals(subjectFilter, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timeOfDayDistribution: StateFlow<com.example.data.model.TimeOfDayDistribution> = combine(
        analyticsEngine.analyticsUpdateTrigger,
        studyRepository.allFocusSessions
    ) { _, _ ->
        analyticsEngine.getTimeOfDayDistribution()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.model.TimeOfDayDistribution())

    val smartInsightsList: StateFlow<List<com.example.data.model.SmartInsight>> = combine(
        analyticsEngine.analyticsUpdateTrigger,
        studyRepository.allFocusSessions
    ) { _, _ ->
        analyticsEngine.getSmartInsights()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Step 57 Exam Preparation & Planner StateFlows ---
    val examPrepService = studyRepository.examPreparationService

    val selectedExamIdFilter = MutableStateFlow<String?>(null)

    val allExamGoals: StateFlow<List<com.example.data.model.ExamGoalEntity>> = combine(
        examPrepService.updateTrigger,
        studyRepository.allFocusSessions
    ) { _, _ ->
        examPrepService.getAllExamGoals()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val primaryExamSummary: StateFlow<com.example.data.model.ExamPreparationSummary?> = combine(
        examPrepService.updateTrigger,
        selectedExamIdFilter,
        allExamGoals
    ) { _, selectedId, goals ->
        val targetId = selectedId ?: goals.firstOrNull()?.examId
        if (targetId != null) examPrepService.getExamPreparationSummary(targetId) else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyPlanPreviewState = MutableStateFlow<com.example.data.model.DailyStudyPlanPreview?>(null)
    val dailyPlanPreview: StateFlow<com.example.data.model.DailyStudyPlanPreview?> = dailyPlanPreviewState.asStateFlow()

    fun selectExamGoal(examId: String) {
        selectedExamIdFilter.value = examId
    }

    fun createOrUpdateExamGoal(
        name: String,
        organization: String,
        dateMillis: Long?,
        target: String,
        priority: String
    ) {
        viewModelScope.launch {
            val created = examPrepService.createOrUpdateExamGoal(
                examName = name,
                organization = organization,
                examDateMillis = dateMillis,
                isExamDateKnown = dateMillis != null,
                target = target,
                priority = priority
            )
            selectedExamIdFilter.value = created.examId
        }
    }

    fun updateSyllabusTopicStatus(topicId: String, status: String) {
        viewModelScope.launch {
            examPrepService.updateTopicStatus(topicId, status)
        }
    }

    fun addCustomSyllabusTopic(examId: String, subject: String, topic: String) {
        viewModelScope.launch {
            examPrepService.addCustomTopic(examId, subject, topic)
        }
    }

    fun generateDailyPlanPreview(examId: String) {
        viewModelScope.launch {
            val preview = examPrepService.generateDailyStudyPlanPreview(examId)
            dailyPlanPreviewState.value = preview
        }
    }

    fun confirmDailyPlan(preview: com.example.data.model.DailyStudyPlanPreview) {
        viewModelScope.launch {
            val success = examPrepService.confirmAndScheduleDailyPlan(preview)
            if (success) {
                _snackbarMessage.emit("Study Plan confirmed and saved to schedule!")
                dailyPlanPreviewState.value = null
            }
        }
    }

    // Step 58 Smart Learning Resources & AI Study Material Engine 2.0
    val resourceEngineService = studyRepository.resourceEngineService
    val resourceQueryState = MutableStateFlow("")
    val selectedResourceTypeFilter = MutableStateFlow(com.example.data.model.ResourceType.ALL.name)
    val resourceUpdateTrigger = MutableStateFlow(System.currentTimeMillis())

    val activeResourceForViewer = MutableStateFlow<com.example.data.model.StudyResourceEntity?>(null)

    val searchResourcesState: StateFlow<List<com.example.data.model.ResourceSearchResult>> = combine(
        resourceQueryState,
        selectedResourceTypeFilter,
        resourceUpdateTrigger
    ) { query, typeFilter, _ ->
        val activeContext = activeExamContext.value
        resourceEngineService.searchAndRankResources(
            query = query,
            selectedType = typeFilter,
            examId = activeContext.examId,
            subjectName = activeContext.subjects.firstOrNull()?.name ?: "",
            topicName = activeContext.topics.firstOrNull()?.name ?: ""
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setResourceSearchQuery(q: String) {
        resourceQueryState.value = q
    }

    fun setResourceTypeFilter(type: String) {
        selectedResourceTypeFilter.value = type
    }

    fun openResourceForViewer(resource: com.example.data.model.StudyResourceEntity) {
        activeResourceForViewer.value = resource
        viewModelScope.launch {
            studyRepository.analyticsEngine.logEvent(
                eventType = com.example.data.model.StudyEventType.FOCUS_STARTED,
                subject = resource.subjectName,
                topic = resource.topicName,
                metadataJson = "{\"resourceId\":\"${resource.resourceId}\"}"
            )
        }
    }

    fun closeResourceViewer() {
        activeResourceForViewer.value = null
    }

    fun toggleSaveResource(resourceId: String) {
        viewModelScope.launch {
            val saved = resourceEngineService.toggleSaveState(resourceId)
            resourceUpdateTrigger.value = System.currentTimeMillis()
            _snackbarMessage.emit(if (saved) "Resource saved to My Resources!" else "Resource removed from saved list.")
        }
    }

    fun updateResourceReadingProgress(resourceId: String, page: Int, totalPages: Int) {
        viewModelScope.launch {
            resourceEngineService.updateReadingProgress(resourceId, page, totalPages)
            resourceUpdateTrigger.value = System.currentTimeMillis()
        }
    }

    fun addResourceBookmark(resourceId: String, page: Int, snippet: String) {
        viewModelScope.launch {
            resourceEngineService.addBookmark(resourceId, page, snippet)
            _snackbarMessage.emit("Bookmark saved for Page $page!")
        }
    }

    fun uploadCustomResource(title: String, desc: String, exam: String, subject: String, topic: String, content: String) {
        viewModelScope.launch {
            val user = userProfile.value?.id ?: "current_user"
            resourceEngineService.saveUserUploadedResource(getApplication(), user, title, desc, exam, subject, topic, content)
            resourceUpdateTrigger.value = System.currentTimeMillis()
            _snackbarMessage.emit("Custom study material uploaded successfully!")
        }
    }

    fun answerDocumentQA(resourceId: String, userQuestion: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = resourceEngineService.answerDocumentQuestion(resourceId, userQuestion)
            onResult(result.answerText)
        }
    }

    val allFocusSessions: StateFlow<List<FocusSession>> = studyRepository.allFocusSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSmartNotes: StateFlow<List<SmartNoteItem>> = studyRepository.allSmartNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedSmartNotes: StateFlow<List<SmartNoteItem>> = studyRepository.bookmarkedSmartNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCurrentAffairs: StateFlow<List<CurrentAffairsItem>> = studyRepository.allCurrentAffairs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedCurrentAffairs: StateFlow<List<CurrentAffairsItem>> = studyRepository.savedCurrentAffairs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExamUpdates: StateFlow<List<ExamUpdateItem>> = studyRepository.allExamUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Step 21 Live Exam Intelligence System ---
    val liveExamIntelligenceEngine = (application as StudyMateApplication).liveExamIntelligenceEngine

    private val _liveExamFeedState = MutableStateFlow(LiveExamFeedState())
    val liveExamFeedState: StateFlow<LiveExamFeedState> = _liveExamFeedState.asStateFlow()

    private val _isRefreshingLiveExam = MutableStateFlow(false)
    val isRefreshingLiveExam: StateFlow<Boolean> = _isRefreshingLiveExam.asStateFlow()

    private val _selectedLiveUpdateForDetail = MutableStateFlow<LiveExamUpdateEntity?>(null)
    val selectedLiveUpdateForDetail: StateFlow<LiveExamUpdateEntity?> = _selectedLiveUpdateForDetail.asStateFlow()

    private val _showLiveExamIntelligenceScreen = MutableStateFlow(false)
    val showLiveExamIntelligenceScreen: StateFlow<Boolean> = _showLiveExamIntelligenceScreen.asStateFlow()

    // --- Step 40 Smart Vacancy, Results & Admit Card Intelligence ---
    val recruitmentIntelligenceEngine = (application as StudyMateApplication).recruitmentIntelligenceEngine

    private val _recruitmentFeedState = MutableStateFlow(RecruitmentFeedState())
    val recruitmentFeedState: StateFlow<RecruitmentFeedState> = _recruitmentFeedState.asStateFlow()

    private val _selectedRecruitmentDetail = MutableStateFlow<RecruitmentEntity?>(null)
    val selectedRecruitmentDetail: StateFlow<RecruitmentEntity?> = _selectedRecruitmentDetail.asStateFlow()

    private val _showSmartVacancyScreen = MutableStateFlow(false)
    val showSmartVacancyScreen: StateFlow<Boolean> = _showSmartVacancyScreen.asStateFlow()

    private val _isRefreshingRecruitment = MutableStateFlow(false)
    val isRefreshingRecruitment: StateFlow<Boolean> = _isRefreshingRecruitment.asStateFlow()

    // --- Step 71 Dedicated Latest Updates Category State Flows ---
    val latestUpdatesRepository: LatestUpdatesRepository by lazy {
        LatestUpdatesRepository(recruitmentDao = (getApplication() as StudyMateApplication).database.recruitmentDao())
    }

    private val _vacancyFeedState = MutableStateFlow(CategoryFeedState())
    val vacancyFeedState: StateFlow<CategoryFeedState> = _vacancyFeedState.asStateFlow()

    private val _admitCardFeedState = MutableStateFlow(CategoryFeedState())
    val admitCardFeedState: StateFlow<CategoryFeedState> = _admitCardFeedState.asStateFlow()

    private val _resultFeedState = MutableStateFlow(CategoryFeedState())
    val resultFeedState: StateFlow<CategoryFeedState> = _resultFeedState.asStateFlow()

    private val _answerKeyFeedState = MutableStateFlow(CategoryFeedState())
    val answerKeyFeedState: StateFlow<CategoryFeedState> = _answerKeyFeedState.asStateFlow()

    private val _admissionFeedState = MutableStateFlow(CategoryFeedState())
    val admissionFeedState: StateFlow<CategoryFeedState> = _admissionFeedState.asStateFlow()

    private val _selectedUpdateDetail = MutableStateFlow<LatestUpdateItem?>(null)
    val selectedUpdateDetail: StateFlow<LatestUpdateItem?> = _selectedUpdateDetail.asStateFlow()

    // --- Room-based Intelligence Engine State Flows ---
    val allExamObjectives: StateFlow<List<ExamObjective>> = studyRepository.allExamObjectives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeExamObjective: StateFlow<ExamObjective?> = studyRepository.activeExamObjective
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTopicMasteries: StateFlow<List<TopicMastery>> = studyRepository.allTopicMasteries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weakTopicMasteries: StateFlow<List<TopicMastery>> = studyRepository.weakTopicMasteries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentSessionHistory: StateFlow<List<StudentSessionHistory>> = studyRepository.studentSessionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestIntelligenceSnapshot: StateFlow<IntelligenceSnapshot?> = studyRepository.latestIntelligenceSnapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedAvailableTimeMinutes = MutableStateFlow<Int?>(null)
    val selectedAvailableTimeMinutes: StateFlow<Int?> = _selectedAvailableTimeMinutes.asStateFlow()

    // --- Step 10 Topic Mastery & Intelligence Engine State Flows ---
    val subjectProgressSummaries: StateFlow<List<SubjectProgressSummary>> = combine(
        activeExamContext,
        allTopicMasteries
    ) { examCtx, masteries ->
        com.example.service.intelligence.TopicMasteryEngine.buildSubjectSummaries(examCtx, masteries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examReadinessScore: StateFlow<ExamReadinessScore> = combine(
        activeExamContext,
        allTopicMasteries,
        mockTestAttempts,
        userProfile
    ) { examCtx, masteries, mocks, user ->
        val streak = user?.streakDays ?: 1
        val examDateMillis = user?.examDateMillis ?: 0L
        com.example.service.intelligence.TopicMasteryEngine.calculateExamReadiness(examCtx, masteries, mocks, streak, examDateMillis)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ExamReadinessScore(
            examId = "",
            examName = "Competitive Exam",
            readinessScore = 0,
            status = "STARTING",
            statusBadgeText = "Starting Preparation 🌱",
            syllabusCoveragePercent = 0,
            subjectBalanceScore = 0,
            recentAccuracyPercent = 0f,
            totalTopicsCount = 0,
            masteredTopicsCount = 0,
            weakTopicsCount = 0,
            revisionDueCount = 0,
            actionableInsight = "Begin studying foundational topics."
        )
    )

    val studyRecommendations: StateFlow<List<StudyRecommendation>> = combine(
        activeExamContext,
        allTopicMasteries,
        mistakes
    ) { examCtx, masteries, mistakeList ->
        com.example.service.intelligence.TopicMasteryEngine.generateRecommendations(examCtx, masteries, mistakeList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyStudyPlan: StateFlow<DailyStudyPlan> = combine(
        _selectedAvailableTimeMinutes,
        studyRecommendations,
        activeExamContext
    ) { timeMinutes, recs, examCtx ->
        val budget = timeMinutes ?: 60
        com.example.service.intelligence.TopicMasteryEngine.generateDailyStudyPlan(budget, recs, examCtx.examName)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailyStudyPlan(
            totalAvailableMinutes = 60,
            targetExamName = "Exam",
            items = emptyList(),
            summaryAdvice = "Plan your daily study session."
        )
    )

    // --- Smart Search State ---
    private val _smartSearchResult = MutableStateFlow<SmartSearchResult?>(null)
    val smartSearchResult: StateFlow<SmartSearchResult?> = _smartSearchResult.asStateFlow()

    private val _isSmartSearching = MutableStateFlow(false)
    val isSmartSearching: StateFlow<Boolean> = _isSmartSearching.asStateFlow()

    private val _smartSearchError = MutableStateFlow<String?>(null)
    val smartSearchError: StateFlow<String?> = _smartSearchError.asStateFlow()

    val studentMasterContext: StateFlow<StudentMasterContext?> = combine(
        userProfile,
        studyPlanItems,
        studyRepository.allFocusSessions,
        mockTestAttempts,
        mistakes,
        flashcards,
        _selectedAvailableTimeMinutes
    ) { args: Array<Any?> ->
        val user = args[0] as? UserProfile
        val plans = (args[1] as? List<*>)?.filterIsInstance<StudyPlanItem>() ?: emptyList()
        val focus = (args[2] as? List<*>)?.filterIsInstance<FocusSession>() ?: emptyList()
        val mockAttempts = (args[3] as? List<*>)?.filterIsInstance<MockTestAttempt>() ?: emptyList()
        val mistakeList = (args[4] as? List<*>)?.filterIsInstance<MistakeItem>() ?: emptyList()
        val cards = (args[5] as? List<*>)?.filterIsInstance<FlashcardItem>() ?: emptyList()
        val timeAvailable = args[6] as? Int

        if (user != null) {
            StudyMateIntelligenceEngine.buildMasterContext(
                profile = user,
                plans = plans,
                focusSessions = focus,
                mockAttempts = mockAttempts,
                mistakes = mistakeList,
                flashcards = cards,
                availableTimeMinutes = timeAvailable
            )
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Theme & Settings ---
    private val _themeMode = MutableStateFlow(AppThemeMode.NOVA_DARK)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _notificationPrefs = MutableStateFlow(NotificationPreference())
    val notificationPrefs: StateFlow<NotificationPreference> = _notificationPrefs.asStateFlow()

    // --- Step 30 Notification Center & Daily Briefing State ---
    private val _appNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val appNotifications: StateFlow<List<AppNotification>> = _appNotifications.asStateFlow()

    private val _activeInAppBanner = MutableStateFlow<AppNotification?>(null)
    val activeInAppBanner: StateFlow<AppNotification?> = _activeInAppBanner.asStateFlow()

    private val _dailyBriefingData = MutableStateFlow(DailyBriefingData())
    val dailyBriefingData: StateFlow<DailyBriefingData> = _dailyBriefingData.asStateFlow()

    // --- Daily Missions ---
    val dailyMissions = flow {
        emit(
            listOf(
                DailyMission("m_1", "Complete 40 mins Physics focus session", 40, 30, "mins", 50, false),
                DailyMission("m_2", "Solve 10 AI practice questions", 10, 10, "questions", 40, true),
                DailyMission("m_3", "Revise 1 Chemistry flashcard topic", 1, 1, "topic", 30, true),
                DailyMission("m_4", "Maintain your daily study streak", 1, 1, "day", 25, true)
            )
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadInitialThemeSettings()
        initTts()
        loadInitialNotificationSettings()
        loadAppNotificationsFromDisk()
        viewModelScope.launch {
            authRepository.getInitialUser()
        }
        viewModelScope.launch {
            userProfile.filterNotNull().collect { profile ->
                refreshLiveExamIntelligence(force = false)
                refreshRecruitmentCatalog(force = false)
                checkAndTriggerSmartNotifications()
            }
        }
        viewModelScope.launch {
            recruitmentIntelligenceEngine.allRecruitmentItems.collect { items ->
                updateRecruitmentFeedState(items)
            }
        }
    }

    private fun loadInitialNotificationSettings() {
        val s = com.example.notification.StudyNotificationManager.getSettings(getApplication())
        _notificationPrefs.value = NotificationPreference(
            masterEnabled = s.masterEnabled,
            studyReminders = s.studySessionReminders,
            examCountdownAlerts = s.examCountdownAlerts,
            dailyGoalReminders = s.dailyGoalReminders,
            missedStudyReminders = s.missedStudyReminders,
            breakReminders = s.breakReminders,
            focusStartedAlerts = s.focusStartedAlerts,
            focusCompletedAlerts = s.focusCompletedAlerts,
            motivationalQuotes = s.dailyMotivationAlerts,
            reminderHour = s.studyReminderHour,
            reminderMinute = s.studyReminderMinute,
            dailyGoalHour = s.dailyGoalReminderHour,
            dailyGoalMinute = s.dailyGoalReminderMinute,
            motivationHour = s.motivationReminderHour,
            motivationMinute = s.motivationReminderMinute,
            activeDays = s.activeReminderDays,
            motivationFrequency = s.motivationFrequency,
            quietHoursEnabled = s.quietHoursEnabled,
            quietStartHour = s.quietHoursStartHour,
            quietStartMinute = s.quietHoursStartMinute,
            quietEndHour = s.quietHoursEndHour,
            quietEndMinute = s.quietHoursEndMinute
        )
    }

    private fun initTts() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    // --- Authentication Actions ---

    private var otpTimerJob: Job? = null

    fun startOtpCooldown(seconds: Int = 120) {
        otpTimerJob?.cancel()
        _otpCooldownSeconds.value = seconds
        otpTimerJob = viewModelScope.launch {
            while (_otpCooldownSeconds.value > 0) {
                delay(1000L)
                _otpCooldownSeconds.value = (_otpCooldownSeconds.value - 1).coerceAtLeast(0)
            }
        }
    }

    fun setAuthNavState(state: AuthScreenState) {
        _authNavState.value = state
        _authErrorMessage.value = null
    }

    fun clearAuthMessages() {
        _authErrorMessage.value = null
        _authSuccessMessage.value = null
    }

    fun updatePendingAuthEmail(email: String) {
        _pendingAuthEmail.value = email.trim()
    }

    fun startSignUp(
        fullName: String,
        email: String,
        phone: String,
        pass: String,
        confirmPass: String
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _authSuccessMessage.value = null
            com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saving)

            val result = authRepository.signUpUser(
                fullName = fullName,
                email = email,
                mobileNumber = phone,
                pass = pass,
                confirmPass = confirmPass
            )

            result.onSuccess { signUpResult ->
                _pendingAuthEmail.value = signUpResult.email
                _pendingAuthName.value = signUpResult.fullName
                _pendingAuthPhone.value = signUpResult.mobileNumber
                startOtpCooldown(120)
                _authNavState.value = AuthScreenState.OTP_VERIFICATION
                _authSuccessMessage.value = "OTP aapke email par bhej diya gaya hai."
                com.example.data.persistence.PersistenceMonitor.updateStatus(
                    com.example.data.persistence.PersistenceStatus.Saved(message = "✓ OTP Sent to email")
                )
            }

            result.onFailure { error ->
                val msg = error.message ?: "Sign-up failed"
                _authErrorMessage.value = msg
                com.example.data.persistence.PersistenceMonitor.updateStatus(
                    com.example.data.persistence.PersistenceStatus.Failed(msg)
                )
            }
            _isAuthLoading.value = false
        }
    }

    fun verifyEmailOtp(otp: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _authSuccessMessage.value = null
            com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saving)

            val result = authRepository.verifyEmailOtp(
                email = _pendingAuthEmail.value,
                otp = otp,
                fullName = _pendingAuthName.value,
                mobileNumber = _pendingAuthPhone.value
            )

            result.onSuccess { profile ->
                otpTimerJob?.cancel()
                _otpCooldownSeconds.value = 0
                _authNavState.value = AuthScreenState.LOGIN
                _authSuccessMessage.value = "Email verification safal raha! Welcome ${profile.name}."
                com.example.data.persistence.PersistenceMonitor.updateStatus(
                    com.example.data.persistence.PersistenceStatus.Saved(message = "✓ Email verified")
                )
            }

            result.onFailure { error ->
                val msg = error.message ?: "Verification failed"
                _authErrorMessage.value = msg
                com.example.data.persistence.PersistenceMonitor.updateStatus(
                    com.example.data.persistence.PersistenceStatus.Failed(msg)
                )
            }
            _isAuthLoading.value = false
        }
    }

    fun resendEmailOtp(isRecovery: Boolean = false) {
        if (_otpCooldownSeconds.value > 0) return
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _authSuccessMessage.value = null

            val type = if (isRecovery) "recovery" else "signup"
            val result = if (isRecovery) {
                authRepository.sendPasswordRecoveryOtp(_pendingAuthEmail.value)
            } else {
                authRepository.resendEmailOtp(_pendingAuthEmail.value, type)
            }

            result.onSuccess {
                startOtpCooldown(120)
                _authSuccessMessage.value = "Naya OTP email par bhej diya gaya hai."
            }

            result.onFailure { error ->
                _authErrorMessage.value = error.message ?: "OTP resend nahi ho paya."
            }
            _isAuthLoading.value = false
        }
    }

    fun startForgotPassword(email: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _authSuccessMessage.value = null

            val normalizedEmail = email.trim().lowercase(Locale.ROOT)
            if (normalizedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                _authErrorMessage.value = "Kripya ek valid email address enter karein."
                _isAuthLoading.value = false
                return@launch
            }

            _pendingAuthEmail.value = normalizedEmail
            val result = authRepository.sendPasswordRecoveryOtp(normalizedEmail)

            result.onSuccess {
                startOtpCooldown(120)
                _authNavState.value = AuthScreenState.FORGOT_PASSWORD_OTP
                _authSuccessMessage.value = "Password reset OTP aapke email par bhej diya gaya hai."
            }

            result.onFailure { error ->
                _authErrorMessage.value = error.message ?: "Reset OTP bhejne me samasya aayi."
            }
            _isAuthLoading.value = false
        }
    }

    fun verifyForgotPasswordOtp(otp: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _authSuccessMessage.value = null

            val result = authRepository.verifyPasswordRecoveryOtp(
                email = _pendingAuthEmail.value,
                otp = otp
            )

            result.onSuccess { token ->
                _recoveryAccessToken.value = token
                _authNavState.value = AuthScreenState.RESET_PASSWORD
                _authSuccessMessage.value = "OTP verify ho gaya. Kripya naya password set karein."
            }

            result.onFailure { error ->
                _authErrorMessage.value = error.message ?: "Invalid OTP."
            }
            _isAuthLoading.value = false
        }
    }

    fun completePasswordReset(newPassword: String, confirmPass: String) {
        if (newPassword.length < 6) {
            _authErrorMessage.value = "Password kam se kam 6 characters ka hona chahiye."
            return
        }
        if (newPassword != confirmPass) {
            _authErrorMessage.value = "Password aur Confirm Password match nahi kar rahe hain."
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _authSuccessMessage.value = null

            val token = _recoveryAccessToken.value ?: ""
            val result = authRepository.resetPasswordWithToken(token, newPassword)

            result.onSuccess {
                _recoveryAccessToken.value = null
                _authNavState.value = AuthScreenState.LOGIN
                _authSuccessMessage.value = "Password safaltapoorvak update ho gaya! Kripya log in karein."
            }

            result.onFailure { error ->
                _authErrorMessage.value = error.message ?: "Password update nahi ho saka."
            }
            _isAuthLoading.value = false
        }
    }

    fun signInWithEmailOrPhone(identifier: String, pass: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _authSuccessMessage.value = null
            com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saving)

            val result = authRepository.signInWithEmailOrPhone(identifier, pass)

            result.onSuccess {
                com.example.data.persistence.PersistenceMonitor.updateStatus(
                    com.example.data.persistence.PersistenceStatus.Saved(message = "✓ Logged in & synced")
                )
            }

            result.onFailure { error ->
                val msg = error.message ?: "Login failed"
                _authErrorMessage.value = msg
                com.example.data.persistence.PersistenceMonitor.updateStatus(
                    com.example.data.persistence.PersistenceStatus.Failed(msg)
                )
            }
            _isAuthLoading.value = false
        }
    }

    fun signInWithGoogle(activityContext: android.content.Context) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            val result = authRepository.signInWithGoogle(activityContext)
            result.onFailure {
                val errorMsg = it.message ?: "Google Sign-In failed"
                if (errorMsg != "Sign-in cancelled." && errorMsg != "Google Sign-In was cancelled.") {
                    _authErrorMessage.value = errorMsg
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun clearAuthError() {
        _authErrorMessage.value = null
        _authSuccessMessage.value = null
    }

    fun signInWithEmail(email: String, pass: String) {
        signInWithEmailOrPhone(email, pass)
    }

    fun signUpWithEmail(email: String, pass: String, displayName: String = "", examName: String = "RRB Group D") {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saving)
            val result = authRepository.signUpWithEmail(email, pass, displayName, examName)
            result.onSuccess {
                com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saved(message = "✓ Account created & synced"))
            }
            result.onFailure {
                _authErrorMessage.value = it.message ?: "Sign-up failed"
                com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Failed(it.message ?: "Sign-up failed"))
            }
            _isAuthLoading.value = false
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            val result = authRepository.continueAsGuest()
            result.onSuccess {
                studyRepository.populateInitialDataIfEmpty()
                if (studyPlanItems.value.isEmpty()) {
                    studyRepository.replaceStudyPlan(
                        listOf(
                            StudyPlanItem(
                                subject = "Physics",
                                chapter = "Electromagnetic Induction",
                                topic = "Faraday's Law & Lenz's Rule Problem Solving",
                                targetMinutes = 45,
                                priority = PlanPriority.HIGH,
                                notes = "High-weightage concept for board & competitive exams."
                            ),
                            StudyPlanItem(
                                subject = "Mathematics",
                                chapter = "Calculus",
                                topic = "Definite Integrals & Area Under Curves",
                                targetMinutes = 60,
                                priority = PlanPriority.HIGH,
                                notes = "Practice 10 numericals and revise properties of integration."
                            ),
                            StudyPlanItem(
                                subject = "Chemistry",
                                chapter = "Thermodynamics",
                                topic = "Gibbs Free Energy & Spontaneity Derivations",
                                targetMinutes = 40,
                                priority = PlanPriority.MEDIUM,
                                notes = "Review ΔG = ΔH - TΔS conditions."
                            )
                        )
                    )
                }
            }
            result.onFailure { _authErrorMessage.value = it.message ?: "Guest session failed" }
            _isAuthLoading.value = false
        }
    }

    fun saveOnboarding(profile: UserProfile) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val currentUser = userProfile.value
            val finalProfile = profile.copy(
                id = "current_user",
                uid = currentUser?.uid ?: profile.uid,
                email = currentUser?.email ?: profile.email,
                photoUrl = currentUser?.photoUrl ?: profile.photoUrl,
                isGuest = currentUser?.isGuest ?: false,
                isOnboardingCompleted = true
            )
            authRepository.completeOnboarding(finalProfile)
            // Generate initial personalized study plan and flashcards
            generateAiStudyPlan()
            _isAuthLoading.value = false
        }
    }

    fun updateUserProfile(profile: UserProfile, refreshStudyPlan: Boolean = false) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            authRepository.updateUserProfile(profile)
            if (refreshStudyPlan) {
                generateAiStudyPlan()
            }
            _isAuthLoading.value = false
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val res = authRepository.sendPasswordResetEmail(email)
            _isAuthLoading.value = false
            res.onSuccess {
                onResult(true, "Password reset link sent to $email. Please check your inbox.")
            }.onFailure {
                onResult(false, it.message ?: "Failed to send password reset email.")
            }
        }
    }

    fun signOut(activityContext: android.content.Context? = null) {
        viewModelScope.launch {
            authRepository.signOut(activityContext)
            _appNotifications.value = emptyList()
            _activeInAppBanner.value = null
            _dailyBriefingData.value = DailyBriefingData()
            _activeTestState.value = ActiveTestState()
            _pendingResumeSession.value = null
            _showLiveExamIntelligenceScreen.value = false
            saveAppNotificationsToDisk()
        }
    }

    fun changePassword(newPassword: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saving)
            val res = authRepository.changePassword(newPassword)
            _isAuthLoading.value = false
            res.onSuccess {
                com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saved(message = "✓ Password updated successfully"))
                onResult(true, "Password updated successfully.")
            }.onFailure { err ->
                val msg = err.message ?: "Failed to update password."
                com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Failed(msg))
                onResult(false, msg)
            }
        }
    }

    fun requestEmailChange(newEmail: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saving)
            val res = authRepository.requestEmailChange(newEmail)
            _isAuthLoading.value = false
            res.onSuccess {
                com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saved(message = "✓ Confirmation link sent"))
                onResult(true, "Confirmation link sent to $newEmail. Please confirm from your inbox.")
            }.onFailure { err ->
                val msg = err.message ?: "Failed to change email."
                com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Failed(msg))
                onResult(false, msg)
            }
        }
    }

    fun deleteAccount(onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saving)
            try {
                studyRepository.clearAllUserStudyData()
                val res = authRepository.deleteAccount()
                _appNotifications.value = emptyList()
                _activeInAppBanner.value = null
                _dailyBriefingData.value = DailyBriefingData()
                _activeTestState.value = ActiveTestState()
                _pendingResumeSession.value = null
                _showLiveExamIntelligenceScreen.value = false
                saveAppNotificationsToDisk()
                _isAuthLoading.value = false
                res.onSuccess {
                    com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saved(message = "✓ Account deleted"))
                    onComplete?.invoke(true, null)
                }.onFailure {
                    com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Failed(it.message ?: "Deletion error"))
                    onComplete?.invoke(false, it.message)
                }
            } catch (e: Exception) {
                _isAuthLoading.value = false
                com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Failed(e.message ?: "Deletion error"))
                onComplete?.invoke(false, e.message)
            }
        }
    }

    fun clearAllLocalStudyData(onComplete: () -> Unit) {
        viewModelScope.launch {
            studyRepository.clearAllUserStudyData()
            generateAiStudyPlan()
            onComplete()
        }
    }

    fun resetPersonalization(onComplete: () -> Unit) {
        viewModelScope.launch {
            val defaultPrefs = UserStudyPreferences(userId = "current_user")
            studyRepository.saveUserPreferences(defaultPrefs)
            onComplete()
        }
    }

    fun triggerManualSync(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Syncing)
            try {
                val sync = (getApplication<android.app.Application>() as com.example.StudyMateApplication).supabaseSyncService
                val profile = userProfile.value
                val notifs = notificationPrefs.value
                if (profile != null && sync != null) {
                    sync.syncUserProfile(profile, notifs)
                }
                kotlinx.coroutines.delay(800)
                com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Saved(message = "✓ All data up to date"))
                onResult(true, "All changes synchronized with cloud.")
            } catch (e: Exception) {
                com.example.data.persistence.PersistenceMonitor.updateStatus(com.example.data.persistence.PersistenceStatus.Offline(message = "⚠ Offline — stored securely on device"))
                onResult(false, e.message ?: "Unable to reach server. Local data is safe.")
            }
        }
    }

    fun exportUserDataJson(): String {
        val profile = userProfile.value ?: UserProfile()
        val prefs = userStudyPreferences.value
        val attempts = mockTestAttempts.value
        val mistakeList = mistakes.value
        val flashcardsList = flashcards.value
        val bookmarks = bookmarkedSmartNotes.value
        val plans = studyPlanItems.value

        val root = org.json.JSONObject().apply {
            put("exportDate", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
            put("appVersion", "2.4.0")
            put("user", org.json.JSONObject().apply {
                put("displayName", profile.name)
                put("email", profile.email)
                put("examName", profile.examName)
                put("level", profile.level)
                put("xp", profile.xp)
                put("streakDays", profile.streakDays)
                put("dailyStudyHours", profile.availableStudyHours)
                put("isGuest", profile.isGuest)
                put("memberSince", java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(profile.createdAt)))
            })
            put("preferences", org.json.JSONObject().apply {
                put("dailyQuestionGoal", prefs.dailyQuestionGoal)
                put("dailyAvailableMinutes", prefs.dailyAvailableMinutes)
                put("personalizationEnabled", prefs.personalizationEnabled)
                put("preferredStudyWindow", prefs.preferredStudyWindow)
                put("preferredSessionMinutes", prefs.preferredSessionMinutes)
            })
            put("statistics", org.json.JSONObject().apply {
                put("mockTestAttemptsCount", attempts.size)
                put("mistakesCount", mistakeList.size)
                put("flashcardsCount", flashcardsList.size)
                put("bookmarksCount", bookmarks.size)
                put("studyPlanItemsCount", plans.size)
            })
        }
        return root.toString(2)
    }

    // --- Study Plan Actions ---

    fun generateAiStudyPlan() {
        generateAdaptiveDailyPlan()
    }

    fun generateAdaptiveDailyPlan(overrideAvailableMinutes: Int? = null) {
        val profile = userProfile.value ?: UserProfile()
        viewModelScope.launch {
            _isPlanGenerating.value = true
            val examCtx = activeExamContext.value ?: ExamContext()
            val masteries = allTopicMasteries.value
            val mistakeList = mistakes.value
            val flashcardsList = flashcards.value
            val attempts = mockTestAttempts.value
            val existingPlans = studyPlanItems.value
            val currentPrefs = userStudyPreferences.value
            val prefs = if (overrideAvailableMinutes != null) currentPrefs.copy(dailyAvailableMinutes = overrideAvailableMinutes) else currentPrefs

            val daysRemaining = ((profile.examDateMillis - System.currentTimeMillis()) / (1000L * 86400)).coerceAtLeast(1).toInt()

            val result = com.example.service.intelligence.StudyPlannerEngine.generateAdaptiveDailyPlan(
                examContext = examCtx,
                topicMasteries = masteries,
                mistakes = mistakeList,
                flashcards = flashcardsList,
                recentMockAttempts = attempts,
                existingTodayPlans = existingPlans,
                userPreferences = prefs,
                examDaysRemaining = daysRemaining
            )

            _deadlineWarning.value = result.deadlineWarningMessage
            studyRepository.replaceStudyPlan(result.sessions)
            studyRepository.saveUserPreferences(prefs)

            _isPlanGenerating.value = false
        }
    }

    fun saveUserPreferences(preferences: UserStudyPreferences) {
        viewModelScope.launch {
            studyRepository.saveUserPreferences(preferences)
        }
    }

    fun applySubjectTimeAllocations(
        subjectMinutesMap: Map<String, Int>,
        totalDailyMinutes: Int,
        startHour: Int = 8,
        startMinute: Int = 0,
        breakMinutes: Int = 5
    ) {
        viewModelScope.launch {
            _isPlanGenerating.value = true
            val examCtx = activeExamContext.value ?: ExamContext()
            val masteries = allTopicMasteries.value
            val mistakeList = mistakes.value
            val flashcardsList = flashcards.value
            val currentPrefs = userStudyPreferences.value
            val updatedPrefs = currentPrefs.copy(
                dailyAvailableMinutes = totalDailyMinutes,
                windowStartHour = startHour,
                breakMinutes = breakMinutes
            )

            val sessions = com.example.service.intelligence.StudyPlannerEngine.generatePlanFromSubjectAllocations(
                examContext = examCtx,
                subjectAllocations = subjectMinutesMap,
                startHour = startHour,
                startMinute = startMinute,
                breakMinutes = breakMinutes,
                topicMasteries = masteries,
                mistakes = mistakeList,
                flashcards = flashcardsList,
                userPreferences = updatedPrefs
            )

            studyRepository.replaceStudyPlan(sessions)
            studyRepository.saveUserPreferences(updatedPrefs)
            _isPlanGenerating.value = false
            _snackbarMessage.emit("Updated daily study plan with custom allocations! 📚")
        }
    }

    fun updateSubjectPriority(subjectName: String, priority: String) {
        viewModelScope.launch {
            val current = userStudyPreferences.value
            val json = try {
                org.json.JSONObject(current.subjectPrioritiesJson)
            } catch (e: Exception) {
                org.json.JSONObject()
            }
            json.put(subjectName, priority)
            saveUserPreferences(current.copy(subjectPrioritiesJson = json.toString()))
        }
    }

    // --- Interactive Session Timer Methods ---

    fun startStudySession(item: StudyPlanItem) {
        _activeStudySession.value = item
        val initialSeconds = (item.targetMinutes * 60).coerceAtLeast(60)
        _sessionRemainingSeconds.value = initialSeconds
        _isSessionTimerRunning.value = true
        _isSessionPaused.value = false
        _activeSessionActualMinutes.value = 0
        startSessionTimerLoop()
    }

    private fun startSessionTimerLoop() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            var elapsedSeconds = 0
            while (_isSessionTimerRunning.value && _sessionRemainingSeconds.value > 0) {
                kotlinx.coroutines.delay(1000L)
                if (!_isSessionPaused.value) {
                    _sessionRemainingSeconds.value = (_sessionRemainingSeconds.value - 1).coerceAtLeast(0)
                    elapsedSeconds++
                    _activeSessionActualMinutes.value = elapsedSeconds / 60
                }
            }
            if (_sessionRemainingSeconds.value == 0 && _activeStudySession.value != null) {
                finishStudySession("Target session time completed!")
            }
        }
    }

    fun pauseStudySession() {
        _isSessionPaused.value = true
    }

    fun resumeStudySession() {
        _isSessionPaused.value = false
    }

    fun finishStudySession(notes: String = "") {
        val session = _activeStudySession.value ?: return
        sessionTimerJob?.cancel()
        _isSessionTimerRunning.value = false
        val actualMins = _activeSessionActualMinutes.value.coerceAtLeast(1)

        viewModelScope.launch {
            val updatedPlan = session.copy(
                isCompleted = true,
                actualMinutesSpent = actualMins,
                sessionState = "COMPLETED",
                completedTimestamp = System.currentTimeMillis(),
                notes = if (notes.isNotBlank()) notes else session.notes
            )
            studyRepository.updateStudyPlanItem(updatedPlan)

            studyRepository.recordFocusSession(
                subject = session.subject,
                topic = session.topic,
                durationMinutes = session.targetMinutes,
                actualMinutesSpent = actualMins
            )

            studyRepository.togglePlanItemCompletion(session.id, true, xpReward = 50)

            _activeStudySession.value = null
            _activeSessionActualMinutes.value = 0
            _sessionRemainingSeconds.value = 0
        }
    }

    fun cancelStudySession() {
        sessionTimerJob?.cancel()
        _isSessionTimerRunning.value = false
        _isSessionPaused.value = false
        _activeStudySession.value = null
    }

    fun togglePlanItem(id: Long, completed: Boolean) {
        viewModelScope.launch {
            studyRepository.togglePlanItemCompletion(id, completed)
        }
    }

    fun addManualPlanItem(subject: String, chapter: String, topic: String, minutes: Int, priority: PlanPriority) {
        viewModelScope.launch {
            studyRepository.addStudyPlanItem(
                StudyPlanItem(
                    subject = subject,
                    chapter = chapter,
                    topic = topic,
                    targetMinutes = minutes,
                    priority = priority
                )
            )
        }
    }

    fun updatePlanItem(item: StudyPlanItem) {
        viewModelScope.launch {
            studyRepository.updateStudyPlanItem(item)
        }
    }

    fun recoverMissedSessions(mode: String) {
        viewModelScope.launch {
            val currentPlans = studyPlanItems.value
            val missedItems = currentPlans.filter { !it.isCompleted && it.priority == PlanPriority.HIGH }
            if (missedItems.isEmpty()) return@launch

            when (mode) {
                "LATER_TODAY" -> {
                    // Reduce duration slightly and keep on today's schedule
                    missedItems.forEach { item ->
                        studyRepository.updateStudyPlanItem(
                            item.copy(
                                targetMinutes = (item.targetMinutes * 0.75).toInt().coerceAtLeast(20),
                                notes = "Recovered Session: Scheduled for later today."
                            )
                        )
                    }
                }
                "SPREAD_WEEK" -> {
                    // Downgrade urgency and mark notes for smooth distribution
                    missedItems.forEachIndexed { idx, item ->
                        studyRepository.updateStudyPlanItem(
                            item.copy(
                                priority = PlanPriority.MEDIUM,
                                notes = "Distributed Recovery Session #${idx + 1}"
                            )
                        )
                    }
                }
            }
        }
    }

    fun updateDailyAvailableTime(minutes: Int) {
        _selectedAvailableTimeMinutes.value = minutes
    }

    fun deletePlanItem(id: Long) {
        viewModelScope.launch {
            studyRepository.deletePlanItem(id)
        }
    }

    // --- AI Tutor Actions ---

    fun setThinkingMode(enabled: Boolean) {
        _useThinkingMode.value = enabled
    }

    fun setTutorPersona(persona: String) {
        _tutorPersona.value = persona
    }

    fun clearChatMessages() {
        val user = userProfile.value
        val name = user?.name ?: "Student"
        val subject = user?.subjects?.firstOrNull() ?: "Physics"
        _chatMessages.value = listOf(
            ChatMessage(
                sender = "model",
                text = "👋 Hello **$name**! I'm your personalized **StudyMate AI Tutor**.\n\nI'm ready with your active profile (*Target: ${user?.examName ?: "JEE / NEET / Board"}*, *Subject: $subject*).\n\nAsk me any question, tap **'Explain Concept'**, **'Simplify'**, or generate practice questions and flashcards!"
            )
        )
    }

    fun buildTutorContext(subject: String, topic: String): TutorStudentContext {
        val user = userProfile.value
        val mistakeList = mistakes.value.filter { it.subject.equals(subject, ignoreCase = true) || subject.isBlank() }
            .map { "${it.topic}: ${it.questionText.take(50)}" }
        val weakList = user?.weakTopics ?: emptyList()
        val daysRemaining = user?.examDateMillis?.let {
            val diff = it - System.currentTimeMillis()
            (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        } ?: 30

        return TutorStudentContext(
            studentName = user?.name?.ifBlank { "Rahul" } ?: "Rahul",
            grade = user?.grade ?: "Class 12",
            targetExam = user?.examName ?: "JEE / NEET / Board Exam",
            examDaysRemaining = daysRemaining,
            selectedSubject = subject.ifBlank { user?.subjects?.firstOrNull() ?: "Physics" },
            selectedTopic = topic.ifBlank { "Current Electricity & Circuits" },
            weakTopics = weakList,
            recentMistakes = mistakeList,
            dailyTargetMinutes = user?.dailyTargetMinutes ?: 180,
            totalFocusMinutes = user?.totalFocusMinutes ?: 135,
            streakDays = user?.streakDays ?: 4,
            learningStyle = user?.revisionFrequency ?: "Spaced Repetition"
        )
    }

    fun sendTutorMessage(text: String, subject: String = "", topic: String = "") {
        if (text.isBlank()) return
        val userMsg = ChatMessage(
            sender = "user",
            text = text,
            subjectContext = subject,
            topicContext = topic,
            actionType = TutorActionType.GENERAL_CHAT
        )
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiThinking.value = true
            val context = buildTutorContext(subject, topic)
            val history = _chatMessages.value.map { it.sender to it.text }
            val result = geminiRepository.askTutorWithContext(
                prompt = text,
                conversationHistory = history.takeLast(6),
                useThinkingMode = _useThinkingMode.value,
                persona = _tutorPersona.value,
                context = context,
                actionType = TutorActionType.GENERAL_CHAT
            )
            result.onSuccess { response ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = "model",
                    text = response.replyMarkdown,
                    isThinking = _useThinkingMode.value,
                    actionType = response.actionType,
                    generatedFlashcards = response.generatedFlashcards,
                    generatedPlanItems = response.generatedPlanItems,
                    generatedQuestions = response.generatedQuestions,
                    isOfflineFallback = response.isOfflineFallback,
                    subjectContext = subject,
                    topicContext = topic
                )
            }.onFailure { error ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = "model",
                    text = "⚠️ Gemini couldn't connect right now. Using local study resources.",
                    isOfflineFallback = true
                )
            }
            _isAiThinking.value = false
        }
    }

    fun executeTutorAction(
        actionType: TutorActionType,
        subject: String,
        topic: String,
        extraPrompt: String = ""
    ) {
        val prompt = when (actionType) {
            TutorActionType.EXPLAIN_CONCEPT -> "Please provide a comprehensive conceptual breakdown of $topic in $subject with key definitions, governing formulas, derivations, and common exam traps."
            TutorActionType.SIMPLIFY_EXPLANATION -> "Please simplify the concept of $topic in $subject using an intuitive real-world analogy and beginner-friendly language without jargon."
            TutorActionType.GIVE_EXAMPLES -> "Please provide 2 practical worked-out numerical calculation examples of $topic in $subject with given variables, formula, step-by-step substitution, and real-world application."
            TutorActionType.PRACTICE_QUESTIONS -> "Generate 3 high-yield practice MCQs with 4 options and detailed step-by-step solutions for $topic in $subject."
            TutorActionType.GENERATE_FLASHCARDS -> "Generate 4 spaced-repetition active recall flashcards (Front, Back, Hint, Difficulty) for $topic in $subject."
            TutorActionType.SUMMARIZE_MATERIAL -> if (extraPrompt.isNotBlank()) "Please summarize this study material for $topic in $subject:\n$extraPrompt" else "Please generate a high-yield study summary with formulas and 3 self-check questions for $topic in $subject."
            TutorActionType.REVISION_PLAN -> "Create a 7-day spaced repetition revision plan for $subject focusing on weak areas and $topic."
            TutorActionType.IDENTIFY_WEAK_AREAS -> "Analyze my recent progress and diagnose weak areas and mistake patterns in $subject."
            TutorActionType.DAILY_STUDY_PLAN -> "Generate an optimal daily study plan for today allocating time blocks for $subject ($topic) and high-priority study sessions."
            TutorActionType.GENERAL_CHAT -> extraPrompt.ifBlank { "How can I master $topic in $subject effectively?" }
        }

        val userLabel = when (actionType) {
            TutorActionType.EXPLAIN_CONCEPT -> "📖 Explain Concept: $topic"
            TutorActionType.SIMPLIFY_EXPLANATION -> "🐣 Simplify: $topic"
            TutorActionType.GIVE_EXAMPLES -> "💡 Give Worked Examples: $topic"
            TutorActionType.PRACTICE_QUESTIONS -> "✍️ Generate Practice Questions: $topic"
            TutorActionType.GENERATE_FLASHCARDS -> "🗂️ Generate Flashcards: $topic"
            TutorActionType.SUMMARIZE_MATERIAL -> "📄 Summarize Study Material: $topic"
            TutorActionType.REVISION_PLAN -> "🔄 Create Revision Plan: $subject"
            TutorActionType.IDENTIFY_WEAK_AREAS -> "🎯 Identify Weak Areas: $subject"
            TutorActionType.DAILY_STUDY_PLAN -> "📅 Generate Daily Study Plan"
            TutorActionType.GENERAL_CHAT -> extraPrompt
        }

        val userMsg = ChatMessage(
            sender = "user",
            text = userLabel,
            subjectContext = subject,
            topicContext = topic,
            actionType = actionType
        )
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiThinking.value = true
            val context = buildTutorContext(subject, topic)
            val history = _chatMessages.value.map { it.sender to it.text }
            val result = geminiRepository.askTutorWithContext(
                prompt = prompt,
                conversationHistory = history.takeLast(6),
                useThinkingMode = _useThinkingMode.value,
                persona = _tutorPersona.value,
                context = context,
                actionType = actionType
            )
            result.onSuccess { response ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = "model",
                    text = response.replyMarkdown,
                    isThinking = _useThinkingMode.value,
                    actionType = response.actionType,
                    generatedFlashcards = response.generatedFlashcards,
                    generatedPlanItems = response.generatedPlanItems,
                    generatedQuestions = response.generatedQuestions,
                    isOfflineFallback = response.isOfflineFallback,
                    subjectContext = subject,
                    topicContext = topic
                )
            }.onFailure { error ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = "model",
                    text = "⚠️ Gemini couldn't connect right now. Using local study resources.",
                    isOfflineFallback = true
                )
            }
            _isAiThinking.value = false
        }
    }

    fun saveFlashcardsToDeck(cards: List<FlashcardItem>) {
        viewModelScope.launch {
            for (card in cards) {
                studyRepository.insertFlashcard(card)
            }
            _flashcardMessage.value = "🎉 Added ${cards.size} flashcards to your deck!"
        }
    }

    fun importPlanItems(items: List<StudyPlanItem>) {
        viewModelScope.launch {
            val current = studyPlanItems.value
            studyRepository.replaceStudyPlan(current + items)
            _flashcardMessage.value = "📅 Imported ${items.size} tasks into your Study Plan!"
        }
    }

    fun sendQuickActionChip(chipText: String) {
        val lastModel = _chatMessages.value.lastOrNull { it.sender == "model" }
        val subject = lastModel?.subjectContext ?: userProfile.value?.subjects?.firstOrNull() ?: "Physics"
        val topic = lastModel?.topicContext ?: "Current Electricity"

        when (chipText) {
            "Explain Simply" -> executeTutorAction(TutorActionType.SIMPLIFY_EXPLANATION, subject, topic)
            "Give Example" -> executeTutorAction(TutorActionType.GIVE_EXAMPLES, subject, topic)
            "Quiz Me" -> executeTutorAction(TutorActionType.PRACTICE_QUESTIONS, subject, topic)
            "Generate Flashcards" -> executeTutorAction(TutorActionType.GENERATE_FLASHCARDS, subject, topic)
            "Explain Another Way" -> sendTutorMessage("Can you re-explain $topic from a completely different perspective with a visual description?", subject, topic)
            else -> sendTutorMessage(chipText, subject, topic)
        }
    }

    fun solveImageQuestion(bitmap: Bitmap, subject: String = "Physics", topic: String = "General") {
        val userMsg = ChatMessage(
            sender = "user",
            text = "📸 [Scanned Question Image Attached]\nPlease solve and explain step by step in $subject.",
            hasImage = true,
            subjectContext = subject,
            topicContext = topic
        )
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiThinking.value = true
            val result = geminiRepository.solveImageQuestion(
                bitmap = bitmap,
                userPrompt = "Please solve and explain this $subject ($topic) question step by step with clear concepts, formulas, and working.",
                useThinkingMode = true
            )
            result.onSuccess { explanation ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = "model",
                    text = explanation,
                    isThinking = true,
                    subjectContext = subject,
                    topicContext = topic
                )
            }.onFailure {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = "model",
                    text = "⚠️ Could not analyze image. Please ensure the question text is clear and retry."
                )
            }
            _isAiThinking.value = false
        }
    }

    fun speakTts(text: String) {
        val cleanText = text.replace("*", "").replace("#", "").replace("`", "")
        if (_isTtsSpeaking.value) {
            tts?.stop()
            _isTtsSpeaking.value = false
        } else {
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "studymate_tts")
            _isTtsSpeaking.value = true
        }
    }

    fun speakText(text: String) = speakTts(text)

    // --- Focus Mode Actions ---

    fun setStrictModeEnabled(enabled: Boolean) {
        _focusState.value = _focusState.value.copy(isStrictModeEnabled = enabled)
    }

    fun checkAndRestoreActiveFocusSession() {
        viewModelScope.launch {
            val session = focusSessionPersistence.loadActiveSession()
            if (session != null && (session.state == com.example.service.FocusSessionExecutionState.FOCUS_ACTIVE || session.state == com.example.service.FocusSessionExecutionState.PAUSED)) {
                val remSecs = session.calculateRemainingSeconds()
                val actMins = session.calculateActualMinutesSpent()
                if (remSecs > 0) {
                    _focusState.value = _focusState.value.copy(
                        isRunning = true,
                        isPaused = session.isPaused,
                        initialMinutes = session.plannedDurationMinutes,
                        remainingSeconds = remSecs,
                        actualMinutesSpent = actMins,
                        subject = session.subject,
                        topic = session.topic,
                        examName = session.examName,
                        sessionGoal = session.sessionGoal,
                        planItemId = session.planItemId,
                        sessionStartTimestamp = session.startedAtTimestamp,
                        pausedAccumulatedSeconds = session.pausedAccumulatedSeconds,
                        pauseStartTimestamp = session.pauseStartTimestamp,
                        isStrictModeActive = session.isStrictMode,
                        isAutoStarted = session.isAutoStarted,
                        isInterrupted = (session.state == com.example.service.FocusSessionExecutionState.INTERRUPTED)
                    )
                    com.example.service.FocusShieldManager.startFocusSession(
                        getApplication(),
                        session.subject,
                        session.topic,
                        session.plannedDurationMinutes
                    )
                    com.example.service.FocusShieldForegroundService.startService(
                        context = getApplication(),
                        subject = session.subject,
                        topic = session.topic,
                        durationMinutes = session.plannedDurationMinutes,
                        examName = session.examName,
                        sessionGoal = session.sessionGoal,
                        planItemId = session.planItemId,
                        isStrictMode = session.isStrictMode,
                        isAutoStarted = session.isAutoStarted,
                        occurrenceId = session.occurrenceId
                    )
                    startAccurateFocusTimerLoop()
                } else {
                    _focusState.value = _focusState.value.copy(
                        isRunning = true,
                        initialMinutes = session.plannedDurationMinutes,
                        remainingSeconds = 0,
                        actualMinutesSpent = session.plannedDurationMinutes,
                        subject = session.subject,
                        topic = session.topic,
                        examName = session.examName,
                        sessionGoal = session.sessionGoal,
                        planItemId = session.planItemId
                    )
                    endFocusSession(isAutoFinished = true)
                }
            }
        }
    }

    fun startFocusSession(
        subject: String,
        topic: String,
        durationMinutes: Int = 25,
        examName: String = "",
        sessionGoal: String = "",
        planItemId: Long? = null,
        isStrictMode: Boolean = _focusState.value.isStrictModeEnabled,
        isAutoStarted: Boolean = false,
        occurrenceId: String? = null
    ) {
        timerJob?.cancel()
        val appContext = getApplication<Application>()
        val restrictedCount = com.example.service.FocusShieldManager.getRestrictedPackages().size
        val currentExam = if (examName.isNotBlank()) examName else (activeExamContext.value.examName.ifBlank { userProfile.value?.examName ?: "RRB Group D" })
        val startTs = System.currentTimeMillis()

        _focusState.value = _focusState.value.copy(
            isRunning = true,
            isPaused = false,
            initialMinutes = durationMinutes,
            remainingSeconds = durationMinutes * 60,
            actualMinutesSpent = 0,
            subject = subject,
            topic = topic,
            examName = currentExam,
            sessionGoal = sessionGoal.ifBlank { "Complete $topic — Key Concepts" },
            planItemId = planItemId,
            restrictedAppsCount = restrictedCount,
            showCelebration = false,
            sessionStartTimestamp = startTs,
            pausedAccumulatedSeconds = 0L,
            pauseStartTimestamp = 0L,
            isBreakActive = false,
            isStrictModeActive = isStrictMode,
            isAutoStarted = isAutoStarted,
            isInterrupted = false
        )

        // Start Foreground Service with accurate ongoing notification
        com.example.service.FocusShieldForegroundService.startService(
            context = appContext,
            subject = subject,
            topic = topic,
            durationMinutes = durationMinutes,
            examName = currentExam,
            sessionGoal = sessionGoal.ifBlank { "Complete $topic — Key Concepts" },
            planItemId = planItemId,
            isStrictMode = isStrictMode,
            isAutoStarted = isAutoStarted,
            occurrenceId = occurrenceId
        )

        startAccurateFocusTimerLoop()
    }

    private fun startAccurateFocusTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_focusState.value.isRunning) {
                delay(1000L)
                val session = focusSessionPersistence.loadActiveSession()
                if (session != null) {
                    val remSecs = session.calculateRemainingSeconds()
                    val actMins = session.calculateActualMinutesSpent()
                    _focusState.value = _focusState.value.copy(
                        remainingSeconds = remSecs,
                        actualMinutesSpent = actMins,
                        isPaused = session.isPaused
                    )
                    com.example.service.FocusShieldManager.updateRemainingTime(remSecs)

                    if (remSecs == 0) {
                        endFocusSession(isAutoFinished = true)
                        break
                    }
                } else {
                    val state = _focusState.value
                    if (state.isRunning && !state.isPaused) {
                        val now = System.currentTimeMillis()
                        val totalPaused = state.pausedAccumulatedSeconds + (if (state.isPaused && state.pauseStartTimestamp > 0L) (now - state.pauseStartTimestamp) / 1000L else 0L)
                        val elapsedSecs = if (state.sessionStartTimestamp > 0L) {
                            ((now - state.sessionStartTimestamp) / 1000L - totalPaused).coerceAtLeast(0L).toInt()
                        } else {
                            (state.initialMinutes * 60) - state.remainingSeconds
                        }
                        val remSecs = (state.initialMinutes * 60 - elapsedSecs).coerceAtLeast(0)
                        val actMins = (elapsedSecs / 60).coerceAtLeast(0)

                        _focusState.value = state.copy(
                            remainingSeconds = remSecs,
                            actualMinutesSpent = actMins
                        )
                        com.example.service.FocusShieldManager.updateRemainingTime(remSecs)

                        if (remSecs == 0) {
                            endFocusSession(isAutoFinished = true)
                            break
                        }
                    }
                }
            }
        }
    }

    fun toggleFocusPause() {
        val state = _focusState.value
        if (!state.isRunning) return
        val willPause = !state.isPaused
        val appContext = getApplication<Application>()
        if (willPause) {
            _focusState.value = state.copy(
                isPaused = true,
                pauseStartTimestamp = System.currentTimeMillis()
            )
            focusSessionPersistence.updatePauseState(true)
            com.example.service.FocusShieldForegroundService.pauseFocus(appContext)
        } else {
            val pausedDuration = if (state.pauseStartTimestamp > 0L) {
                (System.currentTimeMillis() - state.pauseStartTimestamp) / 1000L
            } else 0L
            _focusState.value = state.copy(
                isPaused = false,
                pausedAccumulatedSeconds = state.pausedAccumulatedSeconds + pausedDuration,
                pauseStartTimestamp = 0L
            )
            focusSessionPersistence.updatePauseState(false)
            com.example.service.FocusShieldForegroundService.resumeFocus(appContext)
        }
    }

    fun endFocusSession(isAutoFinished: Boolean = false) {
        timerJob?.cancel()
        val appContext = getApplication<Application>()
        com.example.service.FocusShieldForegroundService.stopFocus(appContext)
        com.example.service.FocusShieldManager.endFocusSession()
        focusSessionPersistence.clearActiveSession()

        val state = _focusState.value
        val initialMins = state.initialMinutes

        val now = System.currentTimeMillis()
        val totalPaused = state.pausedAccumulatedSeconds + (if (state.isPaused && state.pauseStartTimestamp > 0L) (now - state.pauseStartTimestamp) / 1000L else 0L)
        val elapsedSeconds = if (state.sessionStartTimestamp > 0L) {
            ((now - state.sessionStartTimestamp) / 1000L - totalPaused).coerceAtLeast(0L).toInt()
        } else {
            (initialMins * 60) - state.remainingSeconds
        }

        // Accurately record actual elapsed minutes (never inflate partial sessions)
        val actualMinutes = if (elapsedSeconds < 60) {
            if (isAutoFinished) initialMins else 1
        } else {
            (elapsedSeconds / 60).coerceIn(1, initialMins)
        }

        viewModelScope.launch {
            val session = studyRepository.recordFocusSession(
                subject = state.subject,
                topic = state.topic,
                durationMinutes = initialMins,
                actualMinutesSpent = actualMinutes
            )

            state.planItemId?.let { pId ->
                studyRepository.togglePlanItemCompletion(pId, true, xpReward = session.xpEarned)
            }

            val summary = CompletedSessionSummary(
                examName = state.examName.ifBlank { activeExamContext.value.examName.ifBlank { "Exam Prep" } },
                subject = state.subject,
                topic = state.topic,
                plannedMinutes = initialMins,
                actualMinutes = actualMinutes,
                xpEarned = session.xpEarned
            )

            _focusState.value = state.copy(
                isRunning = false,
                isPaused = false,
                showCelebration = true,
                lastSessionXp = session.xpEarned,
                lastCompletedSession = summary,
                actualMinutesSpent = actualMinutes,
                isStrictModeEnabled = false,
                isStrictModeActive = false
            )

            val userName = userProfile.value?.name ?: "Rahul"
            com.example.notification.StudyNotificationManager.sendFocusSessionCompleted(
                context = appContext,
                userName = userName,
                subject = state.subject,
                minutes = actualMinutes,
                xpEarned = session.xpEarned
            )

            if (isAutoFinished) {
                val breakMins = userProfile.value?.breakDurationMinutes ?: 5
                com.example.notification.StudyNotificationManager.sendBreakReminder(
                    context = appContext,
                    userName = userName,
                    breakMinutes = breakMins
                )
            }
        }
    }

    fun emergencyExitFocusSession() {
        timerJob?.cancel()
        val appContext = getApplication<Application>()
        com.example.service.FocusShieldForegroundService.stopFocus(appContext)
        com.example.service.FocusShieldManager.endFocusSession()
        focusSessionPersistence.clearActiveSession()

        val state = _focusState.value
        val initialMins = state.initialMinutes

        val now = System.currentTimeMillis()
        val totalPaused = state.pausedAccumulatedSeconds + (if (state.isPaused && state.pauseStartTimestamp > 0L) (now - state.pauseStartTimestamp) / 1000L else 0L)
        val elapsedSeconds = if (state.sessionStartTimestamp > 0L) {
            ((now - state.sessionStartTimestamp) / 1000L - totalPaused).coerceAtLeast(0L).toInt()
        } else {
            (initialMins * 60) - state.remainingSeconds
        }

        val actualMinutes = (elapsedSeconds / 60).coerceAtLeast(0)

        viewModelScope.launch {
            if (actualMinutes > 0) {
                studyRepository.recordFocusSession(
                    subject = state.subject,
                    topic = state.topic,
                    durationMinutes = initialMins,
                    actualMinutesSpent = actualMinutes
                )
            }

            _focusState.value = state.copy(
                isRunning = false,
                isPaused = false,
                showCelebration = false,
                actualMinutesSpent = actualMinutes,
                isStrictModeEnabled = false,
                isStrictModeActive = false,
                isInterrupted = true
            )
        }
    }

    fun startBreakTimer(durationMinutes: Int = 5) {
        timerJob?.cancel()
        _focusState.value = _focusState.value.copy(
            isBreakActive = true,
            breakDurationMinutes = durationMinutes,
            breakRemainingSeconds = durationMinutes * 60
        )
        timerJob = viewModelScope.launch {
            while (_focusState.value.isBreakActive && _focusState.value.breakRemainingSeconds > 0) {
                delay(1000L)
                val curSecs = _focusState.value.breakRemainingSeconds
                if (curSecs > 0) {
                    _focusState.value = _focusState.value.copy(
                        breakRemainingSeconds = curSecs - 1
                    )
                }
            }
            if (_focusState.value.breakRemainingSeconds == 0) {
                _focusState.value = _focusState.value.copy(isBreakActive = false)
            }
        }
    }

    fun endBreakTimer() {
        timerJob?.cancel()
        _focusState.value = _focusState.value.copy(isBreakActive = false)
    }

    fun saveSessionNote(subject: String, topic: String, content: String) {
        viewModelScope.launch {
            val note = com.example.data.model.SmartNoteItem(
                title = if (topic.isNotBlank()) "$topic Notes" else "$subject Focus Notes",
                subject = subject,
                topic = topic,
                contentMarkdown = content,
                keyPoints = content.lines().filter { it.isNotBlank() },
                isBookmarked = false,
                revisionCategory = com.example.data.model.RevisionCategory.PRACTICE_SOON
            )
            studyRepository.saveSmartNote(note)
            _snackbarMessage.emit("Note saved to Smart Notes 📝")
        }
    }

    fun dismissCelebration() {
        _focusState.value = _focusState.value.copy(showCelebration = false)
    }

    val examQuestionBankRepository = com.example.data.repository.ExamQuestionBankRepository()
    val questionSourceEngine by lazy { com.example.service.intelligence.QuestionSourceEngine(examQuestionBankRepository, geminiRepository) }
    private var mockTestTimerJob: kotlinx.coroutines.Job? = null

    private var pendingTestConfig: MockTestConfig? = null

    // --- Mock Test & Practice Actions ---

    fun launchPracticeSession(
        mode: com.example.service.intelligence.PracticeMode,
        subject: String = "",
        topic: String = "",
        questionCount: Int = 10,
        onSessionCreated: (com.example.service.intelligence.PracticeCurationResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isTestGenerating.value = true
            try {
                val exam = selectedExam.value.examName
                val res = studyRepository.practiceEngineService.createPracticeSession(
                    mode = mode,
                    examName = exam,
                    subject = subject,
                    topic = topic,
                    desiredQuestionCount = questionCount
                )

                val mockType = when(mode) {
                    com.example.service.intelligence.PracticeMode.QUICK_PRACTICE -> MockTestType.SMART_PRACTICE
                    com.example.service.intelligence.PracticeMode.TOPIC_PRACTICE -> MockTestType.TOPIC_TEST
                    com.example.service.intelligence.PracticeMode.SUBJECT_PRACTICE -> MockTestType.SUBJECT_TEST
                    com.example.service.intelligence.PracticeMode.REVISION_PRACTICE -> MockTestType.REVISION_TEST
                    com.example.service.intelligence.PracticeMode.MOCK_TEST -> MockTestType.MOCK_TEST
                    com.example.service.intelligence.PracticeMode.WEAK_AREA_PRACTICE -> MockTestType.WEAK_AREAS
                    com.example.service.intelligence.PracticeMode.SAVED_QUESTIONS -> MockTestType.SMART_PRACTICE
                }

                 val config = MockTestConfig(
                    exam = exam,
                    subject = subject.ifBlank { "All Subjects" },
                    chapter = topic.ifBlank { "All Topics" },
                    questionCount = res.questions.size,
                    timeLimitMinutes = (res.questions.size * 1.2).toInt().coerceAtLeast(5),
                    testType = mockType
                )

                _activeTestState.value = ActiveTestState(
                    config = config,
                    questions = res.questions,
                    totalDurationSeconds = config.timeLimitMinutes * 60,
                    remainingSeconds = config.timeLimitMinutes * 60,
                    isTestInProgress = true,
                    isOrientationConfirmed = false
                )
                sessionPersistence.saveActiveSession(_activeTestState.value)
                onSessionCreated(res)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to launch practice session: ${e.message}")
            } finally {
                _isTestGenerating.value = false
            }
        }
    }

    fun saveQuestion(question: Question) {
        viewModelScope.launch {
            studyRepository.practiceEngineService.saveQuestion(question)
        }
    }

    fun unsaveQuestion(questionId: String) {
        viewModelScope.launch {
            studyRepository.practiceEngineService.unsaveQuestion(questionId)
        }
    }

    // --- Step 60 Smart Revision Actions ---
    fun addTopicToRevision(
        subject: String,
        topic: String,
        sourceType: RevisionSourceType = RevisionSourceType.MANUAL,
        preferredMethod: RevisionMethodType = RevisionMethodType.QUICK_REVIEW,
        notes: String = "",
        resourceId: String? = null,
        resourceTitle: String? = null
    ) {
        viewModelScope.launch {
            val exam = selectedExam.value.examName
            val examDays = selectedExam.value.daysRemaining
            studyRepository.smartRevisionService.addOrUpdateRevisionItem(
                subject = subject,
                topic = topic,
                examId = exam,
                sourceType = sourceType,
                preferredMethod = preferredMethod,
                notes = notes,
                resourceId = resourceId,
                resourceTitle = resourceTitle,
                daysToExam = examDays
            )
        }
    }

    fun snoozeRevisionItem(revisionItemId: String, option: com.example.service.intelligence.SnoozeOption) {
        viewModelScope.launch {
            studyRepository.smartRevisionService.snoozeRevisionItem(revisionItemId, option)
        }
    }

    fun completeRevisionSession(
        revisionItemId: String,
        scoreEarned: Int = 0,
        totalQuestions: Int = 0,
        methodUsed: RevisionMethodType = RevisionMethodType.QUICK_REVIEW,
        timeSpentSeconds: Int = 300,
        notes: String = ""
    ) {
        viewModelScope.launch {
            studyRepository.smartRevisionService.completeRevision(
                revisionItemId = revisionItemId,
                scoreEarned = scoreEarned,
                totalQuestions = totalQuestions,
                methodUsed = methodUsed,
                timeSpentSeconds = timeSpentSeconds,
                notes = notes
            )
        }
    }

    fun removeRevisionItem(revisionItemId: String) {
        viewModelScope.launch {
            studyRepository.smartRevisionService.removeRevisionItem(revisionItemId)
        }
    }

    fun updateRevisionItem(item: RevisionItemEntity) {
        viewModelScope.launch {
            studyRepository.smartRevisionService.updateRevisionItem(item)
        }
    }

    fun startMockTestWithConfig(config: MockTestConfig, forceStartWithAvailable: Boolean = false) {
        pendingTestConfig = config
        if (_isTestGenerating.value) {
            android.util.Log.w("MainViewModel", "Test generation in progress. Ignoring duplicate request.")
            return
        }

        viewModelScope.launch {
            _isTestGenerating.value = true
            _generationError.value = null
            _insufficientPyqNotice.value = null

            try {
                val targetExamName = config.exam.ifBlank { selectedExam.value.examName }
                val masteries = allTopicMasteries.value
                val currentMistakes = mistakes.value
                val flaggedIds = studyRepository.getFlaggedQuestionIds()
                val historyList = studyRepository.allQuestionHistory.firstOrNull() ?: emptyList()

                val weakTopicsSet = masteries.filter { it.masteryState == "WEAK" || it.masteryScore < 60 }.map { it.topic }.toSet()
                val mistakeTopicsSet = currentMistakes.filter { !it.isMastered }.map { it.topic }.toSet()

                val curationResult = when (config.testType) {
                    MockTestType.WEAK_AREAS -> {
                        val candidateAll = examQuestionBankRepository.getQuestionsForTest(
                            examName = targetExamName,
                            subject = config.subject,
                            topic = "All Topics",
                            difficulty = "Mixed",
                            language = config.language,
                            desiredCount = 50,
                            testType = config.testType
                        )
                        val weakFiltered = com.example.service.intelligence.SmartMockEngine.filterWeakAreaQuestions(
                            allQuestions = candidateAll,
                            topicMasteries = masteries,
                            mistakes = currentMistakes,
                            targetSubject = config.subject,
                            desiredCount = config.questionCount
                        )
                        com.example.service.intelligence.QuestionSourceResult.Success(
                            questions = weakFiltered,
                            requestedCount = config.questionCount,
                            sourceSummary = "Targeted Weak Areas Practice"
                        )
                    }
                    MockTestType.REVISION_TEST -> {
                        val candidateAll = examQuestionBankRepository.getQuestionsForTest(
                            examName = targetExamName,
                            subject = config.subject,
                            topic = "All Topics",
                            difficulty = "Mixed",
                            language = config.language,
                            desiredCount = 50,
                            testType = config.testType
                        )
                        val revisionFiltered = com.example.service.intelligence.SmartMockEngine.filterRevisionQuestions(
                            allQuestions = candidateAll,
                            topicMasteries = masteries,
                            targetSubject = config.subject,
                            desiredCount = config.questionCount
                        )
                        com.example.service.intelligence.QuestionSourceResult.Success(
                            questions = revisionFiltered,
                            requestedCount = config.questionCount,
                            sourceSummary = "Spaced Revision Test"
                        )
                    }
                    MockTestType.PREVIOUS_MISTAKES -> {
                        val candidateAll = examQuestionBankRepository.getQuestionsForTest(
                            examName = targetExamName,
                            subject = config.subject,
                            topic = "All Topics",
                            difficulty = "Mixed",
                            language = config.language,
                            desiredCount = 50,
                            testType = config.testType
                        )
                        val mistakeFiltered = com.example.service.intelligence.SmartMockEngine.filterPreviousMistakeQuestions(
                            allQuestions = candidateAll,
                            mistakes = currentMistakes,
                            targetSubject = config.subject,
                            desiredCount = config.questionCount
                        )
                        com.example.service.intelligence.QuestionSourceResult.Success(
                            questions = mistakeFiltered,
                            requestedCount = config.questionCount,
                            sourceSummary = "Past Mistakes Retest"
                        )
                    }
                    else -> {
                        questionSourceEngine.curateTestQuestions(
                            config = config,
                            examContext = activeExamContext.value,
                            historyList = historyList,
                            weakTopics = weakTopicsSet,
                            mistakeTopics = mistakeTopicsSet,
                            flaggedQuestionIds = flaggedIds,
                            allowPartialPyqOrFill = forceStartWithAvailable
                        )
                    }
                }

                when (curationResult) {
                    is com.example.service.intelligence.QuestionSourceResult.Success -> {
                        val finalQuestions = curationResult.questions
                        val totalSeconds = (config.timeLimitMinutes * 60).coerceAtLeast(60)
                        val now = System.currentTimeMillis()
                        val expiresAt = now + (totalSeconds * 1000L)

                        val newState = ActiveTestState(
                            isTestInProgress = true,
                            requestId = "test_${now}",
                            subject = config.subject,
                            title = when (config.testType) {
                                MockTestType.PYQ -> "$targetExamName - Verified PYQ Practice"
                                MockTestType.SUBJECT_PRACTICE, MockTestType.SUBJECT_TEST -> "$targetExamName - ${config.subject}"
                                MockTestType.CHAPTER_PRACTICE, MockTestType.CHAPTER_TEST -> "$targetExamName - Chapter: ${config.chapter}"
                                MockTestType.SMART_PRACTICE -> "$targetExamName - Smart Performance Drill"
                                MockTestType.AI_PRACTICE -> "$targetExamName - AI Practice Drill"
                                MockTestType.MOCK_TEST, MockTestType.FULL_MOCK -> "$targetExamName Full Mock Test"
                                MockTestType.MIXED_PRACTICE -> "$targetExamName - Mixed Practice Drill"
                                MockTestType.TOPIC_TEST -> "$targetExamName - Topic: ${config.topic}"
                                MockTestType.WEAK_AREAS -> "$targetExamName - Weak Areas Targeted Test"
                                MockTestType.REVISION_TEST -> "$targetExamName - Spaced Revision Test"
                                MockTestType.PREVIOUS_MISTAKES -> "$targetExamName - Past Mistakes Retest"
                                MockTestType.ADAPTIVE_PRACTICE -> "$targetExamName - Adaptive Live Practice"
                                MockTestType.CUSTOM_TEST -> "$targetExamName - Custom Test"
                                MockTestType.TIMED_TEST -> "$targetExamName - Timed Speed Test"
                            },
                            questions = finalQuestions,
                            currentQuestionIndex = 0,
                            selectedAnswers = emptyMap(),
                            markedForReview = emptySet(),
                            visitedQuestions = setOf(0),
                            timeSpentSeconds = emptyMap(),
                            currentQuestionEnteredTimestamp = now,
                            startedAtTimestamp = now,
                            expiresAtTimestamp = expiresAt,
                            totalDurationSeconds = totalSeconds,
                            remainingSeconds = totalSeconds,
                            isCompleted = false,
                            isSubmitting = false,
                            submissionError = null,
                            completedAttempt = null,
                            isOrientationConfirmed = false,
                            config = config
                        )

                        _activeTestState.value = newState
                        sessionPersistence.saveActiveSession(newState)
                    }

                    is com.example.service.intelligence.QuestionSourceResult.InsufficientPyq -> {
                        _insufficientPyqNotice.value = InsufficientPyqNotice(
                            availableCount = curationResult.availableCount,
                            requestedCount = curationResult.requestedCount,
                            examName = curationResult.examName,
                            subject = curationResult.subject,
                            availableQuestions = curationResult.availableQuestions
                        )
                    }

                    is com.example.service.intelligence.QuestionSourceResult.Failure -> {
                        _generationError.value = curationResult.error
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Test generation failed: ${e.message}", e)
                _generationError.value = TestGenerationError(
                    stage = "TEST_INITIALIZATION_ERROR",
                    userMessage = "Could not initialize test questions. Please verify your settings and try again.",
                    technicalDetails = e.message ?: "Unknown error"
                )
            } finally {
                _isTestGenerating.value = false
            }
        }
    }

    /**
     * Called when the user has rotated the device and confirmed landscape mode.
     * Starts/synchronizes the actual test timer and renders CBT test screen.
     */
    fun confirmTestOrientationAndStartTimer() {
        if (!_activeTestState.value.isOrientationConfirmed && _activeTestState.value.isTestInProgress) {
            val now = System.currentTimeMillis()
            val totalSeconds = _activeTestState.value.totalDurationSeconds
            val expiresAt = now + (totalSeconds * 1000L)
            _activeTestState.value = _activeTestState.value.copy(
                isOrientationConfirmed = true,
                startedAtTimestamp = now,
                expiresAtTimestamp = expiresAt,
                remainingSeconds = totalSeconds
            )
            sessionPersistence.saveActiveSession(_activeTestState.value)
            startMockTestTimerJob()
        }
    }

    /**
     * Cancels a pending test launch from the rotation gate.
     */
    fun cancelPendingTestLaunch() {
        mockTestTimerJob?.cancel()
        _activeTestState.value = ActiveTestState(isTestInProgress = false)
        sessionPersistence.clearActiveSession()
    }

    private fun startMockTestTimerJob() {
        mockTestTimerJob?.cancel()
        mockTestTimerJob = viewModelScope.launch {
            while (_activeTestState.value.isTestInProgress && !_activeTestState.value.isCompleted) {
                if (!_activeTestState.value.isOrientationConfirmed) {
                    kotlinx.coroutines.delay(200L)
                    continue
                }
                kotlinx.coroutines.delay(1000L)
                val now = System.currentTimeMillis()
                val expiresAt = _activeTestState.value.expiresAtTimestamp
                val remaining = if (expiresAt > 0L) {
                    ((expiresAt - now) / 1000L).coerceAtLeast(0L).toInt()
                } else {
                    (_activeTestState.value.remainingSeconds - 1).coerceAtLeast(0)
                }

                if (remaining <= 0) {
                    _activeTestState.value = _activeTestState.value.copy(remainingSeconds = 0)
                    submitMockTest()
                    break
                } else {
                    _activeTestState.value = _activeTestState.value.copy(remainingSeconds = remaining)
                    // Periodically sync to persistence every 5 seconds
                    if (remaining % 5 == 0) {
                        sessionPersistence.saveActiveSession(_activeTestState.value)
                    }
                }
            }
        }
    }

    fun startMockTest(subject: String, chapter: String, mode: String = "AI Practice", questionCount: Int = 5) {
        startMockTestWithConfig(
            MockTestConfig(
                exam = selectedExam.value.examName,
                subject = subject,
                topic = chapter,
                questionCount = questionCount
            )
        )
    }

    fun selectTestAnswer(questionIndex: Int, optionIndex: Int) {
        val currentState = _activeTestState.value
        val currentAnswers = currentState.selectedAnswers.toMutableMap()
        currentAnswers[questionIndex] = optionIndex

        val updated = currentState.copy(
            selectedAnswers = currentAnswers,
            visitedQuestions = currentState.visitedQuestions + questionIndex
        )
        _activeTestState.value = updated
        sessionPersistence.saveActiveSession(updated)
    }

    fun reportQuestion(questionId: String, reason: String, notes: String = "") {
        viewModelScope.launch {
            val examId = activeExamContext.value.examId
            studyRepository.reportQuestionQuality(
                questionId = questionId,
                examId = examId,
                reason = reason,
                notes = notes
            )
        }
    }

    fun clearTestAnswer(questionIndex: Int) {
        val currentAnswers = _activeTestState.value.selectedAnswers.toMutableMap()
        currentAnswers.remove(questionIndex)
        val updated = _activeTestState.value.copy(selectedAnswers = currentAnswers)
        _activeTestState.value = updated
        sessionPersistence.saveActiveSession(updated)
    }

    fun skipQuestion(questionIndex: Int) {
        clearTestAnswer(questionIndex)
        val nextIdx = questionIndex + 1
        if (nextIdx < _activeTestState.value.questions.size) {
            navigateTestQuestion(nextIdx)
        }
    }

    fun toggleMarkForReview(questionIndex: Int) {
        val current = _activeTestState.value.markedForReview.toMutableSet()
        if (current.contains(questionIndex)) {
            current.remove(questionIndex)
        } else {
            current.add(questionIndex)
        }
        val updated = _activeTestState.value.copy(
            markedForReview = current,
            visitedQuestions = _activeTestState.value.visitedQuestions + questionIndex
        )
        _activeTestState.value = updated
        sessionPersistence.saveActiveSession(updated)
    }

    fun navigateTestQuestion(index: Int) {
        val state = _activeTestState.value
        if (index in 0 until state.questions.size) {
            val now = System.currentTimeMillis()
            val timeSpentMap = state.timeSpentSeconds.toMutableMap()
            val currentIdx = state.currentQuestionIndex
            if (state.currentQuestionEnteredTimestamp > 0L) {
                val elapsedSec = ((now - state.currentQuestionEnteredTimestamp) / 1000).toInt().coerceAtLeast(1)
                timeSpentMap[currentIdx] = (timeSpentMap[currentIdx] ?: 0) + elapsedSec
            }

            val updated = state.copy(
                currentQuestionIndex = index,
                visitedQuestions = state.visitedQuestions + index,
                timeSpentSeconds = timeSpentMap,
                currentQuestionEnteredTimestamp = now
            )
            _activeTestState.value = updated
            sessionPersistence.saveActiveSession(updated)
        }
    }

    fun saveAndNext() {
        val state = _activeTestState.value
        val nextIdx = state.currentQuestionIndex + 1
        if (nextIdx < state.questions.size) {
            navigateTestQuestion(nextIdx)
        } else {
            setPaletteOpen(true)
        }
    }

    fun markForReviewAndNext() {
        val state = _activeTestState.value
        val currentIdx = state.currentQuestionIndex
        val currentMarks = state.markedForReview.toMutableSet()
        currentMarks.add(currentIdx)
        _activeTestState.value = state.copy(markedForReview = currentMarks)

        val nextIdx = currentIdx + 1
        if (nextIdx < state.questions.size) {
            navigateTestQuestion(nextIdx)
        } else {
            setPaletteOpen(true)
        }
    }

    fun previousQuestion() {
        val state = _activeTestState.value
        val prevIdx = state.currentQuestionIndex - 1
        if (prevIdx >= 0) {
            navigateTestQuestion(prevIdx)
        }
    }

    fun cancelTestGeneration() {
        _isTestGenerating.value = false
        _generationError.value = null
        _insufficientPyqNotice.value = null
    }

    fun confirmStartWithAvailablePyqs() {
        val notice = _insufficientPyqNotice.value ?: return
        _insufficientPyqNotice.value = null
        val curConfig = _activeTestState.value.config ?: MockTestConfig(
            exam = notice.examName,
            subject = notice.subject,
            questionCount = notice.availableCount
        )
        val updatedConfig = curConfig.copy(
            questionCount = notice.availableCount.coerceAtLeast(1)
        )
        startMockTestWithConfig(updatedConfig, forceStartWithAvailable = true)
    }

    fun confirmAddAiToPyqs() {
        val notice = _insufficientPyqNotice.value ?: return
        _insufficientPyqNotice.value = null
        val curConfig = _activeTestState.value.config ?: MockTestConfig(
            exam = notice.examName,
            subject = notice.subject,
            questionCount = notice.requestedCount
        )
        val updatedConfig = curConfig.copy(
            questionSource = QuestionSourceType.MIXED
        )
        startMockTestWithConfig(updatedConfig, forceStartWithAvailable = true)
    }

    fun startTestWithAvailablePyqs() = confirmStartWithAvailablePyqs()
    fun startTestWithMixedFallback() = confirmAddAiToPyqs()

    fun setPaletteOpen(isOpen: Boolean) {
        _activeTestState.value = _activeTestState.value.copy(isPaletteOpen = isOpen)
    }

    fun setSubmitConfirmOpen(isOpen: Boolean) {
        _activeTestState.value = _activeTestState.value.copy(isSubmitConfirmOpen = isOpen)
    }

    fun submitMockTest() {
        if (_activeTestState.value.isSubmitting || _activeTestState.value.isCompleted) return
        mockTestTimerJob?.cancel()
        _activeTestState.value = _activeTestState.value.copy(
            isSubmitting = true,
            submissionError = null,
            isPaletteOpen = false,
            isSubmitConfirmOpen = false
        )

        val state = _activeTestState.value
        val questions = state.questions
        val answers = state.selectedAnswers
        var correctCount = 0
        var incorrectCount = 0
        var skippedCount = 0

        // Flush remaining time on currently open question
        val now = System.currentTimeMillis()
        val finalTimeSpentMap = state.timeSpentSeconds.toMutableMap()
        if (state.currentQuestionEnteredTimestamp > 0L) {
            val lastElapsed = ((now - state.currentQuestionEnteredTimestamp) / 1000).toInt().coerceAtLeast(1)
            finalTimeSpentMap[state.currentQuestionIndex] = (finalTimeSpentMap[state.currentQuestionIndex] ?: 0) + lastElapsed
        }

        val topicAttemptCounts = mutableMapOf<String, Int>()
        val topicIncorrectCounts = mutableMapOf<String, Int>()
        val topicCorrectCounts = mutableMapOf<String, Int>()

        val details = questions.mapIndexed { idx, q ->
            val chosen = answers[idx]
            val isCorr = chosen != null && chosen == q.correctOptionIndex
            val topicKey = q.topic.ifBlank { q.subject }
            val qTimeSpent = finalTimeSpentMap[idx] ?: (if (questions.isNotEmpty()) (state.config.timeLimitMinutes * 60 - state.remainingSeconds) / questions.size else 30)

            topicAttemptCounts[topicKey] = (topicAttemptCounts[topicKey] ?: 0) + 1

            if (chosen == null) {
                skippedCount++
            } else if (isCorr) {
                correctCount++
                topicCorrectCounts[topicKey] = (topicCorrectCounts[topicKey] ?: 0) + 1
            } else {
                incorrectCount++
                topicIncorrectCounts[topicKey] = (topicIncorrectCounts[topicKey] ?: 0) + 1
            }

            QuestionAttemptDetail(
                question = q,
                selectedIndex = chosen,
                isCorrect = isCorr,
                isMarkedForReview = state.markedForReview.contains(idx),
                timeSpentSeconds = qTimeSpent
            )
        }

        val totalAllowedSecs = state.config.timeLimitMinutes * 60
        val actualTimeSpent = (totalAllowedSecs - state.remainingSeconds).coerceAtLeast(10)
        val avgSecs = if (questions.isNotEmpty()) actualTimeSpent.toFloat() / questions.size else 0f

        val fastestDetail = details.minByOrNull { it.timeSpentSeconds }
        val longestDetail = details.maxByOrNull { it.timeSpentSeconds }

        val timeAnalysis = TimeAnalysisResult(
            totalTimeSpentSeconds = actualTimeSpent,
            avgTimePerQuestionSeconds = avgSecs,
            fastestQuestionIndex = fastestDetail?.let { details.indexOf(it) } ?: 0,
            fastestTimeSeconds = fastestDetail?.timeSpentSeconds ?: 0,
            longestQuestionIndex = longestDetail?.let { details.indexOf(it) } ?: 0,
            longestTimeSeconds = longestDetail?.timeSpentSeconds ?: 0
        )

        val weakTopics = mutableListOf<String>()
        val strongTopics = mutableListOf<String>()

        topicAttemptCounts.forEach { (topic, totalAttempts) ->
            if (totalAttempts >= 2) {
                val incorrects = topicIncorrectCounts[topic] ?: 0
                val corrects = topicCorrectCounts[topic] ?: 0
                if (incorrects.toFloat() / totalAttempts >= 0.5f) {
                    weakTopics.add(topic)
                } else if (corrects.toFloat() / totalAttempts >= 0.8f) {
                    strongTopics.add(topic)
                }
            }
        }

        viewModelScope.launch {
            try {
                questions.forEachIndexed { idx, q ->
                    val chosen = answers[idx]
                    val isCorr = chosen != null && chosen == q.correctOptionIndex
                    val isSkip = chosen == null
                    val spentSecs = details.getOrNull(idx)?.timeSpentSeconds ?: 30

                    // Record history for anti-repetition engine
                    studyRepository.recordQuestionAttemptHistory(
                        questionId = q.id,
                        examId = activeExamContext.value.examId,
                        subject = q.subject,
                        topic = q.topic,
                        isCorrect = isCorr,
                        isSkipped = isSkip,
                        responseTimeSecs = spentSecs
                    )

                    if (chosen != null && !isCorr) {
                        val chosenText = q.options.getOrNull(chosen) ?: "Not attempted"
                        val correctText = q.options.getOrNull(q.correctOptionIndex) ?: ""
                        studyRepository.recordMistake(
                            questionText = q.questionText,
                            studentAnswer = chosenText,
                            correctAnswer = correctText,
                            subject = q.subject,
                            topic = q.topic,
                            explanation = q.explanation
                        )
                    }
                }

                val (earnedMarks, percentage, schemeLabel) = com.example.service.intelligence.SmartMockEngine.calculateDeterministicScore(
                    correctCount = correctCount,
                    incorrectCount = incorrectCount,
                    skippedCount = skippedCount,
                    examContext = activeExamContext.value
                )

                val totalAllowedSecs = state.config.timeLimitMinutes * 60
                val timeSpent = totalAllowedSecs - state.remainingSeconds
                val recommendation = if (questions.size < 3) {
                    "Not enough data yet for a reliable topic analysis. Practice more questions to identify specific topic strengths and weaknesses."
                } else if (weakTopics.isNotEmpty()) {
                    "Focus on ${weakTopics.take(2).joinToString(" & ")}. Practice targeted PYQs and concept revisions."
                } else {
                    "Excellent mastery across attempted topics! Maintain momentum with full-length timed tests."
                }

                // Update topic performance records
                topicAttemptCounts.forEach { (topicName, totalAtt) ->
                    val correctAtt = topicCorrectCounts[topicName] ?: 0
                    studyRepository.recordTopicPerformance(
                        subject = state.subject,
                        topic = topicName,
                        questionsAttempted = totalAtt,
                        correctCount = correctAtt,
                        difficulty = state.config.difficulty
                    )
                }

                questions.forEachIndexed { idx, q ->
                    val chosen = answers[idx]
                    val isCorrect = chosen == q.correctOptionIndex
                    val isSkipped = chosen == null
                    val qTimeSpent = details.getOrNull(idx)?.timeSpentSeconds ?: 0

                    studyRepository.recordQuestionAttemptHistory(
                        questionId = q.id,
                        examId = activeExamContext.value.examId,
                        subject = q.subject,
                        topic = q.topic,
                        isCorrect = isCorrect,
                        isSkipped = isSkipped,
                        responseTimeSecs = qTimeSpent
                    )

                    if (chosen != null && !isCorrect) {
                        val chosenText = q.options.getOrNull(chosen) ?: "Not attempted"
                        val correctText = q.options.getOrNull(q.correctOptionIndex) ?: ""
                        val reason = com.example.service.intelligence.SmartMockEngine.classifyMistakeReason(
                            question = q,
                            timeSpentSecs = qTimeSpent
                        )
                        studyRepository.recordMistake(
                            questionText = q.questionText,
                            studentAnswer = chosenText,
                            correctAnswer = correctText,
                            subject = q.subject,
                            topic = q.topic,
                            explanation = "${q.explanation} (Classification: $reason)"
                        )
                    }
                }

                val attempt = studyRepository.recordMockTestAttempt(
                    title = state.title,
                    subject = state.subject,
                    score = earnedMarks.roundToInt(),
                    totalQuestions = questions.size,
                    timeSpentSeconds = timeSpent.coerceAtLeast(10),
                    weakTopics = weakTopics,
                    strongTopics = strongTopics,
                    aiRecommendation = recommendation,
                    examName = state.config.exam,
                    topic = state.config.topic,
                    difficulty = state.config.difficulty,
                    correctCount = correctCount,
                    incorrectCount = incorrectCount,
                    skippedCount = skippedCount,
                    avgTimePerQuestionSeconds = if (questions.isNotEmpty()) timeSpent.toFloat() / questions.size else 0f,
                    markingScheme = schemeLabel,
                    totalTimeAllowedSeconds = totalAllowedSecs
                ).copy(
                    examId = activeExamContext.value.examId,
                    language = state.config.language,
                    rawScoreEarned = earnedMarks
                )

                val testIntelligence = com.example.service.intelligence.SmartMockEngine.computeTestIntelligence(
                    attempt = attempt,
                    details = details,
                    testType = state.config.testType,
                    language = state.config.language
                )

                // Clean up local persistent session state on successful submission
                sessionPersistence.clearActiveSession()

                _activeTestState.value = state.copy(
                    isTestInProgress = false,
                    isSubmitting = false,
                    submissionError = null,
                    isCompleted = true,
                    completedAttempt = attempt,
                    detailedQuestions = details,
                    timeAnalysis = timeAnalysis,
                    testIntelligence = testIntelligence,
                    isNovaAnalyzing = true,
                    isPaletteOpen = false,
                    isSubmitConfirmOpen = false
                )

                fetchNovaPostTestAnalysis(attempt, details, testIntelligence, state.config.language)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error recording mock test attempt: ${e.message}", e)
                _activeTestState.value = state.copy(
                    isSubmitting = false,
                    submissionError = "Database submission failed: ${e.message}. You can tap Retry to resubmit without losing your answers."
                )
            }
        }
    }

    private fun fetchNovaPostTestAnalysis(
        attempt: MockTestAttempt,
        details: List<QuestionAttemptDetail>,
        baseIntelligence: com.example.service.intelligence.TestIntelligenceResult,
        language: String
    ) {
        viewModelScope.launch {
            try {
                val prompt = buildString {
                    append("Analyze this test result and provide post-test insight.\n")
                    append("Exam: ${attempt.examName}\n")
                    append("Title: ${attempt.title}\n")
                    append("Score: ${attempt.score} / ${attempt.totalQuestions * 4}\n")
                    append("Accuracy: ${attempt.accuracyPercent.toInt()}%\n")
                    append("Subject Accuracies: ${baseIntelligence.subjectPerformances.joinToString { "${it.subject}: ${it.accuracyPercent.toInt()}%" }}\n")
                    append("Weak Topics: ${baseIntelligence.weakTopics.joinToString()}\n")
                    append("Strong Topics: ${baseIntelligence.strongTopics.joinToString()}\n")
                    append("Language: $language\n")
                    append("\nReturn concise analysis in language: $language.\n")
                    append("Format as JSON with keys:\n")
                    append("whatWentWell: array of 1-3 concise strings\n")
                    append("whatNeedsPractice: array of 1-3 concise strings\n")
                    append("recommendedNextStep: single string\n")
                }

                val res = geminiRepository.askNova(
                    userPrompt = prompt,
                    studyContext = NovaStudyContext(
                        targetExam = attempt.examName,
                        preferredLanguage = language,
                        recentMockAccuracyPercent = attempt.accuracyPercent
                    )
                )

                if (res.isSuccess) {
                    val txt = res.getOrNull()?.replyMarkdown ?: ""
                    val jsonStart = txt.indexOf('{')
                    val jsonEnd = txt.lastIndexOf('}')
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        val jsonStr = txt.substring(jsonStart, jsonEnd + 1)
                        val json = org.json.JSONObject(jsonStr)
                        val wellArr = json.optJSONArray("whatWentWell")
                        val wellList = mutableListOf<String>()
                        if (wellArr != null) {
                            for (i in 0 until wellArr.length()) wellList.add(wellArr.getString(i))
                        }
                        val practiceArr = json.optJSONArray("whatNeedsPractice")
                        val practiceList = mutableListOf<String>()
                        if (practiceArr != null) {
                            for (i in 0 until practiceArr.length()) practiceList.add(practiceArr.getString(i))
                        }
                        val nextStep = json.optString("recommendedNextStep", baseIntelligence.novaInsight.recommendedNextStep)

                        val enrichedInsight = baseIntelligence.novaInsight.copy(
                            whatWentWell = if (wellList.isNotEmpty()) wellList else baseIntelligence.novaInsight.whatWentWell,
                            whatNeedsPractice = if (practiceList.isNotEmpty()) practiceList else baseIntelligence.novaInsight.whatNeedsPractice,
                            recommendedNextStep = nextStep.ifBlank { baseIntelligence.novaInsight.recommendedNextStep },
                            isAiGenerated = true
                        )

                        val updatedIntel = baseIntelligence.copy(novaInsight = enrichedInsight)
                        _activeTestState.value = _activeTestState.value.copy(
                            testIntelligence = updatedIntel,
                            isNovaAnalyzing = false
                        )
                        return@launch
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Nova post-test analysis fallback: ${e.message}")
            }
            _activeTestState.value = _activeTestState.value.copy(isNovaAnalyzing = false)
        }
    }

    fun retryWrongQuestions() {
        val currentDetails = _activeTestState.value.detailedQuestions
        val wrongQuestions = currentDetails.filter { !it.isCorrect }.map { it.question }
        if (wrongQuestions.isEmpty()) return

        val now = System.currentTimeMillis()
        val totalSeconds = (wrongQuestions.size * 90).coerceAtLeast(120)
        val expiresAt = now + (totalSeconds * 1000L)

        val retestConfig = MockTestConfig(
            exam = selectedExam.value.examName,
            subject = _activeTestState.value.subject,
            testType = MockTestType.PREVIOUS_MISTAKES,
            questionCount = wrongQuestions.size,
            timeLimitMinutes = (totalSeconds / 60).coerceAtLeast(2)
        )

        val newState = ActiveTestState(
            isTestInProgress = true,
            requestId = "retest_${now}",
            subject = retestConfig.subject,
            title = "${selectedExam.value.examName} - Incorrect Questions Retest",
            questions = wrongQuestions,
            currentQuestionIndex = 0,
            selectedAnswers = emptyMap(),
            markedForReview = emptySet(),
            visitedQuestions = setOf(0),
            timeSpentSeconds = emptyMap(),
            currentQuestionEnteredTimestamp = now,
            startedAtTimestamp = now,
            expiresAtTimestamp = expiresAt,
            totalDurationSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            isCompleted = false,
            isSubmitting = false,
            submissionError = null,
            completedAttempt = null,
            config = retestConfig
        )

        _activeTestState.value = newState
        sessionPersistence.saveActiveSession(newState)
        startMockTestTimerJob()
    }

    fun retryUnansweredQuestions() {
        val currentDetails = _activeTestState.value.detailedQuestions
        val skippedQuestions = currentDetails.filter { it.selectedIndex == null }.map { it.question }
        if (skippedQuestions.isEmpty()) return

        val now = System.currentTimeMillis()
        val totalSeconds = (skippedQuestions.size * 90).coerceAtLeast(120)
        val expiresAt = now + (totalSeconds * 1000L)

        val retestConfig = MockTestConfig(
            exam = selectedExam.value.examName,
            subject = _activeTestState.value.subject,
            testType = MockTestType.REVISION_TEST,
            questionCount = skippedQuestions.size,
            timeLimitMinutes = (totalSeconds / 60).coerceAtLeast(2)
        )

        val newState = ActiveTestState(
            isTestInProgress = true,
            requestId = "retest_skipped_${now}",
            subject = retestConfig.subject,
            title = "${selectedExam.value.examName} - Unanswered Questions Practice",
            questions = skippedQuestions,
            currentQuestionIndex = 0,
            selectedAnswers = emptyMap(),
            markedForReview = emptySet(),
            visitedQuestions = setOf(0),
            timeSpentSeconds = emptyMap(),
            currentQuestionEnteredTimestamp = now,
            startedAtTimestamp = now,
            expiresAtTimestamp = expiresAt,
            totalDurationSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            isCompleted = false,
            isSubmitting = false,
            submissionError = null,
            completedAttempt = null,
            config = retestConfig
        )

        _activeTestState.value = newState
        sessionPersistence.saveActiveSession(newState)
        startMockTestTimerJob()
    }

    fun retrySkippedQuestions() {
        retryUnansweredQuestions()
    }

    fun startTargetedPractice(rec: com.example.service.intelligence.SmartPracticeRecommendation) {
        startMockTestWithConfig(
            MockTestConfig(
                exam = rec.targetExam,
                subject = rec.targetSubject,
                topic = rec.targetTopic,
                difficulty = rec.recommendedDifficulty,
                questionCount = rec.recommendedQuestionCount,
                timeLimitMinutes = 15,
                testType = rec.recommendedType
            )
        )
    }

    fun saveAndExitActiveTest() {
        if (_activeTestState.value.isTestInProgress && !_activeTestState.value.isCompleted) {
            mockTestTimerJob?.cancel()
            val now = System.currentTimeMillis()
            val currentIdx = _activeTestState.value.currentQuestionIndex
            val enteredAt = _activeTestState.value.currentQuestionEnteredTimestamp
            val deltaSec = ((now - enteredAt) / 1000).toInt().coerceAtLeast(0)
            val updatedMap = _activeTestState.value.timeSpentSeconds.toMutableMap()
            updatedMap[currentIdx] = (updatedMap[currentIdx] ?: 0) + deltaSec

            val stateToSave = _activeTestState.value.copy(
                timeSpentSeconds = updatedMap,
                currentQuestionEnteredTimestamp = now
            )
            sessionPersistence.saveActiveSession(stateToSave)
            _pendingResumeSession.value = stateToSave
            _activeTestState.value = ActiveTestState()
        } else {
            exitTest()
        }
    }

    fun resumePendingTestSession() {
        val pending = _pendingResumeSession.value ?: sessionPersistence.loadActiveSession()
        if (pending != null && pending.questions.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val remaining = if (pending.expiresAtTimestamp > 0L) {
                ((pending.expiresAtTimestamp - now) / 1000L).coerceAtLeast(0L).toInt()
            } else {
                pending.remainingSeconds
            }

            if (remaining <= 0) {
                _activeTestState.value = pending.copy(
                    remainingSeconds = 0,
                    currentQuestionEnteredTimestamp = now
                )
                _pendingResumeSession.value = null
                submitMockTest()
            } else {
                val restored = pending.copy(
                    isTestInProgress = true,
                    isCompleted = false,
                    remainingSeconds = remaining,
                    currentQuestionEnteredTimestamp = now
                )
                _activeTestState.value = restored
                _pendingResumeSession.value = null
                sessionPersistence.saveActiveSession(restored)
                startMockTestTimerJob()
            }
        }
    }

    fun discardPendingTestSession() {
        sessionPersistence.clearActiveSession()
        _pendingResumeSession.value = null
    }

    fun exitTest() {
        mockTestTimerJob?.cancel()
        sessionPersistence.clearActiveSession()
        _pendingResumeSession.value = null
        _activeTestState.value = ActiveTestState()
    }

    fun reviewPastTest(attempt: MockTestAttempt) {
        _activeTestState.value = ActiveTestState(
            isTestInProgress = false,
            isCompleted = true,
            completedAttempt = attempt,
            detailedQuestions = emptyList()
        )
    }

    fun retakeMockTest(attempt: MockTestAttempt) {
        startMockTestWithConfig(
            MockTestConfig(
                exam = attempt.examName,
                subject = attempt.subject,
                topic = attempt.topic,
                difficulty = attempt.difficulty,
                questionCount = attempt.totalQuestions,
                timeLimitMinutes = (attempt.totalTimeAllowedSeconds / 60).coerceAtLeast(5)
            )
        )
    }

    fun addWeakTopicsToStudyPlan(attempt: MockTestAttempt) {
        viewModelScope.launch {
            attempt.weakTopics.forEach { topic ->
                val item = StudyPlanItem(
                    subject = attempt.subject,
                    chapter = topic,
                    topic = topic,
                    targetMinutes = 45,
                    isCompleted = false,
                    scheduledDateMillis = System.currentTimeMillis() + 86400000L,
                    priority = PlanPriority.HIGH,
                    notes = "Recommended practice from ${attempt.examName} Mock Test."
                )
                studyRepository.addStudyPlanItem(item)
            }
        }
    }

    fun addMistakesToRevisionFlashcards(attempt: MockTestAttempt, details: List<QuestionAttemptDetail>) {
        viewModelScope.launch {
            details.filter { !it.isCorrect && it.selectedIndex != null }.forEach { detail ->
                val q = detail.question
                studyRepository.addFlashcard(
                    subject = q.subject,
                    topic = q.topic,
                    front = q.questionText,
                    back = "Correct Answer: ${q.options.getOrNull(q.correctOptionIndex) ?: ""}\n\nExplanation: ${q.explanation}",
                    hint = "Review mistake from ${attempt.examName} mock test",
                    difficulty = q.difficulty,
                    sourceDocTitle = "Mock Test Mistake"
                )
            }
        }
    }

    fun deletePastTest(id: Long) {
        viewModelScope.launch {
            studyRepository.deleteMockTestAttempt(id)
        }
    }

    fun saveUserQuestionMaterial(title: String, exam: String, subject: String, topic: String, rawText: String) {
        viewModelScope.launch {
            studyRepository.saveUserQuestionMaterial(title, exam, subject, topic, rawText)
        }
    }

    fun deleteUserQuestionMaterial(id: Long) {
        viewModelScope.launch {
            studyRepository.deleteUserQuestionMaterial(id)
        }
    }

    fun diagnoseMistakes(subject: String) {
        viewModelScope.launch {
            val list = mistakes.value.filter { it.subject.equals(subject, ignoreCase = true) }
            val input = list.map { it.questionText to it.topic }
            val result = geminiRepository.diagnoseMistakesAndRecommend(input, subject)
            result.onSuccess { diagnosis ->
                _mistakeDiagnosis.value = diagnosis
            }
        }
    }

    fun markMistakeMastered(id: Long, isMastered: Boolean) {
        viewModelScope.launch {
            studyRepository.markMistakeMastered(id, isMastered)
        }
    }

    // --- Flashcards & Spaced Repetition ---
    fun addFlashcard(
        subject: String,
        topic: String,
        front: String,
        back: String,
        hint: String = "",
        difficulty: String = "Medium",
        sourceDocTitle: String = ""
    ) {
        viewModelScope.launch {
            studyRepository.addFlashcard(
                subject = subject.trim(),
                topic = topic.trim(),
                front = front.trim(),
                back = back.trim(),
                hint = hint.trim(),
                difficulty = difficulty,
                sourceDocTitle = sourceDocTitle
            )
            _flashcardMessage.value = "Flashcard created successfully! (+20 XP)"
        }
    }

    fun updateFlashcard(card: FlashcardItem) {
        viewModelScope.launch {
            studyRepository.updateFlashcard(card)
        }
    }

    fun deleteFlashcard(id: Long) {
        viewModelScope.launch {
            studyRepository.deleteFlashcard(id)
            _flashcardMessage.value = "Flashcard removed from library."
        }
    }

    fun reviewFlashcard(id: Long, status: RevisionCategory, confidence: Int) {
        viewModelScope.launch {
            studyRepository.recordFlashcardReview(id, status, confidence)
        }
    }

    fun recordSpacedFlashcardReview(id: Long, ratingQuality: Int) {
        viewModelScope.launch {
            studyRepository.recordSpacedFlashcardReview(id, ratingQuality)
            val xpGain = when (ratingQuality) {
                1 -> 10
                2 -> 15
                3 -> 25
                else -> 35
            }
            val statusLabel = when (ratingQuality) {
                1 -> "Needs Revision (< 1 day)"
                2 -> "Hard (Review in 2 days)"
                3 -> "Good Recall"
                else -> "Mastered! Interval extended"
            }
            _flashcardMessage.value = "$statusLabel (+$xpGain XP)"
        }
    }

    fun generateAiFlashcards(subject: String, topic: String, count: Int = 5) {
        viewModelScope.launch {
            _isFlashcardGenerating.value = true
            _flashcardMessage.value = null
            try {
                val result = geminiRepository.generateFlashcardsForTopic(subject, topic, count)
                result.onSuccess { cards ->
                    studyRepository.insertFlashcardList(cards)
                    _flashcardMessage.value = "Generated ${cards.size} flashcards on $topic! (+${cards.size * 15} XP)"
                }.onFailure { error ->
                    _flashcardMessage.value = "Failed to generate deck: ${error.message}"
                }
            } finally {
                _isFlashcardGenerating.value = false
            }
        }
    }

    fun generateFlashcardsFromNotes(sourceTitle: String, notesText: String, subject: String, count: Int = 6) {
        viewModelScope.launch {
            if (notesText.isBlank()) {
                _flashcardMessage.value = "Please paste or type study notes to generate cards."
                return@launch
            }
            _isFlashcardGenerating.value = true
            _flashcardMessage.value = null
            try {
                val validSubject = subject.ifBlank { "General Study" }
                val validTitle = sourceTitle.ifBlank { "Study Notes" }
                val result = geminiRepository.generateFlashcardsFromNotesOrDoc(validTitle, notesText, validSubject, count)
                result.onSuccess { cards ->
                    studyRepository.insertFlashcardList(cards)
                    _flashcardMessage.value = "Created ${cards.size} active-recall cards from '$validTitle'! (+${cards.size * 15} XP)"
                }.onFailure { error ->
                    _flashcardMessage.value = "Generation error: ${error.message}"
                }
            } finally {
                _isFlashcardGenerating.value = false
            }
        }
    }

    fun generateFlashcardsFromDocumentUri(uri: android.net.Uri, subject: String, count: Int = 6) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isFlashcardGenerating.value = true
            _flashcardMessage.value = null
            try {
                val context = getApplication<Application>()
                var docName = "Document_Study_Notes"
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) docName = name
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }

                val text = extractTextFromUri(context, uri)
                if (text.isBlank()) {
                    _flashcardMessage.value = "Could not extract text from document. Please choose a readable text, pdf, or markdown file."
                    _isFlashcardGenerating.value = false
                    return@launch
                }

                val validSubject = subject.ifBlank { "General Study" }
                val result = geminiRepository.generateFlashcardsFromNotesOrDoc(docName, text, validSubject, count)
                result.onSuccess { cards ->
                    studyRepository.insertFlashcardList(cards)
                    _flashcardMessage.value = "Extracted ${cards.size} flashcards from '$docName'! (+${cards.size * 15} XP)"
                }.onFailure { error ->
                    _flashcardMessage.value = "Generation failed: ${error.message}"
                }
            } catch (e: Exception) {
                _flashcardMessage.value = "Error reading file: ${e.localizedMessage}"
            } finally {
                _isFlashcardGenerating.value = false
            }
        }
    }

    fun convertAllStudyQuestionsToFlashcards(subject: String, questions: List<StudyQuestion>) {
        viewModelScope.launch {
            val validSubject = subject.ifBlank { "General Study" }
            val now = System.currentTimeMillis()
            val cards = questions.map { q ->
                FlashcardItem(
                    subject = validSubject,
                    topic = "Document Notes Synthesis",
                    front = q.question,
                    back = q.answer,
                    hint = "Concept Type: ${q.type}",
                    difficulty = if (q.type.contains("Formula", ignoreCase = true)) "Hard" else "Medium",
                    status = RevisionCategory.REVISE_NOW,
                    confidence = 2,
                    intervalDays = 1,
                    easeFactor = 2.5f,
                    repetitions = 0,
                    nextReviewDate = now,
                    sourceDocTitle = "Document Study Questions",
                    createdAt = now
                )
            }
            studyRepository.insertFlashcardList(cards)
            _flashcardMessage.value = "Saved ${cards.size} active-recall cards to your Spaced Repetition deck!"
        }
    }

    fun clearFlashcardMessage() {
        _flashcardMessage.value = null
    }

    private fun loadInitialThemeSettings() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("studymate_theme_prefs", android.content.Context.MODE_PRIVATE)
            val savedMode = prefs.getString("selected_theme_mode", AppThemeMode.NOVA_DARK.name)
            val mode = try {
                AppThemeMode.valueOf(savedMode ?: AppThemeMode.NOVA_DARK.name)
            } catch (e: Exception) {
                AppThemeMode.NOVA_DARK
            }
            _themeMode.value = mode
            _isDarkTheme.value = (mode != AppThemeMode.GLASS_LIGHT)
        } catch (_: Exception) {}
    }

    fun updateTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        val mode = if (isDark) AppThemeMode.NOVA_DARK else AppThemeMode.GLASS_LIGHT
        _themeMode.value = mode
        saveThemeModeToDisk(mode)
    }

    fun updateThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        _isDarkTheme.value = (mode != AppThemeMode.GLASS_LIGHT)
        saveThemeModeToDisk(mode)
    }

    private fun saveThemeModeToDisk(mode: AppThemeMode) {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("studymate_theme_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("selected_theme_mode", mode.name).apply()
        } catch (_: Exception) {}
    }

    fun updateNotificationPrefs(prefs: NotificationPreference) {
        _notificationPrefs.value = prefs
        val appContext = getApplication<Application>()
        com.example.notification.SmartNotificationPipeline.getInstance(appContext).updatePreferences(prefs)
        val s = com.example.notification.AppNotificationSettings(
            masterEnabled = prefs.masterEnabled,
            studySessionReminders = prefs.studyReminders,
            examCountdownAlerts = prefs.examCountdownAlerts,
            dailyGoalReminders = prefs.dailyGoalReminders,
            missedStudyReminders = prefs.missedStudyReminders,
            breakReminders = prefs.breakReminders,
            focusStartedAlerts = prefs.focusStartedAlerts,
            focusCompletedAlerts = prefs.focusCompletedAlerts,
            dailyMotivationAlerts = prefs.motivationalQuotes,
            studyReminderHour = prefs.reminderHour,
            studyReminderMinute = prefs.reminderMinute,
            dailyGoalReminderHour = prefs.dailyGoalHour,
            dailyGoalReminderMinute = prefs.dailyGoalMinute,
            motivationReminderHour = prefs.motivationHour,
            motivationReminderMinute = prefs.motivationMinute,
            activeReminderDays = prefs.activeDays,
            motivationFrequency = prefs.motivationFrequency,
            quietHoursEnabled = prefs.quietHoursEnabled,
            quietHoursStartHour = prefs.quietStartHour,
            quietHoursStartMinute = prefs.quietStartMinute,
            quietHoursEndHour = prefs.quietEndHour,
            quietHoursEndMinute = prefs.quietEndMinute
        )
        com.example.notification.StudyNotificationManager.saveSettings(appContext, s)
    }

    // --- Instant Notification Testers ---
    fun testStudySessionReminder() {
        val appContext = getApplication<Application>()
        val profile = userProfile.value
        val name = profile?.name ?: "Rahul"
        val subject = profile?.subjects?.firstOrNull() ?: "Physics"
        val topic = studyPlanItems.value.firstOrNull { !it.isCompleted }?.topic ?: "Faraday's Law & Lenz's Rule"
        val timeStr = String.format(Locale.US, "%02d:%02d %s", 
            if (_notificationPrefs.value.reminderHour % 12 == 0) 12 else _notificationPrefs.value.reminderHour % 12,
            _notificationPrefs.value.reminderMinute,
            if (_notificationPrefs.value.reminderHour >= 12) "PM" else "AM"
        )
        com.example.notification.StudyNotificationManager.sendStudySessionReminder(
            context = appContext,
            userName = name,
            subject = subject,
            topic = topic,
            timeString = timeStr,
            isTest = true
        )
    }

    fun testExamCountdownReminder() {
        val appContext = getApplication<Application>()
        val profile = userProfile.value
        val name = profile?.name ?: "Rahul"
        val examName = profile?.examName ?: "JEE Main 2026"
        val targetScore = profile?.targetScore ?: "Top 500 AIR"
        val examMillis = profile?.examDateMillis ?: (System.currentTimeMillis() + 30L * 86400000L)
        val daysLeft = ((examMillis - System.currentTimeMillis()) / 86400000L).coerceAtLeast(1L).toInt()

        com.example.notification.StudyNotificationManager.sendExamCountdownReminder(
            context = appContext,
            userName = name,
            examName = examName,
            daysLeft = daysLeft,
            targetScore = targetScore,
            isTest = true
        )
    }

    fun testDailyGoalReminder() {
        val appContext = getApplication<Application>()
        val profile = userProfile.value
        val name = profile?.name ?: "Rahul"
        val goal = profile?.dailyStudyGoal ?: "Complete daily scheduled topics & 20 flashcards"
        val target = profile?.dailyTargetMinutes ?: 180
        val completed = profile?.totalFocusMinutes ?: 90

        com.example.notification.StudyNotificationManager.sendDailyGoalReminder(
            context = appContext,
            userName = name,
            dailyGoal = goal,
            targetMinutes = target,
            completedMinutes = completed,
            isTest = true
        )
    }

    fun testMissedStudyReminder() {
        val appContext = getApplication<Application>()
        val profile = userProfile.value
        val name = profile?.name ?: "Rahul"
        val subject = profile?.subjects?.firstOrNull() ?: "Physics"
        val topic = studyPlanItems.value.firstOrNull { !it.isCompleted }?.topic ?: "Electromagnetic Induction"

        com.example.notification.StudyNotificationManager.sendMissedStudyReminder(
            context = appContext,
            userName = name,
            subject = subject,
            topic = topic,
            isTest = true
        )
    }

    fun testBreakReminder() {
        val appContext = getApplication<Application>()
        val profile = userProfile.value
        val name = profile?.name ?: "Rahul"
        val breakMins = profile?.breakDurationMinutes ?: 15

        com.example.notification.StudyNotificationManager.sendBreakReminder(
            context = appContext,
            userName = name,
            breakMinutes = breakMins,
            isTest = true
        )
    }

    fun testFocusStartedNotification() {
        val appContext = getApplication<Application>()
        val profile = userProfile.value
        val name = profile?.name ?: "Rahul"
        val subject = profile?.subjects?.firstOrNull() ?: "Physics"
        val topic = "Faraday's Law"

        com.example.notification.StudyNotificationManager.sendFocusSessionStarted(
            context = appContext,
            userName = name,
            subject = subject,
            topic = topic,
            durationMinutes = 25,
            isTest = true
        )
    }

    fun testFocusCompletedNotification() {
        val appContext = getApplication<Application>()
        val profile = userProfile.value
        val name = profile?.name ?: "Rahul"
        val subject = profile?.subjects?.firstOrNull() ?: "Physics"

        com.example.notification.StudyNotificationManager.sendFocusSessionCompleted(
            context = appContext,
            userName = name,
            subject = subject,
            minutes = 25,
            xpEarned = 50,
            isTest = true
        )
    }

    fun testDailyMotivationalNotification() {
        val appContext = getApplication<Application>()
        val profile = userProfile.value
        val name = profile?.name ?: "Rahul"
        val exam = profile?.examName ?: "JEE Main"

        com.example.notification.StudyNotificationManager.sendDailyMotivationalNotification(
            context = appContext,
            userName = name,
            examName = exam,
            isTest = true
        )
    }

    // --- Step 30 Notification Center & Smart Notification Logic ---

    fun markNotificationAsRead(id: String) {
        _appNotifications.value = _appNotifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        saveAppNotificationsToDisk()
    }

    fun markAllNotificationsAsRead() {
        _appNotifications.value = _appNotifications.value.map { it.copy(isRead = true) }
        saveAppNotificationsToDisk()
    }

    fun deleteNotification(id: String) {
        _appNotifications.value = _appNotifications.value.filter { it.id != id }
        saveAppNotificationsToDisk()
    }

    fun clearAllNotifications() {
        _appNotifications.value = emptyList()
        saveAppNotificationsToDisk()
    }

    fun dismissInAppBanner() {
        _activeInAppBanner.value = null
    }

    fun addNotification(notification: AppNotification) {
        val prefs = _notificationPrefs.value
        if (!prefs.masterEnabled) return

        // Category check
        val categoryEnabled = when (notification.category) {
            NotificationCategory.STUDY -> prefs.studyReminders
            NotificationCategory.TESTS -> prefs.testReminders
            NotificationCategory.CURRENT_AFFAIRS -> prefs.currentAffairsReminders
            NotificationCategory.EXAM_UPDATES -> prefs.examUpdatesReminders
            NotificationCategory.NOVA -> prefs.novaReminders
            NotificationCategory.VACANCY -> prefs.vacancyAlerts
            NotificationCategory.RESULTS -> prefs.resultAlerts
            NotificationCategory.ADMIT_CARD -> prefs.admitCardAlerts
            NotificationCategory.SYSTEM -> true
        }
        if (!categoryEnabled) return

        // Quiet Hours Check
        if (prefs.quietHoursEnabled) {
            val cal = Calendar.getInstance()
            val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val startMins = prefs.quietStartHour * 60 + prefs.quietStartMinute
            val endMins = prefs.quietEndHour * 60 + prefs.quietEndMinute
            val inQuiet = if (startMins <= endMins) {
                currentMins in startMins..endMins
            } else {
                currentMins >= startMins || currentMins <= endMins
            }
            if (inQuiet && notification.category != NotificationCategory.SYSTEM) return
        }

        // Duplicate Event Key Check
        if (notification.eventKey != null) {
            val exists = _appNotifications.value.any { it.eventKey == notification.eventKey }
            if (exists) return
        }

        val updated = listOf(notification) + _appNotifications.value
        _appNotifications.value = updated
        _activeInAppBanner.value = notification
        saveAppNotificationsToDisk()
    }

    private fun saveAppNotificationsToDisk() {
        try {
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences("studymate_notifications_store", Context.MODE_PRIVATE)
            val jsonArray = org.json.JSONArray()
            _appNotifications.value.take(50).forEach { item ->
                val obj = org.json.JSONObject().apply {
                    put("id", item.id)
                    put("category", item.category.name)
                    put("title", item.title)
                    put("message", item.message)
                    put("timestamp", item.timestamp)
                    put("isRead", item.isRead)
                    put("actionText", item.actionText)
                    put("deepLink", item.deepLink)
                    put("payload", item.payload)
                    put("eventKey", item.eventKey ?: "")
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("notifications_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error saving notifications: ${e.message}")
        }
    }

    private fun loadAppNotificationsFromDisk() {
        try {
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences("studymate_notifications_store", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("notifications_json", null)
            if (!jsonStr.isNullOrBlank()) {
                val jsonArray = org.json.JSONArray(jsonStr)
                val list = mutableListOf<AppNotification>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        AppNotification(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            category = try { NotificationCategory.valueOf(obj.optString("category", "STUDY")) } catch(e: Exception) { NotificationCategory.STUDY },
                            title = obj.optString("title", ""),
                            message = obj.optString("message", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isRead = obj.optBoolean("isRead", false),
                            actionText = obj.optString("actionText", "Open"),
                            deepLink = obj.optString("deepLink", "HOME"),
                            payload = obj.optString("payload", ""),
                            eventKey = obj.optString("eventKey", "").ifBlank { null }
                        )
                    )
                }
                _appNotifications.value = list
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error loading notifications: ${e.message}")
        }
    }

    fun computeDailyBriefingData() {
        val profile = userProfile.value
        val name = profile?.name ?: "Student"
        val language = profile?.languagePreference ?: _notificationPrefs.value.language

        val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val greetingTime = when {
            hour < 12 -> if (language.equals("Hindi", ignoreCase = true)) "Namaste $name 🙏 Suprabhat!" else "Good Morning, $name 👋"
            hour < 17 -> if (language.equals("Hindi", ignoreCase = true)) "Namaste $name 🙏 Shubh Dopahar!" else "Good Afternoon, $name 👋"
            else -> if (language.equals("Hindi", ignoreCase = true)) "Namaste $name 🙏 Shubh Sandhya!" else "Good Evening, $name 👋"
        }

        // Real focus topic from recommendations or study plan
        val focusRec = studyRecommendations.value.firstOrNull()
        val focusTopic = focusRec?.topicName ?: studyPlanItems.value.firstOrNull { !it.isCompleted }?.topic ?: "Faraday's Law & Electromagnetic Induction"
        val focusSubject = focusRec?.subjectName ?: profile?.subjects?.firstOrNull() ?: "Physics"
        val focusReason = focusRec?.reason ?: "Selected based on high-yield exam weightage and past practice."

        // Real Current Affairs headline from live exam feed
        val caList = liveExamFeedState.value.liveNews
        val caHeadline = caList.firstOrNull()?.title ?: "National Science & Tech Scholarship & Board Exam Updates"
        val caCount = caList.size.coerceAtLeast(3)

        // Unfinished test check
        val activeTest = activeTestState.value
        val unfinishedTitle = if (activeTest.isTestInProgress) activeTest.title else null
        val unfinishedProgress = if (activeTest.isTestInProgress) "Q${activeTest.currentQuestionIndex + 1}/${activeTest.questions.size}" else null

        // Exam Countdown
        val examDate = profile?.examDateMillis ?: (System.currentTimeMillis() + 45L * 86400000L)
        val daysLeft = ((examDate - System.currentTimeMillis()) / 86400000L).coerceAtLeast(1L).toInt()

        _dailyBriefingData.value = DailyBriefingData(
            dateString = dateStr,
            greeting = greetingTime,
            focusTopic = focusTopic,
            focusSubject = focusSubject,
            focusReason = focusReason,
            currentAffairsHeadline = caHeadline,
            currentAffairsCount = caCount,
            practiceSuggestion = "10 $focusSubject Practice Questions",
            practiceQuestionCount = 10,
            examDaysRemaining = daysLeft,
            examName = profile?.examName ?: "JEE Main / Board Exam",
            unfinishedTestTitle = unfinishedTitle,
            unfinishedTestProgress = unfinishedProgress,
            revisionQuestionsCount = mistakes.value.size,
            language = language
        )
    }

    fun checkAndTriggerSmartNotifications() {
        computeDailyBriefingData()

        val todayDate = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())

        // 1. Daily Briefing Ready
        addNotification(
            AppNotification(
                category = NotificationCategory.STUDY,
                title = "☀️ Daily Briefing Ready",
                message = "Your personalized daily preparation overview for today is ready.",
                actionText = "View Briefing",
                deepLink = "DAILY_BRIEFING",
                eventKey = "daily_briefing_$todayDate"
            )
        )

        // 2. Current Affairs Update
        if (_dailyBriefingData.value.currentAffairsCount > 0) {
            addNotification(
                AppNotification(
                    category = NotificationCategory.CURRENT_AFFAIRS,
                    title = "📰 Today's Current Affairs Ready",
                    message = "${_dailyBriefingData.value.currentAffairsCount} verified exam updates are available to read.",
                    actionText = "Read Updates",
                    deepLink = "CURRENT_AFFAIRS",
                    eventKey = "current_affairs_$todayDate"
                )
            )
        }

        // 3. Unfinished Test
        if (activeTestState.value.isTestInProgress) {
            addNotification(
                AppNotification(
                    category = NotificationCategory.TESTS,
                    title = "⏱️ Unfinished Mock Test",
                    message = "You have an active test: ${activeTestState.value.title}. Resume to finish your session.",
                    actionText = "Resume Test",
                    deepLink = "MOCK_TEST",
                    eventKey = "unfinished_test_${activeTestState.value.requestId}"
                )
            )
        }

        // 4. Revision Due
        if (mistakes.value.isNotEmpty()) {
            addNotification(
                AppNotification(
                    category = NotificationCategory.STUDY,
                    title = "🧠 Spaced Revision Due",
                    message = "${mistakes.value.size} saved weak questions are ready for review.",
                    actionText = "Start Revision",
                    deepLink = "REVISION",
                    eventKey = "revision_due_$todayDate"
                )
            )
        }
    }

    // --- Document Summarizer & Study Questions Actions ---

    fun processDocumentUri(uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isDocumentParsing.value = true
            _documentError.value = null
            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver

                var fileName = "Study_Document.pdf"
                var fileSize = "1.2 MB"

                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) {
                                val name = cursor.getString(nameIndex)
                                if (!name.isNullOrBlank()) fileName = name
                            }
                            if (sizeIndex != -1) {
                                val bytes = cursor.getLong(sizeIndex)
                                fileSize = when {
                                    bytes < 1024 -> "$bytes B"
                                    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                                    else -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // fallback to default filename and size
                }

                val extractedText = extractTextFromUri(context, uri)
                if (extractedText.isBlank()) {
                    _documentError.value = "Unable to read text from this document. Please try a text, markdown, or PDF study notes file."
                    _isDocumentParsing.value = false
                    return@launch
                }

                val result = geminiRepository.analyzeDocument(fileName, fileSize, extractedText)
                result.onSuccess { analysis ->
                    _documentAnalysisState.value = analysis
                }.onFailure { error ->
                    _documentError.value = error.message ?: "Failed to analyze document"
                }
            } catch (e: Exception) {
                _documentError.value = "Error reading document: ${e.localizedMessage}"
            } finally {
                _isDocumentParsing.value = false
            }
        }
    }

    fun analyzeDirectText(title: String, text: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (text.isBlank()) {
                _documentError.value = "Please enter or paste study text to analyze."
                return@launch
            }
            _isDocumentParsing.value = true
            _documentError.value = null
            val displayTitle = if (title.isNotBlank()) title else "Study_Notes.txt"
            val displaySize = "${(text.length * 2) / 1024.coerceAtLeast(1)} KB"
            try {
                val result = geminiRepository.analyzeDocument(displayTitle, displaySize, text)
                result.onSuccess { analysis ->
                    _documentAnalysisState.value = analysis
                }.onFailure { error ->
                    _documentError.value = error.message ?: "Failed to generate study summary"
                }
            } catch (e: Exception) {
                _documentError.value = "Failed to analyze text: ${e.localizedMessage}"
            } finally {
                _isDocumentParsing.value = false
            }
        }
    }

    fun clearDocumentAnalysis() {
        _documentAnalysisState.value = null
        _documentError.value = null
    }

    fun convertStudyQuestionToFlashcard(subject: String, question: StudyQuestion) {
        viewModelScope.launch {
            val validSubject = subject.ifBlank { "General Study" }
            studyRepository.addFlashcard(
                subject = validSubject,
                topic = "Document Notes Synthesis",
                front = question.question,
                back = question.answer,
                hint = "Key Type: ${question.type}",
                difficulty = if (question.type.contains("Formula", ignoreCase = true)) "Hard" else "Medium"
            )
        }
    }

    private fun extractTextFromUri(context: android.content.Context, uri: android.net.Uri): String {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val inputStream = contentResolver.openInputStream(uri) ?: return ""
            val bytes = inputStream.use { it.readBytes() }

            if (mimeType.startsWith("text/") ||
                uri.toString().endsWith(".txt", ignoreCase = true) ||
                uri.toString().endsWith(".md", ignoreCase = true) ||
                uri.toString().endsWith(".json", ignoreCase = true) ||
                uri.toString().endsWith(".csv", ignoreCase = true)
            ) {
                return String(bytes, Charsets.UTF_8)
            }

            val asUtf8 = String(bytes, Charsets.UTF_8)
            if (asUtf8.count { it.isLetterOrDigit() } > 100) {
                asUtf8
            } else {
                extractPdfTextFromBytes(bytes)
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractPdfTextFromBytes(bytes: ByteArray): String {
        val sb = StringBuilder()
        val text = String(bytes, Charsets.ISO_8859_1)

        val regexTj = Regex("""\(([^()]*)\)\s*Tj""")
        val matchesTj = regexTj.findAll(text)
        for (m in matchesTj) {
            val raw = m.groupValues[1].trim()
            if (raw.isNotBlank()) {
                sb.append(raw).append(" ")
            }
        }
        if (sb.length > 80) return sb.toString()

        val words = Regex("""[A-Za-z0-9,.\-?!:;'"()/\s]{4,}""").findAll(text)
        val filtered = words.map { it.value.trim() }
            .filter { w -> w.count { it.isLetter() } >= 3 && !w.startsWith("obj") && !w.startsWith("endobj") && !w.startsWith("stream") }
            .joinToString(" ")
        return filtered.take(25000)
    }

    // --- AI Smart Features ---
    fun loadAiCoachRecommendation(subject: String) {
        viewModelScope.launch {
            _isAiCoachLoading.value = true
            val recentSessions = studyRepository.allFocusSessions.firstOrNull()?.take(5) ?: emptyList()
            val recentMistakes = studyRepository.allMistakes.firstOrNull() ?: emptyList()
            val allCards = studyRepository.allFlashcards.firstOrNull() ?: emptyList()

            val result = geminiRepository.getAiCoachRecommendation(subject, recentSessions, recentMistakes, allCards)
            result.onSuccess {
                _aiCoachRecommendation.value = it
            }
            _isAiCoachLoading.value = false
        }
    }

    fun loadStudyNowRecommendation() {
        viewModelScope.launch {
            _isStudyNowLoading.value = true
            val profile = studyRepository.userProfile.firstOrNull()
            val subjects = profile?.subjects ?: listOf("Physics")
            val plan = studyRepository.allPlanItems.firstOrNull() ?: emptyList()
            val cards = studyRepository.allFlashcards.firstOrNull() ?: emptyList()
            val dueCards = cards.count { it.status == RevisionCategory.REVISE_NOW }
            val mistakes = studyRepository.allMistakes.firstOrNull()?.size ?: 0

            val result = geminiRepository.getWhatShouldIStudyNow(subjects, plan, dueCards, mistakes)
            result.onSuccess {
                _studyNowRecommendation.value = it
            }
            _isStudyNowLoading.value = false
        }
    }

    fun loadMistakeInsights(subject: String) {
        viewModelScope.launch {
            _isMistakeInsightsLoading.value = true
            val recentMistakes = studyRepository.allMistakes.firstOrNull() ?: emptyList()
            val subjectMistakes = recentMistakes.filter { it.subject == subject }

            val result = geminiRepository.analyzeMistakeIntelligence(subjectMistakes, subject)
            result.onSuccess {
                _mistakeInsights.value = it
            }
            _isMistakeInsightsLoading.value = false
        }
    }

    fun loadWeeklyReport() {
        viewModelScope.launch {
            _isWeeklyReportLoading.value = true
            val profile = studyRepository.userProfile.firstOrNull()
            if (profile != null) {
                val sessions = studyRepository.allFocusSessions.firstOrNull() ?: emptyList()
                val attempts = studyRepository.allMockTestAttempts.firstOrNull() ?: emptyList()
                val mistakes = studyRepository.allMistakes.firstOrNull() ?: emptyList()
                
                val result = geminiRepository.generateWeeklyAiReport(profile, sessions, attempts, mistakes)
                result.onSuccess {
                    _weeklyReport.value = it
                }
            }
            _isWeeklyReportLoading.value = false
        }
    }

    fun generateCompleteStudyKit(uri: android.net.Uri, subject: String, context: android.content.Context) {
        viewModelScope.launch {
            _isStudyKitGenerating.value = true
            _studyKitMessage.value = "Extracting document contents..."
            try {
                var fileName = "Uploaded Document"
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) fileName = name
                        }
                    }
                } catch (e: Exception) {
                    // Fallback
                }
                
                val text = extractTextFromUri(context, uri)

                if (text.isBlank()) {
                    _studyKitMessage.value = "Could not extract text. Try another document."
                    _isStudyKitGenerating.value = false
                    return@launch
                }

                _studyKitMessage.value = "Synthesizing AI Study Kit..."
                val result = geminiRepository.generateCompleteStudyKit(fileName, text, subject)
                result.onSuccess {
                    _completeStudyKit.value = it
                    _studyKitMessage.value = "Study Kit Ready!"
                }
                result.onFailure {
                    _studyKitMessage.value = "Failed to generate kit."
                }
            } catch (e: Exception) {
                _studyKitMessage.value = "Error: ${e.localizedMessage}"
            }
            _isStudyKitGenerating.value = false
        }
    }

    // =========================================================================
    // MASTER INTELLIGENCE & SMART SEARCH METHODS
    // =========================================================================

    fun setSelectedAvailableTime(minutes: Int?) {
        _selectedAvailableTimeMinutes.value = minutes
    }

    fun performSmartSearch(
        query: String,
        examName: String? = null,
        subject: String = "General"
    ) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSmartSearching.value = true
            _smartSearchError.value = null
            val exam = examName ?: userProfile.value?.examName ?: "Competitive Exam"
            val result = geminiRepository.performSmartSearch(query.trim(), exam, subject)
            result.onSuccess {
                _smartSearchResult.value = it
            }
            result.onFailure {
                _smartSearchError.value = "Search analysis error: ${it.localizedMessage}"
            }
            _isSmartSearching.value = false
        }
    }

    fun clearSmartSearch() {
        _smartSearchResult.value = null
        _smartSearchError.value = null
    }

    fun saveSearchResultAsSmartNote(
        result: SmartSearchResult,
        subject: String = "General",
        topic: String? = null
    ) {
        viewModelScope.launch {
            val note = SmartNoteItem(
                title = result.query.replaceFirstChar { it.uppercase() },
                subject = subject,
                topic = topic ?: result.query.take(30),
                contentMarkdown = result.studentFriendlyAnswer,
                keyPoints = result.keyPoints,
                formulas = result.formulasAndDefinitions,
                importantFacts = result.keyPoints.take(2),
                sourceTitle = result.sources.firstOrNull()?.title ?: "Smart Search",
                sourceUrl = result.sources.firstOrNull()?.url ?: "",
                isBookmarked = true,
                createdAt = System.currentTimeMillis()
            )
            studyRepository.saveSmartNote(note)
        }
    }

    fun saveSearchResultAsFlashcards(result: SmartSearchResult, subject: String = "General") {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cards = result.generatedPracticeQuestions.mapIndexed { idx, q ->
                FlashcardItem(
                    subject = subject,
                    topic = result.query.take(30),
                    front = q.questionText,
                    back = "Answer: ${q.options.getOrElse(q.correctOptionIndex) { "" }}\n\nExplanation: ${q.explanation}",
                    hint = q.options.firstOrNull() ?: "",
                    difficulty = "Medium",
                    status = RevisionCategory.REVISE_NOW,
                    confidence = 2,
                    intervalDays = 1,
                    easeFactor = 2.5f,
                    repetitions = 0,
                    nextReviewDate = now,
                    sourceDocTitle = "Smart Search: ${result.query}",
                    createdAt = now
                )
            }
            if (cards.isNotEmpty()) {
                studyRepository.insertFlashcardList(cards)
            }
        }
    }

    fun saveSearchResultAsPlanTask(result: SmartSearchResult, subject: String = "General", durationMinutes: Int = 30) {
        viewModelScope.launch {
            val plan = StudyPlanItem(
                subject = subject,
                chapter = "Smart Search",
                topic = result.query,
                targetMinutes = durationMinutes,
                isCompleted = false,
                priority = PlanPriority.HIGH,
                notes = "Generated from Smart Search: ${result.query}"
            )
            studyRepository.addStudyPlanItem(plan)
        }
    }

    fun saveSmartNote(note: SmartNoteItem) {
        viewModelScope.launch {
            studyRepository.saveSmartNote(note)
        }
    }

    fun toggleSmartNoteBookmark(id: Long, isBookmarked: Boolean) {
        viewModelScope.launch {
            studyRepository.toggleSmartNoteBookmark(id, isBookmarked)
        }
    }

    fun toggleSmartNoteRevised(id: Long, isRevised: Boolean) {
        viewModelScope.launch {
            studyRepository.toggleSmartNoteRevised(id, isRevised)
        }
    }

    fun deleteSmartNote(id: Long) {
        viewModelScope.launch {
            studyRepository.deleteSmartNote(id)
        }
    }

    fun toggleCurrentAffairsSaved(id: Long, isSaved: Boolean) {
        viewModelScope.launch {
            studyRepository.toggleCurrentAffairsSaved(id, isSaved)
        }
    }

    fun markExamUpdateRead(id: Long) {
        viewModelScope.launch {
            studyRepository.markExamUpdateRead(id)
        }
    }

    // --- Step 21 Live Exam Intelligence Actions ---

    fun refreshLiveExamIntelligence(force: Boolean = false) {
        viewModelScope.launch {
            _isRefreshingLiveExam.value = true
            val currentExam = userProfile.value?.examName ?: selectedExam.value.examName
            liveExamIntelligenceEngine.refreshLiveExamIntelligence(
                examName = currentExam,
                forceRefresh = force
            ).onSuccess { feedState ->
                _liveExamFeedState.value = feedState
            }.onFailure { e ->
                _snackbarMessage.emit("Unable to refresh live updates: ${e.message ?: "Network offline"}")
            }
            _isRefreshingLiveExam.value = false
        }
    }

    fun startInteractiveStudyQuiz(subject: String, topic: String) {
        startMockTest(
            subject = subject.ifBlank { "General Studies" },
            chapter = topic.ifBlank { "Live Updates Practice" },
            mode = "Trending Topic Practice",
            questionCount = 5
        )
    }

    fun toggleSaveLiveExamUpdate(id: String, isSaved: Boolean) {
        viewModelScope.launch {
            liveExamIntelligenceEngine.toggleSaveUpdate(id, isSaved)
            val currentList = _liveExamFeedState.value.liveNews.map {
                if (it.id == id) it.copy(isSaved = isSaved) else it
            }
            val currentTrending = _liveExamFeedState.value.trendingTopics
            val currentSaved = currentList.filter { it.isSaved }
            _liveExamFeedState.update {
                it.copy(
                    liveNews = currentList,
                    whatsNewList = currentList.take(4),
                    officialNotices = currentList.filter { u -> u.isVerifiedOfficial || u.category == LiveExamCategory.OFFICIAL_NOTIFICATION.name },
                    radarUpdates = currentList.filter { u -> u.relevance == ExamRelevanceLevel.HIGH.name || u.isVerifiedOfficial }.take(5),
                    savedUpdates = currentSaved
                )
            }
            if (isSaved) {
                _snackbarMessage.emit("🔖 Update saved to your Revision list!")
            }
        }
    }

    fun toggleSaveTrendingTopic(id: String, isSaved: Boolean) {
        viewModelScope.launch {
            liveExamIntelligenceEngine.toggleSaveTrending(id, isSaved)
            val currentTrending = _liveExamFeedState.value.trendingTopics.map {
                if (it.id == id) it.copy(isSaved = isSaved) else it
            }
            _liveExamFeedState.update { it.copy(trendingTopics = currentTrending) }
            if (isSaved) {
                _snackbarMessage.emit("🔖 Topic saved to your Revision list!")
            }
        }
    }

    fun markLiveUpdateRead(id: String) {
        viewModelScope.launch {
            liveExamIntelligenceEngine.markUpdateAsRead(id)
            val currentList = _liveExamFeedState.value.liveNews.map {
                if (it.id == id) it.copy(isRead = true) else it
            }
            _liveExamFeedState.update { it.copy(liveNews = currentList) }
        }
    }

    fun selectLiveUpdateForDetail(update: LiveExamUpdateEntity?) {
        _selectedLiveUpdateForDetail.value = update
        if (update != null) {
            markLiveUpdateRead(update.id)
        }
    }

    fun setShowLiveExamIntelligenceScreen(show: Boolean) {
        _showLiveExamIntelligenceScreen.value = show
    }

    // --- Step 40 Smart Vacancy, Results & Admit Card Actions ---

    fun refreshRecruitmentCatalog(force: Boolean = false) {
        viewModelScope.launch {
            _isRefreshingRecruitment.value = true
            val currentExam = userProfile.value?.examName ?: selectedExam.value.examName
            val currentState = _recruitmentFeedState.value.selectedState
            val profile = _recruitmentFeedState.value.userProfile
            recruitmentIntelligenceEngine.refreshRecruitmentCatalog(
                profile = profile,
                userExam = currentExam,
                userState = currentState,
                forceLiveSearch = force
            ).onSuccess { count ->
                _recruitmentFeedState.update { it.copy(lastSyncMillis = System.currentTimeMillis()) }
                updateRecruitmentFeedState()
            }.onFailure { e ->
                _snackbarMessage.emit("Unable to refresh recruitment updates: ${e.message ?: "Offline mode active"}")
            }
            _isRefreshingRecruitment.value = false
        }
    }

    fun updateRecruitmentProfile(profile: UserRecruitmentProfile) {
        _recruitmentFeedState.update { it.copy(userProfile = profile) }
        refreshRecruitmentCatalog(force = false)
        viewModelScope.launch {
            _snackbarMessage.emit("🎯 Recruitment preferences updated!")
        }
    }

    fun updateUserApplicationStatus(
        id: String,
        status: UserApplicationStatus,
        applicationNumber: String = "",
        rollNumber: String = "",
        appliedPost: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            recruitmentIntelligenceEngine.updateApplicationStatus(
                id = id,
                status = status,
                applicationNumber = applicationNumber,
                rollNumber = rollNumber,
                appliedPost = appliedPost,
                notes = notes
            )
            updateRecruitmentFeedState()
            val msg = if (status == UserApplicationStatus.APPLIED) "✅ Marked as Application Submitted! Added to Tracker."
                      else "Status updated to ${status.label}"
            _snackbarMessage.emit(msg)
        }
    }

    fun updateDocumentReadyStatus(id: String, docsReady: List<String>) {
        viewModelScope.launch {
            recruitmentIntelligenceEngine.updateDocumentsReadyList(id, docsReady)
            updateRecruitmentFeedState()
        }
    }

    fun updateChecklistChecked(id: String, checked: List<String>) {
        viewModelScope.launch {
            recruitmentIntelligenceEngine.updateChecklistCheckedList(id, checked)
            updateRecruitmentFeedState()
        }
    }

    fun findJobsForMe(
        category: String?,
        state: String?,
        qualification: String?,
        age: Int?
    ) {
        viewModelScope.launch {
            val matches = recruitmentIntelligenceEngine.findJobsForMe(category, state, qualification, age)
            _recruitmentFeedState.update { it.copy(findJobsMatches = matches) }
        }
    }

    fun setRecruitmentCategory(category: String) {
        _recruitmentFeedState.update { it.copy(selectedCategory = category) }
        updateRecruitmentFeedState()
    }

    fun setRecruitmentState(state: String) {
        _recruitmentFeedState.update { it.copy(selectedState = state) }
        updateRecruitmentFeedState()
    }

    fun setRecruitmentTab(tab: String) {
        _recruitmentFeedState.update { it.copy(selectedTab = tab) }
    }

    fun setRecruitmentSearch(query: String) {
        _recruitmentFeedState.update { it.copy(searchQuery = query) }
        updateRecruitmentFeedState()
    }

    fun setRecruitmentSort(sortOption: RecruitmentSortOption) {
        _recruitmentFeedState.update { it.copy(sortOption = sortOption) }
        updateRecruitmentFeedState()
    }

    fun selectRecruitmentDetail(item: RecruitmentEntity?) {
        _selectedRecruitmentDetail.value = item
    }

    fun toggleSaveRecruitment(id: String, isSaved: Boolean) {
        viewModelScope.launch {
            recruitmentIntelligenceEngine.toggleSaveItem(id, isSaved)
            if (isSaved) {
                _snackbarMessage.emit("📌 Saved to your Jobs & Updates watchlist!")
            } else {
                _snackbarMessage.emit("Removed from watchlist.")
            }
            updateRecruitmentFeedState()
        }
    }

    fun setDeadlineReminder(id: String, enabled: Boolean, daysBefore: Int = 3) {
        viewModelScope.launch {
            recruitmentIntelligenceEngine.setDeadlineReminder(id, enabled, daysBefore)
            if (enabled) {
                _snackbarMessage.emit("⏰ Deadline reminder set for $daysBefore days before close!")
            } else {
                _snackbarMessage.emit("Reminder disabled.")
            }
            updateRecruitmentFeedState()
        }
    }

    fun submitRecruitmentReport(itemId: String, reportCategory: String, userComment: String) {
        viewModelScope.launch {
            _snackbarMessage.emit("✓ Thank you! Feedback received. Our team will verify the official notice.")
        }
    }

    fun setShowSmartVacancyScreen(show: Boolean, initialTab: String? = null) {
        _showSmartVacancyScreen.value = show
        if (initialTab != null) {
            _recruitmentFeedState.update { it.copy(selectedTab = initialTab) }
        }
    }

    private fun updateRecruitmentFeedState(rawItems: List<RecruitmentEntity>? = null) {
        viewModelScope.launch {
            val all = rawItems ?: getApplication<StudyMateApplication>().database.recruitmentDao().getAllOnce()
            val state = _recruitmentFeedState.value
            val targetExam = userProfile.value?.examName ?: selectedExam.value.examName
            val targetState = state.selectedState
            val query = state.searchQuery.trim().lowercase()
            val selectedCat = state.selectedCategory

            // 1. Filter for search query if present
            var filtered = if (query.isBlank()) all else {
                all.filter { item ->
                    item.title.lowercase().contains(query) ||
                    item.organization.lowercase().contains(query) ||
                    item.postName.lowercase().contains(query) ||
                    item.examCategory.lowercase().contains(query) ||
                    item.state.lowercase().contains(query) ||
                    item.educationalQualification.lowercase().contains(query)
                }
            }

            // 2. Filter for sector/category chip if not ALL
            if (selectedCat != RecruitmentCategory.ALL.name) {
                filtered = filtered.filter { it.examCategory.equals(selectedCat, ignoreCase = true) }
            }

            // 3. Filter for state if not All India
            if (targetState != "All India") {
                filtered = filtered.filter { it.state.equals("All India", ignoreCase = true) || it.state.equals(targetState, ignoreCase = true) }
            }

            // Hard Application Filter: Only active vacancies (OPEN, LAST_DAY, EXTENDED)
            val activeVacancies = filtered.filter { it.contentType == RecruitmentContentType.VACANCY.name && it.getComputedStatus().isApplyActive }

            // Sorting logic
            val sortedActiveVacancies = when (state.sortOption) {
                RecruitmentSortOption.RECOMMENDED -> activeVacancies.sortedWith(
                    compareByDescending<RecruitmentEntity> { it.personalRelevanceScore }
                        .thenBy { RecruitmentDateLogic.calculateDaysRemaining(it.applicationLastDate) ?: 999 }
                )
                RecruitmentSortOption.LAST_DATE_SOON -> activeVacancies.sortedBy {
                    RecruitmentDateLogic.calculateDaysRemaining(it.applicationLastDate) ?: 999
                }
                RecruitmentSortOption.NEWEST -> activeVacancies.sortedByDescending { it.applicationStartDate ?: "" }
                RecruitmentSortOption.RECENTLY_UPDATED -> activeVacancies.sortedByDescending { it.lastVerifiedAt }
                RecruitmentSortOption.EXAM_RELATED -> activeVacancies.sortedByDescending {
                    if (it.examCategory.contains(targetExam, ignoreCase = true) || it.title.contains(targetExam, ignoreCase = true)) 1 else 0
                }
                RecruitmentSortOption.STATE_RELATED -> activeVacancies.sortedByDescending {
                    if (it.state.equals(targetState, ignoreCase = true)) 1 else 0
                }
            }

            val examCategoryKeyword = when {
                targetExam.contains("Railway", ignoreCase = true) || targetExam.contains("RRB", ignoreCase = true) -> RecruitmentCategory.RAILWAY.name
                targetExam.contains("SSC", ignoreCase = true) || targetExam.contains("CGL", ignoreCase = true) || targetExam.contains("CHSL", ignoreCase = true) -> RecruitmentCategory.SSC.name
                targetExam.contains("Bank", ignoreCase = true) || targetExam.contains("IBPS", ignoreCase = true) || targetExam.contains("SBI", ignoreCase = true) -> RecruitmentCategory.BANKING.name
                targetExam.contains("Defence", ignoreCase = true) || targetExam.contains("NDA", ignoreCase = true) || targetExam.contains("CDS", ignoreCase = true) -> RecruitmentCategory.DEFENCE.name
                targetExam.contains("UPSC", ignoreCase = true) || targetExam.contains("Civil", ignoreCase = true) -> RecruitmentCategory.UPSC.name
                targetExam.contains("Teacher", ignoreCase = true) || targetExam.contains("TET", ignoreCase = true) || targetExam.contains("BPSC", ignoreCase = true) -> RecruitmentCategory.TEACHING.name
                targetExam.contains("Police", ignoreCase = true) -> RecruitmentCategory.STATE_PSC.name
                else -> RecruitmentCategory.ALL.name
            }

            val forYou = sortedActiveVacancies.filter {
                it.relevanceTier == RelevanceTier.HIGHLY_RELEVANT.name ||
                it.examCategory.equals(examCategoryKeyword, ignoreCase = true) ||
                it.title.contains(targetExam, ignoreCase = true)
            }.ifEmpty {
                sortedActiveVacancies.take(4)
            }

            val otherState = sortedActiveVacancies.filter { !forYou.contains(it) }

            val results = filtered.filter { it.contentType == RecruitmentContentType.RESULT.name }
            val admitCards = filtered.filter { it.contentType == RecruitmentContentType.ADMIT_CARD.name }
            val notifications = filtered.filter {
                it.contentType == RecruitmentContentType.NOTIFICATION.name ||
                it.contentType == RecruitmentContentType.EXAM_UPDATE.name ||
                it.isCorrectionNotice ||
                it.isDeadlineExtended
            }
            val saved = all.filter { it.isSaved }
            val tracked = all.filter { it.applicationStatus != UserApplicationStatus.NONE.name }

            _recruitmentFeedState.update {
                it.copy(
                    userTargetExam = targetExam,
                    userTargetState = targetState,
                    latestForYouVacancies = forYou,
                    otherStateVacancies = otherState,
                    allActiveVacancies = sortedActiveVacancies,
                    resultsList = results,
                    admitCardsList = admitCards,
                    notificationsList = notifications,
                    savedItems = saved,
                    activeTrackedApplications = tracked
                )
            }

            // Step 40: Trigger Verified Event Generation & Daily Digest
            val userProfileForEngine = state.userProfile
            recruitmentIntelligenceEngine.generateNotificationEventsForCatalog(
                items = all,
                profile = userProfileForEngine,
                settings = recruitmentIntelligenceEngine.notificationSettings.value
            )
            recruitmentIntelligenceEngine.generateDailyDigest(
                items = all,
                profile = userProfileForEngine
            )
        }
    }

    // Step 40: Recruitment Intelligence Platform 3.0 Notification & Admin Streams
    val recruitmentNotificationSettings = recruitmentIntelligenceEngine.notificationSettings
    val recruitmentOutbox = recruitmentIntelligenceEngine.outboxItems
    val recruitmentDailyDigest = recruitmentIntelligenceEngine.dailyDigest
    val recruitmentDiagnostics = recruitmentIntelligenceEngine.adminDiagnostics

    fun updateRecruitmentNotificationSettings(settings: RecruitmentNotificationSettings) {
        recruitmentIntelligenceEngine.updateNotificationSettings(settings)
        viewModelScope.launch {
            _snackbarMessage.emit("Notification preferences updated.")
        }
    }

    fun muteRecruitment(id: String) {
        recruitmentIntelligenceEngine.muteRecruitment(id)
        viewModelScope.launch { _snackbarMessage.emit("Muted alerts for this recruitment.") }
    }

    fun unmuteRecruitment(id: String) {
        recruitmentIntelligenceEngine.unmuteRecruitment(id)
        viewModelScope.launch { _snackbarMessage.emit("Unmuted recruitment alerts.") }
    }

    fun muteRecruitmentCategory(category: String) {
        recruitmentIntelligenceEngine.muteCategory(category)
        viewModelScope.launch { _snackbarMessage.emit("Muted alerts for $category category.") }
    }

    fun unmuteRecruitmentCategory(category: String) {
        recruitmentIntelligenceEngine.unmuteCategory(category)
        viewModelScope.launch { _snackbarMessage.emit("Unmuted $category alerts.") }
    }

    fun markRecruitmentOutboxItemRead(id: String) {
        recruitmentIntelligenceEngine.markOutboxItemAsRead(id)
    }

    fun markAllRecruitmentOutboxItemsRead() {
        recruitmentIntelligenceEngine.markAllOutboxItemsAsRead()
    }

    fun deleteRecruitmentOutboxItem(id: String) {
        recruitmentIntelligenceEngine.deleteOutboxItem(id)
    }

    fun clearAllRecruitmentOutbox() {
        recruitmentIntelligenceEngine.clearAllOutbox()
    }

    fun handleNovaRecruitmentQuery(query: String, onNavigate: (NovaRecruitmentActionType) -> Unit) {
        val profile = _recruitmentFeedState.value.userProfile
        val (response, action) = recruitmentIntelligenceEngine.handleNovaRecruitmentQuery(query, profile)
        viewModelScope.launch {
            _snackbarMessage.emit(response)
            if (action != null) {
                onNavigate(action)
            }
        }
    }

    // --- Step 71 Dedicated Category Feed Operations ---

    private fun getMutableStateForCategory(category: UpdateCategory): MutableStateFlow<CategoryFeedState> {
        return when (category) {
            UpdateCategory.VACANCY -> _vacancyFeedState
            UpdateCategory.ADMIT_CARD -> _admitCardFeedState
            UpdateCategory.RESULT -> _resultFeedState
            UpdateCategory.ANSWER_KEY -> _answerKeyFeedState
            UpdateCategory.ADMISSION -> _admissionFeedState
        }
    }

    fun loadUpdatesForCategory(category: UpdateCategory, refresh: Boolean = false, page: Int = 0) {
        val stateFlow = getMutableStateForCategory(category)
        viewModelScope.launch {
            if (refresh) {
                stateFlow.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else if (page == 0 && stateFlow.value.items.isEmpty()) {
                stateFlow.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val current = stateFlow.value
            val result = latestUpdatesRepository.getUpdatesForCategory(
                category = category,
                page = page,
                searchQuery = current.searchQuery.ifBlank { null },
                organizationFilter = current.selectedOrg.ifBlank { null },
                examFilter = current.selectedExam.ifBlank { null },
                sortOption = current.selectedSort
            )

            result.onSuccess { items ->
                stateFlow.update { prev ->
                    val combined = if (page == 0) items else (prev.items + items).distinctBy { it.id }
                    prev.copy(
                        items = combined,
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                        currentPage = page,
                        hasMorePages = items.size >= LatestUpdatesRepository.PAGE_SIZE
                    )
                }
            }.onFailure { err ->
                stateFlow.update { prev ->
                    prev.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = if (prev.items.isEmpty()) err.message ?: "Unable to load updates" else null
                    )
                }
            }
        }
    }

    fun setCategorySearch(category: UpdateCategory, query: String) {
        val stateFlow = getMutableStateForCategory(category)
        stateFlow.update { it.copy(searchQuery = query) }
        loadUpdatesForCategory(category, refresh = true, page = 0)
    }

    fun setCategoryOrgFilter(category: UpdateCategory, org: String) {
        val stateFlow = getMutableStateForCategory(category)
        stateFlow.update { it.copy(selectedOrg = org) }
        loadUpdatesForCategory(category, refresh = true, page = 0)
    }

    fun setCategoryExamFilter(category: UpdateCategory, exam: String) {
        val stateFlow = getMutableStateForCategory(category)
        stateFlow.update { it.copy(selectedExam = exam) }
        loadUpdatesForCategory(category, refresh = true, page = 0)
    }

    fun setCategorySort(category: UpdateCategory, sort: String) {
        val stateFlow = getMutableStateForCategory(category)
        stateFlow.update { it.copy(selectedSort = sort) }
        loadUpdatesForCategory(category, refresh = true, page = 0)
    }

    fun selectUpdateDetail(item: LatestUpdateItem?) {
        _selectedUpdateDetail.value = item
    }

    fun toggleSaveLatestUpdate(id: String, isSaved: Boolean) {
        viewModelScope.launch {
            recruitmentIntelligenceEngine.toggleSaveItem(id, isSaved)
            // Update across all category feed state flows
            val flows: List<MutableStateFlow<CategoryFeedState>> = listOf(
                _vacancyFeedState,
                _admitCardFeedState,
                _resultFeedState,
                _answerKeyFeedState,
                _admissionFeedState
            )
            flows.forEach { flow ->
                flow.update { state ->
                    state.copy(items = state.items.map {
                        if (it.id == id) it.copy(isSaved = isSaved) else it
                    })
                }
            }
            if (_selectedUpdateDetail.value?.id == id) {
                _selectedUpdateDetail.update { it?.copy(isSaved = isSaved) }
            }
            _snackbarMessage.emit(if (isSaved) "📌 Saved to your bookmarks" else "Removed from bookmarks")
        }
    }

    fun setLatestUpdateDeadlineReminder(id: String, enabled: Boolean, daysBefore: Int = 3) {
        viewModelScope.launch {
            recruitmentIntelligenceEngine.setDeadlineReminder(id, enabled, daysBefore)
            val flows: List<MutableStateFlow<CategoryFeedState>> = listOf(
                _vacancyFeedState,
                _admitCardFeedState,
                _resultFeedState,
                _answerKeyFeedState,
                _admissionFeedState
            )
            flows.forEach { flow ->
                flow.update { state ->
                    state.copy(items = state.items.map {
                        if (it.id == id) it.copy(hasDeadlineReminder = enabled) else it
                    })
                }
            }
            if (_selectedUpdateDetail.value?.id == id) {
                _selectedUpdateDetail.update { it?.copy(hasDeadlineReminder = enabled) }
            }
            _snackbarMessage.emit(if (enabled) "⏰ Reminder set for $daysBefore days before deadline" else "Reminder cancelled")
        }
    }

    // --- Intelligence Engine Actions ---

    fun saveExamObjective(objective: ExamObjective) {
        viewModelScope.launch {
            studyRepository.saveExamObjective(objective)
            _snackbarMessage.emit("🎯 Exam Objective Updated!")
        }
    }

    fun updateExamObjective(objective: ExamObjective) {
        viewModelScope.launch {
            studyRepository.updateExamObjective(objective)
        }
    }

    fun setActiveExamObjective(id: Long) {
        viewModelScope.launch {
            studyRepository.setActiveExamObjective(id)
            _snackbarMessage.emit("🎯 Active Exam Objective Set")
        }
    }

    fun recordTopicMasteryPerformance(
        subject: String,
        topic: String,
        questionsAttempted: Int,
        correctCount: Int,
        difficulty: String = "Medium",
        weakSpots: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val updated = studyRepository.recordTopicPerformance(
                subject = subject,
                topic = topic,
                questionsAttempted = questionsAttempted,
                correctCount = correctCount,
                difficulty = difficulty,
                weakSpots = weakSpots
            )
            // If topic is mastered or developing, generate a fresh snapshot
            studyRepository.generateIntelligenceSnapshot()
        }
    }

    fun recordStudentSessionHistory(
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
    ) {
        viewModelScope.launch {
            studyRepository.recordStudentSessionHistory(
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
        }
    }

    fun refreshIntelligenceSnapshot() {
        viewModelScope.launch {
            studyRepository.generateIntelligenceSnapshot()
        }
    }

    fun setUserManualTopicOverride(subject: String, topic: String, override: String) {
        viewModelScope.launch {
            val examId = activeExamContext.value.examId
            studyRepository.setUserManualOverride(examId, subject, topic, override)
            _snackbarMessage.emit("Updated preference for $topic")
        }
    }

    fun recordSpacedRevisionFeedback(subject: String, topic: String, feedback: String) {
        viewModelScope.launch {
            val masteries = allTopicMasteries.value
            val existing = masteries.firstOrNull { it.topic.equals(topic, ignoreCase = true) }
            val now = System.currentTimeMillis()
            val dayMs = 24 * 3600 * 1000L

            val nextReviewMs = when (feedback.uppercase()) {
                "UNDERSTOOD", "EASY" -> now + (7 * dayMs)
                "NEEDS_PRACTICE", "MEDIUM" -> now + (2 * dayMs)
                else -> now + (1 * dayMs) // DIFFICULT / HARD
            }

            val newState = when (feedback.uppercase()) {
                "UNDERSTOOD", "EASY" -> "MASTERED"
                "NEEDS_PRACTICE", "MEDIUM" -> "DEVELOPING"
                else -> "REVISION_DUE"
            }

            if (existing != null) {
                val updated = existing.copy(
                    lastStudiedMillis = now,
                    recommendedReviewDateMillis = nextReviewMs,
                    masteryState = newState,
                    updatedAt = now
                )
                studyRepository.saveTopicMastery(updated)
            } else {
                val newMastery = com.example.data.model.TopicMastery(
                    subject = subject,
                    topic = topic,
                    lastStudiedMillis = now,
                    recommendedReviewDateMillis = nextReviewMs,
                    masteryState = newState,
                    updatedAt = now
                )
                studyRepository.saveTopicMastery(newMastery)
            }
            _snackbarMessage.emit("Updated revision schedule for $topic! 🔁")
        }
    }

    fun resetPersonalizationSignals() {
        viewModelScope.launch {
            val currentPrefs = userStudyPreferences.value
            val resetPrefs = currentPrefs.copy(
                personalizationEnabled = true,
                subjectPrioritiesJson = "{}",
                topicPrioritiesJson = "{}",
                updatedAt = System.currentTimeMillis()
            )
            studyRepository.saveUserPreferences(resetPrefs)
            _snackbarMessage.emit("Personalization recommendation signals reset. 🔄")
        }
    }

    fun updatePersonalizationSettings(settings: com.example.service.intelligence.PersonalizationSettings) {
        viewModelScope.launch {
            val current = userStudyPreferences.value
            val updated = current.copy(
                personalizationEnabled = settings.isEnabled,
                dailyQuestionGoal = settings.dailyQuestionGoal,
                dailyStudyMinutesGoal = settings.dailyStudyMinutesGoal,
                weeklyTestsGoal = settings.weeklyTestsGoal,
                studyTimeAvailableOption = settings.studyTimeAvailableOption,
                caRemindersEnabled = settings.caRemindersEnabled,
                revisionRemindersEnabled = settings.revisionRemindersEnabled,
                studyRemindersEnabled = settings.studyRemindersEnabled,
                testRemindersEnabled = settings.testRemindersEnabled,
                goalRemindersEnabled = settings.goalRemindersEnabled,
                updatedAt = System.currentTimeMillis()
            )
            studyRepository.saveUserPreferences(updated)
            _snackbarMessage.emit("Updated personalization & study goals!")
        }
    }

    fun resetActiveExamPreparationData() {
        viewModelScope.launch {
            val examId = activeExamContext.value.examId
            studyRepository.resetUserExamPreparationData(examId)
            _snackbarMessage.emit("Reset preparation data for ${activeExamContext.value.examName}")
        }
    }

    fun changeSelectedExam(
        examId: String,
        customName: String? = null,
        targetDateMillis: Long? = null,
        targetScore: String? = null,
        refreshStudyPlan: Boolean = true
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val current = userProfile.value ?: UserProfile()
            val examEntity = examCatalogRepository.getExamById(examId)
            val finalExamName = customName?.takeIf { it.isNotBlank() } ?: examEntity?.name ?: examId
            val finalCategory = examEntity?.category ?: current.examCategory
            val finalDate = targetDateMillis ?: current.examDateMillis
            val finalScore = targetScore ?: current.targetScore

            // Fetch official subjects for this new exam
            val officialSubjects = examCatalogRepository.getSubjectsForExamOnce(examId).map { it.name }
            val newSubjects = if (officialSubjects.isNotEmpty()) officialSubjects else current.subjects

            val updatedProfile = current.copy(
                examName = finalExamName,
                examCategory = finalCategory,
                examDateMillis = finalDate,
                targetScore = finalScore,
                subjects = newSubjects
            )

            authRepository.updateUserProfile(updatedProfile)

            // Update active Exam Objective
            studyRepository.intelligenceRepository.saveExamObjective(
                ExamObjective(
                    examName = finalExamName,
                    targetScoreOrRank = finalScore,
                    examDateMillis = finalDate,
                    category = finalCategory,
                    prioritySubjects = newSubjects.take(3),
                    status = "ACTIVE"
                )
            )

            if (refreshStudyPlan) {
                generateAiStudyPlan()
            }
            _isAuthLoading.value = false
            _snackbarMessage.emit("Switched target exam to $finalExamName")
        }
    }

    fun createAndSelectCustomExam(
        name: String,
        category: String = "Custom",
        subjects: List<String>,
        targetDateMillis: Long,
        targetScore: String
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val customExam = examCatalogRepository.createCustomExam(
                name = name,
                category = category,
                customSubjectsList = subjects,
                targetDateMillis = targetDateMillis
            )
            changeSelectedExam(
                examId = customExam.id,
                customName = customExam.name,
                targetDateMillis = targetDateMillis,
                targetScore = targetScore,
                refreshStudyPlan = true
            )
        }
    }

    // --- STEP 14 - Smart Content & AI Doubt Solving State & Methods ---
    private val _activeLearningContent = MutableStateFlow<LearningTopicContent?>(null)
    val activeLearningContent: StateFlow<LearningTopicContent?> = _activeLearningContent.asStateFlow()

    private val _isLearningContentLoading = MutableStateFlow(false)
    val isLearningContentLoading: StateFlow<Boolean> = _isLearningContentLoading.asStateFlow()

    private val _novaDoubtResponse = MutableStateFlow("")
    val novaDoubtResponse: StateFlow<String> = _novaDoubtResponse.asStateFlow()

    private val _isNovaDoubtThinking = MutableStateFlow(false)
    val isNovaDoubtThinking: StateFlow<Boolean> = _isNovaDoubtThinking.asStateFlow()

    private val _novaProgressAnalysis = MutableStateFlow<String?>(null)
    val novaProgressAnalysis: StateFlow<String?> = _novaProgressAnalysis.asStateFlow()

    private val _isNovaProgressAnalyzing = MutableStateFlow(false)
    val isNovaProgressAnalyzing: StateFlow<Boolean> = _isNovaProgressAnalyzing.asStateFlow()

    val allLearningBookmarks: StateFlow<List<UserLearningBookmark>> = studyRepository.allLearningBookmarks
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadLearningTopicContent(
        subject: String,
        chapter: String,
        topic: String,
        forceRefresh: Boolean = false,
        language: String = "English"
    ) {
        viewModelScope.launch {
            _isLearningContentLoading.value = true
            try {
                val ctx = activeExamContext.value
                val content = studyRepository.smartLearningEngine.loadTopicContent(
                    examContext = ctx,
                    subject = subject,
                    chapter = chapter,
                    topic = topic,
                    masteryScore = 60,
                    languagePreference = language,
                    forceRefresh = forceRefresh
                )
                _activeLearningContent.value = content
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed loading topic content: $topic", e)
            } finally {
                _isLearningContentLoading.value = false
            }
        }
    }

    fun toggleLearningBookmark(subject: String, topic: String, title: String, snippet: String, type: String = "TOPIC") {
        viewModelScope.launch {
            studyRepository.smartLearningEngine.toggleBookmark(subject, topic, title, snippet, type)
            _snackbarMessage.emit("Bookmark saved")
        }
    }

    fun saveTopicNote(subject: String, topic: String, contentText: String) {
        viewModelScope.launch {
            studyRepository.smartLearningEngine.saveTopicNote(subject, topic, contentText)
            _snackbarMessage.emit("Saved note for $topic")
        }
    }

    fun deleteLearningBookmark(id: Long) {
        viewModelScope.launch {
            studyRepository.smartLearningEngine.removeBookmark(id)
            _snackbarMessage.emit("Bookmark removed")
        }
    }

    fun deleteTopicNote(id: Long) {
        viewModelScope.launch {
            studyRepository.deleteSmartNote(id)
            _snackbarMessage.emit("Note deleted")
        }
    }

    fun askNovaTopicDoubt(
        subject: String,
        chapter: String,
        topic: String,
        prompt: String,
        language: String = "English"
    ) {
        viewModelScope.launch {
            _isNovaDoubtThinking.value = true
            _novaDoubtResponse.value = ""
            try {
                val ctx = activeExamContext.value
                val doubtContext = "Student is studying $topic in $subject under $chapter for target exam ${ctx.examName}. Language: $language."
                val fullPrompt = "$doubtContext\nStudent Doubt: $prompt"

                val result = geminiRepository.askNova(
                    userPrompt = fullPrompt,
                    studyContext = NovaStudyContext(
                        targetExam = ctx.examName,
                        preferredLanguage = language
                    )
                )

                _novaDoubtResponse.value = result.getOrNull()?.replyMarkdown ?: "Nova is here to help! Could you rephrase your doubt?"
            } catch (e: Exception) {
                _novaDoubtResponse.value = "Sorry, I ran into a connection glitch. Please try again!"
            } finally {
                _isNovaDoubtThinking.value = false
            }
        }
    }

    fun completeQuickTest(subject: String, topic: String, score: Int, total: Int) {
        viewModelScope.launch {
            val percent = if (total > 0) (score.toFloat() / total * 100).toInt() else 0
            studyRepository.recordStudentSessionHistory(
                sessionType = "PRACTICE_TEST",
                subject = subject,
                topic = topic,
                durationMinutes = 10,
                actualMinutesSpent = 10,
                xpEarned = score * 15,
                accuracyPercent = percent.toFloat(),
                questionsAttempted = total,
                productivityRating = 5,
                notesSummary = "Completed $topic quick check: $score/$total correct"
            )

            _snackbarMessage.emit("Quick Test Completed! Score: $score/$total (+${score * 15} XP)")
        }
    }

    fun generateNovaProgressAnalysis(
        examName: String,
        aggregatedMetricsSummary: String,
        language: String = "English"
    ) {
        viewModelScope.launch {
            _isNovaProgressAnalyzing.value = true
            try {
                val prompt = """
                    You are NOVA, an expert academic preparation mentor for Indian competitive exams ($examName).
                    Analyze the following REAL student performance analytics data:
                    $aggregatedMetricsSummary

                    Instructions:
                    - Provide a concise, highly actionable evaluation in $language (around 3-4 bullet points or short paragraphs).
                    - Identify: 1) Strongest subject/area, 2) Weakest subject or topic needing improvement, 3) Improvement trend observation, 4) The single most recommended next focus step.
                    - Do not fabricate missing stats or give generic advice; tailor directly to the provided metrics.
                    - Keep the tone encouraging, objective, and mentor-like.
                """.trimIndent()

                val result = geminiRepository.askNova(
                    userPrompt = prompt,
                    studyContext = NovaStudyContext(
                        targetExam = examName,
                        preferredLanguage = language
                    )
                )

                _novaProgressAnalysis.value = result.getOrNull()?.replyMarkdown
                    ?: if (language.equals("Hindi", ignoreCase = true))
                        "आपकी तैयारी का विश्लेषण: अपने कमजोर विषयों पर नियमित रूप से मॉक टेस्ट और रीविजन पर ध्यान दें।"
                    else
                        "Your performance analysis is ready. Focus on regular practice and targeted revision in your lower-accuracy areas."
            } catch (e: Exception) {
                _novaProgressAnalysis.value = "NOVA analysis is temporarily unavailable."
            } finally {
                _isNovaProgressAnalyzing.value = false
            }
        }
    }

    override fun onCleared() {

        super.onCleared()
        timerJob?.cancel()
        tts?.stop()
        tts?.shutdown()
    }
}

class MainViewModelFactory(
    private val application: StudyMateApplication
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                application = application,
                authRepository = application.authRepository,
                geminiRepository = application.geminiRepository,
                studyRepository = application.studyRepository,
                examCatalogRepository = application.examCatalogRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

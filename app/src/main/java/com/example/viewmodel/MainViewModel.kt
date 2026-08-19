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
import com.example.data.remote.AuthRepository
import com.example.data.remote.GeminiRepository
import com.example.data.repository.ExamCatalogRepository
import com.example.data.repository.StudyRepository
import com.example.service.intelligence.StudyMateIntelligenceEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

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

data class FocusTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val initialMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val subject: String = "Physics",
    val topic: String = "Current Electricity",
    val restrictedAppsCount: Int = 4,
    val showCelebration: Boolean = false,
    val lastSessionXp: Int = 50
)

data class ActiveTestState(
    val isTestInProgress: Boolean = false,
    val subject: String = "Physics",
    val title: String = "AI Practice Test",
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(), // questionIndex -> optionIndex
    val markedForReview: Set<Int> = emptySet(),
    val remainingSeconds: Int = 600,
    val isCompleted: Boolean = false,
    val completedAttempt: MockTestAttempt? = null,
    val detailedQuestions: List<QuestionAttemptDetail> = emptyList(),
    val isPaletteOpen: Boolean = false,
    val isSubmitConfirmOpen: Boolean = false,
    val config: MockTestConfig = MockTestConfig()
)

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

    // --- Study Plan Items ---
    val studyPlanItems: StateFlow<List<StudyPlanItem>> = studyRepository.allPlanItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isPlanGenerating = MutableStateFlow(false)
    val isPlanGenerating: StateFlow<Boolean> = _isPlanGenerating.asStateFlow()

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

    // --- Mock Test & Practice ---
    val mockTestAttempts: StateFlow<List<MockTestAttempt>> = studyRepository.allMockTestAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mistakes: StateFlow<List<MistakeItem>> = studyRepository.allMistakes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcards: StateFlow<List<FlashcardItem>> = studyRepository.allFlashcards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userQuestionMaterials: StateFlow<List<UserQuestionMaterial>> = studyRepository.allUserQuestionMaterials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isFlashcardGenerating = MutableStateFlow(false)
    val isFlashcardGenerating: StateFlow<Boolean> = _isFlashcardGenerating.asStateFlow()

    private val _flashcardMessage = MutableStateFlow<String?>(null)
    val flashcardMessage: StateFlow<String?> = _flashcardMessage.asStateFlow()

    private val _activeTestState = MutableStateFlow(ActiveTestState())
    val activeTestState: StateFlow<ActiveTestState> = _activeTestState.asStateFlow()

    private val _isTestGenerating = MutableStateFlow(false)
    val isTestGenerating: StateFlow<Boolean> = _isTestGenerating.asStateFlow()

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
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _notificationPrefs = MutableStateFlow(NotificationPreference())
    val notificationPrefs: StateFlow<NotificationPreference> = _notificationPrefs.asStateFlow()

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
        initTts()
        loadInitialNotificationSettings()
        viewModelScope.launch {
            authRepository.getInitialUser()
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
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            val result = authRepository.signInWithEmail(email, pass)
            result.onFailure { _authErrorMessage.value = it.message ?: "Authentication failed" }
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

    fun signOut(activityContext: android.content.Context? = null) {
        viewModelScope.launch {
            authRepository.signOut(activityContext)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            authRepository.deleteAccount()
        }
    }

    // --- Study Plan Actions ---

    fun generateAiStudyPlan() {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            _isPlanGenerating.value = true
            val result = geminiRepository.generateStudyPlan(
                subjects = profile.subjects,
                grade = profile.grade,
                goal = profile.goal,
                dailyMinutes = profile.dailyTargetMinutes,
                preferredTime = profile.preferredStudyTime
            )
            result.onSuccess { items ->
                studyRepository.replaceStudyPlan(items)
            }
            _isPlanGenerating.value = false
        }
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

    // --- Focus Mode Actions ---

    fun startFocusSession(subject: String, topic: String, durationMinutes: Int = 25) {
        timerJob?.cancel()
        val appContext = getApplication<Application>()
        val restrictedCount = com.example.service.FocusShieldManager.getRestrictedPackages().size
        _focusState.value = _focusState.value.copy(
            isRunning = true,
            isPaused = false,
            initialMinutes = durationMinutes,
            remainingSeconds = durationMinutes * 60,
            subject = subject,
            topic = topic,
            restrictedAppsCount = restrictedCount,
            showCelebration = false
        )

        com.example.service.FocusShieldManager.startFocusSession(appContext, subject, topic, durationMinutes)
        val userName = userProfile.value?.name ?: "Rahul"
        com.example.notification.StudyNotificationManager.sendFocusSessionStarted(
            context = appContext,
            userName = userName,
            subject = subject,
            topic = topic,
            durationMinutes = durationMinutes
        )

        timerJob = viewModelScope.launch {
            while (_focusState.value.remainingSeconds > 0) {
                delay(1000)
                if (!_focusState.value.isPaused) {
                    val nextSecs = _focusState.value.remainingSeconds - 1
                    _focusState.value = _focusState.value.copy(
                        remainingSeconds = nextSecs
                    )
                    com.example.service.FocusShieldManager.updateRemainingTime(nextSecs)
                }
            }
            // Completed
            endFocusSession(isAutoFinished = true)
        }
    }

    fun toggleFocusPause() {
        _focusState.value = _focusState.value.copy(
            isPaused = !_focusState.value.isPaused
        )
    }

    fun endFocusSession(isAutoFinished: Boolean = false) {
        timerJob?.cancel()
        val appContext = getApplication<Application>()
        com.example.service.FocusShieldManager.endFocusSession()
        val initialMins = _focusState.value.initialMinutes
        val elapsedSeconds = (initialMins * 60) - _focusState.value.remainingSeconds
        val actualMinutes = (elapsedSeconds / 60).coerceAtLeast(1)

        viewModelScope.launch {
            val session = studyRepository.recordFocusSession(
                subject = _focusState.value.subject,
                topic = _focusState.value.topic,
                durationMinutes = initialMins,
                actualMinutesSpent = actualMinutes
            )
            _focusState.value = _focusState.value.copy(
                isRunning = false,
                isPaused = false,
                showCelebration = true,
                lastSessionXp = session.xpEarned
            )

            val userName = userProfile.value?.name ?: "Rahul"
            com.example.notification.StudyNotificationManager.sendFocusSessionCompleted(
                context = appContext,
                userName = userName,
                subject = _focusState.value.subject,
                minutes = actualMinutes,
                xpEarned = session.xpEarned
            )

            if (isAutoFinished) {
                val breakMins = userProfile.value?.breakDurationMinutes ?: 15
                com.example.notification.StudyNotificationManager.sendBreakReminder(
                    context = appContext,
                    userName = userName,
                    breakMinutes = breakMins
                )
            }
        }
    }

    fun dismissCelebration() {
        _focusState.value = _focusState.value.copy(showCelebration = false)
    }

    val examQuestionBankRepository = com.example.data.repository.ExamQuestionBankRepository()
    private var mockTestTimerJob: kotlinx.coroutines.Job? = null

    // --- Mock Test & Practice Actions ---

    fun startMockTestWithConfig(config: MockTestConfig) {
        viewModelScope.launch {
            _isTestGenerating.value = true

            val targetExamName = config.exam.ifBlank { selectedExam.value.examName }

            // 1. Fetch verified exam questions
            var questions = examQuestionBankRepository.getQuestionsForTest(
                examName = targetExamName,
                subject = config.subject,
                topic = config.topic,
                difficulty = config.difficulty,
                language = config.language,
                desiredCount = config.questionCount,
                testType = config.testType
            )

            // 2. If additional questions needed, request exam-aware questions from AI
            if (questions.size < config.questionCount) {
                val needCount = config.questionCount - questions.size
                val aiResult = geminiRepository.generateMockTestQuestions(
                    subject = if (config.subject == "All Subjects") (selectedExamSubjects.value.firstOrNull()?.name ?: "General") else config.subject,
                    chapter = config.topic,
                    count = needCount
                )
                aiResult.onSuccess { aiQs ->
                    val tagged = aiQs.map { q ->
                        q.copy(
                            subject = if (q.subject.isBlank() || q.subject == "General") config.subject else q.subject,
                            topic = if (q.topic.isBlank()) config.topic else q.topic,
                            source = QuestionSource.AI_GENERATED,
                            sourceLabel = "AI Exam Concept",
                            yearOrTag = targetExamName
                        )
                    }
                    questions = questions + tagged
                }
            }

            val finalQuestions = questions.take(config.questionCount.coerceAtLeast(1))
            val totalSeconds = (config.timeLimitMinutes * 60).coerceAtLeast(60)

            _activeTestState.value = ActiveTestState(
                isTestInProgress = true,
                subject = config.subject,
                title = when (config.testType) {
                    MockTestType.FULL_MOCK -> "$targetExamName Full Mock Test"
                    MockTestType.SUBJECT_TEST -> "$targetExamName - ${config.subject}"
                    MockTestType.TOPIC_TEST -> "$targetExamName - ${config.topic}"
                    MockTestType.QUICK_PRACTICE -> "$targetExamName Speed Practice"
                },
                questions = finalQuestions,
                currentQuestionIndex = 0,
                selectedAnswers = emptyMap(),
                markedForReview = emptySet(),
                remainingSeconds = totalSeconds,
                isCompleted = false,
                completedAttempt = null,
                config = config
            )
            _isTestGenerating.value = false

            // Cancel any old timer and start countdown job
            mockTestTimerJob?.cancel()
            mockTestTimerJob = viewModelScope.launch {
                while (_activeTestState.value.isTestInProgress && _activeTestState.value.remainingSeconds > 0) {
                    kotlinx.coroutines.delay(1000L)
                    val currentRemaining = _activeTestState.value.remainingSeconds
                    if (currentRemaining <= 1) {
                        _activeTestState.value = _activeTestState.value.copy(remainingSeconds = 0)
                        submitMockTest()
                        break
                    } else {
                        _activeTestState.value = _activeTestState.value.copy(remainingSeconds = currentRemaining - 1)
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
        val currentAnswers = _activeTestState.value.selectedAnswers.toMutableMap()
        currentAnswers[questionIndex] = optionIndex
        _activeTestState.value = _activeTestState.value.copy(selectedAnswers = currentAnswers)
    }

    fun clearTestAnswer(questionIndex: Int) {
        val currentAnswers = _activeTestState.value.selectedAnswers.toMutableMap()
        currentAnswers.remove(questionIndex)
        _activeTestState.value = _activeTestState.value.copy(selectedAnswers = currentAnswers)
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
        _activeTestState.value = _activeTestState.value.copy(markedForReview = current)
    }

    fun navigateTestQuestion(index: Int) {
        if (index in 0 until _activeTestState.value.questions.size) {
            _activeTestState.value = _activeTestState.value.copy(currentQuestionIndex = index)
        }
    }

    fun setPaletteOpen(isOpen: Boolean) {
        _activeTestState.value = _activeTestState.value.copy(isPaletteOpen = isOpen)
    }

    fun setSubmitConfirmOpen(isOpen: Boolean) {
        _activeTestState.value = _activeTestState.value.copy(isSubmitConfirmOpen = isOpen)
    }

    fun submitMockTest() {
        mockTestTimerJob?.cancel()
        val state = _activeTestState.value
        val questions = state.questions
        val answers = state.selectedAnswers
        var correctCount = 0
        var incorrectCount = 0
        var skippedCount = 0

        val topicAttemptCounts = mutableMapOf<String, Int>()
        val topicIncorrectCounts = mutableMapOf<String, Int>()
        val topicCorrectCounts = mutableMapOf<String, Int>()

        val details = questions.mapIndexed { idx, q ->
            val chosen = answers[idx]
            val isCorr = chosen != null && chosen == q.correctOptionIndex
            val topicKey = q.topic.ifBlank { q.subject }

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
                timeSpentSeconds = if (questions.isNotEmpty()) (state.config.timeLimitMinutes * 60 - state.remainingSeconds) / questions.size else 30
            )
        }

        // Apply strict sample threshold before classifying weak or strong topics
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
            questions.forEachIndexed { idx, q ->
                val chosen = answers[idx]
                if (chosen != null && chosen != q.correctOptionIndex) {
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

            val totalAllowedSecs = state.config.timeLimitMinutes * 60
            val timeSpent = totalAllowedSecs - state.remainingSeconds
            val recommendation = if (questions.size < 3) {
                "Not enough data yet for a reliable topic analysis. Practice more questions to identify specific topic strengths and weaknesses."
            } else if (weakTopics.isNotEmpty()) {
                "Focus on ${weakTopics.take(2).joinToString(" & ")}. Practice targeted PYQs and concept revisions."
            } else {
                "Excellent mastery across attempted topics! Maintain momentum with full-length timed tests."
            }

            val attempt = studyRepository.recordMockTestAttempt(
                title = state.title,
                subject = state.subject,
                score = correctCount,
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
                markingScheme = "+4 / -1 (Standard)",
                totalTimeAllowedSeconds = totalAllowedSecs
            )

            _activeTestState.value = state.copy(
                isTestInProgress = false,
                isCompleted = true,
                completedAttempt = attempt,
                detailedQuestions = details,
                isPaletteOpen = false,
                isSubmitConfirmOpen = false
            )
        }
    }

    fun exitTest() {
        mockTestTimerJob?.cancel()
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

    fun updateTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    fun updateNotificationPrefs(prefs: NotificationPreference) {
        _notificationPrefs.value = prefs
        val appContext = getApplication<Application>()
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

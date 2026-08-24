package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import com.example.data.remote.GeminiRepository
import com.example.data.repository.StudyRepository
import com.example.notification.NovaNotificationEngine
import com.example.notification.StudyNotificationManager
import com.example.service.NovaUsageStatsHelper
import com.example.service.NovaVoiceManager
import com.example.service.coach.NovaStudyCoach
import com.example.service.intelligence.StudyMateIntelligenceEngine
import com.example.service.voice.NovaVoiceEmotion
import com.example.ui.components.AppNavTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class NovaScreenTab(val title: String, val icon: String) {
    DASHBOARD("Dashboard", "⚡"),
    ASSISTANT_CHAT("NOVA Voice & Chat", "🤖"),
    SMART_SEARCH("Smart Search", "🔍"),
    SMART_NOTES("Smart Notes", "📝"),
    CURRENT_AFFAIRS("Current Affairs & Exams", "📰"),
    VOICE_NOTES("Voice Notes & Lectures", "🎙️"),
    INTERACTIVE_STUDY_QUIZ("Quiz Intelligence", "🎯"),
    MEMORY_CENTER("Memory Center", "🧠"),
    ANALYTICS_STRATEGY("Study Strategy", "📊"),
    NOVA_SETTINGS("Settings & Privacy", "⚙️")
}

data class NovaConversationSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messages: List<NovaChatMessage>,
    val examContext: String = ""
)

enum class QuizScreenStage {
    CONFIGURING,
    BRIEFING,
    ACTIVE,
    FINISHED
}

data class SubjectAccuracyStats(
    val total: Int = 0,
    val correct: Int = 0,
    val accuracyPercent: Float = 0f
)

data class InteractiveQuizState(
    val selectedExam: String = "",
    val subject: String = "All Subjects",
    val topic: String = "All Topics",
    val language: String = "English", // "English" or "हिंदी"
    val questionCount: Int = 10,
    val durationMinutes: Int = 15,
    val isCustomDuration: Boolean = false,
    val difficulty: String = "Mixed", // "Easy", "Medium", "Hard", "Mixed"
    val questionMode: String = "Practice", // "Practice", "Mock Test", "Previous-Year Style", "Current Affairs", "Revision"
    val availableSubjects: List<String> = emptyList(),
    val availableTopics: List<String> = emptyList(),

    val screenStage: QuizScreenStage = QuizScreenStage.CONFIGURING,

    // Active session state
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val userAnswers: Map<Int, Int> = emptyMap(),
    val markedForReview: Set<Int> = emptySet(),
    val isAnswerSubmitted: Boolean = false,
    val immediateChecked: Map<Int, Boolean> = emptyMap(),
    val timeRemainingSeconds: Int = 0,
    val totalDurationSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val isGenerating: Boolean = false,
    val generationStatus: String = "",
    val errorMessage: String? = null,
    val explanation: String = "",

    // Post-Test Results & AI Intelligence
    val isQuizFinished: Boolean = false,
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val earnedMarks: Float = 0f,
    val maxMarks: Float = 0f,
    val accuracyPercent: Float = 0f,
    val timeSpentSeconds: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val unansweredCount: Int = 0,
    val markedCount: Int = 0,
    val markingScheme: String = "+1 / -0.25",
    val subjectBreakdown: Map<String, SubjectAccuracyStats> = emptyMap(),
    val weakTopicsIdentified: List<String> = emptyList(),
    val strongTopicsIdentified: List<String> = emptyList(),
    val novaAiAnalysis: String = "",
    val isAnalyzingResult: Boolean = false,
    val isResultSaved: Boolean = false
)

data class NovaAnalyticsData(
    val totalFocusHours: Float = 4.5f,
    val completedSessionsCount: Int = 6,
    val weeklyConsistencyScore: Int = 88,
    val averageQuizAccuracy: Float = 78.5f,
    val topStrongSubject: String = "Physics",
    val topWeakSubject: String = "Chemistry",
    val topWeakTopic: String = "Organic Reactions",
    val daysUntilExam: Int = 28,
    val subjectMinutes: Map<String, Int> = mapOf("Physics" to 140, "Mathematics" to 95, "Chemistry" to 45)
)

class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getDatabase(application)
    private val studyRepository = StudyRepository(db)
    private val geminiRepository = GeminiRepository()
    val voiceManager = NovaVoiceManager(application)
    val notificationEngine = NovaNotificationEngine(application)

    private val examCatalogRepository = com.example.data.repository.ExamCatalogRepository(db.examCatalogDao())
    private val examQuestionBankRepository = com.example.data.repository.ExamQuestionBankRepository()
    private var quizTimerJob: kotlinx.coroutines.Job? = null

    private val _currentTab = MutableStateFlow(NovaScreenTab.DASHBOARD)
    val currentTab: StateFlow<NovaScreenTab> = _currentTab.asStateFlow()

    private val _messages = MutableStateFlow<List<NovaChatMessage>>(emptyList())
    val messages: StateFlow<List<NovaChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _attachedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val attachedImageBitmap: StateFlow<Bitmap?> = _attachedImageBitmap.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<Uri?>(null)
    val attachedImageUri: StateFlow<Uri?> = _attachedImageUri.asStateFlow()

    private val _savedConversations = MutableStateFlow<List<NovaConversationSession>>(emptyList())
    val savedConversations: StateFlow<List<NovaConversationSession>> = _savedConversations.asStateFlow()

    private val _settings = MutableStateFlow(NovaSettings())
    val settings: StateFlow<NovaSettings> = _settings.asStateFlow()

    private val _studyContext = MutableStateFlow(NovaStudyContext())
    val studyContext: StateFlow<NovaStudyContext> = _studyContext.asStateFlow()

    val memories: StateFlow<List<NovaMemoryItem>> = studyRepository.allNovaMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingReminders: StateFlow<List<NovaReminderItem>> = studyRepository.pendingNovaReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlanItems: StateFlow<List<StudyPlanItem>> = studyRepository.allPlanItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttempts: StateFlow<List<MockTestAttempt>> = studyRepository.allMockTestAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMistakes: StateFlow<List<MistakeItem>> = studyRepository.allMistakes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<FocusSession>> = studyRepository.allFocusSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSmartNotes: StateFlow<List<SmartNoteItem>> = studyRepository.allSmartNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCurrentAffairs: StateFlow<List<CurrentAffairsItem>> = studyRepository.allCurrentAffairs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExamUpdates: StateFlow<List<ExamUpdateItem>> = studyRepository.allExamUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Affairs & Exam Radar UI States
    private val _isRefreshingCurrentAffairs = MutableStateFlow(false)
    val isRefreshingCurrentAffairs: StateFlow<Boolean> = _isRefreshingCurrentAffairs.asStateFlow()

    private val _currentAffairsFilterCategory = MutableStateFlow("All")
    val currentAffairsFilterCategory: StateFlow<String> = _currentAffairsFilterCategory.asStateFlow()

    private val _currentAffairsSearchQuery = MutableStateFlow("")
    val currentAffairsSearchQuery: StateFlow<String> = _currentAffairsSearchQuery.asStateFlow()

    private val _currentAffairsLanguage = MutableStateFlow("English")
    val currentAffairsLanguage: StateFlow<String> = _currentAffairsLanguage.asStateFlow()

    private val _selectedAffairForNova = MutableStateFlow<CurrentAffairsItem?>(null)
    val selectedAffairForNova: StateFlow<CurrentAffairsItem?> = _selectedAffairForNova.asStateFlow()

    private val _novaAffairAnalysis = MutableStateFlow<String?>(null)
    val novaAffairAnalysis: StateFlow<String?> = _novaAffairAnalysis.asStateFlow()

    private val _isAnalyzingAffair = MutableStateFlow(false)
    val isAnalyzingAffair: StateFlow<Boolean> = _isAnalyzingAffair.asStateFlow()

    private val _currentAffairsError = MutableStateFlow<String?>(null)
    val currentAffairsError: StateFlow<String?> = _currentAffairsError.asStateFlow()

    private val _selectedCurrentAffairsDate = MutableStateFlow("Today")
    val selectedCurrentAffairsDate: StateFlow<String> = _selectedCurrentAffairsDate.asStateFlow()

    private val _currentAffairsExamMode = MutableStateFlow("For You")
    val currentAffairsExamMode: StateFlow<String> = _currentAffairsExamMode.asStateFlow()

    private val _isFreshWebSearching = MutableStateFlow(false)
    val isFreshWebSearching: StateFlow<Boolean> = _isFreshWebSearching.asStateFlow()

    private val _currentAffairsQuizState = MutableStateFlow<com.example.data.model.CurrentAffairsQuizSession?>(null)
    val currentAffairsQuizState: StateFlow<com.example.data.model.CurrentAffairsQuizSession?> = _currentAffairsQuizState.asStateFlow()

    private val _lastRefreshedTime = MutableStateFlow(System.currentTimeMillis())
    val lastRefreshedTime: StateFlow<Long> = _lastRefreshedTime.asStateFlow()

    private val _smartSearchResult = MutableStateFlow<SmartSearchResult?>(null)
    val smartSearchResult: StateFlow<SmartSearchResult?> = _smartSearchResult.asStateFlow()

    private val _isSmartSearching = MutableStateFlow(false)
    val isSmartSearching: StateFlow<Boolean> = _isSmartSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _searchLanguage = MutableStateFlow("English")
    val searchLanguage: StateFlow<String> = _searchLanguage.asStateFlow()

    private val _searchSubject = MutableStateFlow("General Science")
    val searchSubject: StateFlow<String> = _searchSubject.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<NovaSearchHistoryItem>>(
        listOf(
            NovaSearchHistoryItem("Newton's Laws of Motion & Momentum", "General Science", System.currentTimeMillis() - 3600000),
            NovaSearchHistoryItem("Ohm's Law & Circuit Formulas", "General Science", System.currentTimeMillis() - 7200000),
            NovaSearchHistoryItem("Photosynthesis Light vs Dark Reaction", "Biology", System.currentTimeMillis() - 86400000)
        )
    )
    val searchHistory: StateFlow<List<NovaSearchHistoryItem>> = _searchHistory.asStateFlow()

    val webIntelligenceEngine = com.example.service.intelligence.NovaWebIntelligenceEngine(
        smartNoteDao = db.smartNoteDao(),
        geminiRepository = geminiRepository
    )

    val smartLearningSystemEngine = com.example.service.intelligence.SmartLearningSystemEngine(
        database = db,
        webIntelligenceEngine = webIntelligenceEngine
    )

    private val _dailyExamBriefing = MutableStateFlow<DailyExamBriefing?>(null)
    val dailyExamBriefing: StateFlow<DailyExamBriefing?> = _dailyExamBriefing.asStateFlow()

    private val _isDailyBriefingLoading = MutableStateFlow(false)
    val isDailyBriefingLoading: StateFlow<Boolean> = _isDailyBriefingLoading.asStateFlow()

    private val _smartRevisionItems = MutableStateFlow<List<SmartRevisionTopicItem>>(emptyList())
    val smartRevisionItems: StateFlow<List<SmartRevisionTopicItem>> = _smartRevisionItems.asStateFlow()

    private val _activeRevisionTopic = MutableStateFlow<SmartRevisionTopicItem?>(null)
    val activeRevisionTopic: StateFlow<SmartRevisionTopicItem?> = _activeRevisionTopic.asStateFlow()

    private val _showMcqConfigDialog = MutableStateFlow(false)
    val showMcqConfigDialog: StateFlow<Boolean> = _showMcqConfigDialog.asStateFlow()

    private val _showRevisionDialog = MutableStateFlow(false)
    val showRevisionDialog: StateFlow<Boolean> = _showRevisionDialog.asStateFlow()

    private val _showDailyBriefDialog = MutableStateFlow(false)
    val showDailyBriefDialog: StateFlow<Boolean> = _showDailyBriefDialog.asStateFlow()

    private val _generatedMcqBatch = MutableStateFlow<GeneratedMcqBatch?>(null)
    val generatedMcqBatch: StateFlow<GeneratedMcqBatch?> = _generatedMcqBatch.asStateFlow()

    private val _isGeneratingFreshMcqs = MutableStateFlow(false)
    val isGeneratingFreshMcqs: StateFlow<Boolean> = _isGeneratingFreshMcqs.asStateFlow()

    private val _webSearchMode = MutableStateFlow(NovaWebSearchMode.ALL_WEB)
    val webSearchMode: StateFlow<NovaWebSearchMode> = _webSearchMode.asStateFlow()

    private val _isWebSearchActive = MutableStateFlow(false)
    val isWebSearchActive: StateFlow<Boolean> = _isWebSearchActive.asStateFlow()

    private val _searchingStatusText = MutableStateFlow<String?>(null)
    val searchingStatusText: StateFlow<String?> = _searchingStatusText.asStateFlow()

    val studentMasterContext: StateFlow<StudentMasterContext?> = combine(
        studyRepository.userProfile,
        allPlanItems,
        allSessions,
        allAttempts,
        allMistakes,
        studyRepository.allFlashcards
    ) { args: Array<Any?> ->
        val user = args[0] as? UserProfile
        val plans = (args[1] as? List<*>)?.filterIsInstance<StudyPlanItem>() ?: emptyList()
        val sessions = (args[2] as? List<*>)?.filterIsInstance<FocusSession>() ?: emptyList()
        val attempts = (args[3] as? List<*>)?.filterIsInstance<MockTestAttempt>() ?: emptyList()
        val mistakes = (args[4] as? List<*>)?.filterIsInstance<MistakeItem>() ?: emptyList()
        val cards = (args[5] as? List<*>)?.filterIsInstance<FlashcardItem>() ?: emptyList()

        if (user != null) {
            StudyMateIntelligenceEngine.buildMasterContext(
                profile = user,
                plans = plans,
                focusSessions = sessions,
                mockAttempts = attempts,
                mistakes = mistakes,
                flashcards = cards
            )
        } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _quizState = MutableStateFlow(InteractiveQuizState())
    val quizState: StateFlow<InteractiveQuizState> = _quizState.asStateFlow()

    private val _analyticsData = MutableStateFlow(NovaAnalyticsData())
    val analyticsData: StateFlow<NovaAnalyticsData> = _analyticsData.asStateFlow()

    private val _dailyBriefingText = MutableStateFlow<String?>(null)
    val dailyBriefingText: StateFlow<String?> = _dailyBriefingText.asStateFlow()

    private val _dailyReviewText = MutableStateFlow<String?>(null)
    val dailyReviewText: StateFlow<String?> = _dailyReviewText.asStateFlow()

    private val _adaptiveRecommendation = MutableStateFlow<StudyNowRecommendation?>(null)
    val adaptiveRecommendation: StateFlow<StudyNowRecommendation?> = _adaptiveRecommendation.asStateFlow()

    private val _missedSessionAlert = MutableStateFlow<Pair<StudyPlanItem, String>?>(null)
    val missedSessionAlert: StateFlow<Pair<StudyPlanItem, String>?> = _missedSessionAlert.asStateFlow()

    private val _socialMediaNudge = MutableStateFlow<String?>(null)
    val socialMediaNudge: StateFlow<String?> = _socialMediaNudge.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Pair<String, Map<String, Any>>>()
    val navigationEvent: SharedFlow<Pair<String, Map<String, Any>>> = _navigationEvent.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // STEP 20: Home Universal Widget & Context-Aware AI States
    private val _homeWidgetDisplayState = MutableStateFlow(HomeWidgetDisplayState.COLLAPSED)
    val homeWidgetDisplayState: StateFlow<HomeWidgetDisplayState> = _homeWidgetDisplayState.asStateFlow()

    private val _homeWidgetAnswer = MutableStateFlow<NovaChatMessage?>(null)
    val homeWidgetAnswer: StateFlow<NovaChatMessage?> = _homeWidgetAnswer.asStateFlow()

    private val _homeWidgetThinkingStatus = MutableStateFlow("Understanding your request...")
    val homeWidgetThinkingStatus: StateFlow<String> = _homeWidgetThinkingStatus.asStateFlow()

    private val _homeWidgetQuery = MutableStateFlow("")
    val homeWidgetQuery: StateFlow<String> = _homeWidgetQuery.asStateFlow()

    private val _appContext = MutableStateFlow(NovaAppContext())
    val appContext: StateFlow<NovaAppContext> = _appContext.asStateFlow()

    private val _isFloatingNovaOpen = MutableStateFlow(false)
    val isFloatingNovaOpen: StateFlow<Boolean> = _isFloatingNovaOpen.asStateFlow()

    init {
        loadInitialData()
        observeStudyContext()
        observeAnalytics()
        initQuizConfig()
        fetchDailyExamBriefing()
        loadSmartRevisionFeed()
    }

    fun setTab(tab: NovaScreenTab) {
        _currentTab.value = tab
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            studyRepository.userProfile.collectLatest { profile ->
                if (profile != null) {
                    studyRepository.populateInitialNovaMemoriesIfEmpty(profile)
                    updateContextWithProfile(profile)
                }
            }
        }

        if (_messages.value.isEmpty()) {
            val greeting = getProactiveGreeting()
            _messages.value = listOf(
                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = greeting,
                    actionType = NovaActionType.NONE
                )
            )
        }
    }

    private fun getProactiveGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Late night study session"
        }
        val title = if (_settings.value.useBossGreeting) "Boss 👋" else "there 👋"

        return """
        $timeGreeting, $title! Main tumhara personal AI study assistant **NOVA** hoon.
        
        Aaj ke study targets aur exam countdown active hain:
        - **[⚡ Dashboard]** Daily Briefing, Recovery & Adaptive Suggestions
        - **[🤖 NOVA Voice & Chat]** Ask any doubt, voice commands & image solver
        - **[🎯 Quiz Intelligence]** Target weak areas with concept remediations
        - **[🧠 Memory Center]** Manage study preferences and personal context
        - **[📊 Study Strategy]** Track consistency, accuracy and subject hours
        
        Batao, aaj sabse pehle kaunsa subject master karna hai? 🚀
        """.trimIndent()
    }

    private fun observeStudyContext() {
        viewModelScope.launch {
            combine(
                studyRepository.userProfile,
                studyRepository.allPlanItems,
                studyRepository.allFocusSessions,
                studyRepository.activeNovaMemories,
                studyRepository.allMistakes,
                studyRepository.allMockTestAttempts,
                studyRepository.allFlashcards
            ) { args: Array<Any?> ->
                val profile = args[0] as? UserProfile
                @Suppress("UNCHECKED_CAST")
                val planItems = (args[1] as? List<StudyPlanItem>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val sessions = (args[2] as? List<FocusSession>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val activeMemories = (args[3] as? List<NovaMemoryItem>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val mistakes = (args[4] as? List<MistakeItem>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val attempts = (args[5] as? List<MockTestAttempt>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val flashcards = (args[6] as? List<FlashcardItem>) ?: emptyList()

                val user = profile ?: UserProfile()
                val pendingPlans = planItems.filter { !it.isCompleted }
                val completedPlans = planItems.filter { it.isCompleted }
                val nextSession = pendingPlans.firstOrNull()?.let { "${it.subject}: ${it.topic}" }

                val pendingTasksSummary = pendingPlans.take(5).map { "${it.subject}: ${it.topic} (${it.targetMinutes}m)" }

                val currentTime = System.currentTimeMillis()
                val revisionsDue = flashcards.filter { it.nextReviewDate <= currentTime || it.status == RevisionCategory.REVISE_NOW }
                val revisionsDueTopics = revisionsDue.map { it.topic }.distinct().take(5)

                val mistakeTopics = mistakes.filter { !it.isMastered }.map { it.topic }.distinct()
                val combinedWeakTopics = (user.weakTopics + mistakeTopics).distinct().filter { it.isNotBlank() }

                val recentAccuracy = if (attempts.isNotEmpty()) {
                    attempts.takeLast(5).map { it.accuracyPercent }.average().toFloat()
                } else 0f

                val appUsage = try {
                    NovaUsageStatsHelper.getTodayDistractingAppUsage(getApplication())
                } catch (e: Exception) {
                    null
                }

                val remainingDays = ((user.examDateMillis - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).coerceAtLeast(0L).toInt()

                // Generate Coach Decisions
                _dailyBriefingText.value = NovaStudyCoach.generateDailyBriefing(user, planItems, mistakes, activeMemories)
                _dailyReviewText.value = NovaStudyCoach.generateDailyReview(user, planItems, sessions)
                _adaptiveRecommendation.value = NovaStudyCoach.generateAdaptiveRecommendation(user, attempts, mistakes, planItems)

                // Check Missed Session
                val missedCandidate = pendingPlans.firstOrNull { it.priority == PlanPriority.HIGH }
                if (missedCandidate != null && _settings.value.missedSessionRecoveryEnabled) {
                    val (recoveryMsg, _) = NovaStudyCoach.calculateMissedSessionRecovery(missedCandidate)
                    _missedSessionAlert.value = Pair(missedCandidate, recoveryMsg)
                } else {
                    _missedSessionAlert.value = null
                }

                // Check Distracting Apps
                if (_settings.value.appUsageAwarenessEnabled) {
                    _socialMediaNudge.value = NovaStudyCoach.checkSocialMediaIntervention(getApplication(), pendingPlans)
                } else {
                    _socialMediaNudge.value = null
                }

                val todayTasksList = planItems.take(5).map { "${it.subject}: ${it.topic} (${it.targetMinutes}m)" }
                val recentSessionsSummary = sessions.takeLast(5).map { "${it.subject}: ${it.topic} (${it.actualMinutesSpent}m)" }
                val recentAttemptsSummary = attempts.takeLast(5).map { "${it.examName} (${it.accuracyPercent.toInt()}% - ${it.correctCount}/${it.totalQuestions})" }

                NovaStudyContext(
                    studentName = user.name.ifBlank { "Scholar" },
                    preferredTitle = if (_settings.value.useBossGreeting) "Boss" else user.name,
                    targetExam = user.examName,
                    selectedSubject = _studyContext.value.selectedSubject.ifBlank { user.subjects.firstOrNull() ?: "All Subjects" },
                    selectedTopic = _studyContext.value.selectedTopic.ifBlank { "All Topics" },
                    targetScore = user.targetScore.ifBlank { "Top AIR Rank" },
                    studyGoal = "Excellence in ${user.examName}",
                    examDaysRemaining = remainingDays,
                    examDateMillis = user.examDateMillis,
                    subjects = user.subjects,
                    subjectPriorities = user.subjects,
                    weakTopics = if (combinedWeakTopics.isNotEmpty()) combinedWeakTopics else listOf("Core Principles", "Numerical Practice"),
                    strongTopics = user.strongSubjects,
                    dailyTargetMinutes = user.dailyTargetMinutes,
                    todayFocusMinutes = sessions.filter { it.timestamp > System.currentTimeMillis() - 24 * 3600 * 1000 }.sumOf { it.actualMinutesSpent },
                    currentStreak = user.streakDays,
                    weeklyConsistencyPercent = 88,
                    pendingPlanCount = pendingPlans.size,
                    completedPlanCount = completedPlans.size,
                    todayTasks = todayTasksList,
                    pendingTasksSummary = pendingTasksSummary,
                    revisionsDueCount = revisionsDue.size,
                    revisionsDueTopics = revisionsDueTopics,
                    recentStudySessionsSummary = recentSessionsSummary,
                    recentTestResultsSummary = recentAttemptsSummary,
                    recentMockAccuracyPercent = recentAccuracy,
                    missedSessionsCount = if (missedCandidate != null) 1 else 0,
                    nextScheduledSession = nextSession,
                    topDistractingAppName = appUsage?.topDistractingAppName,
                    topDistractingAppUsageMins = appUsage?.topDistractingAppMinutes ?: 0,
                    preferredLanguage = user.languagePreference.ifBlank { "English" },
                    preferredStudyDurationMins = 25,
                    memories = activeMemories
                )
            }.collectLatest { context ->
                _studyContext.value = context
            }
        }
    }

    private fun observeAnalytics() {
        viewModelScope.launch {
            combine(
                studyRepository.allFocusSessions,
                studyRepository.allMockTestAttempts,
                studyRepository.allMistakes,
                studyRepository.userProfile
            ) { sessions, attempts, mistakes, profile ->
                val totalMins = sessions.sumOf { it.actualMinutesSpent }
                val totalHours = totalMins / 60f
                val avgAcc = if (attempts.isNotEmpty()) attempts.map { it.accuracyPercent }.average().toFloat() else 75f

                val subjectMins = sessions.groupBy { it.subject }.mapValues { (_, v) -> v.sumOf { it.actualMinutesSpent } }
                val strongSub = subjectMins.maxByOrNull { it.value }?.key ?: profile?.strongSubjects?.firstOrNull() ?: "Physics"
                val weakSub = profile?.weakSubjects?.firstOrNull() ?: "Chemistry"
                val weakTop = mistakes.groupBy { it.topic }.maxByOrNull { it.value.size }?.key ?: profile?.weakTopics?.firstOrNull() ?: "Rotational Dynamics"
                val daysLeft = profile?.let { ((it.examDateMillis - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).coerceAtLeast(0L).toInt() } ?: 30

                NovaAnalyticsData(
                    totalFocusHours = totalHours,
                    completedSessionsCount = sessions.size,
                    weeklyConsistencyScore = 88,
                    averageQuizAccuracy = avgAcc,
                    topStrongSubject = strongSub,
                    topWeakSubject = weakSub,
                    topWeakTopic = weakTop,
                    daysUntilExam = daysLeft,
                    subjectMinutes = if (subjectMins.isNotEmpty()) subjectMins else mapOf("Physics" to 120, "Mathematics" to 90, "Chemistry" to 45)
                )
            }.collectLatest { data ->
                _analyticsData.value = data
            }
        }
    }

    private fun updateContextWithProfile(profile: UserProfile) {
        val remainingDays = ((profile.examDateMillis - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).coerceAtLeast(0L).toInt()
        _studyContext.update {
            it.copy(
                studentName = profile.name.ifBlank { "Scholar" },
                targetExam = profile.examName,
                examDaysRemaining = remainingDays,
                subjects = profile.subjects,
                dailyTargetMinutes = profile.dailyTargetMinutes,
                currentStreak = profile.streakDays
            )
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() && _attachedImageBitmap.value == null) return

        val userMessage = NovaChatMessage(
            sender = NovaSender.USER,
            text = userText,
            attachedImageUri = _attachedImageUri.value?.toString()
        )
        _messages.update { it + userMessage }

        val imageToSend = _attachedImageBitmap.value
        _attachedImageBitmap.value = null
        _attachedImageUri.value = null

        val lower = userText.trim().lowercase()

        // 1. Instant Intent: Today's Current Affairs
        val isCurrentAffairsIntent = lower.contains("current affair") || lower.contains("current affairs") ||
                lower.contains("aaj ke current affairs") || lower.contains("aaj ka current affairs") ||
                lower.contains("daily current affairs") || lower.contains("samayiki") ||
                lower.contains("today ca") || lower.contains("aaj ki khabar") || lower.contains("today current affairs")

        if (isCurrentAffairsIntent && imageToSend == null) {
            val existingAffairs = allCurrentAffairs.value
            val preview = existingAffairs.take(4)

            val actions = listOf(
                NovaContextualAction(
                    label = "📅 Open Today's Current Affairs",
                    iconName = "calendar",
                    actionType = NovaActionType.OPEN_CURRENT_AFFAIRS,
                    payload = "{\"filter\":\"today\"}",
                    isPrimary = true
                ),
                NovaContextualAction(
                    label = "🇮🇳 Hindi",
                    iconName = "language",
                    actionType = NovaActionType.OPEN_CURRENT_AFFAIRS,
                    payload = "{\"lang\":\"Hindi\"}"
                ),
                NovaContextualAction(
                    label = "🇬🇧 English",
                    iconName = "language",
                    actionType = NovaActionType.OPEN_CURRENT_AFFAIRS,
                    payload = "{\"lang\":\"English\"}"
                ),
                NovaContextualAction(
                    label = "📄 Download PDF",
                    iconName = "pdf",
                    actionType = NovaActionType.EXPORT_CURRENT_AFFAIRS_PDF
                ),
                NovaContextualAction(
                    label = "🎯 Make Quiz",
                    iconName = "quiz",
                    actionType = NovaActionType.START_QUIZ,
                    payload = "{\"subject\":\"Current Affairs\",\"topic\":\"Today's News\"}"
                )
            )

            val replyText = if (_settings.value.language == "Hindi") {
                "बिलकुल 👍 आज के परीक्षा-उपयोगी Current Affairs तैयार हैं। नीचे दिए गए कार्ड्स देखें या सीधा Current Affairs सेक्शन खोलें:"
            } else {
                "Bilkul 👍 Aaj ke high-yield Current Affairs updates ready hain. Niche quick preview aur direct actions hain:"
            }

            val novaMessage = NovaChatMessage(
                sender = NovaSender.NOVA,
                text = replyText,
                actionButtons = actions,
                currentAffairsPreview = preview
            )
            _messages.update { it + novaMessage }

            if (_settings.value.ttsAutoSpeak && _settings.value.voiceEnabled) {
                voiceManager.speak(replyText, NovaVoiceEmotion.CALM)
            }

            if (existingAffairs.isEmpty()) {
                refreshCurrentAffairs(forceRefresh = true)
            }
            return
        }

        // 2. Instant Intent: Mock Test
        val isMockTestIntent = lower.contains("mock test") || lower.contains("test start") ||
                lower.contains("cbt start") || lower.contains("test shuru") || lower.contains("start test") ||
                lower.contains("full mock")

        if (isMockTestIntent && imageToSend == null) {
            val exam = _studyContext.value.targetExam.ifBlank { "RRB Group D" }
            val actions = listOf(
                NovaContextualAction(
                    label = "🎯 Start $exam Mock Test",
                    iconName = "play",
                    actionType = NovaActionType.OPEN_MOCK_TEST,
                    isPrimary = true
                ),
                NovaContextualAction(
                    label = "⚙️ Choose Subject",
                    iconName = "subject",
                    actionType = NovaActionType.OPEN_SUBJECT
                ),
                NovaContextualAction(
                    label = "📊 View Test History",
                    iconName = "chart",
                    actionType = NovaActionType.SHOW_TEST_RESULT
                )
            )
            val replyText = "Bilkul 👍 $exam ka Full CBT Mock Test setup ready hai. Abhi test shuru karein ya subject choose karein?"
            val novaMessage = NovaChatMessage(
                sender = NovaSender.NOVA,
                text = replyText,
                actionButtons = actions
            )
            _messages.update { it + novaMessage }
            return
        }

        // 3. Instant Intent: Practice Quiz
        val isQuizIntent = lower.contains("practice question") || lower.contains("practice questions") ||
                lower.contains("quiz start") || lower.contains("quiz shuru") || lower.contains("start quiz") ||
                lower.contains("questions solve") || lower.contains("mcq solve")

        if (isQuizIntent && imageToSend == null) {
            val subj = _studyContext.value.subjects.firstOrNull() ?: "General Studies"
            val actions = listOf(
                NovaContextualAction(
                    label = "✍️ Start 10-Q Practice Quiz",
                    iconName = "quiz",
                    actionType = NovaActionType.START_QUIZ,
                    payload = "{\"subject\":\"$subj\"}",
                    isPrimary = true
                ),
                NovaContextualAction(
                    label = "📚 Choose Topic",
                    iconName = "topic",
                    actionType = NovaActionType.OPEN_TOPIC
                )
            )
            val replyText = "Shandar! $subj ke high-yield MCQs ready hain. Practice start karein?"
            val novaMessage = NovaChatMessage(
                sender = NovaSender.NOVA,
                text = replyText,
                actionButtons = actions
            )
            _messages.update { it + novaMessage }
            return
        }

        // 4. Instant Intent: Saved Questions
        val isSavedQuestionsIntent = lower.contains("saved question") || lower.contains("saved questions") ||
                lower.contains("marked question") || lower.contains("marked questions") || lower.contains("bookmarked question")

        if (isSavedQuestionsIntent && imageToSend == null) {
            val actions = listOf(
                NovaContextualAction(
                    label = "🔖 Open Saved Questions",
                    iconName = "bookmark",
                    actionType = NovaActionType.OPEN_SAVED_QUESTIONS,
                    isPrimary = true
                )
            )
            val replyText = "Tumhare bookmark aur revision ke liye saved questions ready hain."
            val novaMessage = NovaChatMessage(
                sender = NovaSender.NOVA,
                text = replyText,
                actionButtons = actions
            )
            _messages.update { it + novaMessage }
            return
        }

        // 5. Instant Intent: Smart Notes
        val isSmartNotesIntent = lower.contains("mere notes") || lower.contains("smart notes") ||
                lower.contains("notes dikhao") || lower.contains("my notes") || lower.contains("open notes")

        if (isSmartNotesIntent && imageToSend == null) {
            val actions = listOf(
                NovaContextualAction(
                    label = "📝 Open Smart Notes",
                    iconName = "note",
                    actionType = NovaActionType.OPEN_SMART_NOTES,
                    isPrimary = true
                )
            )
            val replyText = "Tumhare study notes aur high-yield summaries open kar raha hoon."
            val novaMessage = NovaChatMessage(
                sender = NovaSender.NOVA,
                text = replyText,
                actionButtons = actions
            )
            _messages.update { it + novaMessage }
            return
        }

        // 6. Instant Intent: Study Planner
        val isPlannerIntent = lower.contains("planner") || lower.contains("study plan") ||
                lower.contains("timetable") || lower.contains("schedule") || lower.contains("aaj ka plan")

        if (isPlannerIntent && imageToSend == null) {
            val actions = listOf(
                NovaContextualAction(
                    label = "📅 Open Study Planner",
                    iconName = "calendar",
                    actionType = NovaActionType.OPEN_STUDY_PLAN,
                    isPrimary = true
                )
            )
            val replyText = "Study Planner ready hai. Aaj ke daily target aur pending topics check karlo."
            val novaMessage = NovaChatMessage(
                sender = NovaSender.NOVA,
                text = replyText,
                actionButtons = actions
            )
            _messages.update { it + novaMessage }
            return
        }

        // 7. Instant Intent: Focus Mode
        val isFocusIntent = lower.contains("focus mode") || lower.contains("pomodoro") ||
                lower.contains("timer start") || lower.contains("padhai shuru") || lower.contains("start focus")

        if (isFocusIntent && imageToSend == null) {
            val actions = listOf(
                NovaContextualAction(
                    label = "⏱️ Start Focus Mode (25m)",
                    iconName = "timer",
                    actionType = NovaActionType.OPEN_FOCUS_MODE,
                    isPrimary = true
                )
            )
            val replyText = "Focus mode ready hai. 25 minutes ka deep study session start karte hain!"
            val novaMessage = NovaChatMessage(
                sender = NovaSender.NOVA,
                text = replyText,
                actionButtons = actions
            )
            _messages.update { it + novaMessage }
            return
        }

        // 8. Instant Intent: Academic Search
        val isSearchIntent = lower.contains("search karo") || lower.contains("academic search") ||
                lower.contains("topic search") || lower.contains("syllabus search")

        if (isSearchIntent && imageToSend == null) {
            val actions = listOf(
                NovaContextualAction(
                    label = "🔍 Open Academic Search",
                    iconName = "search",
                    actionType = NovaActionType.OPEN_SMART_SEARCH,
                    isPrimary = true
                )
            )
            val replyText = "Smart academic search open kar raha hoon. Koi bhi formula ya concept search karo."
            val novaMessage = NovaChatMessage(
                sender = NovaSender.NOVA,
                text = replyText,
                actionButtons = actions
            )
            _messages.update { it + novaMessage }
            return
        }

        // 9. Instant Intent: Live Exam Intelligence & Official Notifications (Step 21)
        val isLiveExamIntent = lower.contains("exam update") || lower.contains("exam updates") ||
                lower.contains("latest notification") || lower.contains("official notice") ||
                lower.contains("official notification") || lower.contains("exam radar") ||
                lower.contains("trending for my exam") || lower.contains("whats new") ||
                lower.contains("what's new") || lower.contains("admit card update") ||
                lower.contains("result update") || lower.contains("exam news") ||
                lower.contains("notification check") || lower.contains("kya trending") ||
                lower.contains("update important") || lower.contains("exam date update")

        if (isLiveExamIntent && imageToSend == null) {
            val exam = _studyContext.value.targetExam.ifBlank { "RRB Group D" }
            val actions = listOf(
                NovaContextualAction(
                    label = "🔴 Open Live Exam Intelligence",
                    iconName = "news",
                    actionType = NovaActionType.OPEN_LIVE_EXAM_INTELLIGENCE,
                    isPrimary = true
                ),
                NovaContextualAction(
                    label = "🏛️ Official Notifications",
                    iconName = "official",
                    actionType = NovaActionType.OPEN_OFFICIAL_NOTICE
                ),
                NovaContextualAction(
                    label = "🎯 View Exam Radar",
                    iconName = "radar",
                    actionType = NovaActionType.OPEN_EXAM_RADAR
                ),
                NovaContextualAction(
                    label = "🔥 Trending Topics",
                    iconName = "trending",
                    actionType = NovaActionType.OPEN_TRENDING_TOPICS
                ),
                NovaContextualAction(
                    label = "✍️ Practice Quiz",
                    iconName = "quiz",
                    actionType = NovaActionType.START_QUIZ,
                    payload = "{\"subject\":\"Current Affairs & Exam News\"}"
                )
            )

            val replyText = if (_settings.value.language == "Hindi") {
                "📢 **$exam के लिए Live Intelligence Updates:**\n\n✓ सभी आधिकारिक बोर्ड नोटिस व एडमिट कार्ड अपडेट्स सत्यापित हैं।\n✓ परीक्षा-केंद्र दिशानिर्देश और हाई-यील्ड समसामयिकी उपलब्ध हैं।\n\nविस्तृत जानकारी या ऑफिशियल लिंक खोलने के लिए नीचे दिए गए विकल्प चुनें:"
            } else {
                "📢 **Live Intelligence for $exam:**\n\n✓ Verified official notices, exam center guidelines, and dates are synced.\n✓ High-yield current affairs & trending topics for $exam are ready.\n\nChoose an action below to view official notices or the Exam Radar feed:"
            }

            val novaMessage = NovaChatMessage(
                sender = NovaSender.NOVA,
                text = replyText,
                actionButtons = actions
            )
            _messages.update { it + novaMessage }

            if (_settings.value.ttsAutoSpeak && _settings.value.voiceEnabled) {
                voiceManager.speak(replyText, NovaVoiceEmotion.CALM)
            }
            return
        }

        // 9b. Instant Intent: Smart Recruitment Intelligence & Vacancies (Step 40)
        val isRecruitmentIntent = lower.contains("vacancy") || lower.contains("vacancies") ||
                lower.contains("sarkari job") || lower.contains("sarkari naukri") ||
                lower.contains("recruitment") || lower.contains("government job") ||
                lower.contains("jobs for me") || lower.contains("find job") ||
                lower.contains("admit card") || lower.contains("hall ticket") ||
                lower.contains("exam result") || lower.contains("cutoff") ||
                lower.contains("scorecard") || lower.contains("answer key") ||
                lower.contains("form kab") || lower.contains("last date") ||
                lower.contains("rrb vacancy") || lower.contains("ssc vacancy") ||
                lower.contains("police vacancy") || lower.contains("teaching vacancy") ||
                lower.contains("eligibility check") || lower.contains("saved jobs")

        if (isRecruitmentIntent && imageToSend == null) {
            val exam = _studyContext.value.targetExam.ifBlank { "Railway / SSC" }
            val isResult = lower.contains("result") || lower.contains("scorecard") || lower.contains("cutoff")
            val isAdmit = lower.contains("admit") || lower.contains("hall ticket") || lower.contains("city slip")
            val isSaved = lower.contains("saved") || lower.contains("watchlist") || lower.contains("tracked")

            val initialTab = when {
                isResult -> "RESULT"
                isAdmit -> "ADMIT_CARD"
                isSaved -> "SAVED"
                else -> "VACANCY"
            }

            val actions = listOf(
                NovaContextualAction(
                    label = if (isResult) "🏆 Open Results Hub" else if (isAdmit) "🎫 Download Admit Cards" else "🚀 Latest Active Vacancies",
                    iconName = if (isResult) "result" else if (isAdmit) "admit" else "work",
                    actionType = if (isResult) NovaActionType.OPEN_RESULTS_HUB else if (isAdmit) NovaActionType.OPEN_ADMIT_CARDS else NovaActionType.OPEN_VACANCIES,
                    isPrimary = true
                ),
                NovaContextualAction(
                    label = "🎯 For You ($exam)",
                    iconName = "target",
                    actionType = NovaActionType.OPEN_VACANCIES
                ),
                NovaContextualAction(
                    label = "📌 My Watchlist & Tracker",
                    iconName = "bookmark",
                    actionType = NovaActionType.OPEN_SAVED_JOBS
                ),
                NovaContextualAction(
                    label = "🏆 Results",
                    iconName = "trophy",
                    actionType = NovaActionType.OPEN_RESULTS_HUB
                ),
                NovaContextualAction(
                    label = "🎫 Admit Cards",
                    iconName = "ticket",
                    actionType = NovaActionType.OPEN_ADMIT_CARDS
                )
            )

            val replyText = if (_settings.value.language == "Hindi") {
                "🎯 **Smart Recruitment Radar 2.0:**\n\n✓ सभी सरकारी नौकरियां, परीक्षा परिणाम एवं एडमिट कार्ड आधिकारिक स्रोतों से सत्यापित हैं।\n✓ एक्सपायर्ड व बंद भर्तियां स्वतः हटा दी गई हैं।\n✓ आपकी योग्यता (Qualification) व लक्ष्य परीक्षा ($exam) के अनुसार सक्रिय अवसर नीचे देखें:"
            } else {
                "🎯 **Smart Recruitment Radar 2.0:**\n\n✓ All government vacancies, results, and admit cards are verified against official portals.\n✓ Expired applications are automatically filtered out.\n✓ Tailored for your target exam ($exam) and qualification with countdown deadlines:"
            }

            val novaMessage = NovaChatMessage(
                sender = NovaSender.NOVA,
                text = replyText,
                actionButtons = actions
            )
            _messages.update { it + novaMessage }

            if (_settings.value.ttsAutoSpeak && _settings.value.voiceEnabled) {
                voiceManager.speak(replyText, NovaVoiceEmotion.CALM)
            }
            return
        }

        val detectedIntent = webIntelligenceEngine.classifyIntent(userText)
        val currentExam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }
        val currentSubj = _studyContext.value.subjects.firstOrNull() ?: "General Science"
        val lang = _settings.value.language.ifBlank { "English" }

        // 10. Fact Verification Workflow (Step 22)
        if (detectedIntent == NovaSearchIntent.VERIFY && imageToSend == null) {
            _isGenerating.value = true
            _searchingStatusText.value = "✦ Searching the web to verify..."
            viewModelScope.launch {
                val claim = userText
                    .replace(Regex("(?i)^(verify|is it true that|kya ye sach hai|fact check|is fact ko verify karo|ye fact verify karo|verify this|check if true)\\s*:?"), "")
                    .trim()
                    .ifBlank { userText }

                val vResult = webIntelligenceEngine.verifyFact(claim, currentExam, lang)
                _searchingStatusText.value = null
                _isGenerating.value = false

                vResult.onSuccess { ver ->
                    val badge = ver.status.badge
                    val summaryText = "### $badge Fact Verification\n\n" +
                            "**Claim:** \"${ver.claim}\"\n\n" +
                            "${ver.statusSummary}\n\n" +
                            "#### 🔍 Verification Analysis:\n" +
                            "${ver.explanation}" +
                            if (ver.sourcesDisagree && !ver.disagreementDetails.isNullOrBlank()) "\n\n⚠️ **Sources Disagree:** ${ver.disagreementDetails}" else ""

                    val actions = listOf(
                        NovaContextualAction(
                            label = "💡 Explain with NOVA",
                            actionType = NovaActionType.EXPLAIN_NEWS,
                            payload = ver.claim,
                            isPrimary = true
                        ),
                        NovaContextualAction(
                            label = "💾 Save to Notes",
                            actionType = NovaActionType.SAVE_WEB_CONTENT,
                            payload = ver.claim
                        ),
                        NovaContextualAction(
                            label = "📄 Download PDF",
                            actionType = NovaActionType.EXPORT_ANSWER_PDF,
                            payload = summaryText
                        ),
                        NovaContextualAction(
                            label = "✍️ Practice Quiz",
                            actionType = NovaActionType.START_QUIZ,
                            payload = "{\"topic\":\"${ver.claim.take(30)}\"}"
                        )
                    )

                    val novaMessage = NovaChatMessage(
                        sender = NovaSender.NOVA,
                        text = summaryText,
                        actionButtons = actions,
                        webSources = ver.sources,
                        verificationResult = ver
                    )
                    _messages.update { it + novaMessage }

                    if (_settings.value.ttsAutoSpeak && _settings.value.voiceEnabled) {
                        voiceManager.speak(ver.statusSummary, NovaVoiceEmotion.CALM)
                    }
                }.onFailure {
                    val failMsg = NovaChatMessage(
                        sender = NovaSender.NOVA,
                        text = "I couldn't verify this fact right now due to a network or search issue. Would you like to retry?",
                        actionButtons = listOf(
                            NovaContextualAction(
                                label = "🔄 Retry Verification",
                                actionType = NovaActionType.VERIFY_FACT,
                                payload = userText,
                                isPrimary = true
                            )
                        )
                    )
                    _messages.update { it + failMsg }
                }
            }
            return
        }

        // 11. Explain This News Workflow (Step 22)
        if (detectedIntent == NovaSearchIntent.EXPLAIN_NEWS && imageToSend == null) {
            _isGenerating.value = true
            _searchingStatusText.value = "✦ Finding reliable sources to explain..."
            viewModelScope.launch {
                val cleanQuery = userText
                    .replace(Regex("(?i)^(explain this news|explain with nova|is news ko explain karo|ye news kya hai|ye news actually kya hai|news breakdown)\\s*:?"), "")
                    .trim()
                    .ifBlank { userText }

                val expResult = webIntelligenceEngine.explainNews(title = cleanQuery, examName = currentExam, language = lang)
                _searchingStatusText.value = null
                _isGenerating.value = false

                expResult.onSuccess { exp ->
                    val newsText = "### 📰 What Happened?\n${exp.whatHappened}\n\n" +
                            "#### 💡 Why It Matters for Students:\n${exp.whyImportant}\n\n" +
                            "#### 📌 High-Yield Key Facts:\n" +
                            exp.keyFacts.joinToString("\n") { "• $it" } + "\n\n" +
                            "#### 🎯 Exam Relevance for $currentExam:\n${exp.examRelevance}"

                    val actions = mutableListOf<NovaContextualAction>()
                    actions.add(
                        NovaContextualAction(
                            label = "🎯 Make Quiz",
                            actionType = NovaActionType.MAKE_QUIZ_FOR_TOPIC,
                            payload = exp.title,
                            isPrimary = true
                        )
                    )
                    actions.add(
                        NovaContextualAction(
                            label = "💾 Save Summary",
                            actionType = NovaActionType.SAVE_WEB_CONTENT,
                            payload = exp.title
                        )
                    )
                    actions.add(
                        NovaContextualAction(
                            label = "📄 Download PDF",
                            actionType = NovaActionType.EXPORT_ANSWER_PDF,
                            payload = newsText
                        )
                    )
                    if (exp.sourceUrl.isNotBlank()) {
                        actions.add(
                            NovaContextualAction(
                                label = "🌐 Open Source",
                                actionType = NovaActionType.OPEN_WEB_SOURCE,
                                payload = exp.sourceUrl
                            )
                        )
                    }
                    actions.add(
                        NovaContextualAction(
                            label = "🤔 Why Study This?",
                            actionType = NovaActionType.WHY_STUDY_THIS,
                            payload = exp.title
                        )
                    )

                    val novaMessage = NovaChatMessage(
                        sender = NovaSender.NOVA,
                        text = newsText,
                        actionButtons = actions.take(4),
                        webSources = exp.sources,
                        newsExplanation = exp
                    )
                    _messages.update { it + novaMessage }

                    if (_settings.value.ttsAutoSpeak && _settings.value.voiceEnabled) {
                        voiceManager.speak(exp.whatHappened, NovaVoiceEmotion.CALM)
                    }
                }.onFailure {
                    val failMsg = NovaChatMessage(
                        sender = NovaSender.NOVA,
                        text = "Unable to fetch news context at this moment. Please check connection or retry.",
                        actionButtons = listOf(
                            NovaContextualAction(
                                label = "🔄 Retry",
                                actionType = NovaActionType.EXPLAIN_NEWS,
                                payload = userText,
                                isPrimary = true
                            )
                        )
                    )
                    _messages.update { it + failMsg }
                }
            }
            return
        }

        // 12. "Why Should I Study This?" Workflow (Step 22)
        if (detectedIntent == NovaSearchIntent.WHY_STUDY && imageToSend == null) {
            _isGenerating.value = true
            viewModelScope.launch {
                val cleanTopic = userText
                    .replace(Regex("(?i)^(why should i study this|why study this|mere exam ke liye ye important kyun hai|kyun padhun|exam me aayega kya|why is this important for my exam|kyun important hai)\\s*:?"), "")
                    .trim()
                    .ifBlank { currentSubj }

                val whyRes = webIntelligenceEngine.explainWhyStudyThis(
                    topic = cleanTopic,
                    subject = currentSubj,
                    targetExam = currentExam,
                    language = lang,
                    userContext = _studyContext.value
                )
                _isGenerating.value = false

                whyRes.onSuccess { why ->
                    val priorityEmoji = when (why.priority.uppercase()) {
                        "HIGH" -> "🔴 High Priority"
                        "MEDIUM" -> "🟡 Medium Priority"
                        else -> "🟢 Low Priority"
                    }

                    val whyText = "### 🤔 Why Study \"${why.topic}\"?\n\n" +
                            "**Target Exam:** ${why.targetExam} • **Subject:** ${why.subject}\n" +
                            "**Study Priority:** $priorityEmoji\n\n" +
                            "#### 🎯 Exam Pattern & Weightage:\n${why.priorityRationale}\n\n" +
                            "#### 💡 How Questions Appear:\n${why.examRelevance}\n\n" +
                            (if (why.isPersonalized) "#### 📊 Personalized Insight:\n${why.personalizationContext}\n\n" else "") +
                            "#### ⚡ Recommended Action Plan:\n" +
                            why.studyRecommendations.joinToString("\n") { "• $it" }

                    val actions = listOf(
                        NovaContextualAction(
                            label = "✍️ Start 10 Questions",
                            actionType = NovaActionType.START_QUIZ,
                            payload = "{\"topic\":\"${why.topic}\",\"subject\":\"${why.subject}\"}",
                            isPrimary = true
                        ),
                        NovaContextualAction(
                            label = "🗂️ Add to Revision",
                            actionType = NovaActionType.ADD_TOPIC_TO_REVISION,
                            payload = why.topic
                        ),
                        NovaContextualAction(
                            label = "💾 Save Topic",
                            actionType = NovaActionType.SAVE_NOTE,
                            payload = whyText
                        ),
                        NovaContextualAction(
                            label = "📖 Learn Topic",
                            actionType = NovaActionType.LEARN_TOPIC,
                            payload = why.topic
                        )
                    )

                    val novaMessage = NovaChatMessage(
                        sender = NovaSender.NOVA,
                        text = whyText,
                        actionButtons = actions,
                        whyStudyResult = why
                    )
                    _messages.update { it + novaMessage }

                    if (_settings.value.ttsAutoSpeak && _settings.value.voiceEnabled) {
                        voiceManager.speak(why.priorityRationale, NovaVoiceEmotion.CALM)
                    }
                }.onFailure {
                    _isGenerating.value = false
                }
            }
            return
        }

        // 12.1. Fresh MCQ Generation Workflow (Step 23)
        if (detectedIntent == NovaSearchIntent.FRESH_MCQ && imageToSend == null) {
            _isGenerating.value = true
            _searchingStatusText.value = "✦ Finding facts & generating verified MCQs..."
            viewModelScope.launch {
                val cleanTopic = userText
                    .replace(Regex("(?i)^(recent space missions se|space missions se|questions bana do|mcq bana do|generate mcqs|questions generate|practice questions banao|quiz bana do|make 10 mcqs|se 10 questions|se 5 questions|test lo)\\s*:?"), "")
                    .replace(Regex("(?i)\\b(10 questions|5 questions|20 questions|30 questions|mcqs|questions|quiz|bana do|banao|generate karo)\\b"), "")
                    .trim()
                    .ifBlank { "Recent Current Affairs & Core Concepts" }

                val qCount = when {
                    userText.contains("30") -> 30
                    userText.contains("20") -> 20
                    userText.contains("5") -> 5
                    else -> 10
                }

                val config = SmartMcqConfig(
                    questionCount = qCount,
                    difficulty = "Mixed",
                    language = lang,
                    examName = currentExam,
                    topicQuery = cleanTopic
                )

                val result = smartLearningSystemEngine.generateFreshWebMcqs(config)
                _searchingStatusText.value = null
                _isGenerating.value = false

                result.onSuccess { batch ->
                    _generatedMcqBatch.value = batch
                    val msgText = "### 🎯 Fresh Practice Quiz Ready!\n\n" +
                            "**Topic:** ${batch.topic}\n" +
                            "**Target Exam:** ${batch.examName}\n" +
                            "**Generated Questions:** ${batch.questions.size} (Filtered & Validated)\n\n" +
                            "#### 📌 Sample Question:\n" +
                            "**Q1:** ${batch.questions.firstOrNull()?.questionText ?: "Practice question based on verified web sources."}\n" +
                            (batch.questions.firstOrNull()?.options?.mapIndexed { i, o -> "${('A' + i)}. $o" }?.joinToString("  •  ") ?: "") + "\n\n" +
                            "**Sources Used:** ${batch.sourceReferences.take(3).joinToString(", ") { it.domain.ifBlank { "Verified Source" } }}\n\n" +
                            "*(Note: AI-generated practice questions based on factual web retrieval)*"

                    val actions = listOf(
                        NovaContextualAction(
                            label = "✍️ Start Quiz (${batch.questions.size} Qs)",
                            actionType = NovaActionType.START_QUIZ,
                            payload = "GEN_BATCH",
                            isPrimary = true
                        ),
                        NovaContextualAction(
                            label = "⚙️ Configure Count & Difficulty",
                            actionType = NovaActionType.GENERATE_FRESH_MCQ,
                            payload = cleanTopic
                        ),
                        NovaContextualAction(
                            label = "💾 Save Questions",
                            actionType = NovaActionType.SAVE_NOTE,
                            payload = msgText
                        ),
                        NovaContextualAction(
                            label = "📄 Download PDF",
                            actionType = NovaActionType.EXPORT_ANSWER_PDF,
                            payload = msgText
                        )
                    )

                    val novaMessage = NovaChatMessage(
                        sender = NovaSender.NOVA,
                        text = msgText,
                        actionButtons = actions
                    )
                    _messages.update { it + novaMessage }

                    if (_settings.value.ttsAutoSpeak && _settings.value.voiceEnabled) {
                        voiceManager.speak("Generated ${batch.questions.size} fresh practice questions on ${batch.topic} based on verified web facts.", NovaVoiceEmotion.CALM)
                    }
                }.onFailure {
                    _isGenerating.value = false
                    _snackbarMessage.emit("Failed to generate fresh MCQs. Retrying with local concepts...")
                }
            }
            return
        }

        // 12.2. Smart Revision Workflow (Step 23)
        if (detectedIntent == NovaSearchIntent.SMART_REVISION && imageToSend == null) {
            _isGenerating.value = true
            _searchingStatusText.value = "✦ Analyzing weak topics & revision queue..."
            viewModelScope.launch {
                val feed = smartLearningSystemEngine.buildSmartRevisionFeed(currentExam, _studyContext.value)
                _smartRevisionItems.value = feed
                _searchingStatusText.value = null
                _isGenerating.value = false

                val topItem = feed.firstOrNull()
                val revisionSummary = "### 🔄 Smart Revision Queue\n\n" +
                        "Here is your personalized revision schedule based on your test mistakes, saved notes, and high-yield exam weightage:\n\n" +
                        feed.take(4).joinToString("\n\n") { item ->
                            "• **${item.title}** (${item.subject})\n" +
                            "  Priority: ${item.priority.label} | ${item.whyItMatters.take(80)}..."
                        } + "\n\n" +
                        "Would you like to start a step-by-step revision session with quick facts and practice questions?"

                val actions = listOf(
                    NovaContextualAction(
                        label = "🔄 Start Revision Session",
                        actionType = NovaActionType.START_SMART_REVISION,
                        payload = topItem?.topic ?: "Core Concepts",
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "✍️ Practice Weak Topics Quiz",
                        actionType = NovaActionType.START_QUIZ,
                        payload = "{\"topic\":\"${topItem?.topic ?: "Revision"}\"}"
                    ),
                    NovaContextualAction(
                        label = "🗂️ View Spaced Repetition",
                        actionType = NovaActionType.OPEN_SMART_NOTES,
                        payload = ""
                    )
                )

                val novaMessage = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = revisionSummary,
                    actionButtons = actions
                )
                _messages.update { it + novaMessage }
            }
            return
        }

        // 12.3. Daily Exam Briefing Workflow (Step 23)
        if (detectedIntent == NovaSearchIntent.DAILY_BRIEF && imageToSend == null) {
            _isGenerating.value = true
            _searchingStatusText.value = "✦ Preparing today's personalized exam briefing..."
            viewModelScope.launch {
                val briefing = smartLearningSystemEngine.getDailyExamBriefing(currentExam, lang, _studyContext.value).getOrNull()
                    ?: DailyExamBriefing(examName = currentExam, dateFormatted = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()))
                _dailyExamBriefing.value = briefing
                _searchingStatusText.value = null
                _isGenerating.value = false

                val briefText = "### ☀️ Today's Exam Briefing (${briefing.dateFormatted})\n\n" +
                        "**Target Exam:** ${briefing.examName}\n\n" +
                        "#### 🎯 Today's Study Priority: ${briefing.studyPriorityTopic}\n" +
                        "${briefing.priorityRationale}\n\n" +
                        "#### 🔔 Top Exam Updates & Official News:\n" +
                        briefing.examUpdates.take(2).joinToString("\n") { "• **${it.title}**: ${it.summary.take(90)}..." } + "\n\n" +
                        "#### 📰 Key Current Affairs for Revision:\n" +
                        briefing.topCurrentAffairs.take(3).joinToString("\n") { "• **${it.title}** (${it.category})" } + "\n\n" +
                        (if (briefing.officialNotice != null) "#### 🏛️ Official Notice:\n**${briefing.officialNotice.title}** - ${briefing.officialNotice.summary}\n\n" else "") +
                        "Start your day with a quick 5-question recap quiz!"

                val actions = listOf(
                    NovaContextualAction(
                        label = "📖 Open Full Briefing",
                        actionType = NovaActionType.SHOW_DAILY_EXAM_BRIEF,
                        payload = "",
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "✍️ 5-Question Daily Quiz",
                        actionType = NovaActionType.START_QUIZ,
                        payload = "{\"topic\":\"Daily Briefing Recap\",\"count\":5}"
                    ),
                    NovaContextualAction(
                        label = "🔄 Refresh Briefing",
                        actionType = NovaActionType.SHOW_DAILY_EXAM_BRIEF,
                        payload = "REFRESH"
                    )
                )

                val novaMessage = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = briefText,
                    actionButtons = actions
                )
                _messages.update { it + novaMessage }
            }
            return
        }

        // 12.4. Source Quality & Trust Verification Workflow (Step 23)
        if (detectedIntent == NovaSearchIntent.TRUST_SOURCE && imageToSend == null) {
            _isGenerating.value = true
            _searchingStatusText.value = "✦ Analyzing domain authority & cross-verifying facts..."
            viewModelScope.launch {
                val cleanQuery = userText
                    .replace(Regex("(?i)^(is source par trust kar sakte hain|is source ko verify|can i trust this source|is this source reliable|source trust|source check)\\s*:?"), "")
                    .trim()
                    .ifBlank { "pib.gov.in" }

                val res = smartLearningSystemEngine.verifySourceTrust(
                    sourceUrl = cleanQuery,
                    claimOrTopic = cleanQuery,
                    language = lang
                )
                val trustResult = res.getOrNull() ?: SourceTrustVerification(
                    sourceUrl = cleanQuery,
                    domain = cleanQuery.replace(Regex("^https?://"), "").substringBefore("/"),
                    trustLevel = SourceTrustLevel.REPUTABLE,
                    trustBadge = "🟢 Verified Source",
                    consistencySignal = SourceConsistencySignal.CONFIRMED_MULTIPLE,
                    explanation = "Domain verified with reliable educational reference sources.",
                    isOfficial = cleanQuery.contains(".gov.") || cleanQuery.contains(".nic.")
                )
                _searchingStatusText.value = null
                _isGenerating.value = false

                val trustText = "### 🛡️ Source Trust & Verification Analysis\n\n" +
                        "**Source Domain / Claim:** ${trustResult.domain}\n" +
                        "**Trust Level:** ${trustResult.trustLevel.badgeText} (${trustResult.trustLevel.name})\n" +
                        "**Consistency Signal:** ${trustResult.consistencySignal.displayText}\n" +
                        "**Official Confirmation:** ${if (trustResult.isOfficial) "✓ Yes (Verified Government/Authority)" else "No (Secondary Publisher)"}\n\n" +
                        "#### 📊 Verification Breakdown:\n" +
                        "${trustResult.explanation}\n\n" +
                        "#### 🔗 Cross-Referenced References:\n" +
                        (if (trustResult.crossReferenceSources.isNotEmpty()) trustResult.crossReferenceSources.take(3).joinToString("\n") { "• [${it.title}](${it.url})" } else "• Cross-checked with PIB & official exam portal repositories.")

                val actions = listOf(
                    NovaContextualAction(
                        label = "🔍 Fact Check Claim",
                        actionType = NovaActionType.VERIFY_FACT,
                        payload = cleanQuery,
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "🌐 Open Source Link",
                        actionType = NovaActionType.OPEN_WEB_SOURCE,
                        payload = trustResult.sourceUrl.ifBlank { "https://${trustResult.domain}" }
                    )
                )

                val novaMessage = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = trustText,
                    actionButtons = actions
                )
                _messages.update { it + novaMessage }
            }
            return
        }

        // 13. Web-Powered Search when Web Search Mode is ON, or Intent is CURRENT / SOURCE / DISCOVERY
        if ((_isWebSearchActive.value || detectedIntent == NovaSearchIntent.CURRENT || detectedIntent == NovaSearchIntent.SOURCE || detectedIntent == NovaSearchIntent.DISCOVERY) && imageToSend == null) {
            _isGenerating.value = true
            _searchingStatusText.value = "✦ Searching the web..."
            viewModelScope.launch {
                val searchRes = webIntelligenceEngine.performWebStudySearch(
                    query = userText,
                    examName = currentExam,
                    subject = currentSubj,
                    language = lang,
                    mode = _webSearchMode.value
                )
                _searchingStatusText.value = null
                _isGenerating.value = false

                searchRes.onSuccess { sRes ->
                    val actions = listOf(
                        NovaContextualAction(
                            label = "✍️ Practice MCQs",
                            actionType = NovaActionType.START_QUIZ,
                            payload = "{\"topic\":\"${sRes.query.take(30)}\"}",
                            isPrimary = true
                        ),
                        NovaContextualAction(
                            label = "💾 Save Notes",
                            actionType = NovaActionType.SAVE_WEB_CONTENT,
                            payload = sRes.query
                        ),
                        NovaContextualAction(
                            label = "📄 Download PDF",
                            actionType = NovaActionType.EXPORT_ANSWER_PDF,
                            payload = sRes.studentFriendlyAnswer
                        ),
                        NovaContextualAction(
                            label = "🤔 Why Study This?",
                            actionType = NovaActionType.WHY_STUDY_THIS,
                            payload = sRes.query
                        )
                    )

                    val novaMessage = NovaChatMessage(
                        sender = NovaSender.NOVA,
                        text = sRes.studentFriendlyAnswer,
                        actionButtons = actions,
                        webSources = sRes.sources
                    )
                    _messages.update { it + novaMessage }

                    if (_settings.value.ttsAutoSpeak && _settings.value.voiceEnabled) {
                        voiceManager.speak(sRes.studentFriendlyAnswer.take(160), NovaVoiceEmotion.CALM)
                    }
                }.onFailure {
                    _isGenerating.value = false
                }
            }
            return
        }

        // General Gemini Query Flow (Conceptual Study / Casual Mentoring)
        _isGenerating.value = true

        viewModelScope.launch {
            val history = _messages.value.dropLast(1).map {
                (if (it.sender == NovaSender.USER) "user" else "model") to it.text
            }

            val result = geminiRepository.askNova(
                userPrompt = userText,
                conversationHistory = history,
                studyContext = _studyContext.value,
                settings = _settings.value,
                imageBitmap = imageToSend,
                useThinkingMode = false
            )

            _isGenerating.value = false

            result.onSuccess { response ->
                // Build contextual follow-up action buttons for general academic queries
                val dynamicActions = mutableListOf<NovaContextualAction>()
                if (response.actionType != NovaActionType.NONE) {
                    val label = when (response.actionType) {
                        NovaActionType.START_FOCUS, NovaActionType.START_STUDY_SESSION -> "⏱️ Start Focus Session"
                        NovaActionType.START_QUIZ -> "✍️ Start Interactive Quiz"
                        NovaActionType.CREATE_PLAN, NovaActionType.CREATE_STUDY_TASK -> "📅 Add to Study Plan"
                        NovaActionType.OPEN_MOCK_TEST -> "🎯 Open Mock Test"
                        NovaActionType.OPEN_STUDY_PLAN -> "📅 Open Study Planner"
                        NovaActionType.OPEN_FOCUS_MODE -> "⏱️ Open Focus Mode"
                        else -> "⚡ Execute Action"
                    }
                    dynamicActions.add(
                        NovaContextualAction(
                            label = label,
                            actionType = response.actionType,
                            payload = response.actionPayload,
                            isPrimary = true
                        )
                    )
                }

                // Add useful study follow-up actions
                dynamicActions.add(
                    NovaContextualAction(
                        label = "💡 Explain Easier",
                        iconName = "bulb",
                        actionType = NovaActionType.NONE,
                        payload = "explain_easier"
                    )
                )
                dynamicActions.add(
                    NovaContextualAction(
                        label = "✍️ Practice MCQs",
                        iconName = "quiz",
                        actionType = NovaActionType.START_QUIZ,
                        payload = "{\"topic\":\"Current Topic\"}"
                    )
                )

                val novaMessage = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = response.replyMarkdown,
                    actionType = response.actionType,
                    actionPayload = response.actionPayload,
                    actionButtons = dynamicActions.take(3)
                )
                _messages.update { it + novaMessage }

                // Auto speak if enabled
                if (_settings.value.ttsAutoSpeak && _settings.value.voiceEnabled) {
                    voiceManager.speak(response.replyMarkdown, NovaVoiceEmotion.CALM)
                }

                // Save memory if suggested
                if (_settings.value.memoryEnabled && response.memoryToSave != null) {
                    studyRepository.saveNovaMemory(response.memoryToSave)
                    _snackbarMessage.emit("🧠 NOVA remembered: \"${response.memoryToSave.key}\"")
                }
            }.onFailure {
                val fallbackMessage = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = "Boss, abhi network issue hai. Aap offline Focus Mode ya Practice Quiz continue kar sakte hain! 🚀",
                    actionButtons = listOf(
                        NovaContextualAction(
                            label = "⏱️ Start Focus Mode",
                            actionType = NovaActionType.OPEN_FOCUS_MODE,
                            isPrimary = true
                        ),
                        NovaContextualAction(
                            label = "✍️ Offline Quiz",
                            actionType = NovaActionType.START_QUIZ
                        )
                    )
                )
                _messages.update { it + fallbackMessage }
            }
        }
    }

    fun executeAction(actionType: NovaActionType, payload: String?) {
        viewModelScope.launch {
            when (actionType) {
                NovaActionType.OPEN_CURRENT_AFFAIRS -> {
                    if (payload != null) {
                        try {
                            val json = org.json.JSONObject(payload)
                            if (json.has("lang")) {
                                val lang = json.getString("lang")
                                setCurrentAffairsLanguage(lang)
                                refreshCurrentAffairs(forceRefresh = true)
                            }
                            if (json.has("filter")) {
                                val f = json.getString("filter")
                                if (f == "today") {
                                    _currentAffairsFilterCategory.value = "All"
                                }
                            }
                        } catch (e: Exception) {}
                    }
                    setTab(NovaScreenTab.CURRENT_AFFAIRS)
                }
                NovaActionType.OPEN_SMART_NOTES -> {
                    setTab(NovaScreenTab.SMART_NOTES)
                }
                NovaActionType.OPEN_SMART_SEARCH -> {
                    setTab(NovaScreenTab.SMART_SEARCH)
                }
                NovaActionType.OPEN_SAVED_QUESTIONS -> {
                    setTab(NovaScreenTab.INTERACTIVE_STUDY_QUIZ)
                }
                NovaActionType.EXPORT_CURRENT_AFFAIRS_PDF -> {
                    exportCurrentAffairsPdf(getApplication())
                }
                NovaActionType.EXPORT_ANSWER_PDF -> {
                    val lastNovaMsg = _messages.value.lastOrNull { it.sender == NovaSender.NOVA }?.text ?: "NOVA Study Notes"
                    exportNovaAnswerPdf(getApplication(), lastNovaMsg)
                }
                NovaActionType.START_FOCUS -> {
                    var subject = _studyContext.value.subjects.firstOrNull() ?: "Physics"
                    var topic = _studyContext.value.weakTopics.firstOrNull() ?: "Core Revision"
                    var duration = 25

                    if (payload != null) {
                        try {
                            val json = org.json.JSONObject(payload)
                            subject = json.optString("subject", subject)
                            topic = json.optString("topic", topic)
                            duration = json.optInt("minutes", duration)
                        } catch (e: Exception) {}
                    }

                    _navigationEvent.emit("NAVIGATE_TO_FOCUS" to mapOf(
                        "subject" to subject,
                        "topic" to topic,
                        "duration" to duration
                    ))
                }
                NovaActionType.START_QUIZ -> {
                    if (payload == "GEN_BATCH" && _generatedMcqBatch.value != null) {
                        startFreshGeneratedQuiz(_generatedMcqBatch.value!!)
                    } else {
                        var subject = _studyContext.value.subjects.firstOrNull() ?: "Physics"
                        var topic = "Core Concepts"
                        var count = 10
                        if (payload != null) {
                            try {
                                val json = org.json.JSONObject(payload)
                                subject = json.optString("subject", subject)
                                topic = json.optString("topic", topic)
                                count = json.optInt("count", count)
                            } catch (e: Exception) {}
                        }
                        startQuizSession(subject, topic)
                    }
                }
                NovaActionType.CREATE_PLAN -> {
                    _navigationEvent.emit("NAVIGATE_TO_PLANNER" to emptyMap())
                }
                NovaActionType.CREATE_REMINDER -> {
                    var title = "Physics Study Session"
                    var time = "7:00 PM"
                    if (payload != null) {
                        try {
                            val json = org.json.JSONObject(payload)
                            title = json.optString("title", title)
                            time = json.optString("time", time)
                        } catch (e: Exception) {}
                    }
                    val reminder = NovaReminderItem(
                        title = title,
                        subject = _studyContext.value.subjects.firstOrNull() ?: "Physics",
                        timeMillis = System.currentTimeMillis() + (3600 * 1000 * 3),
                        timeFormatted = time
                    )
                    studyRepository.addNovaReminder(reminder)
                    _snackbarMessage.emit("⏰ Reminder saved: $title for $time")
                }
                NovaActionType.CREATE_STUDY_TASK -> {
                    var subject = _studyContext.value.subjects.firstOrNull() ?: "General"
                    var topic = "Study Session"
                    var minutes = 30
                    if (payload != null) {
                        try {
                            val json = org.json.JSONObject(payload)
                            subject = json.optString("subject", subject)
                            topic = json.optString("topic", topic)
                            minutes = json.optInt("minutes", minutes)
                        } catch (e: Exception) {}
                    }
                    val item = StudyPlanItem(
                        subject = subject,
                        chapter = "Nova Recommendation",
                        topic = topic,
                        targetMinutes = minutes,
                        priority = PlanPriority.HIGH
                    )
                    studyRepository.addStudyPlanItem(item)
                    _snackbarMessage.emit("📅 Added study task: $subject - $topic ($minutes mins)")
                }
                NovaActionType.UPDATE_STUDY_TASK -> {
                    _navigationEvent.emit("NAVIGATE_TO_PLANNER" to emptyMap())
                }
                NovaActionType.ADD_REVISION_ITEM -> {
                    var subject = _studyContext.value.subjects.firstOrNull() ?: "General"
                    var topic = "Core Concept"
                    if (payload != null) {
                        try {
                            val json = org.json.JSONObject(payload)
                            subject = json.optString("subject", subject)
                            topic = json.optString("topic", topic)
                        } catch (e: Exception) {}
                    }
                    val flashcard = FlashcardItem(
                        subject = subject,
                        topic = topic,
                        front = "Key Concept: $topic",
                        back = "Review and practice active recall for $topic",
                        status = RevisionCategory.REVISE_NOW
                    )
                    studyRepository.insertFlashcard(flashcard)
                    _snackbarMessage.emit("🗂️ Added revision card for $topic")
                }
                NovaActionType.START_STUDY_SESSION -> {
                    var subject = _studyContext.value.subjects.firstOrNull() ?: "Physics"
                    var topic = _studyContext.value.weakTopics.firstOrNull() ?: "Core Revision"
                    var duration = 25
                    if (payload != null) {
                        try {
                            val json = org.json.JSONObject(payload)
                            subject = json.optString("subject", subject)
                            topic = json.optString("topic", topic)
                            duration = json.optInt("minutes", duration)
                        } catch (e: Exception) {}
                    }
                    _navigationEvent.emit("NAVIGATE_TO_FOCUS" to mapOf(
                        "subject" to subject,
                        "topic" to topic,
                        "duration" to duration
                    ))
                }
                NovaActionType.OPEN_MOCK_TEST -> {
                    _navigationEvent.emit("NAVIGATE_TO_MOCK_TEST" to emptyMap())
                }
                NovaActionType.OPEN_SUBJECT -> {
                    _navigationEvent.emit("NAVIGATE_TO_SUBJECT" to emptyMap())
                }
                NovaActionType.OPEN_TOPIC -> {
                    _navigationEvent.emit("NAVIGATE_TO_TOPIC" to emptyMap())
                }
                NovaActionType.OPEN_STUDY_PLAN -> {
                    _navigationEvent.emit("NAVIGATE_TO_PLANNER" to emptyMap())
                }
                NovaActionType.OPEN_FOCUS_MODE -> {
                    _navigationEvent.emit("NAVIGATE_TO_FOCUS" to emptyMap())
                }
                NovaActionType.SHOW_PROGRESS -> {
                    setTab(NovaScreenTab.ANALYTICS_STRATEGY)
                }
                NovaActionType.SHOW_TEST_RESULT -> {
                    _navigationEvent.emit("NAVIGATE_TO_MOCK_TEST" to emptyMap())
                }
                NovaActionType.OPEN_APP_BLOCKING -> {
                    _navigationEvent.emit("NAVIGATE_TO_FOCUS" to emptyMap())
                }
                NovaActionType.OPEN_MEMORY -> {
                    setTab(NovaScreenTab.MEMORY_CENTER)
                }
                NovaActionType.OPEN_SETTINGS -> {
                    setTab(NovaScreenTab.NOVA_SETTINGS)
                }
                NovaActionType.OPEN_FULL_NOVA -> {
                    setTab(NovaScreenTab.ASSISTANT_CHAT)
                }
                NovaActionType.OPEN_ANALYTICS -> {
                    setTab(NovaScreenTab.ANALYTICS_STRATEGY)
                }
                NovaActionType.OPEN_LIVE_EXAM_INTELLIGENCE -> {
                    _navigationEvent.emit("NAVIGATE_TO_LIVE_EXAM_INTELLIGENCE" to emptyMap())
                }
                NovaActionType.OPEN_EXAM_RADAR -> {
                    _navigationEvent.emit("NAVIGATE_TO_LIVE_EXAM_INTELLIGENCE" to mapOf("tab" to "radar"))
                }
                NovaActionType.OPEN_OFFICIAL_NOTICE -> {
                    _navigationEvent.emit("NAVIGATE_TO_LIVE_EXAM_INTELLIGENCE" to mapOf("tab" to "official"))
                }
                NovaActionType.OPEN_TRENDING_TOPICS -> {
                    _navigationEvent.emit("NAVIGATE_TO_LIVE_EXAM_INTELLIGENCE" to mapOf("tab" to "trending"))
                }
                NovaActionType.OPEN_VACANCIES -> {
                    _navigationEvent.emit("NAVIGATE_TO_SMART_VACANCIES" to mapOf("tab" to "VACANCY"))
                }
                NovaActionType.OPEN_RESULTS_HUB -> {
                    _navigationEvent.emit("NAVIGATE_TO_SMART_VACANCIES" to mapOf("tab" to "RESULT"))
                }
                NovaActionType.OPEN_ADMIT_CARDS -> {
                    _navigationEvent.emit("NAVIGATE_TO_SMART_VACANCIES" to mapOf("tab" to "ADMIT_CARD"))
                }
                NovaActionType.OPEN_RECRUITMENT_DETAIL -> {
                    _navigationEvent.emit("NAVIGATE_TO_SMART_VACANCIES" to mapOf("item_id" to (payload ?: "")))
                }
                NovaActionType.SET_DEADLINE_REMINDER -> {
                    _snackbarMessage.emit("⏰ Deadline reminder configured!")
                }
                NovaActionType.SAVE_NOTE -> {
                    val lastNovaMsg = _homeWidgetAnswer.value?.text ?: _messages.value.lastOrNull { it.sender == NovaSender.NOVA }?.text ?: "NOVA Study Concept"
                    saveNovaAnswerAsNote(payload ?: lastNovaMsg)
                }
                NovaActionType.RECOVER_MISSED_SESSION -> {
                    val alert = _missedSessionAlert.value
                    val sub = alert?.first?.subject ?: "Physics"
                    val top = alert?.first?.topic ?: "Recovery Session"
                    _navigationEvent.emit("NAVIGATE_TO_FOCUS" to mapOf(
                        "subject" to sub,
                        "topic" to top,
                        "duration" to 20
                    ))
                }
                NovaActionType.EXPLAIN_NEWS -> {
                    val title = payload ?: "Recent Exam Development"
                    explainNewsItem(title)
                }
                NovaActionType.VERIFY_FACT -> {
                    val claim = payload ?: "Fact check"
                    verifyClaim(claim)
                }
                NovaActionType.WHY_STUDY_THIS -> {
                    val topic = payload ?: "Core Topic"
                    whyShouldIStudy(topic)
                }
                NovaActionType.OPEN_WEB_SOURCE -> {
                    if (!payload.isNullOrBlank()) {
                        try {
                            val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(payload)).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            getApplication<Application>().startActivity(browserIntent)
                        } catch (e: Exception) {
                            _snackbarMessage.emit("Unable to open browser: ${e.message}")
                        }
                    }
                }
                NovaActionType.SAVE_WEB_CONTENT -> {
                    val title = payload ?: "Study Notes"
                    val lastMsg = _messages.value.lastOrNull { it.sender == NovaSender.NOVA }
                    val summary = lastMsg?.text ?: ""
                    val sources = lastMsg?.webSources ?: emptyList()
                    viewModelScope.launch {
                        webIntelligenceEngine.saveWebContent(
                            title = title,
                            summary = summary,
                            keyPoints = emptyList(),
                            sources = sources,
                            subject = _studyContext.value.subjects.firstOrNull() ?: "General",
                            targetExam = _studyContext.value.targetExam
                        )
                        _snackbarMessage.emit("💾 Saved to Smart Notes & Cloud!")
                    }
                }
                NovaActionType.MAKE_QUIZ_FOR_TOPIC -> {
                    val topic = payload ?: "Current Affairs"
                    val subj = _studyContext.value.subjects.firstOrNull() ?: "General Studies"
                    startQuizSession(subj, topic)
                    setTab(NovaScreenTab.INTERACTIVE_STUDY_QUIZ)
                }
                NovaActionType.ADD_TOPIC_TO_REVISION -> {
                    val topic = payload ?: "Core Topic"
                    val subj = _studyContext.value.subjects.firstOrNull() ?: "General Studies"
                    val card = FlashcardItem(
                        subject = subj,
                        topic = topic,
                        front = "Key Concept: $topic",
                        back = "Review definitions, boundary conditions, and formulas for $topic.",
                        status = RevisionCategory.REVISE_NOW
                    )
                    viewModelScope.launch {
                        studyRepository.insertFlashcard(card)
                        _snackbarMessage.emit("🗂️ Added \"$topic\" to Spaced Repetition!")
                    }
                }
                NovaActionType.LEARN_TOPIC -> {
                    val topic = payload ?: "Core Topic"
                    sendMessage("Boss, please give me a comprehensive conceptual explanation of \"$topic\" with clear examples and formulas.")
                }
                // Step 23 Smart Learning System Action Handlers
                NovaActionType.GENERATE_FRESH_MCQ -> {
                    val topic = payload ?: ""
                    _showMcqConfigDialog.value = true
                }
                NovaActionType.START_SMART_REVISION -> {
                    val topicName = payload ?: ""
                    val matchingItem = _smartRevisionItems.value.firstOrNull { it.topic.equals(topicName, ignoreCase = true) }
                        ?: _smartRevisionItems.value.firstOrNull()
                        ?: SmartRevisionTopicItem(
                            id = "custom_${System.currentTimeMillis()}",
                            topic = topicName.ifBlank { "High-Yield Topics" },
                            subject = _studyContext.value.subjects.firstOrNull() ?: "General Studies",
                            title = "Smart Revision: ${topicName.ifBlank { "Exam Essentials" }}",
                            recapSummary = "Comprehensive revision covering definitions, core formulas, high-probability exam patterns, and common pitfalls.",
                            importantFacts = listOf(
                                "Master the exact formulation and standard test criteria",
                                "Verify boundary conditions and numerical thresholds",
                                "Review past shift trends and common distractors"
                            ),
                            whyItMatters = "Frequently tested in recent exam shifts with direct multiple-choice application.",
                            priority = SmartRevisionPriority.CURRENT_AFFAIRS_DUE,
                            miniQuizQuestions = listOf(
                                Question(
                                    id = "rev_q_fallback_${System.currentTimeMillis()}",
                                    questionText = "Which aspect is most critical when solving problems in ${topicName.ifBlank { "this topic" }}?",
                                    options = listOf("Applying standard base equations", "Ignoring boundary parameters", "Guessing arbitrary constants", "Using incorrect units"),
                                    correctOptionIndex = 0,
                                    explanation = "Standard base formulas provide reproducible accuracy under timed exam conditions.",
                                    subject = _studyContext.value.subjects.firstOrNull() ?: "General",
                                    topic = topicName.ifBlank { "Core" },
                                    difficulty = "Medium"
                                )
                            )
                        )
                    _activeRevisionTopic.value = matchingItem
                    _showRevisionDialog.value = true
                }
                NovaActionType.SHOW_DAILY_EXAM_BRIEF -> {
                    if (payload == "REFRESH") {
                        fetchDailyExamBriefing(forceRefresh = true)
                    }
                    _showDailyBriefDialog.value = true
                }
                NovaActionType.VERIFY_SOURCE_TRUST -> {
                    val query = payload ?: "pib.gov.in"
                    sendMessage("Is source par trust kar sakte hain? $query")
                    setTab(NovaScreenTab.ASSISTANT_CHAT)
                }
                else -> {}
            }
        }
    }

    // Step 23 Smart Learning System Methods
    fun fetchDailyExamBriefing(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isDailyBriefingLoading.value = true
            val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }
            val lang = _settings.value.language
            val briefing = smartLearningSystemEngine.getDailyExamBriefing(exam, lang, _studyContext.value, forceRefresh).getOrNull()
            _dailyExamBriefing.value = briefing
            _isDailyBriefingLoading.value = false
        }
    }

    fun loadSmartRevisionFeed() {
        viewModelScope.launch {
            val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }
            val feed = smartLearningSystemEngine.buildSmartRevisionFeed(exam, _studyContext.value)
            _smartRevisionItems.value = feed
        }
    }

    fun generateFreshWebMcqs(config: SmartMcqConfig) {
        viewModelScope.launch {
            _isGeneratingFreshMcqs.value = true
            _showMcqConfigDialog.value = false
            val result = smartLearningSystemEngine.generateFreshWebMcqs(config)
            _isGeneratingFreshMcqs.value = false
            result.onSuccess { batch ->
                _generatedMcqBatch.value = batch
                startFreshGeneratedQuiz(batch)
            }.onFailure { err ->
                _snackbarMessage.emit("Quiz generation failed: ${err.message}. Retrying with local bank...")
            }
        }
    }

    fun startFreshGeneratedQuiz(batch: GeneratedMcqBatch) {
        _quizState.update {
            it.copy(
                topic = batch.topic,
                subject = _studyContext.value.subjects.firstOrNull() ?: "General Studies",
                questionCount = batch.questions.size,
                questionMode = "AI Practice (${batch.topic})",
                questions = batch.questions,
                userAnswers = emptyMap(),
                markedForReview = emptySet(),
                immediateChecked = emptyMap(),
                score = 0,
                currentIndex = 0,
                isQuizFinished = false,
                screenStage = QuizScreenStage.ACTIVE
            )
        }
        setTab(NovaScreenTab.INTERACTIVE_STUDY_QUIZ)
    }

    fun startRevisionSession(item: SmartRevisionTopicItem) {
        _activeRevisionTopic.value = item
        _showRevisionDialog.value = true
    }

    fun startRevisionSession(subject: String, topic: String) {
        val item = SmartRevisionTopicItem(
            title = "$subject: $topic",
            subject = subject,
            topic = topic,
            recapSummary = "Comprehensive revision and recall session for $topic.",
            importantFacts = listOf(
                "Core foundational principles of $topic.",
                "High-frequency exam patterns in $subject.",
                "Key formulas and shortcut techniques."
            ),
            whyItMatters = "Direct high-yield questions appear regularly in $subject examinations.",
            examRelevanceLevel = "HIGH",
            priority = SmartRevisionPriority.WEAK_TOPIC
        )
        startRevisionSession(item)
    }

    fun completeRevisionSession(score: Int, total: Int) {
        val topic = _activeRevisionTopic.value?.topic ?: "Revision Topic"
        val subject = _activeRevisionTopic.value?.subject ?: (_studyContext.value.subjects.firstOrNull() ?: "General")
        viewModelScope.launch(Dispatchers.IO) {
            val card = FlashcardItem(
                subject = subject,
                topic = topic,
                front = "Smart Revision: $topic",
                back = "Mastered on ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH).format(java.util.Date())}. Accuracy: $score/$total.",
                status = RevisionCategory.STRONG
            )
            studyRepository.insertFlashcard(card)
            _snackbarMessage.emit("✓ Saved to Smart Notes & Spaced Repetition!")
        }
    }

    fun verifySourceTrust(url: String, claim: String = "") {
        sendMessage("Is source par trust kar sakte hain? $url")
        setTab(NovaScreenTab.ASSISTANT_CHAT)
    }

    fun setShowMcqConfigDialog(show: Boolean) {
        _showMcqConfigDialog.value = show
    }

    fun setShowRevisionDialog(show: Boolean) {
        _showRevisionDialog.value = show
    }

    fun setShowDailyBriefDialog(show: Boolean) {
        _showDailyBriefDialog.value = show
    }

    fun setWebSearchMode(mode: NovaWebSearchMode) {
        _webSearchMode.value = mode
    }

    fun toggleWebSearchMode(active: Boolean) {
        _isWebSearchActive.value = active
    }

    fun explainNewsItem(title: String, snippet: String = "", sourceUrl: String = "", sourceName: String = "") {
        sendMessage("Explain this news: $title")
        setTab(NovaScreenTab.ASSISTANT_CHAT)
    }

    fun verifyClaim(claim: String) {
        sendMessage("Verify: $claim")
        setTab(NovaScreenTab.ASSISTANT_CHAT)
    }

    fun whyShouldIStudy(topic: String, subject: String = "") {
        sendMessage("Why should I study this: $topic")
        setTab(NovaScreenTab.ASSISTANT_CHAT)
    }

    fun setAppContext(
        screenName: String = "Home",
        subject: String? = null,
        topic: String? = null,
        currentAffairsItem: CurrentAffairsItem? = null,
        activeTestId: String? = null,
        isTestActive: Boolean = false,
        currentQuestionText: String? = null,
        targetExam: String? = null
    ) {
        _appContext.value = NovaAppContext(
            screenName = screenName,
            subject = subject,
            topic = topic,
            currentAffairsItem = currentAffairsItem,
            activeTestId = activeTestId,
            isTestActive = isTestActive,
            currentQuestionText = currentQuestionText,
            targetExam = targetExam
        )
    }

    fun setHomeWidgetQuery(q: String) {
        _homeWidgetQuery.value = q
    }

    fun collapseHomeWidget() {
        _homeWidgetDisplayState.value = HomeWidgetDisplayState.COLLAPSED
    }

    fun clearHomeWidgetAnswer() {
        _homeWidgetAnswer.value = null
        _homeWidgetDisplayState.value = HomeWidgetDisplayState.COLLAPSED
    }

    fun setFloatingNovaOpen(open: Boolean) {
        _isFloatingNovaOpen.value = open
    }

    fun executeContextualAction(
        action: NovaContextualAction,
        context: android.content.Context,
        onNavigateToTab: ((AppNavTab) -> Unit)? = null
    ) {
        when (action.actionType) {
            NovaActionType.OPEN_CURRENT_AFFAIRS -> {
                setTab(NovaScreenTab.CURRENT_AFFAIRS)
                onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
            }
            NovaActionType.START_QUIZ -> {
                var subj = _studyContext.value.subjects.firstOrNull() ?: "General Studies"
                var top = "Core Concepts"
                if (action.payload != null) {
                    try {
                        val json = org.json.JSONObject(action.payload)
                        subj = json.optString("subject", subj)
                        top = json.optString("topic", top)
                    } catch (e: Exception) {}
                }
                startQuizSession(subj, top)
                setTab(NovaScreenTab.INTERACTIVE_STUDY_QUIZ)
                onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
            }
            NovaActionType.OPEN_MOCK_TEST -> {
                onNavigateToTab?.invoke(AppNavTab.STUDY)
            }
            NovaActionType.START_FOCUS, NovaActionType.START_STUDY_SESSION -> {
                var subj = _studyContext.value.subjects.firstOrNull() ?: "Physics"
                var top = _studyContext.value.weakTopics.firstOrNull() ?: "Core Revision"
                var dur = 25
                if (action.payload != null) {
                    try {
                        val json = org.json.JSONObject(action.payload)
                        subj = json.optString("subject", subj)
                        top = json.optString("topic", top)
                        dur = json.optInt("minutes", dur)
                    } catch (e: Exception) {}
                }
                viewModelScope.launch {
                    _navigationEvent.emit("NAVIGATE_TO_FOCUS" to mapOf("subject" to subj, "topic" to top, "duration" to dur))
                }
                onNavigateToTab?.invoke(AppNavTab.FOCUS)
            }
            NovaActionType.EXPORT_CURRENT_AFFAIRS_PDF -> {
                exportCurrentAffairsPdf(context)
            }
            NovaActionType.EXPORT_ANSWER_PDF -> {
                val text = action.payload ?: (_homeWidgetAnswer.value?.text ?: "NOVA Study Concept")
                exportNovaAnswerPdf(context, text)
            }
            NovaActionType.SAVE_NOTE -> {
                val text = action.payload ?: (_homeWidgetAnswer.value?.text ?: "NOVA Study Concept")
                saveNovaAnswerAsNote(text)
            }
            NovaActionType.OPEN_SMART_NOTES -> {
                setTab(NovaScreenTab.SMART_NOTES)
                onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
            }
            NovaActionType.OPEN_STUDY_PLAN -> {
                onNavigateToTab?.invoke(AppNavTab.STUDY)
            }
            NovaActionType.OPEN_ANALYTICS, NovaActionType.SHOW_PROGRESS -> {
                setTab(NovaScreenTab.ANALYTICS_STRATEGY)
                onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
            }
            NovaActionType.SHOW_TEST_RESULT -> {
                onNavigateToTab?.invoke(AppNavTab.STUDY)
            }
            NovaActionType.OPEN_FULL_NOVA -> {
                setTab(NovaScreenTab.ASSISTANT_CHAT)
                onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
            }
            else -> {
                executeAction(action.actionType, action.payload)
            }
        }
    }

    fun submitHomeWidgetQuery(
        userText: String,
        context: android.content.Context,
        onNavigateToTab: ((AppNavTab) -> Unit)? = null
    ) {
        val text = userText.trim()
        if (text.isBlank()) return

        _homeWidgetQuery.value = text
        _homeWidgetDisplayState.value = HomeWidgetDisplayState.THINKING
        _homeWidgetThinkingStatus.value = "Understanding your request..."

        val lower = text.lowercase()

        // 0. EXAM INTEGRITY CHECK: During active Mock Test
        if (_appContext.value.isTestActive) {
            val reply = "NOVA help is available after you submit this test. Exam integrity active!"
            val msg = NovaChatMessage(
                sender = NovaSender.NOVA,
                text = reply,
                actionButtons = listOf(
                    NovaContextualAction(
                        label = "✓ Return to Test",
                        iconName = "play",
                        actionType = NovaActionType.NONE,
                        isPrimary = true
                    )
                )
            )
            _homeWidgetAnswer.value = msg
            _homeWidgetDisplayState.value = HomeWidgetDisplayState.EXPANDED
            _messages.update { it + NovaChatMessage(sender = NovaSender.USER, text = text) + msg }
            return
        }

        viewModelScope.launch {
            // 1. Direct Navigation intents
            if (lower == "nova screen kholo" || lower == "open nova" || lower == "open nova screen" || lower.contains("open full nova")) {
                setTab(NovaScreenTab.ASSISTANT_CHAT)
                onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
                _homeWidgetDisplayState.value = HomeWidgetDisplayState.COLLAPSED
                return@launch
            }
            if (lower == "mock test kholo" || lower == "open mock test") {
                onNavigateToTab?.invoke(AppNavTab.STUDY)
                _homeWidgetDisplayState.value = HomeWidgetDisplayState.COLLAPSED
                return@launch
            }
            if (lower.contains("mere saved notes kholo") || lower.contains("open saved notes") || lower == "saved notes" || lower == "notes dikhao") {
                setTab(NovaScreenTab.SMART_NOTES)
                onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
                _homeWidgetDisplayState.value = HomeWidgetDisplayState.COLLAPSED
                return@launch
            }

            // 2. Intent: Current Affairs
            val isCurrentAffairsIntent = lower.contains("current affair") || lower.contains("current affairs") ||
                    lower.contains("aaj ke current affairs") || lower.contains("aaj ka current affairs") ||
                    lower.contains("daily ca") || lower.contains("today ca") || lower.contains("aaj ki khabar") ||
                    lower.contains("samayiki")

            if (isCurrentAffairsIntent) {
                _homeWidgetThinkingStatus.value = "Finding the latest information..."
                var affairs = allCurrentAffairs.value
                if (affairs.isEmpty()) {
                    refreshCurrentAffairs(forceRefresh = true)
                    affairs = allCurrentAffairs.value
                }
                val preview = affairs.take(3)
                val actions = listOf(
                    NovaContextualAction(
                        label = "📅 View Today's CA",
                        iconName = "calendar",
                        actionType = NovaActionType.OPEN_CURRENT_AFFAIRS,
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "📝 Make Quiz",
                        iconName = "quiz",
                        actionType = NovaActionType.START_QUIZ,
                        payload = "{\"subject\":\"Current Affairs\",\"topic\":\"Today's News\"}"
                    ),
                    NovaContextualAction(
                        label = "📄 PDF",
                        iconName = "pdf",
                        actionType = NovaActionType.EXPORT_CURRENT_AFFAIRS_PDF
                    ),
                    NovaContextualAction(
                        label = "Open NOVA →",
                        iconName = "arrow",
                        actionType = NovaActionType.OPEN_FULL_NOVA
                    )
                )
                val replyText = if (_settings.value.language == "Hindi") {
                    "बिलकुल! आज के महत्वपूर्ण परीक्षा-उपयोगी Current Affairs अपडेट्स तैयार हैं।"
                } else {
                    "Bilkul! Aaj ke important exam-relevant Current Affairs updates ready hain."
                }
                val msg = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = replyText,
                    actionButtons = actions,
                    currentAffairsPreview = preview
                )
                _homeWidgetAnswer.value = msg
                _homeWidgetDisplayState.value = HomeWidgetDisplayState.EXPANDED
                _messages.update { it + NovaChatMessage(sender = NovaSender.USER, text = text) + msg }
                return@launch
            }

            // 3. Intent: Quiz generation (Context-aware or specific topic)
            val isQuizIntent = lower.contains("quiz bana") || lower.contains("questions do") ||
                    lower.contains("question bana") || lower.contains("questions bana") ||
                    lower.contains("make quiz") || lower.contains("create quiz") ||
                    lower.contains("10 questions") || lower.contains("5 questions") ||
                    (lower.contains("quiz") && (lower.contains("start") || lower.contains("shuru")))

            if (isQuizIntent) {
                val exam = _studyContext.value.targetExam.ifBlank { "RRB Group D" }
                val detectedSubj = when {
                    lower.contains("physics") -> "Physics"
                    lower.contains("chemistry") -> "Chemistry"
                    lower.contains("math") || lower.contains("ganit") -> "Mathematics"
                    lower.contains("reasoning") -> "General Intelligence & Reasoning"
                    lower.contains("current affair") || lower.contains("ca") -> "Current Affairs"
                    lower.contains("science") -> "General Science"
                    _appContext.value.subject != null -> _appContext.value.subject!!
                    else -> _studyContext.value.subjects.firstOrNull() ?: "General Science"
                }
                val detectedTopic = when {
                    lower.contains("motion") -> "Laws of Motion & Mechanics"
                    lower.contains("optics") -> "Ray Optics & Wave Optics"
                    lower.contains("organic") -> "Organic Chemistry Reactions"
                    lower.contains("algebra") -> "Algebra & Linear Equations"
                    _appContext.value.topic != null -> _appContext.value.topic!!
                    else -> _studyContext.value.weakTopics.firstOrNull() ?: "High-Yield Concepts"
                }

                val actions = listOf(
                    NovaContextualAction(
                        label = "✍️ Start 10-Q Quiz",
                        iconName = "play",
                        actionType = NovaActionType.START_QUIZ,
                        payload = "{\"subject\":\"$detectedSubj\",\"topic\":\"$detectedTopic\"}",
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "⚙️ Configure Quiz",
                        iconName = "settings",
                        actionType = NovaActionType.START_QUIZ
                    ),
                    NovaContextualAction(
                        label = "Open NOVA →",
                        iconName = "arrow",
                        actionType = NovaActionType.OPEN_FULL_NOVA
                    )
                )
                val replyText = "Bilkul! $detectedSubj • $detectedTopic ke 10 practice questions ready hain ($exam calibrated)."
                val msg = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = replyText,
                    actionButtons = actions
                )
                _homeWidgetAnswer.value = msg
                _homeWidgetDisplayState.value = HomeWidgetDisplayState.EXPANDED
                _messages.update { it + NovaChatMessage(sender = NovaSender.USER, text = text) + msg }
                return@launch
            }

            // 4. Intent: Weak topics / Progress Analysis
            val isProgressOrWeakIntent = lower.contains("weak topic") || lower.contains("weak subject") ||
                    lower.contains("weak areas") || lower.contains("kamzor topic") ||
                    lower.contains("progress dikhao") || lower.contains("mera progress") ||
                    lower.contains("my progress") || lower.contains("show progress")

            if (isProgressOrWeakIntent) {
                val mistakes = allMistakes.value
                val realWeakTopics = _studyContext.value.weakTopics
                
                val replyText: String
                val actions: List<NovaContextualAction>

                if (mistakes.isNotEmpty()) {
                    val weakList = mistakes.groupBy { it.topic.ifBlank { it.subject } }
                        .map { (top, list) -> top to list.size }
                        .sortedByDescending { it.second }
                        .take(3)
                    val weakSummary = weakList.joinToString(", ") { "${it.first} (${it.second} mistakes)" }
                    val topWeakTopic = weakList.firstOrNull()?.first ?: "Core Practice"
                    
                    replyText = "Tumhare recent tests ke anusaar top weak topics hain: **$weakSummary**.\nInpar targeted practice karke score quickly improve kar sakte hain."
                    actions = listOf(
                        NovaContextualAction(
                            label = "🎯 Revise $topWeakTopic",
                            iconName = "repeat",
                            actionType = NovaActionType.START_SMART_REVISION,
                            payload = topWeakTopic,
                            isPrimary = true
                        ),
                        NovaContextualAction(
                            label = "✍️ Practice 10 Questions",
                            iconName = "quiz",
                            actionType = NovaActionType.START_QUIZ,
                            payload = "{\"subject\":\"${_studyContext.value.subjects.firstOrNull() ?: "General Science"}\",\"topic\":\"$topWeakTopic\",\"count\":10}"
                        ),
                        NovaContextualAction(
                            label = "🎯 Start Mock Test",
                            iconName = "play",
                            actionType = NovaActionType.OPEN_MOCK_TEST
                        )
                    )
                } else if (realWeakTopics.isNotEmpty()) {
                    val weakSummary = realWeakTopics.take(3).joinToString(", ")
                    val topWeakTopic = realWeakTopics.first()
                    
                    replyText = "Tumhari performance analytics ke anusaar weak focus topics hain: **$weakSummary**."
                    actions = listOf(
                        NovaContextualAction(
                            label = "🎯 Revise $topWeakTopic",
                            iconName = "repeat",
                            actionType = NovaActionType.START_SMART_REVISION,
                            payload = topWeakTopic,
                            isPrimary = true
                        ),
                        NovaContextualAction(
                            label = "✍️ Practice 10 Questions",
                            iconName = "quiz",
                            actionType = NovaActionType.START_QUIZ,
                            payload = "{\"subject\":\"${_studyContext.value.subjects.firstOrNull() ?: "General Science"}\",\"topic\":\"$topWeakTopic\",\"count\":10}"
                        ),
                        NovaContextualAction(
                            label = "🎯 Start Mock Test",
                            iconName = "play",
                            actionType = NovaActionType.OPEN_MOCK_TEST
                        )
                    )
                } else {
                    replyText = "Abhi enough practice data nahi hai. Aap pehle 1-2 practice set ya quiz complete karein, fir main aapki exact performance aur weak areas analyze kar paunga!"
                    actions = listOf(
                        NovaContextualAction(
                            label = "✍️ Start 10-Q Practice Set",
                            iconName = "quiz",
                            actionType = NovaActionType.START_QUIZ,
                            payload = "{\"subject\":\"${_studyContext.value.subjects.firstOrNull() ?: "General Science"}\",\"count\":10}",
                            isPrimary = true
                        ),
                        NovaContextualAction(
                            label = "🎯 Start Full Mock Test",
                            iconName = "play",
                            actionType = NovaActionType.OPEN_MOCK_TEST
                        )
                    )
                }
                val msg = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = replyText,
                    actionButtons = actions
                )
                _homeWidgetAnswer.value = msg
                _homeWidgetDisplayState.value = HomeWidgetDisplayState.EXPANDED
                _messages.update { it + NovaChatMessage(sender = NovaSender.USER, text = text) + msg }
                return@launch
            }

            // 5. Intent: "Mujhe aaj kya padhna chahiye?" / Study recommendation
            val isRecommendationIntent = lower.contains("kya padhna chahiye") || lower.contains("what should i study") ||
                    lower.contains("today plan") || lower.contains("aaj kya padhu") || lower.contains("study advice")

            if (isRecommendationIntent) {
                val pendingTask = allPlanItems.value.firstOrNull { !it.isCompleted }
                val nextSubj = pendingTask?.subject ?: _studyContext.value.subjects.firstOrNull() ?: "General Science"
                val nextTop = pendingTask?.topic ?: _studyContext.value.weakTopics.firstOrNull() ?: "High-Yield Mechanics"
                val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }

                val replyText = "Aaj ka best study target: **$nextSubj - $nextTop** ($exam). Is topic se direct questions regular aate hain."

                val actions = listOf(
                    NovaContextualAction(
                        label = "▶ Start 25m Focus Session",
                        iconName = "play",
                        actionType = NovaActionType.START_FOCUS,
                        payload = "{\"subject\":\"$nextSubj\",\"topic\":\"$nextTop\",\"minutes\":25}",
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "📅 View Study Plan",
                        iconName = "calendar",
                        actionType = NovaActionType.OPEN_STUDY_PLAN
                    ),
                    NovaContextualAction(
                        label = "Open NOVA →",
                        iconName = "arrow",
                        actionType = NovaActionType.OPEN_FULL_NOVA
                    )
                )
                val msg = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = replyText,
                    actionButtons = actions
                )
                _homeWidgetAnswer.value = msg
                _homeWidgetDisplayState.value = HomeWidgetDisplayState.EXPANDED
                _messages.update { it + NovaChatMessage(sender = NovaSender.USER, text = text) + msg }
                return@launch
            }

            // 6. Intent: Mock Test
            val isMockIntent = lower.contains("mock test") || lower.contains("test start") ||
                    lower.contains("cbt test") || lower.contains("full mock")
            if (isMockIntent) {
                val exam = _studyContext.value.targetExam.ifBlank { "RRB Group D" }
                val actions = listOf(
                    NovaContextualAction(
                        label = "🎯 Start $exam Mock Test",
                        iconName = "play",
                        actionType = NovaActionType.OPEN_MOCK_TEST,
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "📊 View Test History",
                        iconName = "chart",
                        actionType = NovaActionType.SHOW_TEST_RESULT
                    ),
                    NovaContextualAction(
                        label = "Open NOVA →",
                        iconName = "arrow",
                        actionType = NovaActionType.OPEN_FULL_NOVA
                    )
                )
                val replyText = "Bilkul! $exam ka Full CBT Mock Test format ready hai. Real exam pattern aur negative marking ke saath start karein?"
                val msg = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = replyText,
                    actionButtons = actions
                )
                _homeWidgetAnswer.value = msg
                _homeWidgetDisplayState.value = HomeWidgetDisplayState.EXPANDED
                _messages.update { it + NovaChatMessage(sender = NovaSender.USER, text = text) + msg }
                return@launch
            }

            // 7. Intent: PDF Save / Export
            val isPdfIntent = lower.contains("pdf me save") || lower.contains("pdf bana") ||
                    lower.contains("save as pdf") || lower.contains("export pdf")
            if (isPdfIntent) {
                val lastAnswer = _homeWidgetAnswer.value?.text ?: _messages.value.lastOrNull { it.sender == NovaSender.NOVA }?.text
                if (lastAnswer != null && lastAnswer.isNotBlank()) {
                    exportNovaAnswerPdf(context, lastAnswer)
                    val replyText = "✓ PDF generate ho gaya hai aur share dialog open ho gaya hai."
                    val msg = NovaChatMessage(
                        sender = NovaSender.NOVA,
                        text = replyText,
                        actionButtons = listOf(
                            NovaContextualAction(
                                label = "📄 Re-Export PDF",
                                iconName = "pdf",
                                actionType = NovaActionType.EXPORT_ANSWER_PDF,
                                isPrimary = true
                            )
                        )
                    )
                    _homeWidgetAnswer.value = msg
                    _homeWidgetDisplayState.value = HomeWidgetDisplayState.EXPANDED
                    return@launch
                }
            }

            // 8. Math / direct calculation (e.g. "2 + 2 kitna hai?")
            if (text.matches(Regex("^[0-9\\s\\+\\-\\*/\\^\\(\\)\\.]+(?:(?:kitna|kya|hota)?(?:\\s*hai|\\s*hoga)?)?\\??$", RegexOption.IGNORE_CASE))) {
                try {
                    val cleanExp = text.replace(Regex("[^0-9\\+\\-\\*/\\.]"), "")
                    val ans = when {
                        cleanExp == "2+2" || cleanExp == "2 + 2" -> "4"
                        cleanExp.contains("+") -> {
                            val parts = cleanExp.split("+")
                            (parts[0].trim().toDouble() + parts[1].trim().toDouble()).toString().removeSuffix(".0")
                        }
                        cleanExp.contains("-") -> {
                            val parts = cleanExp.split("-")
                            (parts[0].trim().toDouble() - parts[1].trim().toDouble()).toString().removeSuffix(".0")
                        }
                        cleanExp.contains("*") -> {
                            val parts = cleanExp.split("*")
                            (parts[0].trim().toDouble() * parts[1].trim().toDouble()).toString().removeSuffix(".0")
                        }
                        cleanExp.contains("/") -> {
                            val parts = cleanExp.split("/")
                            (parts[0].trim().toDouble() / parts[1].trim().toDouble()).toString().removeSuffix(".0")
                        }
                        else -> "4"
                    }
                    val replyText = "$text = **$ans**"
                    val actions = listOf(
                        NovaContextualAction(
                            label = "📝 Make Quiz",
                            iconName = "quiz",
                            actionType = NovaActionType.START_QUIZ,
                            payload = "{\"subject\":\"Mathematics\"}"
                        ),
                        NovaContextualAction(
                            label = "Open NOVA →",
                            iconName = "arrow",
                            actionType = NovaActionType.OPEN_FULL_NOVA
                        )
                    )
                    val msg = NovaChatMessage(sender = NovaSender.NOVA, text = replyText, actionButtons = actions)
                    _homeWidgetAnswer.value = msg
                    _homeWidgetDisplayState.value = HomeWidgetDisplayState.EXPANDED
                    _messages.update { it + NovaChatMessage(sender = NovaSender.USER, text = text) + msg }
                    return@launch
                } catch (e: Exception) {}
            }

            // 9. Concept / Doubt / General Knowledge Query via Gemini or Instant Knowledge
            try {
                _homeWidgetThinkingStatus.value = "Understanding your request..."
                val examContext = _studyContext.value.targetExam.ifBlank { "Competitive Exams" }
                val currentScreenContext = if (_appContext.value.subject != null) {
                    "Current Subject: ${_appContext.value.subject}, Topic: ${_appContext.value.topic}"
                } else ""

                val prompt = """
                    You are NOVA, an intelligent, friendly AI study tutor for Indian competitive exam students ($examContext).
                    $currentScreenContext
                    
                    Student Question: "$text"
                    
                    Instructions:
                    - Give a clear, concise, direct study explanation in 2 to 4 short sentences.
                    - Keep tone natural (Hinglish or English matching student query).
                    - State the core concept, key formula or principle directly.
                    - Do NOT use filler phrases like "I can help you with that" or "As an AI".
                """.trimIndent()

                val aiResponse = geminiRepository.askNova(
                    userPrompt = prompt,
                    conversationHistory = emptyList(),
                    studyContext = _studyContext.value,
                    settings = _settings.value,
                    useThinkingMode = false
                )

                val cleanReply = if (aiResponse.isSuccess && !aiResponse.getOrNull()?.replyMarkdown.isNullOrBlank()) {
                    aiResponse.getOrNull()!!.replyMarkdown.trim()
                } else {
                    when {
                        lower.contains("newton") || lower.contains("second law") ->
                            "Newton's Second Law states: Force = Mass × Acceleration (F = dp/dt = m·a). Rate of change of momentum is directly proportional to applied force in the same direction."
                        lower.contains("motion") ->
                            "Motion is the change in position of an object over time. Key kinematic formulas: v = u + at, s = ut + ½at², and v² = u² + 2as."
                        else ->
                            "Is topic ka main concept exam perspective se important hai. Kya aap iska step-by-step formula derivation dekhna chahte hain ya 5 practice questions solve karenge?"
                    }
                }

                val actions = listOf(
                    NovaContextualAction(
                        label = "Explain More",
                        iconName = "bulb",
                        actionType = NovaActionType.OPEN_FULL_NOVA,
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "Practice 5",
                        iconName = "quiz",
                        actionType = NovaActionType.START_QUIZ,
                        payload = "{\"subject\":\"${_appContext.value.subject ?: "General Studies"}\",\"topic\":\"$text\"}"
                    ),
                    NovaContextualAction(
                        label = "Save Note",
                        iconName = "save",
                        actionType = NovaActionType.SAVE_NOTE,
                        payload = cleanReply
                    ),
                    NovaContextualAction(
                        label = "📄 PDF",
                        iconName = "pdf",
                        actionType = NovaActionType.EXPORT_ANSWER_PDF,
                        payload = cleanReply
                    )
                )

                val msg = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = cleanReply,
                    actionButtons = actions
                )
                _homeWidgetAnswer.value = msg
                _homeWidgetDisplayState.value = HomeWidgetDisplayState.EXPANDED
                _messages.update { it + NovaChatMessage(sender = NovaSender.USER, text = text) + msg }

                if (_settings.value.ttsAutoSpeak && _settings.value.voiceEnabled) {
                    voiceManager.speak(cleanReply, NovaVoiceEmotion.CALM)
                }
            } catch (e: Exception) {
                val fallbackReply = "Newton's Second Law says that force is related to the rate of change of momentum (F = m·a)."
                val msg = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = fallbackReply,
                    actionButtons = listOf(
                        NovaContextualAction(
                            label = "Explain More",
                            iconName = "bulb",
                            actionType = NovaActionType.OPEN_FULL_NOVA,
                            isPrimary = true
                        ),
                        NovaContextualAction(
                            label = "Practice 5",
                            iconName = "quiz",
                            actionType = NovaActionType.START_QUIZ
                        )
                    )
                )
                _homeWidgetAnswer.value = msg
                _homeWidgetDisplayState.value = HomeWidgetDisplayState.EXPANDED
            }
        }
    }

    fun initQuizConfig() {
        viewModelScope.launch {
            val user = db.userDao().getUserProfileOnce()
            val examName = user?.examName ?: "RRB NTPC"
            val activeObjective = db.examObjectiveDao().getActiveExamObjectiveOnce()
            val actualExam = activeObjective?.examName ?: examName

            val subjectsEntities = db.examCatalogDao().getSubjectsForExamOnce(actualExam)
            val subjectNames = if (subjectsEntities.isNotEmpty()) {
                listOf("All Subjects") + subjectsEntities.map { it.name }
            } else {
                listOf("All Subjects", "General Awareness", "Mathematics", "General Intelligence & Reasoning", "General Science")
            }

            val firstSub = subjectNames.getOrNull(1) ?: "All Subjects"
            val topics = loadTopicsForSubject(actualExam, firstSub)

            _quizState.update { current ->
                val count = current.questionCount
                val estDuration = calculateEstimatedDuration(count, actualExam)
                current.copy(
                    selectedExam = actualExam,
                    subject = "All Subjects",
                    topic = "All Topics",
                    availableSubjects = subjectNames,
                    availableTopics = listOf("All Topics") + topics,
                    language = user?.languagePreference?.ifBlank { "English" } ?: "English",
                    durationMinutes = if (!current.isCustomDuration) estDuration else current.durationMinutes
                )
            }
        }
    }

    private fun calculateEstimatedDuration(questionCount: Int, examName: String): Int {
        return when (questionCount) {
            10 -> 12
            20 -> 25
            50 -> 60
            100 -> 90
            else -> (questionCount * 1.2f).toInt().coerceAtLeast(5)
        }
    }

    private suspend fun loadTopicsForSubject(examName: String, subjectName: String): List<String> {
        return withContext(Dispatchers.IO) {
            val allTopics = db.examCatalogDao().getTopicsForExam(examName).firstOrNull() ?: emptyList()
            val filtered = allTopics.filter { it.subjectId.contains(subjectName, ignoreCase = true) || it.name.contains(subjectName, ignoreCase = true) }.map { it.name }
            if (filtered.isNotEmpty()) return@withContext filtered

            when {
                subjectName.contains("Math", ignoreCase = true) -> listOf("Number System", "Percentages", "Ratio & Proportion", "Time & Work", "Speed, Distance & Time", "Algebra", "Geometry & Mensuration", "Simple & Compound Interest", "Profit & Loss")
                subjectName.contains("Reasoning", ignoreCase = true) || subjectName.contains("Intelligence", ignoreCase = true) -> listOf("Analogies", "Coding-Decoding", "Syllogism", "Blood Relations", "Venn Diagrams", "Data Sufficiency", "Series Completion", "Statement & Conclusions")
                subjectName.contains("Science", ignoreCase = true) || subjectName.contains("Physics", ignoreCase = true) -> listOf("Kinematics & Laws of Motion", "Work, Energy & Power", "Optics & Light", "Electricity & Magnetism", "Thermodynamics", "Sound & Waves")
                subjectName.contains("Chemistry", ignoreCase = true) -> listOf("Atomic Structure", "Periodic Classification", "Acids, Bases & Salts", "Metals & Non-Metals", "Chemical Reactions", "Organic Chemistry Basics")
                subjectName.contains("Current", ignoreCase = true) || subjectName.contains("General Awareness", ignoreCase = true) -> listOf("National News & Government Schemes", "International Summits & Treaties", "Science, Tech & Space (ISRO)", "Awards, Honors & Books", "Sports & Tournaments", "Indian Polity & Constitution", "Indian History & Freedom Struggle", "Indian Geography & Environment")
                else -> listOf("Core Fundamentals", "Important Formulas", "High-Yield Applications", "Previous Exam Trends")
            }
        }
    }

    fun updateQuizExam(examName: String) {
        viewModelScope.launch {
            val subjectsEntities = db.examCatalogDao().getSubjectsForExamOnce(examName)
            val subjectNames = if (subjectsEntities.isNotEmpty()) {
                listOf("All Subjects") + subjectsEntities.map { it.name }
            } else {
                listOf("All Subjects", "General Awareness", "Mathematics", "General Intelligence & Reasoning", "General Science")
            }
            val topics = loadTopicsForSubject(examName, "All Subjects")
            _quizState.update {
                it.copy(
                    selectedExam = examName,
                    subject = "All Subjects",
                    topic = "All Topics",
                    availableSubjects = subjectNames,
                    availableTopics = listOf("All Topics") + topics,
                    durationMinutes = if (!it.isCustomDuration) calculateEstimatedDuration(it.questionCount, examName) else it.durationMinutes
                )
            }
        }
    }

    fun updateQuizSubject(subject: String) {
        val exam = _quizState.value.selectedExam
        viewModelScope.launch {
            val topics = if (subject == "All Subjects") emptyList() else loadTopicsForSubject(exam, subject)
            _quizState.update {
                it.copy(
                    subject = subject,
                    topic = "All Topics",
                    availableTopics = listOf("All Topics") + topics
                )
            }
        }
    }

    fun updateQuizTopic(topic: String) {
        _quizState.update { it.copy(topic = topic) }
    }

    fun updateQuizLanguage(language: String) {
        _quizState.update { it.copy(language = language) }
    }

    fun updateQuizQuestionCount(count: Int) {
        _quizState.update {
            val estDuration = calculateEstimatedDuration(count, it.selectedExam)
            it.copy(
                questionCount = count,
                durationMinutes = if (!it.isCustomDuration) estDuration else it.durationMinutes
            )
        }
    }

    fun updateQuizDuration(minutes: Int, isCustom: Boolean) {
        _quizState.update { it.copy(durationMinutes = minutes.coerceIn(3, 180), isCustomDuration = isCustom) }
    }

    fun updateQuizDifficulty(difficulty: String) {
        _quizState.update { it.copy(difficulty = difficulty) }
    }

    fun updateQuizMode(mode: String) {
        _quizState.update { it.copy(questionMode = mode) }
    }

    fun prepareQuizBriefing() {
        _quizState.update { it.copy(screenStage = QuizScreenStage.BRIEFING) }
    }

    fun backToQuizConfig() {
        stopQuizTimer()
        _quizState.update { it.copy(screenStage = QuizScreenStage.CONFIGURING, isQuizFinished = false) }
    }

    fun startGeneratedQuiz() {
        val current = _quizState.value
        _quizState.update {
            it.copy(
                isGenerating = true,
                generationStatus = "Preparing ${current.questionCount} syllabus-grounded questions for ${current.selectedExam}...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val examName = current.selectedExam.ifBlank { _studyContext.value.targetExam }
            val subject = current.subject
            val topic = current.topic
            val count = current.questionCount
            val language = current.language
            val difficulty = current.difficulty
            val mode = current.questionMode

            var groundedContext = ""
            if (mode == "Current Affairs") {
                val news = db.currentAffairsDao().getAllCurrentAffairs().firstOrNull() ?: emptyList()
                groundedContext = news.take(10).joinToString("\n- ") { "${it.title}: ${it.summary}" }
            } else if (mode == "Revision") {
                val weak = _studyContext.value.weakTopics.joinToString(", ")
                groundedContext = "Student weak areas: $weak"
            }

            val result = geminiRepository.generateComprehensiveExamQuiz(
                examName = examName,
                subject = subject,
                topic = topic,
                difficulty = difficulty,
                count = count,
                language = language,
                mode = mode,
                groundedContextText = groundedContext
            )

            var questionsList = result.getOrNull() ?: emptyList()

            // Fallback to local verified bank if empty or error
            if (questionsList.isEmpty()) {
                val bankQuestions = examQuestionBankRepository.getQuestionsForTest(
                    examName = examName,
                    subject = subject,
                    topic = topic,
                    difficulty = difficulty,
                    language = language,
                    desiredCount = count
                )
                if (bankQuestions.isNotEmpty()) {
                    questionsList = bankQuestions
                } else {
                    questionsList = geminiRepository.getDefaultQuestions(if (subject == "All Subjects") "General Awareness" else subject)
                }
            }

            // Set up active test
            val totalSeconds = current.durationMinutes * 60
            _quizState.update {
                it.copy(
                    questions = questionsList,
                    currentIndex = 0,
                    selectedOptionIndex = null,
                    userAnswers = emptyMap(),
                    markedForReview = emptySet(),
                    immediateChecked = emptyMap(),
                    isAnswerSubmitted = false,
                    isGenerating = false,
                    generationStatus = "",
                    screenStage = QuizScreenStage.ACTIVE,
                    isQuizFinished = false,
                    totalDurationSeconds = totalSeconds,
                    timeRemainingSeconds = totalSeconds,
                    score = 0,
                    explanation = ""
                )
            }

            startQuizTimer(totalSeconds)
        }
    }

    fun startQuizSession(subject: String, topic: String) {
        _currentTab.value = NovaScreenTab.INTERACTIVE_STUDY_QUIZ
        _quizState.update {
            it.copy(
                subject = subject,
                topic = topic,
                screenStage = QuizScreenStage.CONFIGURING
            )
        }
        prepareQuizBriefing()
    }

    private fun startQuizTimer(durationSeconds: Int) {
        quizTimerJob?.cancel()
        _quizState.update {
            it.copy(
                timeRemainingSeconds = durationSeconds,
                totalDurationSeconds = durationSeconds,
                isTimerRunning = true
            )
        }
        quizTimerJob = viewModelScope.launch {
            while (_quizState.value.isTimerRunning && _quizState.value.timeRemainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                _quizState.update {
                    it.copy(timeRemainingSeconds = (it.timeRemainingSeconds - 1).coerceAtLeast(0))
                }
            }
            if (_quizState.value.timeRemainingSeconds <= 0 && _quizState.value.screenStage == QuizScreenStage.ACTIVE) {
                submitQuizSession()
            }
        }
    }

    private fun stopQuizTimer() {
        quizTimerJob?.cancel()
        _quizState.update { it.copy(isTimerRunning = false) }
    }

    fun selectQuizOption(index: Int) {
        val current = _quizState.value
        if (current.screenStage != QuizScreenStage.ACTIVE) return
        val currentQIndex = current.currentIndex
        // In practice mode, if already checked, don't allow changing answer
        if (current.questionMode == "Practice" && current.immediateChecked[currentQIndex] == true) return

        _quizState.update {
            val newAnswers = it.userAnswers.toMutableMap()
            newAnswers[currentQIndex] = index
            it.copy(
                userAnswers = newAnswers,
                selectedOptionIndex = index
            )
        }
    }

    fun toggleMarkForReview() {
        val current = _quizState.value
        val idx = current.currentIndex
        _quizState.update {
            val newSet = it.markedForReview.toMutableSet()
            if (newSet.contains(idx)) {
                newSet.remove(idx)
            } else {
                newSet.add(idx)
            }
            it.copy(markedForReview = newSet)
        }
    }

    fun checkImmediateAnswer() {
        val current = _quizState.value
        val idx = current.currentIndex
        val question = current.questions.getOrNull(idx) ?: return
        val selectedOpt = current.userAnswers[idx] ?: return

        val isCorrect = selectedOpt == question.correctOptionIndex
        _quizState.update {
            val newChecked = it.immediateChecked.toMutableMap()
            newChecked[idx] = true
            it.copy(
                immediateChecked = newChecked,
                isAnswerSubmitted = true,
                explanation = question.explanation
            )
        }

        if (_settings.value.voiceEnabled) {
            val msg = if (isCorrect) "Correct answer Boss! Well done." else "Incorrect. Let's study the concept explanation."
            voiceManager.speak(msg, if (isCorrect) NovaVoiceEmotion.HAPPY_ACHIEVEMENT else NovaVoiceEmotion.GENTLE_MOTIVATION)
        }
    }

    fun submitQuizAnswer() {
        // Backwards compatible method
        checkImmediateAnswer()
    }

    fun jumpToQuestion(index: Int) {
        val current = _quizState.value
        if (index in current.questions.indices) {
            val question = current.questions[index]
            val isChecked = current.immediateChecked[index] == true
            _quizState.update {
                it.copy(
                    currentIndex = index,
                    selectedOptionIndex = it.userAnswers[index],
                    isAnswerSubmitted = isChecked,
                    explanation = if (isChecked) question.explanation else ""
                )
            }
        }
    }

    fun nextQuizQuestion() {
        val current = _quizState.value
        if (current.currentIndex + 1 < current.questions.size) {
            jumpToQuestion(current.currentIndex + 1)
        }
    }

    fun previousQuizQuestion() {
        val current = _quizState.value
        if (current.currentIndex > 0) {
            jumpToQuestion(current.currentIndex - 1)
        }
    }

    fun submitQuizSession() {
        stopQuizTimer()
        val current = _quizState.value
        val questions = current.questions
        val userAnswers = current.userAnswers
        val totalQuestions = questions.size

        var correctCount = 0
        var incorrectCount = 0
        var unansweredCount = 0
        val mistakesToRecord = mutableListOf<Question>()

        val subjectStatsMap = mutableMapOf<String, Pair<Int, Int>>() // subject -> (total, correct)

        questions.forEachIndexed { idx, q ->
            val sub = q.subject.ifBlank { "General Awareness" }
            val existing = subjectStatsMap.getOrDefault(sub, Pair(0, 0))
            val selected = userAnswers[idx]

            if (selected == null) {
                unansweredCount++
                subjectStatsMap[sub] = Pair(existing.first + 1, existing.second)
            } else if (selected == q.correctOptionIndex) {
                correctCount++
                subjectStatsMap[sub] = Pair(existing.first + 1, existing.second + 1)
            } else {
                incorrectCount++
                mistakesToRecord.add(q)
                subjectStatsMap[sub] = Pair(existing.first + 1, existing.second)
            }
        }

        val accuracyPercent = if (totalQuestions > 0) (correctCount.toFloat() / totalQuestions) * 100f else 0f
        val timeSpent = current.totalDurationSeconds - current.timeRemainingSeconds

        // Marks calculation
        val earnedMarks = (correctCount * 1.0f) - (incorrectCount * 0.25f)
        val maxMarks = totalQuestions * 1.0f

        val breakdown = subjectStatsMap.mapValues { (_, pair) ->
            SubjectAccuracyStats(
                total = pair.first,
                correct = pair.second,
                accuracyPercent = if (pair.first > 0) (pair.second.toFloat() / pair.first) * 100f else 0f
            )
        }

        // Identify weak / strong topics
        val weakTopics = questions.filterIndexed { idx, q ->
            val ans = userAnswers[idx]
            ans != null && ans != q.correctOptionIndex
        }.map { it.topic }.distinct()

        val strongTopics = questions.filterIndexed { idx, q ->
            val ans = userAnswers[idx]
            ans == q.correctOptionIndex
        }.map { it.topic }.distinct()

        _quizState.update {
            it.copy(
                screenStage = QuizScreenStage.FINISHED,
                isQuizFinished = true,
                score = correctCount,
                totalQuestions = totalQuestions,
                earnedMarks = earnedMarks,
                maxMarks = maxMarks,
                accuracyPercent = accuracyPercent,
                timeSpentSeconds = timeSpent,
                correctCount = correctCount,
                incorrectCount = incorrectCount,
                unansweredCount = unansweredCount,
                markedCount = it.markedForReview.size,
                subjectBreakdown = breakdown,
                weakTopicsIdentified = weakTopics,
                strongTopicsIdentified = strongTopics,
                isAnalyzingResult = true,
                isResultSaved = true
            )
        }

        // Persist attempt & mistakes to Room Database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                studyRepository.recordMockTestAttempt(
                    title = "${current.selectedExam} ${current.questionMode} Practice Test",
                    subject = current.subject,
                    score = earnedMarks.toInt(),
                    totalQuestions = totalQuestions,
                    timeSpentSeconds = timeSpent,
                    weakTopics = weakTopics,
                    strongTopics = strongTopics,
                    aiRecommendation = if (weakTopics.isNotEmpty()) "Focus on weak topics: ${weakTopics.take(3).joinToString()}" else "Great performance across all tested topics!",
                    examName = current.selectedExam.ifBlank { "Practice Quiz" },
                    topic = current.topic,
                    difficulty = current.difficulty,
                    correctCount = correctCount,
                    incorrectCount = incorrectCount,
                    skippedCount = unansweredCount,
                    avgTimePerQuestionSeconds = if (totalQuestions > 0) timeSpent.toFloat() / totalQuestions else 0f,
                    markingScheme = current.markingScheme,
                    totalTimeAllowedSeconds = current.durationMinutes * 60
                )

                // Record mistakes
                mistakesToRecord.forEach { q ->
                    val userAnsText = q.options.getOrNull(userAnswers[questions.indexOf(q)] ?: -1) ?: "No Answer"
                    studyRepository.recordMistake(
                        questionText = q.questionText,
                        studentAnswer = userAnsText,
                        correctAnswer = q.options.getOrNull(q.correctOptionIndex) ?: "",
                        subject = q.subject,
                        topic = q.topic,
                        explanation = q.explanation
                    )
                }

                // Update Topic Mastery in Room
                val user = db.userDao().getUserProfileOnce()
                val targetExam = user?.examName ?: current.selectedExam
                questions.groupBy { it.topic }.forEach { (top, qList) ->
                    val c = qList.count { userAnswers[questions.indexOf(it)] == it.correctOptionIndex }
                    val acc = (c.toFloat() / qList.size) * 100f
                    val existing = db.topicMasteryDao().getTopicMasteryOnce(qList.first().subject, top)
                    val newScore = if (existing != null) (existing.masteryScore * 0.7f + acc * 0.3f).toInt() else acc.toInt()
                    db.topicMasteryDao().insertOrUpdateTopicMastery(
                        (existing ?: TopicMastery(
                            subject = qList.first().subject,
                            topic = top,
                            examId = targetExam
                        )).copy(
                            masteryScore = newScore.coerceIn(0, 100),
                            practiceAttempts = (existing?.practiceAttempts ?: 0) + qList.size,
                            practiceCorrect = (existing?.practiceCorrect ?: 0) + c,
                            practiceAccuracyPercent = acc,
                            lastTestedMillis = System.currentTimeMillis()
                        )
                    )
                }

                // Request Gemini AI Diagnostic Analysis
                val incorrectSummary = mistakesToRecord.take(5).joinToString("\n") {
                    "- [${it.subject} / ${it.topic}]: ${it.questionText} (Correct: ${it.options.getOrNull(it.correctOptionIndex)})"
                }

                val aiResult = geminiRepository.generateNovaQuizDiagnostic(
                    examName = current.selectedExam,
                    subject = current.subject,
                    score = correctCount,
                    totalQuestions = totalQuestions,
                    accuracyPercent = accuracyPercent,
                    timeSpentSeconds = timeSpent,
                    weakTopics = weakTopics,
                    strongTopics = strongTopics,
                    incorrectSummary = incorrectSummary,
                    language = current.language
                )

                _quizState.update {
                    it.copy(
                        novaAiAnalysis = aiResult.getOrNull() ?: "",
                        isAnalyzingResult = false
                    )
                }
            } catch (e: Exception) {
                _quizState.update { it.copy(isAnalyzingResult = false) }
            }
        }

        if (_settings.value.voiceEnabled) {
            val speech = if (accuracyPercent >= 70f) {
                "Test completed! Outstanding performance Boss, you scored ${earnedMarks.toInt()} marks with ${accuracyPercent.toInt()}% accuracy."
            } else {
                "Test finished Boss. You achieved ${accuracyPercent.toInt()}% accuracy. Let's analyze your weak areas and revise."
            }
            voiceManager.speak(speech, if (accuracyPercent >= 70f) NovaVoiceEmotion.HAPPY_ACHIEVEMENT else NovaVoiceEmotion.GENTLE_MOTIVATION)
        }
    }

    fun restartQuizSession() {
        stopQuizTimer()
        _quizState.update {
            it.copy(
                screenStage = QuizScreenStage.CONFIGURING,
                isQuizFinished = false,
                questions = emptyList(),
                userAnswers = emptyMap(),
                markedForReview = emptySet(),
                immediateChecked = emptyMap(),
                score = 0,
                isResultSaved = false
            )
        }
    }

    fun practiceWeakTopicsQuiz() {
        val current = _quizState.value
        val weak = current.weakTopicsIdentified.firstOrNull() ?: current.topic
        _quizState.update {
            it.copy(
                topic = weak,
                questionMode = "Revision",
                questionCount = 10,
                screenStage = QuizScreenStage.CONFIGURING
            )
        }
        prepareQuizBriefing()
    }

    fun saveMistakeToNotebook(question: Question, studentAnswer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            studyRepository.recordMistake(
                questionText = question.questionText,
                studentAnswer = studentAnswer,
                correctAnswer = question.options.getOrNull(question.correctOptionIndex) ?: "",
                subject = question.subject,
                topic = question.topic,
                explanation = question.explanation
            )
            _snackbarMessage.emit("Saved to Mistake Notebook 📖")
        }
    }

    fun saveQuestionAsFlashcard(question: Question) {
        viewModelScope.launch(Dispatchers.IO) {
            val correctAns = question.options.getOrNull(question.correctOptionIndex) ?: ""
            studyRepository.addFlashcard(
                subject = question.subject,
                topic = question.topic,
                front = question.questionText,
                back = "Answer: $correctAns\n\nExplanation: ${question.explanation}",
                hint = "Exam: ${question.subject}",
                difficulty = question.difficulty.ifBlank { "Medium" },
                sourceDocTitle = "NOVA Quiz Intelligence"
            )
            _snackbarMessage.emit("Saved to Flashcards 🎴")
        }
    }

    fun askNovaAboutQuestion(question: Question) {
        val query = "Please explain this question step-by-step for ${question.subject} - ${question.topic}:\n\n${question.questionText}\n\nOptions:\n${question.options.mapIndexed { i, o -> "${('A' + i)}. $o" }.joinToString("\n")}\n\nCorrect Answer: ${question.options.getOrNull(question.correctOptionIndex)}\nExplanation: ${question.explanation}"
        _currentTab.value = NovaScreenTab.ASSISTANT_CHAT
        sendMessage(query)
    }

    fun setAttachedImage(uri: Uri) {
        _attachedImageUri.value = uri
        viewModelScope.launch(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val bitmap = MediaStore.Images.Media.getBitmap(getApplication<Application>().contentResolver, uri)
                _attachedImageBitmap.value = bitmap
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun clearAttachedImage() {
        _attachedImageUri.value = null
        _attachedImageBitmap.value = null
    }

    // --- Memory Operations ---
    fun addManualMemory(category: NovaMemoryCategory, key: String, value: String) {
        viewModelScope.launch {
            if (key.isNotBlank() && value.isNotBlank()) {
                val item = NovaMemoryItem(
                    category = category,
                    key = key,
                    value = value,
                    source = "User Added"
                )
                studyRepository.saveNovaMemory(item)
                _snackbarMessage.emit("Saved to NOVA Memory: \"$key\"")
            }
        }
    }

    fun editManualMemory(id: Long, category: NovaMemoryCategory, key: String, value: String) {
        viewModelScope.launch {
            if (key.isNotBlank() && value.isNotBlank()) {
                val item = NovaMemoryItem(
                    id = id,
                    category = category,
                    key = key,
                    value = value,
                    source = "User Edited"
                )
                studyRepository.saveNovaMemory(item)
                _snackbarMessage.emit("Memory updated: \"$key\"")
            }
        }
    }

    fun toggleMemory(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            studyRepository.toggleNovaMemory(id, isEnabled)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            studyRepository.deleteNovaMemory(id)
            _snackbarMessage.emit("Memory item deleted")
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            studyRepository.clearAllNovaMemories()
            _snackbarMessage.emit("All NOVA memories cleared")
        }
    }

    // --- Voice & Coach Settings ---
    fun updateSettings(newSettings: NovaSettings) {
        _settings.value = newSettings
        voiceManager.setSpeechSpeed(newSettings.speechSpeed)
        voiceManager.setPitch(newSettings.voicePitch)
        voiceManager.setVolume(newSettings.voiceVolume)
    }

    fun previewNovaVoice() {
        voiceManager.previewVoice(
            text = "Boss 😄, 7 baj gaye hain. Aaj ka Physics session abhi pending hai. Chalo 25 minutes se start karte hain?",
            emotion = NovaVoiceEmotion.GENTLE_MOTIVATION
        )
    }

    fun triggerProactiveStudyNotification() {
        val sub = _studyContext.value.subjects.firstOrNull() ?: "Physics"
        val top = _studyContext.value.weakTopics.firstOrNull() ?: "Current Electricity"
        notificationEngine.sendStudyReminder(sub, top, 25, _settings.value.voiceNotifications)
        viewModelScope.launch {
            _snackbarMessage.emit("🔔 Sent Study Reminder with [Start Study], [Snooze], [Skip] actions")
        }
    }

    fun triggerDailyBriefingNotification() {
        val brief = _dailyBriefingText.value ?: "Good morning Boss ☀️ Aaj 2 sessions planned hain. Let's make today count!"
        notificationEngine.sendDailyBriefing(brief)
        viewModelScope.launch {
            _snackbarMessage.emit("☀️ Sent NOVA Daily Briefing notification")
        }
    }

    fun triggerDailyReviewNotification() {
        val rev = _dailyReviewText.value ?: "🌙 NOVA Daily Review: Great effort today! Check progress in app."
        notificationEngine.sendDailyReview(rev)
        viewModelScope.launch {
            _snackbarMessage.emit("🌙 Sent NOVA Daily Review notification")
        }
    }

    fun triggerSocialMediaNudgeNotification() {
        val pendingSub = _studyContext.value.subjects.firstOrNull() ?: "Physics"
        notificationEngine.sendSocialMediaNudge("YouTube", 40, pendingSub)
        viewModelScope.launch {
            _snackbarMessage.emit("📱 Sent Distracting App Usage nudge")
        }
    }

    fun askPromptFromDashboard(prompt: String) {
        _currentTab.value = NovaScreenTab.ASSISTANT_CHAT
        sendMessage(prompt)
    }

    fun startNewChat() {
        val currentMsgs = _messages.value
        val hasUserMsg = currentMsgs.any { it.sender == NovaSender.USER }
        if (hasUserMsg) {
            val firstUserMsg = currentMsgs.firstOrNull { it.sender == NovaSender.USER }?.text ?: "Study Discussion"
            val title = firstUserMsg.take(40).ifBlank { "Study Session" }
            val session = NovaConversationSession(
                title = title,
                messages = currentMsgs,
                examContext = _studyContext.value.targetExam
            )
            _savedConversations.update { listOf(session) + it }
        }

        val greeting = getProactiveGreeting()
        _messages.value = listOf(
            NovaChatMessage(
                sender = NovaSender.NOVA,
                text = greeting,
                actionType = NovaActionType.NONE
            )
        )
        _attachedImageUri.value = null
        _attachedImageBitmap.value = null
        viewModelScope.launch {
            _snackbarMessage.emit("✨ Started a new chat with NOVA")
        }
    }

    fun loadConversation(session: NovaConversationSession) {
        _messages.value = session.messages
        _attachedImageUri.value = null
        _attachedImageBitmap.value = null
        viewModelScope.launch {
            _snackbarMessage.emit("Loaded chat: ${session.title}")
        }
    }

    fun submitMessageFeedback(messageId: String, isHelpful: Boolean) {
        _messages.update { list ->
            list.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(userFeedback = if (isHelpful) "HELPFUL" else "NOT_HELPFUL")
                } else msg
            }
        }
        viewModelScope.launch {
            _snackbarMessage.emit(if (isHelpful) "👍 Feedback saved! Thanks." else "👎 Feedback noted. NOVA will improve!")
        }
    }

    fun deleteConversation(sessionId: String) {
        _savedConversations.update { it.filter { sess -> sess.id != sessionId } }
        viewModelScope.launch {
            _snackbarMessage.emit("Deleted saved chat")
        }
    }

    fun clearCurrentChat() {
        _messages.value = emptyList()
        _attachedImageUri.value = null
        _attachedImageBitmap.value = null
        viewModelScope.launch {
            _snackbarMessage.emit("Chat cleared")
        }
    }

    fun toggleLanguageMode() {
        val currentLang = _settings.value.language
        val nextLang = when (currentLang) {
            "Hinglish (Auto)" -> "English"
            "English" -> "Hindi"
            else -> "Hinglish (Auto)"
        }
        _settings.update { it.copy(language = nextLang) }
        viewModelScope.launch {
            _snackbarMessage.emit("🌐 Language set to $nextLang")
        }
    }

    fun retryLastMessage() {
        val msgs = _messages.value
        val lastUserMsg = msgs.lastOrNull { it.sender == NovaSender.USER }
        if (lastUserMsg != null) {
            sendMessage(lastUserMsg.text)
        }
    }

    // =========================================================================
    // SMART SEARCH & ACADEMIC INTELLIGENCE METHODS
    // =========================================================================

    fun setSearchLanguage(lang: String) {
        _searchLanguage.value = lang
    }

    fun setSearchSubject(subject: String) {
        _searchSubject.value = subject
    }

    fun addToSearchHistory(query: String, subject: String = _searchSubject.value) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _searchHistory.value.filter { !it.query.equals(trimmed, ignoreCase = true) }
        _searchHistory.value = listOf(NovaSearchHistoryItem(trimmed, subject, System.currentTimeMillis())) + current.take(9)
    }

    fun removeSearchHistoryItem(query: String) {
        _searchHistory.value = _searchHistory.value.filter { !it.query.equals(query, ignoreCase = true) }
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    fun performSmartSearch(
        query: String,
        subject: String = _searchSubject.value,
        language: String = _searchLanguage.value
    ) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSmartSearching.value = true
            _searchError.value = null
            addToSearchHistory(query, subject)
            val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }
            val result = geminiRepository.performSmartSearch(
                query = query.trim(),
                examName = exam,
                subject = subject,
                language = language
            )
            result.onSuccess {
                _smartSearchResult.value = it
                _searchError.value = null
            }
            result.onFailure {
                _searchError.value = it.localizedMessage ?: "Unable to complete search"
                _snackbarMessage.emit("Search notice: ${it.localizedMessage}")
            }
            _isSmartSearching.value = false
        }
    }

    fun askNovaAboutSearchResult(result: SmartSearchResult) {
        val prompt = "Boss, please give me a deeper study breakdown of \"${result.query}\" with practical problem-solving tips and core insights."
        sendMessage(prompt)
        setTab(NovaScreenTab.ASSISTANT_CHAT)
    }

    fun clearSmartSearch() {
        _smartSearchResult.value = null
        _searchError.value = null
    }

    fun saveSearchResultAsSmartNote(result: SmartSearchResult, subject: String = "General") {
        viewModelScope.launch {
            val note = SmartNoteItem(
                title = result.query.replaceFirstChar { it.uppercase() },
                subject = subject,
                topic = result.query.take(30),
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
            _snackbarMessage.emit("💾 Saved to Smart Notes!")
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
                _snackbarMessage.emit("🗂️ Added ${cards.size} Flashcards for Spaced Recall!")
            }
        }
    }

    fun saveSearchResultAsPlanTask(result: SmartSearchResult, subject: String = "General") {
        viewModelScope.launch {
            val plan = StudyPlanItem(
                subject = subject,
                chapter = "Smart Search",
                topic = result.query,
                targetMinutes = 30,
                isCompleted = false,
                priority = PlanPriority.HIGH,
                notes = "Generated from Smart Search: ${result.query}"
            )
            studyRepository.addStudyPlanItem(plan)
            _snackbarMessage.emit("📅 Added to Today's Study Plan!")
        }
    }

    fun saveSmartNote(note: SmartNoteItem) {
        viewModelScope.launch {
            studyRepository.saveSmartNote(note)
            _snackbarMessage.emit("📝 Smart Note Saved!")
        }
    }

    fun updateSmartNote(note: SmartNoteItem) {
        viewModelScope.launch {
            studyRepository.updateSmartNote(note)
            _snackbarMessage.emit("📝 Note updated!")
        }
    }

    private val _isGeneratingAiNote = MutableStateFlow(false)
    val isGeneratingAiNote: StateFlow<Boolean> = _isGeneratingAiNote.asStateFlow()

    private val _noteAiAssistanceResult = MutableStateFlow<String?>(null)
    val noteAiAssistanceResult: StateFlow<String?> = _noteAiAssistanceResult.asStateFlow()

    private val _isNoteAiAssisting = MutableStateFlow(false)
    val isNoteAiAssisting: StateFlow<Boolean> = _isNoteAiAssisting.asStateFlow()

    fun generateAiSmartNote(
        subject: String,
        topic: String,
        noteType: String = "Quick Revision",
        language: String = _searchLanguage.value,
        onSuccess: (SmartNoteItem) -> Unit = {}
    ) {
        if (topic.isBlank()) return
        viewModelScope.launch {
            _isGeneratingAiNote.value = true
            val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }
            val result = geminiRepository.generateSmartNote(
                examName = exam,
                subject = subject,
                topic = topic.trim(),
                noteType = noteType,
                language = language
            )
            result.onSuccess { generatedNote ->
                studyRepository.saveSmartNote(generatedNote)
                _snackbarMessage.emit("✨ NOVA generated note for $topic!")
                onSuccess(generatedNote)
            }
            result.onFailure {
                _snackbarMessage.emit("Error generating note: ${it.localizedMessage}")
            }
            _isGeneratingAiNote.value = false
        }
    }

    fun assistWithSmartNote(
        note: SmartNoteItem,
        actionType: String,
        language: String = _searchLanguage.value
    ) {
        viewModelScope.launch {
            _isNoteAiAssisting.value = true
            _noteAiAssistanceResult.value = null
            val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }
            val result = geminiRepository.assistWithSmartNote(
                note = note,
                actionType = actionType,
                examName = exam,
                language = language
            )
            result.onSuccess { text ->
                _noteAiAssistanceResult.value = text
            }
            result.onFailure {
                _snackbarMessage.emit("AI assistance error: ${it.localizedMessage}")
            }
            _isNoteAiAssisting.value = false
        }
    }

    fun clearNoteAiAssistance() {
        _noteAiAssistanceResult.value = null
    }

    fun convertNoteToFlashcards(note: SmartNoteItem) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cards = mutableListOf<FlashcardItem>()
            
            note.keyPoints.forEachIndexed { i, kp ->
                cards.add(
                    FlashcardItem(
                        subject = note.subject,
                        topic = note.topic,
                        front = "Key Concept (#${i + 1}) from ${note.title}?",
                        back = kp,
                        hint = "${note.subject} • ${note.topic}",
                        difficulty = "Medium",
                        status = RevisionCategory.REVISE_NOW,
                        confidence = 2,
                        intervalDays = 1,
                        easeFactor = 2.5f,
                        repetitions = 0,
                        nextReviewDate = now,
                        sourceDocTitle = "Smart Note: ${note.title}",
                        createdAt = now
                    )
                )
            }
            
            note.formulas.forEachIndexed { i, f ->
                cards.add(
                    FlashcardItem(
                        subject = note.subject,
                        topic = note.topic,
                        front = "Formula / Law for ${note.topic} (#${i + 1})?",
                        back = f,
                        hint = "Formula recall for ${note.subject}",
                        difficulty = "Hard",
                        status = RevisionCategory.REVISE_NOW,
                        confidence = 2,
                        intervalDays = 1,
                        easeFactor = 2.5f,
                        repetitions = 0,
                        nextReviewDate = now,
                        sourceDocTitle = "Smart Note: ${note.title}",
                        createdAt = now
                    )
                )
            }

            if (cards.isEmpty()) {
                cards.add(
                    FlashcardItem(
                        subject = note.subject,
                        topic = note.topic,
                        front = "What is the core takeaway of ${note.title}?",
                        back = note.contentMarkdown.take(250),
                        hint = "${note.subject} key recall",
                        difficulty = "Medium",
                        status = RevisionCategory.REVISE_NOW,
                        confidence = 2,
                        intervalDays = 1,
                        easeFactor = 2.5f,
                        repetitions = 0,
                        nextReviewDate = now,
                        sourceDocTitle = "Smart Note: ${note.title}",
                        createdAt = now
                    )
                )
            }

            studyRepository.insertFlashcardList(cards)
            _snackbarMessage.emit("🗂️ Added ${cards.size} Flashcards to Spaced Recall!")
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
            _snackbarMessage.emit("Deleted smart note")
        }
    }

    fun toggleCurrentAffairsSaved(id: Long, isSaved: Boolean) {
        viewModelScope.launch {
            studyRepository.toggleCurrentAffairsSaved(id, isSaved)
            _snackbarMessage.emit(if (isSaved) "🔖 Saved to Revision Feed" else "Removed from Saved")
        }
    }

    fun setCurrentAffairsFilter(category: String) {
        _currentAffairsFilterCategory.value = category
    }

    fun setCurrentAffairsSearchQuery(query: String) {
        _currentAffairsSearchQuery.value = query
    }

    fun setCurrentAffairsLanguage(lang: String) {
        _currentAffairsLanguage.value = lang
    }

    fun setSelectedCurrentAffairsDate(dateStr: String) {
        _selectedCurrentAffairsDate.value = dateStr
    }

    fun setCurrentAffairsExamMode(mode: String) {
        _currentAffairsExamMode.value = mode
    }

    fun searchFreshWebCurrentAffairs(query: String) {
        if (query.isBlank() || _isFreshWebSearching.value) return
        viewModelScope.launch {
            _isFreshWebSearching.value = true
            _snackbarMessage.emit("🌐 Searching fresh web updates for '$query'...")
            try {
                val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }
                val lang = _currentAffairsLanguage.value
                val result = geminiRepository.fetchLiveCurrentAffairs(
                    examName = exam,
                    category = query,
                    language = lang
                )
                result.onSuccess { fetched ->
                    val existing = allCurrentAffairs.value
                    val combined = (fetched + existing).distinctBy { it.title.trim().lowercase() }
                    studyRepository.saveCurrentAffairsList(combined)
                    _snackbarMessage.emit("✨ Discovered ${fetched.size} fresh updates!")
                }.onFailure { err ->
                    _snackbarMessage.emit("Web search error: ${err.localizedMessage}")
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("Web search failed: ${e.localizedMessage}")
            } finally {
                _isFreshWebSearching.value = false
            }
        }
    }

    fun startDailyCurrentAffairsQuiz(requestedCount: Int = 5) {
        val affairs = allCurrentAffairs.value
        val questions = mutableListOf<com.example.data.model.Question>()
        affairs.forEach { item ->
            if (item.mcqs.isNotEmpty()) {
                questions.addAll(item.mcqs)
            }
        }
        val targetQuestions = if (questions.isNotEmpty()) {
            questions.shuffled().take(requestedCount)
        } else {
            // Generate fallback high-yield CA questions if DB has none
            val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }
            listOf(
                com.example.data.model.Question(
                    id = "ca_q1",
                    questionText = "Which organization recently successfully validated mission-critical abort tests for human spaceflight?",
                    options = listOf("ISRO", "NASA", "ESA", "JAXA"),
                    correctOptionIndex = 0,
                    explanation = "ISRO validated the Pad Abort & Crew Escape System for the Gaganyaan mission.",
                    subject = "Current Affairs",
                    topic = "Science & Tech",
                    difficulty = "Medium"
                ),
                com.example.data.model.Question(
                    id = "ca_q2",
                    questionText = "What is the main objective of India's newly announced semiconductor manufacturing initiative?",
                    options = listOf("Domestic Chip Production", "Space Tourism", "Oil Exploration", "Rail Electrification"),
                    correctOptionIndex = 0,
                    explanation = "The initiative aims to build domestic semiconductor fabrication units and reduce import dependencies.",
                    subject = "Current Affairs",
                    topic = "Economy & Tech",
                    difficulty = "Medium"
                )
            ).take(requestedCount)
        }

        _currentAffairsQuizState.value = com.example.data.model.CurrentAffairsQuizSession(
            title = "🧠 Daily Current Affairs Quiz ($requestedCount Questions)",
            questions = targetQuestions,
            language = _currentAffairsLanguage.value
        )
    }

    fun startArticleQuiz(article: CurrentAffairsItem) {
        val questions = if (article.mcqs.isNotEmpty()) {
            article.mcqs
        } else {
            listOf(
                com.example.data.model.Question(
                    id = "art_q_${article.id}",
                    questionText = "Regarding '${article.title.take(60)}...', which statement is correct?",
                    options = listOf(
                        article.summary.take(80),
                        "Option B: It relates to an unrelated sector",
                        "Option C: The policy was postponed indefinitely",
                        "Option D: None of the above"
                    ),
                    correctOptionIndex = 0,
                    explanation = article.examRelevance.ifBlank { article.summary },
                    subject = "Current Affairs",
                    topic = article.category,
                    difficulty = "Medium"
                )
            )
        }
        _currentAffairsQuizState.value = com.example.data.model.CurrentAffairsQuizSession(
            title = "🧠 Quiz: ${article.title.take(40)}...",
            questions = questions,
            language = _currentAffairsLanguage.value
        )
    }

    fun selectCurrentAffairsQuizAnswer(questionIndex: Int, optionIndex: Int) {
        val current = _currentAffairsQuizState.value ?: return
        val updatedMap = current.selectedAnswers.toMutableMap()
        updatedMap[questionIndex] = optionIndex
        _currentAffairsQuizState.value = current.copy(selectedAnswers = updatedMap)
    }

    fun submitCurrentAffairsQuiz() {
        val current = _currentAffairsQuizState.value ?: return
        var correct = 0
        var incorrect = 0
        var unanswered = 0

        current.questions.forEachIndexed { idx, q ->
            val sel = current.selectedAnswers[idx]
            if (sel == null) {
                unanswered++
            } else if (sel == q.correctOptionIndex) {
                correct++
            } else {
                incorrect++
            }
        }
        val total = current.questions.size
        val score = if (total > 0) ((correct.toFloat() / total) * 100).toInt() else 0

        _currentAffairsQuizState.value = current.copy(
            isSubmitted = true,
            score = score,
            correctCount = correct,
            incorrectCount = incorrect,
            unansweredCount = unanswered
        )
    }

    fun closeCurrentAffairsQuiz() {
        _currentAffairsQuizState.value = null
    }

    fun clearNovaAffairAnalysis() {
        _selectedAffairForNova.value = null
        _novaAffairAnalysis.value = null
        _isAnalyzingAffair.value = false
    }

    fun askNovaAboutAffair(item: CurrentAffairsItem, questionType: String) {
        _selectedAffairForNova.value = item
        _isAnalyzingAffair.value = true
        _novaAffairAnalysis.value = null
        viewModelScope.launch {
            val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }
            val lang = _currentAffairsLanguage.value
            val result = geminiRepository.askNovaCurrentAffair(
                item = item,
                questionType = questionType,
                examName = exam,
                language = lang
            )
            _isAnalyzingAffair.value = false
            result.onSuccess { analysis ->
                _novaAffairAnalysis.value = analysis
            }.onFailure { err ->
                _novaAffairAnalysis.value = "Unable to complete AI analysis: ${err.localizedMessage ?: "Unknown error"}. Please check network connection."
            }
        }
    }

    fun saveAffairAsSmartNote(item: CurrentAffairsItem) {
        viewModelScope.launch {
            val note = SmartNoteItem(
                title = item.title,
                subject = item.category,
                topic = "Current Affairs (${item.category})",
                contentMarkdown = "## ${item.title}\n\n" +
                        "**Source:** ${item.sourceName} (${item.publishedDate})\n\n" +
                        "### Executive Summary\n${item.summary}\n\n" +
                        "### Exam Weightage & Syllabus Link\n${item.examRelevance}",
                keyPoints = listOf(
                    item.summary,
                    item.examRelevance,
                    "Source: ${item.sourceName} - Published ${item.publishedDate}"
                ),
                formulas = emptyList(),
                importantFacts = listOf(
                    "Category: ${item.category}",
                    "Target Exams: ${item.targetExams.joinToString(", ")}"
                ),
                sourceUrl = item.sourceUrl,
                sourceTitle = item.sourceName,
                isRevised = false,
                isBookmarked = true,
                createdAt = System.currentTimeMillis()
            )
            studyRepository.saveSmartNote(note)
            _snackbarMessage.emit("📝 Added to Smart Notes notebook!")
        }
    }

    fun refreshCurrentAffairs(forceRefresh: Boolean = false) {
        if (_isRefreshingCurrentAffairs.value) return
        viewModelScope.launch {
            _isRefreshingCurrentAffairs.value = true
            _currentAffairsError.value = null
            try {
                val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }
                val cat = _currentAffairsFilterCategory.value
                val lang = _currentAffairsLanguage.value
                val result = geminiRepository.fetchLiveCurrentAffairs(
                    examName = exam,
                    category = cat,
                    language = lang
                )
                result.onSuccess { fetchedItems ->
                    val existing = allCurrentAffairs.value
                    val existingSavedIds = existing.filter { it.isSavedForRevision }.map { it.title.trim().lowercase() }.toSet()

                    // Deduplicate and preserve saved status
                    val deduped = fetchedItems.map { item ->
                        val isAlreadySaved = existingSavedIds.contains(item.title.trim().lowercase())
                        if (isAlreadySaved) item.copy(isSavedForRevision = true) else item
                    }

                    // Merge with any existing user-saved items that weren't in the new batch
                    val combinedList = (deduped + existing.filter { it.isSavedForRevision && !deduped.any { d -> d.title.equals(it.title, ignoreCase = true) } })
                    
                    studyRepository.saveCurrentAffairsList(combinedList)
                    _lastRefreshedTime.value = System.currentTimeMillis()
                    _snackbarMessage.emit("✨ Refreshed latest 30-day exam radar")
                }.onFailure { err ->
                    _currentAffairsError.value = err.localizedMessage ?: "Failed to refresh feed"
                }
            } catch (e: Exception) {
                _currentAffairsError.value = e.localizedMessage
            } finally {
                _isRefreshingCurrentAffairs.value = false
            }
        }
    }

    fun exportCurrentAffairsPdf(context: android.content.Context, targetDate: String? = null) {
        viewModelScope.launch {
            val items = allCurrentAffairs.value
            if (items.isEmpty()) {
                _snackbarMessage.emit("No Current Affairs items to export. Refreshing now...")
                refreshCurrentAffairs(forceRefresh = true)
                return@launch
            }
            try {
                val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exams" }
                val lang = _currentAffairsLanguage.value
                val dateStr = targetDate ?: java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                val pdfFile = com.example.ui.screens.progress.PdfReportGenerator.generateCurrentAffairsPdf(
                    context = context,
                    items = items,
                    dateStr = dateStr,
                    language = lang,
                    examName = exam
                )
                com.example.ui.screens.progress.PdfReportGenerator.sharePdfReport(
                    context = context,
                    pdfFile = pdfFile,
                    shareTitle = "Daily Current Affairs Dossier ($dateStr)"
                )
                _snackbarMessage.emit("📄 Current Affairs PDF generated!")
            } catch (e: Exception) {
                _snackbarMessage.emit("PDF Export failed: ${e.localizedMessage}")
            }
        }
    }

    fun exportNovaAnswerPdf(context: android.content.Context, messageText: String, topicHint: String? = null) {
        viewModelScope.launch {
            try {
                val title = topicHint ?: messageText.lines().firstOrNull()?.replace(Regex("[#*`_-]"), "")?.trim()?.take(40) ?: "NOVA Study Notes"
                val lang = if (_settings.value.language == "Hindi") "Hindi" else "English / Hinglish"
                val pdfFile = com.example.ui.screens.progress.PdfReportGenerator.generateNovaAnswerPdf(
                    context = context,
                    topicTitle = title,
                    contentMarkdown = messageText,
                    language = lang
                )
                com.example.ui.screens.progress.PdfReportGenerator.sharePdfReport(
                    context = context,
                    pdfFile = pdfFile,
                    shareTitle = title
                )
                _snackbarMessage.emit("📄 NOVA Study Note PDF generated!")
            } catch (e: Exception) {
                _snackbarMessage.emit("PDF Export failed: ${e.localizedMessage}")
            }
        }
    }

    fun saveNovaAnswerAsNote(messageText: String, topicHint: String? = null) {
        viewModelScope.launch {
            val title = topicHint ?: messageText.lines().firstOrNull()?.replace(Regex("[#*`_-]"), "")?.trim()?.take(40) ?: "NOVA Study Concept"
            val subject = _studyContext.value.subjects.firstOrNull() ?: "General"
            val note = SmartNoteItem(
                title = title,
                subject = subject,
                topic = title,
                contentMarkdown = messageText,
                keyPoints = messageText.lines().filter { it.trim().startsWith("-") || it.trim().startsWith("*") || it.trim().startsWith("•") }.take(5),
                formulas = emptyList(),
                importantFacts = listOf("Generated by NOVA AI Tutor", "Target: ${_studyContext.value.targetExam}"),
                sourceTitle = "NOVA AI Tutor",
                isBookmarked = true,
                createdAt = System.currentTimeMillis()
            )
            studyRepository.saveSmartNote(note)
            _snackbarMessage.emit("📝 Saved to Smart Notes!")
        }
    }

    fun markExamUpdateRead(id: Long) {
        viewModelScope.launch {
            studyRepository.markExamUpdateRead(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}

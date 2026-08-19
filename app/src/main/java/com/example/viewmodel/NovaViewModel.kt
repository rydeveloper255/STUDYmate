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

data class InteractiveQuizState(
    val subject: String = "Physics",
    val topic: String = "All Topics",
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerSubmitted: Boolean = false,
    val score: Int = 0,
    val isQuizFinished: Boolean = false,
    val isGenerating: Boolean = false,
    val explanation: String = ""
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

    private val _smartSearchResult = MutableStateFlow<SmartSearchResult?>(null)
    val smartSearchResult: StateFlow<SmartSearchResult?> = _smartSearchResult.asStateFlow()

    private val _isSmartSearching = MutableStateFlow(false)
    val isSmartSearching: StateFlow<Boolean> = _isSmartSearching.asStateFlow()

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

    init {
        loadInitialData()
        observeStudyContext()
        observeAnalytics()
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
                val novaMessage = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = response.replyMarkdown,
                    actionType = response.actionType,
                    actionPayload = response.actionPayload
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
                    text = "Boss, abhi internet connection available nahi hai. Aap offline Focus Mode ya Practice Quiz start kar sakte hain! 🚀"
                )
                _messages.update { it + fallbackMessage }
            }
        }
    }

    fun executeAction(actionType: NovaActionType, payload: String?) {
        viewModelScope.launch {
            when (actionType) {
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
                    var subject = _studyContext.value.subjects.firstOrNull() ?: "Physics"
                    var topic = "Core Concepts"
                    if (payload != null) {
                        try {
                            val json = org.json.JSONObject(payload)
                            subject = json.optString("subject", subject)
                            topic = json.optString("topic", topic)
                        } catch (e: Exception) {}
                    }
                    startQuizSession(subject, topic)
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
                else -> {}
            }
        }
    }

    fun startQuizSession(subject: String, topic: String) {
        _currentTab.value = NovaScreenTab.INTERACTIVE_STUDY_QUIZ
        _quizState.value = InteractiveQuizState(
            subject = subject,
            topic = topic,
            isGenerating = true
        )

        viewModelScope.launch {
            val result = geminiRepository.generateMockTestQuestions(
                subject = subject,
                chapter = topic,
                difficulty = "Medium",
                count = 5
            )

            result.onSuccess { questions ->
                _quizState.value = InteractiveQuizState(
                    subject = subject,
                    topic = topic,
                    questions = questions,
                    currentIndex = 0,
                    isGenerating = false
                )
            }.onFailure {
                val fallbackList = listOf(
                    Question(
                        id = "q1",
                        questionText = "Which law states that the induced electromotive force in any closed circuit is equal to the negative rate of change of magnetic flux?",
                        options = listOf("Faraday's Law", "Ampere's Circuital Law", "Coulomb's Law", "Biot-Savart Law"),
                        correctOptionIndex = 0,
                        explanation = "Faraday's Law of Induction states EMF = -dΦ/dt (Lenz's Law gives the negative sign).",
                        subject = subject,
                        topic = topic
                    ),
                    Question(
                        id = "q2",
                        questionText = "What is the SI unit of Magnetic Flux?",
                        options = listOf("Tesla", "Weber (Wb)", "Henry", "Gauss"),
                        correctOptionIndex = 1,
                        explanation = "The SI unit of magnetic flux is Weber (1 Wb = 1 T·m²).",
                        subject = subject,
                        topic = topic
                    ),
                    Question(
                        id = "q3",
                        questionText = "When a dielectric slab is inserted between the plates of an isolated charged capacitor, what happens to its capacitance?",
                        options = listOf("Decreases", "Increases", "Remains constant", "Becomes zero"),
                        correctOptionIndex = 1,
                        explanation = "Capacitance increases by factor K (C = K·C0) due to dielectric polarization.",
                        subject = subject,
                        topic = topic
                    )
                )
                _quizState.value = InteractiveQuizState(
                    subject = subject,
                    topic = topic,
                    questions = fallbackList,
                    currentIndex = 0,
                    isGenerating = false
                )
            }
        }
    }

    fun selectQuizOption(index: Int) {
        if (_quizState.value.isAnswerSubmitted) return
        _quizState.update { it.copy(selectedOptionIndex = index) }
    }

    fun submitQuizAnswer() {
        val current = _quizState.value
        val question = current.questions.getOrNull(current.currentIndex) ?: return
        val isCorrect = current.selectedOptionIndex == question.correctOptionIndex
        val newScore = if (isCorrect) current.score + 1 else current.score

        _quizState.update {
            it.copy(
                isAnswerSubmitted = true,
                score = newScore,
                explanation = question.explanation
            )
        }

        // Voice audio response on answer submit if voice enabled
        if (_settings.value.voiceEnabled) {
            val audioMsg = if (isCorrect) "Correct answer Boss! Well done." else "Incorrect. Let's review the explanation."
            voiceManager.speak(audioMsg, if (isCorrect) NovaVoiceEmotion.HAPPY_ACHIEVEMENT else NovaVoiceEmotion.GENTLE_MOTIVATION)
        }
    }

    fun nextQuizQuestion() {
        val current = _quizState.value
        if (current.currentIndex + 1 < current.questions.size) {
            _quizState.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    selectedOptionIndex = null,
                    isAnswerSubmitted = false,
                    explanation = ""
                )
            }
        } else {
            _quizState.update { it.copy(isQuizFinished = true) }
        }
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

    // =========================================================================
    // SMART SEARCH & ACADEMIC INTELLIGENCE METHODS
    // =========================================================================

    fun performSmartSearch(query: String, subject: String = "General") {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSmartSearching.value = true
            val exam = _studyContext.value.targetExam.ifBlank { "Competitive Exam" }
            val result = geminiRepository.performSmartSearch(query.trim(), exam, subject)
            result.onSuccess {
                _smartSearchResult.value = it
            }
            result.onFailure {
                _snackbarMessage.emit("Search error: ${it.localizedMessage}")
            }
            _isSmartSearching.value = false
        }
    }

    fun clearSmartSearch() {
        _smartSearchResult.value = null
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

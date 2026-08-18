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
import com.example.notification.StudyNotificationManager
import com.example.service.FocusShieldManager
import com.example.service.NovaUsageStatsHelper
import com.example.service.NovaVoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class NovaScreenTab {
    DASHBOARD,
    ASSISTANT_CHAT,
    INTERACTIVE_STUDY_QUIZ,
    MEMORY_CENTER,
    NOVA_SETTINGS
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

class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getDatabase(application)
    private val studyRepository = StudyRepository(db)
    private val geminiRepository = GeminiRepository()
    val voiceManager = NovaVoiceManager(application)

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

    private val _quizState = MutableStateFlow(InteractiveQuizState())
    val quizState: StateFlow<InteractiveQuizState> = _quizState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Pair<String, Map<String, Any>>>()
    val navigationEvent: SharedFlow<Pair<String, Map<String, Any>>> = _navigationEvent.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        loadInitialData()
        observeStudyContext()
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

        // Add initial greeting message
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
        $timeGreeting, $title! Main tumhara personal AI study companion **NOVA** hoon.
        
        Aaj ka study target track ho raha hai. Aap kya karna chahenge?
        - **[📚 Study Focus]** 25-min deep work session
        - **[🧠 Interactive Quiz]** Test yourself on high-yield questions
        - **[📝 Study Planner]** Smart exam prep schedule
        - **[📷 Vision Solver]** Textbook ya notes ki photo bhej kar samjhein
        
        Kuch bhi doubt ho ya distraction rokna ho — bas batao! ⚡
        """.trimIndent()
    }

    private fun observeStudyContext() {
        viewModelScope.launch {
            combine(
                studyRepository.userProfile,
                studyRepository.allPlanItems,
                studyRepository.allFocusSessions,
                studyRepository.activeNovaMemories
            ) { profile, planItems, sessions, activeMemories ->
                val user = profile ?: UserProfile()
                val pendingPlans = planItems.filter { !it.isCompleted }
                val nextSession = pendingPlans.firstOrNull()?.let { "${it.subject}: ${it.topic}" }
                
                val appUsage = try {
                    NovaUsageStatsHelper.getTodayDistractingAppUsage(getApplication())
                } catch (e: Exception) {
                    null
                }

                val remainingDays = ((user.examDateMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(1).toInt()

                NovaStudyContext(
                    studentName = user.name.ifBlank { "Scholar" },
                    preferredTitle = if (_settings.value.useBossGreeting) "Boss" else user.name,
                    targetExam = user.examName,
                    examDaysRemaining = remainingDays,
                    subjects = user.subjects,
                    weakTopics = if (user.weakTopics.isNotEmpty()) user.weakTopics else listOf("Core Principles", "Numerical Practice"),
                    dailyTargetMinutes = user.dailyTargetMinutes,
                    todayFocusMinutes = user.totalFocusMinutes,
                    currentStreak = user.streakDays,
                    pendingPlanCount = pendingPlans.size,
                    nextScheduledSession = nextSession,
                    topDistractingAppName = appUsage?.topDistractingAppName,
                    topDistractingAppUsageMins = appUsage?.topDistractingAppMinutes ?: 0,
                    memories = activeMemories
                )
            }.collectLatest { context ->
                _studyContext.value = context
            }
        }
    }

    private fun updateContextWithProfile(profile: UserProfile) {
        val remainingDays = ((profile.examDateMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(1).toInt()
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
        // Clear attached image
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

                // Auto speak if TTS enabled
                if (_settings.value.ttsAutoSpeak) {
                    voiceManager.speak(response.replyMarkdown)
                }

                // Save memory if suggested
                if (_settings.value.memoryEnabled && response.memoryToSave != null) {
                    studyRepository.saveNovaMemory(response.memoryToSave)
                    _snackbarMessage.emit("🧠 Nova remembered: \"${response.memoryToSave.key}\"")
                }
            }.onFailure { error ->
                val fallbackMessage = NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = "Boss, network issue ki wajah se offline mode use kar raha hoon. Let me know if you want to start a Focus session or Quick Quiz! 🚀"
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
                NovaActionType.OPEN_APP_BLOCKING -> {
                    _navigationEvent.emit("NAVIGATE_TO_FOCUS" to emptyMap())
                }
                NovaActionType.OPEN_MEMORY -> {
                    setTab(NovaScreenTab.MEMORY_CENTER)
                }
                NovaActionType.OPEN_SETTINGS -> {
                    setTab(NovaScreenTab.NOVA_SETTINGS)
                }
                else -> {}
            }
        }
    }

    // --- Interactive Quiz Mode ---
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
                // Fallback default questions
                val fallbackList = listOf(
                    Question(
                        id = "q1",
                        questionText = "Which law states that the induced electromotive force in any closed circuit is equal to the negative of the time rate of change of the magnetic flux?",
                        options = listOf("Faraday's Law", "Ampere's Circuital Law", "Coulomb's Law", "Biot-Savart Law"),
                        correctOptionIndex = 0,
                        explanation = "Faraday's Law of Electromagnetic Induction states that EMF = -dΦ/dt (Lenz's law provides the direction).",
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
                        questionText = "A dielectric slab is inserted between the plates of an isolated charged capacitor. What happens to its capacitance?",
                        options = listOf("Decreases", "Increases", "Remains constant", "Becomes zero"),
                        correctOptionIndex = 1,
                        explanation = "Inserting a dielectric of constant K multiplies capacitance by K (C = K·C0).",
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

    // --- Image Handling ---
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

    // --- Memory Management ---
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
                _snackbarMessage.emit("Saved to Nova Memory: \"$key\"")
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
            _snackbarMessage.emit("All Nova memories cleared")
        }
    }

    // --- Settings Updates ---
    fun updateSettings(newSettings: NovaSettings) {
        _settings.value = newSettings
    }

    fun triggerProactiveCoachAlert() {
        StudyNotificationManager.sendNovaStudyReminder(
            context = getApplication(),
            userName = _studyContext.value.preferredTitle,
            subject = _studyContext.value.subjects.firstOrNull() ?: "Physics",
            topic = _studyContext.value.weakTopics.firstOrNull() ?: "Current Electricity",
            durationMins = 25,
            isTest = true
        )
        viewModelScope.launch {
            _snackbarMessage.emit("Sent Proactive Smart Study notification!")
        }
    }

    fun triggerExcessiveUsageAlert() {
        StudyNotificationManager.sendNovaExcessiveAppUsageAlert(
            context = getApplication(),
            userName = _studyContext.value.preferredTitle,
            appName = "YouTube",
            pendingSubject = _studyContext.value.subjects.firstOrNull() ?: "Physics",
            durationMins = 25,
            isTest = true
        )
        viewModelScope.launch {
            _snackbarMessage.emit("Sent Excessive App Usage study prompt!")
        }
    }

    fun selectTab(tab: NovaScreenTab) {
        _currentTab.value = tab
    }

    fun askPromptFromDashboard(prompt: String) {
        _currentTab.value = NovaScreenTab.ASSISTANT_CHAT
        sendMessage(prompt)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}

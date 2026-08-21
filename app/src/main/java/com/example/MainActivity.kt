package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import com.example.ui.theme.*
import com.example.ui.components.AppNavTab
import com.example.ui.components.FloatingGlassNavBar
import com.example.ui.screens.learning.*
import com.example.ui.screens.auth.LoginScreen

import com.example.ui.screens.auth.OnboardingScreen
import com.example.ui.screens.auth.PermissionSetupScreen
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.document.DocumentSummarizerScreen
import com.example.ui.screens.focus.FocusModeScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.nova.NovaScreen
import com.example.ui.screens.planner.StudyPlannerScreen
import com.example.ui.screens.planner.StudySessionTimerView
import com.example.data.model.ExamContext
import com.example.ui.screens.profile.ProfileSettingsScreen
import com.example.ui.screens.progress.ProgressDashboardScreen
import com.example.ui.screens.progress.ExamReadinessCenterScreen
import com.example.ui.screens.tutor.GeminiTutorScreen
import com.example.ui.theme.StudyMateTheme
import com.example.ui.theme.appBackgroundGradient
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory
import com.example.viewmodel.NovaViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(application as StudyMateApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            StudyMateTheme(
                darkTheme = isDarkTheme,
                themeMode = themeMode,
                onToggleTheme = { viewModel.updateTheme(!isDarkTheme) },
                onSetTheme = { viewModel.updateTheme(it) },
                onSetThemeMode = { viewModel.updateThemeMode(it) }
            ) {
                StudyMateAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StudyMateAppContent(viewModel: MainViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authErrorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()

    var isSplashFinished by remember { mutableStateOf(false) }
    var tabStack by remember { mutableStateOf(listOf(AppNavTab.HOME)) }
    val currentTab = tabStack.lastOrNull() ?: AppNavTab.HOME
    var showProfileSettings by remember { mutableStateOf(false) }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val onSelectTab: (AppNavTab) -> Unit = { selected ->
        if (selected == AppNavTab.HOME) {
            tabStack = listOf(AppNavTab.HOME)
        } else {
            val idx = tabStack.indexOf(selected)
            tabStack = if (idx >= 0) {
                tabStack.subList(0, idx + 1)
            } else {
                tabStack + selected
            }
        }
    }

    // Dynamic Notification Permission Request (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Splash Screen Phase
    if (!isSplashFinished) {
        SplashScreen(
            onSplashFinished = { isSplashFinished = true }
        )
        return
    }

    // Unauthenticated -> Login Screen
    val context = androidx.compose.ui.platform.LocalContext.current
    if (userProfile == null) {
        LoginScreen(
            isLoading = isAuthLoading,
            errorMessage = authErrorMessage,
            onGoogleSignIn = { viewModel.signInWithGoogle(context) },
            onEmailSignIn = { email, pass -> viewModel.signInWithEmail(email, pass) },
            onEmailSignUp = { email, pass, name, exam -> viewModel.signUpWithEmail(email, pass, name, exam) },
            onGuestSignIn = { viewModel.continueAsGuest() },
            onDismissError = { viewModel.clearAuthError() }
        )
        return
    }

    // First-Time User -> Setup / Permission Screen
    val appPrefs = remember(context) { context.getSharedPreferences("studymate_app_prefs", android.content.Context.MODE_PRIVATE) }
    var isPermissionSetupCompleted by remember {
        mutableStateOf(appPrefs.getBoolean("has_completed_permission_setup", false))
    }

    if (!isPermissionSetupCompleted) {
        PermissionSetupScreen(
            onCompleteSetup = {
                appPrefs.edit().putBoolean("has_completed_permission_setup", true).apply()
                isPermissionSetupCompleted = true
            }
        )
        return
    }

    // First-Time User -> Onboarding Screen
    if (!userProfile!!.isOnboardingCompleted) {
        OnboardingScreen(
            initialName = userProfile?.name ?: "",
            onComplete = { profile ->
                viewModel.saveOnboarding(profile)
            }
        )
        return
    }

    // Authenticated & Onboarded Main Application Flow
    val studyPlan by viewModel.studyPlanItems.collectAsStateWithLifecycle()
    val isPlanGenerating by viewModel.isPlanGenerating.collectAsStateWithLifecycle()
    val missions by viewModel.dailyMissions.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val useThinkingMode by viewModel.useThinkingMode.collectAsStateWithLifecycle()
    val tutorPersona by viewModel.tutorPersona.collectAsStateWithLifecycle()
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsStateWithLifecycle()
    val focusState by viewModel.focusState.collectAsStateWithLifecycle()
    val mockAttempts by viewModel.mockTestAttempts.collectAsStateWithLifecycle()
    val mistakes by viewModel.mistakes.collectAsStateWithLifecycle()
    val userQuestionMaterials by viewModel.userQuestionMaterials.collectAsStateWithLifecycle()
    val flashcards by viewModel.flashcards.collectAsStateWithLifecycle()
    val isFlashcardGenerating by viewModel.isFlashcardGenerating.collectAsStateWithLifecycle()
    val flashcardMessage by viewModel.flashcardMessage.collectAsStateWithLifecycle()
    val pendingResumeSession by viewModel.pendingResumeSession.collectAsStateWithLifecycle()
    val activeTestState by viewModel.activeTestState.collectAsStateWithLifecycle()
    val isTestGenerating by viewModel.isTestGenerating.collectAsStateWithLifecycle()
    val generationError by viewModel.generationError.collectAsStateWithLifecycle()
    val insufficientPyqNotice by viewModel.insufficientPyqNotice.collectAsStateWithLifecycle()
    val mistakeDiagnosis by viewModel.mistakeDiagnosis.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val notifPrefs by viewModel.notificationPrefs.collectAsStateWithLifecycle()
    val studentMasterContext by viewModel.studentMasterContext.collectAsStateWithLifecycle()
    val allCatalogExams by viewModel.allCatalogExams.collectAsStateWithLifecycle()

    val allExamObjectives by viewModel.allExamObjectives.collectAsStateWithLifecycle()
    val activeExamObjective by viewModel.activeExamObjective.collectAsStateWithLifecycle()
    val allTopicMasteries by viewModel.allTopicMasteries.collectAsStateWithLifecycle()
    val studentSessionHistory by viewModel.studentSessionHistory.collectAsStateWithLifecycle()
    val latestIntelligenceSnapshot by viewModel.latestIntelligenceSnapshot.collectAsStateWithLifecycle()
    val allFocusSessions by viewModel.allFocusSessions.collectAsStateWithLifecycle()

    val subjectProgressSummaries by viewModel.subjectProgressSummaries.collectAsStateWithLifecycle()
    val examReadinessScore by viewModel.examReadinessScore.collectAsStateWithLifecycle()
    val studyRecommendations by viewModel.studyRecommendations.collectAsStateWithLifecycle()
    val dailyStudyPlan by viewModel.dailyStudyPlan.collectAsStateWithLifecycle()

    val userStudyPreferences by viewModel.userStudyPreferences.collectAsStateWithLifecycle()
    val deadlineWarning by viewModel.deadlineWarning.collectAsStateWithLifecycle()
    val activeStudySession by viewModel.activeStudySession.collectAsStateWithLifecycle()
    val sessionRemainingSeconds by viewModel.sessionRemainingSeconds.collectAsStateWithLifecycle()
    val isSessionTimerRunning by viewModel.isSessionTimerRunning.collectAsStateWithLifecycle()
    val isSessionPaused by viewModel.isSessionPaused.collectAsStateWithLifecycle()
    val activeSessionActualMinutes by viewModel.activeSessionActualMinutes.collectAsStateWithLifecycle()
    val activeExamContext by viewModel.activeExamContext.collectAsStateWithLifecycle()

    val documentAnalysis by viewModel.documentAnalysisState.collectAsStateWithLifecycle()
    val isDocumentParsing by viewModel.isDocumentParsing.collectAsStateWithLifecycle()
    val documentError by viewModel.documentError.collectAsStateWithLifecycle()
    var showDocumentSummarizer by remember { mutableStateOf(false) }
    var showExamReadinessCenter by remember { mutableStateOf(false) }

    val aiCoachRecommendation by viewModel.aiCoachRecommendation.collectAsStateWithLifecycle()
    val studyNowRecommendation by viewModel.studyNowRecommendation.collectAsStateWithLifecycle()
    val isAiCoachLoading by viewModel.isAiCoachLoading.collectAsStateWithLifecycle()
    val isStudyNowLoading by viewModel.isStudyNowLoading.collectAsStateWithLifecycle()
    val completeStudyKit by viewModel.completeStudyKit.collectAsStateWithLifecycle()
    val isStudyKitGenerating by viewModel.isStudyKitGenerating.collectAsStateWithLifecycle()

    // Step 14 Learning Engine State
    val activeLearningContent by viewModel.activeLearningContent.collectAsStateWithLifecycle()
    val isLearningContentLoading by viewModel.isLearningContentLoading.collectAsStateWithLifecycle()
    val novaDoubtResponse by viewModel.novaDoubtResponse.collectAsStateWithLifecycle()
    val isNovaDoubtThinking by viewModel.isNovaDoubtThinking.collectAsStateWithLifecycle()
    val novaProgressAnalysis by viewModel.novaProgressAnalysis.collectAsStateWithLifecycle()
    val isNovaProgressAnalyzing by viewModel.isNovaProgressAnalyzing.collectAsStateWithLifecycle()
    val allLearningBookmarks by viewModel.allLearningBookmarks.collectAsStateWithLifecycle()
    val smartNotes by viewModel.allSmartNotes.collectAsStateWithLifecycle()


    var studySubTab by remember { mutableIntStateOf(0) } // 0: Study Planner & Cards, 1: Smart Content Engine
    var selectedLearningSubject by remember { mutableStateOf<String?>(null) }
    var selectedLearningChapter by remember { mutableStateOf<String?>("General Chapter") }
    var selectedLearningTopic by remember { mutableStateOf<String?>(null) }
    var showSavedLearning by remember { mutableStateOf(false) }

    val activity = context as? Activity
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        when {
            showSavedLearning -> {
                showSavedLearning = false
            }
            selectedLearningTopic != null -> {
                selectedLearningTopic = null
            }
            selectedLearningSubject != null -> {
                selectedLearningSubject = null
            }
            showProfileSettings -> {
                showProfileSettings = false
            }

            showDocumentSummarizer -> {
                showDocumentSummarizer = false
            }
            activeTestState.isTestInProgress -> {
                if (activeTestState.isSubmitConfirmOpen) {
                    viewModel.setSubmitConfirmOpen(false)
                } else if (activeTestState.isPaletteOpen) {
                    viewModel.setPaletteOpen(false)
                } else {
                    viewModel.setSubmitConfirmOpen(true)
                }
            }
            activeTestState.isCompleted -> {
                viewModel.exitTest()
            }
            tabStack.size > 1 -> {
                tabStack = tabStack.dropLast(1)
            }
            else -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000L) {
                    activity?.finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Hide bottom bar during active fullscreen mock test, document summarizer, or profile settings
            if (!activeTestState.isTestInProgress && !activeTestState.isCompleted && !showDocumentSummarizer && !showProfileSettings) {
                FloatingGlassNavBar(
                    currentTab = currentTab,
                    onTabSelected = { onSelectTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundGradient(isDarkTheme, themeMode))
                .padding(innerPadding)
        ) {
            if (showProfileSettings) {
                ProfileSettingsScreen(
                    user = userProfile,
                    themeMode = themeMode,
                    isDarkTheme = isDarkTheme,
                    notificationPrefs = notifPrefs,
                    onSetThemeMode = { viewModel.updateThemeMode(it) },
                    onToggleDarkTheme = { viewModel.updateTheme(it) },
                    onUpdateNotificationPrefs = { viewModel.updateNotificationPrefs(it) },
                    onUpdateProfile = { viewModel.updateUserProfile(it, refreshStudyPlan = true) },
                    onSignOut = { viewModel.signOut(context) },
                    onDeleteAccount = { viewModel.deleteAccount() },
                    onBack = { showProfileSettings = false },
                    onTestStudyReminder = { viewModel.testStudySessionReminder() },
                    onTestExamCountdown = { viewModel.testExamCountdownReminder() },
                    onTestDailyGoal = { viewModel.testDailyGoalReminder() },
                    onTestMissedStudy = { viewModel.testMissedStudyReminder() },
                    onTestBreakReminder = { viewModel.testBreakReminder() },
                    onTestFocusStarted = { viewModel.testFocusStartedNotification() },
                    onTestFocusCompleted = { viewModel.testFocusCompletedNotification() },
                    onTestDailyMotivation = { viewModel.testDailyMotivationalNotification() },
                    catalogExams = allCatalogExams,
                    onChangeExam = { examId ->
                        viewModel.changeSelectedExam(examId = examId, refreshStudyPlan = true)
                    },
                    onOpenStudyPlanner = {
                        showProfileSettings = false
                        onSelectTab(AppNavTab.STUDY)
                    },
                    onResetActiveExamData = {
                        viewModel.resetActiveExamPreparationData()
                    },
                    isAiThinkingMode = useThinkingMode,
                    onSetAiThinkingMode = { viewModel.setThinkingMode(it) },
                    tutorPersona = tutorPersona,
                    onSetTutorPersona = { viewModel.setTutorPersona(it) }
                )
            } else if (showExamReadinessCenter) {
                ExamReadinessCenterScreen(
                    user = userProfile,
                    examReadiness = examReadinessScore,
                    subjectSummaries = subjectProgressSummaries,
                    topicMasteries = allTopicMasteries,
                    mockAttempts = mockAttempts,
                    onStartFocusSession = { sub, top ->
                        viewModel.startFocusSession(sub, top, 25)
                        showExamReadinessCenter = false
                        onSelectTab(AppNavTab.FOCUS)
                    },
                    onNavigateToMocks = {
                        showExamReadinessCenter = false
                        onSelectTab(AppNavTab.PROGRESS)
                    },
                    onNavigateToRevision = {
                        showExamReadinessCenter = false
                        onSelectTab(AppNavTab.STUDY)
                    },
                    onBack = { showExamReadinessCenter = false }
                )
            } else if (showDocumentSummarizer) {
                DocumentSummarizerScreen(
                    analysisResult = documentAnalysis,
                    isLoading = isDocumentParsing,
                    errorMessage = documentError,
                    onSelectDocumentUri = { viewModel.processDocumentUri(it) },
                    onAnalyzeDirectText = { title, text -> viewModel.analyzeDirectText(title, text) },
                    onClearAnalysis = { viewModel.clearDocumentAnalysis() },
                    onSaveQuestionAsFlashcard = { q ->
                        viewModel.convertStudyQuestionToFlashcard(userProfile?.subjects?.firstOrNull() ?: "General", q)
                    },
                    onBack = { showDocumentSummarizer = false },
                    completeStudyKit = completeStudyKit,
                    isStudyKitGenerating = isStudyKitGenerating,
                    onGenerateStudyKit = { uri -> viewModel.generateCompleteStudyKit(uri, userProfile?.subjects?.firstOrNull() ?: "General", context) }
                )
            } else {
                Crossfade(
                    targetState = currentTab,
                    label = "tab_crossfade"
                ) { tab ->
                    when (tab) {
                        AppNavTab.HOME -> HomeScreen(
                            user = userProfile,
                            studyPlan = studyPlan,
                            missions = missions,
                            flashcards = flashcards,
                            studentMasterContext = studentMasterContext,
                            aiCoachRecommendation = aiCoachRecommendation,
                            studyNowRecommendation = studyNowRecommendation,
                            isAiCoachLoading = isAiCoachLoading,
                            isStudyNowLoading = isStudyNowLoading,
                            examReadiness = examReadinessScore,
                            onLoadAiCoach = { sub -> viewModel.loadAiCoachRecommendation(sub) },
                            onLoadStudyNow = { viewModel.loadStudyNowRecommendation() },
                            onSelectTimeAvailable = { mins -> viewModel.setSelectedAvailableTime(mins) },
                            onPerformSmartSearch = { q ->
                                viewModel.performSmartSearch(q)
                                onSelectTab(AppNavTab.AI_TUTOR)
                            },
                            onTogglePlanItem = { id, done -> viewModel.togglePlanItem(id, done) },
                            onStartFocusSession = { sub, top ->
                                viewModel.startFocusSession(sub, top, 25)
                                onSelectTab(AppNavTab.FOCUS)
                            },
                            onNavigateToTab = { onSelectTab(it) },
                            onOpenProfileSettings = { showProfileSettings = true },
                            onOpenExamReadinessCenter = { showExamReadinessCenter = true },
                            onSignOut = { viewModel.signOut(context) },
                            onScanQuestion = { onSelectTab(AppNavTab.AI_TUTOR) },
                            onOpenDocumentSummarizer = { showDocumentSummarizer = true },
                            onUpdateUserProfile = { updatedProfile -> viewModel.updateUserProfile(updatedProfile) }
                        )

                        AppNavTab.AI_TUTOR -> {
                            val novaViewModel: NovaViewModel = viewModel()
                            NovaScreen(
                                viewModel = novaViewModel,
                                onNavigateToFocus = { sub, top, mins ->
                                    viewModel.startFocusSession(sub, top, mins)
                                    onSelectTab(AppNavTab.FOCUS)
                                },
                                onNavigateToPlanner = { onSelectTab(AppNavTab.STUDY) },
                                onOpenDocumentSummarizer = { showDocumentSummarizer = true }
                            )
                        }

                        AppNavTab.STUDY -> {
                            if (activeStudySession != null) {
                                StudySessionTimerView(
                                    activeSession = activeStudySession!!,
                                    remainingSeconds = sessionRemainingSeconds,
                                    isTimerRunning = isSessionTimerRunning,
                                    isPaused = isSessionPaused,
                                    actualMinutesSpent = activeSessionActualMinutes,
                                    onPauseTimer = { viewModel.pauseStudySession() },
                                    onResumeTimer = { viewModel.resumeStudySession() },
                                    onFinishSession = { notes: String -> viewModel.finishStudySession(notes) },
                                    onCancelSession = { viewModel.cancelStudySession() }
                                )
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Segmented Toggle Header
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = androidx.compose.ui.graphics.Color(0xFF0F172A).copy(alpha = 0.9f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = studySubTab == 0,
                                                onClick = { studySubTab = 0 },
                                                label = { Text("📅 Daily Plan & Cards", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = com.example.ui.theme.NeonCyan,
                                                    selectedLabelColor = androidx.compose.ui.graphics.Color(0xFF070B19),
                                                    containerColor = androidx.compose.ui.graphics.Color(0x20FFFFFF),
                                                    labelColor = androidx.compose.ui.graphics.Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = studySubTab == 1,
                                                onClick = { studySubTab = 1 },
                                                label = { Text("📖 Smart Content Engine", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = com.example.ui.theme.ElectricViolet,
                                                    selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                                                    containerColor = androidx.compose.ui.graphics.Color(0x20FFFFFF),
                                                    labelColor = androidx.compose.ui.graphics.Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        if (studySubTab == 0) {
                                            StudyPlannerScreen(
                                                planItems = studyPlan,
                                                flashcards = flashcards,
                                                user = userProfile,
                                                isGenerating = isPlanGenerating,
                                                onGenerateAiPlan = { viewModel.generateAdaptiveDailyPlan() },
                                                onTogglePlanItem = { id, done -> viewModel.togglePlanItem(id, done) },
                                                onAddPlanItem = { sub, chap, top, mins, prio -> viewModel.addManualPlanItem(sub, chap, top, mins, prio) },
                                                onUpdatePlanItem = { item -> viewModel.updatePlanItem(item) },
                                                onDeletePlanItem = { viewModel.deletePlanItem(it) },
                                                onStartFocusSession = { sub, top ->
                                                    viewModel.startFocusSession(sub, top, 25)
                                                    onSelectTab(AppNavTab.FOCUS)
                                                },
                                                onRecoverMissedSessions = { mode -> viewModel.recoverMissedSessions(mode) },
                                                onUpdateDailyAvailableTime = { mins -> viewModel.generateAdaptiveDailyPlan(overrideAvailableMinutes = mins) },
                                                onStartSessionTimer = { item -> viewModel.startStudySession(item) },
                                                deadlineWarning = deadlineWarning,
                                                userPreferences = userStudyPreferences,
                                                onSavePreferences = { prefs -> viewModel.saveUserPreferences(prefs) },
                                                activeExamContext = activeExamContext ?: ExamContext(),
                                                topicMasteries = allTopicMasteries,
                                                focusSessions = allFocusSessions,
                                                onApplySubjectAllocations = { subMinutes, totalMins, startHr, breakMins ->
                                                    viewModel.applySubjectTimeAllocations(subMinutes, totalMins, startHr, 0, breakMins)
                                                },
                                                onOpenExamSelector = { onSelectTab(AppNavTab.HOME) },
                                                activeStudySession = activeStudySession,
                                                sessionRemainingSeconds = sessionRemainingSeconds,
                                                isSessionTimerRunning = isSessionTimerRunning,
                                                isSessionPaused = isSessionPaused,
                                                activeSessionActualMinutes = activeSessionActualMinutes,
                                                onPauseTimer = { viewModel.pauseStudySession() },
                                                onResumeTimer = { viewModel.resumeStudySession() },
                                                onFinishSession = { notes -> viewModel.finishStudySession(notes) },
                                                onCancelSession = { viewModel.cancelStudySession() },
                                                onAddFlashcard = { subject, topic, front, back, hint, difficulty, sourceDoc ->
                                                    viewModel.addFlashcard(subject, topic, front, back, hint, difficulty, sourceDoc)
                                                },
                                                onUpdateFlashcard = { viewModel.updateFlashcard(it) },
                                                onDeleteFlashcard = { viewModel.deleteFlashcard(it) },
                                                onReviewFlashcard = { id, status, conf ->
                                                    viewModel.reviewFlashcard(id, status, conf)
                                                },
                                                onReviewSpaced = { id, quality ->
                                                    viewModel.recordSpacedFlashcardReview(id, quality)
                                                },
                                                onGenerateAiCards = { subject, topic ->
                                                    viewModel.generateAiFlashcards(subject, topic)
                                                },
                                                onGenerateFromNotes = { title, notesText, subject, count ->
                                                    viewModel.generateFlashcardsFromNotes(title, notesText, subject, count)
                                                },
                                                onGenerateFromDocumentUri = { uri, subject, count ->
                                                    viewModel.generateFlashcardsFromDocumentUri(uri, subject, count)
                                                },
                                                flashcardStatusMessage = flashcardMessage,
                                                onClearFlashcardStatusMessage = { viewModel.clearFlashcardMessage() },
                                                isFlashcardGenerating = isFlashcardGenerating
                                            )
                                        } else {
                                            when {
                                                selectedLearningTopic != null -> {
                                                    val sub = selectedLearningSubject ?: "General"
                                                    val chap = selectedLearningChapter ?: "General Chapter"
                                                    val top = selectedLearningTopic!!

                                                    LaunchedEffect(sub, chap, top) {
                                                        viewModel.loadLearningTopicContent(sub, chap, top)
                                                    }

                                                    TopicDetailScreen(
                                                        examContext = activeExamContext ?: ExamContext(),
                                                        subject = sub,
                                                        chapter = chap,
                                                        topic = top,
                                                        topicMastery = allTopicMasteries.firstOrNull { it.topic.equals(top, ignoreCase = true) },
                                                        learningContent = activeLearningContent,
                                                        isLoading = isLearningContentLoading,
                                                        userNotes = smartNotes.firstOrNull { it.topic.equals(top, ignoreCase = true) }?.contentMarkdown ?: "",
                                                        userMistakes = mistakes.filter { it.topic.equals(top, ignoreCase = true) },
                                                        onBack = { selectedLearningTopic = null },
                                                        onRefreshContent = { viewModel.loadLearningTopicContent(sub, chap, top, forceRefresh = true) },
                                                        onSaveNote = { noteText -> viewModel.saveTopicNote(sub, top, noteText) },
                                                        onToggleBookmark = { title, snippet, type -> viewModel.toggleLearningBookmark(sub, top, title, snippet, type) },
                                                        onCompleteQuickTest = { score, total -> viewModel.completeQuickTest(sub, top, score, total) },
                                                        onAskNovaDoubt = { prompt -> viewModel.askNovaTopicDoubt(sub, chap, top, prompt) },
                                                        novaDoubtResponse = novaDoubtResponse,
                                                        isNovaThinking = isNovaDoubtThinking,
                                                        onSpeakTts = { text -> viewModel.speakText(text) }
                                                    )
                                                }
                                                selectedLearningSubject != null -> {
                                                    val sub = selectedLearningSubject!!
                                                    SubjectDetailScreen(
                                                        examContext = activeExamContext ?: ExamContext(),
                                                        subjectName = sub,
                                                        subjectSummary = subjectProgressSummaries.firstOrNull { it.subjectName.equals(sub, ignoreCase = true) },
                                                        topicMasteries = allTopicMasteries.filter { it.subject.equals(sub, ignoreCase = true) },
                                                        onBack = { selectedLearningSubject = null },
                                                        onSelectTopic = { chapName, topName ->
                                                            selectedLearningChapter = chapName
                                                            selectedLearningTopic = topName
                                                        }
                                                    )
                                                }
                                                showSavedLearning -> {
                                                     com.example.ui.screens.learning.SavedLearningScreen(
                                                         bookmarks = allLearningBookmarks,
                                                         smartNotes = smartNotes,
                                                         onOpenTopic = { sub, top ->
                                                             showSavedLearning = false
                                                             selectedLearningSubject = sub
                                                             selectedLearningChapter = "General Chapter"
                                                             selectedLearningTopic = top
                                                         },
                                                         onDeleteBookmark = { viewModel.deleteLearningBookmark(it) },
                                                         onDeleteNote = { viewModel.deleteTopicNote(it) },
                                                         onBack = { showSavedLearning = false }
                                                     )
                                                 }
                                                 else -> {
                                                    LearningDashboardScreen(
                                                        user = userProfile,
                                                        examContext = activeExamContext ?: ExamContext(),
                                                        subjectSummaries = subjectProgressSummaries,
                                                        allMasteries = allTopicMasteries,
                                                        bookmarks = allLearningBookmarks,
                                                        smartNotes = smartNotes,
                                                        onSelectSubject = { selectedLearningSubject = it },
                                                        onSelectTopic = { sub, chap, top ->
                                                            selectedLearningSubject = sub
                                                            selectedLearningChapter = chap
                                                            selectedLearningTopic = top
                                                        },
                                                        onOpenSearch = { query ->
                                                            viewModel.performSmartSearch(query)
                                                            onSelectTab(AppNavTab.AI_TUTOR)
                                                        },
                                                        onChangeExam = { showProfileSettings = true },
                                                        onOpenSavedLearning = { showSavedLearning = true },
                                                        onAskNova = { onSelectTab(AppNavTab.AI_TUTOR) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        }

                        AppNavTab.FOCUS -> FocusModeScreen(
                            focusState = focusState,
                            onStartFocus = { sub, top, mins -> viewModel.startFocusSession(sub, top, mins) },
                            onTogglePause = { viewModel.toggleFocusPause() },
                            onEndSession = { viewModel.endFocusSession() },
                            onDismissCelebration = { viewModel.dismissCelebration() },
                            activeStudySession = activeStudySession,
                            dailyStudyPlan = studyPlan,
                            allFocusSessions = allFocusSessions,
                            userProfile = userProfile,
                            activeExamContext = activeExamContext,
                            onStartNextSession = { item -> viewModel.startStudySession(item) },
                            onAskNova = { sub, ch, top, prompt -> viewModel.askNovaTopicDoubt(sub, ch, top, prompt, userProfile?.languagePreference ?: "English") },
                            onSaveNote = { sub, top, text -> viewModel.saveSessionNote(sub, top, text) },
                            onQuickRevision = { sub, top ->
                                onSelectTab(AppNavTab.STUDY)
                            },
                            onStartBreak = { mins -> viewModel.startBreakTimer(mins) },
                            onEndBreak = { viewModel.endBreakTimer() },
                            novaDoubtResponse = novaDoubtResponse,
                            isNovaDoubtThinking = isNovaDoubtThinking
                        )

                        AppNavTab.PROGRESS -> ProgressDashboardScreen(
                            user = userProfile,
                            attempts = mockAttempts,
                            mistakes = mistakes,
                            userMaterials = userQuestionMaterials,
                            examObjective = activeExamObjective,
                            topicMasteries = allTopicMasteries,
                            sessionHistory = studentSessionHistory,
                            allFocusSessions = allFocusSessions,
                            snapshot = latestIntelligenceSnapshot,
                            activeTestState = activeTestState,
                            isTestGenerating = isTestGenerating,
                            generationError = generationError,
                            insufficientPyqNotice = insufficientPyqNotice,
                            mistakeDiagnosis = mistakeDiagnosis,
                            onStartTestWithConfig = { config -> viewModel.startMockTestWithConfig(config) },
                            onSelectAnswer = { qIdx, optIdx -> viewModel.selectTestAnswer(qIdx, optIdx) },
                            onClearAnswer = { qIdx -> viewModel.clearTestAnswer(qIdx) },
                            onToggleMarkForReview = { qIdx -> viewModel.toggleMarkForReview(qIdx) },
                            onSkipQuestion = { qIdx -> viewModel.skipQuestion(qIdx) },
                            onNavigateQuestion = { idx -> viewModel.navigateTestQuestion(idx) },
                            onSetPaletteOpen = { isOpen -> viewModel.setPaletteOpen(isOpen) },
                            onSetSubmitConfirmOpen = { isOpen -> viewModel.setSubmitConfirmOpen(isOpen) },
                            onSubmitTest = { viewModel.submitMockTest() },
                            onExitTest = { viewModel.exitTest() },
                            onReviewPastTest = { attempt -> viewModel.reviewPastTest(attempt) },
                            onRetakeTest = { attempt -> viewModel.retakeMockTest(attempt) },
                            onRetakeWrongQuestions = { viewModel.retryWrongQuestions() },
                            onRetryUnanswered = { viewModel.retryUnansweredQuestions() },
                            onStartPractice = { rec -> viewModel.startTargetedPractice(rec) },
                            onDeletePastTest = { id -> viewModel.deletePastTest(id) },
                            onClearGenerationError = { viewModel.clearGenerationError() },
                            onConfirmStartWithAvailablePyqs = { viewModel.confirmStartWithAvailablePyqs() },
                            onConfirmAddAiToPyqs = { viewModel.confirmAddAiToPyqs() },
                            onDismissInsufficientPyqNotice = { viewModel.dismissInsufficientPyqNotice() },
                            onCancelTestGeneration = { viewModel.cancelTestGeneration() },
                            onSaveAndNext = { viewModel.saveAndNext() },
                            onMarkForReviewAndNext = { viewModel.markForReviewAndNext() },
                            onPreviousQuestion = { viewModel.previousQuestion() },
                            onSaveUserMaterial = { title, exam, subject, topic, rawText ->
                                viewModel.saveUserQuestionMaterial(title, exam, subject, topic, rawText)
                            },
                            onDeleteUserMaterial = { id -> viewModel.deleteUserQuestionMaterial(id) },
                            onDiagnoseMistakes = { subject -> viewModel.diagnoseMistakes(subject) },
                            onMarkMistakeMastered = { id, mastered -> viewModel.markMistakeMastered(id, mastered) },
                            onSaveExamObjective = { obj -> viewModel.saveExamObjective(obj) },
                            onStartFocusOnTopic = { sub, top ->
                                viewModel.startFocusSession(sub, top, 25)
                                onSelectTab(AppNavTab.FOCUS)
                            },
                            examReadiness = examReadinessScore,
                            subjectSummaries = subjectProgressSummaries,
                            recommendations = studyRecommendations,
                            dailyPlan = dailyStudyPlan,
                            onSetManualTopicOverride = { sub, top, override -> viewModel.setUserManualTopicOverride(sub, top, override) },
                            onResetPreparationData = { viewModel.resetActiveExamPreparationData() },
                            onOpenReadinessCenter = { showExamReadinessCenter = true },
                            onBack = {
                                if (tabStack.size > 1) {
                                    tabStack = tabStack.dropLast(1)
                                } else {
                                    onSelectTab(AppNavTab.HOME)
                                }
                            },
                            onNavigateToStudy = { onSelectTab(AppNavTab.STUDY) },
                            activeExamContext = activeExamContext,
                            novaProgressAnalysis = novaProgressAnalysis,
                            isNovaProgressAnalyzing = isNovaProgressAnalyzing,
                            onGenerateNovaProgressAnalysis = { exam, summary, lang ->
                                viewModel.generateNovaProgressAnalysis(exam, summary, lang)
                            }
                        )
                    }
                }
            }

            // Unfinished Test Session Recovery Modal Dialog
            pendingResumeSession?.let { restored ->
                AlertDialog(
                    onDismissRequest = { viewModel.discardPendingTestSession() },
                    title = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Restore,
                                contentDescription = null,
                                tint = GoldenSpark,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Unfinished Test Session",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = restored.title,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${restored.questions.size} Questions • ${restored.selectedAnswers.size} Answered",
                                color = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Time Remaining: ${String.format("%02d:%02d", restored.remainingSeconds / 60, restored.remainingSeconds % 60)}",
                                color = NeonCyan,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Would you like to resume your active test session or discard it?",
                                color = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.resumePendingTestSession() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = androidx.compose.ui.graphics.Color(0xFF050814)
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                        ) {
                            Text("Resume Test", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.discardPendingTestSession() }
                        ) {
                            Text("Discard", color = CoralRose)
                        }
                    },
                    containerColor = androidx.compose.ui.graphics.Color(0xFF0F172A),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

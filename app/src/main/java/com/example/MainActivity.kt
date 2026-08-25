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
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.screens.progress.ActiveMockTestScreen
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
import com.example.ui.screens.study.StudyHubScreen
import com.example.ui.screens.practice.PracticeHubScreen
import com.example.ui.screens.updates.UpdatesHubScreen
import com.example.ui.screens.auth.LoginScreen

import com.example.ui.screens.auth.OnboardingScreen
import com.example.ui.screens.auth.PermissionSetupScreen
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.document.DocumentSummarizerScreen
import com.example.ui.screens.focus.FocusModeScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.intelligence.LiveExamIntelligenceScreen
import com.example.ui.screens.nova.NovaScreen
import com.example.ui.screens.nova.NovaFloatingAssistant
import com.example.ui.screens.planner.StudyPlannerScreen
import com.example.ui.screens.planner.StudySessionTimerView
import com.example.data.model.ExamContext
import com.example.data.model.NovaRecruitmentActionType
import com.example.ui.screens.profile.ProfileSettingsScreen
import com.example.ui.screens.notification.NotificationCenterScreen
import com.example.ui.screens.notification.DailyBriefingScreen
import com.example.ui.screens.vacancy.SmartVacancyScreen
import com.example.ui.components.InAppNotificationBanner
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
    var tabNames by rememberSaveable { mutableStateOf(listOf(AppNavTab.HOME.name)) }
    val tabStack = remember(tabNames) {
        tabNames.mapNotNull { name -> runCatching { AppNavTab.valueOf(name) }.getOrNull() }.ifEmpty { listOf(AppNavTab.HOME) }
    }
    val currentTab = tabStack.lastOrNull() ?: AppNavTab.HOME
    var showProfileSettings by remember { mutableStateOf(false) }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val onSelectTab: (AppNavTab) -> Unit = { selected ->
        if (selected == AppNavTab.HOME) {
            tabNames = listOf(AppNavTab.HOME.name)
        } else {
            val idx = tabNames.indexOf(selected.name)
            tabNames = if (idx >= 0) {
                tabNames.subList(0, idx + 1)
            } else {
                tabNames + selected.name
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
            onForgotPassword = { email ->
                viewModel.sendPasswordResetEmail(email) { _, msg ->
                    if (!msg.isNullOrBlank()) {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            },
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
    val novaViewModel: NovaViewModel = viewModel()
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

    // Step 21 Live Exam Intelligence State
    val liveExamFeedState by viewModel.liveExamFeedState.collectAsStateWithLifecycle()
    val isRefreshingLiveExam by viewModel.isRefreshingLiveExam.collectAsStateWithLifecycle()
    val showLiveExamIntelligenceScreen by viewModel.showLiveExamIntelligenceScreen.collectAsStateWithLifecycle()

    // Step 40 Smart Vacancy, Results & Admit Card Intelligence State
    val recruitmentFeedState by viewModel.recruitmentFeedState.collectAsStateWithLifecycle()
    val isRefreshingRecruitment by viewModel.isRefreshingRecruitment.collectAsStateWithLifecycle()
    val showSmartVacancyScreen by viewModel.showSmartVacancyScreen.collectAsStateWithLifecycle()
    val selectedRecruitmentDetail by viewModel.selectedRecruitmentDetail.collectAsStateWithLifecycle()
    val recruitmentNotificationSettings by viewModel.recruitmentNotificationSettings.collectAsStateWithLifecycle()
    val recruitmentOutbox by viewModel.recruitmentOutbox.collectAsStateWithLifecycle()
    val recruitmentDailyDigest by viewModel.recruitmentDailyDigest.collectAsStateWithLifecycle()
    val recruitmentDiagnostics by viewModel.recruitmentDiagnostics.collectAsStateWithLifecycle()

    // Step 30 Notification Center & Daily Briefing State
    val appNotifications by viewModel.appNotifications.collectAsStateWithLifecycle()
    val activeInAppBanner by viewModel.activeInAppBanner.collectAsStateWithLifecycle()
    val dailyBriefingData by viewModel.dailyBriefingData.collectAsStateWithLifecycle()
    var showNotificationCenter by remember { mutableStateOf(false) }
    var showDailyBriefingScreen by remember { mutableStateOf(false) }

    val handleDeepLink: (String, String) -> Unit = { link, payload ->
        showNotificationCenter = false
        showDailyBriefingScreen = false
        when {
            link.startsWith("recruitment://vacancy/") -> {
                val vId = link.removePrefix("recruitment://vacancy/")
                viewModel.setShowSmartVacancyScreen(true, initialTab = "VACANCY")
                val item = recruitmentFeedState.allActiveVacancies.find { it.id == vId }
                    ?: recruitmentFeedState.latestForYouVacancies.find { it.id == vId }
                if (item != null) viewModel.selectRecruitmentDetail(item)
            }
            link.startsWith("recruitment://result/") -> {
                val rId = link.removePrefix("recruitment://result/")
                viewModel.setShowSmartVacancyScreen(true, initialTab = "RESULT")
                val item = recruitmentFeedState.resultsList.find { it.id == rId }
                if (item != null) viewModel.selectRecruitmentDetail(item)
            }
            link.startsWith("recruitment://admit_card/") -> {
                val aId = link.removePrefix("recruitment://admit_card/")
                viewModel.setShowSmartVacancyScreen(true, initialTab = "ADMIT_CARD")
                val item = recruitmentFeedState.admitCardsList.find { it.id == aId }
                if (item != null) viewModel.selectRecruitmentDetail(item)
            }
            link.startsWith("recruitment://alerts") || link == "RECRUITMENT_ALERTS" -> {
                viewModel.setShowSmartVacancyScreen(true, initialTab = "ALERTS")
            }
            link.startsWith("recruitment://tracker") -> {
                viewModel.setShowSmartVacancyScreen(true, initialTab = "SAVED")
            }
            link == "CURRENT_AFFAIRS" || link == "EXAM_UPDATES" -> {
                viewModel.setShowLiveExamIntelligenceScreen(true)
            }
            link in listOf("VACANCY", "VACANCIES", "JOBS") -> {
                viewModel.setShowSmartVacancyScreen(true, initialTab = "VACANCY")
            }
            link in listOf("RESULTS", "RESULT") -> {
                viewModel.setShowSmartVacancyScreen(true, initialTab = "RESULT")
            }
            link in listOf("ADMIT_CARD", "ADMIT_CARDS", "HALL_TICKET") -> {
                viewModel.setShowSmartVacancyScreen(true, initialTab = "ADMIT_CARD")
            }
            link in listOf("NOTICES", "CORRECTION") -> {
                viewModel.setShowSmartVacancyScreen(true, initialTab = "NOTIFICATION")
            }
            link == "DAILY_BRIEFING" -> {
                showDailyBriefingScreen = true
            }
            link == "MOCK_TEST" -> {
                if (pendingResumeSession != null) {
                    viewModel.resumePendingTestSession()
                } else {
                    onSelectTab(AppNavTab.PROGRESS)
                }
            }
            link == "REVISION" -> {
                onSelectTab(AppNavTab.PROGRESS)
            }
            link == "FOCUS" -> {
                onSelectTab(AppNavTab.FOCUS)
            }
            link == "NOVA" -> {
                if (payload.isNotBlank()) {
                    novaViewModel.sendMessage(payload)
                }
                onSelectTab(AppNavTab.AI_TUTOR)
            }
            else -> {
                onSelectTab(AppNavTab.HOME)
            }
        }
    }

    var studySubTab by remember { mutableIntStateOf(0) } // 0: Study Planner & Cards, 1: Smart Content Engine
    var selectedLearningSubject by remember { mutableStateOf<String?>(null) }
    var selectedLearningChapter by remember { mutableStateOf<String?>("General Chapter") }
    var selectedLearningTopic by remember { mutableStateOf<String?>(null) }
    var showSavedLearning by remember { mutableStateOf(false) }

    LaunchedEffect(currentTab, activeTestState.isTestInProgress, selectedLearningSubject, selectedLearningTopic, userProfile?.examName) {
        novaViewModel.setAppContext(
            screenName = currentTab.name,
            subject = selectedLearningSubject ?: userProfile?.subjects?.firstOrNull(),
            topic = selectedLearningTopic,
            isTestActive = activeTestState.isTestInProgress,
            targetExam = userProfile?.examName ?: "General"
        )
    }

    LaunchedEffect(Unit) {
        novaViewModel.navigationEvent.collect { (event, data) ->
            when (event) {
                "NAVIGATE_TO_LIVE_EXAM_INTELLIGENCE" -> {
                    viewModel.setShowLiveExamIntelligenceScreen(true)
                }
                "NAVIGATE_TO_SMART_VACANCIES" -> {
                    val tab = data["tab"] as? String
                    val itemId = data["item_id"] as? String
                    viewModel.setShowSmartVacancyScreen(true, initialTab = tab)
                    if (!itemId.isNullOrBlank()) {
                        val item = recruitmentFeedState.allActiveVacancies.find { it.id == itemId }
                            ?: recruitmentFeedState.latestForYouVacancies.find { it.id == itemId }
                            ?: recruitmentFeedState.resultsList.find { it.id == itemId }
                            ?: recruitmentFeedState.admitCardsList.find { it.id == itemId }
                        if (item != null) {
                            viewModel.selectRecruitmentDetail(item)
                        }
                    }
                }
                "NAVIGATE_TO_FOCUS" -> {
                    val subject = data["subject"] as? String ?: userProfile?.subjects?.firstOrNull() ?: "General Science"
                    val topic = data["topic"] as? String ?: "Core Revision"
                    val duration = (data["duration"] as? Int) ?: 25
                    viewModel.startFocusSession(subject, topic, duration)
                    onSelectTab(AppNavTab.FOCUS)
                }
                "NAVIGATE_TO_PLANNER" -> {
                    onSelectTab(AppNavTab.STUDY)
                }
                "NAVIGATE_TO_MOCK_TEST" -> {
                    onSelectTab(AppNavTab.PROGRESS)
                }
                "NAVIGATE_TO_SUBJECT", "NAVIGATE_TO_TOPIC" -> {
                    onSelectTab(AppNavTab.STUDY)
                }
                "NAVIGATE_TO_SUMMARIZER" -> {
                    showDocumentSummarizer = true
                }
                "NAVIGATE_TO_READINESS" -> {
                    showExamReadinessCenter = true
                }
                "NAVIGATE_TO_NOTIFICATIONS" -> {
                    showNotificationCenter = true
                }
                "NAVIGATE_TO_PROFILE" -> {
                    showProfileSettings = true
                }
            }
        }
    }

    val activity = context as? Activity
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        when {
            showNotificationCenter -> {
                showNotificationCenter = false
            }
            showDailyBriefingScreen -> {
                showDailyBriefingScreen = false
            }
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
            showLiveExamIntelligenceScreen -> {
                viewModel.setShowLiveExamIntelligenceScreen(false)
            }
            showSmartVacancyScreen -> {
                viewModel.setShowSmartVacancyScreen(false)
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
            tabNames.size > 1 -> {
                tabNames = tabNames.dropLast(1)
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
            // Hide bottom bar during active fullscreen mock test, document summarizer, profile settings, or notification screens
            if (!activeTestState.isTestInProgress && !activeTestState.isCompleted && !showDocumentSummarizer && !showProfileSettings && !showLiveExamIntelligenceScreen && !showSmartVacancyScreen && !showNotificationCenter && !showDailyBriefingScreen) {
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
            if (activeTestState.isTestInProgress || activeTestState.isCompleted) {
                ActiveMockTestScreen(
                    state = activeTestState,
                    onSelectAnswer = { qIdx, optIdx -> viewModel.selectTestAnswer(qIdx, optIdx) },
                    onClearAnswer = { qIdx -> viewModel.clearTestAnswer(qIdx) },
                    onToggleMarkForReview = { qIdx -> viewModel.toggleMarkForReview(qIdx) },
                    onSkipQuestion = { qIdx -> viewModel.skipQuestion(qIdx) },
                    onNavigateQuestion = { idx -> viewModel.navigateTestQuestion(idx) },
                    onSetPaletteOpen = { isOpen -> viewModel.setPaletteOpen(isOpen) },
                    onSetSubmitConfirmOpen = { isOpen -> viewModel.setSubmitConfirmOpen(isOpen) },
                    onSubmitTest = { viewModel.submitMockTest() },
                    onExitTest = { viewModel.saveAndExitActiveTest() },
                    onRetakeTest = {
                        activeTestState.completedAttempt?.let { viewModel.retakeMockTest(it) }
                    },
                    onRetakeWrongQuestions = { viewModel.retryWrongQuestions() },
                    onRetryUnanswered = { viewModel.retrySkippedQuestions() },
                    onStartPractice = { rec -> viewModel.startTargetedPractice(rec) },
                    onSaveAndNext = { viewModel.saveAndNext() },
                    onMarkForReviewAndNext = { viewModel.markForReviewAndNext() },
                    onPreviousQuestion = { viewModel.previousQuestion() }
                )
            } else if (showLiveExamIntelligenceScreen) {
                LiveExamIntelligenceScreen(
                    feedState = liveExamFeedState,
                    isRefreshing = isRefreshingLiveExam,
                    onRefresh = { viewModel.refreshLiveExamIntelligence(force = true) },
                    onToggleSaveUpdate = { id, s -> viewModel.toggleSaveLiveExamUpdate(id, s) },
                    onToggleSaveTrending = { id, s -> viewModel.toggleSaveTrendingTopic(id, s) },
                    onStartQuizForTopic = { category, topic ->
                        viewModel.startInteractiveStudyQuiz(category, topic)
                        viewModel.setShowLiveExamIntelligenceScreen(false)
                        onSelectTab(AppNavTab.AI_TUTOR)
                    },
                    onAskNovaAboutUpdate = { prompt ->
                        novaViewModel.sendMessage(prompt)
                        viewModel.setShowLiveExamIntelligenceScreen(false)
                        onSelectTab(AppNavTab.AI_TUTOR)
                    },
                    onBack = { viewModel.setShowLiveExamIntelligenceScreen(false) }
                )
            } else if (showSmartVacancyScreen) {
                SmartVacancyScreen(
                    feedState = recruitmentFeedState,
                    isRefreshing = isRefreshingRecruitment,
                    onRefresh = { viewModel.refreshRecruitmentCatalog(force = true) },
                    onCategorySelected = { viewModel.setRecruitmentCategory(it) },
                    onStateSelected = { viewModel.setRecruitmentState(it) },
                    onTabSelected = { viewModel.setRecruitmentTab(it) },
                    onSearchQueryChanged = { viewModel.setRecruitmentSearch(it) },
                    onSortOptionSelected = { viewModel.setRecruitmentSort(it) },
                    onToggleSave = { id, saved -> viewModel.toggleSaveRecruitment(id, saved) },
                    onSetReminder = { id, enabled, days -> viewModel.setDeadlineReminder(id, enabled, days) },
                    selectedDetailItem = selectedRecruitmentDetail,
                    onSelectDetailItem = { viewModel.selectRecruitmentDetail(it) },
                    onUpdateProfile = { viewModel.updateRecruitmentProfile(it) },
                    onUpdateApplicationStatus = { id, status, appNo, rollNo, post, notes ->
                        viewModel.updateUserApplicationStatus(id, status, appNo, rollNo, post, notes)
                    },
                    onUpdateDocumentsReady = { id, docs -> viewModel.updateDocumentReadyStatus(id, docs) },
                    onUpdateChecklistChecked = { id, list -> viewModel.updateChecklistChecked(id, list) },
                    onFindJobsForMe = { category, state, qual, age ->
                        viewModel.findJobsForMe(category, state, qual, age)
                    },
                    onSubmitReport = { id, category, comment ->
                        Toast.makeText(context, "Thank you. Your report has been recorded.", Toast.LENGTH_SHORT).show()
                    },
                    notificationSettings = recruitmentNotificationSettings,
                    onUpdateNotificationSettings = { viewModel.updateRecruitmentNotificationSettings(it) },
                    outboxItems = recruitmentOutbox,
                    dailyDigest = recruitmentDailyDigest,
                    diagnostics = recruitmentDiagnostics,
                    onMuteRecruitment = { viewModel.muteRecruitment(it) },
                    onUnmuteRecruitment = { viewModel.unmuteRecruitment(it) },
                    onMuteCategory = { viewModel.muteRecruitmentCategory(it) },
                    onUnmuteCategory = { viewModel.unmuteRecruitmentCategory(it) },
                    onMarkOutboxRead = { viewModel.markRecruitmentOutboxItemRead(it) },
                    onMarkAllOutboxRead = { viewModel.markAllRecruitmentOutboxItemsRead() },
                    onDeleteOutboxItem = { viewModel.deleteRecruitmentOutboxItem(it) },
                    onClearAllOutbox = { viewModel.clearAllRecruitmentOutbox() },
                    onNovaQuery = { query ->
                        viewModel.handleNovaRecruitmentQuery(query) { action ->
                            when (action) {
                                NovaRecruitmentActionType.OPEN_VACANCIES -> {
                                    viewModel.setShowSmartVacancyScreen(true, initialTab = "VACANCY")
                                }
                                NovaRecruitmentActionType.OPEN_RESULTS -> {
                                    viewModel.setShowSmartVacancyScreen(true, initialTab = "RESULT")
                                }
                                NovaRecruitmentActionType.OPEN_ADMIT_CARDS -> {
                                    viewModel.setShowSmartVacancyScreen(true, initialTab = "ADMIT_CARD")
                                }
                                NovaRecruitmentActionType.OPEN_SAVED_RECRUITMENTS -> {
                                    viewModel.setShowSmartVacancyScreen(true, initialTab = "SAVED")
                                }
                                NovaRecruitmentActionType.OPEN_RECRUITMENT -> {
                                    viewModel.setShowSmartVacancyScreen(true, initialTab = "VACANCY")
                                }
                                NovaRecruitmentActionType.ENABLE_DEADLINE_ALERTS -> {
                                    viewModel.updateRecruitmentNotificationSettings(
                                        recruitmentNotificationSettings.copy(deadlineAlertsEnabled = true)
                                    )
                                    Toast.makeText(context, "Deadline closing alerts enabled", Toast.LENGTH_SHORT).show()
                                }
                                NovaRecruitmentActionType.OPEN_NOTIFICATION_SETTINGS -> {
                                    viewModel.setShowSmartVacancyScreen(true)
                                }
                            }
                        }
                    },
                    onBack = { viewModel.setShowSmartVacancyScreen(false) }
                )
            } else if (showNotificationCenter) {
                NotificationCenterScreen(
                    notifications = appNotifications,
                    onMarkAsRead = { viewModel.markNotificationAsRead(it) },
                    onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
                    onDeleteNotification = { viewModel.deleteNotification(it) },
                    onClearAll = { viewModel.clearAllNotifications() },
                    onNavigateDeepLink = handleDeepLink,
                    onOpenSettings = {
                        showNotificationCenter = false
                        showProfileSettings = true
                    },
                    onBack = { showNotificationCenter = false }
                )
            } else if (showDailyBriefingScreen) {
                DailyBriefingScreen(
                    briefingData = dailyBriefingData,
                    onStartPractice = { sub, top ->
                        viewModel.startFocusSession(sub, top, 25)
                        showDailyBriefingScreen = false
                        onSelectTab(AppNavTab.FOCUS)
                    },
                    onReadCurrentAffairs = {
                        showDailyBriefingScreen = false
                        viewModel.setShowLiveExamIntelligenceScreen(true)
                    },
                    onResumeTest = {
                        showDailyBriefingScreen = false
                        if (pendingResumeSession != null) {
                            viewModel.resumePendingTestSession()
                        } else {
                            onSelectTab(AppNavTab.PROGRESS)
                        }
                    },
                    onStartRevision = {
                        showDailyBriefingScreen = false
                        onSelectTab(AppNavTab.PROGRESS)
                    },
                    onAskNova = { prompt ->
                        showDailyBriefingScreen = false
                        novaViewModel.sendMessage(prompt)
                        onSelectTab(AppNavTab.AI_TUTOR)
                    },
                    onChangeLanguage = { lang ->
                        viewModel.updateNotificationPrefs(notifPrefs.copy(language = lang))
                        viewModel.computeDailyBriefingData()
                    },
                    onBack = { showDailyBriefingScreen = false }
                )
            } else if (showProfileSettings) {
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
                    onSetTutorPersona = { viewModel.setTutorPersona(it) },
                    onChangePassword = { newPass, cb -> viewModel.changePassword(newPass, cb) },
                    onRequestEmailChange = { newEmail, cb -> viewModel.requestEmailChange(newEmail, cb) },
                    onTriggerSync = { cb -> viewModel.triggerManualSync(cb) },
                    onExportData = { viewModel.exportUserDataJson() },
                    onResetPersonalization = { viewModel.resetPersonalization {} },
                    onClearCache = { viewModel.clearAllLocalStudyData {} }
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
                            onUpdateUserProfile = { updatedProfile -> viewModel.updateUserProfile(updatedProfile) },
                            novaViewModel = novaViewModel,
                            liveExamFeedState = liveExamFeedState,
                            isRefreshingLiveExam = isRefreshingLiveExam,
                            onRefreshLiveExam = { viewModel.refreshLiveExamIntelligence(force = true) },
                            onOpenLiveExamUpdateDetail = { update ->
                                viewModel.selectLiveUpdateForDetail(update)
                                viewModel.setShowLiveExamIntelligenceScreen(true)
                            },
                            onToggleSaveLiveExamUpdate = { id, s -> viewModel.toggleSaveLiveExamUpdate(id, s) },
                            onToggleSaveTrendingTopic = { id, s -> viewModel.toggleSaveTrendingTopic(id, s) },
                            onStartQuizForTopic = { category, topic ->
                                viewModel.startInteractiveStudyQuiz(category, topic)
                                onSelectTab(AppNavTab.AI_TUTOR)
                            },
                            onOpenFullLiveExamIntelligence = {
                                viewModel.setShowLiveExamIntelligenceScreen(true)
                            },
                            recruitmentFeedState = recruitmentFeedState,
                            onOpenSmartVacancy = { tab ->
                                viewModel.setShowSmartVacancyScreen(true, initialTab = tab)
                            },
                            pendingResumeSession = pendingResumeSession,
                            onResumePendingTest = { viewModel.resumePendingTestSession() },
                            onDiscardPendingTest = { viewModel.discardPendingTestSession() },
                            unreadNotificationCount = appNotifications.count { !it.isRead },
                            onOpenNotificationCenter = { showNotificationCenter = true },
                            dailyBriefingData = dailyBriefingData,
                            onOpenDailyBriefing = { showDailyBriefingScreen = true },
                            userStudyPreferences = userStudyPreferences,
                            allTopicMasteries = allTopicMasteries,
                            mockAttempts = mockAttempts,
                            mistakes = mistakes,
                            focusSessions = allFocusSessions,
                            onStartPracticeWithConfig = { config ->
                                viewModel.startMockTestWithConfig(config)
                                onSelectTab(AppNavTab.STUDY)
                            },
                            focusTimerState = focusState,
                            onPauseFocusSession = { viewModel.toggleFocusPause() },
                            onResumeFocusSession = { viewModel.toggleFocusPause() },
                            onStopFocusSession = { viewModel.endFocusSession() }
                        )

                        AppNavTab.AI_TUTOR -> {
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

                        AppNavTab.STUDY -> StudyHubScreen(
                            user = userProfile,
                            examContext = activeExamContext ?: ExamContext(),
                            subjectSummaries = subjectProgressSummaries,
                            allMasteries = allTopicMasteries,
                            bookmarks = allLearningBookmarks,
                            smartNotes = smartNotes,
                            studyPlan = studyPlan,
                            flashcards = flashcards,
                            isPlanGenerating = isPlanGenerating,
                            activeLearningContent = activeLearningContent,
                            isLearningContentLoading = isLearningContentLoading,
                            novaDoubtResponse = novaDoubtResponse,
                            isNovaDoubtThinking = isNovaDoubtThinking,
                            userPreferences = userStudyPreferences,
                            activeStudySession = activeStudySession,
                            sessionRemainingSeconds = sessionRemainingSeconds,
                            isSessionTimerRunning = isSessionTimerRunning,
                            isSessionPaused = isSessionPaused,
                            activeSessionActualMinutes = activeSessionActualMinutes,
                            isFlashcardGenerating = isFlashcardGenerating,
                            flashcardMessage = flashcardMessage,
                            allFocusSessions = allFocusSessions,
                            mistakes = mistakes,
                            onSelectTopicLearning = { sub, chap, top -> viewModel.loadLearningTopicContent(sub, chap, top) },
                            onGenerateAiPlan = { viewModel.generateAdaptiveDailyPlan() },
                            onTogglePlanItem = { id, done -> viewModel.togglePlanItem(id, done) },
                            onAddPlanItem = { sub, chap, top, mins, prio -> viewModel.addManualPlanItem(sub, chap, top, mins, prio) },
                            onUpdatePlanItem = { item -> viewModel.updatePlanItem(item) },
                            onDeletePlanItem = { viewModel.deletePlanItem(it) },
                            onStartFocusSession = { sub, top ->
                                viewModel.startFocusSession(sub, top, 25)
                                onSelectTab(AppNavTab.FOCUS)
                            },
                            onStartSessionTimer = { item -> viewModel.startStudySession(item) },
                            onPauseSessionTimer = { viewModel.pauseStudySession() },
                            onResumeSessionTimer = { viewModel.resumeStudySession() },
                            onFinishSessionTimer = { notes -> viewModel.finishStudySession(notes) },
                            onCancelSessionTimer = { viewModel.cancelStudySession() },
                            onAddFlashcard = { subject, topic, front, back, hint, diff, src ->
                                viewModel.addFlashcard(subject, topic, front, back, hint, diff, src)
                            },
                            onUpdateFlashcard = { viewModel.updateFlashcard(it) },
                            onDeleteFlashcard = { viewModel.deleteFlashcard(it) },
                            onReviewFlashcard = { id, status, conf -> viewModel.reviewFlashcard(id, status, conf) },
                            onReviewSpaced = { id, quality -> viewModel.recordSpacedFlashcardReview(id, quality) },
                            onGenerateAiCards = { sub, top -> viewModel.generateAiFlashcards(sub, top) },
                            onGenerateFromNotes = { title, nText, sub, c -> viewModel.generateFlashcardsFromNotes(title, nText, sub, c) },
                            onGenerateFromDocumentUri = { uri, sub, c -> viewModel.generateFlashcardsFromDocumentUri(uri, sub, c) },
                            onClearFlashcardMessage = { viewModel.clearFlashcardMessage() },
                            onSaveTopicNote = { sub, top, noteText -> viewModel.saveTopicNote(sub, top, noteText) },
                            onToggleLearningBookmark = { sub, top, title, snippet, type -> viewModel.toggleLearningBookmark(sub, top, title, snippet, type) },
                            onCompleteQuickTest = { sub, top, score, total -> viewModel.completeQuickTest(sub, top, score, total) },
                            onAskNovaDoubt = { sub, chap, top, prompt -> viewModel.askNovaTopicDoubt(sub, chap, top, prompt) },
                            onSpeakTts = { text -> viewModel.speakText(text) },
                            onDeleteBookmark = { viewModel.deleteLearningBookmark(it) },
                            onDeleteNote = { viewModel.deleteTopicNote(it) },
                            onOpenDocumentSummarizer = { showDocumentSummarizer = true },
                            onOpenSearch = { query ->
                                viewModel.performSmartSearch(query)
                                onSelectTab(AppNavTab.AI_TUTOR)
                            },
                            onAskNovaGlobal = { prompt ->
                                viewModel.performSmartSearch(prompt)
                                onSelectTab(AppNavTab.AI_TUTOR)
                            },
                            onChangeExam = { onSelectTab(AppNavTab.PROFILE) },
                            onRecoverMissedSessions = { mode -> viewModel.recoverMissedSessions(mode) },
                            onUpdateDailyAvailableTime = { mins -> viewModel.generateAdaptiveDailyPlan(overrideAvailableMinutes = mins) },
                            onSavePreferences = { prefs -> viewModel.saveUserPreferences(prefs) },
                            onApplySubjectAllocations = { subMinutes, totalMins, startHr, breakMins ->
                                viewModel.applySubjectTimeAllocations(subMinutes, totalMins, startHr, 0, breakMins)
                            }
                        )

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

                        AppNavTab.PRACTICE, AppNavTab.PROGRESS -> PracticeHubScreen(
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
                            onRetryUnanswered = { viewModel.retrySkippedQuestions() },
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
                            pendingResumeSession = pendingResumeSession,
                            onResumePendingTest = { viewModel.resumePendingTestSession() },
                            onDiscardPendingTest = { viewModel.discardPendingTestSession() },
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
                            userPreferences = userStudyPreferences,
                            onUpdatePersonalizationSettings = { settings -> viewModel.updatePersonalizationSettings(settings) },
                            onResetPersonalizationSignals = { viewModel.resetPersonalizationSignals() },
                            onRecordSpacedRevisionFeedback = { sub, top, fb -> viewModel.recordSpacedRevisionFeedback(sub, top, fb) },
                            onBack = { onSelectTab(AppNavTab.HOME) },
                            onNavigateToStudy = { onSelectTab(AppNavTab.STUDY) },
                            activeExamContext = activeExamContext,
                            novaProgressAnalysis = novaProgressAnalysis,
                            isNovaProgressAnalyzing = isNovaProgressAnalyzing,
                            onGenerateNovaProgressAnalysis = { exam, summary, lang ->
                                viewModel.generateNovaProgressAnalysis(exam, summary, lang)
                            }
                        )

                        AppNavTab.UPDATES -> UpdatesHubScreen(
                            recruitmentFeedState = recruitmentFeedState,
                            isRefreshingRecruitment = isRefreshingRecruitment,
                            onRefreshRecruitment = { viewModel.refreshRecruitmentCatalog(force = true) },
                            onCategorySelected = { viewModel.setRecruitmentCategory(it) },
                            onStateSelected = { viewModel.setRecruitmentState(it) },
                            onTabSelected = { viewModel.setRecruitmentTab(it) },
                            onSearchQueryChanged = { viewModel.setRecruitmentSearch(it) },
                            onSortOptionSelected = { viewModel.setRecruitmentSort(it) },
                            onToggleSaveRecruitment = { id, saved -> viewModel.toggleSaveRecruitment(id, saved) },
                            onSetReminder = { id, set, hours -> viewModel.setDeadlineReminder(id, set, hours) },
                            selectedDetailItem = selectedRecruitmentDetail,
                            onSelectDetailItem = { viewModel.selectRecruitmentDetail(it) },
                            onUpdateProfile = { viewModel.updateRecruitmentProfile(it) },
                            onUpdateApplicationStatus = { id, st, appNo, rollNo, post, notes ->
                                viewModel.updateUserApplicationStatus(id, st, appNo, rollNo, post, notes)
                            },
                            onUpdateDocumentsReady = { id, docs -> viewModel.updateDocumentReadyStatus(id, docs) },
                            onUpdateChecklistChecked = { id, items -> viewModel.updateChecklistChecked(id, items) },
                            onFindJobsForMe = { qual, cat, st, age -> viewModel.findJobsForMe(qual ?: "", cat ?: "", st ?: "", age ?: 0) },
                            recruitmentNotificationSettings = recruitmentNotificationSettings,
                            onUpdateNotificationSettings = { viewModel.updateRecruitmentNotificationSettings(it) },
                            recruitmentOutbox = recruitmentOutbox,
                            recruitmentDailyDigest = recruitmentDailyDigest,
                            recruitmentDiagnostics = recruitmentDiagnostics,
                            onMuteRecruitment = { viewModel.muteRecruitment(it) },
                            onUnmuteRecruitment = { viewModel.unmuteRecruitment(it) },
                            onMuteCategory = { viewModel.muteRecruitmentCategory(it) },
                            onUnmuteCategory = { viewModel.unmuteRecruitmentCategory(it) },
                            onMarkOutboxRead = { viewModel.markRecruitmentOutboxItemRead(it) },
                            onMarkAllOutboxRead = { viewModel.markAllRecruitmentOutboxItemsRead() },
                            onDeleteOutboxItem = { viewModel.deleteRecruitmentOutboxItem(it) },
                            onClearAllOutbox = { viewModel.clearAllRecruitmentOutbox() },
                            onNovaQuery = { q ->
                                viewModel.performSmartSearch(q)
                                onSelectTab(AppNavTab.AI_TUTOR)
                            },
                            liveExamFeedState = liveExamFeedState,
                            isRefreshingLiveExam = isRefreshingLiveExam,
                            onRefreshLiveExam = { viewModel.refreshLiveExamIntelligence(force = true) },
                            onToggleSaveLiveUpdate = { id, s -> viewModel.toggleSaveLiveExamUpdate(id, s) },
                            onToggleSaveTrending = { id, s -> viewModel.toggleSaveTrendingTopic(id, s) },
                            onStartQuizForTopic = { cat, top ->
                                viewModel.startInteractiveStudyQuiz(cat, top)
                                onSelectTab(AppNavTab.AI_TUTOR)
                            },
                            onAskNovaAboutUpdate = { q ->
                                viewModel.performSmartSearch(q)
                                onSelectTab(AppNavTab.AI_TUTOR)
                            },
                            notifications = appNotifications,
                            onMarkNotificationAsRead = { viewModel.markNotificationAsRead(it) },
                            onMarkAllNotificationsAsRead = { viewModel.markAllNotificationsAsRead() },
                            onDeleteNotification = { viewModel.deleteNotification(it) },
                            onClearAllNotifications = { viewModel.clearAllNotifications() },
                            onNavigateDeepLink = { link, payload -> handleDeepLink(link, payload ?: "") },
                            onOpenNotificationSettings = { onSelectTab(AppNavTab.PROFILE) }
                        )

                        AppNavTab.PROFILE -> ProfileSettingsScreen(
                            user = userProfile,
                            themeMode = themeMode,
                            isDarkTheme = isDarkTheme,
                            notificationPrefs = notifPrefs,
                            onSetThemeMode = { viewModel.updateThemeMode(it) },
                            onToggleDarkTheme = { viewModel.updateTheme(it) },
                            onUpdateNotificationPrefs = { viewModel.updateNotificationPrefs(it) },
                            onUpdateProfile = { updatedProfile -> viewModel.updateUserProfile(updatedProfile, refreshStudyPlan = true) },
                            onSignOut = { viewModel.signOut(context) },
                            onDeleteAccount = { viewModel.deleteAccount() },
                            onBack = { onSelectTab(AppNavTab.HOME) },
                            onTestStudyReminder = { viewModel.testStudySessionReminder() },
                            onTestExamCountdown = { viewModel.testExamCountdownReminder() },
                            onTestDailyGoal = { viewModel.testDailyGoalReminder() },
                            onTestMissedStudy = { viewModel.testMissedStudyReminder() },
                            onTestBreakReminder = { viewModel.testBreakReminder() },
                            onTestFocusStarted = { viewModel.testFocusStartedNotification() },
                            onTestFocusCompleted = { viewModel.testFocusCompletedNotification() },
                            onTestDailyMotivation = { viewModel.testDailyMotivationalNotification() },
                            catalogExams = allCatalogExams,
                            onChangeExam = { examId -> viewModel.changeSelectedExam(examId = examId, refreshStudyPlan = true) },
                            onOpenStudyPlanner = { onSelectTab(AppNavTab.STUDY) },
                            onResetActiveExamData = { viewModel.resetActiveExamPreparationData() },
                            isAiThinkingMode = useThinkingMode,
                            onSetAiThinkingMode = { thinking -> viewModel.setThinkingMode(thinking) },
                            tutorPersona = tutorPersona,
                            onSetTutorPersona = { persona -> viewModel.setTutorPersona(persona) },
                            onChangePassword = { oldPass, callback -> viewModel.changePassword(oldPass, callback) },
                            onRequestEmailChange = { newEmail, callback -> viewModel.requestEmailChange(newEmail, callback) },
                            onTriggerSync = { callback -> viewModel.triggerManualSync(callback) },
                            onExportData = { viewModel.exportUserDataJson() },
                            onResetPersonalization = { viewModel.resetPersonalization {} },
                            onClearCache = { viewModel.clearAllLocalStudyData {} }
                        )
                    }
                }
            }

            // Floating In-Context Assistant on secondary screens (except Home, AI Tutor, and active Mock Test)
            if (currentTab != AppNavTab.HOME && currentTab != AppNavTab.AI_TUTOR && !activeTestState.isTestInProgress && !showProfileSettings && !showDocumentSummarizer && !showExamReadinessCenter) {
                NovaFloatingAssistant(
                    viewModel = novaViewModel,
                    onNavigateToTab = { onSelectTab(it) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 96.dp)
                )
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

            // Step 30 In-App Notification Floating Banner Overlay
            InAppNotificationBanner(
                notification = activeInAppBanner,
                onDismiss = { viewModel.dismissInAppBanner() },
                onClick = {
                    activeInAppBanner?.let { handleDeepLink(it.deepLink, it.payload) }
                    viewModel.dismissInAppBanner()
                }
            )
        }
    }
}

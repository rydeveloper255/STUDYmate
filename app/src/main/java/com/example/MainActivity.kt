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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppNavTab
import com.example.ui.components.FloatingGlassNavBar
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.OnboardingScreen
import com.example.ui.screens.auth.PermissionSetupScreen
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.document.DocumentSummarizerScreen
import com.example.ui.screens.focus.FocusModeScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.nova.NovaScreen
import com.example.ui.screens.planner.StudyPlannerScreen
import com.example.ui.screens.profile.ProfileSettingsDialog
import com.example.ui.screens.progress.ProgressDashboardScreen
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
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            StudyMateTheme(
                darkTheme = isDarkTheme,
                onToggleTheme = { viewModel.updateTheme(!isDarkTheme) },
                onSetTheme = { viewModel.updateTheme(it) }
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
    var showProfileDialog by remember { mutableStateOf(false) }

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
    val activeTestState by viewModel.activeTestState.collectAsStateWithLifecycle()
    val isTestGenerating by viewModel.isTestGenerating.collectAsStateWithLifecycle()
    val mistakeDiagnosis by viewModel.mistakeDiagnosis.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val notifPrefs by viewModel.notificationPrefs.collectAsStateWithLifecycle()

    val documentAnalysis by viewModel.documentAnalysisState.collectAsStateWithLifecycle()
    val isDocumentParsing by viewModel.isDocumentParsing.collectAsStateWithLifecycle()
    val documentError by viewModel.documentError.collectAsStateWithLifecycle()
    var showDocumentSummarizer by remember { mutableStateOf(false) }

    val aiCoachRecommendation by viewModel.aiCoachRecommendation.collectAsStateWithLifecycle()
    val studyNowRecommendation by viewModel.studyNowRecommendation.collectAsStateWithLifecycle()
    val isAiCoachLoading by viewModel.isAiCoachLoading.collectAsStateWithLifecycle()
    val isStudyNowLoading by viewModel.isStudyNowLoading.collectAsStateWithLifecycle()
    val completeStudyKit by viewModel.completeStudyKit.collectAsStateWithLifecycle()
    val isStudyKitGenerating by viewModel.isStudyKitGenerating.collectAsStateWithLifecycle()

    val activity = context as? Activity
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        when {
            showProfileDialog -> {
                showProfileDialog = false
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
            // Hide bottom bar during active fullscreen mock test or document summarizer
            if (!activeTestState.isTestInProgress && !activeTestState.isCompleted && !showDocumentSummarizer) {
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
                .background(appBackgroundGradient(isDarkTheme))
                .padding(innerPadding)
        ) {
            if (showDocumentSummarizer) {
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
                            aiCoachRecommendation = aiCoachRecommendation,
                            studyNowRecommendation = studyNowRecommendation,
                            isAiCoachLoading = isAiCoachLoading,
                            isStudyNowLoading = isStudyNowLoading,
                            onLoadAiCoach = { sub -> viewModel.loadAiCoachRecommendation(sub) },
                            onLoadStudyNow = { viewModel.loadStudyNowRecommendation() },
                            onTogglePlanItem = { id, done -> viewModel.togglePlanItem(id, done) },
                            onStartFocusSession = { sub, top ->
                                viewModel.startFocusSession(sub, top, 25)
                                onSelectTab(AppNavTab.FOCUS)
                            },
                            onNavigateToTab = { onSelectTab(it) },
                            onOpenProfileSettings = { showProfileDialog = true },
                            onSignOut = { viewModel.signOut(context) },
                            onScanQuestion = { onSelectTab(AppNavTab.AI_TUTOR) },
                            onOpenDocumentSummarizer = { showDocumentSummarizer = true }
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

                        AppNavTab.STUDY -> StudyPlannerScreen(
                            planItems = studyPlan,
                            flashcards = flashcards,
                            user = userProfile,
                            isGenerating = isPlanGenerating,
                            onGenerateAiPlan = { viewModel.generateAiStudyPlan() },
                            onTogglePlanItem = { id, done -> viewModel.togglePlanItem(id, done) },
                            onAddPlanItem = { sub, chap, top, mins, prio -> viewModel.addManualPlanItem(sub, chap, top, mins, prio) },
                            onDeletePlanItem = { viewModel.deletePlanItem(it) },
                            onStartFocusSession = { sub, top ->
                                viewModel.startFocusSession(sub, top, 25)
                                onSelectTab(AppNavTab.FOCUS)
                            },
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

                        AppNavTab.FOCUS -> FocusModeScreen(
                            focusState = focusState,
                            onStartFocus = { sub, top, mins -> viewModel.startFocusSession(sub, top, mins) },
                            onTogglePause = { viewModel.toggleFocusPause() },
                            onEndSession = { viewModel.endFocusSession() },
                            onDismissCelebration = { viewModel.dismissCelebration() }
                        )

                        AppNavTab.PROGRESS -> ProgressDashboardScreen(
                            user = userProfile,
                            attempts = mockAttempts,
                            mistakes = mistakes,
                            userMaterials = userQuestionMaterials,
                            activeTestState = activeTestState,
                            isTestGenerating = isTestGenerating,
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
                            onDeletePastTest = { id -> viewModel.deletePastTest(id) },
                            onSaveUserMaterial = { title, exam, subject, topic, rawText ->
                                viewModel.saveUserQuestionMaterial(title, exam, subject, topic, rawText)
                            },
                            onDeleteUserMaterial = { id -> viewModel.deleteUserQuestionMaterial(id) },
                            onDiagnoseMistakes = { subject -> viewModel.diagnoseMistakes(subject) },
                            onMarkMistakeMastered = { id, mastered -> viewModel.markMistakeMastered(id, mastered) }
                        )
                    }
                }
            }

            // Profile & Settings Dialog
            if (showProfileDialog) {
                ProfileSettingsDialog(
                    user = userProfile,
                    isDarkTheme = isDarkTheme,
                    notificationPrefs = notifPrefs,
                    onToggleDarkTheme = { viewModel.updateTheme(it) },
                    onUpdateNotificationPrefs = { viewModel.updateNotificationPrefs(it) },
                    onUpdateProfile = { viewModel.updateUserProfile(it) },
                    onSignOut = { viewModel.signOut(context) },
                    onDeleteAccount = { viewModel.deleteAccount() },
                    onDismiss = { showProfileDialog = false },
                    onTestStudyReminder = { viewModel.testStudySessionReminder() },
                    onTestExamCountdown = { viewModel.testExamCountdownReminder() },
                    onTestDailyGoal = { viewModel.testDailyGoalReminder() },
                    onTestMissedStudy = { viewModel.testMissedStudyReminder() },
                    onTestBreakReminder = { viewModel.testBreakReminder() },
                    onTestFocusStarted = { viewModel.testFocusStartedNotification() },
                    onTestFocusCompleted = { viewModel.testFocusCompletedNotification() },
                    onTestDailyMotivation = { viewModel.testDailyMotivationalNotification() }
                )
            }
        }
    }
}

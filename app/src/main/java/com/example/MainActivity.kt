package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.document.DocumentSummarizerScreen
import com.example.ui.screens.focus.FocusModeScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.planner.StudyPlannerScreen
import com.example.ui.screens.profile.ProfileSettingsDialog
import com.example.ui.screens.progress.ProgressDashboardScreen
import com.example.ui.screens.tutor.GeminiTutorScreen
import com.example.ui.theme.StudyMateTheme
import com.example.ui.theme.appBackgroundGradient
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory

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
    var currentTab by remember { mutableStateOf(AppNavTab.HOME) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Dynamic Permission Requests
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
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
            onGuestSignIn = { viewModel.continueAsGuest() }
        )
        return
    }

    // First-Time User -> Onboarding Screen
    if (!userProfile!!.isOnboardingCompleted) {
        OnboardingScreen(
            initialName = userProfile?.name ?: "",
            onComplete = { name, grade, subjects, goal, examName, dailyMins, preferredTime, notifs ->
                viewModel.saveOnboarding(name, grade, subjects, goal, examName, dailyMins, preferredTime, notifs)
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Hide bottom bar during active fullscreen mock test or document summarizer
            if (!activeTestState.isTestInProgress && !activeTestState.isCompleted && !showDocumentSummarizer) {
                FloatingGlassNavBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
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
                            aiCoachRecommendation = aiCoachRecommendation,
                            studyNowRecommendation = studyNowRecommendation,
                            isAiCoachLoading = isAiCoachLoading,
                            isStudyNowLoading = isStudyNowLoading,
                            onLoadAiCoach = { sub -> viewModel.loadAiCoachRecommendation(sub) },
                            onLoadStudyNow = { viewModel.loadStudyNowRecommendation() },
                            onTogglePlanItem = { id, done -> viewModel.togglePlanItem(id, done) },
                            onStartFocusSession = { sub, top ->
                                viewModel.startFocusSession(sub, top, 25)
                                currentTab = AppNavTab.FOCUS
                            },
                            onNavigateToTab = { currentTab = it },
                            onOpenProfileSettings = { showProfileDialog = true },
                            onScanQuestion = { currentTab = AppNavTab.AI_TUTOR },
                            onOpenDocumentSummarizer = { showDocumentSummarizer = true }
                        )

                        AppNavTab.AI_TUTOR -> GeminiTutorScreen(
                            messages = chatMessages,
                            isAiThinking = isAiThinking,
                            useThinkingMode = useThinkingMode,
                            tutorPersona = tutorPersona,
                            isTtsSpeaking = isTtsSpeaking,
                            onSendMessage = { viewModel.sendTutorMessage(it) },
                            onSendQuickAction = { viewModel.sendQuickActionChip(it) },
                            onSolveImage = { viewModel.solveImageQuestion(it) },
                            onToggleThinkingMode = { viewModel.setThinkingMode(it) },
                            onSelectPersona = { viewModel.setTutorPersona(it) },
                            onSpeakTts = { viewModel.speakTts(it) },
                            onOpenDocumentSummarizer = { showDocumentSummarizer = true }
                        )

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
                                currentTab = AppNavTab.FOCUS
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
                            activeTestState = activeTestState,
                            isTestGenerating = isTestGenerating,
                            mistakeDiagnosis = mistakeDiagnosis,
                            onStartTest = { sub, chap, mode -> viewModel.startMockTest(sub, chap, mode) },
                            onSelectAnswer = { qIdx, optIdx -> viewModel.selectTestAnswer(qIdx, optIdx) },
                            onToggleMarkForReview = { viewModel.toggleMarkForReview(it) },
                            onNavigateQuestion = { viewModel.navigateTestQuestion(it) },
                            onSubmitTest = { viewModel.submitMockTest() },
                            onExitTest = { viewModel.exitTest() },
                            onDiagnoseMistakes = { viewModel.diagnoseMistakes(it) },
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
                    onSignOut = { viewModel.signOut(context) },
                    onDeleteAccount = { viewModel.deleteAccount() },
                    onDismiss = { showProfileDialog = false }
                )
            }
        }
    }
}

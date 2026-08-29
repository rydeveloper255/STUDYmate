package com.example.ui.screens.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.localization.GlobalLanguageSwitcher
import com.example.service.intelligence.PersonalizationSettings
import com.example.service.intelligence.PracticeMode
import com.example.service.intelligence.SmartPracticeRecommendation
import com.example.ui.components.GlassCard
import com.example.ui.components.StreakBadge
import com.example.ui.components.springClickable
import com.example.ui.screens.progress.ActiveMockTestScreen
import com.example.ui.theme.*
import com.example.viewmodel.ActiveTestState

/**
 * Step 68 — StudyMate Practice Sub-Destinations
 *
 * Dedicated sub-destinations for the 5 separate Practice screens:
 * 1. Subject & Topic Practice
 * 2. Mock Tests
 * 3. Previous Year Questions (PYQ)
 * 4. Daily Speed Quiz
 * 5. Weak Topics & Mistake Bank
 */
sealed class PracticeSubDestination {
    object MainHub : PracticeSubDestination()
    data class SubjectPractice(val subject: String? = null, val chapter: String? = null, val topic: String? = null) : PracticeSubDestination()
    data class MockTests(val initialCategory: String? = null) : PracticeSubDestination()
    data class Pyq(val exam: String? = null, val year: String? = null, val subject: String? = null) : PracticeSubDestination()
    data class DailyQuiz(val quizDate: String? = null) : PracticeSubDestination()
    object WeakTopics : PracticeSubDestination()
}

enum class PracticeSectionTab(val title: String, val icon: ImageVector) {
    MODES("Practice Modes", Icons.Filled.Dashboard),
    MOCKS("Mock Tests", Icons.Filled.Quiz),
    PYQ("PYQ Bank", Icons.Filled.HistoryEdu),
    QUIZ("Speed Quizzes", Icons.Filled.Bolt),
    PERFORMANCE("Analytics & Weak Areas", Icons.Filled.Assessment)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeHubScreen(
    user: UserProfile?,
    attempts: List<MockTestAttempt>,
    mistakes: List<MistakeItem>,
    userMaterials: List<UserQuestionMaterial> = emptyList(),
    examObjective: ExamObjective? = null,
    topicMasteries: List<TopicMastery> = emptyList(),
    sessionHistory: List<StudentSessionHistory> = emptyList(),
    allFocusSessions: List<FocusSession> = emptyList(),
    snapshot: IntelligenceSnapshot? = null,
    activeTestState: ActiveTestState,
    isTestGenerating: Boolean,
    generationError: TestGenerationError? = null,
    insufficientPyqNotice: InsufficientPyqNotice? = null,
    mistakeDiagnosis: String?,
    savedQuestionsList: List<SavedQuestionEntity> = emptyList(),
    onOpenRevisionHub: () -> Unit = {},
    onLaunchPracticeMode: (PracticeMode, String, String, Int) -> Unit = { _, _, _, _ -> },
    onSaveQuestion: (Question) -> Unit = {},
    onUnsaveQuestion: (String) -> Unit = {},
    onReportQuestion: (String, String, String) -> Unit = { _, _, _ -> },
    onStartTestWithConfig: (MockTestConfig) -> Unit,
    onSelectAnswer: (questionIndex: Int, optionIndex: Int) -> Unit,
    onClearAnswer: (questionIndex: Int) -> Unit,
    onToggleMarkForReview: (questionIndex: Int) -> Unit,
    onSkipQuestion: (questionIndex: Int) -> Unit,
    onNavigateQuestion: (index: Int) -> Unit,
    onSetPaletteOpen: (Boolean) -> Unit,
    onSetSubmitConfirmOpen: (Boolean) -> Unit,
    onSubmitTest: () -> Unit,
    onExitTest: () -> Unit,
    onReviewPastTest: (MockTestAttempt) -> Unit,
    onRetakeTest: (MockTestAttempt) -> Unit,
    onRetakeWrongQuestions: (() -> Unit)? = null,
    onRetryUnanswered: (() -> Unit)? = null,
    onStartPractice: ((SmartPracticeRecommendation) -> Unit)? = null,
    onDeletePastTest: (Long) -> Unit,
    onSaveUserMaterial: (title: String, exam: String, subject: String, topic: String, rawText: String) -> Unit,
    onDeleteUserMaterial: (Long) -> Unit,
    onDiagnoseMistakes: (subject: String) -> Unit,
    onMarkMistakeMastered: (Long, Boolean) -> Unit,
    onClearGenerationError: () -> Unit = {},
    onConfirmStartWithAvailablePyqs: () -> Unit = {},
    onConfirmAddAiToPyqs: () -> Unit = {},
    onDismissInsufficientPyqNotice: () -> Unit = {},
    onCancelTestGeneration: () -> Unit = {},
    onSaveAndNext: () -> Unit = {},
    onMarkForReviewAndNext: () -> Unit = {},
    onPreviousQuestion: () -> Unit = {},
    onConfirmOrientation: () -> Unit = {},
    pendingResumeSession: ActiveTestState? = null,
    onResumePendingTest: () -> Unit = {},
    onDiscardPendingTest: () -> Unit = {},
    onSaveExamObjective: (ExamObjective) -> Unit = {},
    onStartFocusOnTopic: (String, String) -> Unit = { _, _ -> },
    examReadiness: ExamReadinessScore? = null,
    subjectSummaries: List<SubjectProgressSummary> = emptyList(),
    recommendations: List<StudyRecommendation> = emptyList(),
    dailyPlan: DailyStudyPlan? = null,
    onSetManualTopicOverride: (String, String, String) -> Unit = { _, _, _ -> },
    onResetPreparationData: () -> Unit = {},
    onOpenReadinessCenter: () -> Unit = {},
    userPreferences: UserStudyPreferences = UserStudyPreferences(),
    onUpdatePersonalizationSettings: (PersonalizationSettings) -> Unit = {},
    onResetPersonalizationSignals: () -> Unit = {},
    onRecordSpacedRevisionFeedback: (String, String, String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit = {},
    onNavigateToStudy: () -> Unit = {},
    activeExamContext: ExamContext? = null,
    novaProgressAnalysis: String? = null,
    isNovaProgressAnalyzing: Boolean = false,
    onGenerateNovaProgressAnalysis: (String, String, String) -> Unit = { _, _, _ -> },
    initialSubModule: String? = null,
    initialSectionTab: PracticeSectionTab = PracticeSectionTab.MODES,
    modifier: Modifier = Modifier
) {
    // If Active Test Execution is in progress, it takes full screen priority
    if (activeTestState.isTestInProgress) {
        ActiveMockTestScreen(
            state = activeTestState,
            onSelectAnswer = onSelectAnswer,
            onClearAnswer = onClearAnswer,
            onToggleMarkForReview = onToggleMarkForReview,
            onSkipQuestion = onSkipQuestion,
            onNavigateQuestion = onNavigateQuestion,
            onSetPaletteOpen = onSetPaletteOpen,
            onSetSubmitConfirmOpen = onSetSubmitConfirmOpen,
            onSubmitTest = onSubmitTest,
            onExitTest = onExitTest,
            onRetakeTest = { },
            onRetakeWrongQuestions = onRetakeWrongQuestions,
            onRetryUnanswered = onRetryUnanswered,
            onStartPractice = onStartPractice,
            onSaveAndNext = onSaveAndNext,
            onMarkForReviewAndNext = onMarkForReviewAndNext,
            onPreviousQuestion = onPreviousQuestion,
            onConfirmOrientation = onConfirmOrientation
        )
        return
    }

    // Determine initial destination
    val initialDest = remember(initialSubModule) {
        when (initialSubModule?.lowercase()) {
            "practice", "subject_practice", "subjectpractice", "topic_practice", "topic", "drill" -> PracticeSubDestination.SubjectPractice()
            "mock_tests", "mocktests", "mocks" -> PracticeSubDestination.MockTests()
            "pyq", "pyqs", "previous_year" -> PracticeSubDestination.Pyq()
            "daily_quiz", "dailyquiz", "quiz" -> PracticeSubDestination.DailyQuiz()
            "weak_topics", "weaktopics", "mistakes" -> PracticeSubDestination.WeakTopics
            else -> PracticeSubDestination.MainHub
        }
    }

    // Hierarchical back stack for Practice module
    val practiceStack = remember {
        mutableStateListOf<PracticeSubDestination>().apply {
            if (initialDest !is PracticeSubDestination.MainHub) {
                add(PracticeSubDestination.MainHub)
            }
            add(initialDest)
        }
    }

    val currentDestination = practiceStack.lastOrNull() ?: PracticeSubDestination.MainHub

    fun navigatePractice(dest: PracticeSubDestination) {
        if (practiceStack.lastOrNull() != dest) {
            practiceStack.add(dest)
        }
    }

    fun popPractice() {
        if (practiceStack.size > 1) {
            practiceStack.removeAt(practiceStack.size - 1)
        } else {
            onBack()
        }
    }

    // BackHandler ensures back-stack pops one level at a time (e.g. Paper -> Year -> Exam -> PYQ -> Practice -> Home)
    BackHandler(enabled = true) {
        popPractice()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "PracticeSubDestinationTransition"
        ) { destination ->
            when (destination) {
                is PracticeSubDestination.MainHub -> {
                    PracticeMainHubView(
                        user = user,
                        attempts = attempts,
                        mistakes = mistakes,
                        onOpenSubDestination = { navigatePractice(it) },
                        onBack = { popPractice() }
                    )
                }
                is PracticeSubDestination.SubjectPractice -> {
                    SubjectTopicPracticeScreen(
                        user = user,
                        initialSubject = destination.subject,
                        initialChapter = destination.chapter,
                        initialTopic = destination.topic,
                        onBack = { popPractice() }
                    )
                }
                is PracticeSubDestination.MockTests -> {
                    MockTestsScreen(
                        user = user,
                        attempts = attempts,
                        initialCategory = destination.initialCategory,
                        onBack = { popPractice() }
                    )
                }
                is PracticeSubDestination.Pyq -> {
                    PyqScreen(
                        user = user,
                        initialExam = destination.exam,
                        initialYear = destination.year,
                        initialSubject = destination.subject,
                        onBack = { popPractice() }
                    )
                }
                is PracticeSubDestination.DailyQuiz -> {
                    DailySpeedQuizScreen(
                        user = user,
                        initialQuizDate = destination.quizDate,
                        onBack = { popPractice() }
                    )
                }
                is PracticeSubDestination.WeakTopics -> {
                    WeakTopicsScreen(
                        user = user,
                        attempts = attempts,
                        mistakes = mistakes,
                        onLaunchPracticeForTopic = { sub, top ->
                            navigatePractice(PracticeSubDestination.SubjectPractice(subject = sub, topic = top))
                        },
                        onBack = { popPractice() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeMainHubView(
    user: UserProfile?,
    attempts: List<MockTestAttempt>,
    mistakes: List<MistakeItem>,
    onOpenSubDestination: (PracticeSubDestination) -> Unit,
    onBack: () -> Unit
) {
    val examName = user?.examName?.ifBlank { "Competitive Exam" } ?: "Competitive Exam"
    val recentScore = attempts.firstOrNull()?.score ?: 0
    val totalAttemptsCount = attempts.size
    val totalMistakesCount = mistakes.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Practice & Test Hub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$examName • 5 Core Practice Modes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_practice_hub_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    StreakBadge(user?.streakDays ?: 1)
                    Spacer(modifier = Modifier.width(8.dp))
                    GlobalLanguageSwitcher()
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Overview Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Ready for Practice Drills",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Select a dedicated practice module below.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "$totalAttemptsCount Tests Taken",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // The 5 Clean Dedicated Cards
            item {
                Text(
                    text = "PRACTICE MODULES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Card 1: Subject & Topic Practice
            item {
                PracticeModuleCard(
                    title = "Subject & Topic Practice",
                    subtitle = "Customized chapter drills, difficulty levels & question counts",
                    tag = "practice_card_subject_practice",
                    icon = Icons.Filled.Quiz,
                    gradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                    badgeText = "Chapter-wise",
                    onClick = { onOpenSubDestination(PracticeSubDestination.SubjectPractice()) }
                )
            }

            // Card 2: Mock Tests
            item {
                PracticeModuleCard(
                    title = "Mock Tests",
                    subtitle = "Full-length exam simulations, timer & rank estimate",
                    tag = "practice_card_mock_tests",
                    icon = Icons.Filled.TaskAlt,
                    gradient = listOf(ElectricViolet, Color(0xFF6366F1)),
                    badgeText = "Full Length & Mini",
                    onClick = { onOpenSubDestination(PracticeSubDestination.MockTests()) }
                )
            }

            // Card 3: Previous Year Questions (PYQ)
            item {
                PracticeModuleCard(
                    title = "Previous Year Questions (PYQ)",
                    subtitle = "Shift-wise authentic exam papers with verified solutions",
                    tag = "practice_card_pyq",
                    icon = Icons.Filled.HistoryEdu,
                    gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                    badgeText = "2019 - 2024",
                    onClick = { onOpenSubDestination(PracticeSubDestination.Pyq()) }
                )
            }

            // Card 4: Daily Speed Quiz
            item {
                PracticeModuleCard(
                    title = "Daily Speed Quiz",
                    subtitle = "10-minute speed drills for today & archive dates",
                    tag = "practice_card_daily_quiz",
                    icon = Icons.Filled.Bolt,
                    gradient = listOf(NeonCyan, Color(0xFF0284C7)),
                    badgeText = "Daily Challenge",
                    onClick = { onOpenSubDestination(PracticeSubDestination.DailyQuiz()) }
                )
            }

            // Card 5: Weak Topics & Mistake Bank
            item {
                PracticeModuleCard(
                    title = "Weak Topics & Mistake Bank",
                    subtitle = "Diagnostic error tracking & targeted remediation drills",
                    tag = "practice_card_weak_topics",
                    icon = Icons.Filled.Spellcheck,
                    gradient = listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
                    badgeText = "$totalMistakesCount Logged Mistakes",
                    onClick = { onOpenSubDestination(PracticeSubDestination.WeakTopics) }
                )
            }
        }
    }
}

@Composable
private fun PracticeModuleCard(
    title: String,
    subtitle: String,
    tag: String,
    icon: ImageVector,
    gradient: List<Color>,
    badgeText: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .springClickable(testTag = tag, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = gradient.first().copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = gradient.first(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

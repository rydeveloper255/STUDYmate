package com.example.ui.screens.practice

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.service.intelligence.PersonalizationSettings
import com.example.service.intelligence.SmartPracticeRecommendation
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StreakBadge
import com.example.ui.components.springClickable
import com.example.ui.screens.progress.ActiveMockTestScreen
import com.example.ui.screens.progress.MockTestResultScreen
import com.example.ui.screens.progress.ProgressDashboardScreen
import com.example.ui.theme.*
import com.example.viewmodel.ActiveTestState
import java.text.SimpleDateFormat
import java.util.*

enum class PracticeSectionTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
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
    initialSectionTab: PracticeSectionTab = PracticeSectionTab.MOCKS,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var selectedSection by rememberSaveable { mutableStateOf(initialSectionTab.name) }
    val currentSection = runCatching { PracticeSectionTab.valueOf(selectedSection) }.getOrDefault(PracticeSectionTab.MOCKS)

    // Active Mock Test Execution View takes full priority (Full Screen Landscape/Portrait Exam UX)
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
            onRetakeTest = { 
                val attempt = activeTestState.completedAttempt ?: attempts.firstOrNull()
                if (attempt != null) onRetakeTest(attempt)
            },
            onRetakeWrongQuestions = onRetakeWrongQuestions,
            onRetryUnanswered = onRetryUnanswered,
            onStartPractice = onStartPractice,
            onSaveAndNext = onSaveAndNext,
            onMarkForReviewAndNext = onMarkForReviewAndNext,
            onPreviousQuestion = onPreviousQuestion
        )
        return
    }

    // Full Progress Dashboard Screen handles the complete suite of tests, analytics, diagnostics & custom builders
    ProgressDashboardScreen(
        user = user,
        attempts = attempts,
        mistakes = mistakes,
        userMaterials = userMaterials,
        examObjective = examObjective,
        topicMasteries = topicMasteries,
        sessionHistory = sessionHistory,
        allFocusSessions = allFocusSessions,
        snapshot = snapshot,
        activeTestState = activeTestState,
        isTestGenerating = isTestGenerating,
        generationError = generationError,
        insufficientPyqNotice = insufficientPyqNotice,
        mistakeDiagnosis = mistakeDiagnosis,
        onStartTestWithConfig = onStartTestWithConfig,
        onSelectAnswer = onSelectAnswer,
        onClearAnswer = onClearAnswer,
        onToggleMarkForReview = onToggleMarkForReview,
        onSkipQuestion = onSkipQuestion,
        onNavigateQuestion = onNavigateQuestion,
        onSetPaletteOpen = onSetPaletteOpen,
        onSetSubmitConfirmOpen = onSetSubmitConfirmOpen,
        onSubmitTest = onSubmitTest,
        onExitTest = onExitTest,
        onReviewPastTest = onReviewPastTest,
        onRetakeTest = onRetakeTest,
        onRetakeWrongQuestions = onRetakeWrongQuestions,
        onRetryUnanswered = onRetryUnanswered,
        onStartPractice = onStartPractice,
        onDeletePastTest = onDeletePastTest,
        onSaveUserMaterial = onSaveUserMaterial,
        onDeleteUserMaterial = onDeleteUserMaterial,
        onDiagnoseMistakes = onDiagnoseMistakes,
        onMarkMistakeMastered = onMarkMistakeMastered,
        onClearGenerationError = onClearGenerationError,
        onConfirmStartWithAvailablePyqs = onConfirmStartWithAvailablePyqs,
        onConfirmAddAiToPyqs = onConfirmAddAiToPyqs,
        onDismissInsufficientPyqNotice = onDismissInsufficientPyqNotice,
        onCancelTestGeneration = onCancelTestGeneration,
        onSaveAndNext = onSaveAndNext,
        onMarkForReviewAndNext = onMarkForReviewAndNext,
        onPreviousQuestion = onPreviousQuestion,
        pendingResumeSession = pendingResumeSession,
        onResumePendingTest = onResumePendingTest,
        onDiscardPendingTest = onDiscardPendingTest,
        onSaveExamObjective = onSaveExamObjective,
        onStartFocusOnTopic = onStartFocusOnTopic,
        examReadiness = examReadiness,
        subjectSummaries = subjectSummaries,
        recommendations = recommendations,
        dailyPlan = dailyPlan,
        onSetManualTopicOverride = onSetManualTopicOverride,
        onResetPreparationData = onResetPreparationData,
        onOpenReadinessCenter = onOpenReadinessCenter,
        userPreferences = userPreferences,
        onUpdatePersonalizationSettings = onUpdatePersonalizationSettings,
        onResetPersonalizationSignals = onResetPersonalizationSignals,
        onRecordSpacedRevisionFeedback = onRecordSpacedRevisionFeedback,
        onBack = onBack,
        onNavigateToStudy = onNavigateToStudy,
        activeExamContext = activeExamContext,
        novaProgressAnalysis = novaProgressAnalysis,
        isNovaProgressAnalyzing = isNovaProgressAnalyzing,
        onGenerateNovaProgressAnalysis = onGenerateNovaProgressAnalysis,
        modifier = modifier
    )
}

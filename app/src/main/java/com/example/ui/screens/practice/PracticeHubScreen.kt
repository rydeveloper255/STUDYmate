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
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.service.intelligence.PersonalizationSettings
import com.example.service.intelligence.PracticeMode
import com.example.service.intelligence.SmartPracticeRecommendation
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StreakBadge
import com.example.ui.components.springClickable
import com.example.ui.screens.progress.ActiveMockTestScreen
import com.example.ui.screens.progress.ProgressDashboardScreen
import com.example.ui.theme.*
import com.example.viewmodel.ActiveTestState

enum class PracticeSectionTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
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
    initialSectionTab: PracticeSectionTab = PracticeSectionTab.MODES,
    modifier: Modifier = Modifier
) {
    var showSavedDialog by remember { mutableStateOf(false) }
    var reportingQuestionId by remember { mutableStateOf<String?>(null) }

    // Active Mock Test Execution View takes full priority
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

    Column(modifier = modifier.fillMaxSize()) {
        // Mode Selector Bar at Top
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = true,
                    onClick = { showSavedDialog = true },
                    label = { Text("🔖 Saved (${savedQuestionsList.size})") },
                    leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("saved_questions_chip")
                )
            }
            item {
                SuggestionChip(
                    onClick = onOpenRevisionHub,
                    label = { Text("🔄 Revision Hub") },
                    icon = { Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.testTag("open_revision_hub_chip")
                )
            }
            items(PracticeMode.values()) { mode ->
                SuggestionChip(
                    onClick = { onLaunchPracticeMode(mode, "", "", mode.defaultQuestions) },
                    label = { Text(mode.displayName) },
                    icon = {
                        val icon = when(mode) {
                            PracticeMode.QUICK_PRACTICE -> Icons.Filled.Bolt
                            PracticeMode.TOPIC_PRACTICE -> Icons.Filled.Topic
                            PracticeMode.SUBJECT_PRACTICE -> Icons.AutoMirrored.Filled.MenuBook
                            PracticeMode.REVISION_PRACTICE -> Icons.Filled.History
                            PracticeMode.MOCK_TEST -> Icons.Filled.Assignment
                            PracticeMode.WEAK_AREA_PRACTICE -> Icons.Filled.Psychology
                            PracticeMode.SAVED_QUESTIONS -> Icons.Filled.Bookmark
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("practice_mode_chip_${mode.name.lowercase()}")
                )
            }
        }

        // Full Progress Dashboard Screen
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
            modifier = Modifier.weight(1f)
        )
    }

    // Saved Questions Dialog
    if (showSavedDialog) {
        AlertDialog(
            onDismissRequest = { showSavedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saved Questions (${savedQuestionsList.size})")
                }
            },
            text = {
                if (savedQuestionsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No saved questions yet. Click bookmark on any question during practice to save it for review!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedQuestionsList) { q ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = q.questionText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${q.subject} • ${q.topic}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = { onUnsaveQuestion(q.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Unsave", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSavedDialog = false
                    if (savedQuestionsList.isNotEmpty()) {
                        onLaunchPracticeMode(PracticeMode.SAVED_QUESTIONS, "", "", 10)
                    }
                }) {
                    Text(if (savedQuestionsList.isNotEmpty()) "Practice Saved Qs" else "Close")
                }
            },
            dismissButton = {
                if (savedQuestionsList.isNotEmpty()) {
                    TextButton(onClick = { showSavedDialog = false }) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }

    // Question Report Modal
    if (reportingQuestionId != null) {
        var reason by remember { mutableStateOf("Wrong Answer Key") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { reportingQuestionId = null },
            title = { Text("Report Question Issue") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Help us keep practice questions accurate and clear.")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Issue Type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Additional Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    reportingQuestionId?.let { qId ->
                        onReportQuestion(qId, reason, notes)
                    }
                    reportingQuestionId = null
                }) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportingQuestionId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.service.intelligence.*
import com.example.ui.components.GlassCard
import com.example.ui.components.StreakBadge
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.ActiveTestState

@Composable
fun ProgressDashboardScreen(
    user: UserProfile?,
    attempts: List<MockTestAttempt>,
    mistakes: List<MistakeItem>,
    userMaterials: List<UserQuestionMaterial> = emptyList(),
    examObjective: ExamObjective? = null,
    topicMasteries: List<TopicMastery> = emptyList(),
    sessionHistory: List<StudentSessionHistory> = emptyList(),
    snapshot: IntelligenceSnapshot? = null,
    activeTestState: ActiveTestState,
    isTestGenerating: Boolean,
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
    onDeletePastTest: (Long) -> Unit,
    onSaveUserMaterial: (title: String, exam: String, subject: String, topic: String, rawText: String) -> Unit,
    onDeleteUserMaterial: (Long) -> Unit,
    onDiagnoseMistakes: (subject: String) -> Unit,
    onMarkMistakeMastered: (Long, Boolean) -> Unit,
    onSaveExamObjective: (ExamObjective) -> Unit = {},
    onStartFocusOnTopic: (subject: String, topic: String) -> Unit = { _, _ -> },
    examReadiness: ExamReadinessScore? = null,
    subjectSummaries: List<SubjectProgressSummary> = emptyList(),
    recommendations: List<StudyRecommendation> = emptyList(),
    dailyPlan: DailyStudyPlan? = null,
    onSetManualTopicOverride: (subject: String, topic: String, override: String) -> Unit = { _, _, _ -> },
    onResetPreparationData: () -> Unit = {},
    onOpenReadinessCenter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: AI Coach & Mocks, 1: Smart Revision Queue, 2: Mastery & Goals, 3: Mistake Book
    var showSetupDialog by remember { mutableStateOf(false) }
    var showMaterialManager by remember { mutableStateOf(false) }
    var showQuestionBankExplorer by remember { mutableStateOf(false) }

    // Calculate real performance report and revision queue
    val performanceReport = remember(user, attempts, mistakes, topicMasteries) {
        PerformanceCoachEngine.computePerformanceReport(
            profile = user ?: UserProfile(),
            mockAttempts = attempts,
            mistakes = mistakes,
            topicMasteries = topicMasteries,
            focusSessions = emptyList(),
            plans = emptyList()
        )
    }

    val revisionQueue = remember(topicMasteries, mistakes, attempts, user) {
        SmartRevisionEngine.buildRevisionQueue(
            topicMasteries = topicMasteries,
            mistakes = mistakes,
            mockAttempts = attempts,
            examDateMillis = user?.examDateMillis ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
        )
    }

    // If test is in progress or completed, show Fullscreen Active Mock Test / Review screen
    if (activeTestState.isTestInProgress || activeTestState.isCompleted) {
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
                activeTestState.completedAttempt?.let { onRetakeTest(it) }
            }
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(horizontal = 18.dp)
            .testTag("progress_dashboard_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Tab Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📊 AI Coach & Intelligence",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Real preparation analysis, spaced revision & mocks",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                StreakBadge(streakDays = user?.streakDays ?: 1)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Switcher Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x251E293B))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "AI Coach",
                    "Revision (${revisionQueue.size})",
                    "Mastery",
                    "Mistakes (${mistakes.size})"
                ).forEachIndexed { idx, label ->
                    val isSelected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .springClickable(testTag = "progress_tab_$idx") {
                                selectedTab = idx
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color(0xFF070B19) else Color(0xFFCBD5E1),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // --- SECTION 0: AI PERFORMANCE COACH & MOCK TESTS ---
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .springClickable(testTag = "progress_exam_readiness_banner", onClick = onOpenReadinessCenter),
                    elevation = 6.dp,
                    fillAlpha = 0.8f
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CenterFocusStrong, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EXAM READINESS CENTER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Readiness Score: ${examReadiness?.readinessScore ?: 0}% • ${examReadiness?.statusBadgeText ?: "Insufficient Data"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = examReadiness?.explanation ?: "View subject heatmaps, syllabus coverage & readiness breakdown",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onOpenReadinessCenter,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Open →", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                PerformanceCoachView(
                    report = performanceReport,
                    onStartNextBestAction = { nba ->
                        onStartFocusOnTopic(nba.subject, nba.topic)
                    },
                    onStartQuickPractice = { subject, topic ->
                        onStartTestWithConfig(
                            MockTestConfig(
                                exam = user?.examName ?: "JEE Main",
                                testType = MockTestType.SUBJECT_TEST,
                                subject = subject,
                                topic = topic,
                                questionCount = 10,
                                timeLimitMinutes = 15
                            )
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                MockTestAnalyticsView(
                    attempts = attempts,
                    user = user,
                    mistakes = mistakes,
                    focusMinutes = user?.totalFocusMinutes ?: 0,
                    onLaunchNewTest = { showSetupDialog = true },
                    onReviewTest = onReviewPastTest,
                    onRetakeTest = onRetakeTest,
                    onDeleteTest = onDeletePastTest,
                    onManageMaterials = { showMaterialManager = true },
                    onOpenQuestionBankExplorer = { showQuestionBankExplorer = true }
                )
            }
        } else if (selectedTab == 1) {
            // --- SECTION 1: SMART REVISION QUEUE ---
            item {
                SmartRevisionQueueView(
                    queue = revisionQueue,
                    onStartRevisionSession = { item ->
                        onStartFocusOnTopic(item.subject, item.topic)
                    },
                    onStartQuickRevisionTest = { item, count ->
                        onStartTestWithConfig(
                            MockTestConfig(
                                exam = user?.examName ?: "JEE Main",
                                testType = MockTestType.TOPIC_TEST,
                                subject = item.subject,
                                topic = item.topic,
                                questionCount = count,
                                timeLimitMinutes = if (count <= 5) 10 else 20
                            )
                        )
                    }
                )
            }
        } else if (selectedTab == 2) {
            // --- SECTION 2: TOPIC MASTERY & OBJECTIVES ---
            item {
                TopicMasteryAndObjectivesView(
                    user = user,
                    examObjective = examObjective,
                    topicMasteries = topicMasteries,
                    sessionHistory = sessionHistory,
                    snapshot = snapshot,
                    examReadiness = examReadiness,
                    subjectSummaries = subjectSummaries,
                    recommendations = recommendations,
                    dailyPlan = dailyPlan,
                    onSaveExamObjective = onSaveExamObjective,
                    onStartFocusOnTopic = onStartFocusOnTopic,
                    onSetManualTopicOverride = onSetManualTopicOverride,
                    onResetPreparationData = onResetPreparationData
                )
            }
        } else {
            // --- SECTION 3: MISTAKE REVIEW & DIAGNOSTICS ---
            item {
                MistakeReviewView(
                    mistakes = mistakes,
                    onDiagnoseMistakes = onDiagnoseMistakes,
                    onMarkMistakeMastered = onMarkMistakeMastered,
                    onRequestAiExplanation = { mistake ->
                        onDiagnoseMistakes(mistake.subject)
                    },
                    aiExplanationText = mistakeDiagnosis
                )
            }
        }
    }

    // Modal to configure & launch a mock test
    if (showSetupDialog) {
        MockTestSetupDialog(
            userProfile = user,
            userMaterials = userMaterials,
            onDismiss = { showSetupDialog = false },
            onStartTest = { config ->
                showSetupDialog = false
                onStartTestWithConfig(config)
            },
            onManageMaterials = {
                showSetupDialog = false
                showMaterialManager = true
            }
        )
    }

    // Modal to manage custom question materials
    if (showMaterialManager) {
        UserMaterialManagerDialog(
            materials = userMaterials,
            defaultSubject = user?.subjects?.firstOrNull() ?: "Physics",
            onDismiss = { showMaterialManager = false },
            onSaveMaterial = onSaveUserMaterial,
            onDeleteMaterial = onDeleteUserMaterial
        )
    }

    // Modal for Smart Question Bank Explorer
    if (showQuestionBankExplorer) {
        QuestionBankExplorerDialog(
            repository = com.example.data.repository.ExamQuestionBankRepository(),
            currentExamName = user?.examName ?: "RRB NTPC (Railway)",
            onDismiss = { showQuestionBankExplorer = false },
            onStartPracticeWithQuestions = { selectedQs, title ->
                showQuestionBankExplorer = false
                onStartTestWithConfig(
                    MockTestConfig(
                        exam = user?.examName ?: "RRB NTPC (Railway)",
                        testType = MockTestType.CUSTOM_TEST,
                        subject = selectedQs.firstOrNull()?.subject ?: "Mixed",
                        questionCount = selectedQs.size,
                        timeLimitMinutes = (selectedQs.size * 1.5).toInt().coerceAtLeast(10)
                    )
                )
            },
            onReportQuestion = { qId, reason, notes ->
                // Handled in viewModel via reportQuestion
            }
        )
    }

    // Generating loading indicator
    if (isTestGenerating) {
        Dialog(onDismissRequest = {}) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.95f
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Preparing Your Mock Test...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Curating authentic PYQs & AI practice questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

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
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Analytics & Mocks, 1: Topic Mastery, 2: Mistake Book
    var showSetupDialog by remember { mutableStateOf(false) }
    var showMaterialManager by remember { mutableStateOf(false) }

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
                        text = "📊 Progress & Intelligence",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Exam mastery, timed mocks & session history",
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Mocks (${attempts.size})",
                    "Mastery & Goals",
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
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // --- SECTION 0: ANALYTICS & MOCK TESTS ---
            item {
                MockTestAnalyticsView(
                    attempts = attempts,
                    user = user,
                    mistakes = mistakes,
                    focusMinutes = user?.totalFocusMinutes ?: 0,
                    onLaunchNewTest = { showSetupDialog = true },
                    onReviewTest = onReviewPastTest,
                    onRetakeTest = onRetakeTest,
                    onDeleteTest = onDeletePastTest,
                    onManageMaterials = { showMaterialManager = true }
                )
            }
        } else if (selectedTab == 1) {
            // --- SECTION 1: TOPIC MASTERY & OBJECTIVES ---
            item {
                TopicMasteryAndObjectivesView(
                    user = user,
                    examObjective = examObjective,
                    topicMasteries = topicMasteries,
                    sessionHistory = sessionHistory,
                    snapshot = snapshot,
                    onSaveExamObjective = onSaveExamObjective,
                    onStartFocusOnTopic = onStartFocusOnTopic
                )
            }
        } else {
            // --- SECTION 2: MISTAKE BOOK ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mistake Book 📖",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Wrong mock test answers auto-cataloged for mastery",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Button(
                        onClick = { onDiagnoseMistakes(user?.subjects?.firstOrNull() ?: "Physics") },
                        colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("diagnose_mistakes_btn")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Diagnosis", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (mistakeDiagnosis != null) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        fillAlpha = 0.85f
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Psychology, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gemini Mistake Pattern Diagnosis",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldenSpark
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = mistakeDiagnosis,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            if (mistakes.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.5f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.DoneAll, null, tint = EmeraldSuccess, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Zero Mistakes Recorded! 🎉",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Any incorrect answers from mock tests are automatically saved here for deep revision.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(mistakes, key = { it.id }) { mistake ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        fillAlpha = if (mistake.isMastered) 0.35f else 0.75f
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${mistake.subject} • ${mistake.topic}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (mistake.isMastered) "Mastered" else "Mark Mastered",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (mistake.isMastered) EmeraldSuccess else Color(0xFF94A3B8)
                                    )
                                    Checkbox(
                                        checked = mistake.isMastered,
                                        onCheckedChange = { onMarkMistakeMastered(mistake.id, !mistake.isMastered) },
                                        colors = CheckboxDefaults.colors(checkedColor = EmeraldSuccess)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Q: ${mistake.questionText}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row {
                                Text(text = "❌ Your Answer: ", color = CoralRose, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(text = mistake.studentAnswer, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                            }

                            Row {
                                Text(text = "✅ Correct: ", color = EmeraldSuccess, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(text = mistake.correctAnswer, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }

                            if (mistake.explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "💡 Explanation: ${mistake.explanation}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
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

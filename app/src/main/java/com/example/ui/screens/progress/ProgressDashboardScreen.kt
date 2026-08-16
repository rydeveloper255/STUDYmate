package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.*
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StreakBadge
import com.example.ui.components.XpBadge
import com.example.ui.components.glassEffect
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.ActiveTestState

@Composable
fun ProgressDashboardScreen(
    user: UserProfile?,
    attempts: List<MockTestAttempt>,
    mistakes: List<MistakeItem>,
    activeTestState: ActiveTestState,
    isTestGenerating: Boolean,
    mistakeDiagnosis: String?,
    onStartTest: (subject: String, chapter: String, mode: String) -> Unit,
    onSelectAnswer: (questionIndex: Int, optionIndex: Int) -> Unit,
    onToggleMarkForReview: (questionIndex: Int) -> Unit,
    onNavigateQuestion: (index: Int) -> Unit,
    onSubmitTest: () -> Unit,
    onExitTest: () -> Unit,
    onDiagnoseMistakes: (subject: String) -> Unit,
    onMarkMistakeMastered: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Analytics & Tests, 1: Mistake Book
    var showStartTestModal by remember { mutableStateOf(false) }

    var testSubject by remember { mutableStateOf("Physics") }
    var testChapter by remember { mutableStateOf("Electromagnetism") }
    var testMode by remember { mutableStateOf("AI Practice") }

    // If an active test is running or showing completed results, show Fullscreen Test View
    if (activeTestState.isTestInProgress || activeTestState.isCompleted) {
        ActiveMockTestView(
            state = activeTestState,
            onSelectAnswer = onSelectAnswer,
            onToggleMarkForReview = onToggleMarkForReview,
            onNavigateQuestion = onNavigateQuestion,
            onSubmitTest = onSubmitTest,
            onExitTest = onExitTest
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
                        text = "📊 Progress & Tests",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Real-time analytics & AI practice exams",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StreakBadge(streakDays = user?.streakDays ?: 1)
                }
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
                listOf("Analytics & Mocks", "Mistake Book (${mistakes.size})").forEachIndexed { idx, label ->
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
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color(0xFF070B19) else Color(0xFFCBD5E1),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // --- SECTION 0: ANALYTICS & MOCK TESTS ---

            // Stat Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val accuracy = if (attempts.isNotEmpty()) {
                        (attempts.map { it.accuracyPercent }.average()).toInt()
                    } else 88

                    StatMetricCard(
                        title = "Accuracy",
                        value = "$accuracy%",
                        subtitle = "Overall test score",
                        color = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )

                    StatMetricCard(
                        title = "Solved",
                        value = "${user?.totalQuestionsSolved ?: 140}",
                        subtitle = "Total questions",
                        color = ElectricViolet,
                        modifier = Modifier.weight(1f)
                    )

                    StatMetricCard(
                        title = "Focus Time",
                        value = "${(user?.totalFocusMinutes ?: 135) / 60}h",
                        subtitle = "Deep work",
                        color = GoldenSpark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly AI Report Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = 0.8f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(ElectricViolet, NeonCyan))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.AutoGraph, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "📊 Weekly AI Progress Report",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x3038BDF8)
                            ) {
                                Text(
                                    text = "+18% Consistency",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricBox(
                                title = "Focus Time",
                                value = "14.5 hrs",
                                color = NeonCyan,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBox(
                                title = "Questions",
                                value = "142 solved",
                                color = ElectricViolet,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBox(
                                title = "Strongest",
                                value = "Physics",
                                color = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "💡 Next Week's Recommendation: Your consistency improved by 18% this week. Focus next week on Organic Chemistry and Physics numericals to boost total accuracy above 90%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Adaptive Practice Quiz Banner
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = 0.75f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(GoldenSpark, CoralRose))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Psychology, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🧩 Adaptive Practice Quiz",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Questions adapt dynamically (Easy → Medium → Hard). Mistakes trigger targeted concept flashcards.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showStartTestModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF070B19)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Start", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Weekly Study Chart Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    fillAlpha = 0.7f
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Weekly Focus Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Last 7 Days",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Weekly Bar Graph
                        val days = listOf("Mon" to 45, "Tue" to 90, "Wed" to 120, "Thu" to 60, "Fri" to 140, "Sat" to 180, "Sun" to 110)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            days.forEach { (day, minutes) ->
                                val heightFrac = (minutes / 180f).coerceIn(0.15f, 1f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .fillMaxHeight(heightFrac)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                            .background(
                                                Brush.verticalGradient(listOf(NeonCyan, DeepIndigo))
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Mock Test Launcher CTA
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Mock Tests & PYQs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Button(
                        onClick = { showStartTestModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("launch_mock_test_modal_btn")
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Test", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Mock Test History Items
            if (attempts.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.5f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.Quiz, null, tint = NeonCyan, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tests attempted yet.",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Generate your first AI Mock Test to evaluate speed and accuracy.",
                                color = Color(0xFF94A3B8),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(attempts, key = { it.id }) { attempt ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        fillAlpha = 0.7f
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = attempt.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${attempt.score}/${attempt.totalQuestions} Correct • ${attempt.accuracyPercent.toInt()}% Accuracy",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (attempt.accuracyPercent >= 80) EmeraldSuccess.copy(alpha = 0.2f) else CoralRose.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (attempt.accuracyPercent >= 80) "Passed" else "Review",
                                        color = if (attempt.accuracyPercent >= 80) EmeraldSuccess else CoralRose,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (attempt.aiRecommendation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🤖 Recommendation: ${attempt.aiRecommendation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- SECTION 1: MISTAKE BOOK ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mistake Book 📖",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Button(
                        onClick = { onDiagnoseMistakes("Physics") },
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
                                text = "Any wrong answers from mock tests are automatically saved here for deep revision.",
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

    // Modal to create/launch a mock test
    if (showStartTestModal) {
        AlertDialog(
            onDismissRequest = { showStartTestModal = false },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Generate AI Mock Test", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Gemini will craft high-yield exam-pattern questions with step-by-step solutions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    OutlinedTextField(
                        value = testSubject,
                        onValueChange = { testSubject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonCyan)
                    )

                    OutlinedTextField(
                        value = testChapter,
                        onValueChange = { testChapter = it },
                        label = { Text("Chapter / Topic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonCyan)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("AI Practice", "PYQ Mode", "Mixed").forEach { mode ->
                            val isSel = testMode == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) NeonCyan else Color(0x20FFFFFF),
                                modifier = Modifier.springClickable { testMode = mode }
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSel) Color(0xFF070B19) else Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStartTestModal = false
                        onStartTest(testSubject, testChapter, testMode)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                    modifier = Modifier.testTag("confirm_start_mock_test_btn")
                ) {
                    Text("Start Test 🚀", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTestModal = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

// --- Active Fullscreen Mock Test View ---

@Composable
fun ActiveMockTestView(
    state: ActiveTestState,
    onSelectAnswer: (Int, Int) -> Unit,
    onToggleMarkForReview: (Int) -> Unit,
    onNavigateQuestion: (Int) -> Unit,
    onSubmitTest: () -> Unit,
    onExitTest: () -> Unit
) {
    val currentIdx = state.currentQuestionIndex
    val total = state.questions.size
    val currentQ = state.questions.getOrNull(currentIdx)
    val chosenOption = state.selectedAnswers[currentIdx]
    val isMarked = state.markedForReview.contains(currentIdx)

    val remainingMins = state.remainingSeconds / 60
    val remainingSecs = state.remainingSeconds % 60
    val timerText = String.format("%02d:%02d", remainingMins, remainingSecs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(18.dp)
            .testTag("active_mock_test_view")
    ) {
        if (state.isCompleted && state.completedAttempt != null) {
            // Test Result Analysis Summary
            val attempt = state.completedAttempt
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (attempt.accuracyPercent >= 70) Icons.Filled.EmojiEvents else Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = if (attempt.accuracyPercent >= 70) GoldenSpark else NeonCyan,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Test Completed! 🎯",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = attempt.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.85f
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${attempt.score} / ${attempt.totalQuestions}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonCyan
                            )
                            Text(
                                text = "Score (${attempt.accuracyPercent.toInt()}% Accuracy)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "🤖 Gemini AI Evaluation:\n${attempt.aiRecommendation}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                GlassButton(
                    text = "Return to Dashboard",
                    onClick = onExitTest,
                    isPrimary = true,
                    testTag = "exit_test_result_btn"
                )
            }
        } else if (currentQ != null) {
            // Live Test Question
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Test Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentIdx + 1} of $total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x30F43F5E)
                    ) {
                        Text(
                            text = "⏱️ $timerText",
                            color = CoralRose,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Question Box
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    fillAlpha = 0.85f
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x2038BDF8)
                        ) {
                            Text(
                                text = "${currentQ.subject} • ${currentQ.topic}",
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentQ.questionText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Options
                        currentQ.options.forEachIndexed { optIdx, optText ->
                            val isSelected = chosenOption == optIdx
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NeonCyan else Color(0x18FFFFFF))
                                    .border(1.dp, if (isSelected) Color.White else Color(0x25FFFFFF), RoundedCornerShape(12.dp))
                                    .springClickable(testTag = "test_opt_$optIdx") {
                                        onSelectAnswer(currentIdx, optIdx)
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "${('A' + optIdx)}. $optText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) Color(0xFF070B19) else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Question Navigation & Submit
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { onToggleMarkForReview(currentIdx) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isMarked) GoldenSpark else Color(0xFF94A3B8)
                            )
                        ) {
                            Icon(Icons.Filled.Bookmark, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isMarked) "Marked" else "Review Later")
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { onNavigateQuestion(currentIdx - 1) },
                                enabled = currentIdx > 0
                            ) {
                                Icon(Icons.Filled.ChevronLeft, "Previous", tint = Color.White)
                            }
                            IconButton(
                                onClick = { onNavigateQuestion(currentIdx + 1) },
                                enabled = currentIdx < total - 1
                            ) {
                                Icon(Icons.Filled.ChevronRight, "Next", tint = Color.White)
                            }
                        }
                    }

                    GlassButton(
                        text = if (currentIdx == total - 1) "Submit Test" else "Next Question",
                        onClick = {
                            if (currentIdx == total - 1) {
                                onSubmitTest()
                            } else {
                                onNavigateQuestion(currentIdx + 1)
                            }
                        },
                        isPrimary = true,
                        testTag = "test_next_or_submit_btn"
                    )
                }
            }
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassEffect(shape = RoundedCornerShape(18.dp), fillAlpha = 0.65f)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}


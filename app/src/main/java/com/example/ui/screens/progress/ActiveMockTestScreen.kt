package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Question
import com.example.data.model.QuestionSource
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.ActiveTestState

@Composable
fun ActiveMockTestScreen(
    state: ActiveTestState,
    onSelectAnswer: (Int, Int) -> Unit,
    onClearAnswer: (Int) -> Unit,
    onToggleMarkForReview: (Int) -> Unit,
    onSkipQuestion: (Int) -> Unit,
    onNavigateQuestion: (Int) -> Unit,
    onSetPaletteOpen: (Boolean) -> Unit,
    onSetSubmitConfirmOpen: (Boolean) -> Unit,
    onSubmitTest: () -> Unit,
    onExitTest: () -> Unit,
    onRetakeTest: () -> Unit
) {
    val currentIdx = state.currentQuestionIndex
    val total = state.questions.size
    val currentQ = state.questions.getOrNull(currentIdx)
    val chosenOption = state.selectedAnswers[currentIdx]
    val isMarked = state.markedForReview.contains(currentIdx)

    val remainingMins = state.remainingSeconds / 60
    val remainingSecs = state.remainingSeconds % 60
    val timerText = String.format("%02d:%02d", remainingMins, remainingSecs)
    val isLowTime = state.remainingSeconds <= 120

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(16.dp)
            .testTag("active_mock_test_screen")
    ) {
        if (state.isCompleted && state.completedAttempt != null) {
            // Completed Result & Analysis View
            MockTestResultScreen(
                attempt = state.completedAttempt,
                detailedQuestions = state.detailedQuestions,
                onExitToDashboard = onExitTest,
                onRetake = onRetakeTest
            )
        } else if (currentQ != null) {
            // In-Progress Live Test
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top Bar: Header, Timer & Palette Trigger
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = "Question ${currentIdx + 1} of $total",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Timer Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isLowTime) CoralRose.copy(alpha = 0.25f) else Color(0x2038BDF8),
                                border = BorderStroke(
                                    1.dp,
                                    if (isLowTime) CoralRose else NeonCyan.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Timer,
                                        null,
                                        tint = if (isLowTime) CoralRose else NeonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = timerText,
                                        color = if (isLowTime) CoralRose else NeonCyan,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Palette Grid Button
                            IconButton(
                                onClick = { onSetPaletteOpen(true) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x20FFFFFF))
                                    .testTag("open_question_palette_btn")
                            ) {
                                Icon(Icons.Filled.GridView, "Question Palette", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Question Status Ribbon (Source Label & Tags)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Strict Question Source Badge
                        QuestionSourceBadge(source = currentQ.source, label = currentQ.sourceLabel, yearOrTag = currentQ.yearOrTag)

                        // Subject / Topic Tag
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x18FFFFFF)
                        ) {
                            Text(
                                text = "${currentQ.subject} • ${currentQ.topic}",
                                color = Color(0xFFCBD5E1),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // 2. Question Body & Options (Scrollable)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        fillAlpha = 0.85f
                    ) {
                        Column {
                            Text(
                                text = currentQ.questionText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                lineHeight = 24.sp
                            )
                        }
                    }

                    // Options List
                    currentQ.options.forEachIndexed { optIdx, optText ->
                        val isSelected = chosenOption == optIdx
                        val optionChar = ('A' + optIdx).toString()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x14FFFFFF)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else Color(0x20FFFFFF),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .springClickable(testTag = "test_option_$optIdx") {
                                    onSelectAnswer(currentIdx, optIdx)
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) NeonCyan else Color(0x25FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = optionChar,
                                        color = if (isSelected) Color(0xFF050814) else Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = optText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 3. Action Controls: Mark, Skip, Clear, Prev, Next, Submit
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Secondary action row: Mark for review, Skip, Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mark For Review
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isMarked) GoldenSpark.copy(alpha = 0.2f) else Color(0x18FFFFFF),
                            border = BorderStroke(1.dp, if (isMarked) GoldenSpark else Color(0x20FFFFFF)),
                            modifier = Modifier.springClickable(testTag = "mark_for_review_btn") {
                                onToggleMarkForReview(currentIdx)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    if (isMarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    null,
                                    tint = if (isMarked) GoldenSpark else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isMarked) "Marked" else "Mark for Review",
                                    color = if (isMarked) GoldenSpark else Color(0xFFCBD5E1),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Clear Response
                        if (chosenOption != null) {
                            TextButton(
                                onClick = { onClearAnswer(currentIdx) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Clear Selection", color = CoralRose, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Skip Question
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x18FFFFFF),
                            border = BorderStroke(1.dp, Color(0x20FFFFFF)),
                            modifier = Modifier.springClickable(testTag = "skip_question_btn") {
                                onSkipQuestion(currentIdx)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Skip ⏭️",
                                    color = Color(0xFFCBD5E1),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Primary navigation row: Previous, Next / Submit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateQuestion(currentIdx - 1) },
                            enabled = currentIdx > 0,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.7f)
                                .height(44.dp)
                                .testTag("prev_question_btn")
                        ) {
                            Icon(Icons.Filled.ChevronLeft, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Prev")
                        }

                        if (currentIdx == total - 1) {
                            Button(
                                onClick = { onSetSubmitConfirmOpen(true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSuccess,
                                    contentColor = Color(0xFF050814)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("submit_test_primary_btn")
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Submit Test", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { onNavigateQuestion(currentIdx + 1) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color(0xFF050814)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("next_question_btn")
                            ) {
                                Text("Next Question", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Question Palette Modal Dialog
    if (state.isPaletteOpen) {
        QuestionPaletteDialog(
            state = state,
            onDismiss = { onSetPaletteOpen(false) },
            onSelectQuestion = {
                onNavigateQuestion(it)
                onSetPaletteOpen(false)
            },
            onSubmitTestPrompt = {
                onSetPaletteOpen(false)
                onSetSubmitConfirmOpen(true)
            }
        )
    }

    // 5. Submit Confirmation Dialog
    if (state.isSubmitConfirmOpen) {
        val totalQ = state.questions.size
        val answeredCount = state.selectedAnswers.size
        val markedCount = state.markedForReview.size
        val unattemptedCount = totalQ - answeredCount

        AlertDialog(
            onDismissRequest = { onSetSubmitConfirmOpen(false) },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Submit Mock Test?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Review your test summary before final evaluation:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummaryBadge(label = "Answered", count = answeredCount, color = EmeraldSuccess, modifier = Modifier.weight(1f))
                        SummaryBadge(label = "Marked", count = markedCount, color = GoldenSpark, modifier = Modifier.weight(1f))
                        SummaryBadge(label = "Skipped", count = unattemptedCount, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetSubmitConfirmOpen(false)
                        onSubmitTest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF050814)),
                    modifier = Modifier.testTag("confirm_submit_test_btn")
                ) {
                    Text("Yes, Submit Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onSetSubmitConfirmOpen(false) }) {
                    Text("Continue Test", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
fun QuestionSourceBadge(
    source: QuestionSource,
    label: String,
    yearOrTag: String = ""
) {
    val (badgeColor, icon, text) = when (source) {
        QuestionSource.PREVIOUS_YEAR -> Triple(
            GoldenSpark,
            Icons.Filled.School,
            if (yearOrTag.isNotBlank()) "Previous Year • $yearOrTag" else "Previous Year"
        )
        QuestionSource.PRACTICE -> Triple(
            ElectricViolet,
            Icons.Filled.EditNote,
            label.ifBlank { "Practice" }
        )
        QuestionSource.AI_GENERATED -> Triple(
            EmeraldSuccess,
            Icons.Filled.AutoAwesome,
            "AI Generated"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = badgeColor.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(icon, null, tint = badgeColor, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = badgeColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryBadge(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Text(text = "$count", color = color, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = label, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun QuestionPaletteDialog(
    state: ActiveTestState,
    onDismiss: () -> Unit,
    onSelectQuestion: (Int) -> Unit,
    onSubmitTestPrompt: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            fillAlpha = 0.95f
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question Palette",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, null, tint = Color(0xFF94A3B8))
                    }
                }

                // Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem(color = EmeraldSuccess, label = "Answered")
                    LegendItem(color = GoldenSpark, label = "Marked")
                    LegendItem(color = Color(0xFF64748B), label = "Unvisited")
                }

                HorizontalDivider(color = Color(0x20FFFFFF), modifier = Modifier.padding(vertical = 6.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 260.dp)
                ) {
                    itemsIndexed(state.questions) { idx, _ ->
                        val isCurrent = state.currentQuestionIndex == idx
                        val isAnswered = state.selectedAnswers.containsKey(idx)
                        val isMarked = state.markedForReview.contains(idx)

                        val bgColor = when {
                            isAnswered && isMarked -> GoldenSpark
                            isAnswered -> EmeraldSuccess
                            isMarked -> GoldenSpark
                            else -> Color(0x25FFFFFF)
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(bgColor.copy(alpha = if (isCurrent) 1f else 0.8f))
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) NeonCyan else Color.Transparent,
                                    shape = CircleShape
                                )
                                .springClickable { onSelectQuestion(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                color = if (isAnswered || isMarked) Color(0xFF050814) else Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                GlassButton(
                    text = "Submit Test Now",
                    onClick = onSubmitTestPrompt,
                    isPrimary = true
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
    }
}

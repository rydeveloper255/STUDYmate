package com.example.ui.screens.progress

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Question
import com.example.data.model.QuestionCbtState
import com.example.data.model.QuestionSource
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.ActiveTestState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.persistence.PersistenceMonitor
import com.example.data.persistence.PersistenceStatus
import com.example.data.model.MockTestType

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
    onRetakeTest: () -> Unit,
    onRetakeWrongQuestions: (() -> Unit)? = null,
    onRetryUnanswered: (() -> Unit)? = null,
    onStartPractice: ((com.example.service.intelligence.SmartPracticeRecommendation) -> Unit)? = null,
    onSaveAndNext: (() -> Unit)? = null,
    onMarkForReviewAndNext: (() -> Unit)? = null,
    onPreviousQuestion: (() -> Unit)? = null
) {
    var showExitWarningDialog by remember { mutableStateOf(false) }
    var isRightPaletteExpanded by remember { mutableStateOf(true) }

    // Request Landscape mode for live CBT exam when active; restore default when disposed
    val context = LocalContext.current
    DisposableEffect(state.isTestInProgress, state.isCompleted) {
        val activity = context as? Activity
        if (state.isTestInProgress && !state.isCompleted) {
            val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            try {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } catch (_: Exception) { }
            onDispose {
                try {
                    activity?.requestedOrientation = originalOrientation
                } catch (_: Exception) { }
            }
        } else {
            onDispose { }
        }
    }

    BackHandler(enabled = true) {
        if (state.isCompleted) {
            onExitTest()
        } else if (state.isSubmitConfirmOpen) {
            onSetSubmitConfirmOpen(false)
        } else if (state.isPaletteOpen) {
            onSetPaletteOpen(false)
        } else {
            showExitWarningDialog = true
        }
    }

    val currentIdx = state.currentQuestionIndex
    val total = state.questions.size
    val currentQ = state.questions.getOrNull(currentIdx)
    val chosenOption = state.selectedAnswers[currentIdx]
    val isMarked = state.markedForReview.contains(currentIdx)

    val activePersistenceStatus by PersistenceMonitor.activeStatus.collectAsStateWithLifecycle()

    val remainingMins = state.remainingSeconds / 60
    val remainingSecs = state.remainingSeconds % 60
    val timerText = String.format("%02d:%02d", remainingMins, remainingSecs)
    val isLowTime = state.remainingSeconds <= 120

    // Distinct subjects for top section tab bar
    val subjects = remember(state.questions) {
        state.questions.map { it.subject.ifBlank { "General" } }.distinct()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("active_mock_test_screen")
    ) {
        val isLandscape = maxWidth > maxHeight || maxWidth >= 600.dp

        if (state.isCompleted && state.completedAttempt != null) {
            // Completed Result & Analysis View
            MockTestResultScreen(
                attempt = state.completedAttempt,
                detailedQuestions = state.detailedQuestions,
                testIntelligence = state.testIntelligence,
                isNovaAnalyzing = state.isNovaAnalyzing,
                onExitToDashboard = onExitTest,
                onRetake = onRetakeTest,
                onRetryIncorrect = onRetakeWrongQuestions,
                onRetryUnanswered = onRetryUnanswered,
                onStartPractice = onStartPractice
            )
        } else if (currentQ != null) {
            if (isLandscape) {
                // LANDSCAPE PROFESSIONAL CBT EXAM LAYOUT
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // LEFT MAIN AREA (Header, Question, Options, Bottom Bar)
                    Column(
                        modifier = Modifier
                            .weight(if (isRightPaletteExpanded) 0.68f else 1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. LANDSCAPE TOP HEADER BAR
                        CbtTopHeaderBar(
                            title = state.title,
                            currentIdx = currentIdx,
                            total = total,
                            timerText = timerText,
                            remainingSeconds = state.remainingSeconds,
                            currentSubject = currentQ.subject,
                            subjects = subjects,
                            onSelectSubject = { subj ->
                                val targetIdx = state.questions.indexOfFirst { it.subject == subj }
                                if (targetIdx >= 0) onNavigateQuestion(targetIdx)
                            },
                            isPaletteOpen = isRightPaletteExpanded,
                            onTogglePalette = { isRightPaletteExpanded = !isRightPaletteExpanded },
                            onSubmitPrompt = { onSetSubmitConfirmOpen(true) },
                            autoSaveStatus = activePersistenceStatus
                        )

                        if (state.submissionError != null) {
                            SubmissionErrorBanner(
                                error = state.submissionError,
                                onRetry = onSubmitTest
                            )
                        }

                        // 2. SCROLLABLE QUESTION & OPTIONS AREA
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Question Metadata Ribbon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                QuestionSourceBadge(
                                    source = currentQ.source,
                                    label = currentQ.sourceLabel,
                                    yearOrTag = currentQ.yearOrTag
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0x18FFFFFF),
                                    border = BorderStroke(1.dp, Color(0x25FFFFFF))
                                ) {
                                    Text(
                                        text = "Marks: +4 / -1",
                                        color = Color(0xFFE2E8F0),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Question Content Card
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                fillAlpha = 0.92f
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (currentQ.topic.isNotBlank()) {
                                        Text(
                                            text = "TOPIC: ${currentQ.topic.uppercase()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "Question ${currentIdx + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldenSpark,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = currentQ.questionText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        lineHeight = 23.sp
                                    )
                                }
                            }

                            // Answer Options Grid (2x2 or Vertical depending on text length)
                            val useTwoColumns = currentQ.options.all { it.length < 35 }
                            if (useTwoColumns) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (row in 0 until (currentQ.options.size + 1) / 2) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (col in 0..1) {
                                                val optIdx = row * 2 + col
                                                if (optIdx < currentQ.options.size) {
                                                    val optText = currentQ.options[optIdx]
                                                    OptionCard(
                                                        optIdx = optIdx,
                                                        optText = optText,
                                                        isSelected = chosenOption == optIdx,
                                                        onSelect = { onSelectAnswer(currentIdx, optIdx) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    currentQ.options.forEachIndexed { optIdx, optText ->
                                        OptionCard(
                                            optIdx = optIdx,
                                            optText = optText,
                                            isSelected = chosenOption == optIdx,
                                            onSelect = { onSelectAnswer(currentIdx, optIdx) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // 3. STICKY BOTTOM ACTION BAR
                        CbtBottomActionBar(
                            currentIdx = currentIdx,
                            total = total,
                            chosenOption = chosenOption,
                            isMarked = isMarked,
                            onClearAnswer = { onClearAnswer(currentIdx) },
                            onMarkForReview = {
                                if (onMarkForReviewAndNext != null) onMarkForReviewAndNext()
                                else {
                                    onToggleMarkForReview(currentIdx)
                                    if (currentIdx < total - 1) onNavigateQuestion(currentIdx + 1)
                                }
                            },
                            onPrevious = {
                                if (onPreviousQuestion != null) onPreviousQuestion()
                                else onNavigateQuestion(currentIdx - 1)
                            },
                            onSaveAndNext = {
                                if (onSaveAndNext != null) onSaveAndNext()
                                else onNavigateQuestion(currentIdx + 1)
                            },
                            onSubmitConfirm = { onSetSubmitConfirmOpen(true) }
                        )
                    }

                    // RIGHT PANEL (Collapsible CBT Question Palette)
                    if (isRightPaletteExpanded) {
                        Surface(
                            modifier = Modifier
                                .weight(0.32f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0D1526),
                            border = BorderStroke(1.dp, Color(0x3038BDF8))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.GridView, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Question Palette",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { isRightPaletteExpanded = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.ChevronRight, "Collapse Palette", tint = Color(0xFF94A3B8))
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Status Counters Summary
                                PaletteLegendSummary(state = state)

                                HorizontalDivider(color = Color(0x20FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                                // Question Grid
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    itemsIndexed(state.questions) { idx, _ ->
                                        PaletteItemButton(
                                            idx = idx,
                                            total = total,
                                            state = state,
                                            onSelectQuestion = onNavigateQuestion
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Button(
                                    onClick = { onSetSubmitConfirmOpen(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF050814)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("palette_submit_btn")
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Submit Test Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // PORTRAIT RESPONSIVE FALLBACK LAYOUT
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (state.submissionError != null) {
                        SubmissionErrorBanner(
                            error = state.submissionError,
                            onRetry = onSubmitTest
                        )
                    }

                    // 1. TOP BAR
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Q ${currentIdx + 1} of $total",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    Text("•", color = Color(0xFF64748B))
                                    Text(
                                        text = currentQ.subject,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Timer Display
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
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Timer,
                                            contentDescription = "$timerText remaining",
                                            tint = if (isLowTime) CoralRose else NeonCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = timerText,
                                            color = if (isLowTime) CoralRose else NeonCyan,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Palette Drawer Trigger
                                IconButton(
                                    onClick = { onSetPaletteOpen(true) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x20FFFFFF))
                                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(10.dp))
                                        .testTag("open_question_palette_btn")
                                ) {
                                    Icon(Icons.Filled.GridView, "Open Question Palette", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        if (subjects.size > 1) {
                            SubjectTabsRow(
                                subjects = subjects,
                                selectedSubject = currentQ.subject,
                                onSelectSubject = { subj ->
                                    val targetIdx = state.questions.indexOfFirst { it.subject == subj }
                                    if (targetIdx >= 0) onNavigateQuestion(targetIdx)
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QuestionSourceBadge(
                                source = currentQ.source,
                                label = currentQ.sourceLabel,
                                yearOrTag = currentQ.yearOrTag
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x18FFFFFF),
                                border = BorderStroke(1.dp, Color(0x25FFFFFF))
                            ) {
                                Text(
                                    text = "Marks: +4 / -1",
                                    color = Color(0xFFE2E8F0),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // 2. QUESTION BODY & OPTIONS
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            fillAlpha = 0.9f
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (currentQ.topic.isNotBlank()) {
                                    Text(
                                        text = "TOPIC: ${currentQ.topic.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Text(
                                    text = currentQ.questionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    lineHeight = 24.sp
                                )
                            }
                        }

                        currentQ.options.forEachIndexed { optIdx, optText ->
                            OptionCard(
                                optIdx = optIdx,
                                optText = optText,
                                isSelected = chosenOption == optIdx,
                                onSelect = { onSelectAnswer(currentIdx, optIdx) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 3. PORTRAIT BOTTOM ACTION BAR
                    CbtBottomActionBar(
                        currentIdx = currentIdx,
                        total = total,
                        chosenOption = chosenOption,
                        isMarked = isMarked,
                        onClearAnswer = { onClearAnswer(currentIdx) },
                        onMarkForReview = {
                            if (onMarkForReviewAndNext != null) onMarkForReviewAndNext()
                            else {
                                onToggleMarkForReview(currentIdx)
                                if (currentIdx < total - 1) onNavigateQuestion(currentIdx + 1)
                            }
                        },
                        onPrevious = {
                            if (onPreviousQuestion != null) onPreviousQuestion()
                            else onNavigateQuestion(currentIdx - 1)
                        },
                        onSaveAndNext = {
                            if (onSaveAndNext != null) onSaveAndNext()
                            else onNavigateQuestion(currentIdx + 1)
                        },
                        onSubmitConfirm = { onSetSubmitConfirmOpen(true) }
                    )
                }
            }
        }
    }

    // QUESTION PALETTE DIALOG (FOR PORTRAIT OR OVERLAY)
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

    // SUBMIT CONFIRMATION DIALOG
    if (state.isSubmitConfirmOpen) {
        val totalQ = state.questions.size
        val answered = state.selectedAnswers.size
        val marked = state.markedForReview.size
        val unattempted = (totalQ - answered).coerceAtLeast(0)

        AlertDialog(
            onDismissRequest = { onSetSubmitConfirmOpen(false) },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Test Confirmation", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Are you sure you want to end this test session? Your responses will be evaluated instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummaryBadge(label = "Answered", count = answered, color = EmeraldSuccess, modifier = Modifier.weight(1f))
                        SummaryBadge(label = "Marked", count = marked, color = GoldenSpark, modifier = Modifier.weight(1f))
                        SummaryBadge(label = "Unattempted", count = unattempted, color = CoralRose, modifier = Modifier.weight(1f))
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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_submit_test_btn")
                ) {
                    Text("Submit Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onSetSubmitConfirmOpen(false) }) {
                    Text("Continue Test", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // EXIT TEST WARNING DIALOG
    if (showExitWarningDialog) {
        AlertDialog(
            onDismissRequest = { showExitWarningDialog = false },
            containerColor = Color(0xFF1E1428),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = CoralRose, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exit This Test?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Your test progress and answers are automatically saved locally. You can resume this session anytime from the main screen.",
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitWarningDialog = false
                        onExitTest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Exit & Save Progress")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitWarningDialog = false }) {
                    Text("Continue Test", color = NeonCyan)
                }
            }
        )
    }

    // SUBMITTING OVERLAY DIALOG
    if (state.isSubmitting) {
        Dialog(onDismissRequest = {}) {
            GlassCard(
                modifier = Modifier.padding(24.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = EmeraldSuccess, modifier = Modifier.size(44.dp))
                    Text(
                        text = "Evaluating Test...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Calculating deterministic marks & saving attempt history",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private data class CbtTimerVisual(
    val bg: Color,
    val border: Color,
    val color: Color,
    val prefix: String
)

private data class CbtSavePillVisual(
    val bg: Color,
    val border: Color,
    val textColor: Color,
    val label: String
)

@Composable
private fun CbtTopHeaderBar(
    title: String,
    currentIdx: Int,
    total: Int,
    timerText: String,
    remainingSeconds: Int,
    currentSubject: String,
    subjects: List<String>,
    onSelectSubject: (String) -> Unit,
    isPaletteOpen: Boolean,
    onTogglePalette: () -> Unit,
    onSubmitPrompt: () -> Unit,
    autoSaveStatus: PersistenceStatus = PersistenceStatus.Saved(System.currentTimeMillis())
) {
    // Timer 3-State Visuals: Normal (>300s), Warning (121-300s), Critical (<=120s)
    val timerVisual = when {
        remainingSeconds <= 120 -> CbtTimerVisual(
            CoralRose.copy(alpha = 0.25f),
            CoralRose,
            CoralRose,
            "🚨 "
        )
        remainingSeconds <= 300 -> CbtTimerVisual(
            GoldenSpark.copy(alpha = 0.2f),
            GoldenSpark,
            GoldenSpark,
            "⚠️ "
        )
        else -> CbtTimerVisual(
            Color(0x2038BDF8),
            NeonCyan.copy(alpha = 0.5f),
            NeonCyan,
            "⏱ "
        )
    }

    // Auto-Save Status Pill Visuals
    val saveVisual = when (autoSaveStatus) {
        is PersistenceStatus.Saving, is PersistenceStatus.Syncing -> CbtSavePillVisual(
            Color(0x2038BDF8),
            Color(0xFF38BDF8).copy(alpha = 0.5f),
            NeonCyan,
            "↻ Syncing"
        )
        is PersistenceStatus.Offline -> CbtSavePillVisual(
            Color(0x20F59E0B),
            Color(0xFFF59E0B).copy(alpha = 0.5f),
            GoldenSpark,
            "○ Offline"
        )
        else -> CbtSavePillVisual(
            Color(0x2010B981),
            Color(0xFF10B981).copy(alpha = 0.5f),
            EmeraldSuccess,
            "✓ Saved"
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0x3038BDF8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exam Title, Question Number & Auto-Save Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonCyan.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${currentIdx + 1} / $total",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = saveVisual.bg,
                        border = BorderStroke(1.dp, saveVisual.border)
                    ) {
                        Text(
                            text = saveVisual.label,
                            color = saveVisual.textColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Timer & Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Timer Display
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = timerVisual.bg,
                        border = BorderStroke(1.dp, timerVisual.border)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Timer,
                                contentDescription = "$timerText remaining",
                                tint = timerVisual.color,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${timerVisual.prefix}$timerText",
                                color = timerVisual.color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Palette Toggle Button
                    IconButton(
                        onClick = onTogglePalette,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPaletteOpen) NeonCyan.copy(alpha = 0.25f) else Color(0x20FFFFFF))
                            .testTag("toggle_landscape_palette_btn")
                    ) {
                        Icon(
                            imageVector = if (isPaletteOpen) Icons.Filled.ViewSidebar else Icons.Outlined.ViewSidebar,
                            contentDescription = "Toggle Question Palette",
                            tint = if (isPaletteOpen) NeonCyan else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Direct Submit Button
                    Button(
                        onClick = onSubmitPrompt,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF050814)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("top_header_submit_btn")
                    ) {
                        Text("Submit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (subjects.size > 1) {
                Spacer(Modifier.height(4.dp))
                SubjectTabsRow(
                    subjects = subjects,
                    selectedSubject = currentSubject,
                    onSelectSubject = onSelectSubject
                )
            }
        }
    }
}

@Composable
private fun SubjectTabsRow(
    subjects: List<String>,
    selectedSubject: String,
    onSelectSubject: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(subjects) { subj ->
            val isSelected = subj.equals(selectedSubject, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0x15FFFFFF),
                border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0x25FFFFFF)),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelectSubject(subj) }
            ) {
                Text(
                    text = subj,
                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun OptionCard(
    optIdx: Int,
    optText: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val optionChar = ('A' + optIdx).toString()
    val accessibilityLabel = "Option $optionChar: $optText${if (isSelected) ", Selected" else ""}"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.22f) else Color(0x14FFFFFF))
            .border(
                width = if (isSelected) 1.8.dp else 1.dp,
                color = if (isSelected) NeonCyan else Color(0x22FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .springClickable(testTag = "test_option_$optIdx") { onSelect() }
            .semantics { contentDescription = accessibilityLabel }
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

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = optText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CbtBottomActionBar(
    currentIdx: Int,
    total: Int,
    chosenOption: Int?,
    isMarked: Boolean,
    onClearAnswer: () -> Unit,
    onMarkForReview: () -> Unit,
    onPrevious: () -> Unit,
    onSaveAndNext: () -> Unit,
    onSubmitConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clear Response
        OutlinedButton(
            onClick = onClearAnswer,
            enabled = chosenOption != null,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (chosenOption != null) CoralRose.copy(alpha = 0.7f) else Color(0x20FFFFFF)),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(0.7f)
                .height(38.dp)
                .testTag("clear_response_btn")
        ) {
            Text(
                "Clear",
                color = if (chosenOption != null) CoralRose else Color(0xFF64748B),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Mark for Review & Next
        Button(
            onClick = onMarkForReview,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMarked) GoldenSpark.copy(alpha = 0.25f) else Color(0x207C3AED),
                contentColor = if (isMarked) GoldenSpark else Color(0xFFC084FC)
            ),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (isMarked) GoldenSpark else Color(0x607C3AED)),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(1.1f)
                .height(38.dp)
                .testTag("mark_for_review_btn")
        ) {
            Icon(
                if (isMarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (isMarked) "Marked • Next" else "Mark & Next",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Previous
        OutlinedButton(
            onClick = onPrevious,
            enabled = currentIdx > 0,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0x40FFFFFF)),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(0.7f)
                .height(38.dp)
                .testTag("prev_question_btn")
        ) {
            Icon(Icons.Filled.ChevronLeft, null, modifier = Modifier.size(16.dp))
            Text("Prev", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
        }

        // Save & Next or Submit
        if (currentIdx == total - 1) {
            Button(
                onClick = onSubmitConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldSuccess,
                    contentColor = Color(0xFF050814)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(38.dp)
                    .testTag("submit_test_primary_btn")
            ) {
                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Submit Test", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Button(
                onClick = onSaveAndNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF050814)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(38.dp)
                    .testTag("next_question_btn")
            ) {
                Text("Save & Next", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PaletteLegendSummary(state: ActiveTestState) {
    val total = state.questions.size
    val answeredCount = state.selectedAnswers.size
    val markedCount = state.markedForReview.size
    val visitedCount = state.visitedQuestions.size
    val unattemptedCount = (visitedCount - answeredCount).coerceAtLeast(0)
    val notVisitedCount = (total - visitedCount).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x15FFFFFF))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(color = EmeraldSuccess, label = "Ans ($answeredCount)")
            LegendItem(color = CoralRose, label = "Unans ($unattemptedCount)")
            LegendItem(color = Color(0xFF64748B), label = "Not Vis ($notVisitedCount)")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(color = Color(0xFF8B5CF6), label = "Marked ($markedCount)")
            LegendItem(color = GoldenSpark, label = "Ans & Mark")
        }
    }
}

@Composable
private fun PaletteItemButton(
    idx: Int,
    total: Int,
    state: ActiveTestState,
    onSelectQuestion: (Int) -> Unit
) {
    val isCurrent = state.currentQuestionIndex == idx
    val isAnswered = state.selectedAnswers.containsKey(idx)
    val isMarked = state.markedForReview.contains(idx)
    val isVisited = state.visitedQuestions.contains(idx)

    val statusText = when {
        isAnswered && isMarked -> "Answered and Marked for Review"
        isAnswered -> "Answered"
        isMarked -> "Marked for Review"
        isVisited -> "Not Answered"
        else -> "Not Visited"
    }

    val (bgColor, textColor) = when {
        isAnswered && isMarked -> Pair(GoldenSpark, Color(0xFF050814))
        isAnswered -> Pair(EmeraldSuccess, Color(0xFF050814))
        isMarked -> Pair(Color(0xFF8B5CF6), Color.White)
        isVisited -> Pair(CoralRose, Color.White)
        else -> Pair(Color(0x2564748B), Color(0xFFCBD5E1))
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor.copy(alpha = if (isCurrent) 1f else 0.88f))
            .border(
                width = if (isCurrent) 2.5.dp else 1.dp,
                color = if (isCurrent) NeonCyan else Color(0x30FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .springClickable(testTag = "palette_item_$idx") { onSelectQuestion(idx) }
            .semantics {
                contentDescription = "Question ${idx + 1} of $total, $statusText"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${idx + 1}",
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SubmissionErrorBanner(error: String, onRetry: () -> Unit) {
    Surface(
        color = CoralRose,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .testTag("submission_error_banner")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Submission Error", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text(error, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
            }
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = CoralRose),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.testTag("retry_submission_btn")
            ) {
                Text("Retry", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
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
            if (yearOrTag.isNotBlank()) "Authentic PYQ • $yearOrTag" else "Previous Year Question"
        )
        QuestionSource.PRACTICE -> Triple(
            ElectricViolet,
            Icons.Filled.EditNote,
            label.ifBlank { "Chapter Practice" }
        )
        QuestionSource.AI_GENERATED -> Triple(
            EmeraldSuccess,
            Icons.Filled.AutoAwesome,
            label.ifBlank { "AI Exam Drill" }
        )
        QuestionSource.VERIFIED_PREVIOUS_YEAR -> Triple(
            GoldenSpark,
            Icons.Filled.Verified,
            if (yearOrTag.isNotBlank()) "Official PYQ • $yearOrTag" else "Verified Official PYQ"
        )
        QuestionSource.USER_PROVIDED -> Triple(
            NeonCyan,
            Icons.Filled.Description,
            label.ifBlank { "User Material" }
        )
        QuestionSource.CURRENT_AFFAIRS -> Triple(
            Color(0xFF38BDF8),
            Icons.Filled.Article,
            "Current Affairs • 2024"
        )
        QuestionSource.CHAPTER_PRACTICE -> Triple(
            ElectricViolet,
            Icons.Filled.MenuBook,
            label.ifBlank { "Chapter Practice" }
        )
        QuestionSource.EXAM_PATTERN -> Triple(
            Color(0xFFA855F7),
            Icons.Filled.Architecture,
            label.ifBlank { "Exam Pattern" }
        )
        QuestionSource.MIXED -> Triple(
            NeonCyan,
            Icons.Filled.Tune,
            label.ifBlank { "Mixed Practice" }
        )
        else -> Triple(
            Color(0xFF94A3B8),
            Icons.Filled.HelpOutline,
            label.ifBlank { "Standard Question" }
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
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(text = "$count", color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD0050814))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(22.dp),
                fillAlpha = 0.96f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Question Palette",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Standard Real-Exam CBT Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, null, tint = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PaletteLegendSummary(state = state)

                    HorizontalDivider(color = Color(0x20FFFFFF), modifier = Modifier.padding(vertical = 10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(state.questions) { idx, _ ->
                            PaletteItemButton(
                                idx = idx,
                                total = state.questions.size,
                                state = state,
                                onSelectQuestion = onSelectQuestion
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassButton(
                        text = "Submit Test Now",
                        onClick = onSubmitTestPrompt,
                        isPrimary = true
                    )
                }
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
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
    }
}


package com.example.ui.screens.nova

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Question
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaScreenTab
import com.example.viewmodel.NovaViewModel
import com.example.viewmodel.QuizScreenStage

@Composable
fun NovaQuizIntelligenceTab(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val quizState by viewModel.quizState.collectAsState()
    val studyContext by viewModel.studyContext.collectAsState()

    var showSubmitConfirmDialog by remember { mutableStateOf(false) }
    var showPaletteDrawer by remember { mutableStateOf(false) }
    var selectedReviewFilter by remember { mutableStateOf("All") } // "All", "Incorrect", "Correct", "Unanswered"

    Box(modifier = modifier.fillMaxSize()) {
        when (quizState.screenStage) {
            QuizScreenStage.CONFIGURING -> {
                QuizConfigurationScreen(
                    state = quizState,
                    targetExam = studyContext.targetExam,
                    onExamChange = { viewModel.updateQuizExam(it) },
                    onSubjectChange = { viewModel.updateQuizSubject(it) },
                    onTopicChange = { viewModel.updateQuizTopic(it) },
                    onLanguageChange = { viewModel.updateQuizLanguage(it) },
                    onCountChange = { viewModel.updateQuizQuestionCount(it) },
                    onDurationChange = { mins, isCustom -> viewModel.updateQuizDuration(mins, isCustom) },
                    onDifficultyChange = { viewModel.updateQuizDifficulty(it) },
                    onModeChange = { viewModel.updateQuizMode(it) },
                    onProceedToBriefing = { viewModel.prepareQuizBriefing() }
                )
            }

            QuizScreenStage.BRIEFING -> {
                QuizBriefingScreen(
                    state = quizState,
                    onBackToConfig = { viewModel.backToQuizConfig() },
                    onStartTest = { viewModel.startGeneratedQuiz() }
                )
            }

            QuizScreenStage.ACTIVE -> {
                if (quizState.isGenerating) {
                    QuizGeneratingScreen(
                        status = quizState.generationStatus.ifBlank { "NOVA AI is generating exam-grounded questions..." }
                    )
                } else if (quizState.questions.isNotEmpty()) {
                    ActiveQuizScreen(
                        state = quizState,
                        showPalette = showPaletteDrawer,
                        onTogglePalette = { showPaletteDrawer = !showPaletteDrawer },
                        onSelectOption = { viewModel.selectQuizOption(it) },
                        onToggleReview = { viewModel.toggleMarkForReview() },
                        onCheckImmediate = { viewModel.checkImmediateAnswer() },
                        onNext = { viewModel.nextQuizQuestion() },
                        onPrev = { viewModel.previousQuizQuestion() },
                        onJumpToQuestion = {
                            viewModel.jumpToQuestion(it)
                            showPaletteDrawer = false
                        },
                        onRequestSubmit = { showSubmitConfirmDialog = true }
                    )
                } else {
                    QuizGeneratingScreen(
                        status = "Preparing your test environment..."
                    )
                }
            }

            QuizScreenStage.FINISHED -> {
                QuizFinishedScreen(
                    state = quizState,
                    selectedFilter = selectedReviewFilter,
                    onFilterChange = { selectedReviewFilter = it },
                    onRestart = { viewModel.restartQuizSession() },
                    onPracticeWeakTopics = { viewModel.practiceWeakTopicsQuiz() },
                    onSaveMistake = { q, ans -> viewModel.saveMistakeToNotebook(q, ans) },
                    onSaveFlashcard = { q -> viewModel.saveQuestionAsFlashcard(q) },
                    onAskNova = { q -> viewModel.askNovaAboutQuestion(q) }
                )
            }
        }

        // Submit confirmation dialog
        if (showSubmitConfirmDialog) {
            val answeredCount = quizState.userAnswers.size
            val unansweredCount = quizState.questions.size - answeredCount
            val markedCount = quizState.markedForReview.size

            AlertDialog(
                onDismissRequest = { showSubmitConfirmDialog = false },
                containerColor = DarkSurfaceElevated,
                titleContentColor = Color.White,
                textContentColor = TextSecondary,
                icon = {
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = AmberGold, modifier = Modifier.size(32.dp))
                },
                title = {
                    Text("Submit Practice Test?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Are you sure you want to finish and submit your test?", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Questions:", fontSize = 13.sp, color = TextSecondary)
                            Text("${quizState.questions.size}", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Answered:", fontSize = 13.sp, color = EmeraldGreen)
                            Text("$answeredCount", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Unanswered:", fontSize = 13.sp, color = if (unansweredCount > 0) CoralPink else TextSecondary)
                            Text("$unansweredCount", fontWeight = FontWeight.Bold, color = if (unansweredCount > 0) CoralPink else TextSecondary)
                        }
                        if (markedCount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Marked for Review:", fontSize = 13.sp, color = AmberGold)
                                Text("$markedCount", fontWeight = FontWeight.Bold, color = AmberGold)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSubmitConfirmDialog = false
                            viewModel.submitQuizSession()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Yes, Submit", fontWeight = FontWeight.Bold, color = DarkCanvas)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSubmitConfirmDialog = false }) {
                        Text("Continue Test", color = TextSecondary)
                    }
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// STAGE 1: CONFIGURATION SCREEN
// -----------------------------------------------------------------------------

@Composable
private fun QuizConfigurationScreen(
    state: com.example.viewmodel.InteractiveQuizState,
    targetExam: String,
    onExamChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onCountChange: (Int) -> Unit,
    onDurationChange: (Int, Boolean) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onProceedToBriefing: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Premium Hero Header ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f),
            borderColor = AmberGold.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "NOVA QUIZ INTELLIGENCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Create Your Practice Test",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Syllabus-aware AI generation calibrated to your target exam",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeonCyan.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TARGET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        Text(
                            text = if (state.selectedExam.isNotBlank()) state.selectedExam else targetExam,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // --- Card 1: Exam Context & Quiz Mode ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.85f),
            borderColor = Color.White.copy(alpha = 0.12f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Quiz Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))

                val modes = listOf(
                    Triple("Practice", "📝 Immediate feedback & explanation per question", EmeraldGreen),
                    Triple("Mock Test", "🧪 Authentic full exam simulation with timer", NeonCyan),
                    Triple("Previous-Year Style", "🏛️ Pattern-grounded previous-year style MCQs", AmberGold),
                    Triple("Current Affairs", "📰 Grounded in verified recent national & global events", ElectricIndigo),
                    Triple("Revision", "🔄 Focus on weak topics & past mistakes", CoralPink)
                )

                modes.forEach { (modeTitle, modeDesc, modeColor) ->
                    val isSelected = state.questionMode == modeTitle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) modeColor.copy(alpha = 0.15f) else DarkSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) modeColor else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .springClickable { onModeChange(modeTitle) }
                            .testTag("quiz_mode_$modeTitle")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onModeChange(modeTitle) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = modeColor,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = modeTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) modeColor else Color.White
                                )
                                Text(
                                    text = modeDesc,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Card 2: Subject & Topic Selection ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.85f),
            borderColor = Color.White.copy(alpha = 0.12f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Subject & Topic", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        text = state.selectedExam.ifBlank { targetExam },
                        fontSize = 11.sp,
                        color = AmberGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Subject Scope", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                val availableSubjects = if (state.availableSubjects.isNotEmpty()) state.availableSubjects else listOf("All Subjects", "General Awareness", "Mathematics", "Reasoning", "General Science")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableSubjects.size) { idx ->
                        val sub = availableSubjects[idx]
                        val isSelected = state.subject == sub
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSubjectChange(sub) },
                            label = { Text(sub, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGold.copy(alpha = 0.2f),
                                selectedLabelColor = AmberGold,
                                containerColor = DarkSurfaceElevated,
                                labelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.White.copy(alpha = 0.1f),
                                selectedBorderColor = AmberGold
                            ),
                            modifier = Modifier.testTag("subject_chip_$sub")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Topic / Chapter Focus", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                val availableTopics = if (state.availableTopics.isNotEmpty()) state.availableTopics else listOf("All Topics", "Core Fundamentals", "Important Formulas", "Previous Trends")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableTopics.size) { idx ->
                        val top = availableTopics[idx]
                        val isSelected = state.topic == top
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTopicChange(top) },
                            label = { Text(top, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricIndigo.copy(alpha = 0.25f),
                                selectedLabelColor = NeonCyan,
                                containerColor = DarkSurfaceElevated,
                                labelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.White.copy(alpha = 0.1f),
                                selectedBorderColor = NeonCyan
                            ),
                            modifier = Modifier.testTag("topic_chip_$top")
                        )
                    }
                }
            }
        }

        // --- Card 3: Language & Difficulty ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.85f),
            borderColor = Color.White.copy(alpha = 0.12f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Language
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Language / भाषा", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("English", "हिंदी (Hindi)").forEach { lang ->
                        val isSelected = (lang == "English" && state.language.equals("English", ignoreCase = true)) ||
                                (lang.startsWith("हिंदी") && (state.language.contains("हिंदी", ignoreCase = true) || state.language.contains("Hindi", ignoreCase = true)))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldGreen.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) EmeraldGreen else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .weight(1f)
                                .springClickable {
                                    onLanguageChange(if (lang.startsWith("हिंदी")) "हिंदी" else "English")
                                }
                                .testTag("lang_select_$lang")
                        ) {
                            Text(
                                text = lang,
                                textAlign = TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) EmeraldGreen else Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Difficulty
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = CoralPink, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Difficulty Level", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Easy", "Medium", "Hard", "Mixed").forEach { diff ->
                        val isSelected = state.difficulty == diff
                        val color = when (diff) {
                            "Easy" -> EmeraldGreen
                            "Medium" -> AmberGold
                            "Hard" -> CoralPink
                            else -> ElectricIndigo
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) color.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) color else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .weight(1f)
                                .springClickable { onDifficultyChange(diff) }
                                .testTag("diff_select_$diff")
                        ) {
                            Text(
                                text = diff,
                                textAlign = TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) color else Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- Card 4: Question Count & Duration ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.85f),
            borderColor = Color.White.copy(alpha = 0.12f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Question Count & Duration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        text = "${state.durationMinutes} mins",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val countOptions = listOf(
                        10 to 12,
                        20 to 25,
                        50 to 60,
                        100 to 90
                    )
                    countOptions.forEach { (count, defaultMins) ->
                        val isSelected = state.questionCount == count
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .weight(1f)
                                .springClickable {
                                    onCountChange(count)
                                }
                                .testTag("count_select_$count")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$count Qs",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) NeonCyan else Color.White
                                )
                                Text(
                                    text = "${defaultMins}m",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- CTA Button: Proceed to Briefing ---
        Button(
            onClick = onProceedToBriefing,
            colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("generate_quiz_cta_button")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DarkCanvas, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Generate Practice Test (${state.questionCount} Questions)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = DarkCanvas
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// -----------------------------------------------------------------------------
// STAGE 2: PRE-TEST BRIEFING SCREEN
// -----------------------------------------------------------------------------

@Composable
private fun QuizBriefingScreen(
    state: com.example.viewmodel.InteractiveQuizState,
    onBackToConfig: () -> Unit,
    onStartTest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.9f),
            borderColor = NeonCyan.copy(alpha = 0.4f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = NeonCyan.copy(alpha = 0.15f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Test Briefing & Instructions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = state.selectedExam,
                    fontSize = 13.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Parameter Summary Grid
                val params = listOf(
                    "Subject & Topic" to "${state.subject} • ${state.topic}",
                    "Quiz Mode" to state.questionMode,
                    "Total Questions" to "${state.questionCount} Questions",
                    "Allotted Time" to "${state.durationMinutes} Minutes",
                    "Difficulty" to state.difficulty,
                    "Language" to state.language,
                    "Marking Scheme" to state.markingScheme
                )

                params.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 12.sp, color = TextSecondary)
                        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }

        // Instructions Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.85f),
            borderColor = Color.White.copy(alpha = 0.1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Important Guidelines", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                Spacer(modifier = Modifier.height(8.dp))
                val tips = when (state.questionMode) {
                    "Practice" -> listOf(
                        "Instant feedback is active: click 'Check Answer' to view explanations immediately.",
                        "Take your time to understand underlying concepts before moving to the next question."
                    )
                    "Mock Test" -> listOf(
                        "Timed authentic exam environment: timer runs automatically.",
                        "Answers will be submitted together at the end. Use question palette to navigate.",
                        "You can bookmark questions with 'Mark for Review' to check before final submission."
                    )
                    else -> listOf(
                        "Questions are calibrated to official pattern and syllabus standards.",
                        "Review explanations after finishing to identify conceptual gaps."
                    )
                }

                tips.forEach { tip ->
                    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                        Text("• ", color = AmberGold, fontWeight = FontWeight.Bold)
                        Text(tip, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBackToConfig,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Modify Setup", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onStartTest,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(50.dp)
                    .testTag("start_test_now_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkCanvas, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Test Now", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkCanvas)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// STAGE 3: LIVE ACTIVE TEST TAKING SCREEN
// -----------------------------------------------------------------------------

@Composable
private fun ActiveQuizScreen(
    state: com.example.viewmodel.InteractiveQuizState,
    showPalette: Boolean,
    onTogglePalette: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onToggleReview: () -> Unit,
    onCheckImmediate: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onJumpToQuestion: (Int) -> Unit,
    onRequestSubmit: () -> Unit
) {
    val currentQIndex = state.currentIndex
    val currentQuestion = state.questions.getOrNull(currentQIndex) ?: return
    val totalCount = state.questions.size
    val selectedOpt = state.userAnswers[currentQIndex]
    val isMarked = state.markedForReview.contains(currentQIndex)
    val isChecked = state.immediateChecked[currentQIndex] == true

    val minsLeft = state.timeRemainingSeconds / 60
    val secsLeft = state.timeRemainingSeconds % 60
    val timeFormatted = "%02d:%02d".format(minsLeft, secsLeft)
    val isLowTime = state.timeRemainingSeconds in 1..120

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Top Status HUD ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.9f),
            borderColor = if (isLowTime) CoralPink else NeonCyan.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Question ${currentQIndex + 1} of $totalCount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${currentQuestion.subject} • ${currentQuestion.topic}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Timer Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLowTime) CoralPink.copy(alpha = 0.2f) else DarkSurface,
                        border = BorderStroke(1.dp, if (isLowTime) CoralPink else Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (isLowTime) CoralPink else NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeFormatted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLowTime) CoralPink else Color.White
                            )
                        }
                    }

                    // Palette toggle
                    IconButton(
                        onClick = onTogglePalette,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "Question Palette",
                            tint = if (showPalette) NeonCyan else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Mark for review
                    IconButton(
                        onClick = onToggleReview,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Mark for review",
                            tint = if (isMarked) AmberGold else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // --- Question Palette Drawer (Collapsible) ---
        AnimatedVisibility(visible = showPalette) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurface.copy(alpha = 0.95f),
                borderColor = NeonCyan.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Question Palette", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(totalCount) { idx ->
                            val isCurrent = idx == currentQIndex
                            val isAns = state.userAnswers.containsKey(idx)
                            val isM = state.markedForReview.contains(idx)

                            val btnBg = when {
                                isCurrent -> NeonCyan
                                isAns && isM -> ElectricIndigo
                                isAns -> EmeraldGreen.copy(alpha = 0.8f)
                                isM -> AmberGold.copy(alpha = 0.8f)
                                else -> DarkSurfaceElevated
                            }

                            val textColor = if (isCurrent) DarkCanvas else Color.White

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = btnBg,
                                modifier = Modifier
                                    .size(36.dp)
                                    .springClickable { onJumpToQuestion(idx) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("🟩 Answered", fontSize = 10.sp, color = EmeraldGreen)
                        Text("🟨 Marked", fontSize = 10.sp, color = AmberGold)
                        Text("⚪ Unattempted", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }

        // --- Question Content Card ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.9f),
            borderColor = Color.White.copy(alpha = 0.12f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElectricIndigo.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = currentQuestion.difficulty.ifBlank { "Standard" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (isMarked) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberGold.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = AmberGold, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Marked for Review", fontSize = 10.sp, color = AmberGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Question text
                Text(
                    text = currentQuestion.questionText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4 Options
                currentQuestion.options.forEachIndexed { optIndex, optText ->
                    val isThisSelected = selectedOpt == optIndex
                    val isCorrect = optIndex == currentQuestion.correctOptionIndex

                    val optBg = when {
                        isChecked && isCorrect -> EmeraldGreen.copy(alpha = 0.25f)
                        isChecked && isThisSelected && !isCorrect -> CoralPink.copy(alpha = 0.25f)
                        isThisSelected -> ElectricIndigo.copy(alpha = 0.35f)
                        else -> DarkSurfaceElevated
                    }

                    val optBorder = when {
                        isChecked && isCorrect -> EmeraldGreen
                        isChecked && isThisSelected && !isCorrect -> CoralPink
                        isThisSelected -> NeonCyan
                        else -> Color.White.copy(alpha = 0.08f)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = optBg,
                        border = BorderStroke(1.dp, optBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .springClickable { onSelectOption(optIndex) }
                            .testTag("quiz_option_${optIndex}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isThisSelected) NeonCyan else Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${('A' + optIndex)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isThisSelected) DarkCanvas else Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = optText,
                                fontSize = 13.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Instant Practice Mode Concept Explanation Card
                if (isChecked && currentQuestion.explanation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Concept Explanation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentQuestion.explanation,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // --- Bottom Navigation Toolbar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            OutlinedButton(
                onClick = onPrev,
                enabled = currentQIndex > 0,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text("← Prev", color = if (currentQIndex > 0) Color.White else TextSecondary, fontSize = 13.sp)
            }

            // In Practice Mode: Check Answer Button
            if (state.questionMode == "Practice" && !isChecked) {
                Button(
                    onClick = onCheckImmediate,
                    enabled = selectedOpt != null,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                ) {
                    Text("Check Answer", fontWeight = FontWeight.Bold, color = DarkCanvas, fontSize = 13.sp)
                }
            }

            // Next Question / Finish Test
            if (currentQIndex + 1 < totalCount) {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("quiz_next_button")
                ) {
                    Text("Next →", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }
            } else {
                Button(
                    onClick = onRequestSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(46.dp)
                        .testTag("quiz_finish_button")
                ) {
                    Text("Submit Test 🎉", fontWeight = FontWeight.Bold, color = DarkCanvas, fontSize = 13.sp)
                }
            }
        }

        // Final Submit bar at bottom for Mock Test mode
        if (state.questionMode == "Mock Test") {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onRequestSubmit,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Finish & Submit Entire Test", color = AmberGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// -----------------------------------------------------------------------------
// STAGE 4: COMPREHENSIVE POST-TEST RESULT & AI DIAGNOSTIC
// -----------------------------------------------------------------------------

@Composable
private fun QuizFinishedScreen(
    state: com.example.viewmodel.InteractiveQuizState,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onRestart: () -> Unit,
    onPracticeWeakTopics: () -> Unit,
    onSaveMistake: (Question, String) -> Unit,
    onSaveFlashcard: (Question) -> Unit,
    onAskNova: (Question) -> Unit
) {
    val accuracy = state.accuracyPercent.toInt()
    val isPassing = accuracy >= 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Score Hero Card ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.9f),
            borderColor = if (isPassing) EmeraldGreen.copy(alpha = 0.5f) else AmberGold.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (accuracy >= 80) "🏆 Outstanding Mastery!" else if (accuracy >= 50) "🎯 Solid Effort!" else "📚 Revision Needed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPassing) EmeraldGreen else AmberGold
                )
                Text(
                    text = "${state.selectedExam} • ${state.subject}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SCORE", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${"%.1f".format(state.earnedMarks)} / ${"%.0f".format(state.maxMarks)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (isPassing) EmeraldGreen.copy(alpha = 0.15f) else AmberGold.copy(alpha = 0.15f))
                            .border(2.dp, if (isPassing) EmeraldGreen else AmberGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$accuracy%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Accuracy", fontSize = 8.sp, color = TextSecondary)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TIME", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        val m = state.timeSpentSeconds / 60
                        val s = state.timeSpentSeconds % 60
                        Text(
                            text = "${m}m ${s}s",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("✓ Correct: ${state.correctCount}", fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                    Text("✗ Incorrect: ${state.incorrectCount}", fontSize = 12.sp, color = CoralPink, fontWeight = FontWeight.SemiBold)
                    Text("— Skipped: ${state.unansweredCount}", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // --- NOVA AI Diagnostic Analysis Card ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.85f),
            borderColor = AmberGold.copy(alpha = 0.35f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("NOVA AI DIAGNOSTIC REPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberGold, letterSpacing = 1.sp)
                    }
                    if (state.isAnalyzingResult) {
                        CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (state.isAnalyzingResult) {
                    Text("Analyzing concept patterns & identifying weak spots...", fontSize = 12.sp, color = TextSecondary)
                } else if (state.novaAiAnalysis.isNotBlank()) {
                    Text(
                        text = state.novaAiAnalysis,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = 19.sp
                    )
                } else {
                    Text(
                        text = "Great job finishing your practice test! Review incorrect questions below and practice weak topics.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                // If weak topics identified, show one-tap action
                if (state.weakTopicsIdentified.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onPracticeWeakTopics,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = DarkCanvas, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Practice Weak Topics (${state.weakTopicsIdentified.take(2).joinToString(", ")})", fontWeight = FontWeight.Bold, color = DarkCanvas, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- Question by Question Review Header & Filter ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.85f),
            borderColor = Color.White.copy(alpha = 0.1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Question Review & Solutions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Incorrect", "Correct", "Unanswered").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ElectricIndigo else DarkSurfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .springClickable { onFilterChange(filter) }
                        ) {
                            Text(
                                text = filter,
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Filtered questions list
        val filteredQuestions = state.questions.filterIndexed { idx, q ->
            val userAns = state.userAnswers[idx]
            when (selectedFilter) {
                "Incorrect" -> userAns != null && userAns != q.correctOptionIndex
                "Correct" -> userAns == q.correctOptionIndex
                "Unanswered" -> userAns == null
                else -> true
            }
        }

        filteredQuestions.forEachIndexed { displayIdx, question ->
            val origIndex = state.questions.indexOf(question)
            val userAns = state.userAnswers[origIndex]
            val isCorrect = userAns == question.correctOptionIndex
            val isSkipped = userAns == null

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurface.copy(alpha = 0.85f),
                borderColor = when {
                    isCorrect -> EmeraldGreen.copy(alpha = 0.3f)
                    isSkipped -> Color.White.copy(alpha = 0.1f)
                    else -> CoralPink.copy(alpha = 0.3f)
                }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Q${origIndex + 1} • ${question.subject}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                isCorrect -> EmeraldGreen.copy(alpha = 0.2f)
                                isSkipped -> Color.White.copy(alpha = 0.1f)
                                else -> CoralPink.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = when {
                                    isCorrect -> "✓ Correct (+1.0)"
                                    isSkipped -> "— Skipped (0.0)"
                                    else -> "✗ Incorrect (-0.25)"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isCorrect -> EmeraldGreen
                                    isSkipped -> TextSecondary
                                    else -> CoralPink
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = question.questionText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Options list
                    question.options.forEachIndexed { optIdx, optText ->
                        val isUserPick = userAns == optIdx
                        val isRightAnswer = optIdx == question.correctOptionIndex

                        val optColor = when {
                            isRightAnswer -> EmeraldGreen
                            isUserPick -> CoralPink
                            else -> TextSecondary
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${('A' + optIdx)}. ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = optColor
                            )
                            Text(
                                text = optText,
                                fontSize = 12.sp,
                                color = if (isRightAnswer || isUserPick) Color.White else TextSecondary
                            )
                            if (isRightAnswer) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("(Correct)", fontSize = 10.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            } else if (isUserPick) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("(Your Answer)", fontSize = 10.sp, color = CoralPink, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Explanation
                    if (question.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Explanation:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(question.explanation, fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSaveMistake(question, question.options.getOrNull(userAns ?: -1) ?: "No answer") },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📖 Mistake", fontSize = 11.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { onSaveFlashcard(question) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🎴 Flashcard", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = { onAskNova(question) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("🤖 Ask NOVA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // --- Bottom Actions ---
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onRestart,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("practice_another_quiz_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkCanvas, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Practice Another Test", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkCanvas)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// -----------------------------------------------------------------------------
// LOADING / GENERATING SCREEN
// -----------------------------------------------------------------------------

@Composable
private fun QuizGeneratingScreen(status: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.9f),
            borderColor = NeonCyan.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = NeonCyan,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Generating Quiz Intelligence",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = status,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

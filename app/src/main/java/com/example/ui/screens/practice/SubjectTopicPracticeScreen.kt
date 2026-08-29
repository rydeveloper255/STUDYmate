package com.example.ui.screens.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.model.MockTestAttempt
import com.example.data.model.MockTestType
import com.example.data.model.Question
import com.example.data.model.UserProfile
import com.example.data.remote.supabase.SupabaseQuestionBankService
import com.example.localization.GlobalLanguageSwitcher
import com.example.localization.appString
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Step 68: Practice Feature 1 — Subject & Topic Practice Screen
 *
 * Dedicated page for chapter-wise, topic-wise, and difficulty-based practice drills:
 * - Selection: Exam, Subject, Chapter, Topic, Difficulty (Easy, Medium, Hard, Mixed), Question Count (10, 20, 30, 50)
 * - Start Practice
 * - Active Question Player with Timer, Counter, Option Selection, Previous/Next, Submit
 * - Detailed Result Card with Score, Accuracy, Time Taken, Subject/Chapter Performance, Step-by-Step Solutions
 * - Cache-First Supabase Data Source
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectTopicPracticeScreen(
    user: UserProfile?,
    initialSubject: String? = null,
    initialChapter: String? = null,
    initialTopic: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val questionService = remember { SupabaseQuestionBankService.instance }

    val defaultExam = user?.examName?.ifBlank { "SSC CGL" } ?: "SSC CGL"
    val userSubjects = user?.subjects?.filter { it.isNotBlank() }?.ifEmpty {
        listOf("Mathematics", "Reasoning & Logic", "General Science", "General Awareness", "English")
    } ?: listOf("Mathematics", "Reasoning & Logic", "General Science", "General Awareness", "English")

    var selectedExam by remember { mutableStateOf(defaultExam) }
    var selectedSubject by remember { mutableStateOf(initialSubject ?: userSubjects.firstOrNull() ?: "Mathematics") }
    var selectedChapter by remember { mutableStateOf(initialChapter ?: "All Chapters") }
    var selectedTopic by remember { mutableStateOf(initialTopic ?: "All Topics") }
    var selectedDifficulty by remember { mutableStateOf("Mixed") }
    var selectedQuestionCount by remember { mutableIntStateOf(10) }

    // Session State: 0 = Setup / Config, 1 = In Practice, 2 = Results
    var sessionState by remember { mutableIntStateOf(0) }
    var isLoadingQuestions by remember { mutableStateOf(false) }
    var activeQuestions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Mock chapters map
    val subjectChapters = remember(selectedSubject) {
        when (selectedSubject.lowercase()) {
            "mathematics", "quantitative aptitude" -> listOf(
                "All Chapters", "Number System & HCF/LCM", "Percentages & Profit-Loss",
                "Ratio & Proportion", "Time & Work", "Speed, Distance & Time",
                "Simple & Compound Interest", "Algebra & Geometry", "Trigonometry & Mensuration"
            )
            "reasoning & logic", "general intelligence & reasoning", "reasoning" -> listOf(
                "All Chapters", "Analogies & Classification", "Coding-Decoding",
                "Blood Relations", "Syllogism & Venn Diagrams", "Direction Sense",
                "Number & Letter Series", "Non-Verbal & Pattern Folding"
            )
            "general science" -> listOf(
                "All Chapters", "Physics: Mechanics & Units", "Physics: Optics & Electricity",
                "Chemistry: Periodic Table & Acids", "Biology: Cell Biology & Human Physiology",
                "Biology: Nutrition & Diseases", "Environment & Ecology"
            )
            else -> listOf(
                "All Chapters", "Indian Polity & Constitution", "Modern Indian History",
                "Physical & Indian Geography", "Indian Economy & Budget", "Static GK & National Awards"
            )
        }
    }

    // Timer coroutine
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    BackHandler(enabled = true) {
        if (sessionState == 1) {
            // Confirm exit practice
            sessionState = 0
            isTimerRunning = false
        } else if (sessionState == 2) {
            sessionState = 0
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (sessionState) {
                                1 -> "Active Practice Drill"
                                2 -> "Practice Scorecard"
                                else -> "Subject & Topic Practice"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (sessionState == 1) "$selectedSubject • $selectedDifficulty" else selectedExam,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (sessionState != 0) sessionState = 0 else onBack()
                        },
                        modifier = Modifier.testTag("btn_practice_back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    GlobalLanguageSwitcher()
                    if (sessionState == 1) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (sessionState) {
                0 -> {
                    // CONFIG / SELECTION VIEW
                    PracticeConfigView(
                        selectedExam = selectedExam,
                        onSelectExam = { selectedExam = it },
                        userSubjects = userSubjects,
                        selectedSubject = selectedSubject,
                        onSelectSubject = {
                            selectedSubject = it
                            selectedChapter = "All Chapters"
                            selectedTopic = "All Topics"
                        },
                        chapters = subjectChapters,
                        selectedChapter = selectedChapter,
                        onSelectChapter = { selectedChapter = it },
                        selectedDifficulty = selectedDifficulty,
                        onSelectDifficulty = { selectedDifficulty = it },
                        selectedCount = selectedQuestionCount,
                        onSelectCount = { selectedQuestionCount = it },
                        isLoading = isLoadingQuestions,
                        onStartPractice = {
                            isLoadingQuestions = true
                            coroutineScope.launch {
                                val questions = questionService.getQuestionsForPractice(
                                    examName = selectedExam,
                                    subject = selectedSubject,
                                    chapter = if (selectedChapter == "All Chapters") "" else selectedChapter,
                                    topic = if (selectedTopic == "All Topics") "" else selectedTopic,
                                    difficulty = selectedDifficulty,
                                    count = selectedQuestionCount
                                )
                                activeQuestions = questions
                                selectedAnswers.clear()
                                currentQuestionIndex = 0
                                elapsedSeconds = 0
                                isLoadingQuestions = false
                                isTimerRunning = true
                                sessionState = 1
                            }
                        }
                    )
                }
                1 -> {
                    // ACTIVE QUESTION SESSION
                    if (activeQuestions.isNotEmpty()) {
                        val currentQ = activeQuestions.getOrNull(currentQuestionIndex) ?: activeQuestions.first()
                        ActiveQuestionPlayer(
                            question = currentQ,
                            currentIndex = currentQuestionIndex,
                            totalQuestions = activeQuestions.size,
                            selectedOption = selectedAnswers[currentQuestionIndex],
                            onSelectOption = { optIdx ->
                                selectedAnswers[currentQuestionIndex] = optIdx
                            },
                            onNext = {
                                if (currentQuestionIndex < activeQuestions.size - 1) {
                                    currentQuestionIndex++
                                }
                            },
                            onPrevious = {
                                if (currentQuestionIndex > 0) {
                                    currentQuestionIndex--
                                }
                            },
                            onSubmit = {
                                isTimerRunning = false
                                sessionState = 2
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No questions found. Please adjust filters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                2 -> {
                    // RESULT VIEW
                    PracticeScorecardView(
                        questions = activeQuestions,
                        selectedAnswers = selectedAnswers,
                        elapsedSeconds = elapsedSeconds,
                        subject = selectedSubject,
                        chapter = selectedChapter,
                        difficulty = selectedDifficulty,
                        onRetry = {
                            selectedAnswers.clear()
                            currentQuestionIndex = 0
                            elapsedSeconds = 0
                            isTimerRunning = true
                            sessionState = 1
                        },
                        onNewPractice = {
                            sessionState = 0
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeConfigView(
    selectedExam: String,
    onSelectExam: (String) -> Unit,
    userSubjects: List<String>,
    selectedSubject: String,
    onSelectSubject: (String) -> Unit,
    chapters: List<String>,
    selectedChapter: String,
    onSelectChapter: (String) -> Unit,
    selectedDifficulty: String,
    onSelectDifficulty: (String) -> Unit,
    selectedCount: Int,
    onSelectCount: (Int) -> Unit,
    isLoading: Boolean,
    onStartPractice: () -> Unit
) {
    val difficulties = listOf("Easy", "Medium", "Hard", "Mixed")
    val questionCounts = listOf(10, 20, 30, 50)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Quiz,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom Practice Drill",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Supabase verified question bank with step-by-step conceptual solutions.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 1: Subject Selection
        item {
            Text(
                text = "1. Select Subject",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(userSubjects) { subj ->
                    val isSelected = subj == selectedSubject
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSubject(subj) },
                        label = { Text(subj, fontSize = 13.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("chip_subject_${subj.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }

        // Section 2: Chapter Selection
        item {
            Text(
                text = "2. Select Chapter / Unit",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chapters) { chap ->
                    val isSelected = chap == selectedChapter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectChapter(chap) },
                        label = { Text(chap, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }
            }
        }

        // Section 3: Difficulty
        item {
            Text(
                text = "3. Select Difficulty",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                difficulties.forEach { diff ->
                    val isSelected = diff == selectedDifficulty
                    OutlinedButton(
                        onClick = { onSelectDifficulty(diff) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(diff, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        // Section 4: Question Count
        item {
            Text(
                text = "4. Number of Questions",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                questionCounts.forEach { count ->
                    val isSelected = count == selectedCount
                    OutlinedButton(
                        onClick = { onSelectCount(count) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text("$count Qs", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        // Start Practice Button
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartPractice,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_start_practice_session"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Fetching Question Bank...", fontSize = 15.sp)
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Practice ($selectedCount Questions)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun ActiveQuestionPlayer(
    question: Question,
    currentIndex: Int,
    totalQuestions: Int,
    selectedOption: Int?,
    onSelectOption: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit
) {
    val progress = (currentIndex + 1).toFloat() / totalQuestions.coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress Bar & Counter
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentIndex + 1} of $totalQuestions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = question.difficulty,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // Question Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (question.chapter.isNotBlank()) {
                        Text(
                            text = "${question.subject} • ${question.chapter}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = question.questionText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 4 Options
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { optIdx, optText ->
                    val isSelected = selectedOption == optIdx
                    val optionLabels = listOf("A", "B", "C", "D")
                    val label = optionLabels.getOrNull(optIdx) ?: "${optIdx + 1}"

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .springClickable(testTag = "opt_${currentIndex}_$optIdx") {
                                onSelectOption(optIdx)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = optText,
                                fontSize = 14.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons: Previous, Next, Submit
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentIndex > 0) {
                    OutlinedButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }
                }

                if (currentIndex < totalQuestions - 1) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }
                } else {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_submit_practice"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Practice", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeScorecardView(
    questions: List<Question>,
    selectedAnswers: Map<Int, Int>,
    elapsedSeconds: Int,
    subject: String,
    chapter: String,
    difficulty: String,
    onRetry: () -> Unit,
    onNewPractice: () -> Unit
) {
    var correctCount = 0
    var wrongCount = 0
    var unattemptedCount = 0

    questions.forEachIndexed { idx, q ->
        val chosen = selectedAnswers[idx]
        if (chosen == null) {
            unattemptedCount++
        } else if (chosen == q.correctOptionIndex) {
            correctCount++
        } else {
            wrongCount++
        }
    }

    val total = questions.size.coerceAtLeast(1)
    val accuracy = if (correctCount + wrongCount > 0) (correctCount * 100f) / (correctCount + wrongCount) else 0f
    val scorePercent = (correctCount * 100) / total

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Scorecard Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (scorePercent >= 70) listOf(EmeraldSuccess, Color(0xFF10B981))
                                    else listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$scorePercent%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (scorePercent >= 70) "Well Done! Strong Mastery" else "Good Effort! Review Explanations",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$subject • $chapter",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "Score", value = "$correctCount / $total", color = MaterialTheme.colorScheme.primary)
                        StatItem(label = "Accuracy", value = String.format("%.1f%%", accuracy), color = EmeraldSuccess)
                        StatItem(label = "Time", value = String.format("%02dm %02ds", elapsedSeconds / 60, elapsedSeconds % 60), color = ElectricViolet)
                    }
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Drill")
                }
                Button(
                    onClick = onNewPractice,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Drill")
                }
            }
        }

        // Solutions Header
        item {
            Text(
                text = "Step-by-Step Question Review",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // List of question explanations
        items(questions.size) { idx ->
            val q = questions[idx]
            val chosen = selectedAnswers[idx]
            val isCorrect = chosen == q.correctOptionIndex

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(
                    1.dp,
                    if (chosen == null) MaterialTheme.colorScheme.outlineVariant
                    else if (isCorrect) EmeraldSuccess.copy(alpha = 0.5f)
                    else CoralError.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Q${idx + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (chosen == null) "Skipped" else if (isCorrect) "Correct (+1)" else "Incorrect (0)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (chosen == null) MaterialTheme.colorScheme.onSurfaceVariant else if (isCorrect) EmeraldSuccess else CoralError
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = q.questionText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Correct Answer: ${q.options.getOrNull(q.correctOptionIndex) ?: ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                    if (chosen != null && !isCorrect) {
                        Text(
                            text = "Your Answer: ${q.options.getOrNull(chosen) ?: ""}",
                            fontSize = 12.sp,
                            color = CoralError
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Explanation: ${q.explanation}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

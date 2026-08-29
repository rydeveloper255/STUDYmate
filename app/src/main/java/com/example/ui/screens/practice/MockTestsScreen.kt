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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.remote.supabase.MockTestTemplateDto
import com.example.data.remote.supabase.SupabaseQuestionBankService
import com.example.localization.GlobalLanguageSwitcher
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Step 68: Practice Feature 2 — Mock Tests Screen
 *
 * Dedicated page for full-length and subject mock test simulations:
 * - Exam categories & filter chips
 * - Available Mock Tests list with Title, Duration, Question Count, Difficulty, Attempt Status & Previous Score
 * - "Start Test" action -> Full Question Interface with Timer, Palette, Review, Indicators
 * - Result & Scorecard with Subject Analysis, Accuracy & Comparative Analytics
 * - Cache-First Supabase Data Source
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestsScreen(
    user: UserProfile?,
    attempts: List<MockTestAttempt>,
    initialCategory: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val questionService = remember { SupabaseQuestionBankService.instance }

    val defaultExam = user?.examName?.ifBlank { "SSC CGL" } ?: "SSC CGL"
    val categories = listOf("All", "SSC", "Railway", "Banking", "Teaching", "UPSC / State PSC", "Defence")
    var selectedCategory by remember { mutableStateOf(initialCategory ?: "All") }

    // 0 = Mock List, 1 = Active Test, 2 = Scorecard
    var viewState by remember { mutableIntStateOf(0) }
    var availableTemplates by remember { mutableStateOf<List<MockTestTemplateDto>>(emptyList()) }
    var isLoadingTemplates by remember { mutableStateOf(false) }

    // Active Test State
    var activeTemplate by remember { mutableStateOf<MockTestTemplateDto?>(null) }
    var activeQuestions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var userAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var markedForReview by remember { mutableStateOf(mutableSetOf<Int>()) }
    var remainingSeconds by remember { mutableIntStateOf(60 * 60) }
    var isTimerActive by remember { mutableStateOf(false) }
    var isSubmittingTest by remember { mutableStateOf(false) }

    // Load templates on launch
    LaunchedEffect(selectedCategory) {
        isLoadingTemplates = true
        val list = questionService.getMockTestTemplates(
            if (selectedCategory == "All") defaultExam else selectedCategory
        )
        availableTemplates = list
        isLoadingTemplates = false
    }

    // Countdown Timer
    LaunchedEffect(isTimerActive) {
        if (isTimerActive) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
            if (remainingSeconds <= 0) {
                // Auto submit on timeout
                isTimerActive = false
                viewState = 2
            }
        }
    }

    BackHandler(enabled = true) {
        if (viewState != 0) {
            viewState = 0
            isTimerActive = false
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
                            text = when (viewState) {
                                1 -> activeTemplate?.title ?: "Live Mock Test"
                                2 -> "Mock Test Analysis"
                                else -> "All-India Mock Tests"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (viewState == 1) "${activeQuestions.size} Questions • Real-Time Exam Mode" else defaultExam,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (viewState != 0) {
                                viewState = 0
                                isTimerActive = false
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("btn_mock_back")
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
                    if (viewState == 1) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (remainingSeconds < 300) CoralError.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = if (remainingSeconds < 300) CoralError else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remainingSeconds < 300) CoralError else MaterialTheme.colorScheme.primary
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
            when (viewState) {
                0 -> {
                    // MOCK LIST VIEW
                    MockTestListView(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it },
                        templates = availableTemplates,
                        attempts = attempts,
                        isLoading = isLoadingTemplates,
                        onStartMock = { tmpl ->
                            activeTemplate = tmpl
                            coroutineScope.launch {
                                val qs = questionService.getQuestionsForPractice(
                                    examName = tmpl.examCategory,
                                    subject = "All",
                                    count = tmpl.totalQuestions
                                )
                                activeQuestions = qs
                                userAnswers.clear()
                                markedForReview.clear()
                                currentQuestionIndex = 0
                                remainingSeconds = tmpl.durationMinutes * 60
                                isTimerActive = true
                                viewState = 1
                            }
                        }
                    )
                }
                1 -> {
                    // ACTIVE MOCK TEST SCREEN
                    if (activeQuestions.isNotEmpty()) {
                        val currentQ = activeQuestions.getOrNull(currentQuestionIndex) ?: activeQuestions.first()
                        ActiveMockTestPlayer(
                            question = currentQ,
                            currentIndex = currentQuestionIndex,
                            totalQuestions = activeQuestions.size,
                            selectedOption = userAnswers[currentQuestionIndex],
                            isMarkedForReview = markedForReview.contains(currentQuestionIndex),
                            onSelectOption = { opt -> userAnswers[currentQuestionIndex] = opt },
                            onToggleReview = {
                                if (markedForReview.contains(currentQuestionIndex)) {
                                    markedForReview.remove(currentQuestionIndex)
                                } else {
                                    markedForReview.add(currentQuestionIndex)
                                }
                            },
                            onClearAnswer = { userAnswers.remove(currentQuestionIndex) },
                            onNavigateTo = { idx -> currentQuestionIndex = idx },
                            onSubmit = {
                                isTimerActive = false
                                viewState = 2
                            }
                        )
                    }
                }
                2 -> {
                    // SCORECARD VIEW
                    MockTestScorecard(
                        template = activeTemplate,
                        questions = activeQuestions,
                        userAnswers = userAnswers,
                        pastAttempts = attempts,
                        onRetake = {
                            userAnswers.clear()
                            markedForReview.clear()
                            currentQuestionIndex = 0
                            remainingSeconds = (activeTemplate?.durationMinutes ?: 60) * 60
                            isTimerActive = true
                            viewState = 1
                        },
                        onBackToList = {
                            viewState = 0
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MockTestListView(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    templates: List<MockTestTemplateDto>,
    attempts: List<MockTestAttempt>,
    isLoading: Boolean,
    onStartMock: (MockTestTemplateDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(ElectricViolet, Color(0xFF6366F1)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.TaskAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Standard Exam Simulations",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Full negative marking, timed interfaces & deep subject analytics.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Category Filter
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Tests Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available Test Series",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${templates.size} Mock Tests",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        } else {
            items(templates) { tmpl ->
                val pastAttempt = attempts.firstOrNull { it.title.contains(tmpl.title) || it.examName == tmpl.examCategory }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tmpl.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tmpl.subjects.joinToString(" • "),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (tmpl.difficulty == "Hard") CoralError.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = tmpl.difficulty,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tmpl.difficulty == "Hard") CoralError else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Quiz, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${tmpl.totalQuestions} Questions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricViolet)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${tmpl.durationMinutes} Mins", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            if (pastAttempt != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldSuccess)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Score: ${pastAttempt.score}", fontSize = 12.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onStartMock(tmpl) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_start_mock_${tmpl.testId}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (pastAttempt != null) "Retake Mock Test" else "Start Mock Test", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveMockTestPlayer(
    question: Question,
    currentIndex: Int,
    totalQuestions: Int,
    selectedOption: Int?,
    isMarkedForReview: Boolean,
    onSelectOption: (Int) -> Unit,
    onToggleReview: () -> Unit,
    onClearAnswer: () -> Unit,
    onNavigateTo: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Question Navigation Strip
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(totalQuestions) { idx ->
                    val isCurrent = idx == currentIndex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onNavigateTo(idx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Question Details
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isMarkedForReview,
                                onCheckedChange = { onToggleReview() }
                            )
                            Text("Mark for Review", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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

        // Options
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { optIdx, optText ->
                    val isSelected = selectedOption == optIdx
                    val labels = listOf("A", "B", "C", "D")

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .springClickable { onSelectOption(optIdx) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = labels.getOrNull(optIdx) ?: "${optIdx + 1}",
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

        // Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearAnswer,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear", fontSize = 12.sp)
                }

                if (currentIndex > 0) {
                    OutlinedButton(
                        onClick = { onNavigateTo(currentIndex - 1) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Prev", fontSize = 12.sp)
                    }
                }

                if (currentIndex < totalQuestions - 1) {
                    Button(
                        onClick = { onNavigateTo(currentIndex + 1) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MockTestScorecard(
    template: MockTestTemplateDto?,
    questions: List<Question>,
    userAnswers: Map<Int, Int>,
    pastAttempts: List<MockTestAttempt>,
    onRetake: () -> Unit,
    onBackToList: () -> Unit
) {
    var correct = 0
    var wrong = 0
    var skipped = 0

    questions.forEachIndexed { idx, q ->
        val ans = userAnswers[idx]
        if (ans == null) skipped++
        else if (ans == q.correctOptionIndex) correct++
        else wrong++
    }

    val total = questions.size.coerceAtLeast(1)
    val score = correct * 2 - (wrong * 0.5f) // SSC standard marking
    val accuracy = if (correct + wrong > 0) (correct * 100f) / (correct + wrong) else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = template?.title ?: "Mock Test Scorecard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = String.format("%.1f", score),
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total Marks (Max: ${total * 2})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$correct", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldSuccess)
                            Text("Correct", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$wrong", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CoralError)
                            Text("Wrong", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$skipped", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Skipped", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(String.format("%.1f%%", accuracy), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ElectricViolet)
                            Text("Accuracy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retake Test")
                }
                Button(
                    onClick = onBackToList,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("All Tests")
                }
            }
        }

        item {
            Text(
                text = "Detailed Solutions & Explanations",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(questions.size) { idx ->
            val q = questions[idx]
            val chosen = userAnswers[idx]
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
                    Text(
                        text = "Q${idx + 1}: ${q.questionText}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Correct Answer: ${q.options.getOrNull(q.correctOptionIndex) ?: ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Explanation: ${q.explanation}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

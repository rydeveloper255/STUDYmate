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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Question
import com.example.data.model.UserProfile
import com.example.data.remote.supabase.SupabaseQuestionBankService
import com.example.localization.GlobalLanguageSwitcher
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Step 68: Practice Feature 4 — Daily Speed Quiz Screen
 *
 * Dedicated page for date-wise automated daily speed quizzes:
 * - Predefined Quiz prepared ONCE per day in Supabase (shared across all users)
 * - Date Archive & Date Switcher (Today, Yesterday, Past Dates)
 * - 10-Question Timed Speed Quiz Interface
 * - Instant Scorecard & Result Analytics
 * - Cache-First Supabase Data Source
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySpeedQuizScreen(
    user: UserProfile?,
    initialQuizDate: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val questionService = remember { SupabaseQuestionBankService.instance }

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var selectedQuizDate by remember { mutableStateOf(initialQuizDate ?: todayDateStr) }

    // Generate past 7 days list
    val pastDates = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (i in 0 until 7) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        list
    }

    // 0 = Intro / Date Select, 1 = Live Quiz, 2 = Scorecard
    var quizState by remember { mutableIntStateOf(0) }
    var quizQuestions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var isLoadingQuiz by remember { mutableStateOf(false) }

    // Live Quiz State
    var currentQuestionIdx by remember { mutableIntStateOf(0) }
    var userAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var remainingSeconds by remember { mutableIntStateOf(10 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Fetch quiz on date select
    LaunchedEffect(selectedQuizDate) {
        isLoadingQuiz = true
        val questions = questionService.getDailySpeedQuiz(selectedQuizDate)
        quizQuestions = questions
        userAnswers.clear()
        currentQuestionIdx = 0
        remainingSeconds = 10 * 60
        isLoadingQuiz = false
    }

    // Countdown Timer
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
            if (remainingSeconds <= 0) {
                isTimerRunning = false
                quizState = 2
            }
        }
    }

    BackHandler(enabled = true) {
        if (quizState != 0) {
            quizState = 0
            isTimerRunning = false
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
                            text = when (quizState) {
                                1 -> "Speed Drill in Progress"
                                2 -> "Daily Quiz Scorecard"
                                else -> "Daily Speed Quiz"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Date: $selectedQuizDate • 10 Questions",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (quizState != 0) {
                                quizState = 0
                                isTimerRunning = false
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("btn_daily_quiz_back")
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
                    if (quizState == 1) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (remainingSeconds < 60) CoralError.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = if (remainingSeconds < 60) CoralError else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remainingSeconds < 60) CoralError else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (quizState) {
                0 -> {
                    // INTRO & DATE SELECTION VIEW
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
                                                Brush.linearGradient(listOf(NeonCyan, Color(0xFF0284C7)))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(30.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Official Daily Speed Quiz",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "10 high-yield questions across GA, Science, Reasoning & Quant.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Date Selector Tabs
                        item {
                            Text("Select Quiz Date", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(pastDates) { dt ->
                                    val isSel = dt == selectedQuizDate
                                    val label = if (dt == todayDateStr) "Today ($dt)" else dt
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedQuizDate = dt },
                                        label = { Text(label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonCyan,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }

                        // Quiz Details & Start Card
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = "Daily Challenge for $selectedQuizDate",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "• 10 High-Yield Curated Questions\n• 10 Minutes Total Time\n• +1 Mark per Correct Answer, Instant Analytics\n• Unified official daily test set for all StudyMate aspirants",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            if (quizQuestions.isNotEmpty()) {
                                                userAnswers.clear()
                                                currentQuestionIdx = 0
                                                remainingSeconds = 10 * 60
                                                isTimerRunning = true
                                                quizState = 1
                                            }
                                        },
                                        enabled = !isLoadingQuiz && quizQuestions.isNotEmpty(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("btn_start_daily_quiz"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        if (isLoadingQuiz) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Start Speed Quiz", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // LIVE QUIZ PLAYER
                    if (quizQuestions.isNotEmpty()) {
                        val currentQ = quizQuestions.getOrNull(currentQuestionIdx) ?: quizQuestions.first()
                        DailyQuizPlayer(
                            question = currentQ,
                            currentIndex = currentQuestionIdx,
                            totalQuestions = quizQuestions.size,
                            selectedOption = userAnswers[currentQuestionIdx],
                            onSelectOption = { userAnswers[currentQuestionIdx] = it },
                            onNavigateTo = { currentQuestionIdx = it },
                            onSubmit = {
                                isTimerRunning = false
                                quizState = 2
                            }
                        )
                    }
                }
                2 -> {
                    // INSTANT SCORECARD
                    DailyQuizScorecard(
                        dateStr = selectedQuizDate,
                        questions = quizQuestions,
                        userAnswers = userAnswers,
                        elapsedSeconds = (10 * 60) - remainingSeconds,
                        onRetake = {
                            userAnswers.clear()
                            currentQuestionIdx = 0
                            remainingSeconds = 10 * 60
                            isTimerRunning = true
                            quizState = 1
                        },
                        onBackToList = {
                            quizState = 0
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyQuizPlayer(
    question: Question,
    currentIndex: Int,
    totalQuestions: Int,
    selectedOption: Int?,
    onSelectOption: (Int) -> Unit,
    onNavigateTo: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress & Number
        item {
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
                Text(
                    text = "${(currentIndex + 1) * 100 / totalQuestions}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / totalQuestions },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = NeonCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Question Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentIndex > 0) {
                    OutlinedButton(
                        onClick = { onNavigateTo(currentIndex - 1) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Previous")
                    }
                }

                if (currentIndex < totalQuestions - 1) {
                    Button(
                        onClick = { onNavigateTo(currentIndex + 1) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit Quiz", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyQuizScorecard(
    dateStr: String,
    questions: List<Question>,
    userAnswers: Map<Int, Int>,
    elapsedSeconds: Int,
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
                        text = "Daily Speed Quiz — $dateStr",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$correct / $total",
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = if (correct >= 7) EmeraldSuccess else MaterialTheme.colorScheme.primary
                    )
                    Text("Total Correct Answers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(String.format("%.1f%%", accuracy), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ElectricViolet)
                            Text("Accuracy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(String.format("%02dm %02ds", elapsedSeconds / 60, elapsedSeconds % 60), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Time Taken", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$wrong", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CoralError)
                            Text("Mistakes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Retake Quiz")
                }
                Button(onClick = onBackToList, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Daily Archive")
                }
            }
        }

        item {
            Text("Review Questions & Explanations", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        items(questions.size) { idx ->
            val q = questions[idx]
            val ans = userAnswers[idx]
            val isCorrect = ans == q.correctOptionIndex

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (isCorrect) EmeraldSuccess.copy(alpha = 0.5f) else CoralError.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Q${idx + 1}: ${q.questionText}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Correct Answer: ${q.options.getOrNull(q.correctOptionIndex) ?: ""}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Explanation: ${q.explanation}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

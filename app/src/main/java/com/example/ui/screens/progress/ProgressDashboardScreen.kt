package com.example.ui.screens.progress

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.service.intelligence.*
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StreakBadge
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.ActiveTestState
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class ProgressTimeRange(val displayName: String, val days: Int) {
    DAYS_7("7D", 7),
    DAYS_30("30D", 30),
    DAYS_90("90D", 90),
    ALL_TIME("All", 3650)
}

@Composable
fun ProgressDashboardScreen(
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
    onStartPractice: ((com.example.service.intelligence.SmartPracticeRecommendation) -> Unit)? = null,
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
    onSaveAndNext: (() -> Unit)? = null,
    onMarkForReviewAndNext: (() -> Unit)? = null,
    onPreviousQuestion: (() -> Unit)? = null,
    onSaveExamObjective: (ExamObjective) -> Unit = {},
    onStartFocusOnTopic: (subject: String, topic: String) -> Unit = { _, _ -> },
    examReadiness: ExamReadinessScore? = null,
    subjectSummaries: List<SubjectProgressSummary> = emptyList(),
    recommendations: List<StudyRecommendation> = emptyList(),
    dailyPlan: DailyStudyPlan? = null,
    onSetManualTopicOverride: (subject: String, topic: String, override: String) -> Unit = { _, _, _ -> },
    onResetPreparationData: () -> Unit = {},
    onOpenReadinessCenter: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onNavigateToStudy: (() -> Unit)? = null,
    activeExamContext: ExamContext? = null,
    novaProgressAnalysis: String? = null,
    isNovaProgressAnalyzing: Boolean = false,
    onGenerateNovaProgressAnalysis: ((examName: String, summaryText: String, language: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val isHindi = user?.languagePreference?.equals("Hindi", ignoreCase = true) == true

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Overview Analytics, 1: Spaced Revision, 2: Syllabus Mastery, 3: Mistake Book
    var selectedTimeRange by remember { mutableStateOf(ProgressTimeRange.DAYS_30) }
    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var expandedSubjectName by remember { mutableStateOf<String?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    var showSetupDialog by remember { mutableStateOf(false) }
    var showMaterialManager by remember { mutableStateOf(false) }
    var showQuestionBankExplorer by remember { mutableStateOf(false) }

    // 1. Context Resolution & Exam-Specific Isolation
    val currentExamName = remember(activeExamContext?.examName, user?.examName) {
        activeExamContext?.examName?.takeIf { it.isNotBlank() }
            ?: user?.examName?.takeIf { it.isNotBlank() }
            ?: "General Competitive Exam"
    }

    val examSubjects = remember(activeExamContext?.subjects, user?.subjects) {
        val list = when {
            !activeExamContext?.subjects.isNullOrEmpty() -> activeExamContext!!.subjects.map { it.name }
            !user?.subjects.isNullOrEmpty() -> user!!.subjects
            else -> listOf("General Science", "Mathematics", "General Intelligence & Reasoning", "General Awareness")
        }
        list.distinct().filter { it.isNotBlank() }
    }

    // Isolated Mock Test Attempts for Active Exam
    val examIsolatedAttempts = remember(attempts, currentExamName) {
        val matching = attempts.filter {
            it.examName.equals(currentExamName, ignoreCase = true) ||
            it.examId == activeExamContext?.examId ||
            (it.examName.isBlank() || it.examName == "JEE / NEET / Board Exam" || it.examName == "default_exam")
        }
        if (matching.isNotEmpty()) matching else attempts
    }

    // Time-Filtered Mock Attempts
    val filteredAttempts = remember(examIsolatedAttempts, selectedTimeRange, selectedSubjectFilter) {
        val cutoff = System.currentTimeMillis() - (selectedTimeRange.days.toLong() * 24 * 3600 * 1000L)
        examIsolatedAttempts.filter { attempt ->
            val inTime = if (selectedTimeRange == ProgressTimeRange.ALL_TIME) true else attempt.timestamp >= cutoff
            val inSubject = if (selectedSubjectFilter == "All") true else attempt.subject.equals(selectedSubjectFilter, ignoreCase = true) || attempt.subject == "All Subjects"
            inTime && inSubject
        }.sortedBy { it.timestamp }
    }

    // Previous Period Mock Attempts (for comparison)
    val previousPeriodAttempts = remember(examIsolatedAttempts, selectedTimeRange) {
        if (selectedTimeRange == ProgressTimeRange.ALL_TIME) emptyList()
        else {
            val periodMillis = selectedTimeRange.days.toLong() * 24 * 3600 * 1000L
            val currentCutoff = System.currentTimeMillis() - periodMillis
            val previousCutoff = currentCutoff - periodMillis
            examIsolatedAttempts.filter { it.timestamp in previousCutoff until currentCutoff }
        }
    }

    // Isolated Study Sessions & Time Calculation
    val filteredStudySessions = remember(allFocusSessions, sessionHistory, examSubjects, selectedTimeRange) {
        val cutoff = System.currentTimeMillis() - (selectedTimeRange.days.toLong() * 24 * 3600 * 1000L)
        val combinedSessions = allFocusSessions.map {
            StudentSessionHistory(
                id = it.id,
                sessionType = "FOCUS_STUDY",
                subject = it.subject,
                topic = it.topic,
                durationMinutes = it.durationMinutes,
                actualMinutesSpent = it.actualMinutesSpent,
                xpEarned = it.xpEarned,
                accuracyPercent = 100f,
                questionsAttempted = 0,
                productivityRating = 5,
                notesSummary = "",
                timestamp = it.timestamp
            )
        } + sessionHistory

        combinedSessions.filter { session ->
            val inTime = if (selectedTimeRange == ProgressTimeRange.ALL_TIME) true else session.timestamp >= cutoff
            val inExam = examSubjects.isEmpty() || examSubjects.any { it.equals(session.subject, ignoreCase = true) } || session.subject == "General"
            inTime && inExam
        }.sortedBy { it.timestamp }
    }

    val previousPeriodStudySessions = remember(allFocusSessions, sessionHistory, examSubjects, selectedTimeRange) {
        if (selectedTimeRange == ProgressTimeRange.ALL_TIME) emptyList()
        else {
            val periodMillis = selectedTimeRange.days.toLong() * 24 * 3600 * 1000L
            val currentCutoff = System.currentTimeMillis() - periodMillis
            val previousCutoff = currentCutoff - periodMillis
            val combined = allFocusSessions.map {
                StudentSessionHistory(
                    id = it.id,
                    sessionType = "FOCUS_STUDY",
                    subject = it.subject,
                    topic = it.topic,
                    durationMinutes = it.durationMinutes,
                    actualMinutesSpent = it.actualMinutesSpent,
                    xpEarned = it.xpEarned,
                    accuracyPercent = 100f,
                    questionsAttempted = 0,
                    productivityRating = 5,
                    notesSummary = "",
                    timestamp = it.timestamp
                )
            } + sessionHistory
            combined.filter { it.timestamp in previousCutoff until currentCutoff }
        }
    }

    // Core Metrics Calculations (Deterministic & Local)
    val totalTestsCount = filteredAttempts.size
    val totalQuestionsAttempted = filteredAttempts.sumOf { it.totalQuestions }
    val averageAccuracy = if (filteredAttempts.isNotEmpty()) {
        filteredAttempts.map { it.accuracyPercent }.average().toFloat()
    } else null

    val totalStudyMinutes = remember(filteredStudySessions) {
        filteredStudySessions.sumOf { it.actualMinutesSpent.coerceAtLeast(1) }
    }

    val totalStudyTimeFormatted = remember(totalStudyMinutes) {
        val hours = totalStudyMinutes / 60
        val mins = totalStudyMinutes % 60
        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    val completedTopicsCount = remember(topicMasteries, examSubjects) {
        topicMasteries.filter { mastery ->
            mastery.masteryScore >= 70 && (examSubjects.isEmpty() || examSubjects.any { it.equals(mastery.subject, ignoreCase = true) })
        }.size
    }

    // Comparison metrics (Previous period vs Current period)
    val prevAverageAccuracy = if (previousPeriodAttempts.isNotEmpty()) {
        previousPeriodAttempts.map { it.accuracyPercent }.average().toFloat()
    } else null

    val prevStudyMinutes = previousPeriodStudySessions.sumOf { it.actualMinutesSpent.coerceAtLeast(1) }

    // Subject Performance Analysis
    val subjectPerformanceList = remember(examSubjects, filteredAttempts, topicMasteries) {
        examSubjects.map { subjectName ->
            val subAttempts = filteredAttempts.filter { it.subject.equals(subjectName, ignoreCase = true) }
            val subMasteries = topicMasteries.filter { it.subject.equals(subjectName, ignoreCase = true) }
            val avgSubAccuracy = if (subAttempts.isNotEmpty()) subAttempts.map { it.accuracyPercent }.average().toFloat() else null
            val totalQs = subAttempts.sumOf { it.totalQuestions }
            val testsCount = subAttempts.size
            val avgMastery = if (subMasteries.isNotEmpty()) subMasteries.map { it.masteryScore }.average().toInt() else null

            SubjectProgressItem(
                name = subjectName,
                accuracy = avgSubAccuracy,
                testsTaken = testsCount,
                questionsSolved = totalQs,
                masteryScore = avgMastery,
                topics = subMasteries
            )
        }
    }

    // Weak Areas (accuracy < 60% or unmastered mistakes)
    val weakAreasList = remember(subjectPerformanceList, mistakes, topicMasteries) {
        val list = mutableListOf<WeakAreaInfo>()
        subjectPerformanceList.forEach { sub ->
            if (sub.accuracy != null && sub.accuracy < 60f) {
                list.add(WeakAreaInfo(sub.name, "All Topics", sub.accuracy.toInt(), sub.testsTaken, "Low subject accuracy (${sub.accuracy.toInt()}%)"))
            }
        }
        topicMasteries.filter { it.masteryScore < 60 }.forEach { tm ->
            if (list.none { it.topic == tm.topic }) {
                list.add(WeakAreaInfo(tm.subject, tm.topic, tm.masteryScore, 0, if (tm.masteryScore < 40) "Needs more practice" else "Low mastery score (${tm.masteryScore}%)"))
            }
        }
        val unmasteredMistakesByTopic = mistakes.filter { !it.isMastered }.groupBy { it.topic }
        unmasteredMistakesByTopic.forEach { (topic, mistList) ->
            if (mistList.size >= 2 && list.none { it.topic == topic }) {
                val sub = mistList.first().subject
                list.add(WeakAreaInfo(sub, topic, 45, mistList.size, "${mistList.size} unmastered mistakes in this topic"))
            }
        }
        list.take(4)
    }

    // Strong Areas (accuracy >= 75% or mastery >= 75%)
    val strongAreasList = remember(subjectPerformanceList, topicMasteries) {
        val list = mutableListOf<StrongAreaInfo>()
        subjectPerformanceList.forEach { sub ->
            if (sub.accuracy != null && sub.accuracy >= 75f) {
                list.add(StrongAreaInfo(sub.name, "Subject Level", sub.accuracy.toInt(), "${sub.testsTaken} tests completed (${sub.accuracy.toInt()}% accuracy)"))
            }
        }
        topicMasteries.filter { it.masteryScore >= 75 }.forEach { tm ->
            if (list.none { it.topic == tm.topic }) {
                list.add(StrongAreaInfo(tm.subject, tm.topic, tm.masteryScore, "Mastery score ${tm.masteryScore}%"))
            }
        }
        list.take(4)
    }

    // Spaced Revision Queue & Performance Report Engine
    val performanceReport = remember(user, attempts, mistakes, topicMasteries) {
        PerformanceCoachEngine.computePerformanceReport(
            profile = user ?: UserProfile(),
            mockAttempts = examIsolatedAttempts,
            mistakes = mistakes,
            topicMasteries = topicMasteries,
            focusSessions = allFocusSessions,
            plans = emptyList()
        )
    }

    val revisionQueue = remember(topicMasteries, mistakes, examIsolatedAttempts, user) {
        SmartRevisionEngine.buildRevisionQueue(
            topicMasteries = topicMasteries,
            mistakes = mistakes,
            mockAttempts = examIsolatedAttempts,
            examDateMillis = user?.examDateMillis ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
        )
    }

    // Daily Study Target Info
    val dailyTargetMinutes = remember(user?.dailyTargetMinutes) {
        user?.dailyTargetMinutes ?: 180
    }
    val todayStudyMinutes = remember(allFocusSessions, sessionHistory) {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        allFocusSessions.filter { it.timestamp >= startOfToday }.sumOf { it.actualMinutesSpent }
    }

    // High-leverage Recommended Next Step
    val recommendedNextStep = remember(weakAreasList, dailyTargetMinutes, todayStudyMinutes, mistakes, examSubjects) {
        when {
            weakAreasList.isNotEmpty() -> {
                val weak = weakAreasList.first()
                RecommendedAction(
                    title = if (isHindi) "कमजोर टॉपिक का रीविजन करें: ${weak.topic}" else "Revise Weak Area: ${weak.topic}",
                    subtitle = "${weak.subject} • ${weak.reason}",
                    badge = if (isHindi) "उच्च प्राथमिकता" else "Priority Focus",
                    actionType = ActionType.PRACTICE_TOPIC,
                    subject = weak.subject,
                    topic = weak.topic
                )
            }
            mistakes.count { !it.isMastered } >= 3 -> {
                val unmasteredCount = mistakes.count { !it.isMastered }
                RecommendedAction(
                    title = if (isHindi) "$unmasteredCount गलत प्रश्नों का अभ्यास करें" else "Review $unmasteredCount Past Mistakes",
                    subtitle = if (isHindi) "अपनी गलतियों को सुधारें" else "Turn past errors into strengths",
                    badge = if (isHindi) "सुधार" else "Diagnostics",
                    actionType = ActionType.MISTAKE_BOOK,
                    subject = mistakes.firstOrNull()?.subject ?: examSubjects.firstOrNull() ?: "General",
                    topic = "Mistake Revision"
                )
            }
            todayStudyMinutes < dailyTargetMinutes -> {
                val remaining = dailyTargetMinutes - todayStudyMinutes
                val firstSub = examSubjects.firstOrNull() ?: "General Science"
                RecommendedAction(
                    title = if (isHindi) "दैनिक लक्ष्य पूरा करें (${remaining} मिनट शेष)" else "Complete Daily Target (${remaining}m remaining)",
                    subtitle = if (isHindi) "$firstSub के साथ अध्ययन जारी रखें" else "Continue study session in $firstSub",
                    badge = if (isHindi) "दैनिक लक्ष्य" else "Daily Target",
                    actionType = ActionType.FOCUS_STUDY,
                    subject = firstSub,
                    topic = "Core Concepts"
                )
            }
            else -> {
                val nextSub = examSubjects.firstOrNull() ?: "General"
                RecommendedAction(
                    title = if (isHindi) "$nextSub का मॉक टेस्ट दें" else "Take a Mock Test in $nextSub",
                    subtitle = if (isHindi) "परीक्षा पैटर्न के अनुसार अभ्यास करें" else "Test your speed and accuracy under exam timing",
                    badge = if (isHindi) "मॉक टेस्ट" else "Full Mock",
                    actionType = ActionType.MOCK_TEST,
                    subject = nextSub,
                    topic = "Comprehensive"
                )
            }
        }
    }

    // Active Test / Completed Review Handler
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(horizontal = 16.dp)
            .testTag("progress_dashboard_screen"),
        contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. TOP HEADER & COMPACT ACTIONS ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x221E293B))
                                    .testTag("progress_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column {
                            Text(
                                text = if (isHindi) "📊 मेरी प्रगति" else "📊 My Progress",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isHindi) "अपनी परीक्षा तैयारी ट्रैक करें" else "Track your preparation & performance",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StreakBadge(streakDays = user?.streakDays ?: 1)
                        Spacer(modifier = Modifier.width(6.dp))

                        Box {
                            IconButton(
                                onClick = { showOptionsMenu = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x221E293B))
                                    .testTag("progress_options_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color(0xFFCBD5E1)
                                )
                            }

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false },
                                modifier = Modifier.background(Color(0xFF0F172A))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📄 Export Analytics PDF", color = Color.White) },
                                    onClick = {
                                        showOptionsMenu = false
                                        try {
                                            val pdfFile = PdfReportGenerator.generateAnalyticsSummaryPdf(
                                                context = context,
                                                user = user,
                                                attempts = examIsolatedAttempts,
                                                mistakes = mistakes,
                                                focusMinutes = totalStudyMinutes,
                                                studyStreak = user?.streakDays ?: 1
                                            )
                                            PdfReportGenerator.sharePdfReport(
                                                context = context,
                                                pdfFile = pdfFile,
                                                shareTitle = "StudyMate AI - Academic Analytics Summary"
                                            )
                                            Toast.makeText(context, "Analytics Portfolio PDF generated", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🎯 Exam Readiness Center", color = Color.White) },
                                    onClick = {
                                        showOptionsMenu = false
                                        onOpenReadinessCenter()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📁 Custom Materials", color = Color.White) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showMaterialManager = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔄 Reset Preparation Data", color = CoralRose) },
                                    onClick = {
                                        showOptionsMenu = false
                                        onResetPreparationData()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- 2. SELECTED EXAM BANNER (ISOLATED CONTEXT) ---
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    fillAlpha = 0.85f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(ElectricIndigo.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TrackChanges,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "🎯 तैयारी का लक्ष्य" else "🎯 Preparing For",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentExamName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${examSubjects.size} ${if (isHindi) "विषय" else "Subjects"}",
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sub-navigation Switcher Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x251E293B))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        if (isHindi) "एनालिटिक्स" else "Analytics",
                        if (isHindi) "रीविजन (${revisionQueue.size})" else "Revision (${revisionQueue.size})",
                        if (isHindi) "मास्टरी" else "Mastery",
                        if (isHindi) "गलतियां (${mistakes.size})" else "Mistakes (${mistakes.size})"
                    ).forEachIndexed { idx, label ->
                        val isSelected = selectedSubTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .springClickable(testTag = "progress_subtab_$idx") {
                                    selectedSubTab = idx
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
        }

        // --- SUBTAB 0: MAIN ANALYTICS DASHBOARD ---
        if (selectedSubTab == 0) {
            // Filters (Period & Subject)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time Range Selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x201E293B))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ProgressTimeRange.values().forEach { range ->
                            val isSelected = selectedTimeRange == range
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ElectricIndigo else Color.Transparent)
                                    .clickable { selectedTimeRange = range }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = range.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Launch Test Shortcut
                    FilledTonalButton(
                        onClick = { showSetupDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("progress_take_test_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "मॉक टेस्ट" else "Take Quiz",
                            color = NeonCyan,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- 3. OVERALL PERFORMANCE CARD ---
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_overall_card"),
                    shape = RoundedCornerShape(22.dp),
                    fillAlpha = 0.92f
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
                            Text(
                                text = if (isHindi) "समग्र प्रदर्शन" else "Overall Performance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = selectedTimeRange.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4 Key Performance Metrics Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Accuracy Tile
                            PerformanceMetricTile(
                                title = if (isHindi) "सटीकता" else "Accuracy",
                                value = if (averageAccuracy != null) "${averageAccuracy.toInt()}%" else "Not enough data yet",
                                subtitle = if (averageAccuracy != null) "$totalQuestionsAttempted ${if (isHindi) "प्रश्न" else "questions"}" else "Take a test to see",
                                color = if (averageAccuracy != null && averageAccuracy >= 70) EmeraldSuccess else GoldenSpark,
                                modifier = Modifier.weight(1f)
                            )

                            // Tests Taken Tile
                            PerformanceMetricTile(
                                title = if (isHindi) "मॉक टेस्ट" else "Tests Taken",
                                value = "$totalTestsCount",
                                subtitle = if (isHindi) "पूरे किए गए" else "Completed tests",
                                color = NeonCyan,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Study Time Tile
                            PerformanceMetricTile(
                                title = if (isHindi) "अध्ययन समय" else "Study Time",
                                value = totalStudyTimeFormatted,
                                subtitle = "${filteredStudySessions.size} ${if (isHindi) "सत्र" else "sessions"}",
                                color = ElectricViolet,
                                modifier = Modifier.weight(1f)
                            )

                            // Topics Completed Tile
                            PerformanceMetricTile(
                                title = if (isHindi) "पूरे टॉपिक" else "Topics Mastered",
                                value = "$completedTopicsCount",
                                subtitle = if (isHindi) "70%+ मास्टरी" else "≥ 70% mastery",
                                color = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Personal Period Comparison (if enough historical data exists)
                        if (prevAverageAccuracy != null && averageAccuracy != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            val accuracyDelta = averageAccuracy - prevAverageAccuracy
                            val studyDeltaMins = totalStudyMinutes - prevStudyMinutes

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x301E293B),
                                border = BorderStroke(1.dp, Color(0x20FFFFFF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (accuracyDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = if (accuracyDelta >= 0) EmeraldSuccess else CoralRose,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "पिछली अवधि बनाम वर्तमान:" else "Previous Period → Current:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    Text(
                                        text = "Acc: ${prevAverageAccuracy.toInt()}% → ${averageAccuracy.toInt()}% (${if (accuracyDelta >= 0) "+" else ""}${accuracyDelta.toInt()}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (accuracyDelta >= 0) EmeraldSuccess else CoralRose
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. PERFORMANCE TREND (ACCESSIBLE CHART) ---
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_performance_trend_card"),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = 0.88f
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
                                Icon(Icons.Filled.ShowChart, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "प्रदर्शन रुझान" else "Performance Trend",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = "${filteredAttempts.size} ${if (isHindi) "परीक्षण" else "tests"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredAttempts.isEmpty()) {
                            // Friendly Empty State
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Quiz,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isHindi) "इस अवधि में कोई टेस्ट नहीं लिया गया" else "Your Progress Starts Here",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isHindi) "प्रदर्शन रुझान देखने के लिए अपना पहला मॉक टेस्ट पूरा करें।" else "Complete your first quiz to see performance analytics.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { showSetupDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "पहला टेस्ट दें" else "Take a Quiz",
                                        color = Color(0xFF060B18),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        } else {
                            // Visual Plot of Actual Test Results
                            PerformanceTrendPlot(
                                attempts = filteredAttempts,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Accessible Text Summary
                            val firstScore = filteredAttempts.first().accuracyPercent.toInt()
                            val latestScore = filteredAttempts.last().accuracyPercent.toInt()
                            val diff = latestScore - firstScore
                            val trendSummaryText = when {
                                filteredAttempts.size == 1 -> "1 test completed with ${latestScore}% accuracy."
                                diff > 0 -> "Accuracy improved from ${firstScore}% to ${latestScore}% (+${diff}%) across recent tests."
                                diff < 0 -> "Accuracy changed from ${firstScore}% to ${latestScore}% (${diff}%) across recent tests."
                                else -> "Consistent performance averaging ${latestScore}% across ${filteredAttempts.size} tests."
                            }

                            Text(
                                text = trendSummaryText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (diff >= 0) EmeraldSuccess else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            // --- 5. STUDY TIME ACTIVITY TREND ---
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_study_trend_card"),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = 0.88f
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
                                Icon(Icons.Filled.AccessTime, null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "अध्ययन गतिविधि" else "Study Activity",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = totalStudyTimeFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricViolet,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredStudySessions.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isHindi) "अध्ययन शुरू करें" else "Start Studying",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isHindi) "जैसे-जैसे आप पढ़ेंगे, आपकी अध्ययन गतिविधि यहाँ दिखाई देगी।" else "Your study activity will appear here as you learn.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        val firstSub = examSubjects.firstOrNull() ?: "General"
                                        onStartFocusOnTopic(firstSub, "Core Concepts")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "अध्ययन शुरू करें" else "Start Studying",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        } else {
                            StudyTimeDailyBarChart(
                                sessions = filteredStudySessions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            )
                        }
                    }
                }
            }

            // --- 6. SUBJECT PERFORMANCE (EXAM-SPECIFIC ONLY) ---
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_subject_performance_card"),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = 0.88f
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
                                Icon(Icons.Filled.MenuBook, null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "विषयवार प्रदर्शन" else "Subject Performance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = "${subjectPerformanceList.size} ${if (isHindi) "विषय" else "Subjects"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        subjectPerformanceList.forEach { item ->
                            val isExpanded = expandedSubjectName == item.name
                            SubjectPerformanceRow(
                                item = item,
                                isExpanded = isExpanded,
                                isHindi = isHindi,
                                onToggleExpand = {
                                    expandedSubjectName = if (isExpanded) null else item.name
                                },
                                onPracticeSubject = {
                                    onStartTestWithConfig(
                                        MockTestConfig(
                                            exam = currentExamName,
                                            testType = MockTestType.SUBJECT_TEST,
                                            subject = item.name,
                                            questionCount = 15,
                                            timeLimitMinutes = 20
                                        )
                                    )
                                },
                                onPracticeTopic = { topic ->
                                    onStartTestWithConfig(
                                        MockTestConfig(
                                            exam = currentExamName,
                                            testType = MockTestType.TOPIC_TEST,
                                            subject = item.name,
                                            topic = topic,
                                            questionCount = 10,
                                            timeLimitMinutes = 15
                                        )
                                    )
                                },
                                onReviseTopic = { topic ->
                                    onStartFocusOnTopic(item.name, topic)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // --- 7. WEAK AREAS & STRENGTHS ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Weak Areas
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        fillAlpha = 0.88f
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
                                    Icon(Icons.Filled.WarningAmber, null, tint = CoralRose, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHindi) "⚠️ सुधार की आवश्यकता" else "⚠️ Needs Improvement",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (weakAreasList.isEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHindi) "शानदार! कोई कमजोर विषय दर्ज नहीं है।" else "Great work! No critically weak areas identified.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            } else {
                                weakAreasList.forEach { weak ->
                                    WeakAreaRowItem(
                                        item = weak,
                                        isHindi = isHindi,
                                        onPractice = {
                                            onStartTestWithConfig(
                                                MockTestConfig(
                                                    exam = currentExamName,
                                                    testType = MockTestType.WEAK_AREAS,
                                                    subject = weak.subject,
                                                    topic = weak.topic,
                                                    questionCount = 10,
                                                    timeLimitMinutes = 15
                                                )
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }

                    // Strong Areas
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        fillAlpha = 0.88f
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
                                    Icon(Icons.Filled.Star, null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHindi) "⭐ आपकी ताकत" else "⭐ Your Strengths",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (strongAreasList.isEmpty()) {
                                Text(
                                    text = if (isHindi) "अधिक टेस्ट दें ताकि आपकी मजबूतियाँ पहचानी जा सकें।" else "Complete more quizzes to identify your consistent strengths.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            } else {
                                strongAreasList.forEach { strong ->
                                    StrongAreaRowItem(item = strong)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // --- 8. QUIZ & STUDY PERFORMANCE SUMMARY ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quiz Performance Compact Group
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        fillAlpha = 0.85f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = if (isHindi) "क्विज़ आंकड़े" else "Quiz Stats",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val bestScore = if (filteredAttempts.isNotEmpty()) filteredAttempts.maxOf { it.accuracyPercent }.toInt() else null
                            val recentScore = filteredAttempts.lastOrNull()?.accuracyPercent?.toInt()

                            CompactStatRow("Total Tests", "$totalTestsCount")
                            CompactStatRow("Avg Accuracy", if (averageAccuracy != null) "${averageAccuracy.toInt()}%" else "N/A")
                            CompactStatRow("Best Score", if (bestScore != null) "$bestScore%" else "N/A")
                            CompactStatRow("Recent Score", if (recentScore != null) "$recentScore%" else "N/A")
                        }
                    }

                    // Study Performance Compact Group
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        fillAlpha = 0.85f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = if (isHindi) "अध्ययन आंकड़े" else "Study Stats",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ElectricViolet
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val avgSession = if (filteredStudySessions.isNotEmpty()) {
                                (filteredStudySessions.sumOf { it.actualMinutesSpent } / filteredStudySessions.size)
                            } else 0

                            CompactStatRow("Total Time", totalStudyTimeFormatted)
                            CompactStatRow("Sessions", "${filteredStudySessions.size}")
                            CompactStatRow("Avg Session", "${avgSession}m")
                            CompactStatRow("Streak", "${user?.streakDays ?: 1} days")
                        }
                    }
                }
            }

            // --- 9. DAILY TARGET & WEEKLY SUMMARY ---
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    fillAlpha = 0.85f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "दैनिक अध्ययन लक्ष्य" else "Daily Study Target",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            val todayHours = todayStudyMinutes / 60
                            val todayMins = todayStudyMinutes % 60
                            val targetHours = dailyTargetMinutes / 60
                            val targetMins = dailyTargetMinutes % 60
                            Text(
                                text = "${todayHours}h ${todayMins}m / ${targetHours}h ${targetMins}m",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val targetProgress = if (dailyTargetMinutes > 0) {
                            (todayStudyMinutes.toFloat() / dailyTargetMinutes.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        LinearProgressIndicator(
                            progress = { targetProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (targetProgress >= 1f) EmeraldSuccess else NeonCyan,
                            trackColor = Color(0x301E293B)
                        )
                    }
                }
            }

            // --- 10. ✨ NOVA PERFORMANCE ANALYSIS ---
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_nova_analysis_card"),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = 0.95f
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
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(NeonCyan, ElectricViolet))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF050814),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "✨ NOVA ANALYSIS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    Text(
                                        text = if (isHindi) "एआई तैयारी मूल्यांकन" else "AI Preparation Intelligence",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            if (onGenerateNovaProgressAnalysis != null) {
                                OutlinedButton(
                                    onClick = {
                                        val summary = "Target Exam: $currentExamName. Tests Taken: $totalTestsCount. Accuracy: ${averageAccuracy?.toInt() ?: "N/A"}%. Study Time: $totalStudyTimeFormatted. Weak Areas: ${weakAreasList.map { "${it.subject} (${it.topic})" }.joinToString()}. Strong Areas: ${strongAreasList.map { "${it.subject} (${it.topic})" }.joinToString()}."
                                        onGenerateNovaProgressAnalysis(currentExamName, summary, if (isHindi) "Hindi" else "English")
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    enabled = !isNovaProgressAnalyzing
                                ) {
                                    if (isNovaProgressAnalyzing) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = NeonCyan)
                                    } else {
                                        Text(
                                            text = if (novaProgressAnalysis != null) (if (isHindi) "रिफ्रेश" else "Refresh") else (if (isHindi) "विश्लेषण करें" else "Analyze"),
                                            color = NeonCyan,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isNovaProgressAnalyzing) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isHindi) "NOVA आपके प्रदर्शन का मूल्यांकन कर रहा है..." else "NOVA is analyzing your real preparation metrics...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        } else if (novaProgressAnalysis != null) {
                            Text(
                                text = novaProgressAnalysis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 20.sp
                            )
                        } else {
                            // Default deterministic intelligence summary based on actual user data
                            val strongest = strongAreasList.firstOrNull()?.subject ?: examSubjects.firstOrNull() ?: "General"
                            val weakest = weakAreasList.firstOrNull()?.subject
                            val accuracyStr = if (averageAccuracy != null) "${averageAccuracy.toInt()}%" else "in testing"

                            val defaultAnalysis = if (weakest != null) {
                                if (isHindi)
                                    "आपका मजबूत क्षेत्र $strongest ($accuracyStr सटीकता) है। आगामी दिनों में $weakest पर अधिक अभ्यास और मॉक टेस्ट देने की सिफारिश की जाती है।"
                                else
                                    "Your strongest area is $strongest with $accuracyStr accuracy. We recommend dedicating targeted revision and practice sessions to $weakest to boost overall readiness."
                            } else {
                                if (isHindi)
                                    "आपकी तैयारी संतुलित चल रही है। नियमित मॉक टेस्ट देकर अपनी गति और सटीकता को बनाए रखें।"
                                else
                                    "Your preparation is on track. Maintain regular daily study hours and take frequent full-length mocks to strengthen speed and timing."
                            }

                            Text(
                                text = defaultAnalysis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // --- 11. 🎯 RECOMMENDED NEXT STEP ---
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_recommended_next_step_card"),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = 0.95f
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
                                Icon(
                                    imageVector = Icons.Filled.AdsClick,
                                    contentDescription = null,
                                    tint = GoldenSpark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "🎯 अनुशंसित अगला कदम" else "🎯 Recommended Next Step",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldenSpark
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CoralRose.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = recommendedNextStep.badge,
                                    color = CoralRose,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = recommendedNextStep.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = recommendedNextStep.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                when (recommendedNextStep.actionType) {
                                    ActionType.PRACTICE_TOPIC -> {
                                        onStartTestWithConfig(
                                            MockTestConfig(
                                                exam = currentExamName,
                                                testType = MockTestType.TOPIC_TEST,
                                                subject = recommendedNextStep.subject,
                                                topic = recommendedNextStep.topic,
                                                questionCount = 10,
                                                timeLimitMinutes = 15
                                            )
                                        )
                                    }
                                    ActionType.MISTAKE_BOOK -> {
                                        selectedSubTab = 3
                                    }
                                    ActionType.FOCUS_STUDY -> {
                                        onStartFocusOnTopic(recommendedNextStep.subject, recommendedNextStep.topic)
                                    }
                                    ActionType.MOCK_TEST -> {
                                        showSetupDialog = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("progress_start_next_step_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isHindi) "अभी शुरू करें" else "Start Now",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        } else if (selectedSubTab == 1) {
            // --- SUBTAB 1: SMART REVISION QUEUE ---
            item {
                SmartRevisionQueueView(
                    queue = revisionQueue,
                    onStartRevisionSession = { item ->
                        onStartFocusOnTopic(item.subject, item.topic)
                    },
                    onStartQuickRevisionTest = { item, count ->
                        onStartTestWithConfig(
                            MockTestConfig(
                                exam = currentExamName,
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
        } else if (selectedSubTab == 2) {
            // --- SUBTAB 2: SYLLABUS MASTERY & OBJECTIVES ---
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
            // --- SUBTAB 3: MISTAKE BOOK & DIAGNOSTICS ---
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
            defaultSubject = examSubjects.firstOrNull() ?: "Physics",
            onDismiss = { showMaterialManager = false },
            onSaveMaterial = onSaveUserMaterial,
            onDeleteMaterial = onDeleteUserMaterial
        )
    }

    // Generating loading indicator with cancellation
    if (isTestGenerating) {
        Dialog(onDismissRequest = onCancelTestGeneration) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.96f
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isHindi) "मॉक टेस्ट तैयार हो रहा है..." else "Preparing Your Mock Test...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) "सटीक PYQs और परीक्षा पैटर्न प्रश्न संकलित किए जा रहे हैं" else "Assembling authentic PYQs & CBT exam drill",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onCancelTestGeneration,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0x35FFFFFF)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Cancel Preparation", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    // Insufficient PYQ Alert Notice Dialog
    if (insufficientPyqNotice != null) {
        val notice = insufficientPyqNotice
        AlertDialog(
            onDismissRequest = onDismissInsufficientPyqNotice,
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.School, null, tint = GoldenSpark, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PYQ Availability Notice", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Found ${notice.availableCount} authentic PYQ questions for ${notice.examName} • ${notice.subject} (Requested: ${notice.requestedCount}).",
                        color = Color(0xFFE2E8F0),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "How would you like to proceed with this test session?",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onConfirmAddAiToPyqs,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF050814)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add AI Exam Questions (Total ${notice.requestedCount})", fontWeight = FontWeight.Bold)
                    }

                    if (notice.availableCount > 0) {
                        OutlinedButton(
                            onClick = onConfirmStartWithAvailablePyqs,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldenSpark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start with ${notice.availableCount} Available PYQs", color = GoldenSpark, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = onDismissInsufficientPyqNotice,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Test Filters", color = Color(0xFF94A3B8))
                    }
                }
            }
        )
    }

    // Generation Error Dialog
    if (generationError != null) {
        val err = generationError
        AlertDialog(
            onDismissRequest = onClearGenerationError,
            containerColor = Color(0xFF1E1428),
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = CoralRose, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Preparation Notice", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = err.userMessage,
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (err.canRetry) {
                        Button(
                            onClick = {
                                onClearGenerationError()
                                showSetupDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF050814)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = {
                            onClearGenerationError()
                            showSetupDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x30FFFFFF), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Configure Test")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onClearGenerationError) {
                    Text("Dismiss", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

// --- SUPPORTING SUB-COMPOSABLES ---

@Composable
private fun PerformanceMetricTile(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0x351E293B),
        border = BorderStroke(1.dp, Color(0x20FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = if (value.length > 8) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PerformanceTrendPlot(
    attempts: List<MockTestAttempt>,
    modifier: Modifier = Modifier
) {
    val scores = remember(attempts) { attempts.map { it.accuracyPercent.toInt() } }
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            attempts.takeLast(7).forEachIndexed { idx, attempt ->
                val score = attempt.accuracyPercent.toInt()
                val barHeightFraction = (score.toFloat() / 100f).coerceIn(0.1f, 1f)
                val barColor = if (score >= 70) EmeraldSuccess else if (score >= 50) GoldenSpark else CoralRose

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$score%",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .fillMaxHeight(barHeightFraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(barColor, barColor.copy(alpha = 0.4f))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dateFormat.format(Date(attempt.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyTimeDailyBarChart(
    sessions: List<StudentSessionHistory>,
    modifier: Modifier = Modifier
) {
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val calendar = Calendar.getInstance()

    // Group last 7 days study minutes
    val dailyMinutes = remember(sessions) {
        val list = mutableListOf<Pair<String, Int>>()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dayLabel = dayFormat.format(cal.time)
            val startOfDay = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val endOfDay = startOfDay + (24 * 3600 * 1000L)

            val mins = sessions.filter { it.timestamp in startOfDay until endOfDay }
                .sumOf { it.actualMinutesSpent }
            list.add(dayLabel to mins)
        }
        list
    }

    val maxMins = remember(dailyMinutes) {
        dailyMinutes.maxOf { it.second }.coerceAtLeast(60)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        dailyMinutes.forEach { (day, mins) ->
            val fraction = (mins.toFloat() / maxMins.toFloat()).coerceIn(0.08f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (mins > 0) "${mins}m" else "0",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (mins > 0) ElectricViolet else Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (mins > 0) Brush.verticalGradient(listOf(ElectricViolet, NeonCyan))
                            else Brush.verticalGradient(listOf(Color(0x301E293B), Color(0x201E293B)))
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun SubjectPerformanceRow(
    item: SubjectProgressItem,
    isExpanded: Boolean,
    isHindi: Boolean,
    onToggleExpand: () -> Unit,
    onPracticeSubject: () -> Unit,
    onPracticeTopic: (String) -> Unit,
    onReviseTopic: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x281E293B),
        border = BorderStroke(1.dp, Color(0x18FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (item.testsTaken > 0) "${item.testsTaken} ${if (isHindi) "टेस्ट" else "tests"} • ${item.questionsSolved} ${if (isHindi) "प्रश्न" else "questions"}" else (if (isHindi) "अभी तक परीक्षण नहीं हुआ" else "Not tested yet"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.accuracy != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (item.accuracy >= 70) EmeraldSuccess.copy(alpha = 0.15f) else GoldenSpark.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${item.accuracy.toInt()}%",
                                color = if (item.accuracy >= 70) EmeraldSuccess else GoldenSpark,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = if (isHindi) "N/A" else "Not tested",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Accuracy progress indicator
            if (item.accuracy != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (item.accuracy / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (item.accuracy >= 70) EmeraldSuccess else GoldenSpark,
                    trackColor = Color(0x20FFFFFF)
                )
            }

            // Expandable Topic Level Breakdown
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(color = Color(0x18FFFFFF))

                    if (item.topics.isEmpty()) {
                        Text(
                            text = if (isHindi) "इस विषय के लिए कोई टॉपिक रिकॉर्ड नहीं मिला।" else "No topic data recorded yet for this subject.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        item.topics.forEach { topic ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = topic.topic,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFE2E8F0)
                                    )
                                    Text(
                                        text = "Mastery: ${topic.masteryScore}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (topic.masteryScore >= 70) EmeraldSuccess else CoralRose,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onReviseTopic(topic.topic) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f)),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Revise", style = MaterialTheme.typography.labelSmall, color = ElectricViolet, fontSize = 10.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { onPracticeTopic(topic.topic) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Practice", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Practice Entire Subject Button
                    FilledTonalButton(
                        onClick = onPracticeSubject,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = ElectricIndigo.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (isHindi) "${item.name} का विषय टेस्ट दें" else "Take Subject Test in ${item.name}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeakAreaRowItem(
    item: WeakAreaInfo,
    isHindi: Boolean,
    onPractice: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x301E293B),
        border = BorderStroke(1.dp, CoralRose.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.subject} • ${item.topic}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = item.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = CoralRose,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onPractice,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralRose.copy(alpha = 0.85f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = if (isHindi) "अभ्यास" else "Practice",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StrongAreaRowItem(item: StrongAreaInfo) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x301E293B),
        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.subject} • ${item.topic}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldSuccess,
                    fontSize = 11.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldSuccess.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "${item.score}%",
                    color = EmeraldSuccess,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// Data helper representations
data class SubjectProgressItem(
    val name: String,
    val accuracy: Float?,
    val testsTaken: Int,
    val questionsSolved: Int,
    val masteryScore: Int?,
    val topics: List<TopicMastery> = emptyList()
)

data class WeakAreaInfo(
    val subject: String,
    val topic: String,
    val score: Int,
    val sampleCount: Int,
    val reason: String
)

data class StrongAreaInfo(
    val subject: String,
    val topic: String,
    val score: Int,
    val subtitle: String
)

enum class ActionType {
    PRACTICE_TOPIC,
    MISTAKE_BOOK,
    FOCUS_STUDY,
    MOCK_TEST
}

data class RecommendedAction(
    val title: String,
    val subtitle: String,
    val badge: String,
    val actionType: ActionType,
    val subject: String,
    val topic: String
)

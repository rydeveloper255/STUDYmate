package com.example.ui.screens.progress

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MockTestAttempt
import com.example.data.model.QuestionAttemptDetail
import com.example.service.intelligence.SmartPracticeRecommendation
import com.example.service.intelligence.TestIntelligenceResult
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.PersistenceStatusIndicator
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun MockTestResultScreen(
    attempt: MockTestAttempt,
    detailedQuestions: List<QuestionAttemptDetail>,
    testIntelligence: TestIntelligenceResult? = null,
    isNovaAnalyzing: Boolean = false,
    onExitToDashboard: () -> Unit,
    onRetake: () -> Unit,
    onRetryIncorrect: (() -> Unit)? = null,
    onRetryUnanswered: (() -> Unit)? = null,
    onStartPractice: ((SmartPracticeRecommendation) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Incorrect, 2: Correct, 3: Skipped
    var selectedSubjectTab by remember { mutableStateOf<String?>(null) }
    var isSolutionsExpanded by remember { mutableStateOf(true) }

    fun exportAndSharePdf() {
        try {
            val pdfFile = PdfReportGenerator.generateMockTestPdf(context, attempt)
            PdfReportGenerator.sharePdfReport(
                context = context,
                pdfFile = pdfFile,
                shareTitle = "${attempt.examName} Mock Test Report - ${attempt.subject}"
            )
            Toast.makeText(context, "PDF Report generated successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val filteredDetails = remember(detailedQuestions, selectedFilter, selectedSubjectTab) {
        var list = detailedQuestions
        if (selectedSubjectTab != null) {
            list = list.filter { it.question.subject == selectedSubjectTab }
        }
        when (selectedFilter) {
            1 -> list.filter { it.selectedIndex != null && !it.isCorrect }
            2 -> list.filter { it.isCorrect }
            3 -> list.filter { it.selectedIndex == null }
            else -> list
        }
    }

    val totalMins = attempt.timeSpentSeconds / 60
    val totalSecs = attempt.timeSpentSeconds % 60
    val formattedTotalTime = if (totalMins > 0) "${totalMins}m ${totalSecs}s" else "${totalSecs}s"
    val avgTime = attempt.avgTimePerQuestionSeconds.toInt()

    val activePersistenceStatus by com.example.data.persistence.PersistenceMonitor.activeStatus.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mock_test_result_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PersistenceStatusIndicator(status = activePersistenceStatus)
        }

        // 1. RESULT HEADER & OVERALL SCORE DASHBOARD
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                fillAlpha = 0.92f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top row badge + Export PDF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeonCyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = attempt.examName.ifBlank { "Full Mock Test" },
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (attempt.language.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x20FFFFFF)
                                ) {
                                    Text(
                                        text = attempt.language,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { exportAndSharePdf() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x2238BDF8))
                                .testTag("export_pdf_top_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = "Export PDF Report",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Performance Category Trophy Icon & Badge
                    val category = testIntelligence?.performanceCategory
                    val categoryBadgeColor = when (category?.level) {
                        "Excellent" -> GoldenSpark
                        "Strong" -> EmeraldSuccess
                        "Good" -> NeonCyan
                        "Improving" -> ElectricViolet
                        else -> CoralRose
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(categoryBadgeColor.copy(alpha = 0.8f), Color(0xFF050814))
                                )
                            )
                            .border(2.dp, categoryBadgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category?.emoji ?: if (attempt.accuracyPercent >= 75) "🏆" else "📈",
                            fontSize = 28.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Performance: ${category?.level ?: if (attempt.accuracyPercent >= 75) "Strong" else "Improving"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        text = category?.description ?: "Keep practicing to master all core exam concepts!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Score Metrics Grid (Marks, Accuracy, Time Taken, Avg/Question)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScoreMetricBadge(
                            label = "Score",
                            value = "${attempt.score}",
                            color = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )
                        ScoreMetricBadge(
                            label = "Accuracy",
                            value = "${attempt.accuracyPercent.toInt()}%",
                            color = if (attempt.accuracyPercent >= 70) EmeraldSuccess else CoralRose,
                            modifier = Modifier.weight(1f)
                        )
                        ScoreMetricBadge(
                            label = "Time Used",
                            value = formattedTotalTime,
                            color = GoldenSpark,
                            modifier = Modifier.weight(1f)
                        )
                        ScoreMetricBadge(
                            label = "Avg/Question",
                            value = "${avgTime}s",
                            color = ElectricViolet,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Breakdown Row: Correct / Incorrect / Unanswered / Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusCountChip(
                            label = "Correct",
                            count = attempt.correctCount,
                            color = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        StatusCountChip(
                            label = "Incorrect",
                            count = attempt.incorrectCount,
                            color = CoralRose,
                            modifier = Modifier.weight(1f)
                        )
                        StatusCountChip(
                            label = "Skipped",
                            count = attempt.skippedCount,
                            color = Color(0xFF64748B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. NOVA AI POST-TEST INSIGHT CARD
        item {
            val novaInsight = testIntelligence?.novaInsight
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.88f
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOVA Study Coach Insights",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark
                            )
                        }

                        if (isNovaAnalyzing) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldenSpark.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = GoldenSpark,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Analyzing...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldenSpark
                                    )
                                }
                            }
                        } else if (novaInsight?.isAiGenerated == true) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "AI Enhanced",
                                    color = EmeraldSuccess,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // What Went Well
                    Text(
                        text = "What Went Well 🎉",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    (novaInsight?.whatWentWell ?: listOf("Good effort in completing the test under time limits.")).forEach { point ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Needs Practice
                    Text(
                        text = "Needs Practice 🎯",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoralRose
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    (novaInsight?.whatNeedsPractice ?: listOf("Targeted revision on incorrect topics recommended.")).forEach { point ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", color = CoralRose, fontWeight = FontWeight.Bold)
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recommended Action Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NeonCyan.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Lightbulb, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Recommended Next Action",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Text(
                                    text = novaInsight?.recommendedNextStep ?: "Try a 15-question targeted set on weak areas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. SUBJECT-WISE & CHAPTER-WISE PERFORMANCE ANALYSIS
        val subjectPerformances = testIntelligence?.subjectPerformances ?: emptyList()
        val chapterPerformances = testIntelligence?.chapterPerformances ?: emptyList()

        if (subjectPerformances.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Subject & Chapter Performance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    subjectPerformances.forEach { sub ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .springClickable {
                                    selectedSubjectTab = if (selectedSubjectTab == sub.subject) null else sub.subject
                                },
                            shape = RoundedCornerShape(16.dp),
                            fillAlpha = if (selectedSubjectTab == sub.subject) 0.95f else 0.8f
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = sub.subject,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (selectedSubjectTab == sub.subject) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = NeonCyan.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "Filtered",
                                                    color = NeonCyan,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "${sub.accuracyPercent.toInt()}% Accuracy",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sub.accuracyPercent >= 70) EmeraldSuccess else CoralRose
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Progress bar
                                LinearProgressIndicator(
                                    progress = { (sub.accuracyPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = if (sub.accuracyPercent >= 70) EmeraldSuccess else CoralRose,
                                    trackColor = Color(0x20FFFFFF)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Correct: ${sub.correct} / ${sub.totalQuestions}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSuccess
                                    )
                                    Text(
                                        text = "Incorrect: ${sub.incorrect}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CoralRose
                                    )
                                    Text(
                                        text = "Avg Time: ${sub.avgTimePerQuestionSeconds.toInt()}s/q",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldenSpark
                                    )
                                }
                            }
                        }
                    }

                    // Chapter breakdown cards
                    if (chapterPerformances.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Chapter Level Breakdown",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )

                        chapterPerformances.take(6).forEach { chap ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x12FFFFFF),
                                border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chap.chapter,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (!chap.hasSufficientData) {
                                            Text(
                                                text = "Limited data — practice more questions on this topic",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GoldenSpark
                                            )
                                        } else {
                                            Text(
                                                text = "${chap.correct} correct out of ${chap.totalQuestions} questions",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${chap.accuracyPercent.toInt()}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (chap.accuracyPercent >= 70) EmeraldSuccess else CoralRose
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. TIME MANAGEMENT & PATTERN ANALYSIS
        val timeMatrix = testIntelligence?.timeMatrix
        if (timeMatrix != null) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = 0.85f
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Timer, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Time Management Analysis",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fast vs Longest Question row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldSuccess.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Fastest Question", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                    Text(
                                        text = "Q${timeMatrix.fastestQuestionIndex + 1} (${timeMatrix.fastestTimeSeconds}s)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CoralRose.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Longest Question", style = MaterialTheme.typography.labelSmall, color = CoralRose)
                                    Text(
                                        text = "Q${timeMatrix.longestQuestionIndex + 1} (${timeMatrix.longestTimeSeconds}s)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Pattern Matrix Breakdown
                        Text(
                            text = "Solving Speed vs Accuracy Pattern:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PatternCountBadge(
                                label = "High Time + Wrong",
                                count = timeMatrix.highTimeWrongCount,
                                subtitle = "Inflexible solving",
                                color = CoralRose,
                                modifier = Modifier.weight(1f)
                            )
                            PatternCountBadge(
                                label = "Low Time + Wrong",
                                count = timeMatrix.lowTimeWrongCount,
                                subtitle = "Careless errors",
                                color = GoldenSpark,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PatternCountBadge(
                                label = "High Time + Right",
                                count = timeMatrix.highTimeCorrectCount,
                                subtitle = "Time consuming",
                                color = NeonCyan,
                                modifier = Modifier.weight(1f)
                            )
                            PatternCountBadge(
                                label = "Low Time + Right",
                                count = timeMatrix.lowTimeCorrectCount,
                                subtitle = "Peak efficiency",
                                color = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Neutral Advice
                        Text(
                            text = timeMatrix.neutralAdvice,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // 5. SMART PRACTICE LOOPS & NEXT BEST ACTIONS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Personalized Practice Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Recommended Practice Buttons
                if (onRetryIncorrect != null && attempt.incorrectCount > 0) {
                    Button(
                        onClick = onRetryIncorrect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldenSpark,
                            contentColor = Color(0xFF050814)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("retake_wrong_questions_btn")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retry Incorrect Questions (${attempt.incorrectCount})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }

                if (onRetryUnanswered != null && attempt.skippedCount > 0) {
                    OutlinedButton(
                        onClick = onRetryUnanswered,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("retry_unanswered_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Solve Skipped Questions (${attempt.skippedCount})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val recommendations = testIntelligence?.recommendations ?: emptyList()
                recommendations.forEach { rec ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .springClickable { onStartPractice?.invoke(rec) },
                        shape = RoundedCornerShape(14.dp),
                        fillAlpha = 0.8f
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    tint = GoldenSpark,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = rec.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = rec.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. QUESTION-BY-QUESTION SOLUTION REVIEW SECTION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Question Solutions (${filteredDetails.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { isSolutionsExpanded = !isSolutionsExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isSolutionsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle solutions",
                            tint = NeonCyan
                        )
                    }
                }

                // Quick Filters: All, Wrong, Right, Skipped
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("All", "Wrong", "Right", "Skipped").forEachIndexed { idx, label ->
                        val isSel = selectedFilter == idx
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) NeonCyan else Color(0x18FFFFFF),
                            modifier = Modifier.springClickable { selectedFilter = idx }
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color(0xFF050814) else Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isSolutionsExpanded) {
            itemsIndexed(filteredDetails) { idx, detail ->
                val q = detail.question
                val isCorrect = detail.isCorrect
                val isSkipped = detail.selectedIndex == null

                val statusColor = when {
                    isCorrect -> EmeraldSuccess
                    isSkipped -> Color(0xFF94A3B8)
                    else -> CoralRose
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    fillAlpha = 0.8f
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusColor.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "Q${idx + 1}: ${if (isCorrect) "Correct (+4)" else if (isSkipped) "Skipped (0)" else "Incorrect (-1)"}",
                                        color = statusColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${detail.timeSpentSeconds}s",
                                    color = Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            QuestionSourceBadge(source = q.source, label = q.sourceLabel, yearOrTag = q.yearOrTag)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = q.questionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        q.options.forEachIndexed { optIdx, optText ->
                            val isStudentChoice = detail.selectedIndex == optIdx
                            val isCorrectOption = q.correctOptionIndex == optIdx

                            val optionBg = when {
                                isCorrectOption -> EmeraldSuccess.copy(alpha = 0.2f)
                                isStudentChoice && !isCorrect -> CoralRose.copy(alpha = 0.2f)
                                else -> Color(0x10FFFFFF)
                            }

                            val optionBorder = when {
                                isCorrectOption -> EmeraldSuccess
                                isStudentChoice && !isCorrect -> CoralRose
                                else -> Color(0x18FFFFFF)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(optionBg)
                                    .border(1.dp, optionBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${('A' + optIdx)}. $optText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isCorrectOption) Color.White else Color(0xFFCBD5E1),
                                        fontWeight = if (isCorrectOption || isStudentChoice) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isCorrectOption) {
                                        Text("✅ Correct", color = EmeraldSuccess, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    } else if (isStudentChoice) {
                                        Text("❌ Your Answer", color = CoralRose, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (q.explanation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x1838BDF8)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Lightbulb, null, tint = GoldenSpark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Step-by-Step Solution & Principle:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldenSpark
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = q.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE2E8F0),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. BOTTOM ACTION BUTTONS
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { exportAndSharePdf() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x2838BDF8),
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_pdf_report_btn")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Official PDF Report", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("retake_test_btn")
                    ) {
                        Icon(Icons.Filled.Replay, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retake Test")
                    }

                    Button(
                        onClick = onExitToDashboard,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color(0xFF050814)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("exit_to_dashboard_btn")
                    ) {
                        Text("Dashboard", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreMetricBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Text(
                text = value,
                color = color,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun StatusCountChip(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0x18FFFFFF),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: $count",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PatternCountBadge(
    label: String,
    count: Int,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }
    }
}

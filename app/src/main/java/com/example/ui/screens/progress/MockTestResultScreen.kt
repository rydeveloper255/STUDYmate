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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MockTestAttempt
import com.example.data.model.QuestionAttemptDetail
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun MockTestResultScreen(
    attempt: MockTestAttempt,
    detailedQuestions: List<QuestionAttemptDetail>,
    onExitToDashboard: () -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Incorrect, 2: Correct, 3: Skipped
    var expandedQuestionIndex by remember { mutableStateOf<Int?>(null) }

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

    val filteredDetails = remember(detailedQuestions, selectedFilter) {
        when (selectedFilter) {
            1 -> detailedQuestions.filter { it.selectedIndex != null && !it.isCorrect }
            2 -> detailedQuestions.filter { it.isCorrect }
            3 -> detailedQuestions.filter { it.selectedIndex == null }
            else -> detailedQuestions
        }
    }

    val totalMins = attempt.timeSpentSeconds / 60
    val totalSecs = attempt.timeSpentSeconds % 60
    val formattedTotalTime = if (totalMins > 0) "${totalMins}m ${totalSecs}s" else "${totalSecs}s"
    val avgTime = attempt.avgTimePerQuestionSeconds.toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mock_test_result_screen"),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Result Header & Score Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                fillAlpha = 0.9f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = attempt.examName,
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Export PDF Action
                        IconButton(
                            onClick = { exportAndSharePdf() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x2238BDF8))
                                .testTag("export_pdf_top_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = "Export PDF Report",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (attempt.accuracyPercent >= 75) Brush.linearGradient(listOf(GoldenSpark, NeonCyan))
                                else Brush.linearGradient(listOf(CoralRose, ElectricViolet))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (attempt.accuracyPercent >= 75) Icons.Filled.EmojiEvents else Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = Color(0xFF050814),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (attempt.accuracyPercent >= 80) "Exceptional Performance! 🎉"
                        else if (attempt.accuracyPercent >= 50) "Good Effort! Keep Pushing 🚀"
                        else "Needs Targeted Practice 📚",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = attempt.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScoreMetricBadge(
                            label = "Marks",
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
                            label = "Time Taken",
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Breakdown Bars: Correct / Incorrect / Skipped
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

        // 2. AI Recommendation & Topic Diagnosis Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                fillAlpha = 0.85f
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Performance Evaluation",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSpark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = attempt.aiRecommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 20.sp
                    )

                    if (attempt.weakTopics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Weak Topics Identified (Added to Mistake Book):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CoralRose
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            attempt.weakTopics.take(3).forEach { topic ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CoralRose.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, CoralRose.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = topic,
                                        color = CoralRose,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Question-by-Question Solution Review Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detailed Solutions Review (${detailedQuestions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Quick Filter
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("All", "Wrong", "Right").forEachIndexed { idx, label ->
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

        // List of question reviews
        itemsIndexed(filteredDetails) { idx, detail ->
            val q = detail.question
            val isExpanded = expandedQuestionIndex == idx
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
                    // Header: Q Number + Status + Source Tag + Time Spent
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

                        // Exact Source Label
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

                    // Options Review
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

                    // Explanation toggle
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

        // 4. Return to Dashboard, Export PDF & Retake Buttons
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
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(text = value, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = label, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun StatusCountChip(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x18FFFFFF),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp)
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

package com.example.ui.screens.progress

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MistakeItem
import com.example.data.model.MockTestAttempt
import com.example.data.model.UserProfile
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun MockTestAnalyticsView(
    attempts: List<MockTestAttempt>,
    user: UserProfile? = null,
    mistakes: List<MistakeItem> = emptyList(),
    focusMinutes: Int = 0,
    onLaunchNewTest: () -> Unit,
    onReviewTest: (MockTestAttempt) -> Unit,
    onRetakeTest: (MockTestAttempt) -> Unit,
    onDeleteTest: (Long) -> Unit,
    onManageMaterials: () -> Unit,
    onOpenQuestionBankExplorer: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val totalAttempts = attempts.size
    val avgAccuracy = if (attempts.isNotEmpty()) attempts.map { it.accuracyPercent }.average().toInt() else 0
    val totalQuestionsSolved = attempts.sumOf { it.totalQuestions }
    val totalScore = attempts.sumOf { it.score }
    val avgTimePerQ = if (attempts.isNotEmpty()) attempts.map { it.avgTimePerQuestionSeconds }.average().toInt() else 0

    fun exportOverallAnalyticsPdf() {
        try {
            val pdfFile = PdfReportGenerator.generateAnalyticsSummaryPdf(
                context = context,
                user = user,
                attempts = attempts,
                mistakes = mistakes,
                focusMinutes = focusMinutes,
                studyStreak = user?.streakDays ?: 1
            )
            PdfReportGenerator.sharePdfReport(
                context = context,
                pdfFile = pdfFile,
                shareTitle = "StudyMate AI - Academic Analytics Summary"
            )
            Toast.makeText(context, "Analytics Portfolio PDF generated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate analytics PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. High Level Performance Overview Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = "Tests Taken",
                value = "$totalAttempts",
                subtitle = "Completed mocks",
                color = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = "Avg Accuracy",
                value = if (totalAttempts > 0) "$avgAccuracy%" else "N/A",
                subtitle = "Across all subjects",
                color = if (avgAccuracy >= 70) EmeraldSuccess else GoldenSpark,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = "Avg Speed",
                value = if (totalAttempts > 0) "${avgTimePerQ}s" else "N/A",
                subtitle = "Per question",
                color = ElectricViolet,
                modifier = Modifier.weight(1f)
            )
        }

        // Action Row: Export Portfolio PDF + Explore QB + Manage Questions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { exportOverallAnalyticsPdf() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x256366F1),
                    contentColor = Color(0xFFA5B4FC)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x55818CF8)),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("export_analytics_pdf_btn")
            ) {
                Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            if (onOpenQuestionBankExplorer != null) {
                Button(
                    onClick = onOpenQuestionBankExplorer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x2510B981),
                        contentColor = EmeraldSuccess
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("open_qb_explorer_btn")
                ) {
                    Icon(Icons.Filled.MenuBook, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Question Bank", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onManageMaterials,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x3538BDF8)),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("manage_custom_materials_btn")
            ) {
                Icon(Icons.Filled.Description, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Materials", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
            }
        }

        // 2. Launch New Test Action Banner
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            fillAlpha = 0.9f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Timed Exam Simulator",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Real PYQs, custom notes, or Gemini AI questions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onLaunchNewTest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF050814)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("launch_new_mock_test_banner_btn")
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Test", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Score & Accuracy Improvement Trend Card
        if (attempts.isNotEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.85f
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ShowChart, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📈 Improvement Over Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x2038BDF8)
                        ) {
                            Text(
                                text = "Last ${attempts.take(6).size} Tests",
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Visual bar trend for recent attempts
                    val recentAttempts = attempts.take(6).reversed()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        recentAttempts.forEachIndexed { idx, att ->
                            val heightFraction = (att.accuracyPercent / 100f).coerceIn(0.1f, 1f)
                            val barColor = when {
                                att.accuracyPercent >= 80 -> EmeraldSuccess
                                att.accuracyPercent >= 50 -> NeonCyan
                                else -> CoralRose
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${att.accuracyPercent.toInt()}%",
                                    color = barColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(barColor, barColor.copy(alpha = 0.4f))
                                            )
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "T${idx + 1}",
                                    color = Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "💡 Trend insight: Consistent timed test practice reduces average question response time by ~24%.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // 4. Performance History List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Performance History (${attempts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            TextButton(onClick = onManageMaterials) {
                Icon(Icons.Filled.UploadFile, null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Custom Materials", color = ElectricViolet, style = MaterialTheme.typography.labelSmall)
            }
        }

        if (attempts.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                fillAlpha = 0.6f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.HistoryEdu, null, tint = NeonCyan, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Mock Tests Attempted Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Take your first timed mock test to view detailed accuracy analysis and score trends.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onLaunchNewTest,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF050814))
                    ) {
                        Text("Launch First Mock Test", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            attempts.forEach { att ->
                PastTestAttemptCard(
                    attempt = att,
                    onReview = { onReviewTest(att) },
                    onRetake = { onRetakeTest(att) },
                    onDelete = { onDeleteTest(att.id) }
                )
            }
        }
    }
}

@Composable
private fun PastTestAttemptCard(
    attempt: MockTestAttempt,
    onReview: () -> Unit,
    onRetake: () -> Unit,
    onDelete: () -> Unit
) {
    val cardContext = LocalContext.current
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attempt.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = "${attempt.examName} • ${attempt.subject} • ${attempt.difficulty}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (attempt.accuracyPercent >= 75) EmeraldSuccess.copy(alpha = 0.2f) else CoralRose.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (attempt.accuracyPercent >= 75) EmeraldSuccess.copy(alpha = 0.5f) else CoralRose.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${attempt.accuracyPercent.toInt()}% Acc",
                        color = if (attempt.accuracyPercent >= 75) EmeraldSuccess else CoralRose,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Marks: ${attempt.score} | Qs: ${attempt.totalQuestions} (✅ ${attempt.correctCount} / ❌ ${attempt.incorrectCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )

                Text(
                    text = "${attempt.timeSpentSeconds / 60}m ${attempt.timeSpentSeconds % 60}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            if (attempt.aiRecommendation.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🤖 ${attempt.aiRecommendation}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onReview,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Visibility, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Solutions", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onRetake,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x258B5CF6), contentColor = ElectricViolet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Replay, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retake", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                // PDF Export button
                IconButton(
                    onClick = {
                        try {
                            val pdfFile = PdfReportGenerator.generateMockTestPdf(cardContext, attempt)
                            PdfReportGenerator.sharePdfReport(
                                context = cardContext,
                                pdfFile = pdfFile,
                                shareTitle = "${attempt.examName} Mock Test Report - ${attempt.subject}"
                            )
                        } catch (e: Exception) {
                            Toast.makeText(cardContext, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1A38BDF8))
                ) {
                    Icon(Icons.Filled.PictureAsPdf, "Export PDF", tint = NeonCyan, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.DeleteOutline, "Delete", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        fillAlpha = 0.85f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                fontSize = 10.sp
            )
        }
    }
}

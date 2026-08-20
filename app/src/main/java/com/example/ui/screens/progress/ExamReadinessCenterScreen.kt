package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamReadinessCenterScreen(
    user: UserProfile?,
    examReadiness: ExamReadinessScore?,
    subjectSummaries: List<SubjectProgressSummary>,
    topicMasteries: List<TopicMastery>,
    mockAttempts: List<MockTestAttempt>,
    onStartFocusSession: (subject: String, topic: String) -> Unit = { _, _ -> },
    onNavigateToMocks: () -> Unit = {},
    onNavigateToRevision: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val examName = examReadiness?.examName?.ifBlank { user?.examName } ?: "Competitive Exam"
    val daysRemaining = remember(user?.examDateMillis) {
        val targetMillis = user?.examDateMillis ?: 0L
        if (targetMillis > System.currentTimeMillis()) {
            ((targetMillis - System.currentTimeMillis()) / (1000L * 3600 * 24)).toInt().coerceAtLeast(1)
        } else 0
    }
    val formattedExamDate = remember(user?.examDateMillis) {
        val targetMillis = user?.examDateMillis ?: 0L
        if (targetMillis > 0) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(targetMillis))
        } else ""
    }

    var selectedSubjectDetail by remember { mutableStateOf<SubjectProgressSummary?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundGradient(isDark))
            .padding(horizontal = 18.dp)
            .testTag("exam_readiness_center_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP HEADER WITH BACK & ACTIVE EXAM
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }

                        Column {
                            Text(
                                text = "🎯 Exam Readiness Center",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Preparing for $examName",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (daysRemaining > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GoldenSpark.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldenSpark.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "⏳ $daysRemaining days left",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. NEAR EXAM MODE BANNER (if active)
        if (examReadiness?.isNearExamMode == true) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x20F59E0B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldenSpark.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GoldenSpark.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Bolt, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Near-Exam Revision Mode Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark
                            )
                            Text(
                                text = "Exam is in $daysRemaining days! Daily target emphasizes timed mock tests, error log analysis, and weak topic revision.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                    }
                }
            }
        }

        // 3. OVERALL READINESS SCORE GAUGE CARD
        item {
            val score = examReadiness?.readinessScore ?: 0
            val status = examReadiness?.status ?: "INSUFFICIENT_DATA"
            val badgeText = examReadiness?.statusBadgeText ?: "Insufficient Data 🌱"
            val explanation = examReadiness?.explanation ?: "Complete initial sessions or tests to see your preparation metrics."

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
                fillAlpha = 0.8f,
                testTag = "overall_readiness_score_card"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREPARATION READINESS INDEX",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (status) {
                                "HIGH_READINESS" -> EmeraldSuccess.copy(alpha = 0.2f)
                                "ON_TRACK" -> NeonCyan.copy(alpha = 0.2f)
                                "NEEDS_ATTENTION" -> CoralRose.copy(alpha = 0.2f)
                                else -> GoldenSpark.copy(alpha = 0.2f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (status) {
                                    "HIGH_READINESS" -> EmeraldSuccess
                                    "ON_TRACK" -> NeonCyan
                                    "NEEDS_ATTENTION" -> CoralRose
                                    else -> GoldenSpark
                                }
                            )
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (status) {
                                    "HIGH_READINESS" -> EmeraldSuccess
                                    "ON_TRACK" -> NeonCyan
                                    "NEEDS_ATTENTION" -> CoralRose
                                    else -> GoldenSpark
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Liquid Ring Gauge
                        LiquidProgressRing(
                            progress = (score / 100f).coerceIn(0f, 1f),
                            currentText = "$score%",
                            targetText = "Score",
                            size = 100.dp,
                            strokeWidth = 10.dp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Readiness: $score / 100",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }

        // 4. READINESS BREAKDOWN (5 Core Components)
        item {
            Text(
                text = "📊 Readiness Component Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Component 1: Syllabus Coverage
                ReadinessComponentRow(
                    title = "Syllabus Coverage",
                    scoreText = "${examReadiness?.syllabusCoveragePercent ?: 0}%",
                    progress = ((examReadiness?.syllabusCoveragePercent ?: 0) / 100f),
                    detailText = "${examReadiness?.masteredTopicsCount ?: 0}/${examReadiness?.totalTopicsCount ?: 0} topics studied",
                    icon = Icons.Filled.MenuBook,
                    color = NeonCyan
                )

                // Component 2: Topic Mastery
                ReadinessComponentRow(
                    title = "Topic Mastery",
                    scoreText = "${examReadiness?.topicMasteryPercent ?: 0}%",
                    progress = ((examReadiness?.topicMasteryPercent ?: 0) / 100f),
                    detailText = "Average accuracy across completed chapters",
                    icon = Icons.Filled.Psychology,
                    color = ElectricIndigo
                )

                // Component 3: Mock Performance
                ReadinessComponentRow(
                    title = "Mock Test Performance",
                    scoreText = if ((examReadiness?.mockPerformancePercent ?: 0) > 0) "${examReadiness?.mockPerformancePercent}%" else "No Mock Yet",
                    progress = ((examReadiness?.mockPerformancePercent ?: 0) / 100f),
                    detailText = if (mockAttempts.isNotEmpty()) "${mockAttempts.size} mock attempts recorded" else "Take a test to calculate accuracy",
                    icon = Icons.Filled.Quiz,
                    color = EmeraldSuccess
                )

                // Component 4: Revision Health
                ReadinessComponentRow(
                    title = "Revision Health",
                    scoreText = "${examReadiness?.revisionHealthPercent ?: 100}%",
                    progress = ((examReadiness?.revisionHealthPercent ?: 100) / 100f),
                    detailText = "${examReadiness?.revisionDueCount ?: 0} topics in revision queue",
                    icon = Icons.Filled.Repeat,
                    color = if ((examReadiness?.revisionDueCount ?: 0) > 3) CoralRose else GoldenSpark
                )

                // Component 5: Study Consistency
                ReadinessComponentRow(
                    title = "Study Consistency",
                    scoreText = "${examReadiness?.consistencyPercent ?: 100}%",
                    progress = ((examReadiness?.consistencyPercent ?: 100) / 100f),
                    detailText = "${user?.streakDays ?: 1} day study streak active",
                    icon = Icons.Filled.LocalFireDepartment,
                    color = GoldenSpark
                )
            }
        }

        // 5. READINESS WARNINGS (if any)
        if (!examReadiness?.warnings.isNullOrEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    fillAlpha = 0.7f,
                    testTag = "readiness_warnings_card"
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = CoralRose, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ATTENTION REQUIRED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CoralRose
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        examReadiness?.warnings?.forEach { warningText ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", color = CoralRose, fontWeight = FontWeight.Bold)
                                Text(
                                    text = warningText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. READINESS ACTION PLAN
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 6.dp,
                fillAlpha = 0.75f,
                testTag = "readiness_action_plan_card"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Checklist, null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RECOMMENDED ACTION PLAN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val planItems = examReadiness?.actionPlan.orEmpty().ifEmpty {
                        listOf(
                            "1. Complete a 20-minute study session on core concepts",
                            "2. Attempt 10 practice questions to evaluate accuracy",
                            "3. Take a quick diagnostic mock test"
                        )
                    }

                    planItems.forEach { stepText ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000)
                        ) {
                            Text(
                                text = stepText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToRevision,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Filled.Repeat, null, tint = Color(0xFF070B19), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Revision", color = Color(0xFF070B19), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onNavigateToMocks,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Filled.Quiz, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Take Mock Test", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 7. SUBJECT-WISE PROGRESS BREAKDOWN
        item {
            Text(
                text = "📚 Subject Mastery & Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (subjectSummaries.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjectSummaries.forEach { sub ->
                        SubjectSummaryCard(
                            summary = sub,
                            isDark = isDark,
                            onClick = { selectedSubjectDetail = sub }
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No subjects linked to current exam context. Select an exam to view subject-wise progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 8. ACCESSIBLE TOPIC STRENGTH HEATMAP
        item {
            Text(
                text = "🔥 Topic Strength Heatmap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            TopicHeatmapSection(
                topicMasteries = topicMasteries,
                isDark = isDark,
                onStartStudy = onStartFocusSession
            )
        }
    }

    // SUBJECT DETAIL MODAL DIALOG
    if (selectedSubjectDetail != null) {
        val sub = selectedSubjectDetail!!
        AlertDialog(
            onDismissRequest = { selectedSubjectDetail = null },
            containerColor = if (isDark) Color(0xFF131C2E) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Book, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sub.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Average Mastery:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        Text("${sub.averageMasteryScore}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Topics Mastered:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        Text("${sub.masteredTopicsCount} / ${sub.totalTopicsCount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Weak Topics:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        Text("${sub.weakTopicsCount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = CoralRose)
                    }

                    Divider(color = Color(0x20FFFFFF), thickness = 1.dp)

                    Text(
                        text = "Mastery vs Coverage Explanation:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldenSpark
                    )

                    Text(
                        text = "Coverage represents how much syllabus content you have opened and studied. Mastery reflects your performance accuracy in tests & quizzes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStartFocusSession(sub.subjectName, "Core Practice")
                        selectedSubjectDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Start Focus Session", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSubjectDetail = null }) {
                    Text("Close", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

// HELPER COMPOSABLE: READINESS COMPONENT ROW
@Composable
private fun ReadinessComponentRow(
    title: String,
    scoreText: String,
    progress: Float,
    detailText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    val isDark = isAppInDarkTheme()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0x18FFFFFF) else Color(0x0A000000),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }

                Text(
                    text = scoreText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = detailText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
    }
}

// HELPER COMPOSABLE: SUBJECT SUMMARY CARD
@Composable
private fun SubjectSummaryCard(
    summary: SubjectProgressSummary,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .springClickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0x20FFFFFF) else Color(0x15000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${summary.masteredTopicsCount} / ${summary.totalTopicsCount} topics mastered • ${summary.weakTopicsCount} weak",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${summary.averageMasteryScore}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        summary.averageMasteryScore >= 75 -> EmeraldSuccess
                        summary.averageMasteryScore >= 50 -> NeonCyan
                        else -> CoralRose
                    }
                )

                Text(
                    text = when {
                        summary.averageMasteryScore >= 75 -> "Strong"
                        summary.averageMasteryScore >= 50 -> "Improving"
                        else -> "Needs Practice"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
    }
}

// HELPER COMPOSABLE: TOPIC HEATMAP SECTION (Accessibility Supported with Labels)
@Composable
private fun TopicHeatmapSection(
    topicMasteries: List<TopicMastery>,
    isDark: Boolean,
    onStartStudy: (subject: String, topic: String) -> Unit
) {
    if (topicMasteries.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Topic strength heatmap will populate as you complete practice questions and study sessions.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        topicMasteries.take(8).forEach { tm ->
            val score = tm.masteryScore
            val (categoryLabel, categoryIcon, color) = when {
                score >= 80 -> Triple("Strong", Icons.Filled.CheckCircle, EmeraldSuccess)
                score >= 60 -> Triple("Good", Icons.Filled.ThumbUp, NeonCyan)
                score >= 35 -> Triple("Needs Practice", Icons.Filled.Build, GoldenSpark)
                tm.totalQuestionsAttempted > 0 -> Triple("Weak", Icons.Filled.Warning, CoralRose)
                else -> Triple("Not Started", Icons.Filled.HourglassEmpty, Color(0xFF94A3B8))
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .springClickable { onStartStudy(tm.subject, tm.topic) },
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = categoryIcon, contentDescription = categoryLabel, tint = color, modifier = Modifier.size(16.dp))
                        Column {
                            Text(
                                text = tm.topic,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${tm.subject} • $categoryLabel ($score%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = color
                            )
                        }
                    }

                    Text(
                        text = "Revise →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }
        }
    }
}

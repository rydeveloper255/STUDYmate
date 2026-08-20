package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun TopicMasteryAndObjectivesView(
    user: UserProfile?,
    examObjective: ExamObjective?,
    topicMasteries: List<TopicMastery>,
    sessionHistory: List<StudentSessionHistory>,
    snapshot: IntelligenceSnapshot?,
    examReadiness: ExamReadinessScore? = null,
    subjectSummaries: List<SubjectProgressSummary> = emptyList(),
    recommendations: List<StudyRecommendation> = emptyList(),
    dailyPlan: DailyStudyPlan? = null,
    onSaveExamObjective: (ExamObjective) -> Unit,
    onStartFocusOnTopic: (subject: String, topic: String) -> Unit,
    onSetManualTopicOverride: (subject: String, topic: String, override: String) -> Unit = { _, _, _ -> },
    onResetPreparationData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var showEditObjectiveDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val subjects = remember(topicMasteries, subjectSummaries) {
        val listFromMasteries = topicMasteries.map { it.subject }.distinct()
        val listFromSummaries = subjectSummaries.map { it.subjectName }
        listOf("All") + (listFromMasteries + listFromSummaries).distinct().filter { it.isNotBlank() }
    }

    val filteredMasteries = remember(topicMasteries, selectedSubjectFilter) {
        if (selectedSubjectFilter == "All") topicMasteries
        else topicMasteries.filter { it.subject.equals(selectedSubjectFilter, ignoreCase = true) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Exam Objective & Target Readiness Index Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            fillAlpha = 0.85f
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "EXAM OBJECTIVE & READINESS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = examReadiness?.examName ?: examObjective?.examName ?: user?.examName ?: "Competitive Exam",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = { showEditObjectiveDialog = true },
                        modifier = Modifier.testTag("edit_exam_objective_btn")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Target", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val readinessVal = examReadiness?.readinessScore ?: snapshot?.readinessIndex?.toInt() ?: 50
                val statusText = examReadiness?.statusBadgeText ?: "Building Mastery ⚡"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Target Rank/Score", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text(
                            text = examObjective?.targetScoreOrRank ?: "Top 500 AIR",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSpark
                        )
                    }

                    Column {
                        Text("Weekly Target", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text(
                            text = "${examObjective?.targetWeeklyStudyHours?.toInt() ?: 25} hrs/wk",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Readiness Score", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$readinessVal%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (readinessVal >= 70) EmeraldSuccess else GoldenSpark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Readiness Badge & Progress Bar
                LinearProgressIndicator(
                    progress = { readinessVal / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (readinessVal >= 70) EmeraldSuccess else NeonCyan,
                    trackColor = Color(0x33FFFFFF)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )

                    Text(
                        text = "Syllabus: ${examReadiness?.syllabusCoveragePercent ?: 0}% | Balance: ${examReadiness?.subjectBalanceScore ?: 0}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                val insight = examReadiness?.actionableInsight ?: snapshot?.insightsSummary
                if (!insight.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22000000))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "💡 $insight",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }

        // 2. "What Should I Study Next?" Recommendation Engine Bar
        if (recommendations.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "What Should I Study Next? 🎯",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = dailyPlan?.summaryAdvice ?: "Smart Recommendation",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recommendations) { rec ->
                        RecommendationCard(
                            recommendation = rec,
                            onStartTopic = { onStartFocusOnTopic(rec.subjectName, rec.topicName) }
                        )
                    }
                }
            }
        }

        // 3. Subject Progress Summaries
        if (subjectSummaries.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Subject Mastery Breakdown 📊",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                subjectSummaries.forEach { summary ->
                    SubjectSummaryCard(summary = summary)
                }
            }
        }

        // 4. Topic Mastery Matrix & Subject Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Topic Mastery Matrix 🧠",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Real-time accuracy & confidence level per topic",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Text(
                text = "${topicMasteries.count { it.masteryScore >= 85 }}/${topicMasteries.size} Mastered",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldSuccess
            )
        }

        // Subject Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(subjects) { sub ->
                val isSelected = selectedSubjectFilter == sub
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0x15FFFFFF))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonCyan else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .springClickable(testTag = "mastery_filter_$sub") {
                            selectedSubjectFilter = sub
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) NeonCyan else Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Topic Mastery Cards
        if (filteredMasteries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x15FFFFFF))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No topic mastery data recorded for $selectedSubjectFilter yet.",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            filteredMasteries.forEach { item ->
                Step10TopicMasteryCard(
                    item = item,
                    onPracticeTopic = { onStartFocusOnTopic(item.subject, item.topic) },
                    onSetOverride = { override -> onSetManualTopicOverride(item.subject, item.topic, override) }
                )
            }
        }

        // 5. Reset Preparation Data Option
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { showResetConfirmDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_prep_data_btn"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55F87171))
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset Exam Preparation Data", fontSize = 12.sp)
        }
    }

    // Edit Exam Objective Dialog
    if (showEditObjectiveDialog) {
        var examName by remember { mutableStateOf(examObjective?.examName ?: user?.examName ?: "") }
        var targetRank by remember { mutableStateOf(examObjective?.targetScoreOrRank ?: "") }
        var targetHours by remember { mutableStateOf(examObjective?.targetWeeklyStudyHours?.toString() ?: "25") }

        AlertDialog(
            onDismissRequest = { showEditObjectiveDialog = false },
            title = { Text("Set Exam Target & Goal", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = examName,
                        onValueChange = { examName = it },
                        label = { Text("Exam Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetRank,
                        onValueChange = { targetRank = it },
                        label = { Text("Target Rank / Score Goal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetHours,
                        onValueChange = { targetHours = it },
                        label = { Text("Weekly Study Hours") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = targetHours.toFloatOrNull() ?: 25f
                        onSaveExamObjective(
                            ExamObjective(
                                id = examObjective?.id ?: 0,
                                examName = examName,
                                targetScoreOrRank = targetRank,
                                examDateMillis = examObjective?.examDateMillis ?: (System.currentTimeMillis() + 60L * 24 * 3600 * 1000),
                                targetWeeklyStudyHours = hours,
                                status = "ACTIVE"
                            )
                        )
                        showEditObjectiveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("Save Target")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditObjectiveDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF131C2E)
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Exam Preparation Data?", color = Color.White) },
            text = {
                Text(
                    "This will clear topic mastery scores and mistake logs for the current exam. Your syllabus structure and study materials will remain intact.",
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetPreparationData()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                ) {
                    Text("Reset Preparation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF131C2E)
        )
    }
}

@Composable
private fun RecommendationCard(
    recommendation: StudyRecommendation,
    onStartTopic: () -> Unit
) {
    val actionColor = when (recommendation.recommendedAction) {
        "REVISE" -> GoldenSpark
        "MISTAKE_REVIEW" -> Color(0xFFF87171)
        "LEARN_NEW" -> ElectricViolet
        else -> NeonCyan
    }

    GlassCard(
        modifier = Modifier.width(220.dp),
        fillAlpha = 0.8f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(actionColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = recommendation.recommendedAction.replace("_", " "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = actionColor
                    )
                }

                Text(
                    text = "${recommendation.recommendedDurationMinutes}m",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Text(
                text = recommendation.topicName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )

            Text(
                text = "${recommendation.subjectName} • ${recommendation.reason}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                maxLines = 2
            )

            Button(
                onClick = onStartTopic,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = actionColor, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Study Topic", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SubjectSummaryCard(summary: SubjectProgressSummary) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.75f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Avg Mastery: ${summary.averageMasteryScore}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.averageMasteryScore >= 70) EmeraldSuccess else GoldenSpark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { summary.averageMasteryScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (summary.averageMasteryScore >= 70) EmeraldSuccess else NeonCyan,
                trackColor = Color(0x22FFFFFF)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Topics: ${summary.completedTopicsCount}/${summary.totalTopicsCount}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text("Mastered: ${summary.masteredTopicsCount}", fontSize = 11.sp, color = EmeraldSuccess)
                Text("Weak: ${summary.weakTopicsCount}", fontSize = 11.sp, color = Color(0xFFF87171))
                Text("Acc: ${summary.overallAccuracyPercent.toInt()}%", fontSize = 11.sp, color = GoldenSpark)
            }
        }
    }
}

@Composable
private fun Step10TopicMasteryCard(
    item: TopicMastery,
    onPracticeTopic: () -> Unit,
    onSetOverride: (String) -> Unit
) {
    val stateColor = when (item.masteryState) {
        "MASTERED" -> EmeraldSuccess
        "STRONG" -> NeonCyan
        "IMPROVING" -> GoldenSpark
        "WEAK" -> Color(0xFFF87171)
        "REVISION_DUE" -> ElectricViolet
        "PRACTICING" -> Color(0xFF38BDF8)
        "LEARNING" -> Color(0xFFFBBF24)
        else -> Color(0xFF94A3B8)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.8f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.topic,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${item.subject}${if (item.chapter.isNotBlank()) " • ${item.chapter}" else ""} • ${item.totalQuestionsAttempted} Questions",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(stateColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${item.masteryScore}% ${item.masteryState.replace("_", " ")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = stateColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mastery Progress Bar
            LinearProgressIndicator(
                progress = { item.masteryScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = stateColor,
                trackColor = Color(0x22FFFFFF)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Extra metrics & Manual Override Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Acc: ${item.accuracyPercent.toInt()}% • Conf: ${item.confidenceLevel}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.userManualOverride == "NONE") {
                        TextButton(
                            onClick = { onSetOverride("I_KNOW_THIS") },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("I Know This", fontSize = 10.sp, color = EmeraldSuccess)
                        }
                        TextButton(
                            onClick = { onSetOverride("NEED_HELP") },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Need Help", fontSize = 10.sp, color = Color(0xFFF87171))
                        }
                    } else {
                        Text(
                            text = "Pref: ${item.userManualOverride}",
                            fontSize = 10.sp,
                            color = GoldenSpark,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { onSetOverride("NONE") },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Clear", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    Button(
                        onClick = onPracticeTopic,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Practice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

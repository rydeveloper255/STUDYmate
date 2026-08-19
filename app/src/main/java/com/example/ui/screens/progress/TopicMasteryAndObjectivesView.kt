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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TopicMasteryAndObjectivesView(
    user: UserProfile?,
    examObjective: ExamObjective?,
    topicMasteries: List<TopicMastery>,
    sessionHistory: List<StudentSessionHistory>,
    snapshot: IntelligenceSnapshot?,
    onSaveExamObjective: (ExamObjective) -> Unit,
    onStartFocusOnTopic: (subject: String, topic: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubjectFilter by remember { mutableStateOf<String?>("All") }
    var showEditObjectiveDialog by remember { mutableStateOf(false) }

    val subjects = remember(topicMasteries) {
        listOf("All") + topicMasteries.map { it.subject }.distinct()
    }

    val filteredMasteries = remember(topicMasteries, selectedSubjectFilter) {
        if (selectedSubjectFilter == null || selectedSubjectFilter == "All") topicMasteries
        else topicMasteries.filter { it.subject.equals(selectedSubjectFilter, ignoreCase = true) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Exam Objective & Target Readiness Card
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
                                .size(36.dp)
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
                                text = "EXAM OBJECTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = examObjective?.examName ?: user?.examName ?: "Competitive Exam",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Target Goal", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
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

                    Column {
                        Text("Readiness Index", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text(
                            text = "${snapshot?.readinessIndex?.toInt() ?: 72}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                }

                if (snapshot != null && snapshot.insightsSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22000000))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "💡 ${snapshot.insightsSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }

        // 2. Topic Mastery Levels Section
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
                    text = "Real-time retention and problem-solving levels",
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
                val isSelected = (selectedSubjectFilter ?: "All") == sub
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

        // Topic Mastery Items
        if (filteredMasteries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x15FFFFFF))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No topic masteries recorded yet.", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            filteredMasteries.forEach { item ->
                TopicMasteryCard(
                    item = item,
                    onPracticeTopic = { onStartFocusOnTopic(item.subject, item.topic) }
                )
            }
        }

        // 3. Study Session History Log
        if (sessionHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Recent Study Sessions ⏱️",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            sessionHistory.take(10).forEach { session ->
                SessionHistoryItemCard(session = session)
            }
        }
    }

    // Edit Objective Dialog
    if (showEditObjectiveDialog) {
        var examName by remember { mutableStateOf(examObjective?.examName ?: user?.examName ?: "JEE Main") }
        var targetRank by remember { mutableStateOf(examObjective?.targetScoreOrRank ?: "Top 500 AIR") }
        var targetHours by remember { mutableStateOf((examObjective?.targetWeeklyStudyHours?.toInt() ?: 25).toString()) }

        AlertDialog(
            onDismissRequest = { showEditObjectiveDialog = false },
            title = { Text("Update Exam Objective", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = examName,
                        onValueChange = { examName = it },
                        label = { Text("Target Exam Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetRank,
                        onValueChange = { targetRank = it },
                        label = { Text("Target Score / Rank Goal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetHours,
                        onValueChange = { targetHours = it },
                        label = { Text("Target Study Hours / Week") },
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
                                examDateMillis = examObjective?.examDateMillis ?: (user?.examDateMillis ?: System.currentTimeMillis() + 60L * 24 * 3600 * 1000),
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
}

@Composable
private fun TopicMasteryCard(
    item: TopicMastery,
    onPracticeTopic: () -> Unit
) {
    val levelColor = when (item.masteryLevel) {
        "MASTERED" -> EmeraldSuccess
        "PROFICIENT" -> NeonCyan
        "DEVELOPING" -> GoldenSpark
        else -> Color(0xFFF87171)
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
                        text = "${item.subject} • ${item.totalQuestionsAttempted} Questions Attempted",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(levelColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${item.masteryScore}% ${item.masteryLevel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mastery Progress Bar
            LinearProgressIndicator(
                progress = { item.masteryScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = levelColor,
                trackColor = Color(0x33FFFFFF)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 ${item.accuracyPercent.toInt()}% Acc | 🟢 ${item.easySolved}E • 🟡 ${item.medSolved}M • 🔴 ${item.hardSolved}H",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1)
                )

                TextButton(
                    onClick = onPracticeTopic,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Practice", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }

            if (item.weakSpots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("⚠️ Focus Areas:", fontSize = 10.sp, color = GoldenSpark, fontWeight = FontWeight.SemiBold)
                item.weakSpots.forEach { spot ->
                    Text(" • $spot", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryItemCard(session: StudentSessionHistory) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedDate = remember(session.timestamp) { dateFormat.format(Date(session.timestamp)) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.7f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${session.sessionType}: ${session.topic}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${session.subject} • $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.actualMinutesSpent} mins",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "+${session.xpEarned} XP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldSuccess
                )
            }
        }
    }
}

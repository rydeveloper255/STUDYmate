package com.example.ui.screens.planner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

/**
 * Step 50: Dedicated Nova Smart Study Planner Screen
 * Features:
 * - Actionable Today's Mission (respects Study Schedule)
 * - Weak Topic Insights
 * - Adaptive Schedule Shift Proposals (Requires user confirmation!)
 * - Weekly Study Review & Goal Editor
 * - Real Study Streak
 * - End-Of-Day Summary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartStudyPlannerScreen(
    dailyMissions: List<DailyMissionTask>,
    weakTopicInsights: List<WeakTopicInsight>,
    adaptiveScheduleShift: AdaptiveScheduleShiftSuggestion?,
    weeklyReviewStats: WeeklyReviewStats,
    weeklyGoalHours: Float,
    studyStreakDays: Int,
    onToggleMission: (taskId: String, isCompleted: Boolean) -> Unit,
    onDismissMission: (taskId: String) -> Unit,
    onAcceptScheduleShift: (AdaptiveScheduleShiftSuggestion) -> Unit,
    onDismissScheduleShift: () -> Unit,
    onUpdateWeeklyGoal: (Float) -> Unit,
    onStartAction: (actionType: String, subject: String, topic: String, minutes: Int) -> Unit,
    onBack: () -> Unit
) {
    var showGoalEditDialog by remember { mutableStateOf(false) }
    var tempGoalHours by remember { mutableFloatStateOf(weeklyGoalHours) }

    val completedMissionCount = dailyMissions.count { it.isCompleted }
    val totalMissionCount = dailyMissions.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🧠 Smart Study Planner",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Nova Smart Intelligence & Companion",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCanvas)
            )
        },
        containerColor = DarkCanvas
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. TODAY'S MISSION CARD
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    borderColor = PrimaryCyan.copy(alpha = 0.5f),
                    fillAlpha = 0.6f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🎯 TODAY'S MISSION",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$completedMissionCount / $totalMissionCount completed",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = PrimaryCyan,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress bar
                        val progress = if (totalMissionCount > 0) completedMissionCount.toFloat() / totalMissionCount else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = PrimaryCyan,
                            trackColor = ElectricBlue.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (dailyMissions.isEmpty()) {
                            Text(
                                text = "Daily mission is up to date! Check back tomorrow or add a schedule.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        } else {
                            dailyMissions.forEach { task ->
                                MissionTaskRow(
                                    task = task,
                                    onToggle = { onToggleMission(task.id, !task.isCompleted) },
                                    onDismiss = { onDismissMission(task.id) },
                                    onStartAction = {
                                        onStartAction(task.actionType, task.subject, task.topic, task.targetMinutes)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // 2. STUDY STREAK CARD
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderColor = AmberAlert.copy(alpha = 0.5f),
                    fillAlpha = 0.5f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AmberAlert.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🔥", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "$studyStreakDays Day Study Streak",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Minimum 20 min active study requirement verified",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = AmberAlert.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AmberAlert,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 3. ADAPTIVE SCHEDULE SHIFT PROPOSAL (If available)
            adaptiveScheduleShift?.let { shift ->
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        borderColor = CoralRose.copy(alpha = 0.6f),
                        fillAlpha = 0.6f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = CoralRose,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Adaptive Schedule Suggestion",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CoralRose
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = shift.hinglishMessage,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = onDismissScheduleShift,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                                ) {
                                    Text("Keep Schedule")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onAcceptScheduleShift(shift) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                                ) {
                                    Text("Move to ${shift.suggestedTime}", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // 4. WEAK TOPIC INSIGHTS
            if (weakTopicInsights.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "🧠 Nova Weak Topic Insights",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        weakTopicInsights.forEach { insight ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                borderColor = ElectricBlue.copy(alpha = 0.4f),
                                fillAlpha = 0.5f
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
                                            text = "${insight.subject} • ${insight.topic}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CoralRose.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Accuracy: ${insight.accuracyPercentage}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = CoralRose,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = insight.insightHinglish,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            onStartAction("PRACTICE", insight.subject, insight.topic, insight.recommendedPracticeMinutes)
                                        },
                                        modifier = Modifier.align(Alignment.End),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Practice Now")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. WEEKLY STUDY REVIEW & GOALS
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    borderColor = ElectricBlue.copy(alpha = 0.5f),
                    fillAlpha = 0.6f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📊 WEEKLY STUDY REVIEW",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            IconButton(onClick = { showGoalEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Goal", tint = PrimaryCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Weekly Goal Bar
                        val completedHoursFormatted = String.format(Locale.US, "%.1fh", weeklyReviewStats.completedHours)
                        val targetHoursFormatted = String.format(Locale.US, "%.0fh", weeklyGoalHours)
                        val goalProgress = (weeklyReviewStats.completedHours / weeklyGoalHours).coerceIn(0f, 1f)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Weekly Goal Target",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                            Text(
                                text = "$completedHoursFormatted / $targetHoursFormatted",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCyan
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { goalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = PrimaryCyan,
                            trackColor = ElectricBlue.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4-cell stats grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatMiniBox(
                                label = "Planned",
                                value = String.format(Locale.US, "%.0fh", weeklyReviewStats.plannedHours),
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniBox(
                                label = "Completed",
                                value = completedHoursFormatted,
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniBox(
                                label = "Completion",
                                value = "${weeklyReviewStats.completionPercentage}%",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatMiniBox(
                                label = "Best Subject",
                                value = weeklyReviewStats.bestSubject,
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniBox(
                                label = "Needs Attention",
                                value = weeklyReviewStats.needsAttentionSubject,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Weekly Goal Edit Dialog
    if (showGoalEditDialog) {
        AlertDialog(
            onDismissRequest = { showGoalEditDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Set Weekly Study Goal",
                    style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Current Goal: ${tempGoalHours.toInt()} hours / week",
                        style = MaterialTheme.typography.bodyMedium.copy(color = PrimaryCyan, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = tempGoalHours,
                        onValueChange = { tempGoalHours = it },
                        valueRange = 5f..40f,
                        steps = 34,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryCyan,
                            activeTrackColor = PrimaryCyan
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateWeeklyGoal(tempGoalHours)
                        showGoalEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                ) {
                    Text("Save Goal", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGoalEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MissionTaskRow(
    task: DailyMissionTask,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onStartAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (task.isCompleted) EmeraldGreen.copy(alpha = 0.4f) else ElectricBlue.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldGreen,
                        uncheckedColor = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (task.isCompleted) TextSecondary else TextPrimary
                        )
                    )
                    if (task.scheduledTime.isNotEmpty()) {
                        Text(
                            text = "Scheduled: ${task.scheduledTime}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!task.isCompleted) {
                    val actionLabel = when (task.actionType) {
                        "FOCUS" -> "Start Focus"
                        "PRACTICE" -> "Start Practice"
                        "CURRENT_AFFAIRS" -> "Open CA"
                        "MOCK" -> "Take Mock"
                        else -> "Start"
                    }
                    Button(
                        onClick = onStartAction,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }
                }
                if (!task.isFromSchedule) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
            )
        }
    }
}

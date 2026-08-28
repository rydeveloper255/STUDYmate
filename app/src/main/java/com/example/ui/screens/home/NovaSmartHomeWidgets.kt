package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyMissionTask
import com.example.data.model.WeakTopicInsight
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

/**
 * Step 50: Compact Home Widgets for Today's Mission, Weak Topic Insight, & Plan Navigation
 */

@Composable
fun TodayMissionHomeWidget(
    missions: List<DailyMissionTask>,
    completedCount: Int,
    totalCount: Int,
    onToggleMission: (taskId: String, isCompleted: Boolean) -> Unit,
    onStartAction: (actionType: String, subject: String, topic: String, minutes: Int) -> Unit,
    onOpenPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderColor = PrimaryCyan.copy(alpha = 0.4f),
        fillAlpha = 0.5f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🎯 TODAY'S MISSION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$completedCount/$totalCount Done",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = PrimaryCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenPlan() },
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "View Plan",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = PrimaryCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View Plan",
                            tint = PrimaryCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Compact Progress Bar
            val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = PrimaryCyan,
                trackColor = ElectricBlue.copy(alpha = 0.2f)
            )

            // Show top 2 tasks max to keep card compact
            if (missions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    missions.take(2).forEach { task ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onToggleMission(task.id, !task.isCompleted) }
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (task.isCompleted) EmeraldGreen else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = if (task.isCompleted) TextSecondary else TextPrimary,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!task.isCompleted) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryCyan.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable {
                                        onStartAction(task.actionType, task.subject, task.topic, task.targetMinutes)
                                    }
                                ) {
                                    Text(
                                        text = when (task.actionType) {
                                            "FOCUS" -> "Focus"
                                            "PRACTICE" -> "Practice"
                                            "CURRENT_AFFAIRS" -> "CA"
                                            else -> "Start"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = PrimaryCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeakTopicInsightHomeWidget(
    insight: WeakTopicInsight,
    onStartPractice: (subject: String, topic: String, minutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
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
                    text = "🧠 Nova Insight",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCyan
                    )
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CoralRose.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${insight.accuracyPercentage}% Accuracy",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CoralRose,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                onClick = { onStartPractice(insight.subject, insight.topic, insight.recommendedPracticeMinutes) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Practice Now", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

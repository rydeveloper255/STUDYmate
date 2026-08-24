package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.service.intelligence.*
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 1. Subjects Performance View (Step 36 Section 2)
 */
@Composable
fun SubjectsPerformanceView(
    subjectDetails: List<SubjectPerformanceDetail>,
    onStartSubjectPractice: (String) -> Unit
) {
    if (subjectDetails.isEmpty()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.MenuBook, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Subject Data Yet", fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Complete quizzes or mock tests to view subject-wise trends and accuracy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        subjectDetails.forEach { sub ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_perf_${sub.subject}"),
                shape = RoundedCornerShape(18.dp)
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
                            text = sub.subject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Trend Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (sub.trend) {
                                PerformanceTrend.IMPROVING -> EmeraldSuccess.copy(alpha = 0.15f)
                                PerformanceTrend.DECLINING -> CoralRose.copy(alpha = 0.15f)
                                else -> ElectricViolet.copy(alpha = 0.15f)
                            },
                            border = BorderStroke(
                                0.5.dp,
                                when (sub.trend) {
                                    PerformanceTrend.IMPROVING -> EmeraldSuccess.copy(alpha = 0.4f)
                                    PerformanceTrend.DECLINING -> CoralRose.copy(alpha = 0.4f)
                                    else -> ElectricViolet.copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Text(
                                text = sub.trend.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (sub.trend) {
                                    PerformanceTrend.IMPROVING -> EmeraldSuccess
                                    PerformanceTrend.DECLINING -> CoralRose
                                    else -> ElectricViolet
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (sub.accuracyPercent == null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x18FFFFFF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ ${sub.statusMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Accuracy", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(
                                    "${sub.accuracyPercent.toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sub.accuracyPercent >= 70) EmeraldSuccess else CoralRose
                                )
                            }

                            Column {
                                Text("Questions", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(
                                    "${sub.totalAttempts}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column {
                                Text("Avg Time/Q", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(
                                    "${sub.avgTimePerQuestionSeconds.toInt()}s",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = sub.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onStartSubjectPractice(sub.subject) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Practice ${sub.subject}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 2. Topics Speed & Accuracy Matrix View (Step 36 Section 3)
 */
@Composable
fun TopicsMatrixView(
    topicDetails: List<TopicPerformanceDetail>,
    onStartTopicPractice: (String, String) -> Unit
) {
    if (topicDetails.isEmpty()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.GridOn, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Topic Matrix Data", fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Practice quizzes to populate the Speed-Accuracy intelligence matrix.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        topicDetails.forEach { top ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("topic_perf_${top.topic}"),
                shape = RoundedCornerShape(18.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(top.topic, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(top.subject, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }

                        // Matrix Quadrant Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                top.isStrong -> EmeraldSuccess.copy(alpha = 0.2f)
                                top.isWeak -> CoralRose.copy(alpha = 0.2f)
                                else -> ElectricViolet.copy(alpha = 0.2f)
                            },
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = top.speedAccuracyCategory,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    top.isStrong -> EmeraldSuccess
                                    top.isWeak -> CoralRose
                                    else -> ElectricViolet
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = top.speedAccuracyAdvice.ifBlank { top.actionReason },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onStartTopicPractice(top.subject, top.topic) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Practice Topic", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Test History View (Step 36 Section 4)
 */
@Composable
fun TestHistoryView(
    attempts: List<MockTestAttempt>,
    onReviewAttempt: (MockTestAttempt) -> Unit,
    onRetakeAttempt: (MockTestAttempt) -> Unit,
    onDeleteAttempt: (Long) -> Unit
) {
    if (attempts.isEmpty()) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Quiz, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Test History Yet", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Complete practice or mock tests to view detailed performance history.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
        attempts.forEach { att ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_history_item_${att.id}"),
                shape = RoundedCornerShape(16.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(att.title.ifBlank { att.examName }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${att.subject} • ${att.totalQuestions} Qs • ${sdf.format(Date(att.timestamp))}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (att.accuracyPercent >= 70) EmeraldSuccess.copy(alpha = 0.2f) else CoralRose.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Score: ${att.score} (${att.accuracyPercent.toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (att.accuracyPercent >= 70) EmeraldSuccess else CoralRose,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onReviewAttempt(att) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Review Answers", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onRetakeAttempt(att) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Retake", color = Color.White, fontSize = 11.sp)
                        }

                        IconButton(onClick = { onDeleteAttempt(att.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.DeleteOutline, null, tint = CoralRose, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Personalization Settings & Goals View (Step 36 Section 7)
 */
@Composable
fun PersonalizationSettingsView(
    userPreferences: UserStudyPreferences,
    onUpdateSettings: (PersonalizationSettings) -> Unit,
    onResetSignals: () -> Unit
) {
    var isEnabled by remember(userPreferences) { mutableStateOf(userPreferences.personalizationEnabled) }
    var questionGoal by remember(userPreferences) { mutableFloatStateOf(userPreferences.dailyQuestionGoal.toFloat()) }
    var minutesGoal by remember(userPreferences) { mutableFloatStateOf(userPreferences.dailyStudyMinutesGoal.toFloat()) }
    var weeklyTestsGoal by remember(userPreferences) { mutableFloatStateOf(userPreferences.weeklyTestsGoal.toFloat()) }

    var caReminders by remember(userPreferences) { mutableStateOf(userPreferences.caRemindersEnabled) }
    var revisionReminders by remember(userPreferences) { mutableStateOf(userPreferences.revisionRemindersEnabled) }
    var studyReminders by remember(userPreferences) { mutableStateOf(userPreferences.studyRemindersEnabled) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("personalization_settings_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("⚙️ Personalization & Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

            // Personalization Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable Personalization Engine", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Tailor recommendations using your actual test attempts & mistakes.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = {
                        isEnabled = it
                        onUpdateSettings(
                            PersonalizationSettings(
                                isEnabled = isEnabled,
                                dailyQuestionGoal = questionGoal.toInt(),
                                dailyStudyMinutesGoal = minutesGoal.toInt(),
                                weeklyTestsGoal = weeklyTestsGoal.toInt(),
                                caRemindersEnabled = caReminders,
                                revisionRemindersEnabled = revisionReminders,
                                studyRemindersEnabled = studyReminders
                            )
                        )
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonCyan)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Daily Question Goal Slider
            Column {
                Text("Daily Question Goal: ${questionGoal.toInt()} Qs", fontWeight = FontWeight.Bold, color = Color.White)
                Slider(
                    value = questionGoal,
                    onValueChange = { questionGoal = it },
                    onValueChangeFinished = {
                        onUpdateSettings(
                            PersonalizationSettings(
                                isEnabled = isEnabled,
                                dailyQuestionGoal = questionGoal.toInt(),
                                dailyStudyMinutesGoal = minutesGoal.toInt(),
                                weeklyTestsGoal = weeklyTestsGoal.toInt(),
                                caRemindersEnabled = caReminders,
                                revisionRemindersEnabled = revisionReminders,
                                studyRemindersEnabled = studyReminders
                            )
                        )
                    },
                    valueRange = 10f..100f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }

            // Daily Minutes Goal Slider
            Column {
                Text("Daily Study Time Target: ${minutesGoal.toInt()} Mins", fontWeight = FontWeight.Bold, color = Color.White)
                Slider(
                    value = minutesGoal,
                    onValueChange = { minutesGoal = it },
                    onValueChangeFinished = {
                        onUpdateSettings(
                            PersonalizationSettings(
                                isEnabled = isEnabled,
                                dailyQuestionGoal = questionGoal.toInt(),
                                dailyStudyMinutesGoal = minutesGoal.toInt(),
                                weeklyTestsGoal = weeklyTestsGoal.toInt(),
                                caRemindersEnabled = caReminders,
                                revisionRemindersEnabled = revisionReminders,
                                studyRemindersEnabled = studyReminders
                            )
                        )
                    },
                    valueRange = 30f..300f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = ElectricViolet, activeTrackColor = ElectricViolet)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Reset Signals Button
            OutlinedButton(
                onClick = onResetSignals,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CoralRose.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_personalization_button")
            ) {
                Icon(Icons.Filled.Refresh, null, tint = CoralRose, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Personalization Recommendation Signals", color = CoralRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

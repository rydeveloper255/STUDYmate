package com.example.ui.screens.planner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyPlanItem
import com.example.ui.components.ThemeToggleButton
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun StudySessionTimerView(
    activeSession: StudyPlanItem,
    remainingSeconds: Int,
    isTimerRunning: Boolean,
    isPaused: Boolean,
    actualMinutesSpent: Int,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onFinishSession: (notes: String) -> Unit,
    onCancelSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var sessionNotes by remember { mutableStateOf("") }
    var showFinishDialog by remember { mutableStateOf(false) }

    val totalSeconds = (activeSession.targetMinutes * 60).coerceAtLeast(1)
    val progress = ((totalSeconds - remainingSeconds).toFloat() / totalSeconds).coerceIn(0f, 1f)

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isDark) Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617)))
                else Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFEEF2FF)))
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancelSession,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x30FFFFFF) else Color(0x10000000))
                    .testTag("close_timer_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cancel Session",
                    tint = if (isDark) Color.White else Color(0xFF0F172A)
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when (activeSession.sessionType) {
                    "REVISION" -> Color(0x25F59E0B)
                    "WEAK_TOPIC" -> Color(0x25EF4444)
                    "LEARNING" -> Color(0x253B82F6)
                    "MOCK_TEST" -> Color(0x258B5CF6)
                    else -> Color(0x2510B981)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (activeSession.sessionType) {
                        "REVISION" -> AmberAlert
                        "WEAK_TOPIC" -> CoralPink
                        "LEARNING" -> NeonCyan
                        "MOCK_TEST" -> ElectricViolet
                        else -> EmeraldSuccess
                    }
                )
            ) {
                Text(
                    text = "🎯 ${activeSession.sessionType.replace('_', ' ')}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (activeSession.sessionType) {
                        "REVISION" -> AmberAlert
                        "WEAK_TOPIC" -> CoralPink
                        "LEARNING" -> NeonCyan
                        "MOCK_TEST" -> ElectricViolet
                        else -> EmeraldSuccess
                    }
                )
            }

            ThemeToggleButton(testTag = "timer_theme_toggle")
        }

        // Active Subject & Topic Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = activeSession.subject.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GoldenSpark,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = activeSession.topic.ifBlank { activeSession.chapter },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
            if (activeSession.aiRecommendationReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 ${activeSession.aiRecommendationReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Timer Ring Display
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x181E293B) else Color(0x60E2E8F0))
                .border(
                    width = 6.dp,
                    brush = Brush.sweepGradient(
                        listOf(NeonCyan, ElectricViolet, GoldenSpark, EmeraldSuccess, NeonCyan)
                    ),
                    shape = CircleShape
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPaused) "PAUSED" else "PLANNED: ${activeSession.targetMinutes}m",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPaused) AmberAlert else NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Actual: ${actualMinutesSpent}m spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            if (isPaused) {
                Button(
                    onClick = onResumeTimer,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f)
                        .testTag("resume_timer_btn")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF070B19))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resume", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onPauseTimer,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x35F59E0B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f)
                        .testTag("pause_timer_btn")
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null, tint = AmberAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pause", color = AmberAlert, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { showFinishDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(52.dp)
                    .weight(1f)
                    .testTag("finish_timer_btn")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF070B19))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Finish", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
            }
        }
    }

    // Finish Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = {
                Text(
                    text = "🎉 Complete Study Session",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Actual time studied: $actualMinutesSpent minutes out of ${activeSession.targetMinutes}m planned.")
                    OutlinedTextField(
                        value = sessionNotes,
                        onValueChange = { sessionNotes = it },
                        label = { Text("Session Notes (Optional)") },
                        placeholder = { Text("What concepts did you master?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        onFinishSession(sessionNotes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Text("Save & Complete", fontWeight = FontWeight.Bold, color = Color(0xFF070B19))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Back to Timer")
                }
            }
        )
    }
}

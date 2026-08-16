package com.example.ui.screens.focus

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FocusShieldManager
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.FocusTimerState

@Composable
fun FocusModeScreen(
    focusState: FocusTimerState,
    onStartFocus: (subject: String, topic: String, minutes: Int) -> Unit,
    onTogglePause: () -> Unit,
    onEndSession: () -> Unit,
    onDismissCelebration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMinutes by remember { mutableIntStateOf(focusState.initialMinutes) }
    var selectedSubject by remember { mutableStateOf(focusState.subject) }
    var selectedTopic by remember { mutableStateOf(focusState.topic) }
    var showAppShieldModal by remember { mutableStateOf(false) }

    val blockedAppsCount = remember(showAppShieldModal, focusState.isRunning) {
        FocusShieldManager.getRestrictedPackages().size
    }

    val presetDurations = listOf(15, 25, 45, 60)
    val subjects = listOf("Physics", "Mathematics", "Chemistry", "Biology", "Computer Science")

    val remainingMinutes = focusState.remainingSeconds / 60
    val remainingSecs = focusState.remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", remainingMinutes, remainingSecs)
    val totalSecs = (focusState.initialMinutes * 60).toFloat()
    val progress = if (totalSecs > 0) (focusState.remainingSeconds.toFloat() / totalSecs) else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(20.dp)
            .testTag("focus_mode_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎯 Deep Focus Mode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Distraction-free deep work environment",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Blocked Apps Quick Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x2038BDF8),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4038BDF8)),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { showAppShieldModal = true }.testTag("app_shield_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Shield, "Blocked Apps", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Blocked Apps ($blockedAppsCount)",
                            color = NeonCyan,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Center Focus Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (focusState.isRunning) {
                    // Active Timer View
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x2538BDF8),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x5038BDF8))
                    ) {
                        Text(
                            text = "${focusState.subject} • ${focusState.topic}",
                            color = NeonCyan,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Big Liquid Glowing Clock
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0x2538BDF8), Color(0x10818CF8), Color.Transparent)
                                )
                            )
                            .border(2.dp, Brush.linearGradient(listOf(NeonCyan, NebulaPurple)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (focusState.isPaused) "PAUSED" else "STAY FOCUSED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (focusState.isPaused) GoldenSpark else NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x20FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x25FFFFFF)),
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { showAppShieldModal = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Shield, null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$blockedAppsCount distracting apps blocked • Tap to manage 🛡️",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                } else {
                    // Idle Setup View
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.75f
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Select Duration",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presetDurations.forEach { mins ->
                                    val isSelected = selectedMinutes == mins
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) NeonCyan else Color(0x20FFFFFF)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color.White else Color(0x30FFFFFF),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .springClickable(testTag = "preset_${mins}m") {
                                                selectedMinutes = mins
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${mins}m",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF070B19) else Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Subject & Topic",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                subjects.take(3).forEach { sub ->
                                    val isSelected = selectedSubject == sub
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) ElectricViolet else Color(0x18FFFFFF),
                                        modifier = Modifier.springClickable { selectedSubject = sub }
                                    ) {
                                        Text(
                                            text = sub,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = selectedTopic,
                                onValueChange = { selectedTopic = it },
                                label = { Text("Topic name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonCyan
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Focus shield status hint
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x1538BDF8),
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { showAppShieldModal = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Shield, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Focus App Shield: $blockedAppsCount Apps Blocked", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                    }
                                    Text("Configure →", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Timer Controls
            if (focusState.isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onTogglePause,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pause_resume_focus_btn"),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x50FFFFFF))
                    ) {
                        Icon(
                            imageVector = if (focusState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (focusState.isPaused) "Resume" else "Pause",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onEndSession,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("end_focus_session_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                    ) {
                        Icon(Icons.Filled.Stop, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finish", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                GlassButton(
                    text = "▶ Start $selectedMinutes Min Focus",
                    onClick = {
                        onStartFocus(
                            selectedSubject,
                            selectedTopic.ifBlank { "Deep Study" },
                            selectedMinutes
                        )
                    },
                    icon = Icons.Filled.PlayArrow,
                    isPrimary = true,
                    testTag = "start_focus_btn"
                )
            }
        }

        // Session Celebration Dialog
        if (focusState.showCelebration) {
            AlertDialog(
                onDismissRequest = onDismissCelebration,
                containerColor = Color(0xFF131C2E),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = GoldenSpark, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Focus Session Complete! 🎉",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "+${focusState.lastSessionXp} XP Earned",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Great discipline! Consistent focused blocks create lasting knowledge.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onDismissCelebration,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Awesome!", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // App Shield Modal / Overlay Screen
        if (showAppShieldModal) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("focus_shield_settings_modal"),
                color = Color(0xFF070B19)
            ) {
                FocusShieldSettingsScreen(
                    onBack = { showAppShieldModal = false }
                )
            }
        }
    }
}

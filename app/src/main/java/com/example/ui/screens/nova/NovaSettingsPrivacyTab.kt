package com.example.ui.screens.nova

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NovaSettings
import com.example.data.model.NovaVoiceState
import com.example.ui.components.GlassCard
import com.example.ui.components.NovaVoiceWaveform
import com.example.ui.theme.*
import com.example.viewmodel.NovaViewModel

@Composable
fun NovaSettingsPrivacyTab(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f),
            borderColor = NeonCyan.copy(alpha = 0.35f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("NOVA CONFIGURATION & PRIVACY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Voice, Smart Coach & Security", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Personalize AI voice, notifications, and privacy controls", fontSize = 12.sp, color = TextSecondary)
            }
        }

        // --- 1. Consistent NOVA Voice Controls ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.8f),
            borderColor = ElectricIndigo.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NOVA Voice & TTS Synthesis", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ElevenLabs Neural Voice Engine • Natural Hindi, Hinglish & English Conversational Speech",
                    fontSize = 11.sp,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Voice Enabled Switch
                SettingToggleRow(
                    title = "Voice Speech Enabled",
                    subtitle = "Allows NOVA to speak answers and explanations aloud",
                    isChecked = settings.voiceEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(voiceEnabled = it)) }
                )

                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                // Auto Speak Responses
                SettingToggleRow(
                    title = "Auto Speak Responses",
                    subtitle = "Automatically voice out new chat answers without tapping Listen",
                    isChecked = settings.ttsAutoSpeak,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(ttsAutoSpeak = it)) }
                )

                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                // Voice Notifications
                SettingToggleRow(
                    title = "Voice Notifications",
                    subtitle = "Read out study reminders if phone is not on silent or DND",
                    isChecked = settings.voiceNotifications,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(voiceNotifications = it)) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Language Selection Chips
                Text("Voice Language Preference", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val langOptions = listOf("Auto (Context-Aware)", "Hinglish", "Hindi", "English")
                    langOptions.forEach { opt ->
                        val isSelected = settings.voiceLanguage.equals(opt, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateSettings(settings.copy(voiceLanguage = opt)) },
                            label = { Text(opt, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricIndigo,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Speed Slider
                Text("Speech Speed: ${String.format("%.2fx", settings.speechSpeed)}", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = settings.speechSpeed,
                    onValueChange = { viewModel.updateSettings(settings.copy(speechSpeed = it)) },
                    valueRange = 0.75f..1.5f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )

                // Volume Slider
                Text("Voice Volume: ${(settings.voiceVolume * 100).toInt()}%", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = settings.voiceVolume,
                    onValueChange = { viewModel.updateSettings(settings.copy(voiceVolume = it)) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = ElectricIndigo, activeTrackColor = ElectricIndigo)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Voice Preview & Test Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.previewNovaVoice(settings.voiceLanguage) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkCanvas, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview Nova Voice", fontWeight = FontWeight.Bold, color = DarkCanvas, fontSize = 12.sp)
                    }

                    if (voiceState == NovaVoiceState.SPEAKING) {
                        NovaVoiceWaveform(
                            isActive = true,
                            isProcessing = false,
                            barCount = 4,
                            minBarHeight = 4.dp,
                            maxBarHeight = 16.dp,
                            barWidth = 3.dp,
                            barSpacing = 3.dp
                        )
                        OutlinedButton(
                            onClick = { viewModel.voiceManager.stopSpeaking() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralPink)
                        ) {
                            Text("Stop", fontSize = 12.sp)
                        }
                    }
                }

            }
        }

        // --- 2. Personality & Greeting ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.8f),
            borderColor = AmberGold.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Personality & Companion Style", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingToggleRow(
                    title = "Address as 'Boss'",
                    subtitle = "Friendly, respectful assistant address style (used naturally & occasionally)",
                    isChecked = settings.useBossGreeting,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(useBossGreeting = it)) }
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Persona Tone", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Friendly & Professional", "Concise & Analytical", "Empathetic Mentor").forEach { persona ->
                        FilterChip(
                            selected = settings.personality == persona,
                            onClick = { viewModel.updateSettings(settings.copy(personality = persona)) },
                            label = { Text(persona, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // --- 3. Smart Coach & Notification Engine ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.8f),
            borderColor = EmeraldGreen.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Smart Coach & Proactive Triggers", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingToggleRow(
                    title = "Study Session Reminders",
                    subtitle = "Sends actionable reminders with [Start Study], [Snooze], and [Skip]",
                    isChecked = settings.studyRemindersEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(studyRemindersEnabled = it)) }
                )

                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                SettingToggleRow(
                    title = "Missed Session Recovery",
                    subtitle = "Suggests gentle 20m recovery sessions instead of guilt-tripping",
                    isChecked = settings.missedSessionRecoveryEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(missedSessionRecoveryEnabled = it)) }
                )

                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                SettingToggleRow(
                    title = "Daily Morning Briefing (8:00 AM)",
                    subtitle = "Morning overview of today's study items and exam countdown",
                    isChecked = settings.dailyBriefingEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(dailyBriefingEnabled = it)) }
                )

                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                SettingToggleRow(
                    title = "Daily Evening Review (9:00 PM)",
                    subtitle = "End-of-day study progress summary and tomorrow's priorities",
                    isChecked = settings.dailyReviewEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(dailyReviewEnabled = it)) }
                )

                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                SettingToggleRow(
                    title = "Quiet Hours (10:00 PM – 7:00 AM)",
                    subtitle = "Silences non-essential notifications to ensure restful sleep",
                    isChecked = settings.quietHoursEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(quietHoursEnabled = it)) }
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Test Proactive Notification Triggers", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.triggerProactiveStudyNotification() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Study 🔔", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.triggerDailyBriefingNotification() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Brief ☀️", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.triggerSocialMediaNudgeNotification() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Nudge 📱", fontSize = 10.sp)
                    }
                }
            }
        }

        // --- 4. Privacy Center Audit ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.8f),
            borderColor = NeonCyan.copy(alpha = 0.25f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NOVA Privacy Center", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Strict privacy-first architecture. All memory and focus records reside securely on your local device Room database.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                PrivacyAuditRow(feature = "NOVA Memory", status = "Stored in Local Room DB", icon = Icons.Default.Memory)
                PrivacyAuditRow(feature = "Study & Test History", status = "Private on-device", icon = Icons.Default.History)
                PrivacyAuditRow(feature = "App Usage Awareness", status = "App Name & Mins only (No chats/passwords)", icon = Icons.Default.Apps)
                PrivacyAuditRow(feature = "Microphone Access", status = "Only active when tap Mic button", icon = Icons.Default.Mic)
                PrivacyAuditRow(feature = "Camera / Gallery", status = "Only active on photo doubt attachment", icon = Icons.Default.CameraAlt)
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyan.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun PrivacyAuditRow(
    feature: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(feature, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
        Text(status, fontSize = 11.sp, color = TextSecondary)
    }
}

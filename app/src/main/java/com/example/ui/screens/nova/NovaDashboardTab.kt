package com.example.ui.screens.nova

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NovaActionType
import com.example.data.model.NovaVoiceState
import com.example.service.NovaUsageStatsHelper
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaScreenTab
import com.example.viewmodel.NovaViewModel
import java.util.Calendar

@Composable
fun NovaDashboardTab(
    viewModel: NovaViewModel,
    onNavigateToFocus: (subject: String, topic: String, duration: Int) -> Unit,
    onNavigateToPlanner: () -> Unit,
    onRequestMicPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val studyContext by viewModel.studyContext.collectAsState()
    val analytics by viewModel.analyticsData.collectAsState()
    val dailyBriefingText by viewModel.dailyBriefingText.collectAsState()
    val adaptiveRec by viewModel.adaptiveRecommendation.collectAsState()
    val missedAlert by viewModel.missedSessionAlert.collectAsState()
    val socialNudge by viewModel.socialMediaNudge.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val audioRms by viewModel.voiceManager.audioLevelRms.collectAsState()

    var quickInputText by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setAttachedImage(it)
            viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
        }
    }

    val hasUsagePermission = remember(context) {
        NovaUsageStatsHelper.hasUsageStatsPermission(context)
    }

    val greetingPrefix = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Futuristic NOVA Status & Orb Header ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f),
            borderColor = NeonCyan.copy(alpha = 0.35f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NOVA ACTIVE • PERSONAL AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$greetingPrefix, ${studyContext.preferredTitle} 👋",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${studyContext.targetExam} • ${studyContext.examDaysRemaining} days left",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                NovaOrbVisualizer(
                    voiceState = voiceState,
                    audioRms = audioRms,
                    size = 72.dp
                )
            }
        }

        // --- 2. Context Card: Today's Progress (1h 40m / 3h) ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.8f),
            borderColor = ElectricIndigo.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Today's Progress",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "1h 40m / 3h (55%)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Indicator Bar
                LinearProgressIndicator(
                    progress = { 0.55f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = NeonCyan,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProgressStatItem(title = "Consistency", value = "${analytics.weeklyConsistencyScore}%", color = EmeraldGreen)
                    ProgressStatItem(title = "Quiz Accuracy", value = "${analytics.averageQuizAccuracy.toInt()}%", color = NeonCyan)
                    ProgressStatItem(title = "Streak", value = "${studyContext.currentStreak} Days 🔥", color = AmberGold)
                }
            }
        }

        // --- 3. Primary & Secondary Quick Actions ---
        Column {
            Text(
                text = "Quick Assistant Actions",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: [📚 Study] [🧠 Quiz] [🎯 Focus] [📝 Plan]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = "📚",
                    title = "Study",
                    subtitle = "Adaptive Rec",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val firstSub = studyContext.subjects.firstOrNull() ?: "Physics"
                        onNavigateToFocus(firstSub, "Adaptive Concept", 25)
                    }
                )
                QuickActionButton(
                    icon = "🧠",
                    title = "Quiz",
                    subtitle = "Test Memory",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTab(NovaScreenTab.INTERACTIVE_STUDY_QUIZ) }
                )
                QuickActionButton(
                    icon = "🎯",
                    title = "Focus",
                    subtitle = "Shield Active",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToFocus("Physics", "Focus Sprint", 25) }
                )
                QuickActionButton(
                    icon = "📝",
                    title = "Plan",
                    subtitle = "Syllabus",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToPlanner() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: [🎙️ Talk] [🎓 Voice Notes] [📷 Ask Image] [🧠 Memory]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = "🎙️",
                    title = "Talk to NOVA",
                    subtitle = "Live Voice",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
                        onRequestMicPermission()
                    }
                )
                QuickActionButton(
                    icon = "🎓",
                    title = "Voice Notes",
                    subtitle = "Lectures AI",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTab(NovaScreenTab.VOICE_NOTES) }
                )
                QuickActionButton(
                    icon = "📷",
                    title = "Ask Image",
                    subtitle = "OCR Doubt",
                    modifier = Modifier.weight(1f),
                    onClick = { imagePickerLauncher.launch("image/*") }
                )
                QuickActionButton(
                    icon = "🧠",
                    title = "Memory",
                    subtitle = "Saved Insights",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTab(NovaScreenTab.MEMORY_CENTER) }
                )
            }
        }

        // --- 4. Missed Session Recovery Alert Banner ---
        if (missedAlert != null) {
            val (planItem, message) = missedAlert!!
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CoralPink.copy(alpha = 0.12f),
                borderColor = CoralPink.copy(alpha = 0.45f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recovery",
                            tint = CoralPink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Smart Recovery Recommended",
                            fontWeight = FontWeight.Bold,
                            color = CoralPink,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.executeAction(NovaActionType.RECOVER_MISSED_SESSION, null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Recovery (20m)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { onNavigateToPlanner() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Reschedule", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- 5. Daily Briefing Card ---
        if (!dailyBriefingText.isNullOrBlank()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurface.copy(alpha = 0.8f),
                borderColor = ElectricIndigo.copy(alpha = 0.35f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Daily Brief",
                                tint = AmberGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Today's Briefing",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                if (voiceState == NovaVoiceState.SPEAKING) {
                                    viewModel.voiceManager.stopSpeaking()
                                } else {
                                    dailyBriefingText?.let { viewModel.voiceManager.speak(it) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (voiceState == NovaVoiceState.SPEAKING) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = "Speak Briefing",
                                tint = NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dailyBriefingText ?: "",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.88f),
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val firstSubject = studyContext.subjects.firstOrNull() ?: "Physics"
                                onNavigateToFocus(firstSubject, "Session 1", 25)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkCanvas, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Focus (25m)", fontWeight = FontWeight.Bold, color = DarkCanvas, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onNavigateToPlanner() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View Planner", fontSize = 12.sp, color = NeonCyan)
                        }
                    }
                }
            }
        }

        // --- 6. Adaptive Study Recommendation ("What Should I Study Next?") ---
        if (adaptiveRec != null) {
            val rec = adaptiveRec!!
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurfaceElevated.copy(alpha = 0.75f),
                borderColor = NeonCyan.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Adaptive",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "What to Study Next",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = rec.urgencyLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${rec.subject} • ${rec.topic}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rec.reasoning,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onNavigateToFocus(rec.subject, rec.topic, rec.targetMinutes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Start ${rec.targetMinutes}m ${rec.actionType}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // --- 7. Distracting Social Media & App Usage Monitor ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.75f),
            borderColor = if (socialNudge != null) CoralPink.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Usage Monitor",
                            tint = if (socialNudge != null) CoralPink else EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "App Usage Awareness",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    if (!hasUsagePermission) {
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        ) {
                            Text("Enable Access ⚙️", fontSize = 11.sp, color = NeonCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (hasUsagePermission) {
                    if (socialNudge != null) {
                        Text(
                            text = socialNudge ?: "",
                            fontSize = 13.sp,
                            color = CoralPink,
                            lineHeight = 18.sp
                        )
                    } else {
                        Text(
                            text = "Distracting app usage is healthy and within bounds. Focus Shield is ready when you begin studying.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    Text(
                        text = "Grant Usage Access so NOVA can gently remind you when social media usage delays your study plans.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // --- 8. Quick Voice Prompts Chips ---
        Column {
            Text(
                text = "Quick Voice Prompts",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickPromptChip(
                    text = "Aaj kya padhna hai?",
                    onClick = { viewModel.askPromptFromDashboard("Nova, aaj mujhe kya padhna chahiye?") }
                )
                QuickPromptChip(
                    text = "30 min focus mode laga do",
                    onClick = { viewModel.askPromptFromDashboard("Nova, 30 minute ka focus session start karo") }
                )
                QuickPromptChip(
                    text = "Physics ka quiz lo",
                    onClick = { viewModel.askPromptFromDashboard("Nova, Physics ka ek quick test lo") }
                )
                QuickPromptChip(
                    text = "Meri progress batao",
                    onClick = { viewModel.askPromptFromDashboard("Nova, meri overall progress aur consistency kaisi hai?") }
                )
            }
        }

        // --- 9. Bottom Quick "Ask NOVA..." Input Bar ---
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach Image Button
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach Question Image",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text Input Field
                TextField(
                    value = quickInputText,
                    onValueChange = { quickInputText = it },
                    placeholder = {
                        Text(
                            text = "Ask NOVA a doubt, concept or task...",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                // Voice Mic Button
                IconButton(
                    onClick = {
                        viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
                        onRequestMicPermission()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Send Button
                IconButton(
                    onClick = {
                        if (quickInputText.isNotBlank()) {
                            viewModel.askPromptFromDashboard(quickInputText)
                            quickInputText = ""
                        }
                    },
                    enabled = quickInputText.isNotBlank(),
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (quickInputText.isNotBlank()) ElectricIndigo else Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (quickInputText.isNotBlank()) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressStatItem(
    title: String,
    value: String,
    color: Color
) {
    Column {
        Text(text = title, fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun QuickActionButton(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = modifier.springClickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun QuickPromptChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.springClickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, fontSize = 12.sp, color = Color.White)
        }
    }
}

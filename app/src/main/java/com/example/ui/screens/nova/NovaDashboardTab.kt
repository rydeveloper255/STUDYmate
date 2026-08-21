package com.example.ui.screens.nova

import android.net.Uri
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.NovaScreenTab
import com.example.viewmodel.NovaViewModel

@Composable
fun NovaDashboardTab(
    viewModel: NovaViewModel,
    onNavigateToFocus: (subject: String, topic: String, duration: Int) -> Unit,
    onNavigateToPlanner: () -> Unit,
    onRequestMicPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val currentTheme = currentThemeMode()
    val context = LocalContext.current

    val studyContext by viewModel.studyContext.collectAsState()
    val analytics by viewModel.analyticsData.collectAsState()
    val dailyBriefingText by viewModel.dailyBriefingText.collectAsState()
    val adaptiveRec by viewModel.adaptiveRecommendation.collectAsState()
    val missedAlert by viewModel.missedSessionAlert.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val audioRms by viewModel.voiceManager.audioLevelRms.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val allSmartNotes by viewModel.allSmartNotes.collectAsState()
    val allCurrentAffairs by viewModel.allCurrentAffairs.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var quickInputText by remember { mutableStateOf("") }
    var showAllToolsDialog by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setAttachedImage(it)
            viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
        }
    }

    // Dynamic Greeting
    val greetingTime = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Late night study"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // =========================================================================
        // 1. COMPACT NOVA HEADER (Avatar/Orb, Title, Online Status, Tools Button)
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // NOVA Visualizer Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x2538BDF8) else Color(0x186366F1))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    NovaOrbVisualizer(
                        voiceState = voiceState,
                        audioRms = audioRms,
                        size = 42.dp
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✨ NOVA AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Online • Personal AI Study Assistant",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Quick Tools Button
            IconButton(
                onClick = { showAllToolsDialog = true },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x1CFFFFFF) else Color(0x10000000))
                    .border(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x2064748B), CircleShape)
                    .testTag("nova_all_tools_header_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = "All NOVA Tools",
                    tint = if (isDark) NeonCyan else DeepIndigo,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // =========================================================================
        // 2. PRIMARY NOVA ACTION (What can I help you with? + Input + Voice/Image/Send)
        // =========================================================================
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nova_primary_input_card"),
            shape = RoundedCornerShape(20.dp),
            elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 2.dp else 8.dp,
            borderColor = NeonCyan.copy(alpha = 0.45f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✨ What can I help you with?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                    // Language tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0x20818CF8) else Color(0x156366F1)
                    ) {
                        Text(
                            text = if (settings.language.contains("hi", ignoreCase = true)) "हिंदी / Hinglish" else "English / Bilingual",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isDark) ElectricViolet else DeepIndigo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Box Container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0x22131C2E) else Color(0x12000000),
                    border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x2564748B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Image attachment button
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .testTag("nova_input_image_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = "Attach Question or Problem Image",
                                tint = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Text Field
                        TextField(
                            value = quickInputText,
                            onValueChange = { quickInputText = it },
                            placeholder = {
                                Text(
                                    text = "Ask NOVA anything about your exam...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .testTag("nova_hub_text_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A)
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
                                .background(if (isDark) Color(0x2038BDF8) else Color(0x156366F1))
                                .testTag("nova_input_voice_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Voice Input",
                                tint = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Send Action Button
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
                                .then(
                                    if (quickInputText.isNotBlank()) {
                                        Modifier.background(
                                            brush = Brush.linearGradient(listOf(NeonCyan, DeepIndigo)),
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier.background(
                                            color = if (isDark) Color(0x10FFFFFF) else Color(0x0A000000),
                                            shape = CircleShape
                                        )
                                    }
                                )
                                .testTag("nova_input_send_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (quickInputText.isNotBlank()) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fast prompt starter chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PromptSuggestionChip(
                        label = "💡 Explain a Concept",
                        isDark = isDark,
                        onClick = { viewModel.askPromptFromDashboard("Explain the core exam concepts for today's target subject in simple terms.") }
                    )
                    PromptSuggestionChip(
                        label = "🎯 Test My Knowledge",
                        isDark = isDark,
                        onClick = { viewModel.setTab(NovaScreenTab.INTERACTIVE_STUDY_QUIZ) }
                    )
                    PromptSuggestionChip(
                        label = "⚡ 30m Focus Session",
                        isDark = isDark,
                        onClick = { viewModel.askPromptFromDashboard("Nova, 30 minute ka focused study session start karo") }
                    )
                    PromptSuggestionChip(
                        label = "📊 Check Readiness",
                        isDark = isDark,
                        onClick = { viewModel.setTab(NovaScreenTab.ANALYTICS_STRATEGY) }
                    )
                }
            }
        }

        // =========================================================================
        // 3. SMART QUICK ACTIONS (2-Column Clean Grid)
        // =========================================================================
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Study Help + Smart Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NovaQuickActionCard(
                    icon = Icons.Outlined.AutoStories,
                    iconTint = NeonCyan,
                    title = "📚 Study Help",
                    subtitle = "Ask concepts, doubts & explanations",
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    testTag = "nova_quick_study_help",
                    onClick = { viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT) }
                )

                NovaQuickActionCard(
                    icon = Icons.Outlined.Search,
                    iconTint = ElectricViolet,
                    title = "🔎 Smart Search",
                    subtitle = "Search academic information & citations",
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    testTag = "nova_quick_smart_search",
                    onClick = { viewModel.setTab(NovaScreenTab.SMART_SEARCH) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Smart Notes + Current Affairs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NovaQuickActionCard(
                    icon = Icons.Outlined.EditNote,
                    iconTint = GoldenSpark,
                    title = "📝 Smart Notes",
                    subtitle = "Create & manage revision notes",
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    testTag = "nova_quick_smart_notes",
                    onClick = { viewModel.setTab(NovaScreenTab.SMART_NOTES) }
                )

                NovaQuickActionCard(
                    icon = Icons.Outlined.Newspaper,
                    iconTint = NebulaPurple,
                    title = "📰 Current Affairs",
                    subtitle = "Exam Radar & daily awareness",
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    testTag = "nova_quick_current_affairs",
                    onClick = { viewModel.setTab(NovaScreenTab.CURRENT_AFFAIRS) }
                )
            }
        }

        // =========================================================================
        // 4. TODAY'S NOVA BRIEFING (Dynamic Glass Briefing Card)
        // =========================================================================
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nova_daily_briefing_card"),
            shape = RoundedCornerShape(18.dp),
            elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "☀️ TODAY'S AI BRIEFING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Audio Speak Button
                    IconButton(
                        onClick = {
                            if (voiceState == NovaVoiceState.SPEAKING) {
                                viewModel.voiceManager.stopSpeaking()
                            } else {
                                val textToSpeak = dailyBriefingText ?: "Welcome back ${studyContext.preferredTitle}. You have ${studyContext.examDaysRemaining} days remaining for ${studyContext.targetExam}."
                                viewModel.voiceManager.speak(textToSpeak)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (voiceState == NovaVoiceState.SPEAKING) Icons.Filled.Stop else Icons.Outlined.VolumeUp,
                            contentDescription = "Read Briefing Aloud",
                            tint = if (isDark) NeonCyan else DeepIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dynamic Briefing Text
                if (!dailyBriefingText.isNullOrBlank()) {
                    Text(
                        text = dailyBriefingText ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E293B),
                        lineHeight = 20.sp
                    )
                } else {
                    // Fallback structured dynamic briefing from live state
                    val targetSubject = studyContext.subjects.firstOrNull() ?: "General Studies"
                    val countdown = studyContext.examDaysRemaining
                    val exam = studyContext.targetExam.ifBlank { "Competitive Exam" }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "• $greetingTime, ${studyContext.preferredTitle}! You have $countdown days remaining for $exam.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1E293B)
                        )
                        Text(
                            text = "• NOVA recommends a 30-minute focused session in $targetSubject today to maintain consistent syllabus progress.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val sub = studyContext.subjects.firstOrNull() ?: "General Science"
                            onNavigateToFocus(sub, "Core Syllabus Sprint", 25)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0x2838BDF8) else Color(0x186366F1),
                            contentColor = if (isDark) NeonCyan else DeepIndigo
                        ),
                        border = BorderStroke(0.5.dp, if (isDark) NeonCyan.copy(alpha = 0.4f) else DeepIndigo.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start 25m Focus", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToPlanner,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x2564748B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "View Planner",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 5. RECOMMENDED NEXT ACTION (Adaptive Recommendation Card)
        // =========================================================================
        if (adaptiveRec != null) {
            val rec = adaptiveRec!!
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nova_recommendation_card"),
                shape = RoundedCornerShape(18.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 6.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨ NOVA RECOMMENDS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                letterSpacing = 0.8.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0x2038BDF8) else Color(0x156366F1),
                            border = BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = rec.urgencyLabel.ifBlank { "Priority Topic" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${rec.subject} • ${rec.topic}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rec.reasoning,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onNavigateToFocus(rec.subject, rec.topic, rec.targetMinutes)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepIndigo
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Start ${rec.targetMinutes}m ${rec.actionType}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 6. CURRENT AFFAIRS ENTRY (Compact Glass Card)
        // =========================================================================
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nova_current_affairs_entry"),
            shape = RoundedCornerShape(18.dp),
            elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 4.dp,
            onClick = { viewModel.setTab(NovaScreenTab.CURRENT_AFFAIRS) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x28A855F7) else Color(0x18A855F7))
                            .border(0.5.dp, NebulaPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Newspaper,
                            contentDescription = "Current Affairs",
                            tint = NebulaPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "📰 Current Affairs & Exam Radar",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val latestUpdate = allCurrentAffairs.firstOrNull()?.title ?: "Realtime exam-relevant news & notifications"
                        Text(
                            text = latestUpdate,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0x20A855F7) else Color(0x15A855F7),
                    border = BorderStroke(0.5.dp, NebulaPurple.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NebulaPurple
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = NebulaPurple,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 7. RECENT NOVA ACTIVITY (Compact Recent Qs / Notes)
        // =========================================================================
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "RECENT ACTIVITY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val userMessages = remember(messages) {
                messages.filter { it.sender == NovaSender.USER }.takeLast(3).reversed()
            }

            if (userMessages.isNotEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 4.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        userMessages.forEachIndexed { index, msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = if (isDark) NeonCyan else DeepIndigo,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Open Chat",
                                    tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (index < userMessages.size - 1) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = if (isDark) Color(0x1FFFFFFF) else Color(0x12000000)
                                )
                            }
                        }
                    }
                }
            } else {
                // Empty state
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "No recent activity",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                            Text(
                                text = "Ask NOVA your first question above to get started.",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 8. ALL NOVA TOOLS BUTTON (View All NOVA Tools →)
        // =========================================================================
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("view_all_nova_tools_card"),
            shape = RoundedCornerShape(18.dp),
            elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 4.dp,
            onClick = { showAllToolsDialog = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GridView,
                        contentDescription = null,
                        tint = if (isDark) NeonCyan else DeepIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "View All NOVA Tools",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Quiz Intelligence, Voice Notes, Strategy & Memory",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Tools",
                    tint = if (isDark) NeonCyan else DeepIndigo,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }

    // =========================================================================
    // ALL TOOLS MODAL DIALOG
    // =========================================================================
    if (showAllToolsDialog) {
        NovaAllToolsModal(
            isDark = isDark,
            onDismiss = { showAllToolsDialog = false },
            onSelectTab = { tab ->
                viewModel.setTab(tab)
                showAllToolsDialog = false
            }
        )
    }
}

@Composable
private fun NovaQuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = if (isDark) 0.22f else 0.14f))
                    .border(0.5.dp, iconTint.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun PromptSuggestionChip(
    label: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0x18FFFFFF) else Color(0x0A000000),
        border = BorderStroke(0.5.dp, if (isDark) Color(0x25FFFFFF) else Color(0x2064748B)),
        modifier = Modifier.springClickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun NovaAllToolsModal(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSelectTab: (NovaScreenTab) -> Unit
) {
    val allTools = listOf(
        Triple(NovaScreenTab.ASSISTANT_CHAT, "🤖 Voice & Chat", "Multi-turn tutor, voice AI & doubt scanner"),
        Triple(NovaScreenTab.SMART_SEARCH, "🔍 Smart Search", "Web-verified citations & academic explorer"),
        Triple(NovaScreenTab.SMART_NOTES, "📝 Smart Notes", "Structured markdown notes & formula cheatsheets"),
        Triple(NovaScreenTab.CURRENT_AFFAIRS, "📰 Current Affairs", "Exam radar, daily updates & awareness feeds"),
        Triple(NovaScreenTab.INTERACTIVE_STUDY_QUIZ, "🎯 Quiz Intelligence", "Adaptive tests & concept remediations"),
        Triple(NovaScreenTab.VOICE_NOTES, "🎙️ Voice Notes", "Lecture transcriber & voice audio summary"),
        Triple(NovaScreenTab.ANALYTICS_STRATEGY, "📊 Study Strategy", "Consistency tracking, weakness & time analytics"),
        Triple(NovaScreenTab.MEMORY_CENTER, "🧠 Memory Center", "Learner context facts & persistent preferences"),
        Triple(NovaScreenTab.NOVA_SETTINGS, "⚙️ Settings & Privacy", "AI Voice rate, language & personalization")
    )

    GlassDialog(
        onDismissRequest = onDismiss,
        title = "✨ All NOVA Tools",
        subtitle = "Select any tool to assist your exam preparation",
        dismissText = "Close"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allTools.forEach { (tab, title, desc) ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = 1.dp,
                    onClick = { onSelectTab(tab) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (isDark) NeonCyan else DeepIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

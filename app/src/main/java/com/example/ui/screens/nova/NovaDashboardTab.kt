package com.example.ui.screens.nova

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val allConversations by viewModel.allNovaConversations.collectAsState()

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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // =========================================================================
        // 1. COMPACT NOVA HEADER (Avatar/Orb, Title, Online Status, Target Exam)
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

            // Target Exam Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ElectricIndigo.copy(alpha = 0.2f),
                border = BorderStroke(0.8.dp, ElectricIndigo.copy(alpha = 0.45f))
            ) {
                Text(
                    text = studyContext.targetExam.ifBlank { "SSC CGL" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // =========================================================================
        // 2. HERO QUICK ASK CARD (Routes directly into Nova Chat)
        // =========================================================================
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nova_primary_input_card"),
            shape = RoundedCornerShape(20.dp),
            elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 2.dp else 6.dp,
            borderColor = NeonCyan.copy(alpha = 0.45f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "💬 $greetingTime, Aspirant",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    // Language Switcher Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0x20818CF8) else Color(0x156366F1),
                        border = BorderStroke(0.5.dp, if (isDark) Color(0x40818CF8) else Color(0x306366F1)),
                        modifier = Modifier.springClickable { viewModel.toggleLanguageMode() }
                    ) {
                        Text(
                            text = "🌐 ${settings.language}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) NeonCyan else DeepIndigo,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Box with action buttons
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) DarkSurface.copy(alpha = 0.85f) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0x18000000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = quickInputText,
                            onValueChange = { quickInputText = it },
                            placeholder = {
                                Text(
                                    text = "Ask any syllabus doubt, formula or concept...",
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nova_quick_text_input"),
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

                        // Attach Photo
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = "Attach question photo",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Voice Mic
                        IconButton(
                            onClick = { onRequestMicPermission() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Send / Start Chat
                        val hasPrompt = quickInputText.isNotBlank()
                        IconButton(
                            onClick = {
                                if (hasPrompt) {
                                    viewModel.sendMessage(quickInputText)
                                    quickInputText = ""
                                    viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
                                } else {
                                    viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasPrompt) Brush.linearGradient(listOf(NeonCyan, ElectricIndigo))
                                    else Brush.linearGradient(listOf(ElectricIndigo.copy(alpha = 0.5f), ElectricIndigo.copy(alpha = 0.5f)))
                                )
                                .testTag("nova_quick_send_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Prompt Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val promptChips = listOf(
                        "Explain PYQ Concept" to "Explain previous year question patterns for ${studyContext.targetExam}",
                        "Formula Cheat Sheet" to "Create a quick formula cheat sheet for ${studyContext.selectedSubject}",
                        "15-Min Study Plan" to "Give me a high-yield 15 minute revision roadmap for today",
                        "Clear My Doubts" to "I have a doubt regarding today's study topic"
                    )
                    items(promptChips) { (title, prompt) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) DarkSurface.copy(alpha = 0.7f) else Color(0xFFF1F5F9),
                            border = BorderStroke(0.5.dp, if (isDark) Color(0x30FFFFFF) else Color(0x20000000)),
                            modifier = Modifier.springClickable {
                                viewModel.sendMessage(prompt)
                                viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 $title",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. RECENT DISCUSSIONS (LOCAL-FIRST QUICK RESUME)
        // =========================================================================
        if (allConversations.isNotEmpty()) {
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
                        text = "Recent Discussions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "View All (${allConversations.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan,
                        modifier = Modifier.springClickable { viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT) }
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allConversations.take(4), key = { it.id }) { conv ->
                        RecentConversationCard(
                            conversation = conv,
                            isDark = isDark,
                            onClick = {
                                viewModel.loadConversationEntity(conv)
                                viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 4. NOVA AI TOOLS SUITE (DEDICATED SCREEN LAUNCHERS)
        // =========================================================================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Nova AI Tools Suite",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            // Tool 1: Nova Chat (ChatGPT-style)
            NovaLauncherCard(
                icon = "💬",
                title = "Nova Chat",
                badge = "Main AI Companion",
                badgeColor = NeonCyan,
                subtitle = "Interactive study doubt solver, concept breakdowns, step-by-step reasoning & saved history.",
                isDark = isDark,
                accentColor = NeonCyan,
                onClick = { viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT) }
            )

            // Tool 2: AI Smart Notes & Flashcards
            NovaLauncherCard(
                icon = "📝",
                title = "Smart Notes & Summaries",
                badge = "${allSmartNotes.size} Saved Notes",
                badgeColor = ElectricIndigo,
                subtitle = "AI-generated exam-focused revision summaries, key formulas, bullet points & markdown exports.",
                isDark = isDark,
                accentColor = ElectricIndigo,
                onClick = { viewModel.setTab(NovaScreenTab.SMART_NOTES) }
            )

            // Tool 3: Voice Notes & Audio Learning
            NovaLauncherCard(
                icon = "🎙️",
                title = "Voice Notes & Audio AI",
                badge = "Hands-Free",
                badgeColor = CoralPink,
                subtitle = "Speak lectures or thoughts to generate structured study notes, transcriptions & audio revisions.",
                isDark = isDark,
                accentColor = CoralPink,
                onClick = { viewModel.setTab(NovaScreenTab.VOICE_NOTES) }
            )

            // Tool 4: AI Interactive Quiz Generator
            NovaLauncherCard(
                icon = "🎯",
                title = "AI Quiz & Test Generator",
                badge = "Adaptive MCQs",
                badgeColor = EmeraldGreen,
                subtitle = "Generate customized exam-level practice questions, timed quizzes & instant answer explanations.",
                isDark = isDark,
                accentColor = EmeraldGreen,
                onClick = { viewModel.setTab(NovaScreenTab.INTERACTIVE_STUDY_QUIZ) }
            )

            // Tool 5: Current Affairs & Daily Briefing
            NovaLauncherCard(
                icon = "📰",
                title = "Current Affairs AI",
                badge = "${allCurrentAffairs.size} Digest Items",
                badgeColor = AmberGold,
                subtitle = "Curated national & international news digests, editorial analysis & exam relevance tags.",
                isDark = isDark,
                accentColor = AmberGold,
                onClick = { viewModel.setTab(NovaScreenTab.CURRENT_AFFAIRS) }
            )

            // Tool 6: AI Strategy & Study Analytics
            NovaLauncherCard(
                icon = "📊",
                title = "Strategy & Mastery Analytics",
                badge = "${analytics.completedSessionsCount} Sessions",
                badgeColor = ElectricIndigo,
                subtitle = "Deep revision tracking, weak-area detection, spaced repetition alerts & personalized strategy.",
                isDark = isDark,
                accentColor = ElectricIndigo,
                onClick = { viewModel.setTab(NovaScreenTab.ANALYTICS_STRATEGY) }
            )

            // Tool 7: Nova Memory & Personalization Center
            NovaLauncherCard(
                icon = "🧠",
                title = "Nova Memory & Personalization",
                badge = if (settings.memoryEnabled) "Active" else "Off",
                badgeColor = AmberGold,
                subtitle = "Inspect and manage user-approved learning preferences, exam targets & custom memory keys.",
                isDark = isDark,
                accentColor = AmberGold,
                onClick = { viewModel.setTab(NovaScreenTab.MEMORY_CENTER) }
            )
        }

        // =========================================================================
        // 5. DAILY BRIEFING / STUDY INSIGHT BANNER
        // =========================================================================
        if (dailyBriefingText?.isNotBlank() == true) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = if (isDark) DarkSurfaceElevated.copy(alpha = 0.85f) else Color.White,
                borderColor = AmberGold.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AmberGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily AI Study Insight",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dailyBriefingText ?: "",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RecentConversationCard(
    conversation: NovaConversationEntity,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val dateStr = remember(conversation.updatedAt) {
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(conversation.updatedAt))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) DarkSurface else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color(0x18000000)),
        modifier = Modifier
            .width(190.dp)
            .springClickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💬", fontSize = 14.sp)
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = conversation.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = conversation.lastMessagePreview.ifBlank { "Discussion with Nova AI" },
                fontSize = 10.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun NovaLauncherCard(
    icon: String,
    title: String,
    badge: String,
    badgeColor: Color,
    subtitle: String,
    isDark: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) DarkSurface.copy(alpha = 0.9f) else Color.White,
        border = BorderStroke(1.dp, if (isDark) accentColor.copy(alpha = 0.25f) else Color(0x15000000)),
        modifier = Modifier
            .fillMaxWidth()
            .springClickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Icon Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 20.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        if (badge.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Forward Arrow
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open $title",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

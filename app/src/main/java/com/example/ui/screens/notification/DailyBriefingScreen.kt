package com.example.ui.screens.notification

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.example.data.model.DailyBriefingData
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyBriefingScreen(
    briefingData: DailyBriefingData,
    onStartPractice: (subject: String, topic: String) -> Unit,
    onReadCurrentAffairs: () -> Unit,
    onResumeTest: () -> Unit,
    onStartRevision: () -> Unit,
    onAskNova: (prompt: String) -> Unit,
    onChangeLanguage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(enabled = true) {
        onBack()
    }

    val scrollState = rememberScrollState()

    val isHindi = briefingData.language.equals("Hindi", ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isHindi) "☀️ Dainik Briefing" else "☀️ Daily Briefing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Language Switcher
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x20FFFFFF))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (!isHindi) NeonCyan else Color.Transparent)
                                .clickable { onChangeLanguage("English") }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isHindi) Color(0xFF070B19) else Color(0xFF94A3B8)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isHindi) NeonCyan else Color.Transparent)
                                .clickable { onChangeLanguage("Hindi") }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHindi) Color(0xFF070B19) else Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B1021)
                )
            )
        },
        containerColor = Color(0xFF070B19)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = briefingData.dateString.ifBlank { "Today's Briefing" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Text(
                        text = briefingData.greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isHindi) "Aaj ki padhai ka sabse mahatvapurna summary aur action items Yahan hain:" else "Here is your concise daily preparation briefing tailored to your current learning data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // 1. TODAY'S FOCUS SECTION
            if (briefingData.focusTopic != null) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TrackChanges, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "🎯 Aaj Ka Focus Topic" else "🎯 Today's Focus",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x2000E5FF))
                                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${briefingData.focusSubject ?: "Subject"} • ${briefingData.focusTopic}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (!briefingData.focusReason.isNullOrBlank()) {
                                    Text(
                                        text = briefingData.focusReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onStartPractice(
                                    briefingData.focusSubject ?: "General Science",
                                    briefingData.focusTopic
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("briefing_start_practice_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFF070B19), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "Targeted Practice Shuru Karein" else "Start Practice",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF070B19)
                            )
                        }
                    }
                }
            }

            // 2. UNFINISHED TEST ALERT (If applicable)
            if (briefingData.unfinishedTestTitle != null) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.HourglassTop, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "⏱️ Adhoora Mock Test" else "⏱️ Unfinished Mock Test",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark
                            )
                        }

                        Text(
                            text = "${briefingData.unfinishedTestTitle} (${briefingData.unfinishedTestProgress ?: "In Progress"})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        OutlinedButton(
                            onClick = onResumeTest,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenSpark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("briefing_resume_test_button")
                        ) {
                            Icon(Icons.Filled.Restore, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "Test Resume Karein" else "Resume Mock Test",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. CURRENT AFFAIRS SECTION
            if (briefingData.currentAffairsCount > 0) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Newspaper, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "📰 Aaj Ke Samachar & Current Affairs" else "📰 Current Affairs",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark
                            )
                        }

                        Text(
                            text = if (isHindi) "${briefingData.currentAffairsCount} naye important current affairs tayar hain." else "${briefingData.currentAffairsCount} key verified updates are ready for today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )

                        if (!briefingData.currentAffairsHeadline.isNullOrBlank()) {
                            Text(
                                text = "• \"${briefingData.currentAffairsHeadline}\"",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = onReadCurrentAffairs,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("briefing_read_ca_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Color(0xFF070B19), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "Current Affairs Padhein" else "Read Current Affairs",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF070B19)
                            )
                        }
                    }
                }
            }

            // 4. SPACED REVISION QUEUE (If applicable)
            if (briefingData.revisionQuestionsCount > 0) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Psychology, null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "🧠 Spaced Revision Due" else "🧠 Spaced Revision Due",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricViolet
                            )
                        }

                        Text(
                            text = if (isHindi) "${briefingData.revisionQuestionsCount} prashna revision ke liye due hain." else "You have ${briefingData.revisionQuestionsCount} saved questions due for memory retention review.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )

                        OutlinedButton(
                            onClick = onStartRevision,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricViolet),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Repeat, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "Revision Shuru Karein" else "Start Revision Queue",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 5. EXAM RADAR COUNTDOWN
            if (briefingData.examDaysRemaining != null) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isHindi) "📈 Exam Radar" else "📈 Exam Radar",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = briefingData.examName ?: "Target Exam",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonCyan.copy(alpha = 0.2f))
                                .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${briefingData.examDaysRemaining} ${if (isHindi) "Din Baki" else "Days Left"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }

            // 6. ASK NOVA PROMPT BAR
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFFFF4081), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "✦ NOVA Se Poochhein" else "✦ Ask NOVA",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF4081)
                        )
                    }

                    Text(
                        text = if (isHindi) "Aaj ki taiyari ka custom study plan ya strategy NOVA AI se discuss karein." else "Get an AI strategy or explanation for today's focus topic.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    Button(
                        onClick = {
                            val prompt = if (isHindi) {
                                "Mera aaj ka focus topic ${briefingData.focusTopic ?: "General"} hai. Iski best 20-min revision strategy batao."
                            } else {
                                "My focus topic today is ${briefingData.focusTopic ?: "General"}. What is the most effective 20-minute study strategy for this?"
                            }
                            onAskNova(prompt)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("briefing_ask_nova_button")
                    ) {
                        Icon(Icons.Filled.ChatBubble, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "NOVA Se Strategy Poochhein" else "Ask NOVA Strategy",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

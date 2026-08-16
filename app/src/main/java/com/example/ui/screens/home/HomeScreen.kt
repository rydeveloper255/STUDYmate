package com.example.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DailyMission
import com.example.data.model.StudyPlanItem
import com.example.data.model.UserProfile
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.Calendar

import com.example.data.model.AiCoachRecommendation
import com.example.data.model.FlashcardItem
import com.example.data.model.RevisionCategory
import com.example.data.model.StudyNowRecommendation

@Composable
fun HomeScreen(
    user: UserProfile?,
    studyPlan: List<StudyPlanItem>,
    missions: List<DailyMission>,
    flashcards: List<FlashcardItem> = emptyList(),
    aiCoachRecommendation: AiCoachRecommendation? = null,
    studyNowRecommendation: StudyNowRecommendation? = null,
    isAiCoachLoading: Boolean = false,
    isStudyNowLoading: Boolean = false,
    onLoadAiCoach: (String) -> Unit = {},
    onLoadStudyNow: () -> Unit = {},
    onTogglePlanItem: (Long, Boolean) -> Unit,
    onStartFocusSession: (subject: String, topic: String) -> Unit,
    onNavigateToTab: (AppNavTab) -> Unit,
    onOpenProfileSettings: () -> Unit,
    onScanQuestion: () -> Unit,
    onOpenDocumentSummarizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingTime = when (currentHour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }
    val studentName = user?.name?.ifBlank { "Student" } ?: "Student"

    val totalFocusMins = user?.totalFocusMinutes ?: 0
    val targetMins = user?.dailyTargetMinutes ?: 180
    val progressFraction = (totalFocusMins.toFloat() / targetMins.toFloat()).coerceIn(0f, 1f)

    val examDaysRemaining = remember(user?.examDateMillis) {
        val diff = (user?.examDateMillis ?: (System.currentTimeMillis() + 30L * 86400000)) - System.currentTimeMillis()
        (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1).toInt()
    }

    val isDark = isAppInDarkTheme()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundGradient(isDark))
            .padding(horizontal = 18.dp)
            .testTag("home_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header with User Greeting & Profile Avatar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudyMateBrandLogo(
                        size = 42.dp,
                        showTypography = false,
                        animated = false
                    )

                    Column {
                        Text(
                            text = "$greetingTime, $studentName 👋",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "StudyMate AI • Learn • Focus",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onOpenProfileSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x18FFFFFF) else Color(0x10000000))
                            .testTag("home_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsNone,
                            contentDescription = "Notifications & Reminders",
                            tint = if (isDark) NeonCyan else Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    ThemeToggleButton(testTag = "home_theme_toggle")
                    StreakBadge(streakDays = user?.streakDays ?: 1)

                    // Profile Avatar Trigger
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x3338BDF8))
                            .border(1.5.dp, NeonCyan, CircleShape)
                            .springClickable(testTag = "profile_avatar_button", onClick = onOpenProfileSettings),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user?.photoUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile",
                                tint = if (isDark) Color.White else Color(0xFF0F172A),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Large Glass Card: Today's Progress
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
                fillAlpha = 0.75f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Timeline,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TODAY'S PROGRESS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val hours = totalFocusMins / 60
                        val mins = totalFocusMins % 60
                        val targetHours = targetMins / 60
                        val targetRemainingMins = targetMins % 60

                        Text(
                            text = "${hours}h ${mins}m / ${targetHours}h ${if (targetRemainingMins > 0) "${targetRemainingMins}m" else ""}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (progressFraction >= 1f) "🎯 Daily target reached! Outstanding work."
                            else "${((1f - progressFraction) * targetMins).toInt()} mins left to hit daily goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        XpBadge(xp = user?.xp ?: 0, level = user?.level ?: 1)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    LiquidProgressRing(
                        progress = progressFraction,
                        currentText = "${(progressFraction * 100).toInt()}%",
                        targetText = "Goal",
                        size = 105.dp,
                        strokeWidth = 10.dp
                    )
                }
            }
        }

        // 2b. AI Study Coach Card
        item {
            var showWhyDialog by remember { mutableStateOf(false) }
            var isCoachDismissed by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (aiCoachRecommendation == null && !isAiCoachLoading) {
                    val defaultSub = user?.subjects?.firstOrNull() ?: "Physics"
                    onLoadAiCoach(defaultSub)
                }
            }

            if (!isCoachDismissed) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = 0.8f
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(GoldenSpark, ElectricViolet))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Psychology,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "✨ AI Study Coach",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAiCoachLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = NeonCyan,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = { isCoachDismissed = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, "Dismiss", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val recMessage = aiCoachRecommendation?.message ?: "Your Physics accuracy improved this week. Today, revise Current Electricity for 25 minutes and then attempt 10 questions. 🚀"
                        val recSubject = aiCoachRecommendation?.subject ?: "Physics"
                        val recTopic = aiCoachRecommendation?.topic ?: "Current Electricity"

                        Text(
                            text = recMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF1F5F9),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onStartFocusSession(recSubject, recTopic) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.2f).height(38.dp)
                            ) {
                                Text("Start Now", color = Color(0xFF070B19), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { showWhyDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x5038BDF8)),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Why this?", color = NeonCyan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = { isCoachDismissed = true },
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("Later", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (showWhyDialog) {
                    AlertDialog(
                        onDismissRequest = { showWhyDialog = false },
                        containerColor = Color(0xFF131C2E),
                        shape = RoundedCornerShape(20.dp),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lightbulb, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Why this recommendation?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        },
                        text = {
                            Text(
                                text = aiCoachRecommendation?.whyThisExplanation ?: "Gemini analyzed your recent test performance, pending study plan tasks, and time since last revision. Focusing on this topic now optimizes your memory retention curve and addresses your weakest accuracy area before your upcoming exam.",
                                color = Color(0xFFCBD5E1),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showWhyDialog = false
                                    onStartFocusSession(
                                        aiCoachRecommendation?.subject ?: "Physics",
                                        aiCoachRecommendation?.topic ?: "Current Electricity"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Text("Got it, Start Focus!", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showWhyDialog = false }) {
                                Text("Close", color = Color(0xFF94A3B8))
                            }
                        }
                    )
                }
            }
        }

        // 2c. "What Should I Study Now?" Smart Action Card
        item {
            LaunchedEffect(Unit) {
                if (studyNowRecommendation == null && !isStudyNowLoading) {
                    onLoadStudyNow()
                }
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.75f
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33F43F5E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdsClick,
                                    contentDescription = null,
                                    tint = CoralRose,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "What should I study now? 🎯",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (isStudyNowLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = CoralRose,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = onLoadStudyNow,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, "Refresh", tint = CoralRose, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val recSubject = studyNowRecommendation?.subject ?: "Physics"
                    val recTopic = studyNowRecommendation?.topic ?: "Current Electricity"
                    val recMins = studyNowRecommendation?.targetMinutes ?: 30
                    val recReason = studyNowRecommendation?.reasoning ?: "Evaluated current time, exam countdown & revision due."

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x25F43F5E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40F43F5E))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.TaskAlt, null, tint = CoralRose, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Study $recSubject → $recTopic → $recMins min",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = recReason,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onStartFocusSession(recSubject, recTopic) },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRose),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Focus", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onLoadStudyNow,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x35FFFFFF)),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text("Choose Something Else", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2d. Smart Revision Radar Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.7f
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Radar, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Revision Radar 🔄",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        TextButton(onClick = { onNavigateToTab(AppNavTab.STUDY) }) {
                            Text("Flashcards", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val now = remember { System.currentTimeMillis() }
                    val reviseNowCount = if (flashcards.isNotEmpty()) {
                        flashcards.count { it.status == RevisionCategory.REVISE_NOW || it.nextReviewDate <= now }
                    } else {
                        user?.weakSubjects?.size ?: 0
                    }

                    val practiceSoonCount = if (flashcards.isNotEmpty()) {
                        flashcards.count { it.status == RevisionCategory.PRACTICE_SOON && it.nextReviewDate > now }
                    } else {
                        user?.subjects?.size?.minus(user?.weakSubjects?.size ?: 0)?.coerceAtLeast(0) ?: 0
                    }

                    val strongCount = if (flashcards.isNotEmpty()) {
                        flashcards.count { it.status == RevisionCategory.STRONG }
                    } else {
                        user?.strongSubjects?.size ?: 0
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RevisionRadarBadge(
                            label = "🔴 Revise Now",
                            count = "$reviseNowCount Cards",
                            color = CoralRose,
                            modifier = Modifier.weight(1f)
                        )
                        RevisionRadarBadge(
                            label = "🟡 Practice Soon",
                            count = "$practiceSoonCount Cards",
                            color = GoldenSpark,
                            modifier = Modifier.weight(1f)
                        )
                        RevisionRadarBadge(
                            label = "🟢 Strong",
                            count = "$strongCount Cards",
                            color = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        item {
            val topSubject = studyPlan.firstOrNull { !it.isCompleted }?.subject ?: "Physics"
            val topTopic = studyPlan.firstOrNull { !it.isCompleted }?.topic ?: "Core Concepts"

            GlassButton(
                text = "▶ Start Focus Session ($topSubject)",
                onClick = { onStartFocusSession(topSubject, topTopic) },
                icon = Icons.Filled.PlayArrow,
                isPrimary = true,
                testTag = "start_focus_session_cta"
            )
        }

        // 4. Quick Actions Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    title = "Ask Gemini",
                    icon = Icons.Filled.AutoAwesome,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f),
                    testTag = "qa_ask_gemini",
                    onClick = { onNavigateToTab(AppNavTab.AI_TUTOR) }
                )
                QuickActionItem(
                    title = "Take Test",
                    icon = Icons.Filled.Quiz,
                    color = ElectricViolet,
                    modifier = Modifier.weight(1f),
                    testTag = "qa_take_test",
                    onClick = { onNavigateToTab(AppNavTab.PROGRESS) }
                )
                QuickActionItem(
                    title = "Scan Question",
                    icon = Icons.Filled.CameraAlt,
                    color = NebulaPurple,
                    modifier = Modifier.weight(1f),
                    testTag = "qa_scan_question",
                    onClick = onScanQuestion
                )
                QuickActionItem(
                    title = "Revise",
                    icon = Icons.Filled.Psychology,
                    color = GoldenSpark,
                    modifier = Modifier.weight(1f),
                    testTag = "qa_revise",
                    onClick = { onNavigateToTab(AppNavTab.STUDY) }
                )
            }
        }

        // Document Summarizer Feature Banner Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenDocumentSummarizer,
                testTag = "home_doc_summarizer_card",
                shape = RoundedCornerShape(18.dp),
                fillAlpha = 0.65f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(DeepIndigo, NeonCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = "Document Summarizer",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Document Summarizer & Qs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("AI", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Parse PDF / notes to generate concise bullet summaries & exam study questions",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 5. Exam Countdown Banner
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                fillAlpha = 0.5f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33F43F5E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CalendarMonth, null, tint = CoralRose, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = user?.examName?.ifBlank { "Major Exam" } ?: "Major Exam",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Exam Countdown: $examDaysRemaining days remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = CoralRose
                            )
                        }
                    }

                    TextButton(
                        onClick = { onNavigateToTab(AppNavTab.STUDY) },
                        modifier = Modifier.testTag("view_roadmap_button")
                    ) {
                        Text("Roadmap", color = NeonCyan, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 6. Today's Plan Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "${studyPlan.count { it.isCompleted }} / ${studyPlan.size} Done",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // 7. Today's Plan Items
        if (studyPlan.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    fillAlpha = 0.5f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = NeonCyan, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No study tasks for today.",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { onNavigateToTab(AppNavTab.STUDY) }) {
                            Text("Generate AI Plan with Gemini ✨", color = NeonCyan)
                        }
                    }
                }
            }
        } else {
            items(studyPlan, key = { it.id }) { item ->
                PlanItemGlassCard(
                    item = item,
                    onToggleComplete = { onTogglePlanItem(item.id, !item.isCompleted) },
                    onStartFocus = { onStartFocusSession(item.subject, item.topic) }
                )
            }
        }

        // 8. Daily Missions Section
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Daily Missions 🎯",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        items(missions, key = { it.id }) { mission ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                fillAlpha = 0.5f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (mission.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (mission.isCompleted) EmeraldSuccess else Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = mission.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${mission.current}/${mission.target} ${mission.unit} • +${mission.xpReward} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldenSpark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanItemGlassCard(
    item: StudyPlanItem,
    onToggleComplete: () -> Unit,
    onStartFocus: () -> Unit
) {
    val subjectIcon = when {
        item.subject.contains("Math", ignoreCase = true) -> "📐"
        item.subject.contains("Phys", ignoreCase = true) -> "⚡"
        item.subject.contains("Chem", ignoreCase = true) -> "🧪"
        item.subject.contains("Bio", ignoreCase = true) -> "🧬"
        item.subject.contains("Comp", ignoreCase = true) -> "💻"
        else -> "📚"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        fillAlpha = if (item.isCompleted) 0.35f else 0.7f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subjectIcon,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.subject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isCompleted) Color(0xFF94A3B8) else Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${item.targetMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${item.chapter} — ${item.topic}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isCompleted) Color(0xFF64748B) else Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.isCompleted) {
                    IconButton(
                        onClick = onStartFocus,
                        modifier = Modifier.testTag("plan_focus_btn_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = "Focus",
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldSuccess,
                        checkmarkColor = Color(0xFF070B19)
                    ),
                    modifier = Modifier.testTag("plan_checkbox_${item.id}")
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        modifier = modifier
            .glassEffect(shape = RoundedCornerShape(16.dp), fillAlpha = 0.6f)
            .springClickable(testTag = testTag, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RevisionRadarBadge(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

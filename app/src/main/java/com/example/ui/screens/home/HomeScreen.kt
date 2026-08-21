package com.example.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: UserProfile?,
    studyPlan: List<StudyPlanItem>,
    missions: List<DailyMission> = emptyList(),
    flashcards: List<FlashcardItem> = emptyList(),
    studentMasterContext: StudentMasterContext? = null,
    aiCoachRecommendation: AiCoachRecommendation? = null,
    studyNowRecommendation: StudyNowRecommendation? = null,
    isAiCoachLoading: Boolean = false,
    isStudyNowLoading: Boolean = false,
    examReadiness: ExamReadinessScore? = null,
    onLoadAiCoach: (String) -> Unit = {},
    onLoadStudyNow: () -> Unit = {},
    onSelectTimeAvailable: (Int?) -> Unit = {},
    onPerformSmartSearch: (String) -> Unit = {},
    onTogglePlanItem: (Long, Boolean) -> Unit = { _, _ -> },
    onStartFocusSession: (subject: String, topic: String) -> Unit,
    onNavigateToTab: (AppNavTab) -> Unit,
    onOpenProfileSettings: () -> Unit,
    onOpenExamReadinessCenter: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onScanQuestion: () -> Unit = {},
    onOpenDocumentSummarizer: () -> Unit = {},
    onUpdateUserProfile: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val themeController = LocalThemeController.current
    val currentTheme = currentThemeMode()

    // Modals state
    var showExamSwitcherDialog by remember { mutableStateOf(false) }
    var showQuickSearchDialog by remember { mutableStateOf(false) }
    var showNotificationSummaryDialog by remember { mutableStateOf(false) }

    // 1. Time-based dynamic greeting
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingTime = when (currentHour) {
        in 4..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    val studentDisplayName = remember(user?.name) {
        cleanDisplayName(user?.name)
    }

    // 2. Exam Context & Dynamic Countdown
    val selectedExamName = user?.examName?.ifBlank { "Competitive / Board Exam" } ?: "Competitive / Board Exam"
    val targetDateMillis = user?.examDateMillis ?: (System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000)
    val daysRemaining = remember(targetDateMillis) {
        val diff = targetDateMillis - System.currentTimeMillis()
        (diff / (1000L * 60 * 60 * 24)).coerceAtLeast(1).toInt()
    }
    val formattedTargetDate = remember(targetDateMillis) {
        try {
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(targetDateMillis))
        } catch (e: Exception) {
            "20 Aug 2027"
        }
    }

    // 3. Smart Mission Determination (The ONE Primary Mission)
    val nextPendingTask = remember(studyPlan) {
        studyPlan.firstOrNull { !it.isCompleted }
    }
    val completedPlanCount = remember(studyPlan) {
        studyPlan.count { it.isCompleted }
    }
    val totalPlanCount = remember(studyPlan) {
        studyPlan.size
    }

    val missionSubject = nextPendingTask?.subject
        ?: studyNowRecommendation?.subject
        ?: user?.weakSubjects?.firstOrNull()
        ?: user?.subjects?.firstOrNull()
        ?: "General Science"

    val missionTopic = nextPendingTask?.topic
        ?: studyNowRecommendation?.topic
        ?: user?.weakTopics?.firstOrNull()
        ?: "Core Foundation & Practice"

    val missionTargetMinutes = nextPendingTask?.targetMinutes
        ?: studyNowRecommendation?.targetMinutes
        ?: 30

    val todayFocusMinutes = user?.totalFocusMinutes ?: 0
    val targetDailyMinutes = user?.dailyTargetMinutes ?: 180
    val prepPercentage = examReadiness?.readinessScore
        ?: if (totalPlanCount > 0) ((completedPlanCount.toFloat() / totalPlanCount.toFloat()) * 100).toInt().coerceIn(10, 95)
        else 56

    // Custom study schedule blocks (from user profile)
    val customBlocks = remember(user?.customStudyBlocksJson) {
        parseCustomStudyBlocks(user?.customStudyBlocksJson)
    }

    // Catalog for switching target exams
    val examCatalogOptions = listOf(
        "RRB Group D (Railway Recruitment)",
        "RRB NTPC (Railway Recruitment Board)",
        "SSC CGL (Staff Selection Commission)",
        "JEE Main (Engineering Entrance)",
        "NEET UG (Medical Entrance)",
        "UPSC Civil Services Examination",
        "IBPS PO (Banking Probationary Officer)",
        "CBSE Class 12 Board Exam",
        "State PSC General Studies"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // 1. TOP HEADER (Dynamic Greeting, Search, Notifications, Profile)
        // =========================================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Greeting & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$greetingTime, $studentDisplayName 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Let's make today count.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Action Buttons (Search, Notifications, Profile, Theme)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Search Button
                    IconButton(
                        onClick = { showQuickSearchDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1CFFFFFF) else Color(0x10000000))
                            .border(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x2064748B), CircleShape)
                            .testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search study topics",
                            tint = if (isDark) NeonCyan else DeepIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Notification Button with badge
                    Box {
                        IconButton(
                            onClick = { showNotificationSummaryDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x1CFFFFFF) else Color(0x10000000))
                                .border(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x2064748B), CircleShape)
                                .testTag("home_notification_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications & Reminders",
                                tint = if (isDark) Color.White else Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        // Unread pulse indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(GoldenSpark)
                        )
                    }

                    // Profile / Avatar Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.3f), ElectricViolet.copy(alpha = 0.3f)))
                            )
                            .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape)
                            .springClickable(testTag = "home_profile_button", onClick = onOpenProfileSettings),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user?.photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = studentDisplayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) NeonCyan else DeepIndigo
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. CURRENT EXAM CARD (Dynamic Context, Days Remaining, Exam Switcher)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("current_exam_card"),
                shape = RoundedCornerShape(20.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 2.dp else 8.dp,
                onClick = { showExamSwitcherDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Exam Info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "CURRENT EXAM",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = selectedExamName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Target Date: $formattedTargetDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right Days Remaining Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0x2838BDF8) else Color(0x186366F1),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 0.5.dp,
                            color = if (isDark) NeonCyan.copy(alpha = 0.5f) else DeepIndigo.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$daysRemaining",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) NeonCyan else DeepIndigo
                            )
                            Text(
                                text = "days left",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. TODAY'S MISSION — THE MOST IMPORTANT CARD
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("todays_mission_card"),
                shape = RoundedCornerShape(22.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 3.dp else 10.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header row: Mission badge & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TODAY'S MISSION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                letterSpacing = 0.8.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0x2010B981) else Color(0x1510B981),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, EmeraldSuccess.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (nextPendingTask != null) "ACTIVE PLAN" else "SMART NEXT ACTION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mission Subject & Topic
                    Text(
                        text = "$missionSubject • $missionTopic",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$missionTargetMinutes min focused session",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subtle Progress Bar
                    val completedFraction = if (targetDailyMinutes > 0) {
                        (todayFocusMinutes.toFloat() / targetDailyMinutes.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$todayFocusMinutes min completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                text = "$targetDailyMinutes min goal",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isDark) Color(0x20FFFFFF) else Color(0x15000000))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(completedFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(NeonCyan, ElectricViolet, NebulaPurple)
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons: START NOW / CONTINUE & VIEW PLAN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Primary Action
                        Button(
                            onClick = { onStartFocusSession(missionSubject, missionTopic) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("start_mission_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) ElectricIndigo else DeepIndigo
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (todayFocusMinutes > 0) "CONTINUE" else "START NOW",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Secondary Action
                        OutlinedButton(
                            onClick = { onNavigateToTab(AppNavTab.STUDY) },
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("view_plan_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0x40FFFFFF) else Color(0x406366F1)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "VIEW PLAN",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else DeepIndigo
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 4. TODAY'S PROGRESS (Study Time, Tasks Completed, Streak, Preparation %)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("todays_progress_card"),
                shape = RoundedCornerShape(20.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 2.dp else 6.dp,
                onClick = onOpenExamReadinessCenter
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TODAY'S PROGRESS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenExamReadinessCenter() }
                        ) {
                            Text(
                                text = "Analytics",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Study Time Metric
                        val hours = todayFocusMinutes / 60
                        val mins = todayFocusMinutes % 60
                        val formattedTime = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                        ProgressMetricItem(
                            label = "Study Time",
                            value = formattedTime,
                            icon = Icons.Outlined.Timer,
                            color = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )

                        // Tasks Completed Metric
                        ProgressMetricItem(
                            label = "Tasks Done",
                            value = "$completedPlanCount / $totalPlanCount",
                            icon = Icons.Outlined.CheckCircle,
                            color = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )

                        // Current Streak Metric
                        ProgressMetricItem(
                            label = "Streak",
                            value = "${user?.streakDays ?: 1} Days",
                            icon = Icons.Filled.LocalFireDepartment,
                            color = GoldenSpark,
                            modifier = Modifier.weight(1f)
                        )

                        // Preparation % Metric
                        ProgressMetricItem(
                            label = "Prep Score",
                            value = "$prepPercentage%",
                            icon = Icons.Outlined.TrendingUp,
                            color = NebulaPurple,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 5. NOVA AI CARD (Contextual AI Study Guidance)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nova_ai_card"),
                shape = RoundedCornerShape(20.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 2.dp else 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Nova Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Nova AI",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✨ Nova AI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }

                        TextButton(
                            onClick = { onNavigateToTab(AppNavTab.AI_TUTOR) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ask Nova",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Contextual Nova AI Message
                    val novaContextualMessage = remember(user, nextPendingTask, completedPlanCount, todayFocusMinutes) {
                        generateContextualNovaMessage(
                            user = user,
                            nextTask = nextPendingTask,
                            pendingTasksCount = totalPlanCount - completedPlanCount,
                            completedTasksCount = completedPlanCount,
                            todayFocusMins = todayFocusMinutes
                        )
                    }

                    Text(
                        text = novaContextualMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Nova Buttons: Chat With Nova & Get Guidance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToTab(AppNavTab.AI_TUTOR) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("chat_nova_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0x2838BDF8) else Color(0x186366F1),
                                contentColor = if (isDark) NeonCyan else DeepIndigo
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isDark) NeonCyan.copy(alpha = 0.5f) else DeepIndigo.copy(alpha = 0.4f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "CHAT WITH NOVA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onLoadAiCoach(missionSubject)
                                onNavigateToTab(AppNavTab.AI_TUTOR)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("get_guidance_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isDark) Color(0x30FFFFFF) else Color(0x306366F1)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "GET GUIDANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 6. TODAY'S SCHEDULE (Compact Timetable)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("todays_schedule_card"),
                shape = RoundedCornerShape(20.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 2.dp else 6.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TODAY'S SCHEDULE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            letterSpacing = 0.8.sp
                        )

                        TextButton(
                            onClick = { onNavigateToTab(AppNavTab.STUDY) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Full Timetable",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Schedule Items (Next 2-3 sessions)
                    if (customBlocks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customBlocks.take(3).forEach { block ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AccessTime,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = block.startTime,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "• ${block.subject} (${block.topic.ifBlank { "Concept Practice" }})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = "${block.durationMinutes}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else if (studyPlan.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            studyPlan.take(3).forEach { planItem ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onTogglePlanItem(planItem.id, !planItem.isCompleted) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (planItem.isCompleted) {
                                        if (isDark) Color(0x1810B981) else Color(0x1010B981)
                                    } else {
                                        if (isDark) Color(0x14FFFFFF) else Color(0x08000000)
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (planItem.isCompleted) EmeraldSuccess.copy(alpha = 0.4f)
                                        else if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (planItem.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                                contentDescription = null,
                                                tint = if (planItem.isCompleted) EmeraldSuccess else Color(0xFF94A3B8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "${planItem.subject} — ${planItem.topic}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (planItem.isCompleted) Color(0xFF94A3B8) else (if (isDark) Color.White else Color(0xFF0F172A)),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = "${planItem.targetMinutes}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (planItem.isCompleted) EmeraldSuccess else NeonCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty State
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Nothing scheduled for today.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )

                            Button(
                                onClick = { onNavigateToTab(AppNavTab.STUDY) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) ElectricIndigo else DeepIndigo
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Plan Schedule",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 7. QUICK ACTIONS (Sleek 4-Action Hub: Study, Mock Test, Revision, Focus)
        // =========================================================================
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Action 1: Study
                    QuickActionCard(
                        title = "Study",
                        subtitle = "Planner",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        accentColor = ElectricIndigo,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_study",
                        onClick = { onNavigateToTab(AppNavTab.STUDY) }
                    )

                    // Action 2: Mock Test
                    QuickActionCard(
                        title = "Mock Test",
                        subtitle = "Exam Mode",
                        icon = Icons.Filled.Quiz,
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_mock",
                        onClick = { onNavigateToTab(AppNavTab.PROGRESS) }
                    )

                    // Action 3: Revision
                    QuickActionCard(
                        title = "Revision",
                        subtitle = "Spaced Review",
                        icon = Icons.Filled.Repeat,
                        accentColor = GoldenSpark,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_revision",
                        onClick = { onNavigateToTab(AppNavTab.STUDY) }
                    )

                    // Action 4: Focus
                    QuickActionCard(
                        title = "Focus",
                        subtitle = "App Shield",
                        icon = Icons.Filled.Shield,
                        accentColor = CoralRose,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_focus",
                        onClick = { onNavigateToTab(AppNavTab.FOCUS) }
                    )
                }
            }
        }
    }

    // =========================================================================
    // EXAM SWITCHER MODAL DIALOG
    // =========================================================================
    if (showExamSwitcherDialog) {
        AlertDialog(
            onDismissRequest = { showExamSwitcherDialog = false },
            containerColor = if (isDark) Color(0xFF111827) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.School, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Change Target Exam",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Select your target exam. Study topics, mock tests, and Nova context will automatically align.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    examCatalogOptions.forEach { examOption ->
                        val isSelected = selectedExamName.contains(examOption.take(10), ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .springClickable {
                                    if (user != null) {
                                        val updated = user.copy(
                                            examName = examOption,
                                            examCategory = examOption,
                                            goal = examOption
                                        )
                                        onUpdateUserProfile(updated)
                                    }
                                    showExamSwitcherDialog = false
                                },
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) NeonCyan else Color(0x20FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = examOption,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                if (isSelected) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExamSwitcherDialog = false }) {
                    Text("Close", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // =========================================================================
    // QUICK SEARCH MODAL DIALOG
    // =========================================================================
    if (showQuickSearchDialog) {
        var searchQuery by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQuickSearchDialog = false },
            containerColor = if (isDark) Color(0xFF111827) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart Study Search",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Search questions, chapters, formulas, or past year topics for $selectedExamName.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_search_input"),
                        placeholder = { Text("e.g. Thermodynamics formulas, Railway GK...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )

                    // Popular topic chips
                    Text(
                        text = "Quick Topics:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Formulas", "Mock Test", "Weak Areas").forEach { chip ->
                            SuggestionChip(
                                onClick = {
                                    searchQuery = chip
                                    onPerformSmartSearch(chip)
                                    showQuickSearchDialog = false
                                },
                                label = { Text(chip, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            onPerformSmartSearch(searchQuery)
                            showQuickSearchDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("Search Nova", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickSearchDialog = false }) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                }
            }
        )
    }

    // =========================================================================
    // NOTIFICATION & REMINDER MODAL DIALOG
    // =========================================================================
    if (showNotificationSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationSummaryDialog = false },
            containerColor = if (isDark) Color(0xFF111827) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Notifications, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Study Reminders & Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.School, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    text = "$selectedExamName Countdown",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "$daysRemaining days remaining until target date ($formattedTargetDate).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Timer, null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    text = "Daily Goal Reminder",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "$todayFocusMinutes / $targetDailyMinutes minutes logged today.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationSummaryDialog = false }) {
                    Text("Close", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// =========================================================================
// HELPER COMPOSABLES & FUNCTIONS
// =========================================================================

@Composable
private fun ProgressMetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isDark) 0.18f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color.White else Color(0xFF0F172A),
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()

    Surface(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(16.dp))
            .springClickable(testTag = testTag, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            accentColor.copy(alpha = if (isDark) 0.4f else 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * Strips raw internal user IDs (e.g. "rahul810232" or "john_doe@mail") to display a clean friendly name.
 */
private fun cleanDisplayName(rawName: String?): String {
    if (rawName.isNullOrBlank()) return "Scholar"
    val beforeAt = rawName.substringBefore("@").trim()
    val spaced = beforeAt.replace("_", " ").replace(".", " ")
    val noDigits = spaced.replace(Regex("\\d+$"), "").trim()
    if (noDigits.isBlank()) return "Scholar"
    return noDigits.split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
}

/**
 * Intelligent contextual Nova AI message generator based on student state and language preference.
 */
private fun generateContextualNovaMessage(
    user: UserProfile?,
    nextTask: StudyPlanItem?,
    pendingTasksCount: Int,
    completedTasksCount: Int,
    todayFocusMins: Int
): String {
    val lang = user?.languagePreference ?: "English"
    val isHindi = lang.equals("Hindi", ignoreCase = true) || lang.equals("Hinglish", ignoreCase = true)
    val exam = user?.examName?.ifBlank { "your target exam" } ?: "your target exam"
    val student = cleanDisplayName(user?.name)

    if (nextTask != null) {
        return if (isHindi) {
            "Namaste $student! Aaj $exam ke liye '${nextTask.subject}: ${nextTask.topic}' ka ${nextTask.targetMinutes}-minute focused session plan kiya hai. Shuru karein?"
        } else {
            "Hey $student! You have a ${nextTask.targetMinutes}-minute session for ${nextTask.subject} (${nextTask.topic}) scheduled next for $exam. Let's make it count!"
        }
    }

    if (completedTasksCount > 0 && pendingTasksCount == 0) {
        return if (isHindi) {
            "Shabash $student! Aaj ke saare scheduled study tasks complete ho chuke hain. Chahe toh ek quick 10-minute quiz revise kar lo!"
        } else {
            "Great job, $student! You've completed all scheduled tasks for today. Feel free to do a quick mock test or ask me any doubts!"
        }
    }

    if (todayFocusMins > 0) {
        val h = todayFocusMins / 60
        val m = todayFocusMins % 60
        val timeStr = if (h > 0) "${h}h ${m}m" else "${m}m"
        return if (isHindi) {
            "Aaj aapne $timeStr focused study complete ki hai $exam ke liye. Consistency hi aapko top score dilayegi!"
        } else {
            "You've logged $timeStr of focused study today for $exam. Fantastic momentum — keep pushing forward!"
        }
    }

    return if (isHindi) {
        "Namaste $student! $exam ki taiyari ke liye aaj ka mission ready hai. Koi bhi doubt ya question ho, mujhse poochho."
    } else {
        "Welcome back, $student! Your $exam preparation dashboard is calibrated. Ready to start your focused study session?"
    }
}

/**
 * Parses custom study time blocks from JSON.
 */
private fun parseCustomStudyBlocks(json: String?): List<StudyTimeBlock> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<StudyTimeBlock>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                StudyTimeBlock(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    startTime = obj.optString("startTime", "06:00 PM"),
                    subject = obj.optString("subject", "General"),
                    durationMinutes = obj.optInt("durationMinutes", 45),
                    topic = obj.optString("topic", "Core Practice")
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

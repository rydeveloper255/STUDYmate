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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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

    // 1. Time-based Greeting
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingTime = when (currentHour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    val studentName = user?.name?.ifBlank { "Scholar" } ?: "Scholar"

    // 2. Exam Context & Countdown (Strictly from UserProfile / Exam Context)
    val selectedExamName = user?.examName?.ifBlank { user.examCategory } ?: ""
    val hasExamSelected = selectedExamName.isNotBlank() && selectedExamName != "Not Selected"

    val examDaysRemaining = remember(user?.examDateMillis) {
        val targetMillis = user?.examDateMillis ?: 0L
        if (targetMillis > System.currentTimeMillis()) {
            val diff = targetMillis - System.currentTimeMillis()
            (diff / (1000L * 60 * 60 * 24)).coerceAtLeast(1).toInt()
        } else {
            0
        }
    }

    val formattedExamDate = remember(user?.examDateMillis) {
        val targetMillis = user?.examDateMillis ?: 0L
        if (targetMillis > 0) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(targetMillis))
        } else {
            ""
        }
    }

    // 3. Real Study Progress Metrics
    val totalFocusMins = user?.totalFocusMinutes ?: 0
    val targetMins = if ((user?.dailyTargetMinutes ?: 0) > 0) user!!.dailyTargetMinutes else ((user?.availableStudyHours ?: 3f) * 60).toInt().coerceAtLeast(60)
    val progressFraction = (totalFocusMins.toFloat() / targetMins.toFloat()).coerceIn(0f, 1f)

    val completedTasksCount = studyPlan.count { it.isCompleted }
    val totalTasksCount = studyPlan.size
    val pendingTasks = studyPlan.filter { !it.isCompleted }
    val nextBestTask = pendingTasks.firstOrNull()

    // 4. Revision Due Count (Real data from flashcards)
    val now = remember { System.currentTimeMillis() }
    val dueRevisionCount = flashcards.count { it.nextReviewDate <= now || it.status == RevisionCategory.REVISE_NOW }

    // 5. Scheduled Study Blocks
    val scheduledBlocks = remember(user?.customStudyBlocksJson) {
        parseCustomStudyBlocks(user?.customStudyBlocksJson)
    }

    // 6. Exam Switcher Dialog State
    var showExamSwitcherDialog by remember { mutableStateOf(false) }

    val examCatalogOptions = listOf(
        "RRB NTPC (Railway Non-Technical)",
        "RRB Group D (Railway Recruitment)",
        "SSC CGL (Staff Selection Commission)",
        "JEE Main & Advanced (Engineering)",
        "NEET UG (Medical Entrance)",
        "UPSC Civil Services (IAS / IPS)",
        "IBPS / SBI Banking PO & Clerk",
        "CBSE Class 12 Board Exam"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundGradient(isDark))
            .padding(horizontal = 18.dp)
            .testTag("home_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // 1. CLEAN HEADER (Greeting, Exam Pill, Streak & Profile Avatar)
        // =========================================================================
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greetingTime, $studentName 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "StudyMate AI • Personal Learning Dashboard",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StreakBadge(streakDays = (user?.streakDays ?: 1).coerceAtLeast(1))

                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x3338BDF8))
                                .border(1.5.dp, NeonCyan, CircleShape)
                                .springClickable(testTag = "profile_avatar_button", onClick = onOpenProfileSettings),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!user?.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = user?.photoUrl,
                                    contentDescription = "Profile Avatar",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Profile",
                                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exam Context Chip & Countdown Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .springClickable(testTag = "exam_switcher_button") {
                            showExamSwitcherDialog = true
                        },
                    color = if (isDark) Color(0x2238BDF8) else Color(0x150284C7),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isDark) NeonCyan.copy(alpha = 0.4f) else Color(0x400284C7)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (hasExamSelected) selectedExamName else "Set your exam",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Change Exam",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (hasExamSelected && formattedExamDate.isNotBlank()) {
                                    Text(
                                        text = "Target Date: $formattedExamDate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        if (hasExamSelected && examDaysRemaining > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoldenSpark.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldenSpark.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "⏳ $examDaysRemaining days left",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldenSpark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else if (!hasExamSelected) {
                            Button(
                                onClick = { showExamSwitcherDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Select", color = Color(0xFF070B19), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 1B. OVERALL EXAM READINESS INDEX CARD
        // =========================================================================
        item {
            val score = examReadiness?.readinessScore ?: 0
            val badgeText = examReadiness?.statusBadgeText ?: "Insufficient Data 🌱"
            val explanation = examReadiness?.explanation ?: "Complete study sessions or mock tests to unlock your readiness index."

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .springClickable(testTag = "home_exam_readiness_card", onClick = onOpenExamReadinessCenter),
                elevation = 6.dp,
                fillAlpha = 0.75f
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EXAM READINESS SCORE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonCyan.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Preparation: $score%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = onOpenExamReadinessCenter,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Details →", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. TODAY'S PROGRESS (Prominent Glass Card with Real Data Only)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
                fillAlpha = 0.75f,
                testTag = "todays_progress_card"
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

                        Spacer(modifier = Modifier.height(8.dp))

                        val hours = totalFocusMins / 60
                        val mins = totalFocusMins % 60
                        val targetHours = targetMins / 60
                        val targetRemainingMins = targetMins % 60

                        if (totalFocusMins > 0 || totalTasksCount > 0) {
                            Text(
                                text = "${hours}h ${mins}m / ${targetHours}h ${if (targetRemainingMins > 0) "${targetRemainingMins}m" else ""}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (totalTasksCount > 0) {
                                    "$completedTasksCount / $totalTasksCount tasks completed"
                                } else {
                                    if (progressFraction >= 1f) "🎯 Daily study target reached!"
                                    else "${((1f - progressFraction) * targetMins).toInt()} mins left to hit daily goal"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        } else {
                            // Friendly Empty State for New/Idle User
                            Text(
                                text = "Start your first study session today",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Daily Target: ${targetHours}h ${if (targetRemainingMins > 0) "${targetRemainingMins}m" else ""} • 0m logged",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        XpBadge(xp = user?.xp ?: 0, level = user?.level ?: 1)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    LiquidProgressRing(
                        progress = progressFraction,
                        currentText = "${(progressFraction * 100).toInt()}%",
                        targetText = "Goal",
                        size = 94.dp,
                        strokeWidth = 9.dp
                    )
                }
            }
        }

        // =========================================================================
        // 3. PRIMARY ACTION (Single, Unambiguous CTA)
        // =========================================================================
        item {
            val primaryActionText = when {
                nextBestTask != null -> "▶ Continue Study • ${nextBestTask.subject}"
                totalTasksCount > 0 && completedTasksCount < totalTasksCount -> "Start Today's Plan"
                totalTasksCount > 0 && completedTasksCount == totalTasksCount -> "✨ Review & Revise Completed Tasks"
                else -> "Create Study Plan 📚"
            }

            GlassButton(
                text = primaryActionText,
                onClick = {
                    if (nextBestTask != null) {
                        onStartFocusSession(nextBestTask.subject, nextBestTask.topic)
                    } else {
                        onNavigateToTab(AppNavTab.STUDY)
                    }
                },
                icon = if (nextBestTask != null) Icons.Filled.PlayArrow else Icons.Filled.Addchart,
                isPrimary = true,
                testTag = "primary_continue_study_button"
            )
        }

        // =========================================================================
        // 4. NEXT BEST TASK (Compact, Real-Data-Only Card)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 6.dp,
                fillAlpha = 0.7f,
                testTag = "next_best_task_card"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = GoldenSpark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NEXT BEST TASK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark
                            )
                        }

                        if (nextBestTask != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CoralRose.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, CoralRose.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = nextBestTask.priority.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRose,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (nextBestTask != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nextBestTask.subject,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = nextBestTask.topic,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${nextBestTask.targetMinutes} min focused session",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = { onStartFocusSession(nextBestTask.subject, nextBestTask.topic) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("start_next_task_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF070B19),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Empty state for Next Best Task
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (totalTasksCount > 0) "All tasks completed for today! 🎉" else "No study tasks scheduled yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (totalTasksCount > 0) "Great progress! Take a break or do a flashcard revision." else "Set up your study plan to get personalized daily topics.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }

                            OutlinedButton(
                                onClick = { onNavigateToTab(AppNavTab.STUDY) },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                            ) {
                                Text("Planner", color = NeonCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 5. NOVA CARD (Compact AI Study Assistant Recommendation)
        // =========================================================================
        item {
            val novaMessage = remember(user, nextBestTask, completedTasksCount, totalTasksCount, totalFocusMins) {
                generateContextualNovaMessage(
                    user = user,
                    nextTask = nextBestTask,
                    pendingTasksCount = pendingTasks.size,
                    completedTasksCount = completedTasksCount,
                    todayFocusMins = totalFocusMins
                )
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 6.dp,
                fillAlpha = 0.75f,
                testTag = "home_nova_card"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(NeonCyan, ElectricIndigo))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nova AI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        TextButton(
                            onClick = { onNavigateToTab(AppNavTab.AI_TUTOR) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Ask Nova →", color = NeonCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "\"$novaMessage\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToTab(AppNavTab.AI_TUTOR) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Filled.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chat with Nova", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        if (nextBestTask != null) {
                            OutlinedButton(
                                onClick = { onStartFocusSession(nextBestTask.subject, nextBestTask.topic) },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Start Session", color = NeonCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 6. QUICK PROGRESS SUMMARY (Minimal 3-Column Scannable Metric Row)
        // =========================================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Study Time
                val studyHours = totalFocusMins / 60
                val studyMins = totalFocusMins % 60
                QuickProgressPill(
                    title = "Study",
                    value = "${studyHours}h ${studyMins}m",
                    subtitle = "Logged today",
                    icon = Icons.Filled.Timer,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                // Metric 2: Tasks / Completion Rate
                val taskRate = if (totalTasksCount > 0) "${(completedTasksCount * 100) / totalTasksCount}%" else "0%"
                QuickProgressPill(
                    title = "Tasks",
                    value = if (totalTasksCount > 0) "$completedTasksCount/$totalTasksCount" else "0",
                    subtitle = if (totalTasksCount > 0) "$taskRate done" else "No tasks",
                    icon = Icons.Filled.CheckCircleOutline,
                    color = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )

                // Metric 3: Revision Due
                QuickProgressPill(
                    title = "Revision",
                    value = "$dueRevisionCount due",
                    subtitle = if (dueRevisionCount > 0) "Cards ready" else "All caught up",
                    icon = Icons.Filled.Psychology,
                    color = if (dueRevisionCount > 0) CoralRose else GoldenSpark,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(AppNavTab.STUDY) }
                )
            }
        }

        // =========================================================================
        // 7. UPCOMING / TODAY'S SCHEDULE (Compact Schedule View)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                fillAlpha = 0.65f,
                testTag = "home_schedule_card"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TODAY'S SCHEDULE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        TextButton(
                            onClick = { onNavigateToTab(AppNavTab.STUDY) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Full Timetable", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (scheduledBlocks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            scheduledBlocks.take(3).forEach { block ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0x18FFFFFF) else Color(0x10000000))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = block.startTime,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldenSpark
                                            )
                                            Text(
                                                text = "•",
                                                color = Color(0xFF64748B)
                                            )
                                            Text(
                                                text = block.subject,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color.White else Color(0xFF0F172A)
                                            )
                                        }

                                        Text(
                                            text = "${block.durationMinutes} min",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan
                                        )
                                    }
                                }
                            }
                        }
                    } else if (studyPlan.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            studyPlan.take(3).forEach { planItem ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (planItem.isCompleted) Color(0x1010B981) else if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, if (planItem.isCompleted) EmeraldSuccess.copy(alpha = 0.4f) else Color(0x18FFFFFF))
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
                                                color = if (planItem.isCompleted) Color(0xFF94A3B8) else if (isDark) Color.White else Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${planItem.targetMinutes}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty State
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Nothing scheduled yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )

                            Button(
                                onClick = { onNavigateToTab(AppNavTab.STUDY) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Plan Study", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // QUICK ACCESS SHORTCUTS HUB
        // =========================================================================
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "⚡ Quick Study Hub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickProgressPill(
                        title = "Readiness",
                        value = "${examReadiness?.readinessScore ?: 0}%",
                        subtitle = "Full Center",
                        icon = Icons.Filled.CenterFocusStrong,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenExamReadinessCenter
                    )

                    QuickProgressPill(
                        title = "Planner",
                        value = "${studyPlan.count { !it.isCompleted }} Tasks",
                        subtitle = "Adaptive Schedule",
                        icon = Icons.Filled.CalendarMonth,
                        color = ElectricIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(AppNavTab.STUDY) }
                    )

                    QuickProgressPill(
                        title = "Mock Tests",
                        value = "Speed & Mocks",
                        subtitle = "Exam Mode",
                        icon = Icons.Filled.Quiz,
                        color = EmeraldSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(AppNavTab.PROGRESS) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickProgressPill(
                        title = "Spaced Revision",
                        value = "${examReadiness?.revisionDueCount ?: 0} Due",
                        subtitle = "Smart Review",
                        icon = Icons.Filled.Repeat,
                        color = GoldenSpark,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(AppNavTab.STUDY) }
                    )

                    QuickProgressPill(
                        title = "Focus Shield",
                        value = "App Blocker",
                        subtitle = "Distraction Free",
                        icon = Icons.Filled.Shield,
                        color = CoralRose,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(AppNavTab.FOCUS) }
                    )

                    QuickProgressPill(
                        title = "Nova AI",
                        value = "Instant Help",
                        subtitle = "24/7 AI Tutor",
                        icon = Icons.Filled.AutoAwesome,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(AppNavTab.AI_TUTOR) }
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
            containerColor = if (isDark) Color(0xFF131C2E) else Color.White,
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
                        text = "Select your current target exam. Your study topics, mock tests, and AI context will switch accordingly.",
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
}

// =========================================================================
// HELPER COMPOSABLE: QUICK PROGRESS PILL
// =========================================================================
@Composable
private fun QuickProgressPill(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    var surfaceModifier = modifier.clip(RoundedCornerShape(14.dp))
    if (onClick != null) {
        surfaceModifier = surfaceModifier.springClickable(onClick = onClick)
    }

    Surface(
        modifier = surfaceModifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0x18FFFFFF) else Color(0x0A000000),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =========================================================================
// HELPER: CONTEXTUAL NOVA MESSAGE GENERATOR
// =========================================================================
private fun generateContextualNovaMessage(
    user: UserProfile?,
    nextTask: StudyPlanItem?,
    pendingTasksCount: Int,
    completedTasksCount: Int,
    todayFocusMins: Int
): String {
    val lang = user?.languagePreference ?: "English"
    val isHindi = lang.equals("Hindi", ignoreCase = true) || lang.equals("Hinglish", ignoreCase = true)
    val exam = user?.examName?.ifBlank { "Exam" } ?: "Exam"
    val student = user?.name?.ifBlank { "Scholar" } ?: "Scholar"

    if (nextTask != null) {
        return if (isHindi) {
            "Boss, aaj ${nextTask.subject} me '${nextTask.topic}' ka ${nextTask.targetMinutes}-minute session pending hai. Chalo ise complete kar lete hain!"
        } else {
            "Hey $student! You have a ${nextTask.targetMinutes}-minute session for ${nextTask.subject} (${nextTask.topic}) pending. Let's conquer it!"
        }
    }

    if (completedTasksCount > 0 && pendingTasksCount == 0) {
        return if (isHindi) {
            "Shabash! Aaj ke saare planned tasks complete ho chuke hain. Chahe toh ek quick 10-minute quiz revise kar lo!"
        } else {
            "Great job! You've completed all scheduled tasks for today. Feel free to do a quick revision or ask me anything!"
        }
    }

    if (todayFocusMins > 0) {
        val h = todayFocusMins / 60
        val m = todayFocusMins % 60
        return if (isHindi) {
            "Aaj aapne ${if (h > 0) "${h}h " else ""}${m}m study complete ki hai $exam ke liye. Consistency hi success ki key hai!"
        } else {
            "You've logged ${if (h > 0) "${h}h " else ""}${m}m of focused study today for $exam. Keep this great momentum going!"
        }
    }

    return if (isHindi) {
        "Namaste! $exam ki taiyari ke liye aaj ka study session shuru karte hain. Jo bhi doubt ho, bina jhijhak puchho."
    } else {
        "Welcome back! Ready to accelerate your $exam preparation today? Ask me any doubts or start your first session."
    }
}

// =========================================================================
// HELPER: PARSE STUDY TIME BLOCKS JSON
// =========================================================================
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

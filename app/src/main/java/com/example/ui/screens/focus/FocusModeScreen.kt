package com.example.ui.screens.focus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.ExamContext
import com.example.data.model.FocusSession
import com.example.data.model.StudyPlanItem
import com.example.data.model.UserProfile
import com.example.service.FocusShieldManager
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ThemeToggleButton
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
    modifier: Modifier = Modifier,
    activeStudySession: StudyPlanItem? = null,
    dailyStudyPlan: List<StudyPlanItem> = emptyList(),
    allFocusSessions: List<FocusSession> = emptyList(),
    userProfile: UserProfile? = null,
    activeExamContext: ExamContext? = null,
    onStartNextSession: ((StudyPlanItem) -> Unit)? = null,
    onAskNova: ((subject: String, chapter: String, topic: String, prompt: String) -> Unit)? = null,
    onSaveNote: ((subject: String, topic: String, noteText: String) -> Unit)? = null,
    onQuickRevision: ((subject: String, topic: String) -> Unit)? = null,
    onStartBreak: ((Int) -> Unit)? = null,
    onEndBreak: (() -> Unit)? = null,
    novaDoubtResponse: String = "",
    isNovaDoubtThinking: Boolean = false,
    onDismissNovaDoubt: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val isHindi = userProfile?.languagePreference?.equals("Hindi", ignoreCase = true) == true

    // Context resolution
    val currentExam = remember(focusState.examName, activeExamContext?.examName, userProfile?.examName) {
        when {
            focusState.examName.isNotBlank() -> focusState.examName
            activeExamContext != null && activeExamContext.examName.isNotBlank() -> activeExamContext.examName
            !userProfile?.examName.isNullOrBlank() -> userProfile?.examName ?: "RRB Group D"
            else -> "RRB Group D"
        }
    }

    val currentSubject = remember(focusState.subject, activeStudySession?.subject) {
        focusState.subject.ifBlank { activeStudySession?.subject ?: "General Science" }
    }

    val currentTopic = remember(focusState.topic, activeStudySession?.topic) {
        focusState.topic.ifBlank { activeStudySession?.topic ?: "Sound" }
    }

    val currentGoal = remember(focusState.sessionGoal, currentSubject, currentTopic) {
        if (focusState.sessionGoal.isNotBlank()) {
            focusState.sessionGoal
        } else {
            if (isHindi) "$currentSubject में $currentTopic पूरा करें" else "Complete $currentTopic — Basic Concepts"
        }
    }

    // Local states for idle configuration
    var selectedMinutes by remember { mutableIntStateOf(focusState.initialMinutes) }
    var selectedSubject by remember { mutableStateOf(currentSubject) }
    var selectedTopic by remember { mutableStateOf(currentTopic) }
    var customMinutesInput by remember { mutableStateOf("") }
    var isCustomDurationSelected by remember { mutableStateOf(false) }

    // Dialog states
    var showEndConfirmDialog by remember { mutableStateOf(false) }
    var showAskNovaDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAppShieldModal by remember { mutableStateOf(false) }
    var noteInputText by remember { mutableStateOf("") }
    var customNovaPrompt by remember { mutableStateOf("") }

    // Accessibility check
    var isAccessibilityGranted by remember { mutableStateOf(FocusShieldManager.isAccessibilityServiceEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = FocusShieldManager.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showAppShieldModal) {
        BackHandler(enabled = true) { showAppShieldModal = false }
    }

    val blockedAppsCount = remember(showAppShieldModal, focusState.isRunning) {
        FocusShieldManager.getRestrictedPackages().size
    }

    // Presets
    val presetDurations = listOf(15, 25, 45, 60)
    val defaultSubjects = remember(activeExamContext?.subjects, userProfile?.subjects) {
        when {
            !activeExamContext?.subjects.isNullOrEmpty() -> activeExamContext!!.subjects.map { it.name }
            !userProfile?.subjects.isNullOrEmpty() -> userProfile!!.subjects
            else -> listOf("General Science", "Mathematics", "General Intelligence & Reasoning", "General Awareness")
        }
    }

    // Time calculations
    val remainingMinutes = if (focusState.isBreakActive) focusState.breakRemainingSeconds / 60 else focusState.remainingSeconds / 60
    val remainingSecs = if (focusState.isBreakActive) focusState.breakRemainingSeconds % 60 else focusState.remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", remainingMinutes, remainingSecs)

    val totalSessionSecs = (focusState.initialMinutes * 60).toFloat().coerceAtLeast(1f)
    val sessionProgress = if (focusState.isRunning) {
        ((totalSessionSecs - focusState.remainingSeconds.toFloat()) / totalSessionSecs).coerceIn(0f, 1f)
    } else 0f

    // Today's Progress Calculation
    val targetDailyMinutes = remember(userProfile?.dailyTargetMinutes) {
        (userProfile?.dailyTargetMinutes ?: 180).coerceAtLeast(60)
    }

    val completedTodayMinutes = remember(dailyStudyPlan, allFocusSessions, focusState.actualMinutesSpent) {
        val fromPlan = dailyStudyPlan.filter { it.isCompleted }.sumOf { it.actualMinutesSpent.coerceAtLeast(it.targetMinutes) }
        val fromSessions = allFocusSessions.sumOf { it.actualMinutesSpent }
        maxOf(fromPlan, fromSessions) + (if (focusState.isRunning) focusState.actualMinutesSpent else 0)
    }

    val todayProgressFraction = (completedTodayMinutes.toFloat() / targetDailyMinutes.toFloat()).coerceIn(0f, 1f)
    val todayCompletedHours = completedTodayMinutes / 60
    val todayCompletedRemainingMins = completedTodayMinutes % 60
    val targetHours = targetDailyMinutes / 60
    val targetRemainingMins = targetDailyMinutes % 60

    val todayFormatted = remember(completedTodayMinutes, targetDailyMinutes) {
        val compStr = if (todayCompletedRemainingMins > 0) "${todayCompletedHours}h ${todayCompletedRemainingMins}m" else "${todayCompletedHours}h"
        val tgtStr = if (targetRemainingMins > 0) "${targetHours}h ${targetRemainingMins}m" else "${targetHours}h"
        "$compStr / $tgtStr"
    }

    // Next Session discovery
    val nextPendingItem = remember(dailyStudyPlan, focusState.planItemId, focusState.isRunning) {
        dailyStudyPlan.firstOrNull { item ->
            !item.isCompleted && (focusState.planItemId == null || item.id != focusState.planItemId)
        }
    }

    // Gentle pulse animation for active timer
    val infiniteTransition = rememberInfiniteTransition(label = "timer_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isDark) Brush.verticalGradient(listOf(Color(0xFF070B19), Color(0xFF0D1527), Color(0xFF050811)))
                else Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE2E8F0)))
            )
            .testTag("focus_mode_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -------------------------------------------------------------
            // 1. TOP BAR & SESSION CONTEXT
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isHindi) "🎯 वर्तमान सत्र" else "🎯 Current Session",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSpark,
                            letterSpacing = 1.2.sp
                        )
                        if (focusState.isRunning) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (focusState.isPaused) Color(0x25F59E0B) else Color(0x2510B981)
                            ) {
                                Text(
                                    text = if (focusState.isPaused) (if (isHindi) "विरामावस्था" else "PAUSED") else (if (isHindi) "सक्रिय" else "LIVE"),
                                    color = if (focusState.isPaused) AmberAlert else EmeraldSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentExam,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$currentSubject • $currentTopic",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Shield status pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0x2038BDF8) else Color(0x150284C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x4038BDF8) else Color(0x300284C7)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAppShieldModal = true }
                            .testTag("app_shield_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "Focus Shield",
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (blockedAppsCount > 0) "$blockedAppsCount" else (if (isHindi) "शील्ड" else "Shield"),
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    ThemeToggleButton(testTag = "focus_theme_toggle")
                }
            }

            // -------------------------------------------------------------
            // 2. SESSION GOAL
            // -------------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x181E293B) else Color(0x70FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x25FFFFFF) else Color(0x15000000)),
                modifier = Modifier.fillMaxWidth().testTag("session_goal_card")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "आज का लक्ष्य" else "Today's Goal",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
                        )
                        Text(
                            text = currentGoal,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. MAIN TIMER (CENTERPIECE)
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Frosted Ring Container
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) Brush.radialGradient(
                                listOf(
                                    Color(0x2538BDF8).copy(alpha = glowAlpha),
                                    Color(0x10818CF8).copy(alpha = glowAlpha * 0.7f),
                                    Color(0x151E293B),
                                    Color.Transparent
                                )
                            ) else Brush.radialGradient(
                                listOf(
                                    Color(0x2538BDF8),
                                    Color(0x15EEF2FF),
                                    Color.White
                                )
                            )
                        )
                        .border(
                            width = 6.dp,
                            brush = if (focusState.isPaused) {
                                Brush.linearGradient(listOf(AmberAlert, GoldenSpark))
                            } else if (focusState.isBreakActive) {
                                Brush.linearGradient(listOf(EmeraldSuccess, NeonCyan))
                            } else {
                                Brush.sweepGradient(
                                    listOf(NeonCyan, ElectricViolet, GoldenSpark, EmeraldSuccess, NeonCyan)
                                )
                            },
                            shape = CircleShape
                        )
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            spotColor = if (focusState.isPaused) AmberAlert else NeonCyan,
                            ambientColor = if (focusState.isPaused) AmberAlert else ElectricViolet
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Large Digital Timer Display
                        Text(
                            text = if (focusState.isRunning || focusState.isBreakActive) timeFormatted else String.format("%02d:00", selectedMinutes),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 54.sp,
                                letterSpacing = 2.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("focus_timer_text")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                focusState.isBreakActive -> Color(0x3010B981)
                                focusState.isPaused -> Color(0x30F59E0B)
                                focusState.isRunning -> Color(0x2538BDF8)
                                else -> Color(0x2094A3B8)
                            }
                        ) {
                            Text(
                                text = when {
                                    focusState.isBreakActive -> if (isHindi) "☕ विराम समय" else "☕ BREAK TIME"
                                    focusState.isPaused -> if (isHindi) "⏸ विरामावस्था" else "⏸ PAUSED"
                                    focusState.isRunning -> if (isHindi) "🎯 एकाग्र रहें" else "🎯 STAY FOCUSED"
                                    else -> if (isHindi) "तैयार सत्र" else "READY TO FOCUS"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    focusState.isBreakActive -> EmeraldSuccess
                                    focusState.isPaused -> AmberAlert
                                    focusState.isRunning -> NeonCyan
                                    else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sub info (Accurate session metadata — no fake percentages!)
                        if (focusState.isRunning) {
                            Text(
                                text = "${focusState.initialMinutes} min session • ${focusState.actualMinutesSpent} min completed",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "${selectedMinutes} min session",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. TIMER CONTROLS
            // -------------------------------------------------------------
            if (focusState.isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (focusState.isPaused) {
                        Button(
                            onClick = onTogglePause,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("resume_focus_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldSuccess,
                                contentColor = Color(0xFF070B19)
                            )
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Resume")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "सत्र जारी रखें" else "Resume",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onTogglePause,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("pause_focus_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AmberAlert
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberAlert)
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = "Pause")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "रोकें" else "Pause",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showEndConfirmDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("end_focus_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralRose,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "End Session")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "सत्र समाप्त करें" else "End Session",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            } else if (focusState.isBreakActive) {
                Button(
                    onClick = { onEndBreak?.invoke() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("end_break_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = Color(0xFF070B19)
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "विराम समाप्त करें • अध्ययन शुरू करें" else "End Break • Resume Study",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            } else {
                // Idle Setup Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("focus_setup_card"),
                    fillAlpha = if (isDark) 0.7f else 0.85f
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isHindi) "अवधि चुनें" else "Choose Duration",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        // Preset Duration Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetDurations.forEach { mins ->
                                val isSelected = !isCustomDurationSelected && selectedMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) NeonCyan
                                            else if (isDark) Color(0x20FFFFFF) else Color(0x10000000)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.White else if (isDark) Color(0x30FFFFFF) else Color(0x15000000),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .springClickable(testTag = "preset_${mins}m") {
                                            selectedMinutes = mins
                                            isCustomDurationSelected = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (mins == 25) "25m\n(Pomodoro)" else "${mins}m",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF070B19) else if (isDark) Color.White else Color(0xFF0F172A),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }

                        // Subject Selection
                        Text(
                            text = if (isHindi) "विषय और अध्याय" else "Subject & Topic",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            defaultSubjects.take(3).forEach { sub ->
                                val isSelected = selectedSubject == sub
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) ElectricViolet else if (isDark) Color(0x18FFFFFF) else Color(0x10000000),
                                    modifier = Modifier.springClickable { selectedSubject = sub }
                                ) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = selectedTopic,
                            onValueChange = { selectedTopic = it },
                            label = { Text(if (isHindi) "विषय/अध्याय का नाम" else "Topic Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("focus_topic_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                focusedBorderColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        GlassButton(
                            text = if (isHindi) "▶ $selectedMinutes मिनट का अध्ययन शुरू करें" else "▶ Start $selectedMinutes Min Focus Session",
                            onClick = {
                                onStartFocus(
                                    selectedSubject,
                                    selectedTopic.ifBlank { "Deep Study" },
                                    selectedMinutes
                                )
                            },
                            icon = Icons.Filled.PlayArrow,
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "start_focus_btn"
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. TODAY'S PROGRESS
            // -------------------------------------------------------------
            GlassCard(
                modifier = Modifier.fillMaxWidth().testTag("today_progress_card"),
                fillAlpha = if (isDark) 0.6f else 0.8f
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "आज की प्रगति" else "Today's Progress",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }

                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan
                        )
                    }

                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDark) Color(0x25FFFFFF) else Color(0x15000000))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(todayProgressFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(NeonCyan, ElectricViolet, EmeraldSuccess)
                                    )
                                )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isHindi) "$completedTodayMinutes मिनट अध्ययन पूरा" else "$completedTodayMinutes min studied today",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        val remMins = (targetDailyMinutes - completedTodayMinutes).coerceAtLeast(0)
                        Text(
                            text = if (remMins > 0) "${remMins}m remaining" else (if (isHindi) "दैनिक लक्ष्य प्राप्त! 🎯" else "Daily Goal Met! 🎯"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (remMins > 0) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else EmeraldSuccess
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 6. ACTION SHORTCUTS (Ask NOVA / Quick Revision / Add Note)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ask NOVA
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0x208B5CF6) else Color(0x158B5CF6),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x408B5CF6) else Color(0x308B5CF6)),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showAskNovaDialog = true }
                        .testTag("ask_nova_focus_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "✨ Ask NOVA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricViolet,
                            maxLines = 1
                        )
                    }
                }

                // Quick Revision
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0x2038BDF8) else Color(0x150284C7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x4038BDF8) else Color(0x300284C7)),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onQuickRevision?.invoke(currentSubject, currentTopic)
                                ?: Toast.makeText(context, "Opening Revision for $currentTopic", Toast.LENGTH_SHORT).show()
                        }
                        .testTag("quick_revision_focus_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isHindi) "⚡ पुनरावृत्ति" else "⚡ Quick Revision",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            maxLines = 1
                        )
                    }
                }

                // Add Note
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0x2010B981) else Color(0x15059669),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x4010B981) else Color(0x30059669)),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showAddNoteDialog = true }
                        .testTag("add_note_focus_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isHindi) "📝 नोट्स जोड़ें" else "📝 Add Note",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess,
                            maxLines = 1
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 7. UP NEXT SESSION
            // -------------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0x181E293B) else Color(0x70FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x25FFFFFF) else Color(0x15000000)),
                modifier = Modifier.fillMaxWidth().testTag("up_next_card")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isHindi) "अगला सत्र (UP NEXT)" else "UP NEXT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldenSpark,
                        letterSpacing = 1.2.sp
                    )

                    if (nextPendingItem != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${nextPendingItem.subject} • ${nextPendingItem.topic.ifBlank { nextPendingItem.chapter }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${nextPendingItem.targetMinutes} min session",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }

                            Button(
                                onClick = {
                                    if (onStartNextSession != null) {
                                        onStartNextSession(nextPendingItem)
                                    } else {
                                        onStartFocus(nextPendingItem.subject, nextPendingItem.topic.ifBlank { nextPendingItem.chapter }, nextPendingItem.targetMinutes)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                                modifier = Modifier.testTag("start_next_session_btn")
                            ) {
                                Text(
                                    text = if (isHindi) "शुरू करें →" else "Start Next →",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "आज के सभी नियोजित सत्र पूरे हो चुके हैं 🎉" else "You're done for today 🎉 All planned sessions complete!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }

        // =============================================================
        // MODALS & DIALOGS
        // =============================================================

        // 1. End Session Confirmation Dialog
        if (showEndConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showEndConfirmDialog = false },
                containerColor = if (isDark) Color(0xFF131C2E) else Color.White,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = if (isHindi) "सत्र समाप्त करें?" else "End this session?",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isHindi) "आपकी प्रगति सुरक्षित कर ली जाएगी।" else "Your progress will be saved.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                        Text(
                            text = if (isHindi) "${focusState.actualMinutesSpent} मिनट का अध्ययन इतिहास में दर्ज होगा।" else "${focusState.actualMinutesSpent} minutes studied so far will be logged.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEndConfirmDialog = false
                            onEndSession()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRose, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_end_session_btn")
                    ) {
                        Text(if (isHindi) "सत्र समाप्त करें" else "End Session", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEndConfirmDialog = false },
                        modifier = Modifier.testTag("cancel_end_session_btn")
                    ) {
                        Text(if (isHindi) "अध्ययन जारी रखें" else "Continue Studying", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // 2. Session Celebration Dialog
        if (focusState.showCelebration) {
            val summary = focusState.lastCompletedSession
            AlertDialog(
                onDismissRequest = onDismissCelebration,
                containerColor = if (isDark) Color(0xFF131C2E) else Color.White,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = GoldenSpark,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHindi) "🎉 सत्र पूरा हुआ!" else "🎉 Session Complete",
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0x2038BDF8) else Color(0x150284C7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Exam: ${summary?.examName?.ifBlank { currentExam } ?: currentExam}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                                Text(
                                    text = "${summary?.subject ?: currentSubject} • ${summary?.topic ?: currentTopic}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = if (isDark) Color(0x25FFFFFF) else Color(0x15000000))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Planned: ${summary?.plannedMinutes ?: focusState.initialMinutes}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "Actual: ${summary?.actualMinutes ?: focusState.actualMinutesSpent}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                        }

                        Text(
                            text = "+${summary?.xpEarned ?: focusState.lastSessionXp} XP Earned",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldenSpark
                        )

                        Text(
                            text = if (isHindi) "शानदार अनुशासन! सतत अध्ययन से स्थायी सफलता मिलती है।" else "Great discipline! Consistent focused blocks create lasting knowledge.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDismissCelebration,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("done_celebration_btn")
                        ) {
                            Text(if (isHindi) "सम्पन्न (Done)" else "Done", fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onDismissCelebration()
                                    onQuickRevision?.invoke(currentSubject, currentTopic)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("start_revision_btn")
                            ) {
                                Text(if (isHindi) "पुनरावृत्ति" else "Start Revision", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onDismissCelebration()
                                    onStartBreak?.invoke(5)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF070B19)),
                                modifier = Modifier.weight(1f).testTag("take_break_btn")
                            ) {
                                Text(if (isHindi) "5m विराम" else "Take 5m Break", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        }

        // 3. ✨ Ask NOVA Dialog
        if (showAskNovaDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAskNovaDialog = false
                    onDismissNovaDoubt?.invoke()
                },
                containerColor = if (isDark) Color(0xFF131C2E) else Color.White,
                shape = RoundedCornerShape(22.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = ElectricViolet, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ask NOVA • $currentTopic",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isHindi) "सत्र संदर्भ: $currentExam • $currentSubject" else "Context: $currentExam • $currentSubject",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )

                        // 4 Context Chips
                        val prompts = listOf(
                            "💡 Explain this topic" to "Explain $currentTopic in $currentSubject clearly with key concepts.",
                            "⚡ Give a quick example" to "Give a practical, exam-relevant example for $currentTopic.",
                            "❓ Give 5 practice questions" to "Give 5 high-yield multiple choice questions with answers for $currentTopic.",
                            "📝 Summarize key points" to "Summarize the essential formulas, facts, and key points for $currentTopic."
                        )

                        prompts.forEach { (label, pText) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0x208B5CF6) else Color(0x108B5CF6),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x358B5CF6) else Color(0x208B5CF6)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onAskNova?.invoke(currentSubject, "General", currentTopic, pText)
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElectricViolet,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Custom Query input
                        OutlinedTextField(
                            value = customNovaPrompt,
                            onValueChange = { customNovaPrompt = it },
                            placeholder = { Text(if (isHindi) "अपना प्रश्न यहाँ लिखें..." else "Or ask a custom doubt...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            trailingIcon = {
                                if (customNovaPrompt.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            onAskNova?.invoke(currentSubject, "General", currentTopic, customNovaPrompt)
                                            customNovaPrompt = ""
                                        }
                                    ) {
                                        Icon(Icons.Filled.Send, "Send", tint = ElectricViolet)
                                    }
                                }
                            }
                        )

                        // AI Response Viewer
                        if (isNovaDoubtThinking) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ElectricViolet)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isHindi) "NOVA उत्तर तैयार कर रहा है..." else "NOVA is analyzing your doubt...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElectricViolet
                                )
                            }
                        } else if (novaDoubtResponse.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0x251E293B) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x35FFFFFF) else Color(0x15000000)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = novaDoubtResponse,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("NOVA Reply", novaDoubtResponse)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text("Copy", fontSize = 11.sp, color = NeonCyan)
                                        }
                                        TextButton(
                                            onClick = {
                                                onSaveNote?.invoke(currentSubject, currentTopic, novaDoubtResponse)
                                                Toast.makeText(context, "Saved to Smart Notes 📝", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text("Save to Notes", fontSize = 11.sp, color = EmeraldSuccess)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAskNovaDialog = false }) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // 4. 📝 Add Note Dialog
        if (showAddNoteDialog) {
            AlertDialog(
                onDismissRequest = { showAddNoteDialog = false },
                containerColor = if (isDark) Color(0xFF131C2E) else Color.White,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "📝 Add Note for $currentTopic",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Exam: $currentExam • Subject: $currentSubject",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        OutlinedTextField(
                            value = noteInputText,
                            onValueChange = { noteInputText = it },
                            placeholder = { Text("Enter key formulas, facts or notes from this study session...") },
                            modifier = Modifier.fillMaxWidth().height(140.dp).testTag("note_input_field"),
                            maxLines = 6,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (noteInputText.isNotBlank()) {
                                onSaveNote?.invoke(currentSubject, currentTopic, noteInputText)
                                noteInputText = ""
                                showAddNoteDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF070B19)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_note_btn")
                    ) {
                        Text("Save Note", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddNoteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 5. App Shield Modal
        if (showAppShieldModal) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("focus_shield_settings_modal"),
                color = if (isDark) Color(0xFF070B19) else Color(0xFFF8FAFC)
            ) {
                FocusShieldSettingsScreen(
                    onBack = { showAppShieldModal = false }
                )
            }
        }
    }
}

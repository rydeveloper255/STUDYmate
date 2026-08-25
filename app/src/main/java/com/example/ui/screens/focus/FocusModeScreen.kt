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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExamContext
import com.example.data.model.FocusPolicy
import com.example.data.model.FocusPreset
import com.example.data.model.FocusProtectionStatus
import com.example.data.model.FocusSession
import com.example.data.model.StudyPlanItem
import com.example.data.model.UserProfile
import com.example.service.*
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ThemeToggleButton
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.FocusTimerState

@OptIn(ExperimentalMaterial3Api::class)
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

    var showShieldSettings by remember { mutableStateOf(false) }

    if (showShieldSettings) {
        FocusShieldSettingsScreen(
            onBack = { showShieldSettings = false }
        )
        return
    }

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
        focusState.topic.ifBlank { activeStudySession?.topic ?: "Sound & Waves" }
    }

    val currentGoal = remember(focusState.sessionGoal, currentSubject, currentTopic) {
        if (focusState.sessionGoal.isNotBlank()) {
            focusState.sessionGoal
        } else {
            if (isHindi) "$currentSubject में $currentTopic पूरा करें" else "Complete $currentTopic — High-Yield Review"
        }
    }

    // Local states for idle configuration
    var selectedMinutes by remember { mutableIntStateOf(focusState.initialMinutes.coerceIn(15, 120)) }
    var selectedSubject by remember { mutableStateOf(currentSubject) }
    var selectedTopic by remember { mutableStateOf(currentTopic) }
    var customMinutesInput by remember { mutableStateOf("") }
    var isCustomDurationSelected by remember { mutableStateOf(false) }
    var showStartSetupSheet by remember { mutableStateOf(false) }

    // Dialog states
    var showEndConfirmDialog by remember { mutableStateOf(false) }
    var showAskNovaDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteInputText by remember { mutableStateOf("") }
    var customNovaPrompt by remember { mutableStateOf("") }

    val policy by FocusShieldManager.currentPolicy.collectAsStateWithLifecycle()
    val activePreset by FocusShieldManager.activePreset.collectAsStateWithLifecycle()
    val protectionHealth by FocusShieldManager.protectionHealth.collectAsStateWithLifecycle()

    val blockedAppsCount = remember(policy.blockedPackages) {
        policy.blockedPackages.size
    }

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
                            text = if (focusState.isRunning) "🎯 FOCUS ACTIVE" else "🎯 FOCUS MODE 2.0",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (focusState.isRunning) NeonCyan else GoldenSpark,
                            letterSpacing = 1.2.sp
                        )
                        if (focusState.isRunning) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (focusState.isPaused) Color(0x25F59E0B) else Color(0x2510B981)
                            ) {
                                Text(
                                    text = if (focusState.isPaused) "PAUSED" else "LIVE",
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
                            .clickable { showShieldSettings = true }
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
                                text = if (blockedAppsCount > 0) "$blockedAppsCount Blocked" else "Shield",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    ThemeToggleButton()
                }
            }

            // -------------------------------------------------------------
            // 2. MAIN TIMER CARD & CIRCULAR DASHBOARD
            // -------------------------------------------------------------
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("focus_timer_card"),
                shape = RoundedCornerShape(28.dp),
                fillAlpha = if (isDark) 0.65f else 0.85f,
                borderColor = if (focusState.isRunning) NeonCyan.copy(alpha = glowAlpha) else Color(0x2538BDF8),
                borderWidth = if (focusState.isRunning) 2.dp else 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Active Preset Indicator Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0x15FFFFFF) else Color(0x10000000),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x25FFFFFF) else Color(0x15000000))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = activePreset.badgeIcon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activePreset.displayName} Policy",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Circular Countdown Timer
                    Box(
                        modifier = Modifier.size(230.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background track
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = if (isDark) Color(0x15FFFFFF) else Color(0x15000000),
                            strokeWidth = 14.dp,
                            trackColor = Color.Transparent
                        )

                        // Active animated progress
                        CircularProgressIndicator(
                            progress = { if (focusState.isRunning) sessionProgress else 0.05f },
                            modifier = Modifier.fillMaxSize(),
                            color = if (focusState.isPaused) AmberAlert else NeonCyan,
                            strokeWidth = 14.dp,
                            trackColor = Color.Transparent
                        )

                        // Inner Center Content
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (focusState.isRunning) timeFormatted else String.format("%02d:00", selectedMinutes),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (focusState.isRunning) {
                                    if (focusState.isPaused) "PAUSED" else "FOCUSING"
                                } else {
                                    "PLANNED SPRINT"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (focusState.isRunning) (if (focusState.isPaused) AmberAlert else EmeraldSuccess) else Color(0xFF94A3B8),
                                letterSpacing = 1.5.sp
                            )
                            if (focusState.isRunning) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Current: ${focusState.actualMinutesSpent} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldenSpark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Goal banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0x2038BDF8) else Color(0x150284C7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = currentGoal,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFE0F2FE) else Color(0xFF0369A1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // -------------------------------------------------------------
                    // 3. ACTION CONTROLS (RUNNING vs IDLE)
                    // -------------------------------------------------------------
                    if (focusState.isRunning) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pause / Resume
                            GlassButton(
                                text = if (focusState.isPaused) "Resume" else "Pause",
                                onClick = onTogglePause,
                                icon = if (focusState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pause_resume_button")
                            )

                            // End Focus
                            Button(
                                onClick = { showEndConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CoralRose,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("end_focus_button")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("End Focus", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // In-Focus Helper Tools
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0x15FFFFFF) else Color(0x10000000),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x25FFFFFF) else Color(0x15000000)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showAskNovaDialog = true }
                                    .testTag("ask_nova_focus_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = GoldenSpark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ask Nova", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF1E293B))
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0x15FFFFFF) else Color(0x10000000),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x25FFFFFF) else Color(0x15000000)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showAddNoteDialog = true }
                                    .testTag("quick_note_focus_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.EditNote, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Quick Note", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF1E293B))
                                }
                            }
                        }
                    } else {
                        // IDLE STATE: Quick Start + Customize Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onStartFocus(selectedSubject, selectedTopic, selectedMinutes)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = NeonCyan)
                                    .testTag("start_focus_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Start Focus ($selectedMinutes min)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassButton(
                                    text = "⚙️ Customize Setup",
                                    onClick = { showStartSetupSheet = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("customize_focus_button")
                                )
                                GlassButton(
                                    text = "🛡️ Shield Rules",
                                    onClick = { showShieldSettings = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_shield_settings_button")
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. QUICK DURATION & SUBJECT SELECTOR (When Idle)
            // -------------------------------------------------------------
            if (!focusState.isRunning) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isDark) 0.5f else 0.75f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Choose Sprint Duration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val durations = listOf(25, 45, 60, 90)
                            durations.forEach { mins ->
                                val isSelected = selectedMinutes == mins && !isCustomDurationSelected
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else if (isDark) Color(0x15FFFFFF) else Color(0x10000000),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonCyan else if (isDark) Color(0x25FFFFFF) else Color(0x15000000)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            selectedMinutes = mins
                                            isCustomDurationSelected = false
                                        }
                                        .testTag("duration_${mins}m")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$mins",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) NeonCyan else if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "min",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) NeonCyan else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }

                        // Subject chips
                        Text(
                            text = "Subject Focus",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(defaultSubjects) { subj ->
                                val isSelected = selectedSubject == subj
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSubject = subj },
                                    label = { Text(subj, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = NeonCyan,
                                        containerColor = if (isDark) Color(0x15FFFFFF) else Color(0x10000000),
                                        labelColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) NeonCyan else Color(0x20FFFFFF)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. TODAY'S FOCUS STATS & DAILY TARGET PROGRESS
            // -------------------------------------------------------------
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = if (isDark) 0.5f else 0.75f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Today's Study Focus",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$completedTodayMinutes mins completed of $targetDailyMinutes min daily target",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${(completedTodayMinutes * 100 / targetDailyMinutes).coerceIn(0, 100)}%",
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // START FOCUS SETUP BOTTOM SHEET
    // -------------------------------------------------------------
    if (showStartSetupSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStartSetupSheet = false },
            containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🎯 Configure Study Session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                // Presets
                Text(
                    text = "Select Focus Preset",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(FocusPreset.DEEP_STUDY, FocusPreset.MOCK_TEST, FocusPreset.RESEARCH, FocusPreset.LIGHT_FOCUS).forEach { preset ->
                        val isSelected = activePreset == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else if (isDark) Color(0x15FFFFFF) else Color(0x10000000),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0x20FFFFFF)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { FocusShieldManager.applyPreset(context, preset) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(preset.badgeIcon, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.displayName.take(8),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) NeonCyan else if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                // Subject input
                OutlinedTextField(
                    value = selectedSubject,
                    onValueChange = { selectedSubject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Topic / Chapter input
                OutlinedTextField(
                    value = selectedTopic,
                    onValueChange = { selectedTopic = it },
                    label = { Text("Topic or Chapter") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Policy Protection Summary
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    fillAlpha = 0.4f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "🛡️ ${blockedAppsCount} apps & ${policy.blockedWebsites.size} websites restricted • UPI & Banking protected",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                        )
                    }
                }

                Button(
                    onClick = {
                        showStartSetupSheet = false
                        onStartFocus(selectedSubject, selectedTopic, selectedMinutes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🚀 Launch Focus Session", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    // -------------------------------------------------------------
    // EARLY EXIT CONFIRMATION DIALOG
    // -------------------------------------------------------------
    if (showEndConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEndConfirmDialog = false },
            title = {
                Text(
                    text = "End Focus Session Early?",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "You've focused for ${focusState.actualMinutesSpent} minutes. Your progress will be saved to your study streak and cloud history.",
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndConfirmDialog = false
                        onEndSession()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose, contentColor = Color.White)
                ) {
                    Text("End Session", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmDialog = false }) {
                    Text("Keep Focusing", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // -------------------------------------------------------------
    // POST-SESSION COMPLETION DIALOG
    // -------------------------------------------------------------
    if (focusState.showCelebration) {
        val summary = focusState.lastCompletedSession
        AlertDialog(
            onDismissRequest = onDismissCelebration,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎉", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Focus Sprint Complete!",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Great job! You dedicated ${summary?.actualMinutes ?: focusState.actualMinutesSpent} minutes of deep, distraction-free study.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldenSpark.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "✨", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+${summary?.xpEarned ?: focusState.lastSessionXp} XP Earned • Saved to Cloud",
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissCelebration,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0F172A))
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismissCelebration()
                        onStartBreak?.invoke(5)
                    }
                ) {
                    Text("☕ Take 5m Break", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))
                }
            },
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // -------------------------------------------------------------
    // ASK NOVA IN FOCUS DIALOG
    // -------------------------------------------------------------
    if (showAskNovaDialog) {
        AlertDialog(
            onDismissRequest = { showAskNovaDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ask Nova Doubt",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Subject: $currentSubject • $currentTopic",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                    OutlinedTextField(
                        value = customNovaPrompt,
                        onValueChange = { customNovaPrompt = it },
                        placeholder = { Text("What concept or formula is causing confusion?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customNovaPrompt.isNotBlank()) {
                            onAskNova?.invoke(currentSubject, currentTopic, currentTopic, customNovaPrompt)
                            showAskNovaDialog = false
                            customNovaPrompt = ""
                            Toast.makeText(context, "Doubt sent to Nova AI", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0F172A))
                ) {
                    Text("Ask", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAskNovaDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // -------------------------------------------------------------
    // QUICK NOTE IN FOCUS DIALOG
    // -------------------------------------------------------------
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📝", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Revision Note",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "$currentSubject — $currentTopic",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldenSpark
                    )
                    OutlinedTextField(
                        value = noteInputText,
                        onValueChange = { noteInputText = it },
                        placeholder = { Text("Capture key formula, trap or mnemonic...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteInputText.isNotBlank()) {
                            onSaveNote?.invoke(currentSubject, currentTopic, noteInputText)
                            showAddNoteDialog = false
                            noteInputText = ""
                            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF0F172A))
                ) {
                    Text("Save Note", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

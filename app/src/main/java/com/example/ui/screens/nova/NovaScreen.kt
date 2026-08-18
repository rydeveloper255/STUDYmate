package com.example.ui.screens.nova

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.service.NovaUsageStatsHelper
import com.example.ui.components.GlassCard
import com.example.ui.components.glassEffect
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.InteractiveQuizState
import com.example.viewmodel.NovaScreenTab
import com.example.viewmodel.NovaViewModel

@Composable
fun NovaScreen(
    viewModel: NovaViewModel,
    onNavigateToFocus: (subject: String, topic: String, duration: Int) -> Unit = { _, _, _ -> },
    onNavigateToPlanner: () -> Unit = {},
    onOpenDocumentSummarizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val attachedBitmap by viewModel.attachedImageBitmap.collectAsState()
    val attachedUri by viewModel.attachedImageUri.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val studyContext by viewModel.studyContext.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val quizState by viewModel.quizState.collectAsState()

    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val audioRms by viewModel.voiceManager.audioLevelRms.collectAsState()
    val recognizedText by viewModel.voiceManager.recognizedText.collectAsState()

    var showVoiceModal by remember { mutableStateOf(false) }
    var showAddMemoryDialog by remember { mutableStateOf(false) }

    // Listen to ViewModel Navigation Events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { (route, params) ->
            when (route) {
                "NAVIGATE_TO_FOCUS" -> {
                    val sub = params["subject"] as? String ?: "Physics"
                    val top = params["topic"] as? String ?: "Core Revision"
                    val dur = params["duration"] as? Int ?: 25
                    onNavigateToFocus(sub, top, dur)
                }
                "NAVIGATE_TO_PLANNER" -> {
                    onNavigateToPlanner()
                }
            }
        }
    }

    // Snackbar notifications
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for Microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceModal = true
            viewModel.voiceManager.startListening { text ->
                if (text.isNotBlank()) {
                    viewModel.sendMessage(text)
                    showVoiceModal = false
                }
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice assistant", Toast.LENGTH_SHORT).show()
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setAttachedImage(it) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Master NOVA Header & Animated Avatar Orb
            NovaMasterHeader(
                studyContext = studyContext,
                voiceState = voiceState,
                onOrbClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        showVoiceModal = true
                        viewModel.voiceManager.startListening { text ->
                            if (text.isNotBlank()) {
                                viewModel.sendMessage(text)
                                showVoiceModal = false
                            }
                        }
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )

            // Quick Action Pills Row
            NovaQuickActionPills(
                onActionSelected = { action ->
                    when (action) {
                        "STUDY" -> viewModel.executeAction(NovaActionType.START_FOCUS, null)
                        "QUIZ" -> viewModel.startQuizSession(studyContext.subjects.firstOrNull() ?: "Physics", "Core Principles")
                        "FOCUS" -> onNavigateToFocus("Physics", "Focus Sprint", 25)
                        "PLAN" -> onNavigateToPlanner()
                        "VOICE" -> {
                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                showVoiceModal = true
                                viewModel.voiceManager.startListening { text ->
                                    if (text.isNotBlank()) {
                                        viewModel.sendMessage(text)
                                        showVoiceModal = false
                                    }
                                }
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        "VISION" -> imagePickerLauncher.launch("image/*")
                        "MEMORY" -> viewModel.setTab(NovaScreenTab.MEMORY_CENTER)
                        "SETTINGS" -> viewModel.setTab(NovaScreenTab.NOVA_SETTINGS)
                    }
                }
            )

            // Tab Selector
            NovaTabRow(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentTab) {
                    NovaScreenTab.DASHBOARD -> {
                        NovaDashboard(
                            studyContext = studyContext,
                            voiceState = voiceState,
                            onNavigateToChat = { viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT) },
                            onStartStudyFocus = { viewModel.executeAction(NovaActionType.START_FOCUS, null) },
                            onStartTopicQuiz = {
                                viewModel.startQuizSession(studyContext.subjects.firstOrNull() ?: "Physics", "Core Principles")
                                viewModel.setTab(NovaScreenTab.INTERACTIVE_STUDY_QUIZ)
                            },
                            onOpenFocusShield = { onNavigateToFocus("Physics", "Focus Shield", 25) },
                            onOpenStudyPlan = { onNavigateToPlanner() },
                            onPromptSelected = { prompt ->
                                viewModel.askPromptFromDashboard(prompt)
                            },
                            onVoiceClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    showVoiceModal = true
                                    viewModel.voiceManager.startListening { text ->
                                        if (text.isNotBlank()) {
                                            viewModel.sendMessage(text)
                                            showVoiceModal = false
                                        }
                                    }
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onOpenMemoryCenter = { viewModel.setTab(NovaScreenTab.MEMORY_CENTER) },
                            onOpenSettings = { viewModel.setTab(NovaScreenTab.NOVA_SETTINGS) }
                        )
                    }

                    NovaScreenTab.ASSISTANT_CHAT -> {
                        NovaChat(
                            messages = messages,
                            isGenerating = isGenerating,
                            attachedUri = attachedUri,
                            onSendMessage = { text -> viewModel.sendMessage(text) },
                            onExecuteAction = { act, payload -> viewModel.executeAction(act, payload) },
                            onSpeakTts = { text -> viewModel.voiceManager.speak(text) },
                            onClearAttachedImage = { viewModel.clearAttachedImage() },
                            onAttachImage = { imagePickerLauncher.launch("image/*") },
                            onVoiceClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    showVoiceModal = true
                                    viewModel.voiceManager.startListening { text ->
                                        if (text.isNotBlank()) {
                                            viewModel.sendMessage(text)
                                            showVoiceModal = false
                                        }
                                    }
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                    }

                    NovaScreenTab.INTERACTIVE_STUDY_QUIZ -> {
                        NovaInteractiveQuizView(
                            quizState = quizState,
                            availableSubjects = studyContext.subjects,
                            onStartNewQuiz = { sub, top -> viewModel.startQuizSession(sub, top) },
                            onSelectOption = { idx -> viewModel.selectQuizOption(idx) },
                            onSubmitAnswer = { viewModel.submitQuizAnswer() },
                            onNextQuestion = { viewModel.nextQuizQuestion() },
                            onSwitchToChat = { viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT) }
                        )
                    }

                    NovaScreenTab.MEMORY_CENTER -> {
                        NovaMemoryCenterView(
                            memories = memories,
                            isMemoryEnabled = settings.memoryEnabled,
                            onToggleMaster = { enabled ->
                                viewModel.updateSettings(settings.copy(memoryEnabled = enabled))
                            },
                            onToggleItem = { id, enabled -> viewModel.toggleMemory(id, enabled) },
                            onDeleteItem = { id -> viewModel.deleteMemory(id) },
                            onClearAll = { viewModel.clearAllMemories() },
                            onOpenAddDialog = { showAddMemoryDialog = true }
                        )
                    }

                    NovaScreenTab.NOVA_SETTINGS -> {
                        NovaSettingsView(
                            settings = settings,
                            studyContext = studyContext,
                            onUpdateSettings = { viewModel.updateSettings(it) },
                            onTestCoachAlert = { viewModel.triggerProactiveCoachAlert() },
                            onTestAppUsageAlert = { viewModel.triggerExcessiveUsageAlert() }
                        )
                    }
                }
            }
        }
    }

    // Voice Modal Overlay
    if (showVoiceModal) {
        NovaVoiceOverlayDialog(
            voiceState = voiceState,
            audioRms = audioRms,
            recognizedText = recognizedText,
            onDismiss = {
                viewModel.voiceManager.cancelListening()
                showVoiceModal = false
            },
            onConfirmSpeech = {
                if (recognizedText.isNotBlank()) {
                    viewModel.sendMessage(recognizedText)
                }
                viewModel.voiceManager.stopListening()
                showVoiceModal = false
            }
        )
    }

    // Add Memory Dialog
    if (showAddMemoryDialog) {
        AddMemoryItemDialog(
            onDismiss = { showAddMemoryDialog = false },
            onSave = { cat, key, value ->
                viewModel.addManualMemory(cat, key, value)
                showAddMemoryDialog = false
            }
        )
    }
}

// =========================================================================
// 1. NOVA MASTER HEADER & ANIMATED AVATAR
// =========================================================================

@Composable
private fun NovaMasterHeader(
    studyContext: NovaStudyContext,
    voiceState: NovaVoiceState,
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        elevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Breathing Nova Avatar
            NovaAvatar(
                voiceState = voiceState,
                onClick = onOrbClick,
                size = 58
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Good ${getTimeOfDayGreeting()}, ${studyContext.preferredTitle}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x3038BDF8))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NOVA AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val remainingMins = (studyContext.dailyTargetMinutes - studyContext.todayFocusMinutes).coerceAtLeast(0)
                val hours = remainingMins / 60
                val mins = remainingMins % 60
                val goalText = if (remainingMins == 0) "Daily goal achieved! 🎉" else "You're ${if (hours > 0) "${hours}h " else ""}${mins}m away from today's target"

                Text(
                    text = "$goalText · ${studyContext.currentStreak}d streak 🔥",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                if (studyContext.nextScheduledSession != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Next: ${studyContext.nextScheduledSession}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricViolet
                    )
                }
            }
        }
    }
}

/**
 * Animated Nova Avatar with breathing and multi-layered glowing pulse.
 * Adapts visual feedback based on IDLE, LISTENING, PROCESSING, and SPEAKING states.
 */
@Composable
fun NovaAvatar(
    voiceState: NovaVoiceState = NovaVoiceState.IDLE,
    isGenerating: Boolean = false,
    onClick: () -> Unit = {},
    size: Int = 64,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nova_avatar_breathing")

    // Continuous rotation of the holographic halo
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isGenerating) 3000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )

    // Breathing scale animation
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isGenerating) 900 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    // Glowing aura pulse alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isGenerating) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Secondary pulsating ripple
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (voiceState == NovaVoiceState.SPEAKING || voiceState == NovaVoiceState.LISTENING) 1200 else 3000,
                easing = FastOutLinearInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (voiceState == NovaVoiceState.SPEAKING || voiceState == NovaVoiceState.LISTENING) 1200 else 3000,
                easing = FastOutLinearInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    val auraColors = when {
        isGenerating || voiceState == NovaVoiceState.PROCESSING -> listOf(ElectricViolet, NebulaPurple, NeonCyan, Color(0xFFE879F9))
        voiceState == NovaVoiceState.LISTENING -> listOf(NeonGreen, NeonCyan, ElectricViolet, MatrixGreen)
        voiceState == NovaVoiceState.SPEAKING -> listOf(NeonCyan, Color(0xFFFF7043), CyberAmber, ElectricViolet)
        else -> listOf(NeonCyan, ElectricViolet, NebulaPurple, Color(0xFF38BDF8))
    }

    Box(
        modifier = modifier
            .size((size * 1.35).dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ambient glow diffusion
        Box(
            modifier = Modifier
                .size((size * 1.25).dp)
                .scale(rippleScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            auraColors.first().copy(alpha = rippleAlpha * 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Pulsating glow halo
        Box(
            modifier = Modifier
                .size((size + 10).dp)
                .scale(breathingScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            auraColors.first().copy(alpha = glowAlpha * 0.5f),
                            auraColors[1 % auraColors.size].copy(alpha = glowAlpha * 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main Orb Container
        Box(
            modifier = Modifier
                .size(size.dp)
                .scale(breathingScale)
                .springClickable(testTag = "nova_avatar_button", onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Holographic sweep ring
            Box(
                modifier = Modifier
                    .size((size + 4).dp)
                    .rotate(rotation)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(auraColors))
            )

            // Inner dark metallic core with radial gradient
            Box(
                modifier = Modifier
                    .size((size - 4).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E1B4B),
                                Color(0xFF020617)
                            )
                        )
                    )
                    .border(1.5.dp, Color(0x6038BDF8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isGenerating || voiceState == NovaVoiceState.PROCESSING -> Icons.Filled.AutoAwesome
                        voiceState == NovaVoiceState.LISTENING -> Icons.Filled.Mic
                        voiceState == NovaVoiceState.SPEAKING -> Icons.AutoMirrored.Outlined.VolumeUp
                        else -> Icons.Filled.SmartToy
                    },
                    contentDescription = "Nova Assistant",
                    tint = when {
                        isGenerating || voiceState == NovaVoiceState.PROCESSING -> NebulaPurple
                        voiceState == NovaVoiceState.LISTENING -> NeonGreen
                        voiceState == NovaVoiceState.SPEAKING -> Color(0xFFFFB74D)
                        else -> NeonCyan
                    },
                    modifier = Modifier.size((size * 0.46).dp)
                )
            }
        }
    }
}

// =========================================================================
// 2. NOVA DASHBOARD (GLASSMORPHISM STYLE - NOVA HOME)
// =========================================================================

@Composable
fun NovaDashboard(
    studyContext: NovaStudyContext,
    voiceState: NovaVoiceState,
    onNavigateToChat: () -> Unit,
    onStartStudyFocus: () -> Unit,
    onStartTopicQuiz: () -> Unit,
    onOpenFocusShield: () -> Unit,
    onOpenStudyPlan: () -> Unit,
    onPromptSelected: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onOpenMemoryCenter: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Holographic Avatar & Live Greeting Hero Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Breathing Nova Avatar
                NovaAvatar(
                    voiceState = voiceState,
                    onClick = onVoiceClick,
                    size = 72
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Good ${getTimeOfDayGreeting()}, ${studyContext.preferredTitle}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NOVA AI Companion • Ready to Assist",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Voice Prompt CTA bar inside Hero
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x3038BDF8) else Color(0x1538BDF8))
                        .border(1.dp, Color(0x4038BDF8), RoundedCornerShape(16.dp))
                        .springClickable(testTag = "hero_voice_prompt", onClick = onVoiceClick)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice Input",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tap to speak with Nova or ask anything...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Study Status Summary Card (Glassmorphism)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Analytics,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STUDY STATUS SUMMARY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x30F59E0B))
                            .border(1.dp, Color(0x60F59E0B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = GoldenSpark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${studyContext.currentStreak} Days Streak",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar & Focus Time
                val progressFraction = (studyContext.todayFocusMinutes.toFloat() / studyContext.dailyTargetMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                val remainingMins = (studyContext.dailyTargetMinutes - studyContext.todayFocusMinutes).coerceAtLeast(0)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${studyContext.todayFocusMinutes}m / ${studyContext.dailyTargetMinutes}m",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (remainingMins == 0) "Daily target reached! 🌟" else "$remainingMins mins left for today's goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Liquid Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isDark) Color(0x30FFFFFF) else Color(0x30000000))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeonCyan, ElectricViolet, NebulaPurple)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Exam & Next Session Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x18FFFFFF) else Color(0x0A000000))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "TARGET EXAM",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "${studyContext.targetExam} (${studyContext.examDaysRemaining}d)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x18FFFFFF) else Color(0x0A000000))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "NEXT SESSION",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = studyContext.nextScheduledSession ?: "${studyContext.subjects.firstOrNull() ?: "Physics"}: Core Revision",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricViolet,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Primary Action Grid (2x2 Glass Cards: Study, Quiz, Focus, Plan)
        Text(
            text = "PRIMARY ACTIONS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action 1: Study
            NovaActionCard(
                title = "Study",
                subtitle = "Smart AI Coach",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f),
                onClick = onStartStudyFocus
            )

            // Action 2: Quiz
            NovaActionCard(
                title = "Quiz",
                subtitle = "Topic Practice",
                icon = Icons.Filled.Quiz,
                accentColor = ElectricViolet,
                modifier = Modifier.weight(1f),
                onClick = onStartTopicQuiz
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action 3: Focus
            NovaActionCard(
                title = "Focus",
                subtitle = "Focus Shield",
                icon = Icons.Filled.TrackChanges,
                accentColor = NeonGreen,
                modifier = Modifier.weight(1f),
                onClick = onOpenFocusShield
            )

            // Action 4: Plan
            NovaActionCard(
                title = "Plan",
                subtitle = "Study Schedule",
                icon = Icons.Filled.EditCalendar,
                accentColor = Color(0xFFFFB74D),
                modifier = Modifier.weight(1f),
                onClick = onOpenStudyPlan
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Quick Suggestions / Conversation Starters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUGGESTED QUESTIONS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet,
                modifier = Modifier.padding(start = 4.dp)
            )

            Text(
                text = "Open Chat 💬",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                modifier = Modifier
                    .springClickable(testTag = "dashboard_open_chat", onClick = onNavigateToChat)
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val promptSuggestions = listOf(
            "📚 Explain ${studyContext.weakTopics.firstOrNull() ?: "Kirchhoff's Rules"} with step-by-step intuition",
            "🧠 Quiz me on 5 high-yield ${studyContext.subjects.firstOrNull() ?: "Physics"} MCQs",
            "⚡ Create a 2-hour study plan for today's session",
            "🔍 Give me formula mnemonics for ${studyContext.subjects.getOrNull(1) ?: "Chemistry"}"
        )

        promptSuggestions.forEach { prompt ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0x221E293B) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color(0x3038BDF8) else Color(0x2094A3B8), RoundedCornerShape(14.dp))
                    .springClickable(testTag = "prompt_${prompt.hashCode()}") {
                        onPromptSelected(prompt)
                    }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Open Full Nova Chat Button
        Button(
            onClick = onNavigateToChat,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_full_chat_button"),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubbleOutline,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Open Conversational Chat",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NovaActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    GlassCard(
        modifier = modifier
            .springClickable(testTag = "action_card_${title.lowercase()}", onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
    }
}

// =========================================================================
// 3. QUICK ACTION PILLS ROW
// =========================================================================

@Composable
private fun NovaQuickActionPills(
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        Triple("STUDY", "📚 Study Focus", NeonCyan),
        Triple("QUIZ", "🧠 Topic Quiz", ElectricViolet),
        Triple("FOCUS", "🎯 Focus Shield", NeonGreen),
        Triple("PLAN", "📝 Study Plan", Color(0xFFFFB74D)),
        Triple("VOICE", "🎙️ Talk to Nova", NeonCyan),
        Triple("VISION", "📷 Ask from Image", ElectricViolet),
        Triple("MEMORY", "🧠 Memory Center", Color(0xFF38BDF8)),
        Triple("SETTINGS", "⚙️ Nova Settings", Color(0xFF94A3B8))
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { (id, label, color) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.12f))
                    .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .springClickable(testTag = "nova_quick_$id") {
                        onActionSelected(id)
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
        }
    }
}

// =========================================================================
// 4. NOVA TAB ROW
// =========================================================================

@Composable
private fun NovaTabRow(
    currentTab: NovaScreenTab,
    onTabSelected: (NovaScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    val tabs = listOf(
        NovaScreenTab.DASHBOARD to "🏠 Home",
        NovaScreenTab.ASSISTANT_CHAT to "💬 Chat",
        NovaScreenTab.INTERACTIVE_STUDY_QUIZ to "🧠 Quiz",
        NovaScreenTab.MEMORY_CENTER to "🔒 Memory",
        NovaScreenTab.NOVA_SETTINGS to "⚙️ Settings"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDark) Color(0xFF131C2E).copy(alpha = 0.7f) else Color(0xFFF1F5F9))
            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x2094A3B8), RoundedCornerShape(18.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEach { (tab, title) ->
            val isSelected = currentTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        if (isSelected) 1.dp else 0.dp,
                        if (isSelected) NeonCyan.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(14.dp)
                    )
                    .springClickable(testTag = "nova_tab_${tab.name.lowercase()}") {
                        onTabSelected(tab)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) NeonCyan else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    maxLines = 1
                )
            }
        }
    }
}

// =========================================================================
// 5. TAB 1: ASSISTANT CHAT VIEW (NovaChat)
// =========================================================================

@Composable
fun NovaChat(
    messages: List<NovaChatMessage>,
    isGenerating: Boolean,
    attachedUri: Uri?,
    onSendMessage: (String) -> Unit,
    onExecuteAction: (NovaActionType, String?) -> Unit,
    onSpeakTts: (String) -> Unit,
    onClearAttachedImage: () -> Unit,
    onAttachImage: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NovaChatView(
        messages = messages,
        isGenerating = isGenerating,
        attachedUri = attachedUri,
        onSendMessage = onSendMessage,
        onExecuteAction = onExecuteAction,
        onSpeakTts = onSpeakTts,
        onClearAttachedImage = onClearAttachedImage,
        onAttachImage = onAttachImage,
        onVoiceClick = onVoiceClick,
        modifier = modifier
    )
}

@Composable
private fun NovaChatView(
    messages: List<NovaChatMessage>,
    isGenerating: Boolean,
    attachedUri: Uri?,
    onSendMessage: (String) -> Unit,
    onExecuteAction: (NovaActionType, String?) -> Unit,
    onSpeakTts: (String) -> Unit,
    onClearAttachedImage: () -> Unit,
    onAttachImage: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val isDark = isAppInDarkTheme()

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "What should I study right now?",
        "Give me a 5-question quiz on Physics",
        "Explain Lenz's Law in simple Hinglish",
        "Start 25m Focus Session",
        "How can I improve my study streak?"
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Chat Messages Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                NovaMessageBubble(
                    message = msg,
                    onExecuteAction = onExecuteAction,
                    onSpeakTts = onSpeakTts
                )
            }

            if (isGenerating) {
                item {
                    NovaThinkingBubble()
                }
            }
        }

        // Quick Prompt Suggestions
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFFE2E8F0))
                        .border(1.dp, if (isDark) Color(0x3038BDF8) else Color(0x40CBD5E1), RoundedCornerShape(14.dp))
                        .springClickable { onSendMessage(prompt) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                    )
                }
            }
        }

        // Image Attachment Preview
        if (attachedUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = attachedUri,
                        contentDescription = "Attached Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Image attached for Nova Vision solver",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClearAttachedImage) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove Image", tint = Color.Red)
                }
            }
        }

        // Input Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .glassEffect(
                    shape = RoundedCornerShape(28.dp),
                    elevation = 12.dp,
                    fillAlpha = 0.85f
                ),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach Image Button
                IconButton(
                    onClick = onAttachImage,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Attach image",
                        tint = NeonCyan
                    )
                }

                // Voice Mic Button
                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice Assistant",
                        tint = ElectricViolet
                    )
                }

                // Text Input
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Ask Nova (Hindi / Hinglish / English)...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nova_input_field"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() || attachedUri != null) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                        )
                        .testTag("nova_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NovaMessageBubble(
    message: NovaChatMessage,
    onExecuteAction: (NovaActionType, String?) -> Unit,
    onSpeakTts: (String) -> Unit
) {
    val isUser = message.sender == NovaSender.USER
    val isDark = isAppInDarkTheme()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(NeonCyan, ElectricViolet, NebulaPurple)))
                    .border(1.dp, Color(0x60FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "Nova",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (message.attachedImageUri != null) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = message.attachedImageUri,
                        contentDescription = "User attached image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Surface(
                modifier = Modifier.glassEffect(
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    ),
                    elevation = 6.dp,
                    fillAlpha = if (isUser) 0.85f else 0.7f
                ),
                color = if (isUser) {
                    if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.85f) else Color(0xFFDBEAFE)
                } else {
                    Color.Transparent
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                        lineHeight = 22.sp
                    )

                    // Actionable tool card if attached
                    if (message.actionType != NovaActionType.NONE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        NovaActionCard(
                            actionType = message.actionType,
                            payload = message.actionPayload,
                            onExecute = { onExecuteAction(message.actionType, message.actionPayload) }
                        )
                    }

                    // TTS button for Nova replies
                    if (!isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .springClickable { onSpeakTts(message.text) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                        contentDescription = "Read aloud",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Listen",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaActionCard(
    actionType: NovaActionType,
    payload: String?,
    onExecute: () -> Unit
) {
    val (title, icon, buttonLabel, color) = when (actionType) {
        NovaActionType.START_FOCUS -> Quadruple(
            "Ready to Focus?",
            Icons.Filled.TrackChanges,
            "Start 25m Focus Session 🎯",
            NeonGreen
        )
        NovaActionType.START_QUIZ -> Quadruple(
            "Test Your Knowledge",
            Icons.Filled.Psychology,
            "Launch Interactive Quiz 🧠",
            ElectricViolet
        )
        NovaActionType.CREATE_PLAN -> Quadruple(
            "Balanced Study Plan",
            Icons.Filled.CalendarMonth,
            "View / Create Study Plan 📝",
            Color(0xFFFFB74D)
        )
        NovaActionType.CREATE_REMINDER -> Quadruple(
            "Study Reminder Ready",
            Icons.Filled.Alarm,
            "Set Reminder ⏰",
            NeonCyan
        )
        NovaActionType.OPEN_APP_BLOCKING -> Quadruple(
            "App Shield Protection",
            Icons.Filled.Shield,
            "Open Focus Shield 🛡️",
            Color(0xFFF43F5E)
        )
        NovaActionType.OPEN_MEMORY -> Quadruple(
            "Nova Privacy Memory",
            Icons.Filled.Storage,
            "Open Memory Center 🧠",
            NeonCyan
        )
        else -> Quadruple("", Icons.Filled.Star, "Execute", NeonCyan)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onExecute,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun NovaThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_dots")
    val alpha1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1")
    val alpha2 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "d2")
    val alpha3 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "d3")

    Row(
        modifier = Modifier.padding(start = 40.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonCyan.copy(alpha = alpha1)))
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ElectricViolet.copy(alpha = alpha2)))
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NebulaPurple.copy(alpha = alpha3)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Nova is thinking...",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8)
        )
    }
}

// =========================================================================
// 5. TAB 2: INTERACTIVE STUDY & QUIZ VIEW
// =========================================================================

@Composable
private fun NovaInteractiveQuizView(
    quizState: InteractiveQuizState,
    availableSubjects: List<String>,
    onStartNewQuiz: (String, String) -> Unit,
    onSelectOption: (Int) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onSwitchToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var selectedSubject by remember { mutableStateOf(availableSubjects.firstOrNull() ?: "Physics") }
    var selectedTopic by remember { mutableStateOf("Core Concepts") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Quiz Header & Subject Picker
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🧠 Interactive Topic Quiz",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = "Solve conceptual MCQs generated by Nova with immediate explanations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableSubjects) { sub ->
                        val isSel = sub == selectedSubject
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (isSel) NeonCyan else Color(0x3094A3B8), RoundedCornerShape(12.dp))
                                .springClickable {
                                    selectedSubject = sub
                                    onStartNewQuiz(sub, "All Topics")
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) NeonCyan else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (quizState.isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nova is generating high-yield quiz questions...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else if (quizState.isQuizFinished) {
            // Quiz Finished Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 Quiz Completed!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Score: ${quizState.score} / ${quizState.questions.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onStartNewQuiz(selectedSubject, selectedTopic) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("New Quiz", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onSwitchToChat,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ask Nova", color = NeonCyan)
                        }
                    }
                }
            }
        } else if (quizState.questions.isNotEmpty()) {
            val question = quizState.questions.getOrNull(quizState.currentIndex)
            if (question != null) {
                // Question Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Question ${quizState.currentIndex + 1} of ${quizState.questions.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = "Score: ${quizState.score}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = question.questionText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Option Items
                        question.options.forEachIndexed { index, optionText ->
                            val isSelected = quizState.selectedOptionIndex == index
                            val isSubmitted = quizState.isAnswerSubmitted
                            val isCorrectOption = index == question.correctOptionIndex

                            val borderColor = when {
                                isSubmitted && isCorrectOption -> NeonGreen
                                isSubmitted && isSelected && !isCorrectOption -> Color.Red
                                isSelected -> NeonCyan
                                else -> if (isDark) Color(0x20FFFFFF) else Color(0x3094A3B8)
                            }

                            val bgColor = when {
                                isSubmitted && isCorrectOption -> NeonGreen.copy(alpha = 0.15f)
                                isSubmitted && isSelected && !isCorrectOption -> Color.Red.copy(alpha = 0.15f)
                                isSelected -> NeonCyan.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bgColor)
                                    .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                                    .springClickable(testTag = "quiz_opt_$index") {
                                        onSelectOption(index)
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val optLetter = ('A' + index).toString()
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) NeonCyan else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = optLetter,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else if (isDark) Color.White else Color(0xFF1E293B)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Submit or Next Button
                        if (!quizState.isAnswerSubmitted) {
                            Button(
                                onClick = onSubmitAnswer,
                                enabled = quizState.selectedOptionIndex != null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Check Answer", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Explanation Glass Card
                            if (quizState.explanation.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricViolet.copy(alpha = 0.12f))
                                        .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "💡 Concept Explanation:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricViolet
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = quizState.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Button(
                                onClick = onNextQuestion,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Next Question →", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 6. TAB 3: PRIVACY MEMORY CENTER VIEW
// =========================================================================

@Composable
private fun NovaMemoryCenterView(
    memories: List<NovaMemoryItem>,
    isMemoryEnabled: Boolean,
    onToggleMaster: (Boolean) -> Unit,
    onToggleItem: (Long, Boolean) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearAll: () -> Unit,
    onOpenAddDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var selectedCategory by remember { mutableStateOf<NovaMemoryCategory?>(null) }

    val filteredMemories = remember(memories, selectedCategory) {
        if (selectedCategory == null) memories else memories.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Privacy Banner Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🧠 Nova Personal Memory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Local-first privacy: All remembered study preferences are stored on-device only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = isMemoryEnabled,
                        onCheckedChange = onToggleMaster,
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenAddDialog,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Memory", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear All", color = Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                val isSel = selectedCategory == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (isSel) NeonCyan else Color(0x3094A3B8), RoundedCornerShape(12.dp))
                        .springClickable { selectedCategory = null }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "All (${memories.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) NeonCyan else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                }
            }
            items(NovaMemoryCategory.entries) { cat ->
                val isSel = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (isSel) NeonCyan else Color(0x3094A3B8), RoundedCornerShape(12.dp))
                        .springClickable { selectedCategory = cat }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) NeonCyan else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Memory List
        if (filteredMemories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No memories stored in this category yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredMemories.forEach { item ->
                    NovaMemoryCard(
                        item = item,
                        onToggle = { onToggleItem(item.id, it) },
                        onDelete = { onDeleteItem(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NovaMemoryCard(
    item: NovaMemoryItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isAppInDarkTheme()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricViolet.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricViolet
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Source: ${item.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.key,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Text(
                    text = item.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete memory",
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}

// =========================================================================
// 7. TAB 4: NOVA ASSISTANT SETTINGS VIEW
// =========================================================================

@Composable
private fun NovaSettingsView(
    settings: NovaSettings,
    studyContext: NovaStudyContext,
    onUpdateSettings: (NovaSettings) -> Unit,
    onTestCoachAlert: () -> Unit,
    onTestAppUsageAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()

    var hasUsagePermission by remember { mutableStateOf(NovaUsageStatsHelper.hasUsageStatsPermission(context)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Persona & Voice Settings Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🤖 Persona & Communication",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Address as Boss toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Address casually as \"Boss\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "e.g. \"Boss 😄 Physics ka time ho gaya\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = settings.useBossGreeting,
                        onCheckedChange = { onUpdateSettings(settings.copy(useBossGreeting = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0x20FFFFFF))

                // TTS Auto-speak toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Text-to-Speech Voice Responses",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Automatically read Nova replies aloud",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = settings.ttsAutoSpeak,
                        onCheckedChange = { onUpdateSettings(settings.copy(ttsAutoSpeak = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }
            }
        }

        // Smart Coach & Usage Awareness Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🛡️ Smart Coach & Usage Awareness",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Proactive Study Reminders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Proactive Study Reminders",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Friendly nudges at scheduled study slots",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = settings.studyRemindersEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(studyRemindersEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0x20FFFFFF))

                // Distracting App Usage Awareness
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Excessive Social Media Awareness",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (hasUsagePermission) "Usage access granted · Distraction nudges enabled" else "Requires Usage Access permission to detect screen time",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasUsagePermission) NeonGreen else Color(0xFFF59E0B)
                        )
                    }
                    if (!hasUsagePermission) {
                        Button(
                            onClick = {
                                NovaUsageStatsHelper.openUsageAccessSettings(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Grant", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Switch(
                            checked = settings.appUsageAwarenessEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(appUsageAwarenessEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                        )
                    }
                }
            }
        }

        // Test Notifications Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔔 Test Proactive Nova Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTestCoachAlert,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Test Study Alert", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onTestAppUsageAlert,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Test Usage Alert", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Privacy & Transparency Ledger
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔒 Privacy & Permission Transparency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val permissionsTable = listOf(
                    Triple("Study Schedule & Targets", "To generate contextual study plans & reminders", "100% On-Device Room DB"),
                    Triple("Personal Memory", "Remembers weak topics & study habits", "100% On-Device Room DB"),
                    Triple("App Usage Stats", "To detect distracting screen time while study is pending", "100% On-Device (No tracking)"),
                    Triple("Microphone", "To enable voice conversation with Nova", "Processed for voice recognition only"),
                    Triple("Camera / Photos", "To scan questions and formulas from textbooks", "Sent securely to Gemini for solving")
                )

                permissionsTable.forEach { (perm, reason, storage) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "• $perm",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Text(
                            text = "  Purpose: $reason\n  Storage: $storage",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 8. VOICE ASSISTANT OVERLAY MODAL
// =========================================================================

@Composable
private fun NovaVoiceOverlayDialog(
    voiceState: NovaVoiceState,
    audioRms: Float,
    recognizedText: String,
    onDismiss: () -> Unit,
    onConfirmSpeech: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .glassEffect(
                    shape = RoundedCornerShape(28.dp),
                    elevation = 16.dp,
                    fillAlpha = 0.92f
                ),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎙️ NOVA Voice Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Reactive pulsating voice orb
                val orbSize = 80 + (audioRms * 40).toInt()
                Box(
                    modifier = Modifier
                        .size(orbSize.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NeonCyan, ElectricViolet, Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(2.dp, NeonGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Listening",
                            tint = NeonGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (recognizedText.isNotBlank()) "\"$recognizedText\"" else "Listening to you, Boss...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }

                    Button(
                        onClick = onConfirmSpeech,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ask Nova", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 9. ADD MEMORY ITEM DIALOG
// =========================================================================

@Composable
private fun AddMemoryItemDialog(
    onDismiss: () -> Unit,
    onSave: (NovaMemoryCategory, String, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(NovaMemoryCategory.ACADEMIC) }
    var keyText by remember { mutableStateOf("") }
    var valueText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .glassEffect(
                    shape = RoundedCornerShape(24.dp),
                    elevation = 16.dp,
                    fillAlpha = 0.95f
                ),
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🧠 Add to Nova Memory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Category:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(NovaMemoryCategory.entries) { cat ->
                        val isSel = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (isSel) NeonCyan else Color(0x3094A3B8), RoundedCornerShape(8.dp))
                                .springClickable { selectedCategory = cat }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) NeonCyan else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("Memory Topic / Key (e.g. Weak in Optics)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("Details (e.g. Needs sign convention revision)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (keyText.isNotBlank() && valueText.isNotBlank()) {
                                onSave(selectedCategory, keyText, valueText)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Memory", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getTimeOfDayGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..21 -> "evening"
        else -> "night"
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

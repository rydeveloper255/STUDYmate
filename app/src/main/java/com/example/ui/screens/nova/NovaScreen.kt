package com.example.ui.screens.nova

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import android.app.Application
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.NovaVoiceState
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassDialog
import com.example.ui.components.springClickable
import com.example.ui.screens.voicenotes.VoiceNotesTab
import com.example.ui.theme.*
import com.example.viewmodel.NovaScreenTab
import com.example.viewmodel.NovaViewModel
import com.example.viewmodel.VoiceNotesViewModel
import com.example.viewmodel.VoiceNotesViewModelFactory

@Composable
fun NovaScreen(
    viewModel: NovaViewModel,
    onNavigateToFocus: (subject: String, topic: String, duration: Int) -> Unit = { _, _, _ -> },
    onNavigateToPlanner: () -> Unit = {},
    onOpenDocumentSummarizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val voiceNotesViewModel: VoiceNotesViewModel = viewModel(
        factory = VoiceNotesViewModelFactory(context.applicationContext as Application)
    )
    val currentTab by viewModel.currentTab.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val audioRms by viewModel.voiceManager.audioLevelRms.collectAsState()
    val recognizedText by viewModel.voiceManager.recognizedText.collectAsState()

    // Step 23 Smart Learning System States
    val showMcqConfigDialog by viewModel.showMcqConfigDialog.collectAsState()
    val showRevisionDialog by viewModel.showRevisionDialog.collectAsState()
    val showDailyBriefDialog by viewModel.showDailyBriefDialog.collectAsState()
    val activeRevisionTopic by viewModel.activeRevisionTopic.collectAsState()
    val dailyExamBriefing by viewModel.dailyExamBriefing.collectAsState()
    val studyContext by viewModel.studyContext.collectAsState()

    var showVoiceModal by remember { mutableStateOf(false) }
    var showQuickSwitchMenu by remember { mutableStateOf(false) }

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

    // Mic Permission Launcher
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
            Toast.makeText(context, "Microphone permission required for NOVA Voice commands", Toast.LENGTH_SHORT).show()
        }
    }

    fun startListeningWithPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (currentTab != NovaScreenTab.DASHBOARD && currentTab != NovaScreenTab.ASSISTANT_CHAT && currentTab != NovaScreenTab.SMART_SEARCH && currentTab != NovaScreenTab.SMART_NOTES) {
                NovaSubScreenTopBar(
                    currentTab = currentTab,
                    isDark = isDark,
                    onBackToHub = { viewModel.setTab(NovaScreenTab.DASHBOARD) },
                    onOpenQuickSwitch = { showQuickSwitchMenu = true }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (currentTab == NovaScreenTab.DASHBOARD || currentTab == NovaScreenTab.ASSISTANT_CHAT || currentTab == NovaScreenTab.SMART_SEARCH || currentTab == NovaScreenTab.SMART_NOTES) PaddingValues(0.dp) else paddingValues)
        ) {
            when (currentTab) {
                NovaScreenTab.DASHBOARD -> {
                    NovaDashboardTab(
                        viewModel = viewModel,
                        onNavigateToFocus = onNavigateToFocus,
                        onNavigateToPlanner = onNavigateToPlanner,
                        onRequestMicPermission = { startListeningWithPermission() }
                    )
                }
                NovaScreenTab.ASSISTANT_CHAT -> {
                    NovaAssistantChatTab(
                        viewModel = viewModel,
                        onRequestMicPermission = { startListeningWithPermission() },
                        onBackToHub = { viewModel.setTab(NovaScreenTab.DASHBOARD) }
                    )
                }
                NovaScreenTab.SMART_SEARCH -> {
                    NovaSmartSearchTab(
                        viewModel = viewModel,
                        onNavigateToFocus = onNavigateToFocus,
                        onBackToHub = { viewModel.setTab(NovaScreenTab.DASHBOARD) },
                        onRequestMicPermission = { startListeningWithPermission() }
                    )
                }
                NovaScreenTab.SMART_NOTES -> {
                    NovaSmartNotesTab(
                        viewModel = viewModel,
                        onBackToHub = { viewModel.setTab(NovaScreenTab.DASHBOARD) }
                    )
                }
                NovaScreenTab.CURRENT_AFFAIRS -> {
                    NovaCurrentAffairsTab(
                        viewModel = viewModel
                    )
                }
                NovaScreenTab.VOICE_NOTES -> {
                    VoiceNotesTab(
                        viewModel = voiceNotesViewModel
                    )
                }
                NovaScreenTab.INTERACTIVE_STUDY_QUIZ -> {
                    NovaQuizIntelligenceTab(viewModel = viewModel)
                }
                NovaScreenTab.MEMORY_CENTER -> {
                    NovaMemoryCenterTab(viewModel = viewModel)
                }
                NovaScreenTab.ANALYTICS_STRATEGY -> {
                    NovaStrategyAnalyticsTab(viewModel = viewModel)
                }
                NovaScreenTab.NOVA_SETTINGS -> {
                    NovaSettingsPrivacyTab(viewModel = viewModel)
                }
            }

            // Voice Modal Overlay
            if (showVoiceModal) {
                NovaVoiceListeningModal(
                    recognizedText = recognizedText,
                    voiceState = voiceState,
                    audioRms = audioRms,
                    onStopListening = {
                        viewModel.voiceManager.stopListening()
                        showVoiceModal = false
                    },
                    onCancel = {
                        viewModel.voiceManager.cancelListening()
                        showVoiceModal = false
                    }
                )
            }

            // Quick Tool Switcher Dialog
            if (showQuickSwitchMenu) {
                NovaQuickSwitchDialog(
                    currentTab = currentTab,
                    isDark = isDark,
                    onDismiss = { showQuickSwitchMenu = false },
                    onSelectTab = { tab ->
                        viewModel.setTab(tab)
                        showQuickSwitchMenu = false
                    }
                )
            }

            // Step 23 Smart Learning System Dialogs
            if (showMcqConfigDialog) {
                NovaWebMcqGeneratorDialog(
                    initialTopic = "Recent Space Missions & Science Updates",
                    examName = studyContext.targetExam,
                    onDismiss = { viewModel.setShowMcqConfigDialog(false) },
                    onGenerate = { config ->
                        viewModel.generateFreshWebMcqs(config)
                    }
                )
            }

            if (showRevisionDialog && activeRevisionTopic != null) {
                SmartRevisionSessionDialog(
                    item = activeRevisionTopic!!,
                    examName = studyContext.targetExam,
                    onDismiss = { viewModel.setShowRevisionDialog(false) },
                    onComplete = { score, total ->
                        viewModel.completeRevisionSession(score, total)
                        viewModel.setShowRevisionDialog(false)
                    },
                    onAskNova = { prompt ->
                        viewModel.setShowRevisionDialog(false)
                        viewModel.sendMessage(prompt)
                    }
                )
            }

            if (showDailyBriefDialog && dailyExamBriefing != null) {
                DailyExamBriefingDialog(
                    briefing = dailyExamBriefing,
                    onDismiss = { viewModel.setShowDailyBriefDialog(false) },
                    onStartPractice = { topic ->
                        viewModel.setShowDailyBriefDialog(false)
                        viewModel.generateFreshWebMcqs(
                            com.example.data.model.SmartMcqConfig(
                                topicQuery = topic,
                                questionCount = 10,
                                difficulty = "Medium",
                                examName = studyContext.targetExam
                            )
                        )
                    },
                    onAskNova = { prompt ->
                        viewModel.setShowDailyBriefDialog(false)
                        viewModel.sendMessage(prompt)
                    }
                )
            }
        }
    }
}

@Composable
private fun NovaSubScreenTopBar(
    currentTab: NovaScreenTab,
    isDark: Boolean,
    onBackToHub: () -> Unit,
    onOpenQuickSwitch: () -> Unit
) {
    Surface(
        color = if (isDark) DarkSurface.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f),
        border = BorderStroke(0.5.dp, if (isDark) Color(0x20FFFFFF) else Color(0x18000000)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBackToHub,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x15FFFFFF) else Color(0x0A000000))
                        .testTag("nova_back_to_hub_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to NOVA Hub",
                        tint = if (isDark) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "${currentTab.icon} ${currentTab.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "NOVA AI Companion",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            }

            IconButton(
                onClick = onOpenQuickSwitch,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x15FFFFFF) else Color(0x0A000000))
            ) {
                Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = "Switch Tool",
                    tint = if (isDark) NeonCyan else DeepIndigo,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NovaQuickSwitchDialog(
    currentTab: NovaScreenTab,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSelectTab: (NovaScreenTab) -> Unit
) {
    GlassDialog(
        onDismissRequest = onDismiss,
        title = "Switch Tool",
        subtitle = "Jump to any NOVA AI capability",
        dismissText = "Cancel"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NovaScreenTab.values().forEach { tab ->
                val isSelected = tab == currentTab
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        if (isDark) Color(0x2838BDF8) else Color(0x186366F1)
                    } else {
                        if (isDark) Color(0x12FFFFFF) else Color(0x08000000)
                    },
                    border = BorderStroke(
                        0.5.dp,
                        if (isSelected) NeonCyan.copy(alpha = 0.5f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .springClickable { onSelectTab(tab) }
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(tab.icon, fontSize = 16.sp)
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    if (isDark) NeonCyan else DeepIndigo
                                } else {
                                    if (isDark) Color.White else Color(0xFF0F172A)
                                }
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaVoiceListeningModal(
    recognizedText: String,
    voiceState: NovaVoiceState,
    audioRms: Float,
    onStopListening: () -> Unit,
    onCancel: () -> Unit
) {
    val isDark = isAppInDarkTheme()

    Dialog(onDismissRequest = onCancel) {
        GlassCard(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NOVA is listening...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Speak your doubt or study command",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(20.dp))

                NovaOrbVisualizer(
                    voiceState = voiceState,
                    audioRms = audioRms,
                    size = 100.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0x22131C2E) else Color(0x10000000),
                    border = BorderStroke(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x2064748B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (recognizedText.isNotBlank()) recognizedText else "Listening to your voice...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (recognizedText.isNotBlank()) {
                                if (isDark) Color.White else Color(0xFF0F172A)
                            } else {
                                if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            },
                            fontWeight = if (recognizedText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                    }
                    Button(
                        onClick = onStopListening,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, color = Color(0xFF0B1120))
                    }
                }
            }
        }
    }
}

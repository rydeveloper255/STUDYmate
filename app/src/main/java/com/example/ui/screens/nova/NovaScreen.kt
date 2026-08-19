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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import android.app.Application
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.NovaVoiceState
import com.example.ui.components.GlassCard
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
    val voiceNotesViewModel: VoiceNotesViewModel = viewModel(
        factory = VoiceNotesViewModelFactory(context.applicationContext as Application)
    )
    val currentTab by viewModel.currentTab.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val audioRms by viewModel.voiceManager.audioLevelRms.collectAsState()
    val recognizedText by viewModel.voiceManager.recognizedText.collectAsState()

    var showVoiceModal by remember { mutableStateOf(false) }

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
        containerColor = DarkCanvas,
        topBar = {
            NovaTopBar(
                currentTab = currentTab,
                onSelectTab = { viewModel.setTab(it) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        onRequestMicPermission = { startListeningWithPermission() }
                    )
                }
                NovaScreenTab.SMART_SEARCH -> {
                    NovaSmartSearchTab(
                        viewModel = viewModel,
                        onNavigateToFocus = onNavigateToFocus
                    )
                }
                NovaScreenTab.SMART_NOTES -> {
                    NovaSmartNotesTab(
                        viewModel = viewModel
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
        }
    }
}

@Composable
private fun NovaTopBar(
    currentTab: NovaScreenTab,
    onSelectTab: (NovaScreenTab) -> Unit
) {
    Surface(
        color = DarkCanvas.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            .background(NeonCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NOVA AI Assistant",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ONLINE • ORIGINAL FEMALE AI VOICE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Selector Chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NovaScreenTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeonCyan else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.springClickable { onSelectTab(tab) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tab.icon, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.8f)
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
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NOVA is listening...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Bolna start karein (e.g. '30 min focus mode laga do')",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                NovaOrbVisualizer(
                    voiceState = voiceState,
                    audioRms = audioRms,
                    size = 110.dp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkCanvas.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (recognizedText.isNotBlank()) recognizedText else "Speak now...",
                            fontSize = 14.sp,
                            color = if (recognizedText.isNotBlank()) Color.White else TextSecondary,
                            fontWeight = if (recognizedText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(
                        onClick = onStopListening,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, color = DarkCanvas)
                    }
                }
            }
        }
    }
}

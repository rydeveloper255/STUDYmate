package com.example.ui.screens.nova

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.service.voice.NovaVoiceEmotion
import com.example.ui.components.GlassCard
import com.example.ui.components.NovaVoiceWaveform
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaViewModel
import kotlinx.coroutines.launch

@Composable
fun NovaAssistantChatTab(
    viewModel: NovaViewModel,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val attachedUri by viewModel.attachedImageUri.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val audioRms by viewModel.voiceManager.audioLevelRms.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll on new message
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setAttachedImage(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // --- Conversation List ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                NovaChatMessageItem(
                    message = msg,
                    onExecuteAction = { actionType, payload ->
                        viewModel.executeAction(actionType, payload)
                    },
                    onSpeak = { text ->
                        if (voiceState == NovaVoiceState.SPEAKING) {
                            viewModel.voiceManager.stopSpeaking()
                        } else {
                            viewModel.voiceManager.speak(text, NovaVoiceEmotion.CALM)
                        }
                    },
                    isSpeaking = voiceState == NovaVoiceState.SPEAKING
                )
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier
                            .padding(start = 8.dp, top = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceElevated)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NovaVoiceWaveform(
                            isActive = true,
                            isProcessing = true,
                            barCount = 5,
                            minBarHeight = 6.dp,
                            maxBarHeight = 18.dp,
                            barWidth = 3.dp,
                            barSpacing = 3.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "NOVA is formulating answer...",
                            fontSize = 12.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- Attached Image Preview ---
        if (attachedUri != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = attachedUri,
                            contentDescription = "Attached Image",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Textbook / Doubt Image Attached", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Step-by-step math/physics solver ready", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = { viewModel.clearAttachedImage() }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = CoralPink)
                    }
                }
            }
        }

        // --- Bottom Input & Voice Dock ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.95f),
            borderColor = NeonCyan.copy(alpha = 0.25f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach Image Button
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") }
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Attach Question Photo",
                        tint = NeonCyan
                    )
                }

                // Text Input
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (voiceState == NovaVoiceState.LISTENING) "Listening..." else "Ask NOVA doubt, formula or plan...",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = false,
                    maxLines = 3
                )

                // Voice Mic Button
                IconButton(
                    onClick = {
                        onRequestMicPermission()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (voiceState == NovaVoiceState.LISTENING) NeonCyan else Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (voiceState == NovaVoiceState.LISTENING) DarkCanvas else NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() || attachedUri != null) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() || attachedUri != null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() || attachedUri != null) ElectricIndigo else Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() || attachedUri != null) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NovaChatMessageItem(
    message: NovaChatMessage,
    onExecuteAction: (NovaActionType, String?) -> Unit,
    onSpeak: (String) -> Unit,
    isSpeaking: Boolean
) {
    val isUser = message.sender == NovaSender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) ElectricIndigo.copy(alpha = 0.9f) else DarkSurfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (isUser) ElectricIndigo else Color.White.copy(alpha = 0.12f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.attachedImageUri != null) {
                        AsyncImage(
                            model = message.attachedImageUri,
                            contentDescription = "Attached Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .padding(bottom = 8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 19.sp
                    )

                    // Action Trigger Cards
                    if (message.actionType != NovaActionType.NONE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ActionTriggerButton(
                            actionType = message.actionType,
                            payload = message.actionPayload,
                            onClick = { onExecuteAction(message.actionType, message.actionPayload) }
                        )
                    }

                    // Voice Readout Button on Assistant message
                    if (!isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSpeaking) {
                                NovaVoiceWaveform(
                                    isActive = true,
                                    isProcessing = false,
                                    barCount = 4,
                                    minBarHeight = 4.dp,
                                    maxBarHeight = 14.dp,
                                    barWidth = 2.5.dp,
                                    barSpacing = 2.5.dp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                            TextButton(
                                onClick = { onSpeak(message.text) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = "Voice Readout",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSpeaking) "Stop" else "Listen",
                                    fontSize = 11.sp,
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

@Composable
private fun ActionTriggerButton(
    actionType: NovaActionType,
    payload: String?,
    onClick: () -> Unit
) {
    val (label, icon, color) = when (actionType) {
        NovaActionType.START_FOCUS -> Triple("Start Focus Session", Icons.Default.PlayArrow, NeonCyan)
        NovaActionType.START_QUIZ -> Triple("Start Interactive Quiz", Icons.Default.Psychology, AmberGold)
        NovaActionType.CREATE_PLAN -> Triple("Generate Study Plan", Icons.Default.CalendarMonth, EmeraldGreen)
        NovaActionType.CREATE_REMINDER -> Triple("Save Study Reminder", Icons.Default.Alarm, CoralPink)
        NovaActionType.OPEN_APP_BLOCKING -> Triple("Open Focus Shield", Icons.Default.Shield, NeonCyan)
        NovaActionType.OPEN_MEMORY -> Triple("View NOVA Memory", Icons.Default.Memory, ElectricIndigo)
        NovaActionType.RECOVER_MISSED_SESSION -> Triple("Start 20m Recovery", Icons.Default.Refresh, CoralPink)
        else -> Triple("Open Action", Icons.Default.Check, NeonCyan)
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = DarkCanvas, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkCanvas)
    }
}

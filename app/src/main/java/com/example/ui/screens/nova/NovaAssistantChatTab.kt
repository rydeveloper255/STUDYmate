package com.example.ui.screens.nova

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.service.voice.NovaVoiceEmotion
import com.example.ui.components.GlassCard
import com.example.ui.components.NovaVoiceWaveform
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaConversationSession
import com.example.viewmodel.NovaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NovaAssistantChatTab(
    viewModel: NovaViewModel,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier,
    onBackToHub: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val attachedUri by viewModel.attachedImageUri.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val studyContext by viewModel.studyContext.collectAsState()
    val savedConversations by viewModel.savedConversations.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll on new message or generation state
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo picker launcher (Gallery)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setAttachedImage(it) }
    }

    // Context tag string
    val currentExam = studyContext.targetExam.trim().ifBlank { "Competitive Exam" }
    val currentSubject = studyContext.selectedSubject.takeIf { it.isNotBlank() && it != "All Subjects" }
        ?: studyContext.subjects.firstOrNull() ?: ""
    val contextIndicatorText = if (currentSubject.isNotBlank()) {
        "$currentExam • $currentSubject"
    } else {
        currentExam
    }

    val hasUserInteracted = messages.any { it.sender == NovaSender.USER }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .statusBarsPadding()
    ) {
        // =========================================================================
        // 1. TOP APP BAR
        // =========================================================================
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f),
            borderColor = Color.White.copy(alpha = 0.12f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back button + Title & Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (onBackToHub != null) {
                        IconButton(
                            onClick = onBackToHub,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to NOVA Hub",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Glowing NOVA Badge
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NeonCyan.copy(alpha = 0.25f), ElectricIndigo.copy(alpha = 0.35f))
                                )
                            )
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NOVA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Status Dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isGenerating) AmberGold else EmeraldGreen)
                            )
                        }
                        Text(
                            text = if (isGenerating) "Formulating response..." else "Personal Study Assistant",
                            fontSize = 11.sp,
                            color = if (isGenerating) AmberGold else NeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right: Conversation History & More Options
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // History Button
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Conversation History",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("➕ Start New Chat", color = Color.White, fontSize = 13.sp) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.startNewChat()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "🌐 Language: ${settings.language}",
                                        color = NeonCyan,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.toggleLanguageMode()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (settings.ttsAutoSpeak) "🔊 Auto-Speak: On" else "🔇 Auto-Speak: Off",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.updateSettings(settings.copy(ttsAutoSpeak = !settings.ttsAutoSpeak))
                                }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            DropdownMenuItem(
                                text = { Text("🗑️ Clear Current Chat", color = CoralPink, fontSize = 13.sp) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.clearCurrentChat()
                                }
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. CONTEXT INDICATOR CHIP
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = contextIndicatorText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // =========================================================================
        // 3. CONVERSATION AREA
        // =========================================================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
            ) {
                // Empty state greeting card with study-aware quick prompts
                if (!hasUserInteracted) {
                    item {
                        NovaWelcomeHeroCard(
                            examName = currentExam,
                            subjectName = currentSubject,
                            onSelectPrompt = { prompt ->
                                viewModel.sendMessage(prompt)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Messages list
                items(messages, key = { it.id }) { msg ->
                    NovaChatMessageBubble(
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
                        isSpeaking = voiceState == NovaVoiceState.SPEAKING,
                        onCopyText = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("NOVA Notes", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // AI Response generation indicator
                if (isGenerating) {
                    item {
                        NovaThinkingBubble()
                    }
                }
            }
        }

        // =========================================================================
        // 4. ATTACHED IMAGE PREVIEW (IF ANY)
        // =========================================================================
        AnimatedVisibility(
            visible = attachedUri != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (attachedUri != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
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
                                Text(
                                    text = "Doubt Image Attached",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Step-by-step problem solver active",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.clearAttachedImage() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove attached image",
                                tint = CoralPink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 5. ACTIVE VOICE LISTENING BANNER (IF LISTENING)
        // =========================================================================
        AnimatedVisibility(
            visible = voiceState == NovaVoiceState.LISTENING,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurfaceElevated.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        NovaVoiceWaveform(
                            isActive = true,
                            isProcessing = false,
                            barCount = 6,
                            minBarHeight = 6.dp,
                            maxBarHeight = 22.dp,
                            barWidth = 3.5.dp,
                            barSpacing = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🎙️ Listening to you...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = "Speak your doubt, question or topic",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    TextButton(
                        onClick = { viewModel.voiceManager.stopListening() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Done", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // =========================================================================
        // 6. FLOATING GLASS COMPOSER
        // =========================================================================
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .navigationBarsPadding()
                .imePadding(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.95f),
            borderColor = NeonCyan.copy(alpha = 0.3f),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plus / Attachment Button
                Box {
                    IconButton(
                        onClick = { showAttachmentMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Attach Doubt Photo",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false },
                        modifier = Modifier.background(DarkSurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gallery / Textbook Photo", color = Color.White, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showAttachmentMenu = false
                                imagePickerLauncher.launch("image/*")
                            }
                        )
                    }
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
                val isListening = voiceState == NovaVoiceState.LISTENING
                val micScale by animateFloatAsState(
                    targetValue = if (isListening) 1.15f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                    label = "mic_scale"
                )

                IconButton(
                    onClick = {
                        if (isListening) {
                            viewModel.voiceManager.stopListening()
                        } else {
                            onRequestMicPermission()
                        }
                    },
                    modifier = Modifier
                        .scale(micScale)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) NeonCyan else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (isListening) NeonCyan else Color.White.copy(alpha = 0.12f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) DarkCanvas else NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Send Button
                val canSend = inputText.isNotBlank() || attachedUri != null
                IconButton(
                    onClick = {
                        if (canSend) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) Brush.linearGradient(listOf(NeonCyan, ElectricIndigo)) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.05f)))
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) Color.White else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // =========================================================================
    // 7. CONVERSATION HISTORY DIALOG
    // =========================================================================
    if (showHistoryDialog) {
        NovaConversationHistoryDialog(
            sessions = savedConversations,
            onDismiss = { showHistoryDialog = false },
            onLoadSession = { session ->
                viewModel.loadConversation(session)
                showHistoryDialog = false
            },
            onDeleteSession = { sessionId ->
                viewModel.deleteConversation(sessionId)
            },
            onStartNewChat = {
                viewModel.startNewChat()
                showHistoryDialog = false
            }
        )
    }
}

// =============================================================================
// WELCOME HERO CARD WITH STUDY-AWARE PROMPTS
// =============================================================================
@Composable
private fun NovaWelcomeHeroCard(
    examName: String,
    subjectName: String,
    onSelectPrompt: (String) -> Unit
) {
    val cleanSub = subjectName.ifBlank { "Physics" }

    val quickPrompts = listOf(
        "💡 Explain $cleanSub core concepts simply with examples",
        "🎯 Give me 5 practice questions with step-by-step solutions",
        "📝 Provide key formulas & definitions for $cleanSub",
        "⚡ Create a 30-minute high-yield study plan for today"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f),
        borderColor = NeonCyan.copy(alpha = 0.35f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Orb
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeonCyan.copy(alpha = 0.3f), ElectricIndigo.copy(alpha = 0.4f))
                        )
                    )
                    .border(1.5.dp, NeonCyan.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Hi, I'm NOVA.",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Your Personal AI Study Assistant",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeonCyan
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ask me doubts, solve formulas, generate practice questions, or create smart study schedules for $examName.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "TRY ASKING:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic Prompt Chips
            quickPrompts.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .springClickable { onSelectPrompt(prompt) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// CHAT MESSAGE BUBBLE
// =============================================================================
@Composable
fun NovaChatMessageBubble(
    message: NovaChatMessage,
    onExecuteAction: (NovaActionType, String?) -> Unit,
    onSpeak: (String) -> Unit,
    isSpeaking: Boolean,
    onCopyText: (String) -> Unit
) {
    val isUser = message.sender == NovaSender.USER
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeonCyan.copy(alpha = 0.25f), ElectricIndigo.copy(alpha = 0.35f))
                        )
                    )
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 310.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) ElectricIndigo.copy(alpha = 0.9f) else DarkSurfaceElevated.copy(alpha = 0.95f),
                border = BorderStroke(
                    1.dp,
                    if (isUser) ElectricIndigo else Color.White.copy(alpha = 0.12f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Attached image preview in bubble
                    if (message.attachedImageUri != null) {
                        AsyncImage(
                            model = message.attachedImageUri,
                            contentDescription = "Attached Doubt Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .padding(bottom = 8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Main Text
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 19.sp
                    )

                    // Action Trigger Cards
                    if (message.actionType != NovaActionType.NONE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        NovaActionTriggerButton(
                            actionType = message.actionType,
                            payload = message.actionPayload,
                            onClick = { onExecuteAction(message.actionType, message.actionPayload) }
                        )
                    }

                    // Bottom utility row for Assistant messages
                    if (!isUser) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeFormatted,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Copy Button
                                IconButton(
                                    onClick = { onCopyText(message.text) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Notes",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Listen Button
                                if (isSpeaking) {
                                    NovaVoiceWaveform(
                                        isActive = true,
                                        isProcessing = false,
                                        barCount = 4,
                                        minBarHeight = 4.dp,
                                        maxBarHeight = 14.dp,
                                        barWidth = 2.5.dp,
                                        barSpacing = 2.5.dp,
                                        modifier = Modifier.padding(end = 4.dp)
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
                    } else {
                        // User message timestamp
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeFormatted,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// THINKING / GENERATING BUBBLE
// =============================================================================
@Composable
private fun NovaThinkingBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                    text = "NOVA is thinking...",
                    fontSize = 12.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// =============================================================================
// ACTION TRIGGER BUTTON
// =============================================================================
@Composable
private fun NovaActionTriggerButton(
    actionType: NovaActionType,
    payload: String?,
    onClick: () -> Unit
) {
    var isConfirmed by remember { mutableStateOf(false) }
    var isCancelled by remember { mutableStateOf(false) }

    if (isCancelled) {
        Text(
            text = "Action cancelled",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        return
    }

    if (isConfirmed) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = EmeraldGreen.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Action Completed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
            }
        }
        return
    }

    val (label, icon, color) = when (actionType) {
        NovaActionType.START_FOCUS, NovaActionType.START_STUDY_SESSION -> Triple("Start Focus Session", Icons.Default.PlayArrow, NeonCyan)
        NovaActionType.START_QUIZ -> Triple("Start Interactive Quiz", Icons.Default.Psychology, AmberGold)
        NovaActionType.CREATE_PLAN, NovaActionType.CREATE_STUDY_TASK -> Triple("Add to Today's Study Plan", Icons.Default.CalendarMonth, EmeraldGreen)
        NovaActionType.UPDATE_STUDY_TASK -> Triple("Update Study Plan", Icons.Default.Edit, NeonCyan)
        NovaActionType.ADD_REVISION_ITEM -> Triple("Add Revision Card", Icons.Default.Style, AmberGold)
        NovaActionType.CREATE_REMINDER -> Triple("Save Study Reminder", Icons.Default.Alarm, CoralPink)
        NovaActionType.OPEN_MOCK_TEST -> Triple("Open Exam Mock Test", Icons.Default.Quiz, NeonCyan)
        NovaActionType.OPEN_SUBJECT -> Triple("View Selected Subject", Icons.Default.Book, ElectricIndigo)
        NovaActionType.OPEN_TOPIC -> Triple("View Selected Topic", Icons.Default.Topic, NeonCyan)
        NovaActionType.OPEN_STUDY_PLAN -> Triple("Open Study Planner", Icons.Default.CalendarToday, EmeraldGreen)
        NovaActionType.OPEN_FOCUS_MODE -> Triple("Open Focus Timer", Icons.Default.Timer, NeonCyan)
        NovaActionType.SHOW_PROGRESS -> Triple("View Progress Analytics", Icons.Default.BarChart, AmberGold)
        NovaActionType.SHOW_TEST_RESULT -> Triple("View Test Results", Icons.Default.Analytics, NeonCyan)
        NovaActionType.OPEN_APP_BLOCKING -> Triple("Open Focus Shield", Icons.Default.Shield, NeonCyan)
        NovaActionType.OPEN_MEMORY -> Triple("View NOVA Memory", Icons.Default.Memory, ElectricIndigo)
        NovaActionType.RECOVER_MISSED_SESSION -> Triple("Start 20m Recovery", Icons.Default.Refresh, CoralPink)
        else -> Triple("Perform Action", Icons.Default.Check, NeonCyan)
    }

    val isModifyingAction = when (actionType) {
        NovaActionType.CREATE_STUDY_TASK, NovaActionType.UPDATE_STUDY_TASK,
        NovaActionType.CREATE_PLAN, NovaActionType.CREATE_REMINDER,
        NovaActionType.ADD_REVISION_ITEM -> true
        else -> false
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isModifyingAction) "NOVA Recommendation: $label" else label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isModifyingAction) {
                    TextButton(
                        onClick = { isCancelled = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Cancel", fontSize = 12.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(
                    onClick = {
                        isConfirmed = true
                        onClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isModifyingAction) "Confirm / Add" else "Open",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkCanvas
                    )
                }
            }
        }
    }
}

// =============================================================================
// CONVERSATION HISTORY DIALOG
// =============================================================================
@Composable
private fun NovaConversationHistoryDialog(
    sessions: List<NovaConversationSession>,
    onDismiss: () -> Unit,
    onLoadSession: (NovaConversationSession) -> Unit,
    onDeleteSession: (String) -> Unit,
    onStartNewChat: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.95f),
            borderColor = NeonCyan.copy(alpha = 0.4f),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chat History",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Start New Chat Button
                Button(
                    onClick = onStartNewChat,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Fresh Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved conversation history yet.\nChats are automatically saved when you start a new one.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(session.timestamp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .springClickable { onLoadSession(session) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "$dateStr • ${session.messages.size} messages",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteSession(session.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete chat",
                                            tint = CoralPink.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
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
}

package com.example.ui.screens.tutor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.glassEffect
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.ChatMessage

@Composable
fun GeminiTutorScreen(
    messages: List<ChatMessage>,
    isAiThinking: Boolean,
    useThinkingMode: Boolean,
    tutorPersona: String,
    isTtsSpeaking: Boolean,
    onSendMessage: (String) -> Unit,
    onSendQuickAction: (String) -> Unit,
    onSolveImage: (Bitmap) -> Unit,
    onToggleThinkingMode: (Boolean) -> Unit,
    onSelectPersona: (String) -> Unit,
    onSpeakTts: (String) -> Unit,
    onOpenDocumentSummarizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showPersonaMenu by remember { mutableStateOf(false) }

    val personas = listOf("Friendly AI Tutor", "Socratic Guide", "Step-by-Step Solver", "Strict Professor")
    val quickChips = listOf("📄 Summarize Doc", "Explain Simply", "Give Example", "Quiz Me", "Explain Another Way")

    // Image Picker for Question Solver
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                onSolveImage(bitmap)
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    // Voice Input Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(top = 16.dp)
            .testTag("gemini_tutor_screen")
    ) {
        // 1. Tutor Header & Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonCyan, NebulaPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "✨ Gemini Study Tutor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (useThinkingMode) "Gemini 3.1 Pro (High Thinking)" else "Gemini 3.5 Flash",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (useThinkingMode) NeonCyan else Color(0xFF94A3B8)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Thinking Mode Toggle Pill
                FilterChip(
                    selected = useThinkingMode,
                    onClick = { onToggleThinkingMode(!useThinkingMode) },
                    label = {
                        Text(
                            text = if (useThinkingMode) "🧠 Thinking ON" else "🧠 Thinking",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = Color(0xFF070B19),
                        containerColor = Color(0x20FFFFFF),
                        labelColor = Color(0xFFCBD5E1)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = useThinkingMode,
                        borderColor = if (useThinkingMode) Color.White else Color(0x30FFFFFF)
                    ),
                    modifier = Modifier.testTag("thinking_mode_toggle")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Persona Selector Menu
                Box {
                    IconButton(
                        onClick = { showPersonaMenu = true },
                        modifier = Modifier.testTag("tutor_persona_menu_btn")
                    ) {
                        Icon(Icons.Outlined.Tune, "Settings", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showPersonaMenu,
                        onDismissRequest = { showPersonaMenu = false },
                        containerColor = Color(0xFF131C2E)
                    ) {
                        personas.forEach { persona ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = persona,
                                        color = if (tutorPersona == persona) NeonCyan else Color.White,
                                        fontWeight = if (tutorPersona == persona) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectPersona(persona)
                                    showPersonaMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    isTtsSpeaking = isTtsSpeaking,
                    onSpeak = { onSpeakTts(msg.text) }
                )
            }

            if (isAiThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (useThinkingMode) "Gemini 3.1 Pro is reasoning deeply..." else "StudyMate is typing...",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        // 3. Quick Action Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickChips) { chip ->
                SuggestionChip(
                    onClick = {
                        if (chip == "📄 Summarize Doc") {
                            onOpenDocumentSummarizer()
                        } else {
                            onSendQuickAction(chip)
                        }
                    },
                    label = {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0x251E293B)
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = Color(0x3538BDF8)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("chip_${chip.lowercase().replace(" ", "_")}")
                )
            }
        }

        // 4. Input Bar with Voice & Camera Scanner & Doc Summarizer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .padding(bottom = 80.dp), // Clear bottom nav
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document Summarizer Button
            IconButton(
                onClick = onOpenDocumentSummarizer,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x286366F1))
                    .testTag("tutor_doc_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = "Summarize Document",
                    tint = ElectricIndigo,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Camera Question Solver Button
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x2838BDF8))
                    .testTag("tutor_camera_button")
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Scan Question", tint = NeonCyan, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Voice Input Button
            IconButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Gemini Study Tutor...")
                    }
                    try {
                        speechLauncher.launch(intent)
                    } catch (e: Exception) {
                        // Speech not supported on some environments
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x25818CF8))
                    .testTag("tutor_mic_button")
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice Input", tint = ElectricViolet, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text Input Box
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Ask a concept or problem...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tutor_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0x35FFFFFF),
                    focusedContainerColor = Color(0x30111827),
                    unfocusedContainerColor = Color(0x20111827)
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val toSend = inputText
                        inputText = ""
                        onSendMessage(toSend)
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank()) Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                        else Brush.linearGradient(listOf(Color(0x20FFFFFF), Color(0x10FFFFFF)))
                    )
                    .testTag("tutor_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) Color(0xFF070B19) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isTtsSpeaking: Boolean,
    onSpeak: () -> Unit
) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonCyan, ElectricViolet))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isUser) Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF4F46E5)))
                        else Brush.linearGradient(listOf(Color(0x351E293B), Color(0x251E293B)))
                    )
                    .border(
                        1.dp,
                        if (isUser) Color(0x4038BDF8) else Color(0x30FFFFFF),
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Column {
                    if (message.isThinking && !isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(Icons.Filled.Psychology, null, tint = GoldenSpark, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "High Thinking Analysis",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldenSpark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 22.sp
                    )
                }
            }

            if (!isUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                ) {
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isTtsSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Listen",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

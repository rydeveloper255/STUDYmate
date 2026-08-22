package com.example.ui.screens.nova

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppNavTab
import com.example.data.model.*
import com.example.service.voice.NovaVoiceEmotion
import com.example.viewmodel.NovaScreenTab
import com.example.viewmodel.NovaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaHomeUniversalWidget(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTab: ((AppNavTab) -> Unit)? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val widgetState by viewModel.homeWidgetDisplayState.collectAsState()
    val answerMsg by viewModel.homeWidgetAnswer.collectAsState()
    val thinkingStatus by viewModel.homeWidgetThinkingStatus.collectAsState()
    val studyContext by viewModel.studyContext.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showPlusMenu by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    var isSavedLocally by remember { mutableStateOf(false) }

    // Voice recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
                viewModel.submitHomeWidgetQuery(spokenText, context, onNavigateToTab)
            } else {
                voiceError = "Couldn't catch that. Please try again."
            }
        } else {
            voiceError = null
        }
    }

    fun startVoiceInput() {
        voiceError = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to NOVA (e.g. 'Aaj ke current affairs batao')")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            voiceError = "Voice input is not supported on this device"
        }
    }

    val placeholderText = remember(studyContext.targetExam) {
        if (studyContext.targetExam.isNotBlank()) {
            "✦ Ask NOVA about ${studyContext.targetExam.take(18)}..."
        } else {
            "✦ Ask NOVA anything..."
        }
    }

    // Liquid Glass Container
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                .padding(14.dp)
        ) {
            // MAIN SEARCH & COMMAND BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sparkle Badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Input Field
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = placeholderText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (inputText.isNotBlank()) {
                                keyboardController?.hide()
                                isSavedLocally = false
                                viewModel.submitHomeWidgetQuery(inputText, context, onNavigateToTab)
                            }
                        }
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                // Plus / Actions Button
                IconButton(
                    onClick = { showPlusMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = "Nova Tools",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Microphone / Submit Action
                if (inputText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            isSavedLocally = false
                            viewModel.submitHomeWidgetQuery(inputText, context, onNavigateToTab)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Submit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { startVoiceInput() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Voice Error Banner if any
            if (voiceError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = voiceError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { startVoiceInput() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // 1. COLLAPSED STATE: Quick Action Chips
            if (widgetState == HomeWidgetDisplayState.COLLAPSED) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickChip(
                        icon = "📅",
                        label = "Today's CA",
                        onClick = {
                            inputText = "Aaj ke current affairs batao"
                            isSavedLocally = false
                            viewModel.submitHomeWidgetQuery("Aaj ke current affairs batao", context, onNavigateToTab)
                        }
                    )
                    QuickChip(
                        icon = "🎯",
                        label = "Weak Topics",
                        onClick = {
                            inputText = "Mera weak topic batao"
                            isSavedLocally = false
                            viewModel.submitHomeWidgetQuery("Mera weak topic batao", context, onNavigateToTab)
                        }
                    )
                    QuickChip(
                        icon = "📝",
                        label = "Make Quiz",
                        onClick = {
                            inputText = "Iska quiz bana do"
                            isSavedLocally = false
                            viewModel.submitHomeWidgetQuery("Iska quiz bana do", context, onNavigateToTab)
                        }
                    )
                    QuickChip(
                        icon = "💡",
                        label = "What to study?",
                        onClick = {
                            inputText = "NOVA, mujhe aaj kya padhna chahiye?"
                            isSavedLocally = false
                            viewModel.submitHomeWidgetQuery("NOVA, mujhe aaj kya padhna chahiye?", context, onNavigateToTab)
                        }
                    )
                    QuickChip(
                        icon = "📚",
                        label = "Revision",
                        onClick = {
                            inputText = "Revision questions do"
                            isSavedLocally = false
                            viewModel.submitHomeWidgetQuery("Revision questions do", context, onNavigateToTab)
                        }
                    )
                    QuickChip(
                        icon = "⏱️",
                        label = "25m Focus",
                        onClick = {
                            viewModel.executeContextualAction(
                                NovaContextualAction(
                                    label = "Focus",
                                    actionType = NovaActionType.START_FOCUS,
                                    payload = "{\"minutes\":25}"
                                ),
                                context,
                                onNavigateToTab
                            )
                        }
                    )
                }
            }

            // 2. THINKING STATE: Glowing / Pulse Indicator
            if (widgetState == HomeWidgetDisplayState.THINKING) {
                Spacer(modifier = Modifier.height(14.dp))
                ThinkingAnimationBar(statusText = thinkingStatus)
            }

            // 3. EXPANDED ANSWER STATE: Concise Answer + Preview Cards + Action Buttons
            if (widgetState == HomeWidgetDisplayState.EXPANDED && answerMsg != null) {
                val msg = answerMsg!!
                Spacer(modifier = Modifier.height(12.dp))

                // Answer Card Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Header: NOVA Tag, Audio read button, Close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✦", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "NOVA Answer",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Voice TTS readout button
                                val isSpeaking = voiceState == NovaVoiceState.SPEAKING
                                IconButton(
                                    onClick = {
                                        if (isSpeaking) {
                                            viewModel.voiceManager.stopSpeaking()
                                        } else {
                                            viewModel.voiceManager.speak(msg.text)
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = "Read Aloud",
                                        tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Save Note Action Icon
                                IconButton(
                                    onClick = {
                                        viewModel.saveNovaAnswerAsNote(msg.text)
                                        isSavedLocally = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        if (isSavedLocally) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                                        contentDescription = "Save Note",
                                        tint = if (isSavedLocally) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Close / Collapse button
                                IconButton(
                                    onClick = {
                                        viewModel.collapseHomeWidget()
                                        inputText = ""
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Answer Text
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )

                        // Current Affairs Preview Cards (if present)
                        if (!msg.currentAffairsPreview.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Top Headlines:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            msg.currentAffairsPreview.forEach { item ->
                                CurrentAffairsMiniCard(
                                    item = item,
                                    onClick = {
                                        viewModel.executeContextualAction(
                                            NovaContextualAction(
                                                label = "Open CA",
                                                actionType = NovaActionType.OPEN_CURRENT_AFFAIRS
                                            ),
                                            context,
                                            onNavigateToTab
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons Bar
                        if (msg.actionButtons.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                msg.actionButtons.forEach { action ->
                                    if (action.isPrimary) {
                                        Button(
                                            onClick = {
                                                viewModel.executeContextualAction(action, context, onNavigateToTab)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = action.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.executeContextualAction(action, context, onNavigateToTab)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = action.label,
                                                style = MaterialTheme.typography.labelMedium
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

    // PLUS / ACTIONS BOTTOM SHEET
    if (showPlusMenu) {
        ModalBottomSheet(
            onDismissRequest = { showPlusMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "✦ NOVA Study Hub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Fast actions and smart assistant tools",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                NovaSheetActionTile(
                    icon = Icons.Default.CameraAlt,
                    title = "Ask from Image",
                    subtitle = "Capture questions, formulas, or diagrams",
                    onClick = {
                        showPlusMenu = false
                        viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
                        onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
                    }
                )

                NovaSheetActionTile(
                    icon = Icons.Default.Description,
                    title = "Upload Document & Summarize",
                    subtitle = "Extract key takeaways, formulas, and flashcards",
                    onClick = {
                        showPlusMenu = false
                        viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
                        onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
                    }
                )

                NovaSheetActionTile(
                    icon = Icons.Default.Quiz,
                    title = "Create Custom Quiz",
                    subtitle = "Generate high-yield test for any topic",
                    onClick = {
                        showPlusMenu = false
                        viewModel.setTab(NovaScreenTab.INTERACTIVE_STUDY_QUIZ)
                        onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
                    }
                )

                NovaSheetActionTile(
                    icon = Icons.Default.Search,
                    title = "Universal Smart Search",
                    subtitle = "Instant formulas, summaries, and pyqs",
                    onClick = {
                        showPlusMenu = false
                        viewModel.setTab(NovaScreenTab.SMART_SEARCH)
                        onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
                    }
                )

                NovaSheetActionTile(
                    icon = Icons.Default.Chat,
                    title = "Open Full Dedicated NOVA",
                    subtitle = "Access complete voice & multimodal tutor",
                    onClick = {
                        showPlusMenu = false
                        viewModel.setTab(NovaScreenTab.ASSISTANT_CHAT)
                        onNavigateToTab?.invoke(AppNavTab.AI_TUTOR)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun QuickChip(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ThinkingAnimationBar(statusText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "nova_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "✦ NOVA: $statusText",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CurrentAffairsMiniCard(
    item: CurrentAffairsItem,
    onClick: () -> Unit
) {
    val categoryIcon = when (item.category.lowercase()) {
        "economy", "finance" -> "📈"
        "science", "tech", "science & tech" -> "🔬"
        "polity", "national" -> "🏛️"
        "environment" -> "🌿"
        "international" -> "🌍"
        else -> "📰"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryIcon,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun NovaSheetActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

package com.example.ui.screens.nova

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppNavTab
import com.example.data.model.NovaActionType
import com.example.data.model.NovaContextualAction
import com.example.data.model.NovaVoiceState
import com.example.service.voice.NovaVoiceEmotion
import com.example.viewmodel.NovaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaFloatingAssistant(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTab: ((AppNavTab) -> Unit)? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val appContext by viewModel.appContext.collectAsState()
    val answerMsg by viewModel.homeWidgetAnswer.collectAsState()
    val widgetState by viewModel.homeWidgetDisplayState.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val isFloatingOpen by viewModel.isFloatingNovaOpen.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var queryText by remember { mutableStateOf("") }
    var voiceError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isFloatingOpen) {
        if (isFloatingOpen) {
            showSheet = true
            viewModel.setFloatingNovaOpen(false)
        }
    }

    // Voice recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                queryText = spokenText
                viewModel.submitHomeWidgetQuery(spokenText, context, onNavigateToTab)
            } else {
                voiceError = "Couldn't catch that. Please speak again."
            }
        } else {
            voiceError = null
        }
    }

    fun startVoiceInput() {
        voiceError = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask NOVA anything (e.g. 'Iska formula samjhao', 'Quiz banao')")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            voiceError = "Voice input is not supported on this device"
        }
    }

    // If on active mock test screen, hide floating assistant to respect exam integrity
    if (appContext.isTestActive) {
        return
    }

    Box(modifier = modifier) {
        // Floating Pill Button ("Ask Nova... 🎙️")
        FloatingActionButton(
            onClick = { showSheet = true },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
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
                    Text("✦", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ask Nova... 🎙️",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Quick Assistant Bottom Sheet
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                ) {
                    // Header & Context badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✦", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nova Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Screen Context Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = appContext.topic ?: appContext.subject ?: appContext.screenName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Bar with Voice Button & Submit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = queryText,
                            onValueChange = { queryText = it },
                            placeholder = { Text("Ask Nova about ${appContext.topic ?: appContext.subject ?: "anything"}...") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (queryText.isNotBlank()) {
                                        keyboardController?.hide()
                                        viewModel.submitHomeWidgetQuery(queryText, context, onNavigateToTab)
                                    }
                                }
                            )
                        )

                        // Voice Mic Button
                        IconButton(
                            onClick = { startVoiceInput() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Submit Button
                        IconButton(
                            onClick = {
                                if (queryText.isNotBlank()) {
                                    keyboardController?.hide()
                                    viewModel.submitHomeWidgetQuery(queryText, context, onNavigateToTab)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (voiceError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = voiceError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Contextual Suggestions Chips based on current screen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeContext = appContext.topic ?: appContext.subject ?: "this"
                        when {
                            appContext.screenName.contains("STUDY", ignoreCase = true) || appContext.subject != null -> {
                                SuggestionChip(
                                    onClick = {
                                        val q = "Explain key concept of $activeContext"
                                        queryText = q
                                        viewModel.submitHomeWidgetQuery(q, context, onNavigateToTab)
                                    },
                                    label = { Text("💡 Explain Concept", style = MaterialTheme.typography.labelSmall) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        val q = "Make 5 questions for $activeContext"
                                        queryText = q
                                        viewModel.submitHomeWidgetQuery(q, context, onNavigateToTab)
                                    },
                                    label = { Text("✍️ Practice 5", style = MaterialTheme.typography.labelSmall) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        viewModel.executeContextualAction(
                                            NovaContextualAction(
                                                label = "Start Focus",
                                                actionType = NovaActionType.START_FOCUS,
                                                payload = "{\"subject\":\"${appContext.subject ?: "General"}\",\"topic\":\"$activeContext\",\"minutes\":25}"
                                            ),
                                            context,
                                            onNavigateToTab
                                        )
                                        showSheet = false
                                    },
                                    label = { Text("⏱️ 25m Focus", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            appContext.screenName.contains("PROGRESS", ignoreCase = true) || appContext.screenName.contains("PRACTICE", ignoreCase = true) -> {
                                SuggestionChip(
                                    onClick = {
                                        val q = "Mera weak topic batao"
                                        queryText = q
                                        viewModel.submitHomeWidgetQuery(q, context, onNavigateToTab)
                                    },
                                    label = { Text("🎯 Weak Topics", style = MaterialTheme.typography.labelSmall) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        val q = "Revision questions do"
                                        queryText = q
                                        viewModel.submitHomeWidgetQuery(q, context, onNavigateToTab)
                                    },
                                    label = { Text("📚 Smart Revision", style = MaterialTheme.typography.labelSmall) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        viewModel.executeContextualAction(
                                            NovaContextualAction(
                                                label = "Mock Test",
                                                actionType = NovaActionType.OPEN_MOCK_TEST
                                            ),
                                            context,
                                            onNavigateToTab
                                        )
                                        showSheet = false
                                    },
                                    label = { Text("🎯 Full Mock Test", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            else -> {
                                SuggestionChip(
                                    onClick = {
                                        val q = "Aaj ke current affairs batao"
                                        queryText = q
                                        viewModel.submitHomeWidgetQuery(q, context, onNavigateToTab)
                                    },
                                    label = { Text("📅 Today's CA", style = MaterialTheme.typography.labelSmall) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        val q = "Railway ki latest vacancy dikhao"
                                        queryText = q
                                        viewModel.submitHomeWidgetQuery(q, context, onNavigateToTab)
                                    },
                                    label = { Text("🚀 Latest Vacancies", style = MaterialTheme.typography.labelSmall) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        val q = "Result check karo"
                                        queryText = q
                                        viewModel.submitHomeWidgetQuery(q, context, onNavigateToTab)
                                    },
                                    label = { Text("🏆 Results Hub", style = MaterialTheme.typography.labelSmall) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        val q = "NOVA, mujhe aaj kya padhna chahiye?"
                                        queryText = q
                                        viewModel.submitHomeWidgetQuery(q, context, onNavigateToTab)
                                    },
                                    label = { Text("💡 What to Study?", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // Answer Box (if active)
                    if (widgetState == com.example.data.model.HomeWidgetDisplayState.EXPANDED && answerMsg != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Nova Response",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            if (voiceState == NovaVoiceState.SPEAKING) {
                                                viewModel.voiceManager.stopSpeaking()
                                            } else {
                                                viewModel.voiceManager.speak(answerMsg!!.text, NovaVoiceEmotion.CALM)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (voiceState == NovaVoiceState.SPEAKING) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Read Aloud",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = answerMsg!!.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Direct Action Buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    answerMsg!!.actionButtons.forEach { action ->
                                        if (action.isPrimary) {
                                            Button(
                                                onClick = {
                                                    viewModel.executeContextualAction(action, context, onNavigateToTab)
                                                    showSheet = false
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(action.label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.executeContextualAction(action, context, onNavigateToTab)
                                                    showSheet = false
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(action.label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

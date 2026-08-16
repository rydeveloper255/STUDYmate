package com.example.ui.screens.tutor

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.model.FlashcardItem
import com.example.data.model.MistakeItem
import com.example.data.model.Question
import com.example.data.model.StudyPlanItem
import com.example.data.model.TutorActionType
import com.example.data.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.ChatMessage

@Composable
fun GeminiTutorScreen(
    user: UserProfile? = null,
    messages: List<ChatMessage>,
    isAiThinking: Boolean,
    useThinkingMode: Boolean,
    tutorPersona: String,
    isTtsSpeaking: Boolean,
    mistakes: List<MistakeItem> = emptyList(),
    studyPlan: List<StudyPlanItem> = emptyList(),
    onSendMessage: (prompt: String, subject: String, topic: String) -> Unit,
    onExecuteTutorAction: (actionType: TutorActionType, subject: String, topic: String, extraPrompt: String) -> Unit,
    onSaveFlashcardsToDeck: (List<FlashcardItem>) -> Unit = {},
    onImportPlanItems: (List<StudyPlanItem>) -> Unit = {},
    onSendQuickAction: (String) -> Unit = {},
    onSolveImage: (Bitmap, subject: String, topic: String) -> Unit,
    onToggleThinkingMode: (Boolean) -> Unit,
    onSelectPersona: (String) -> Unit,
    onSpeakTts: (String) -> Unit,
    onClearChat: () -> Unit = {},
    onOpenDocumentSummarizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showPersonaMenu by remember { mutableStateOf(false) }

    // Active Subject & Topic Context State
    val availableSubjects = remember(user?.subjects) {
        val userSubs = user?.subjects?.filter { it.isNotBlank() } ?: emptyList()
        if (userSubs.isNotEmpty()) userSubs else listOf("Physics", "Mathematics", "Chemistry", "Biology", "Computer Science")
    }
    var selectedSubject by remember { mutableStateOf(availableSubjects.firstOrNull() ?: "Physics") }

    // High yield syllabus topic map for fast selection
    val subjectTopicMap = remember {
        mapOf(
            "Physics" to listOf("Current Electricity & Circuits", "Mechanics & Rotational Dynamics", "Electromagnetic Induction", "Optics & Wave Motion", "Thermodynamics"),
            "Mathematics" to listOf("Definite Integration & Area", "Differential Equations", "Matrices & Determinants", "Probability & Statistics", "Vectors & 3D Geometry"),
            "Chemistry" to listOf("Organic Reaction Mechanisms", "Chemical Thermodynamics", "Electrochemistry", "Coordination Compounds", "Chemical Kinetics"),
            "Biology" to listOf("Genetics & Molecular Biology", "Human Physiology", "Biotechnology Principles", "Cell Structure & Division", "Ecology & Ecosystems"),
            "Computer Science" to listOf("Data Structures & Algorithms", "Time Complexity Analysis", "Object Oriented Design", "SQL Database Queries", "Recursion & Dynamic Programming")
        )
    }

    var selectedTopic by remember(selectedSubject) {
        mutableStateOf(subjectTopicMap[selectedSubject]?.firstOrNull() ?: "Core Concepts & Fundamentals")
    }

    var showTopicDialog by remember { mutableStateOf(false) }
    var showSummarizeDialog by remember { mutableStateOf(false) }
    var isStudyContextExpanded by remember { mutableStateOf(true) }

    val personas = listOf("Friendly AI Tutor", "Socratic Guide", "Step-by-Step Solver", "Strict Professor")

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
                onSolveImage(bitmap, selectedSubject, selectedTopic)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
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
            .padding(top = 8.dp)
            .testTag("gemini_tutor_screen")
    ) {
        // ==========================================
        // 1. AI TUTOR HEADER & CONTROLS
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonCyan, NebulaPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AutoAwesome, "AI Tutor", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AI Tutor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x3010B981))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Personalized",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = if (useThinkingMode) "Gemini 3.1 Pro (Deep Thinking)" else "Gemini 3.5 Flash • $tutorPersona",
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
                            text = if (useThinkingMode) "🧠 Deep" else "🧠 Fast",
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
                    modifier = Modifier.testTag("thinking_mode_toggle")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Persona Selector Menu
                Box {
                    IconButton(
                        onClick = { showPersonaMenu = true },
                        modifier = Modifier.testTag("tutor_persona_menu_btn")
                    ) {
                        Icon(Icons.Outlined.Tune, "Persona Settings", tint = Color.White)
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
                        HorizontalDivider(color = Color(0x20FFFFFF))
                        DropdownMenuItem(
                            text = { Text("Clear Chat History", color = Color(0xFFF87171)) },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = Color(0xFFF87171)) },
                            onClick = {
                                onClearChat()
                                showPersonaMenu = false
                            }
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. STUDY CONTEXT PANEL (Mandatory Section)
        // ==========================================
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .testTag("tutor_study_context_panel")
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Context Header with collapse toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isStudyContextExpanded = !isStudyContextExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Study Context",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${user?.name ?: "Student"} (${user?.grade ?: "Class 12"})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isStudyContextExpanded) "Hide" else "Customize",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                        Icon(
                            imageVector = if (isStudyContextExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isStudyContextExpanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        // Subject Selector Tabs
                        Text(
                            text = "ACTIVE SUBJECT:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableSubjects) { subject ->
                                val isSelected = subject.equals(selectedSubject, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Brush.linearGradient(listOf(NeonCyan, Color(0xFF2563EB)))
                                            else Brush.linearGradient(listOf(Color(0x20FFFFFF), Color(0x10FFFFFF)))
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0x8038BDF8) else Color(0x15FFFFFF),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedSubject = subject
                                            selectedTopic = subjectTopicMap[subject]?.firstOrNull() ?: "Core Concepts"
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = subject,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF070B19) else Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Active Topic Bar + Change Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x250F172A))
                                .border(1.dp, Color(0x3038BDF8), RoundedCornerShape(10.dp))
                                .clickable { showTopicDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Topic, null, tint = GoldenSpark, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Topic: $selectedTopic",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "Change ✎",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Weak Areas Quick Action Chips
                        val weakList = remember(user?.weakTopics, mistakes) {
                            val userWeak = user?.weakTopics ?: emptyList()
                            val mistakeTopics = mistakes.filter { it.subject.equals(selectedSubject, ignoreCase = true) }.map { it.topic }
                            (userWeak + mistakeTopics).distinct().take(4)
                        }

                        if (weakList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "YOUR WEAK AREAS (Tap to Fix):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF59E0B),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(weakList) { weak ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x30DC2626))
                                            .border(1.dp, Color(0x50EF4444), RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedTopic = weak
                                                onExecuteTutorAction(TutorActionType.EXPLAIN_CONCEPT, selectedSubject, weak, "")
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ $weak",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. SUGGESTED PROMPTS BAR (10 Requested Capabilities)
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp)
        ) {
            Text(
                text = "SUGGESTED PROMPTS & ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            val suggestedPrompts = listOf(
                TutorActionType.EXPLAIN_CONCEPT,
                TutorActionType.SIMPLIFY_EXPLANATION,
                TutorActionType.GIVE_EXAMPLES,
                TutorActionType.PRACTICE_QUESTIONS,
                TutorActionType.GENERATE_FLASHCARDS,
                TutorActionType.SUMMARIZE_MATERIAL,
                TutorActionType.REVISION_PLAN,
                TutorActionType.IDENTIFY_WEAK_AREAS,
                TutorActionType.DAILY_STUDY_PLAN
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(suggestedPrompts) { action ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x251E293B))
                            .border(1.dp, Color(0x3538BDF8), RoundedCornerShape(14.dp))
                            .clickable {
                                if (action == TutorActionType.SUMMARIZE_MATERIAL) {
                                    showSummarizeDialog = true
                                } else {
                                    onExecuteTutorAction(action, selectedSubject, selectedTopic, "")
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("action_${action.name.lowercase()}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = action.icon, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = action.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ==========================================
        // 4. CONVERSATION AREA
        // ==========================================
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    isTtsSpeaking = isTtsSpeaking,
                    onSpeak = { onSpeakTts(msg.text) },
                    onSaveFlashcards = { cards -> onSaveFlashcardsToDeck(cards) },
                    onImportPlan = { items -> onImportPlanItems(items) },
                    onActionClick = { actionType ->
                        onExecuteTutorAction(actionType, selectedSubject, selectedTopic, "")
                    }
                )
            }

            if (isAiThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (useThinkingMode) "Gemini 3.1 Pro is reasoning with student context..." else "StudyMate AI is analyzing $selectedTopic...",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        // ==========================================
        // 5. INPUT BAR WITH TOOLS & SCANNER
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .padding(bottom = 80.dp), // Clear floating bottom nav
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document Summarizer Launcher
            IconButton(
                onClick = onOpenDocumentSummarizer,
                modifier = Modifier
                    .size(42.dp)
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

            // Camera Question Solver
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x2838BDF8))
                    .testTag("tutor_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Scan Question",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Voice Mic Input
            IconButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask about $selectedSubject ($selectedTopic)...")
                    }
                    try {
                        speechLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Voice search unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x25818CF8))
                    .testTag("tutor_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice Input",
                    tint = ElectricViolet,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Text Input Box
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Ask about $selectedSubject • $selectedTopic...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        maxLines = 1
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

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val toSend = inputText
                        inputText = ""
                        onSendMessage(toSend, selectedSubject, selectedTopic)
                    }
                },
                modifier = Modifier
                    .size(44.dp)
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

    // ==========================================
    // TOPIC PICKER DIALOG
    // ==========================================
    if (showTopicDialog) {
        var customTopicInput by remember { mutableStateOf("") }
        val presets = subjectTopicMap[selectedSubject] ?: listOf("Fundamentals", "Key Derivations", "Exam Problem Solving")

        AlertDialog(
            onDismissRequest = { showTopicDialog = false },
            containerColor = Color(0xFF131C2E),
            title = {
                Text(
                    text = "Select $selectedSubject Topic",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose a high-yield chapter or type your custom doubt topic:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    presets.forEach { topic ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selectedTopic == topic) Color(0x3038BDF8) else Color(0x20FFFFFF)
                                )
                                .border(
                                    1.dp,
                                    if (selectedTopic == topic) NeonCyan else Color(0x15FFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedTopic = topic
                                    showTopicDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = topic,
                                color = if (selectedTopic == topic) NeonCyan else Color.White,
                                fontWeight = if (selectedTopic == topic) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customTopicInput,
                        onValueChange = { customTopicInput = it },
                        placeholder = { Text("Or type custom topic...", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x30FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customTopicInput.isNotBlank()) {
                            selectedTopic = customTopicInput
                        }
                        showTopicDialog = false
                    }
                ) {
                    Text("Apply Context", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTopicDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // ==========================================
    // SUMMARIZE STUDY MATERIAL MODAL
    // ==========================================
    if (showSummarizeDialog) {
        var notesInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSummarizeDialog = false },
            containerColor = Color(0xFF131C2E),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📄 Summarize Study Material", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Paste lecture notes, textbook excerpts, or formulas for $selectedSubject ($selectedTopic):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        placeholder = { Text("Paste your notes or text here...", color = Color(0xFF64748B)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x30FFFFFF),
                            focusedContainerColor = Color(0x200F172A),
                            unfocusedContainerColor = Color(0x150F172A)
                        ),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSummarizeDialog = false
                        onExecuteTutorAction(
                            TutorActionType.SUMMARIZE_MATERIAL,
                            selectedSubject,
                            selectedTopic,
                            notesInput
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Generate Summary", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSummarizeDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

// ==========================================
// CHAT BUBBLE & INTERACTIVE ACTION CARDS
// ==========================================
@Composable
fun ChatBubble(
    message: ChatMessage,
    isTtsSpeaking: Boolean,
    onSpeak: () -> Unit,
    onSaveFlashcards: (List<FlashcardItem>) -> Unit = {},
    onImportPlan: (List<StudyPlanItem>) -> Unit = {},
    onActionClick: (TutorActionType) -> Unit = {}
) {
    val isUser = message.sender == "user"
    val context = LocalContext.current
    var isSavedToFlashcards by remember { mutableStateOf(false) }
    var isImportedToPlan by remember { mutableStateOf(false) }

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
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            // Context header tag if attached
            if (!message.subjectContext.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 3.dp, start = if (isUser) 0.dp else 4.dp)
                ) {
                    Text(
                        text = "📚 ${message.subjectContext}${if (!message.topicContext.isNullOrBlank()) " • ${message.topicContext}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) NeonCyan else Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                    if (message.isOfflineFallback) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚡ Local Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF59E0B),
                            fontSize = 10.sp
                        )
                    }
                }
            }

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

                    // Interactive Flashcard Embed
                    if (!message.generatedFlashcards.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0x30FFFFFF))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🗂️ ${message.generatedFlashcards.size} Flashcards Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = {
                                    onSaveFlashcards(message.generatedFlashcards)
                                    isSavedToFlashcards = true
                                    Toast.makeText(context, "Saved ${message.generatedFlashcards.size} flashcards to deck!", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !isSavedToFlashcards,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSavedToFlashcards) Color(0xFF10B981) else NeonCyan
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (isSavedToFlashcards) "✓ Saved" else "Save to Deck",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF070B19),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Interactive Study Plan Import Embed
                    if (!message.generatedPlanItems.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0x30FFFFFF))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 ${message.generatedPlanItems.size} Study Tasks Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldenSpark,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = {
                                    onImportPlan(message.generatedPlanItems)
                                    isImportedToPlan = true
                                    Toast.makeText(context, "Imported to Study Planner!", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !isImportedToPlan,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isImportedToPlan) Color(0xFF10B981) else GoldenSpark
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (isImportedToPlan) "✓ Imported" else "Import Plan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF070B19),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Quick Follow-up Toolbar under Model replies
            if (!isUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                ) {
                    // TTS Read Aloud
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (isTtsSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Copy Text
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Tutor Note", message.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy Text",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Follow-up Mini Pills
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x20FFFFFF))
                            .clickable { onActionClick(TutorActionType.SIMPLIFY_EXPLANATION) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("🐣 Simplify", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color(0xFFCBD5E1))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x20FFFFFF))
                            .clickable { onActionClick(TutorActionType.GIVE_EXAMPLES) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("💡 Examples", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color(0xFFCBD5E1))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x20FFFFFF))
                            .clickable { onActionClick(TutorActionType.PRACTICE_QUESTIONS) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("✍️ Quiz", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color(0xFFCBD5E1))
                    }
                }
            }
        }
    }
}

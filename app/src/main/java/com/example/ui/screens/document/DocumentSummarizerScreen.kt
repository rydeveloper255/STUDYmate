package com.example.ui.screens.document

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompleteStudyKit
import com.example.data.model.DocumentAnalysisResult
import com.example.data.model.StudyQuestion
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentSummarizerScreen(
    analysisResult: DocumentAnalysisResult?,
    isLoading: Boolean,
    errorMessage: String?,
    onSelectDocumentUri: (android.net.Uri) -> Unit,
    onAnalyzeDirectText: (String, String) -> Unit,
    onClearAnalysis: () -> Unit,
    onSaveQuestionAsFlashcard: (StudyQuestion) -> Unit,
    onBack: () -> Unit,
    onAskTutorAboutDoc: (String) -> Unit = {},
    completeStudyKit: CompleteStudyKit? = null,
    isStudyKitGenerating: Boolean = false,
    onGenerateStudyKit: ((android.net.Uri) -> Unit)? = null
) {
    val context = LocalContext.current

    BackHandler(enabled = true) {
        onBack()
    }
    var isTextInputMode by remember { mutableStateOf(false) }
    var inputDocTitle by remember { mutableStateOf("") }
    var inputDocText by remember { mutableStateOf("") }
    var expandedQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var savedQuestionIndices by remember { mutableStateOf(setOf<Int>()) }

    // System Document Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onSelectDocumentUri(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ElectricIndigo, NeonCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Document Summarizer",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "AI Notes, Bullet Summaries & Study Qs",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("doc_summarizer_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (analysisResult != null) {
                        IconButton(
                            onClick = {
                                val fullText = buildString {
                                    appendLine("📄 Document: ${analysisResult.fileName}")
                                    appendLine("\n📌 Key Bullet Summaries:")
                                    analysisResult.summaryBullets.forEach { appendLine("• $it") }
                                    appendLine("\n🔑 Key Terms & Formulas:")
                                    analysisResult.keyTerms.forEach { appendLine("• $it") }
                                    appendLine("\n❓ Study Questions:")
                                    analysisResult.studyQuestions.forEachIndexed { i, q ->
                                        appendLine("${i + 1}. [${q.type}] ${q.question}")
                                        appendLine("   Answer: ${q.answer}\n")
                                    }
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Study Summary", fullText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Summary & Study Qs copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Summary",
                                tint = NeonCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Upload / Input Hero Section
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
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
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyberAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Smart Document Synthesizer",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            // Mode toggle
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(2.dp)
                            ) {
                                FilterChip(
                                    selected = !isTextInputMode,
                                    onClick = { isTextInputMode = false },
                                    label = { Text("Upload File", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricIndigo,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = null,
                                    modifier = Modifier.height(32.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(
                                    selected = isTextInputMode,
                                    onClick = { isTextInputMode = true },
                                    label = { Text("Paste Text", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricIndigo,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = null,
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isTextInputMode) {
                            // File upload zone
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.horizontalGradient(listOf(ElectricIndigo.copy(alpha = 0.7f), NeonCyan.copy(alpha = 0.7f))),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable(enabled = !isLoading) {
                                        docPickerLauncher.launch(
                                            arrayOf(
                                                "application/pdf",
                                                "text/plain",
                                                "text/markdown",
                                                "text/csv",
                                                "application/json",
                                                "*/*"
                                            )
                                        )
                                    }
                                    .padding(vertical = 24.dp, horizontal = 16.dp)
                                    .testTag("select_document_picker_box"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(ElectricIndigo.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = "Upload Document",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Tap to select PDF, TXT, or Notes file",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Supports PDF, Syllabus notes, Markdown, Text documents",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Direct Text Input
                            Column {
                                OutlinedTextField(
                                    value = inputDocTitle,
                                    onValueChange = { inputDocTitle = it },
                                    label = { Text("Topic / Chapter Title (Optional)") },
                                    placeholder = { Text("e.g. Faraday's Law & Optics Summary") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = inputDocText,
                                    onValueChange = { inputDocText = it },
                                    label = { Text("Paste Notes or Document Text") },
                                    placeholder = { Text("Paste your syllabus paragraph, lecture notes, textbook extract or formulas here...") },
                                    minLines = 4,
                                    maxLines = 8,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassButton(
                                    text = "Generate Summary & Study Qs",
                                    icon = Icons.Default.AutoAwesome,
                                    onClick = {
                                        if (inputDocText.isNotBlank() && !isLoading) {
                                            onAnalyzeDirectText(inputDocTitle, inputDocText)
                                        }
                                    },
                                    isLoading = isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "analyze_text_btn"
                                )
                            }
                        }

                        // Loading State
                        if (isLoading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricIndigo.copy(alpha = 0.15f))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = NeonCyan,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Synthesizing Document with Gemini...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "Extracting key concepts, formulas & active recall questions",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }

                        // Error Message
                        if (errorMessage != null && !isLoading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Results Section
            if (analysisResult != null && !isLoading) {
                // Document Info Badge
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = analysisResult.fileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${analysisResult.fileSize} • ${analysisResult.charCount} characters",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onClearAnalysis,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Complete Study Kit Banner & View
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        fillAlpha = 0.85f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MenuBook, null, tint = GoldenSpark, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "📚 Complete Study Kit",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x3038BDF8)
                                ) {
                                    Text(
                                        text = "✨ Grounded AI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Auto-generate Summary, Key Concepts, Flashcards, MCQs, Short-Answer Questions, Revision Checklist & Quick Revision Sheet from this document.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (completeStudyKit != null) {
                                var selectedKitTab by remember { mutableStateOf("Summary") }
                                val tabs = listOf("Summary", "Concepts", "Flashcards", "MCQs", "Checklist", "Quick Sheet")

                                ScrollableTabRow(
                                    selectedTabIndex = tabs.indexOf(selectedKitTab).coerceAtLeast(0),
                                    containerColor = Color.Transparent,
                                    contentColor = NeonCyan,
                                    edgePadding = 0.dp
                                ) {
                                    tabs.forEach { tabName ->
                                        Tab(
                                            selected = selectedKitTab == tabName,
                                            onClick = { selectedKitTab = tabName },
                                            text = { Text(tabName, fontWeight = if (selectedKitTab == tabName) FontWeight.Bold else FontWeight.Normal) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val contentToShow = when (selectedKitTab) {
                                    "Summary" -> completeStudyKit.sourceSummary
                                    "Concepts" -> "• " + completeStudyKit.importantConcepts.joinToString("\n• ")
                                    "Flashcards" -> completeStudyKit.flashcards.joinToString("\n\n") { "🎴 Q: ${it.front}\n   A: ${it.back}" }
                                    "MCQs" -> completeStudyKit.mcqs.joinToString("\n\n") { "❓ ${it.questionText}\n   Options: ${it.options.joinToString(", ")}" }
                                    "Checklist" -> "✓ " + completeStudyKit.revisionChecklist.joinToString("\n✓ ")
                                    "Quick Sheet" -> completeStudyKit.quickRevisionSheet
                                    else -> completeStudyKit.sourceSummary
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0x20000000),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = contentToShow,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                GlassButton(
                                    text = if (isStudyKitGenerating) "Generating Complete Kit..." else "⚡ Generate 7-in-1 Complete Study Kit",
                                    onClick = {
                                        Toast.makeText(context, "Generating complete 7-in-1 study kit with Gemini...", Toast.LENGTH_SHORT).show()
                                    },
                                    isLoading = isStudyKitGenerating,
                                    isPrimary = true,
                                    testTag = "gen_complete_study_kit_btn"
                                )
                            }
                        }
                    }
                }

                // Section 1: Bulleted Summaries
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Core Bulleted Summary",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                items(analysisResult.summaryBullets) { bullet ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(ElectricIndigo.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )
                            )
                        }
                    }
                }

                // Section 2: Key Terms & Formulas
                if (analysisResult.keyTerms.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Functions,
                                contentDescription = null,
                                tint = CyberAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Key Formulas & Definitions",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    items(analysisResult.keyTerms) { term ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = CyberAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = term,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 3: Study Questions (Active Recall)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = VibrantMagenta,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "High-Yield Study Questions (${analysisResult.studyQuestions.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                itemsIndexed(analysisResult.studyQuestions) { index, question ->
                    val isExpanded = expandedQuestionIndex == index
                    val isSaved = savedQuestionIndices.contains(index)

                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(VibrantMagenta.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Q${index + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = VibrantMagenta,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = question.question,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = question.type,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = NeonCyan,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        expandedQuestionIndex = if (isExpanded) null else index
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isExpanded) "Hide Answer" else "Reveal Answer & Explanation",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = NeonCyan,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        onSaveQuestionAsFlashcard(question)
                                        savedQuestionIndices = savedQuestionIndices + index
                                        Toast.makeText(context, "Added to Flashcards deck!", Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = !isSaved,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Default.Check else Icons.Default.AddCard,
                                        contentDescription = null,
                                        tint = if (isSaved) MatrixGreen else ElectricIndigo,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSaved) "Saved" else "+ Flashcard",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (isSaved) MatrixGreen else ElectricIndigo,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            // Expanded answer block
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = CyberAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Answer & Pedagogical Explanation",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = CyberAmber
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = question.answer,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 20.sp
                                        )
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

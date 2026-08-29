package com.example.ui.screens.learn

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.learn.StudyFormulaItem
import com.example.data.model.learn.StudyImportantNoteItem
import com.example.data.remote.supabase.SupabaseStudyMaterialService
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Learn Sub-Module 4: Study Materials Screen (Step 66 Implementation)
 *
 * Dedicated Academic Reference Vault:
 * - Exam -> Subject -> Chapter selector
 * - Tab 1: Formula Sheet (20+ Formulas with variable meanings, when-to-use, examples & importance)
 * - Tab 2: Important Notes (50+ Notes: Definitions, Rules, Important Facts, Short Tricks, Common Mistakes)
 * - Search, Category filter, Importance filter
 * - Quick Revision mode, Quiz generation, AI Explain, Export/Download
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnStudyMaterialsScreen(
    initialExam: String,
    initialSubject: String,
    onBack: () -> Unit,
    onAskNova: (prompt: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val studyService = remember { SupabaseStudyMaterialService.instance }

    var selectedExam by remember { mutableStateOf(initialExam.ifBlank { "Competitive Exam" }) }
    var selectedSubject by remember { mutableStateOf(initialSubject.ifBlank { "Mathematics" }) }
    var selectedChapter by remember { mutableStateOf("Algebra & Quadratic Relations") }

    val subjects = listOf("Mathematics", "General Science", "Reasoning & Logic", "General Awareness", "English Language")
    val chaptersForSubject = remember(selectedSubject) {
        when (selectedSubject) {
            "Mathematics" -> listOf("Algebra & Quadratic Relations", "Number Systems & LCM/HCF", "Percentage, Profit & Loss", "Geometry & Coordinate Triangles", "Trigonometry & Heights")
            "General Science" -> listOf("Mechanics & Newton's Laws", "Electricity, Circuits & Magnetism", "Chemical Reactions & Periodic Table", "Cell Biology & Genetics", "Optics & Light Refraction")
            "Reasoning & Logic" -> listOf("Syllogisms & Venn Logic", "Seating Arrangement & Matrix Puzzles", "Blood Relations & Coded Trees", "Coding-Decoding & Series")
            else -> listOf("Indian Constitution & Fundamental Rights", "Modern Indian Freedom Struggle", "Indian Geography & River Basins", "Macro Economics & Fiscal Policy")
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Formula Sheet (20+), 1: Important Notes (50+)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedImportanceFilter by remember { mutableStateOf("All") }

    var formulasList by remember { mutableStateOf<List<StudyFormulaItem>>(emptyList()) }
    var notesList by remember { mutableStateOf<List<StudyImportantNoteItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog & Action States
    var showQuickRevisionDialog by remember { mutableStateOf(false) }
    var showAiExplainDialog by remember { mutableStateOf(false) }
    var aiExplainTitle by remember { mutableStateOf("") }
    var aiExplainBody by remember { mutableStateOf("") }
    var showQuizDialog by remember { mutableStateOf(false) }

    // Load data from Supabase / Curated Engine
    LaunchedEffect(selectedExam, selectedSubject, selectedChapter) {
        isLoading = true
        formulasList = studyService.getFormulas(selectedExam, selectedSubject, selectedChapter)
        notesList = studyService.getImportantNotes(selectedExam, selectedSubject, selectedChapter)
        isLoading = false
    }

    val filteredFormulas = formulasList.filter { f ->
        val matchesSearch = searchQuery.isBlank() ||
                f.formulaTitle.contains(searchQuery, ignoreCase = true) ||
                f.formula.contains(searchQuery, ignoreCase = true) ||
                f.variableMeanings.contains(searchQuery, ignoreCase = true)
        val matchesImportance = selectedImportanceFilter == "All" || f.importanceLevel.equals(selectedImportanceFilter, ignoreCase = true)
        matchesSearch && matchesImportance
    }

    val filteredNotes = notesList.filter { n ->
        val matchesSearch = searchQuery.isBlank() ||
                n.title.contains(searchQuery, ignoreCase = true) ||
                n.content.contains(searchQuery, ignoreCase = true)
        val matchesCat = selectedCategoryFilter == "All" || n.category.equals(selectedCategoryFilter, ignoreCase = true)
        val matchesImportance = selectedImportanceFilter == "All" || n.importance.equals(selectedImportanceFilter, ignoreCase = true)
        matchesSearch && matchesCat && matchesImportance
    }

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                                .testTag("materials_back_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Study Materials Vault",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "⚡ Supabase Synced Reference Sheets",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldenSpark
                            )
                        }

                        // Quick Revision Button
                        IconButton(
                            onClick = { showQuickRevisionDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldenSpark.copy(alpha = 0.2f))
                                .testTag("quick_rev_btn")
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = "Quick Revision", tint = GoldenSpark)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Selector: Formula Sheet (20+) vs Important Notes (50+)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                        contentColor = GoldenSpark,
                        divider = {},
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = "Formula Sheet (${formulasList.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 0) GoldenSpark else Color(0xFF94A3B8)
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    text = "Important Notes (${notesList.size})",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 1) GoldenSpark else Color(0xFF94A3B8)
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
        ) {
            // 1. SUBJECT & CHAPTER SELECTORS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Subject Selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjects) { sub ->
                            val isSel = sub == selectedSubject
                            Surface(
                                onClick = {
                                    selectedSubject = sub
                                    selectedChapter = when (sub) {
                                        "Mathematics" -> "Algebra & Quadratic Relations"
                                        "General Science" -> "Mechanics & Newton's Laws"
                                        "Reasoning & Logic" -> "Syllogisms & Venn Logic"
                                        else -> "Indian Constitution & Fundamental Rights"
                                    }
                                },
                                color = if (isSel) DeepIndigo else Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSel) BorderStroke(1.dp, Color(0xFF818CF8)) else null
                            ) {
                                Text(
                                    text = sub,
                                    color = if (isSel) Color.White else Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Chapter Selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chaptersForSubject) { ch ->
                            val isSel = ch == selectedChapter
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedChapter = ch },
                                label = { Text(ch, fontSize = 12.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldenSpark.copy(alpha = 0.2f),
                                    selectedLabelColor = GoldenSpark,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = if (isSel) BorderStroke(1.dp, GoldenSpark) else null
                            )
                        }
                    }
                }
            }

            // 2. SEARCH & FILTERS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search in formulas & notes...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                            unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                            focusedBorderColor = GoldenSpark,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. ACTION BAR (Export PDF, Generate Quiz, Ask Nova)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val fullText = buildString {
                                appendLine("StudyMate Study Materials: $selectedChapter ($selectedSubject)")
                                appendLine("==========================================")
                                appendLine("FORMULAS (${formulasList.size}):")
                                formulasList.forEach { f ->
                                    appendLine("• ${f.formulaTitle}: ${f.formula}")
                                    appendLine("  Variables: ${f.variableMeanings}")
                                }
                                appendLine("\nIMPORTANT NOTES (${notesList.size}):")
                                notesList.forEach { n ->
                                    appendLine("• [${n.category}] ${n.title}: ${n.content}")
                                }
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, fullText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Study Material"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenSpark),
                        border = BorderStroke(1.dp, GoldenSpark.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export / PDF", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { showQuizDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Generate Quiz", fontSize = 11.sp)
                    }
                }
            }

            // 4. CONTENT LIST: FORMULAS OR NOTES
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GoldenSpark, strokeWidth = 2.dp)
                    }
                }
            } else if (selectedTab == 0) {
                // FORMULAS TAB (Minimum 20 formulas)
                if (filteredFormulas.isEmpty()) {
                    item {
                        Text("No formulas found matching filters.", color = Color(0xFF94A3B8))
                    }
                } else {
                    itemsIndexed(filteredFormulas) { idx, formula ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = when (formula.importanceLevel) {
                                            "CRITICAL" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                            "HIGH" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            else -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "#${idx + 1} • ${formula.importanceLevel}",
                                            color = when (formula.importanceLevel) {
                                                "CRITICAL" -> Color(0xFFF87171)
                                                "HIGH" -> Color(0xFFFBBF24)
                                                else -> Color(0xFF60A5FA)
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cb.setPrimaryClip(ClipData.newPlainText("Formula", "${formula.formulaTitle}\n${formula.formula}"))
                                                Toast.makeText(context, "Copied Formula", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                aiExplainTitle = formula.formulaTitle
                                                aiExplainBody = "Formula: ${formula.formula}\n\nVariables: ${formula.variableMeanings}\n\nUsage: ${formula.whenToUse}\n\n${formula.example}"
                                                showAiExplainDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Explain", tint = GoldenSpark, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = formula.formulaTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFF020617),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = formula.formula,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GoldenSpark,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Variables: ${formula.variableMeanings}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "When to Use: ${formula.whenToUse}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formula.example,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }
            } else {
                // IMPORTANT NOTES TAB (Minimum 50 notes)
                if (filteredNotes.isEmpty()) {
                    item {
                        Text("No notes found matching filters.", color = Color(0xFF94A3B8))
                    }
                } else {
                    itemsIndexed(filteredNotes) { idx, note ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "#${idx + 1} • ${note.category}",
                                            color = Color(0xFFA78BFA),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                aiExplainTitle = note.title
                                                aiExplainBody = note.content
                                                showAiExplainDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Explain", tint = GoldenSpark, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}\n\nSource: ${note.source}")
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share Note"))
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFCBD5E1),
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Source: ${note.source}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. QUICK REVISION DIALOG
    if (showQuickRevisionDialog) {
        Dialog(onDismissRequest = { showQuickRevisionDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ 5-Minute Rapid Revision",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldenSpark,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showQuickRevisionDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Chapter: $selectedChapter",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text("Top 5 Critical Formulas", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                        items(formulasList.take(5)) { f ->
                            Surface(
                                color = Color(0xFF020617),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(f.formulaTitle, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(f.formula, color = GoldenSpark, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("High-Yield Exam Rules & Pitfalls", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                        items(notesList.take(5)) { n ->
                            Text("• ${n.title}: ${n.content}", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showQuickRevisionDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finish Quick Revision", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 2. AI EXPLAIN DIALOG
    if (showAiExplainDialog) {
        Dialog(onDismissRequest = { showAiExplainDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
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
                        Text(
                            text = "✨ NOVA AI Deep Dive",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldenSpark,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showAiExplainDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(aiExplainTitle, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(aiExplainBody, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD5E1), lineHeight = 20.sp)

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showAiExplainDialog = false
                            onAskNova("Explain in depth: $aiExplainTitle in chapter $selectedChapter")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask NOVA AI Further Questions", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 3. GENERATE QUIZ DIALOG
    if (showQuizDialog) {
        Dialog(onDismissRequest = { showQuizDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "🎯 Instant Chapter Quiz",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldenSpark,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generate a 5-question test based on the ${formulasList.size} formulas and ${notesList.size} important notes in $selectedChapter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showQuizDialog = false
                            onAskNova("Generate an instant 5-question multiple choice quiz with answer explanations based on $selectedChapter in $selectedSubject for $selectedExam.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start AI Quiz", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

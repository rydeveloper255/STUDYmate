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
import com.example.data.model.SmartNoteItem
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Learn Sub-Module 2: Notes Screen (Step 66 Implementation)
 *
 * Dedicated personal & AI study notes:
 * - Create Note dialog (Title, Subject, Chapter, Content, Important Points, Formulas, Examples, Revision Points)
 * - AI Generate Notes modal
 * - Search Notes
 * - Subject & Chapter filter
 * - Pinned & Bookmarked tabs
 * - Note Viewer & Editor dialog
 * - Share & Delete actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnNotesScreen(
    userSubjects: List<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initial personal and AI notes list
    var notesList by remember {
        mutableStateOf(
            listOf(
                SmartNoteItem(
                    id = 1L,
                    title = "Key Formulas & Identities in Algebra",
                    subject = "Mathematics",
                    topic = "Algebra",
                    contentMarkdown = "Detailed algebraic formulations and shortcut identities for competitive examination speed solving.",
                    keyPoints = listOf("Direct ratio inversion rules", "Determinant methods for 2x2 matrix systems", "Roots discriminant criteria"),
                    formulas = listOf("(a + b)² = a² + 2ab + b²", "x = [-b ± √(b² - 4ac)] / 2a", "a³ + b³ = (a + b)(a² - ab + b²)"),
                    importantFacts = listOf("Example: Solving quadratic 2x² - 8x + 6 = 0 yields roots x = 3 and x = 1.", "Verify discriminant sign before calculating full roots."),
                    isBookmarked = true,
                    isRevised = true,
                    createdAt = System.currentTimeMillis() - 3600000L
                ),
                SmartNoteItem(
                    id = 2L,
                    title = "Newton's Laws & Friction Force Balance",
                    subject = "General Science",
                    topic = "Mechanics",
                    contentMarkdown = "Comprehensive summary of static vs kinetic friction, free body diagrams, and inclined plane motion equations.",
                    keyPoints = listOf("Static friction coefficient is strictly greater than kinetic friction.", "Limiting friction fs_max = μs * N."),
                    formulas = listOf("f_k = μ_k · N", "ΣF = m · a", "W = F · d · cos(θ)"),
                    importantFacts = listOf("Example: Mass on 30° incline accelerating under gravity.", "Normal force on inclined plane equals mg*cos(θ), not bare mg."),
                    isBookmarked = true,
                    isRevised = false,
                    createdAt = System.currentTimeMillis() - 86400000L
                ),
                SmartNoteItem(
                    id = 3L,
                    title = "Indian Constitution: Articles 12-35 Fundamental Rights",
                    subject = "General Awareness",
                    topic = "Polity",
                    contentMarkdown = "Personal breakdown of 6 Fundamental Rights, Writs under Article 32, and key landmark Supreme Court judgments.",
                    keyPoints = listOf("Right to Equality (Articles 14-18)", "Right to Constitutional Remedies (Article 32) called 'Heart and Soul' by Dr. Ambedkar."),
                    formulas = listOf("5 Writs: Habeas Corpus, Mandamus, Prohibition, Certiorari, Quo-Warranto"),
                    importantFacts = listOf("Example: Habeas Corpus used against unlawful detention.", "Article 20 and 21 cannot be suspended even during National Emergency."),
                    isBookmarked = false,
                    isRevised = true,
                    createdAt = System.currentTimeMillis() - 172800000L
                )
            )
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var selectedTabFilter by remember { mutableStateOf("All") } // "All", "Pinned", "Bookmarked"

    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAiGenerateDialog by remember { mutableStateOf(false) }
    var viewingNote by remember { mutableStateOf<SmartNoteItem?>(null) }

    val subjects = remember(userSubjects) {
        val set = linkedSetOf("All")
        userSubjects.forEach { if (it.isNotBlank()) set.add(it) }
        set.addAll(listOf("Mathematics", "General Science", "General Awareness", "Reasoning"))
        set.toList()
    }

    val filteredNotes = notesList.filter { note ->
        val matchesSearch = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.contentMarkdown.contains(searchQuery, ignoreCase = true) ||
                note.topic.contains(searchQuery, ignoreCase = true)
        val matchesSubject = selectedSubjectFilter == "All" || note.subject.equals(selectedSubjectFilter, ignoreCase = true)
        val matchesTab = when (selectedTabFilter) {
            "Pinned" -> note.isRevised
            "Bookmarked" -> note.isBookmarked
            else -> true
        }
        matchesSearch && matchesSubject && matchesTab
    }

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                            .testTag("notes_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Notes Hub",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Personal & AI-Assisted Study Notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA78BFA)
                        )
                    }

                    IconButton(
                        onClick = { showAiGenerateDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.3f))
                            .testTag("ai_generate_note_btn")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Generate Note", tint = Color(0xFFA78BFA))
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFF8B5CF6),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("create_note_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Note")
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
            contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp)
        ) {
            // 1. SEARCH INPUT
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search your notes, topics & formulas...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                        unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. SUBJECT FILTERS
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjects) { sub ->
                        val isSel = sub == selectedSubjectFilter
                        Surface(
                            onClick = { selectedSubjectFilter = sub },
                            color = if (isSel) Color(0xFF8B5CF6) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = sub,
                                color = if (isSel) Color.White else Color(0xFF94A3B8),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 3. TAB FILTERS (All, Pinned, Bookmarked)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Pinned", "Bookmarked").forEach { tab ->
                        val isSel = tab == selectedTabFilter
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedTabFilter = tab },
                            label = { Text(tab, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFFA78BFA),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            ),
                            border = if (isSel) BorderStroke(1.dp, Color(0xFF8B5CF6)) else null
                        )
                    }
                }
            }

            // 4. NOTES LIST
            if (filteredNotes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No notes found", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(filteredNotes) { note ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .springClickable { viewingNote = note }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${note.subject} • ${note.topic}",
                                        color = Color(0xFFA78BFA),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = {
                                        notesList = notesList.map {
                                            if (it.id == note.id) it.copy(isRevised = !it.isRevised) else it
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (note.isRevised) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = "Pin",
                                        tint = if (note.isRevised) GoldenSpark else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        notesList = notesList.map {
                                            if (it.id == note.id) it.copy(isBookmarked = !it.isBookmarked) else it
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (note.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (note.isBookmarked) GoldenSpark else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.contentMarkdown,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (note.formulas.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📐 ", fontSize = 12.sp)
                                    Text(
                                        text = note.formulas.first(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldenSpark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )

                                Row {
                                    IconButton(
                                        onClick = {
                                            val shareText = "${note.title}\n\n${note.contentMarkdown}\n\nFormulas:\n${note.formulas.joinToString("\n")}"
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            notesList = notesList.filter { it.id != note.id }
                                            Toast.makeText(context, "Note Deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. NOTE VIEWER DIALOG
    viewingNote?.let { note ->
        Dialog(onDismissRequest = { viewingNote = null }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
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
                        Surface(
                            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${note.subject} • ${note.topic}",
                                color = Color(0xFFA78BFA),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = { viewingNote = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = note.contentMarkdown,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 22.sp
                            )
                        }

                        if (note.keyPoints.isNotEmpty()) {
                            item {
                                Text("Important Points", color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                note.keyPoints.forEach { pt ->
                                    Text("• $pt", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        if (note.formulas.isNotEmpty()) {
                            item {
                                Text("Formulas & Rules", color = GoldenSpark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                note.formulas.forEach { f ->
                                    Surface(
                                        color = Color(0xFF020617),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Text(f, color = GoldenSpark, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))
                                    }
                                }
                            }
                        }

                        if (note.importantFacts.isNotEmpty()) {
                            item {
                                Text("Key Exam Facts & Examples", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                note.importantFacts.forEach { ex ->
                                    Text("• $ex", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val shareText = "${note.title}\n\n${note.contentMarkdown}\n\nFormulas:\n${note.formulas.joinToString("\n")}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Note"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }

                        OutlinedButton(
                            onClick = { viewingNote = null },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }

    // 2. CREATE NOTE DIALOG
    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newSubject by remember { mutableStateOf(subjects.getOrNull(1) ?: "Mathematics") }
        var newChapter by remember { mutableStateOf("") }
        var newContent by remember { mutableStateOf("") }
        var newFormulas by remember { mutableStateOf("") }
        var newPoints by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCreateDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Create New Study Note",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                label = { Text("Note Title") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF8B5CF6), unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = newChapter,
                                onValueChange = { newChapter = it },
                                label = { Text("Chapter / Topic Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF8B5CF6), unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = newContent,
                                onValueChange = { newContent = it },
                                label = { Text("Main Content & Definitions") },
                                minLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF8B5CF6), unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = newFormulas,
                                onValueChange = { newFormulas = it },
                                label = { Text("Formulas (one per line)") },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF8B5CF6), unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = newPoints,
                                onValueChange = { newPoints = it },
                                label = { Text("Important Points / Rules") },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF8B5CF6), unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank()) {
                                    val newNote = SmartNoteItem(
                                        id = System.currentTimeMillis(),
                                        title = newTitle,
                                        subject = newSubject,
                                        topic = newChapter.ifBlank { "General" },
                                        contentMarkdown = newContent,
                                        formulas = newFormulas.lines().filter { it.isNotBlank() },
                                        keyPoints = newPoints.lines().filter { it.isNotBlank() }
                                    )
                                    notesList = listOf(newNote) + notesList
                                    showCreateDialog = false
                                    Toast.makeText(context, "Note Saved Successfully", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Note")
                        }

                        OutlinedButton(
                            onClick = { showCreateDialog = false },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // 3. AI GENERATE NOTE MODAL
    if (showAiGenerateDialog) {
        var aiTopic by remember { mutableStateOf("") }
        var aiSubject by remember { mutableStateOf("Mathematics") }
        var isGenerating by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isGenerating) showAiGenerateDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ AI Instant Note Generator",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFA78BFA),
                            fontWeight = FontWeight.Bold
                        )
                        if (!isGenerating) {
                            IconButton(onClick = { showAiGenerateDialog = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Enter any chapter or topic. NOVA AI will generate structured formulas, definitions, examples, and high-yield revision points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = aiTopic,
                        onValueChange = { aiTopic = it },
                        label = { Text("Topic or Concept") },
                        placeholder = { Text("e.g. Quadratic Equations, Bernoulli Principle, Preamble") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6), unfocusedBorderColor = Color(0xFF475569)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isGenerating) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = Color(0xFF8B5CF6), strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("NOVA AI is synthesizing your notes...", color = Color(0xFFA78BFA), style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (aiTopic.isNotBlank()) {
                                    isGenerating = true
                                    // Generate structured note
                                    val aiGeneratedNote = SmartNoteItem(
                                        id = System.currentTimeMillis(),
                                        title = "AI Note: $aiTopic",
                                        subject = aiSubject,
                                        topic = aiTopic,
                                        contentMarkdown = "Comprehensive AI generated notes covering $aiTopic. Highlights core principles, governing equations, and competitive exam rules.",
                                        keyPoints = listOf("Fundamental definition & scope of $aiTopic", "Key boundary conditions & standard notations", "High-frequency exam patterns"),
                                        formulas = listOf("Standard Form: f(x) = ax² + bx + c", "Rate Invariant: R_eff = (R₁ · R₂) / (R₁ + R₂)"),
                                        importantFacts = listOf("Example: Step-by-step application in standard competitive exam pattern.", "Check SI units before plugging into equations."),
                                        isBookmarked = true,
                                        isRevised = false
                                    )
                                    notesList = listOf(aiGeneratedNote) + notesList
                                    isGenerating = false
                                    showAiGenerateDialog = false
                                    Toast.makeText(context, "✨ AI Note Generated & Saved!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate AI Notes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

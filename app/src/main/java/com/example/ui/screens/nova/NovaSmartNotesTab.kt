package com.example.ui.screens.nova

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.RevisionCategory
import com.example.data.model.SmartNoteItem
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaScreenTab
import com.example.viewmodel.NovaViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class NoteStatusFilter(val label: String) {
    ALL("All"),
    BOOKMARKED("⭐ Important"),
    NEEDS_REVISION("🔄 Needs Revision"),
    REVISED("✅ Revised"),
    AI_GENERATED("✨ AI Notes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaSmartNotesTab(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier,
    onBackToHub: () -> Unit = { viewModel.setTab(NovaScreenTab.DASHBOARD) }
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val notes by viewModel.allSmartNotes.collectAsState(initial = emptyList())
    val studyContext by viewModel.studyContext.collectAsState()
    val isGeneratingAiNote by viewModel.isGeneratingAiNote.collectAsState()
    val noteAiResult by viewModel.noteAiAssistanceResult.collectAsState()
    val isNoteAiAssisting by viewModel.isNoteAiAssisting.collectAsState()

    // Filter and search states
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf("All") }
    var selectedStatusFilter by remember { mutableStateOf(NoteStatusFilter.ALL) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Dialog & Detail states
    var selectedNoteForDetail by remember { mutableStateOf<SmartNoteItem?>(null) }
    var showCreateManualDialog by remember { mutableStateOf(false) }
    var showCreateAiDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<SmartNoteItem?>(null) }

    // Derive active exam subjects
    val examName = studyContext.targetExam.ifBlank { "Competitive Exam" }
    val examSubjects = remember(studyContext.subjects, notes) {
        val subjectSet = linkedSetOf<String>()
        subjectSet.add("All")
        // Add subjects from user profile / study context
        studyContext.subjects.forEach { if (it.isNotBlank()) subjectSet.add(it) }
        // Add subjects present in saved notes
        notes.forEach { if (it.subject.isNotBlank()) subjectSet.add(it.subject) }
        // Add defaults if minimal
        if (subjectSet.size <= 1) {
            subjectSet.addAll(listOf("General Science", "Mathematics", "Reasoning", "General Awareness"))
        }
        subjectSet.toList()
    }

    // Filter notes
    val filteredNotes = remember(notes, searchQuery, selectedSubject, selectedStatusFilter) {
        notes.filter { note ->
            val matchesSubject = selectedSubject == "All" || note.subject.equals(selectedSubject, ignoreCase = true)
            val matchesStatus = when (selectedStatusFilter) {
                NoteStatusFilter.ALL -> true
                NoteStatusFilter.BOOKMARKED -> note.isBookmarked
                NoteStatusFilter.NEEDS_REVISION -> !note.isRevised
                NoteStatusFilter.REVISED -> note.isRevised
                NoteStatusFilter.AI_GENERATED -> note.sourceTitle.contains("NOVA", ignoreCase = true) || note.sourceTitle.contains("AI", ignoreCase = true) || note.sourceTitle.contains("Smart Search", ignoreCase = true)
            }
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                note.title.lowercase().contains(q) ||
                        note.subject.lowercase().contains(q) ||
                        note.topic.lowercase().contains(q) ||
                        note.contentMarkdown.lowercase().contains(q) ||
                        note.formulas.any { it.lowercase().contains(q) } ||
                        note.keyPoints.any { it.lowercase().contains(q) } ||
                        note.importantFacts.any { it.lowercase().contains(q) }
            }
            matchesSubject && matchesStatus && matchesQuery
        }
    }

    val importantNotes = remember(filteredNotes) {
        filteredNotes.filter { it.isBookmarked }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF060913),
                        Color(0xFF0C1322),
                        Color(0xFF070B14)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. COMPACT LIQUID GLASS HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackToHub,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x2A1E293B))
                                .testTag("smart_notes_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Hub",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Smart Notes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 19.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x3300F5FF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${notes.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = "Your personal study notebook",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isSearchVisible = !isSearchVisible },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSearchVisible || searchQuery.isNotBlank()) Color(0x3300F5FF) else Color(0x2A1E293B))
                                .testTag("toggle_notes_search")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search Notes",
                                tint = if (isSearchVisible || searchQuery.isNotBlank()) NeonCyan else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2A1E293B))
                                    .testTag("notes_more_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More Options",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier.background(Color(0xFF131D31))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Mark All as Revised", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Outlined.DoneAll, contentDescription = null, tint = Color(0xFF4ADE80)) },
                                    onClick = {
                                        notes.forEach { note ->
                                            if (!note.isRevised) viewModel.toggleSmartNoteRevised(note.id, true)
                                        }
                                        showMoreMenu = false
                                        Toast.makeText(context, "All notes marked as revised! ✅", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear All Filters", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Outlined.FilterAltOff, contentDescription = null, tint = NeonCyan) },
                                    onClick = {
                                        searchQuery = ""
                                        selectedSubject = "All"
                                        selectedStatusFilter = NoteStatusFilter.ALL
                                        showMoreMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. EXAM CONTEXT CHIP & ACTION BUTTONS
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    fillAlpha = 0.85f,
                    borderColor = Color(0x3300F5FF)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x336366F1))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🎯 $examName",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedSubject == "All") "All Subjects" else selectedSubject,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = "${filteredNotes.size} matching",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons Row: + New Note & ✨ Create with NOVA
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showCreateManualDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("btn_new_manual_note"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0x4438BDF8))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = NeonCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "New Note",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { showCreateAiDialog = true },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(42.dp)
                                    .testTag("btn_create_with_nova"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0284C7),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Create with NOVA",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. SEARCH BAR (COLLAPSIBLE / TOGGLEABLE)
            if (isSearchVisible || searchQuery.isNotBlank()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.9f,
                        borderColor = Color(0x4400F5FF)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search notes, topics, formulas...",
                                        color = Color(0xFF64748B),
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("notes_search_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                            )
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. SUBJECT FILTER CHIPS (HORIZONTAL SCROLL)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(examSubjects) { subj ->
                            val isSelected = selectedSubject.equals(subj, ignoreCase = true)
                            val count = if (subj == "All") notes.size else notes.count { it.subject.equals(subj, ignoreCase = true) }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF4F46E5)))
                                        } else {
                                            Brush.horizontalGradient(listOf(Color(0x331E293B), Color(0x331E293B)))
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan else Color(0x22FFFFFF),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .springClickable(
                                        testTag = "filter_subject_${subj.replace(" ", "_")}",
                                        onClick = { selectedSubject = subj }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = subj,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0x33FFFFFF) else Color(0x22000000))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Status Filters: All, ⭐ Important, 🔄 Needs Revision, ✅ Revised, ✨ AI Notes
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    ) {
                        items(NoteStatusFilter.values()) { filter ->
                            val isSelected = selectedStatusFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStatusFilter = filter },
                                label = {
                                    Text(
                                        text = filter.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = if (isSelected) NeonCyan else Color(0xFF94A3B8)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0x220F172A),
                                    selectedContainerColor = Color(0x330284C7)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0x6600F5FF) else Color(0x1AFFFFFF)
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }

            // 5. IMPORTANT / BOOKMARKED NOTES SECTION (CONDITIONAL)
            if (importantNotes.isNotEmpty() && selectedStatusFilter != NoteStatusFilter.BOOKMARKED) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "⭐ Important Notes",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x33FBBF24))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${importantNotes.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Display top 2 important notes
                        importantNotes.take(2).forEach { note ->
                            SmartNoteCompactCard(
                                note = note,
                                isHighlighted = true,
                                onToggleBookmark = { viewModel.toggleSmartNoteBookmark(note.id, !note.isBookmarked) },
                                onToggleRevised = { viewModel.toggleSmartNoteRevised(note.id, !note.isRevised) },
                                onDelete = { noteToDelete = note },
                                onClick = { selectedNoteForDetail = note }
                            )
                        }
                    }
                }
            }

            // 6. ALL FILTERED NOTES LIST
            if (filteredNotes.isEmpty()) {
                item {
                    NotesEmptyState(
                        isSearching = searchQuery.isNotBlank() || selectedSubject != "All" || selectedStatusFilter != NoteStatusFilter.ALL,
                        searchQuery = searchQuery,
                        onClearSearch = {
                            searchQuery = ""
                            selectedSubject = "All"
                            selectedStatusFilter = NoteStatusFilter.ALL
                        },
                        onNewNote = { showCreateManualDialog = true },
                        onAiNote = { showCreateAiDialog = true }
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedSubject == "All") "All Study Notes" else "$selectedSubject Notes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${filteredNotes.size} total",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                items(filteredNotes, key = { it.id }) { note ->
                    SmartNoteCompactCard(
                        note = note,
                        isHighlighted = false,
                        onToggleBookmark = { viewModel.toggleSmartNoteBookmark(note.id, !note.isBookmarked) },
                        onToggleRevised = { viewModel.toggleSmartNoteRevised(note.id, !note.isRevised) },
                        onDelete = { noteToDelete = note },
                        onClick = { selectedNoteForDetail = note }
                    )
                }
            }
        }
    }

    // =========================================================================
    // DIALOGS & OVERLAYS
    // =========================================================================

    // 1. NOTE DETAIL & EDITOR FULL MODAL
    selectedNoteForDetail?.let { note ->
        SmartNoteDetailModal(
            note = note,
            examName = examName,
            isAiAssisting = isNoteAiAssisting,
            aiAssistanceResult = noteAiResult,
            onDismiss = {
                selectedNoteForDetail = null
                viewModel.clearNoteAiAssistance()
            },
            onSaveUpdatedNote = { updatedNote ->
                viewModel.updateSmartNote(updatedNote)
                selectedNoteForDetail = updatedNote
            },
            onToggleBookmark = {
                viewModel.toggleSmartNoteBookmark(note.id, !note.isBookmarked)
                selectedNoteForDetail = note.copy(isBookmarked = !note.isBookmarked)
            },
            onToggleRevised = {
                viewModel.toggleSmartNoteRevised(note.id, !note.isRevised)
                selectedNoteForDetail = note.copy(isRevised = !note.isRevised)
            },
            onDeleteNote = {
                viewModel.deleteSmartNote(note.id)
                selectedNoteForDetail = null
            },
            onConvertToFlashcards = {
                viewModel.convertNoteToFlashcards(note)
                Toast.makeText(context, "Added flashcards to Spaced Recall! 🗂️", Toast.LENGTH_SHORT).show()
            },
            onAskNova = { action ->
                viewModel.assistWithSmartNote(note, action)
            },
            onClearAiResult = {
                viewModel.clearNoteAiAssistance()
            }
        )
    }

    // 2. CREATE WITH NOVA (AI GENERATOR) MODAL
    if (showCreateAiDialog) {
        CreateWithNovaDialog(
            examName = examName,
            availableSubjects = examSubjects.filter { it != "All" },
            isGenerating = isGeneratingAiNote,
            onDismiss = { showCreateAiDialog = false },
            onGenerate = { subject, topic, noteType, language ->
                viewModel.generateAiSmartNote(
                    subject = subject,
                    topic = topic,
                    noteType = noteType,
                    language = language,
                    onSuccess = { generated ->
                        showCreateAiDialog = false
                        selectedNoteForDetail = generated
                    }
                )
            }
        )
    }

    // 3. MANUAL NEW NOTE DIALOG
    if (showCreateManualDialog) {
        CreateManualNoteDialog(
            examSubjects = examSubjects.filter { it != "All" },
            onDismiss = { showCreateManualDialog = false },
            onSave = { newNote ->
                viewModel.saveSmartNote(newNote)
                showCreateManualDialog = false
                Toast.makeText(context, "Note Saved! 📝", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 4. DELETE CONFIRMATION DIALOG
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = {
                Text(
                    text = "Delete Note?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${note.title}\"? This action cannot be undone.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSmartNote(note.id)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF131D31),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// =============================================================================
// COMPACT NOTE CARD
// =============================================================================

@Composable
private fun SmartNoteCompactCard(
    note: SmartNoteItem,
    isHighlighted: Boolean = false,
    onToggleBookmark: () -> Unit,
    onToggleRevised: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val isAiNote = note.sourceTitle.contains("NOVA", ignoreCase = true) ||
            note.sourceTitle.contains("AI", ignoreCase = true) ||
            note.sourceTitle.contains("Smart Search", ignoreCase = true)

    val relativeTime = remember(note.createdAt) {
        val diff = System.currentTimeMillis() - note.createdAt
        when {
            diff < 60_000L -> "Just now"
            diff < 3600_000L -> "${diff / 60_000L}m ago"
            diff < 86400_000L -> "${diff / 3600_000L}h ago"
            diff < 7 * 86400_000L -> "${diff / 86400_000L}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(note.createdAt))
        }
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .springClickable(testTag = "note_item_${note.id}", onClick = onClick),
        fillAlpha = if (isHighlighted) 0.9f else 0.82f,
        borderColor = if (isHighlighted) Color(0x66FBBF24) else Color(0x2A38BDF8)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Status dot, Title, and Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (note.isRevised) Color(0xFF4ADE80) else Color(0xFF38BDF8))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (note.isBookmarked) Color(0xFFFBBF24) else Color(0xFF64748B),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleRevised,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isRevised) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                            contentDescription = "Revised Status",
                            tint = if (note.isRevised) Color(0xFF4ADE80) else Color(0xFF64748B),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Note",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Metadata row: Subject • Topic • Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${note.subject} • ${note.topic}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569),
                    fontSize = 10.sp
                )
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }

            // Excerpt snippet
            if (note.contentMarkdown.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val cleanSnippet = note.contentMarkdown
                    .replace(Regex("#+\\s*"), "")
                    .replace(Regex("\\*\\*"), "")
                    .replace("\n", " ")
                    .trim()

                Text(
                    text = cleanSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            // Badges row: AI Generated, Formulas count, Key points count
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAiNote) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x226366F1))
                            .border(0.5.dp, Color(0x55818CF8), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "NOVA AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA5B4FC),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (note.formulas.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x2200F5FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "📐 ${note.formulas.size} Formula${if (note.formulas.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontSize = 9.sp
                        )
                    }
                }

                if (note.keyPoints.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x2238BDF8))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🔑 ${note.keyPoints.size} Key Points",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Revised indicator pill
                if (note.isRevised) {
                    Text(
                        text = "Revised",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4ADE80),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =============================================================================
// NOTE DETAIL & EDITOR FULL MODAL
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartNoteDetailModal(
    note: SmartNoteItem,
    examName: String,
    isAiAssisting: Boolean,
    aiAssistanceResult: String?,
    onDismiss: () -> Unit,
    onSaveUpdatedNote: (SmartNoteItem) -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleRevised: () -> Unit,
    onDeleteNote: () -> Unit,
    onConvertToFlashcards: () -> Unit,
    onAskNova: (String) -> Unit,
    onClearAiResult: () -> Unit
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }

    // Editable state
    var editTitle by remember(note) { mutableStateOf(note.title) }
    var editSubject by remember(note) { mutableStateOf(note.subject) }
    var editTopic by remember(note) { mutableStateOf(note.topic) }
    var editContent by remember(note) { mutableStateOf(note.contentMarkdown) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            containerColor = Color(0xFF070C18),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEditing) "Edit Note" else note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (isEditing) {
                            IconButton(onClick = {
                                if (editTitle.isNotBlank()) {
                                    onSaveUpdatedNote(
                                        note.copy(
                                            title = editTitle.trim(),
                                            subject = editSubject.trim(),
                                            topic = editTopic.trim().ifBlank { "General" },
                                            contentMarkdown = editContent.trim()
                                        )
                                    )
                                    isEditing = false
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Save Changes",
                                    tint = NeonCyan
                                )
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = onToggleBookmark) {
                                Icon(
                                    imageVector = if (note.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (note.isBookmarked) Color(0xFFFBBF24) else Color.White
                                )
                            }
                            IconButton(onClick = onToggleRevised) {
                                Icon(
                                    imageVector = if (note.isRevised) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                    contentDescription = "Revised",
                                    tint = if (note.isRevised) Color(0xFF4ADE80) else Color.White
                                )
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Study Note", "${note.title}\n\n${note.contentMarkdown}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Note copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0C1324))
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Metadata Pill
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x3300F5FF))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${note.subject} • ${note.topic}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }

                        if (note.sourceTitle.isNotBlank()) {
                            Text(
                                text = "Source: ${note.sourceTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // EDITING MODE vs READING MODE
                if (isEditing) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Note Title") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x44FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editSubject,
                                    onValueChange = { editSubject = it },
                                    label = { Text("Subject") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = Color(0x44FFFFFF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                OutlinedTextField(
                                    value = editTopic,
                                    onValueChange = { editTopic = it },
                                    label = { Text("Topic") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = Color(0x44FFFFFF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                            OutlinedTextField(
                                value = editContent,
                                onValueChange = { editContent = it },
                                label = { Text("Content / Markdown") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x44FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }
                } else {
                    // MAIN CONTENT SECTION
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            fillAlpha = 0.85f
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = note.contentMarkdown,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    // FORMULAS SECTION
                    if (note.formulas.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "📐 Formulas & Core Rules",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    fontSize = 13.sp
                                )
                                note.formulas.forEach { formula ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0F172A))
                                            .border(1.dp, Color(0x3300F5FF), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = formula,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // KEY POINTS SECTION
                    if (note.keyPoints.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "🔑 Key Exam Takeaways",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 13.sp
                                )
                                note.keyPoints.forEach { kp ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x221E293B))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "•",
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text(
                                            text = kp,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // IMPORTANT FACTS
                    if (note.importantFacts.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "📌 High Yield Exam Facts",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    fontSize = 13.sp
                                )
                                note.importantFacts.forEach { fact ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x22FBBF24))
                                            .border(0.5.dp, Color(0x44FBBF24), RoundedCornerShape(6.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "💡 $fact",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFFEF3C7),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. NOVA NOTE ASSISTANCE BAR ("✨ Ask NOVA")
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            fillAlpha = 0.9f,
                            borderColor = Color(0x666366F1)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                            tint = Color(0xFFA5B4FC),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "✨ NOVA Note Assistant ($examName)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (isAiAssisting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = NeonCyan,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }

                                // Interactive Action Chips
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val actions = listOf(
                                        "📝 Summarize",
                                        "💡 Explain simply",
                                        "🔑 Key points",
                                        "🎴 Flashcards",
                                        "❓ Questions",
                                        "⚡ Improve note",
                                        "📌 Important facts"
                                    )
                                    items(actions) { actionText ->
                                        AssistChip(
                                            onClick = {
                                                if (!isAiAssisting) {
                                                    val cleanAction = actionText.substringAfter(" ")
                                                    onAskNova(cleanAction)
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = actionText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 11.sp,
                                                    color = Color.White
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = Color(0x331E293B)
                                            ),
                                            border = BorderStroke(1.dp, Color(0x446366F1))
                                        )
                                    }
                                }

                                // AI Response Display Panel
                                aiAssistanceResult?.let { resultText ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0F172A))
                                            .border(1.dp, Color(0x6600F5FF), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "NOVA Insights",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeonCyan
                                                )
                                                IconButton(
                                                    onClick = onClearAiResult,
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Close AI result",
                                                        tint = Color(0xFF94A3B8),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = resultText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFE2E8F0),
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp
                                            )

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Button(
                                                    onClick = {
                                                        onSaveUpdatedNote(
                                                            note.copy(
                                                                contentMarkdown = "${note.contentMarkdown}\n\n---\n### AI Additions\n$resultText"
                                                            )
                                                        )
                                                        onClearAiResult()
                                                        Toast.makeText(context, "Appended to Note! 📝", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0x3300F5FF),
                                                        contentColor = NeonCyan
                                                    ),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Append to Note", fontSize = 11.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        onConvertToFlashcards()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0x336366F1),
                                                        contentColor = Color(0xFFA5B4FC)
                                                    ),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Spaced Flashcards", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // CONVERT TO FLASHCARDS & DELETE ROW
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onConvertToFlashcards,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFF38BDF8)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0x3338BDF8))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Layers,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Convert to Cards", fontSize = 12.sp)
                            }

                            Button(
                                onClick = onDeleteNote,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x22EF4444),
                                    contentColor = Color(0xFFF87171)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0x33EF4444))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete Note", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// "CREATE WITH NOVA" AI GENERATOR DIALOG
// =============================================================================

@Composable
private fun CreateWithNovaDialog(
    examName: String,
    availableSubjects: List<String>,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (subject: String, topic: String, noteType: String, language: String) -> Unit
) {
    var subject by remember { mutableStateOf(availableSubjects.firstOrNull() ?: "General Science") }
    var topic by remember { mutableStateOf("") }
    var noteType by remember { mutableStateOf("Quick Revision") }
    var language by remember { mutableStateOf("English") }

    val noteTypes = listOf(
        "⚡ Quick Revision",
        "📖 Detailed Explanation",
        "📐 Formula Sheet",
        "📌 Important Facts",
        "🎯 Exam Notes",
        "❌ Mistake Notes"
    )

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Create with NOVA",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Target Exam: $examName",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA5B4FC),
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Topic Field
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Chapter (e.g. Thermodynamics, Optics)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_note_topic_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                // Subject Selection Chips
                Text("Subject:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableSubjects) { s ->
                        val isSelected = subject == s
                        FilterChip(
                            selected = isSelected,
                            onClick = { subject = s },
                            label = { Text(s, fontSize = 10.sp, color = if (isSelected) Color.White else Color(0xFFCBD5E1)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0x221E293B),
                                selectedContainerColor = Color(0xFF0284C7)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0x22FFFFFF))
                        )
                    }
                }

                // Note Type Selection Chips
                Text("Note Style:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    noteTypes.chunked(2).forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowItems.forEach { nt ->
                                val cleanName = nt.substringAfter(" ")
                                val isSelected = noteType.equals(cleanName, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0x3300F5FF) else Color(0x221E293B))
                                        .border(1.dp, if (isSelected) NeonCyan else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable { noteType = cleanName }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = nt,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonCyan else Color(0xFFCBD5E1),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Language Selection Chips
                Text("Language:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("English", "हिंदी", "Hinglish").forEach { lang ->
                        val isSelected = language == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = { language = lang },
                            label = { Text(lang, fontSize = 11.sp, color = if (isSelected) Color.White else Color(0xFFCBD5E1)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0x221E293B),
                                selectedContainerColor = Color(0xFF4F46E5)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFA5B4FC) else Color(0x22FFFFFF))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (topic.isNotBlank()) {
                        onGenerate(subject, topic, noteType, language)
                    }
                },
                enabled = topic.isNotBlank() && !isGenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_generate_ai_note")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Synthesizing...", fontWeight = FontWeight.Bold)
                } else {
                    Text("Generate ✨", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isGenerating) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF101827),
        shape = RoundedCornerShape(16.dp)
    )
}

// =============================================================================
// MANUAL NOTE CREATOR DIALOG
// =============================================================================

@Composable
private fun CreateManualNoteDialog(
    examSubjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (SmartNoteItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(examSubjects.firstOrNull() ?: "General Science") }
    var topic by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var formula by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Smart Note",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Key Content / Synthesis") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = formula,
                    onValueChange = { formula = it },
                    label = { Text("Formula (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            SmartNoteItem(
                                title = title.trim(),
                                subject = subject.trim(),
                                topic = topic.trim().ifBlank { "General" },
                                contentMarkdown = content.trim(),
                                formulas = if (formula.isNotBlank()) listOf(formula.trim()) else emptyList(),
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                )
            ) {
                Text("Save Note", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF101827),
        shape = RoundedCornerShape(16.dp)
    )
}

// =============================================================================
// EMPTY STATES
// =============================================================================

@Composable
private fun NotesEmptyState(
    isSearching: Boolean,
    searchQuery: String,
    onClearSearch: () -> Unit,
    onNewNote: () -> Unit,
    onAiNote: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(0.92f),
            fillAlpha = 0.8f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isSearching) Color(0x2200F5FF) else Color(0x226366F1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSearching) Icons.Outlined.SearchOff else Icons.Outlined.NoteAlt,
                        contentDescription = null,
                        tint = if (isSearching) NeonCyan else Color(0xFFA5B4FC),
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (isSearching) {
                    Text(
                        text = "No notes found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "No notes match \"$searchQuery\". Try another keyword or subject filter." else "No notes match current subject/status filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onClearSearch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x3300F5FF),
                            contentColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear Filters")
                    }
                } else {
                    Text(
                        text = "Your study notebook is empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Create your first note and NOVA will help you organize formulas, key points, and revision tasks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onNewNote,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0x3338BDF8))
                        ) {
                            Text("+ New Note", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onAiNote,
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("✨ Create with NOVA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

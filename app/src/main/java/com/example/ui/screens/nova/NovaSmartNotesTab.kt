package com.example.ui.screens.nova

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SmartNoteItem
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaViewModel

@Composable
fun NovaSmartNotesTab(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes by viewModel.allSmartNotes.collectAsState()
    var filterBookmarkedOnly by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<SmartNoteItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val displayedNotes = if (filterBookmarkedOnly) notes.filter { it.isBookmarked } else notes

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Actions
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                fillAlpha = 0.85f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Smart Notes & Formulas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${notes.size} Notes • AI synthesized & verified",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { filterBookmarkedOnly = !filterBookmarkedOnly },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (filterBookmarkedOnly) NeonCyan.copy(alpha = 0.2f) else Color(0x18FFFFFF))
                                .testTag("bookmark_filter_btn")
                        ) {
                            Icon(
                                imageVector = if (filterBookmarkedOnly) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmarks",
                                tint = if (filterBookmarkedOnly) NeonCyan else Color.White
                            )
                        }

                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f))
                                .testTag("add_note_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Note",
                                tint = NeonCyan
                            )
                        }
                    }
                }
            }
        }

        if (displayedNotes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.NoteAlt,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (filterBookmarkedOnly) "No bookmarked notes found" else "No smart notes yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Search concepts or tap '+' to create one!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        } else {
            items(displayedNotes, key = { it.id }) { note ->
                SmartNoteItemCard(
                    note = note,
                    onToggleBookmark = { viewModel.toggleSmartNoteBookmark(note.id, !note.isBookmarked) },
                    onToggleRevised = { viewModel.toggleSmartNoteRevised(note.id, !note.isRevised) },
                    onDelete = {
                        viewModel.deleteSmartNote(note.id)
                        Toast.makeText(context, "Note removed", Toast.LENGTH_SHORT).show()
                    },
                    onClick = { selectedNote = note }
                )
            }
        }
    }

    // Note Details Modal
    selectedNote?.let { note ->
        AlertDialog(
            onDismissRequest = { selectedNote = null },
            title = {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "${note.subject} • ${note.topic}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }

                    item {
                        Text(
                            text = note.contentMarkdown,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }

                    if (note.formulas.isNotEmpty()) {
                        item {
                            Text(
                                text = "📐 Formulas:",
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                        items(note.formulas) { formula ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x22000000))
                                    .padding(8.dp)
                            ) {
                                Text(formula, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (note.keyPoints.isNotEmpty()) {
                        item {
                            Text(
                                text = "🔑 Key Points:",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        items(note.keyPoints) { kp ->
                            Text("• $kp", color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedNote = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("Close")
                }
            },
            containerColor = Color(0xFF131C2E)
        )
    }

    // Create Note Dialog
    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("Physics") }
        var topic by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var formula by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Smart Note", color = Color.White, fontWeight = FontWeight.Bold) },
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Topic") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Key Content / Synthesis") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = formula,
                        onValueChange = { formula = it },
                        label = { Text("Formula (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.saveSmartNote(
                                SmartNoteItem(
                                    title = title,
                                    subject = subject,
                                    topic = topic.ifBlank { "General" },
                                    contentMarkdown = content,
                                    formulas = if (formula.isNotBlank()) listOf(formula) else emptyList(),
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            showCreateDialog = false
                            Toast.makeText(context, "Note Created 📝", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF131C2E)
        )
    }
}

@Composable
private fun SmartNoteItemCard(
    note: SmartNoteItem,
    onToggleBookmark: () -> Unit,
    onToggleRevised: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.8f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .springClickable(testTag = "note_item_${note.id}", onClick = onClick)
        ) {
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (note.isRevised) Color(0xFF4ADE80) else NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleBookmark, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (note.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (note.isBookmarked) NeonCyan else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onToggleRevised, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (note.isRevised) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                            contentDescription = "Revised",
                            tint = if (note.isRevised) Color(0xFF4ADE80) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${note.subject} • ${note.topic}",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan
            )

            if (note.formulas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x22000000))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "📐 ${note.formulas.first()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

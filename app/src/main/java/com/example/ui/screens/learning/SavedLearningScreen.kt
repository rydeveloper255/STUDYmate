package com.example.ui.screens.learning

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SmartNoteItem
import com.example.data.model.UserLearningBookmark
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLearningScreen(
    bookmarks: List<UserLearningBookmark>,
    smartNotes: List<SmartNoteItem>,
    onOpenTopic: (subject: String, topic: String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Notes, 2: Bookmarks, 3: Explanations
    var searchQuery by remember { mutableStateOf("") }

    val tabs = listOf("All Saved", "📝 Notes (${smartNotes.size})", "🔖 Bookmarks (${bookmarks.size})")

    val filteredNotes = remember(smartNotes, searchQuery) {
        if (searchQuery.isBlank()) smartNotes
        else smartNotes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.topic.contains(searchQuery, ignoreCase = true) ||
                    it.subject.contains(searchQuery, ignoreCase = true) ||
                    it.contentMarkdown.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredBookmarks = remember(bookmarks, searchQuery) {
        if (searchQuery.isBlank()) bookmarks
        else bookmarks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.topic.contains(searchQuery, ignoreCase = true) ||
                    it.subject.contains(searchQuery, ignoreCase = true) ||
                    it.snippet.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("saved_learning_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Saved Learning & Notes", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("${smartNotes.size + bookmarks.size} total items preserved", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("saved_learning_back_btn")) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050814))
            )
        },
        containerColor = Color(0xFF050814)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search saved notes, bookmarks, topics...", color = Color(0xFF64748B), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = NeonCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, null, tint = Color(0xFF94A3B8))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x18FFFFFF),
                    unfocusedContainerColor = Color(0x10FFFFFF),
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0x20FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = NeonCyan,
                edgePadding = 0.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) NeonCyan else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag(
                            when (index) {
                                0 -> "saved_tab_all"
                                1 -> "saved_tab_notes"
                                else -> "saved_tab_bookmarks"
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val showNotes = selectedTab == 0 || selectedTab == 1
            val showBookmarks = selectedTab == 0 || selectedTab == 2

            if ((!showNotes || filteredNotes.isEmpty()) && (!showBookmarks || filteredBookmarks.isEmpty())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.BookmarkBorder, null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No saved items found", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                        Text("Save key formulas, notes, or explanations during learning.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (showNotes) {
                        items(filteredNotes, key = { "note_${it.id}" }) { note ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .springClickable { onOpenTopic(note.subject, note.topic) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x3038BDF8)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.EditNote, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${note.subject} • ${note.topic}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeonCyan,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteNote(note.id) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("delete_saved_item_btn")
                                        ) {
                                            Icon(Icons.Outlined.Delete, "Delete", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = note.contentMarkdown,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Saved ${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(note.createdAt))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    if (showBookmarks) {
                        items(filteredBookmarks, key = { "bm_${it.id}" }) { bm ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .springClickable { onOpenTopic(bm.subject, bm.topic) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x30F59E0B)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.Bookmark, null, tint = GoldenSpark, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${bm.subject} • ${bm.topic}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GoldenSpark,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteBookmark(bm.id) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("delete_saved_item_btn")
                                        ) {
                                            Icon(Icons.Outlined.Delete, "Delete", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = bm.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = bm.snippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
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

package com.example.ui.screens.resources

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReadingStatus
import com.example.data.model.ResourceSearchResult
import com.example.data.model.ResourceSource
import com.example.data.model.ResourceType
import com.example.data.model.StudyResourceEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyResourceHubScreen(
    resources: List<ResourceSearchResult>,
    activeExam: String = "",
    activeSubject: String = "",
    activeTopic: String = "",
    searchQuery: String = "",
    selectedResourceType: String = ResourceType.ALL.name,
    onSearchQueryChange: (String) -> Unit = {},
    onSelectResourceType: (String) -> Unit = {},
    onOpenResource: (StudyResourceEntity) -> Unit = {},
    onToggleSave: (String) -> Unit = {},
    onStartFocusFromResource: (subject: String, topic: String, resourceId: String) -> Unit = { _, _, _ -> },
    onUploadCustomResource: (title: String, description: String, exam: String, subject: String, topic: String, content: String) -> Unit = { _, _, _, _, _, _ -> },
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadTitle by remember { mutableStateOf("") }
    var uploadDesc by remember { mutableStateOf("") }
    var uploadSubject by remember { mutableStateOf(activeSubject.ifBlank { "Mathematics" }) }
    var uploadTopic by remember { mutableStateOf(activeTopic.ifBlank { "Percentage" }) }
    var uploadContent by remember { mutableStateOf("") }

    val topRecommended = resources.firstOrNull { it.relevanceScore > 30 }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("study_resource_hub_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Study Resources", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text(
                            text = if (activeSubject.isNotBlank()) "Context: $activeSubject ${if (activeTopic.isNotBlank()) "• $activeTopic" else ""}" else "PDF Vault, Formula Sheets & Notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showUploadDialog = true }) {
                        Icon(Icons.Filled.UploadFile, contentDescription = "Upload Study Material", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkCanvas
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resource_search_input"),
                placeholder = { Text("Search PDF notes, formulas (e.g. Maths percentage, Railway CA)...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = NeonCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = CardSurfaceDark,
                    unfocusedContainerColor = CardSurfaceDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Type Category Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ResourceType.values()) { type ->
                    val isSelected = selectedResourceType.equals(type.name, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectResourceType(type.name) },
                        label = { Text(type.displayName, fontSize = 12.sp, color = if (isSelected) Color.White else Color.LightGray) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            containerColor = CardSurfaceDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resource List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Context Recommended Top Banner
                if (topRecommended != null && searchQuery.isBlank() && selectedResourceType == ResourceType.ALL.name) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("top_recommended_resource_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = NebulaPurple.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NebulaPurple, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Nova Recommended", style = MaterialTheme.typography.labelSmall, color = NebulaPurple, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = topRecommended.resource.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = topRecommended.matchReason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { onOpenResource(topRecommended.resource) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                    ) {
                                        Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Document", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (resources.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No study resources available yet.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Upload a study note or clear search filters to explore all resources.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showUploadDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Icon(Icons.Filled.UploadFile, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload Study Note", color = Color.Black)
                                }
                            }
                        }
                    }
                } else {
                    items(resources, key = { it.resource.resourceId }) { item ->
                        ResourceCard(
                            resource = item.resource,
                            matchReason = item.matchReason,
                            onOpen = { onOpenResource(item.resource) },
                            onToggleSave = { onToggleSave(item.resource.resourceId) },
                            onStartFocus = { onStartFocusFromResource(item.resource.subjectName, item.resource.topicName, item.resource.resourceId) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        // Upload Study Material Dialog
        if (showUploadDialog) {
            AlertDialog(
                onDismissRequest = { showUploadDialog = false },
                title = { Text("Upload Study Material", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uploadTitle,
                            onValueChange = { uploadTitle = it },
                            label = { Text("Title (e.g. Percentage Short Formula Sheet)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uploadSubject,
                            onValueChange = { uploadSubject = it },
                            label = { Text("Subject (e.g. Mathematics)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uploadTopic,
                            onValueChange = { uploadTopic = it },
                            label = { Text("Topic (e.g. Percentage)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uploadContent,
                            onValueChange = { uploadContent = it },
                            label = { Text("Paste Notes / Study Content") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (uploadTitle.isNotBlank() && uploadContent.isNotBlank()) {
                                onUploadCustomResource(uploadTitle, uploadDesc, activeExam, uploadSubject, uploadTopic, uploadContent)
                                showUploadDialog = false
                                Toast.makeText(context, "Study material added successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Save Material")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUploadDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

@Composable
fun ResourceCard(
    resource: StudyResourceEntity,
    matchReason: String,
    onOpen: () -> Unit,
    onToggleSave: () -> Unit,
    onStartFocus: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("resource_card_${resource.resourceId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source & Type Badges
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = resource.resourceType,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val sourceColor = when (resource.source) {
                        ResourceSource.OFFICIAL.name -> EmeraldSuccess
                        ResourceSource.AI_GENERATED.name -> NebulaPurple
                        ResourceSource.USER_UPLOADED.name -> GoldenSpark
                        else -> NeonCyan
                    }

                    Surface(
                        color = sourceColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = resource.source,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = sourceColor
                        )
                    }
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (resource.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Save Resource",
                        tint = if (resource.isSaved) GoldenSpark else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = resource.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${resource.subjectName} • ${resource.topicName} (${resource.language})",
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan
            )

            if (resource.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resource.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Actions & Reading Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusText = when (resource.readingStatus) {
                        ReadingStatus.COMPLETED.name -> "✓ Completed"
                        ReadingStatus.IN_PROGRESS.name -> "Page ${resource.lastViewedPage} of ${resource.totalPages}"
                        else -> "Not Started (${resource.totalPages} pgs)"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (resource.readingStatus == ReadingStatus.COMPLETED.name) EmeraldSuccess else Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onStartFocus,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Filled.Timer, contentDescription = null, tint = NebulaPurple, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Focus", fontSize = 11.sp, color = NebulaPurple)
                    }

                    Button(
                        onClick = onOpen,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Read", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

package com.example.ui.screens.revision

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.service.intelligence.PracticeMode
import com.example.service.intelligence.SnoozeOption
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class RevisionFilterTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DUE_TODAY("Due Today", Icons.Filled.LocalFireDepartment),
    UPCOMING("Upcoming", Icons.Filled.Event),
    NEEDS_PRACTICE("Needs Practice", Icons.Filled.WarningAmber),
    RECENTLY_REVISED("Recently Revised", Icons.Filled.CheckCircle),
    ALL("All Topics", Icons.Filled.List)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionHubScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit = {},
    onStartFocus: (String, String, Int) -> Unit = { _, _, _ -> },
    onStartPractice: (PracticeMode, String, String) -> Unit = { _, _, _ -> },
    onOpenResources: (String?) -> Unit = {}
) {
    val allItems by mainViewModel.allRevisionItems.collectAsState()
    val dueItems by mainViewModel.dueRevisionItems.collectAsState()
    val stats by mainViewModel.revisionRetentionStats.collectAsState()
    val selectedExam by mainViewModel.selectedExam.collectAsState()
    val allMistakes by mainViewModel.mistakes.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(RevisionFilterTab.DUE_TODAY) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var activeSessionItem by remember { mutableStateOf<RevisionItemEntity?>(null) }
    var itemToSnooze by remember { mutableStateOf<RevisionItemEntity?>(null) }
    var itemToEdit by remember { mutableStateOf<RevisionItemEntity?>(null) }

    val now = System.currentTimeMillis()

    // Filter items based on tab & search
    val filteredItems = remember(allItems, dueItems, selectedTab, searchQuery) {
        val baseList = when (selectedTab) {
            RevisionFilterTab.DUE_TODAY -> dueItems
            RevisionFilterTab.UPCOMING -> allItems.filter { it.status == "PENDING" && it.scheduledAt > now }
            RevisionFilterTab.NEEDS_PRACTICE -> allItems.filter { it.mistakeCount > 0 || (it.practiceAccuracy in 0.01f..0.59f) }
            RevisionFilterTab.RECENTLY_REVISED -> allItems.filter { it.status == "COMPLETED" || it.lastReviewedAt > 0 }
            RevisionFilterTab.ALL -> allItems.filter { it.status != "ARCHIVED" }
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.topic.contains(searchQuery, ignoreCase = true) ||
                it.subject.contains(searchQuery, ignoreCase = true) ||
                it.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Smart Revision Hub",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    selectedExam.examName.ifBlank { "Exam Prep" },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                        Text(
                            "Intelligent Spaced Repetition & Retention Engine",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("revision_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_revision_item_button")
                    ) {
                        Icon(Icons.Filled.AddCircleOutline, contentDescription = "Add Revision Topic")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("revision_hub_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Retention & Daily Target Banner
            item {
                RevisionRetentionSummaryBanner(
                    stats = stats,
                    dueCount = dueItems.size,
                    completedToday = allItems.count {
                        it.lastReviewedAt > 0 &&
                        TimeUnit.MILLISECONDS.toDays(now - it.lastReviewedAt) == 0L
                    },
                    dailyTarget = 3
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search topics, subjects, or formulas...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("revision_search_field")
                )
            }

            // Filter Tabs Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(RevisionFilterTab.values()) { tab ->
                        val isSelected = selectedTab == tab
                        val badgeCount = when (tab) {
                            RevisionFilterTab.DUE_TODAY -> dueItems.size
                            RevisionFilterTab.NEEDS_PRACTICE -> allItems.count { it.mistakeCount > 0 }
                            else -> null
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tab.title)
                                    if (badgeCount != null && badgeCount > 0) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.error,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "$badgeCount",
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onError,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Items List or Empty State
            if (filteredItems.isEmpty()) {
                item {
                    RevisionEmptyState(
                        tab = selectedTab,
                        onAddTopic = { showAddDialog = true }
                    )
                }
            } else {
                items(filteredItems, key = { it.revisionItemId }) { item ->
                    RevisionItemCard(
                        item = item,
                        onStartRevision = { activeSessionItem = item },
                        onStartFocus = {
                            onStartFocus(item.subject, item.topic, 25)
                        },
                        onPracticeTopic = {
                            onStartPractice(PracticeMode.TOPIC_PRACTICE, item.subject, item.topic)
                        },
                        onSnooze = { itemToSnooze = item },
                        onEdit = { itemToEdit = item },
                        onDelete = { mainViewModel.removeRevisionItem(item.revisionItemId) },
                        onOpenResources = { onOpenResources(item.resourceId) }
                    )
                }
            }
        }
    }

    // Active Revision Session Interactive Dialog
    if (activeSessionItem != null) {
        RevisionActiveSessionDialog(
            item = activeSessionItem!!,
            onDismiss = { activeSessionItem = null },
            onComplete = { score, total, method, timeSpent, notes ->
                mainViewModel.completeRevisionSession(
                    revisionItemId = activeSessionItem!!.revisionItemId,
                    scoreEarned = score,
                    totalQuestions = total,
                    methodUsed = method,
                    timeSpentSeconds = timeSpent,
                    notes = notes
                )
                activeSessionItem = null
            },
            onLaunchPractice = { subject, topic ->
                val itm = activeSessionItem
                activeSessionItem = null
                if (itm != null) {
                    onStartPractice(PracticeMode.TOPIC_PRACTICE, subject, topic)
                }
            }
        )
    }

    // Snooze Bottom Dialog
    if (itemToSnooze != null) {
        RevisionSnoozeDialog(
            item = itemToSnooze!!,
            onDismiss = { itemToSnooze = null },
            onSelectSnooze = { option ->
                mainViewModel.snoozeRevisionItem(itemToSnooze!!.revisionItemId, option)
                itemToSnooze = null
            }
        )
    }

    // Add Topic Dialog
    if (showAddDialog) {
        AddRevisionTopicDialog(
            onDismiss = { showAddDialog = false },
            onAddTopic = { subject, topic, priority, method, notes, prompt ->
                mainViewModel.addTopicToRevision(
                    subject = subject,
                    topic = topic,
                    sourceType = RevisionSourceType.MANUAL,
                    preferredMethod = method,
                    notes = notes
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RevisionRetentionSummaryBanner(
    stats: RevisionRetentionStats,
    dueCount: Int,
    completedToday: Int,
    dailyTarget: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Daily Retention Target",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "$completedToday of $dailyTarget topics reviewed today",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Surface(
                    color = if (dueCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (dueCount > 0) "$dueCount Due" else "Up to date",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (dueCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            val progress = (completedToday.toFloat() / dailyTarget.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RetentionStatItem(label = "Total Sessions", value = "${stats.totalSessions}")
                RetentionStatItem(label = "Time Spent", value = "${stats.totalTimeMinutes}m")
                RetentionStatItem(label = "Items Done", value = "${stats.itemsCompleted}")
                RetentionStatItem(
                    label = "Completion",
                    value = "${stats.completionRatePercent.toInt()}%"
                )
            }
        }
    }
}

@Composable
fun RetentionStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

@Composable
fun RevisionItemCard(
    item: RevisionItemEntity,
    onStartRevision: () -> Unit,
    onStartFocus: () -> Unit,
    onPracticeTopic: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenResources: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()) }
    val scheduledDateStr = remember(item.scheduledAt) { sdf.format(Date(item.scheduledAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("revision_card_${item.topic}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Subject, Topic & Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.subject,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        item.topic,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Priority Badge
                val (pBg, pFg) = when (item.priority) {
                    "URGENT" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    "HIGH" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                    "MEDIUM" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    color = pBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        item.priority,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = pFg
                        )
                    )
                }
            }

            // Rationale / Explainable Reason Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        item.priorityReason,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Metadata Row: Method, Review Count, Spaced Interval
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "⚡ ${item.preferredMethod.replace('_', ' ')}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "🔄 Step ${item.reviewCount} (Interval: ${item.intervalDays}d)",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                if (item.mistakeCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "⚠️ ${item.mistakeCount} mistakes",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartRevision,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Revise Now")
                }

                OutlinedButton(
                    onClick = onPracticeTopic,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Practice")
                }

                IconButton(onClick = onSnooze) {
                    Icon(Icons.Filled.Snooze, contentDescription = "Snooze")
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun RevisionActiveSessionDialog(
    item: RevisionItemEntity,
    onDismiss: () -> Unit,
    onComplete: (score: Int, total: Int, method: RevisionMethodType, timeSpent: Int, notes: String) -> Unit,
    onLaunchPractice: (String, String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Active Recall, 2: Key Formulas & Notes, 3: Completion Evaluation
    var showAnswer by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf(item.notes) }
    var confidenceScore by remember { mutableStateOf(3) } // 1..5

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Revision: ${item.topic}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Step $step of 3 • ${item.subject}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                when (step) {
                    1 -> {
                        // Step 1: Active Recall Prompt
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "🧠 Active Recall Challenge",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    item.activeRecallPrompt.ifBlank {
                                        "Explain key rules, principles, and common pitfalls of ${item.topic} without looking at notes."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (!showAnswer) {
                            Button(
                                onClick = { showAnswer = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Show Verification & Notes")
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Key Concept Summary:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        item.notes.ifBlank { "Verify fundamental concepts and standard formulas for ${item.topic}." },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Button(
                                onClick = { step = 2 },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Proceed to Practice Check")
                            }
                        }
                    }
                    2 -> {
                        // Step 2: Practice Check
                        Text(
                            "Would you like to solve 5 fast practice questions on ${item.topic}?",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Button(
                            onClick = {
                                onLaunchPractice(item.subject, item.topic)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Quiz, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Launch 5-Question Practice")
                        }

                        OutlinedButton(
                            onClick = { step = 3 },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Skip to Self-Assessment")
                        }
                    }
                    3 -> {
                        // Step 3: Self-Assessment & Interval Update
                        Text(
                            "How confident do you feel on this topic?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                1 to "Struggled",
                                3 to "Good",
                                5 to "Mastered"
                            ).forEach { (score, label) ->
                                val isSel = confidenceScore == score
                                FilterChip(
                                    selected = isSel,
                                    onClick = { confidenceScore = score },
                                    label = { Text(label) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("Revision Notes / Key Formulas") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Button(
                            onClick = {
                                onComplete(
                                    confidenceScore,
                                    5,
                                    RevisionMethodType.QUICK_REVIEW,
                                    300,
                                    notesText
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Complete & Step Spaced Interval")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RevisionSnoozeDialog(
    item: RevisionItemEntity,
    onDismiss: () -> Unit,
    onSelectSnooze: (SnoozeOption) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Snooze Revision: ${item.topic}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Choose when you'd like to review this topic next:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                OutlinedButton(
                    onClick = { onSelectSnooze(SnoozeOption.LATER_TODAY) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Later Today (+4 hours)")
                }

                Button(
                    onClick = { onSelectSnooze(SnoozeOption.TOMORROW) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Today, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tomorrow Morning (9:00 AM)")
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRevisionTopicDialog(
    onDismiss: () -> Unit,
    onAddTopic: (subject: String, topic: String, priority: String, method: RevisionMethodType, notes: String, prompt: String) -> Unit
) {
    var subject by remember { mutableStateOf("Mathematics") }
    var topic by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var preferredMethod by remember { mutableStateOf(RevisionMethodType.QUICK_REVIEW) }
    var notes by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }

    val subjects = listOf("Mathematics", "Reasoning", "General Awareness", "General Science", "English", "Quantitative Aptitude")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Add Topic to Revision Queue",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic Name *") },
                    placeholder = { Text("e.g., Percentage, Coding Decoding") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Subject", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjects) { s ->
                        FilterChip(
                            selected = subject == s,
                            onClick = { subject = s },
                            label = { Text(s, fontSize = 12.sp) }
                        )
                    }
                }

                Text("Priority", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("LOW", "MEDIUM", "HIGH", "URGENT").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Key Notes / Formulas (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (topic.isNotBlank()) {
                                onAddTopic(subject, topic.trim(), priority, preferredMethod, notes, prompt)
                            }
                        },
                        enabled = topic.isNotBlank()
                    ) {
                        Text("Add to Revision")
                    }
                }
            }
        }
    }
}

@Composable
fun RevisionEmptyState(
    tab: RevisionFilterTab,
    onAddTopic: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            tab.icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Text(
            when (tab) {
                RevisionFilterTab.DUE_TODAY -> "All Caught Up Today! 🎉"
                RevisionFilterTab.NEEDS_PRACTICE -> "No Weak Topics Detected 👍"
                else -> "No Revision Items in this Filter"
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Text(
            when (tab) {
                RevisionFilterTab.DUE_TODAY -> "You have completed all scheduled retention items for today. Add new topics or review upcoming ones."
                RevisionFilterTab.NEEDS_PRACTICE -> "Practice mistakes are automatically flagged here for high-yield revision."
                else -> "Add topics manually or continue practicing to build your spaced retention queue."
            },
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onAddTopic,
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add Topic to Revision")
        }
    }
}

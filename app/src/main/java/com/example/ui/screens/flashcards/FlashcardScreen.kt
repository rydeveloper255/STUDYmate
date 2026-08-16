package com.example.ui.screens.flashcards

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FlashcardItem
import com.example.data.model.RevisionCategory
import com.example.ui.components.*
import com.example.ui.theme.*

private enum class FlashcardTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    REVIEW("Spaced Review", Icons.Filled.Psychology),
    LIBRARY("Library & Decks", Icons.Filled.CollectionsBookmark),
    AI_GENERATE("AI Generator", Icons.Filled.AutoAwesome)
}

private enum class ReviewQueueMode(val label: String) {
    DUE_TODAY("🔥 Due Today"),
    ALL_CARDS("📚 All Cards"),
    NEEDS_REVISE("⚠️ Revise Now"),
    MASTERED("⭐ Mastered")
}

private enum class AiGeneratorSource(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    NOTES_TEXT("Pasted Notes", Icons.Filled.EditNote),
    DOCUMENT_FILE("Upload Document", Icons.Filled.UploadFile),
    TOPIC_CONCEPT("Topic & Concept", Icons.Filled.AutoAwesome)
}

@Composable
fun FlashcardScreen(
    flashcards: List<FlashcardItem>,
    isAiGenerating: Boolean,
    onAddFlashcard: (subject: String, topic: String, front: String, back: String, hint: String, difficulty: String, sourceDoc: String) -> Unit,
    onUpdateFlashcard: (FlashcardItem) -> Unit,
    onDeleteFlashcard: (Long) -> Unit,
    onReviewFlashcard: (id: Long, status: RevisionCategory, confidence: Int) -> Unit,
    onReviewSpaced: (id: Long, ratingQuality: Int) -> Unit = { id, q ->
        val status = when (q) {
            1 -> RevisionCategory.REVISE_NOW
            2, 3 -> RevisionCategory.PRACTICE_SOON
            else -> RevisionCategory.STRONG
        }
        onReviewFlashcard(id, status, q)
    },
    onGenerateAiCards: (subject: String, topic: String) -> Unit,
    onGenerateFromNotes: (title: String, notesText: String, subject: String, count: Int) -> Unit = { _, _, _, _ -> },
    onGenerateFromDocumentUri: (Uri, String, Int) -> Unit = { _, _, _ -> },
    statusMessage: String? = null,
    onClearStatusMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var selectedTab by remember { mutableStateOf(FlashcardTab.REVIEW) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<FlashcardItem?>(null) }
    var subjectFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var reviewQueueMode by remember { mutableStateOf(ReviewQueueMode.DUE_TODAY) }

    val now = remember { System.currentTimeMillis() }

    val totalCount = flashcards.size
    val strongCount = flashcards.count { it.status == RevisionCategory.STRONG }
    val dueCount = flashcards.count { it.nextReviewDate <= now || it.status == RevisionCategory.REVISE_NOW }
    val practiceCount = flashcards.count { it.status == RevisionCategory.PRACTICE_SOON }
    val masteryRate = if (totalCount > 0) (strongCount.toFloat() / totalCount) else 0f

    val subjects = remember(flashcards) {
        listOf("All") + flashcards.map { it.subject }.distinct().filter { it.isNotBlank() }
    }

    // Filtered review deck based on queue mode
    val reviewQueueCards = remember(flashcards, reviewQueueMode, subjectFilter) {
        val subjectFiltered = if (subjectFilter == "All") flashcards else flashcards.filter { it.subject.equals(subjectFilter, ignoreCase = true) }
        when (reviewQueueMode) {
            ReviewQueueMode.DUE_TODAY -> {
                val due = subjectFiltered.filter { it.nextReviewDate <= now || it.status == RevisionCategory.REVISE_NOW }
                if (due.isNotEmpty()) due else subjectFiltered // Fallback to all if none due
            }
            ReviewQueueMode.ALL_CARDS -> subjectFiltered
            ReviewQueueMode.NEEDS_REVISE -> subjectFiltered.filter { it.status == RevisionCategory.REVISE_NOW }
            ReviewQueueMode.MASTERED -> subjectFiltered.filter { it.status == RevisionCategory.STRONG }
        }
    }

    // Library filtered list
    val libraryCards = remember(flashcards, subjectFilter, searchQuery) {
        flashcards.filter { card ->
            val matchesSubject = subjectFilter == "All" || card.subject.equals(subjectFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    card.front.contains(searchQuery, ignoreCase = true) ||
                    card.back.contains(searchQuery, ignoreCase = true) ||
                    card.topic.contains(searchQuery, ignoreCase = true) ||
                    card.subject.contains(searchQuery, ignoreCase = true) ||
                    card.sourceDocTitle.contains(searchQuery, ignoreCase = true)
            matchesSubject && matchesSearch
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundGradient(isDark))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🧠", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Spaced Flashcards",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                        Text(
                            text = "SM-2 algorithmic review & Gemini note extractor",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemeToggleButton(testTag = "flashcard_theme_toggle")

                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GlassGradientPrimary)
                                .testTag("create_flashcard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Create Flashcard",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Status Message Alert if present
            if (!statusMessage.isNullOrBlank()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = if (isDark) 0.85f else 0.95f
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }
                            IconButton(onClick = onClearStatusMessage, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Summary Stats Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 6.dp,
                    fillAlpha = if (isDark) 0.65f else 0.85f
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SPACED REPETITION ENGINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${(masteryRate * 100).toInt()}% Mastered",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatPill(
                                    label = "Due Today",
                                    value = dueCount.toString(),
                                    color = CoralRose,
                                    isDark = isDark
                                )
                                StatPill(
                                    label = "Learning",
                                    value = practiceCount.toString(),
                                    color = GoldenSpark,
                                    isDark = isDark
                                )
                                StatPill(
                                    label = "Mastered",
                                    value = strongCount.toString(),
                                    color = EmeraldSuccess,
                                    isDark = isDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress distribution bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        ) {
                            if (totalCount > 0) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    val strongFraction = strongCount.toFloat() / totalCount
                                    val practiceFraction = practiceCount.toFloat() / totalCount
                                    val dueFraction = dueCount.toFloat() / totalCount

                                    if (strongFraction > 0) {
                                        Box(
                                            modifier = Modifier
                                                .weight(strongFraction)
                                                .fillMaxHeight()
                                                .background(EmeraldSuccess)
                                        )
                                    }
                                    if (practiceFraction > 0) {
                                        Box(
                                            modifier = Modifier
                                                .weight(practiceFraction)
                                                .fillMaxHeight()
                                                .background(GoldenSpark)
                                        )
                                    }
                                    if (dueFraction > 0) {
                                        Box(
                                            modifier = Modifier
                                                .weight(dueFraction)
                                                .fillMaxHeight()
                                                .background(CoralRose)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Navigation Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x301E293B) else Color(0x60E2E8F0))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FlashcardTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        if (isDark) Color(0xFF1E293B) else Color.White
                                    } else Color.Transparent
                                )
                                .clickable { selectedTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        if (isDark) Color.White else Color(0xFF0F172A)
                                    } else {
                                        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Active Tab Content
            when (selectedTab) {
                FlashcardTab.REVIEW -> {
                    item {
                        // Queue Filter chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ReviewQueueMode.entries) { mode ->
                                val isSelected = reviewQueueMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { reviewQueueMode = mode },
                                    label = { Text(mode.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = Color.Black,
                                        containerColor = if (isDark) Color(0x201E293B) else Color(0xFFE2E8F0),
                                        labelColor = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                )
                            }
                        }
                    }

                    item {
                        SpacedReviewDeckView(
                            cards = reviewQueueCards,
                            isDark = isDark,
                            onReviewSpaced = onReviewSpaced,
                            onCreateClick = { showCreateDialog = true },
                            onEditClick = { card -> cardToEdit = card }
                        )
                    }
                }

                FlashcardTab.LIBRARY -> {
                    item {
                        // Filters row
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("flashcard_search_input"),
                                placeholder = {
                                    Text(
                                        "Search question, notes source, or concept...",
                                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = NeonCyan
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Filled.Clear,
                                                contentDescription = "Clear",
                                                tint = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = if (isDark) Color(0x30FFFFFF) else Color(0xFFCBD5E1),
                                    focusedContainerColor = if (isDark) Color(0x201E293B) else Color(0x80FFFFFF),
                                    unfocusedContainerColor = if (isDark) Color(0x101E293B) else Color(0x50FFFFFF)
                                )
                            )

                            // Subject Filters
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(subjects) { subject ->
                                    val isSelected = subjectFilter == subject
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { subjectFilter = subject },
                                        label = { Text(subject) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonCyan,
                                            selectedLabelColor = Color.Black,
                                            containerColor = if (isDark) Color(0x201E293B) else Color(0xFFE2E8F0),
                                            labelColor = if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (libraryCards.isEmpty()) {
                        item {
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                fillAlpha = if (isDark) 0.5f else 0.8f
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LayersClear,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No flashcards found",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add manual flashcards or extract cards from notes/PDF using Gemini AI.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    GlassButton(
                                        text = "+ Create Flashcard",
                                        onClick = { showCreateDialog = true },
                                        isPrimary = true,
                                        testTag = "empty_create_flashcard_cta"
                                    )
                                }
                            }
                        }
                    } else {
                        items(libraryCards, key = { it.id }) { card ->
                            FlashcardListItem(
                                card = card,
                                isDark = isDark,
                                onEdit = { cardToEdit = card },
                                onDelete = { onDeleteFlashcard(card.id) }
                            )
                        }
                    }
                }

                FlashcardTab.AI_GENERATE -> {
                    item {
                        AiDeckGeneratorView(
                            isGenerating = isAiGenerating,
                            isDark = isDark,
                            onGenerateTopic = onGenerateAiCards,
                            onGenerateNotes = onGenerateFromNotes,
                            onGenerateDocument = onGenerateFromDocumentUri
                        )
                    }
                }
            }
        }
    }

    // Create / Edit Dialog
    if (showCreateDialog || cardToEdit != null) {
        FlashcardEditDialog(
            existingCard = cardToEdit,
            isDark = isDark,
            onDismiss = {
                showCreateDialog = false
                cardToEdit = null
            },
            onSave = { subject, topic, front, back, hint, difficulty, sourceDoc ->
                if (cardToEdit != null) {
                    onUpdateFlashcard(
                        cardToEdit!!.copy(
                            subject = subject,
                            topic = topic,
                            front = front,
                            back = back,
                            hint = hint,
                            difficulty = difficulty,
                            sourceDocTitle = sourceDoc
                        )
                    )
                } else {
                    onAddFlashcard(subject, topic, front, back, hint, difficulty, sourceDoc)
                }
                showCreateDialog = false
                cardToEdit = null
            }
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    color: Color,
    isDark: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = if (isDark) 0.15f else 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SpacedReviewDeckView(
    cards: List<FlashcardItem>,
    isDark: Boolean,
    onReviewSpaced: (Long, Int) -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (FlashcardItem) -> Unit
) {
    if (cards.isEmpty()) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            fillAlpha = if (isDark) 0.5f else 0.8f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "🎉 All Caught Up For Today!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No cards are currently due in this queue. Great job staying on top of your spaced repetition schedule!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                GlassButton(
                    text = "+ Add New Flashcards",
                    onClick = onCreateClick,
                    isPrimary = true,
                    testTag = "review_add_first_card_btn"
                )
            }
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }

    val safeIndex = currentIndex.coerceIn(0, cards.size - 1)
    val currentCard = cards[safeIndex]

    LaunchedEffect(safeIndex) {
        isFlipped = false
        showHint = false
    }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "flashcard_flip_rotation"
    )

    val radians = Math.toRadians(rotation.toDouble())
    val depthScale = (1f - (0.06f * kotlin.math.sin(radians))).toFloat()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Deck Progress & Controls Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Card ${safeIndex + 1} of ${cards.size}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                if (currentCard.repetitions > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x2038BDF8))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Streak: ${currentCard.repetitions} reps • Interval: ${currentCard.intervalDays}d",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = NeonCyan
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    enabled = currentIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Card",
                        tint = if (currentIndex > 0) (if (isDark) Color.White else Color(0xFF0F172A)) else Color(0xFF64748B)
                    )
                }

                IconButton(onClick = { onEditClick(currentCard) }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Card",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                IconButton(
                    onClick = { if (currentIndex < cards.size - 1) currentIndex++ },
                    enabled = currentIndex < cards.size - 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Card",
                        tint = if (currentIndex < cards.size - 1) (if (isDark) Color.White else Color(0xFF0F172A)) else Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3D Flip Flashcard Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 340.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 16f * density
                    scaleX = depthScale
                    scaleY = depthScale
                }
                .springClickable(testTag = "flashcard_interactive_flip") {
                    isFlipped = !isFlipped
                }
        ) {
            if (rotation <= 90f) {
                // FRONT OF CARD
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 12.dp,
                    fillAlpha = if (isDark) 0.88f else 0.96f,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top row badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SubjectBadge(subject = currentCard.subject)
                                DifficultyBadge(difficulty = currentCard.difficulty)
                            }

                            StatusBadge(status = currentCard.status)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Topic & Source Document
                        Column {
                            Text(
                                text = currentCard.topic.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                            if (currentCard.sourceDocTitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentCard.sourceDocTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Front Question
                        Text(
                            text = currentCard.front,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            lineHeight = 28.sp
                        )

                        // Hint Section
                        if (currentCard.hint.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            if (showHint) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x20FBBF24))
                                        .border(1.dp, Color(0x50FBBF24), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lightbulb,
                                        contentDescription = null,
                                        tint = GoldenSpark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentCard.hint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showHint = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lightbulb,
                                        contentDescription = null,
                                        tint = GoldenSpark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "💡 Tap for hint",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldenSpark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Tap instruction & CTA
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.TouchApp,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tap card to flip",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = "Reveal Answer",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Reveal Answer",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }
            } else {
                // BACK OF CARD (Answer + SM-2 Spaced Repetition Grading)
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { rotationY = 180f },
                    elevation = 12.dp,
                    fillAlpha = if (isDark) 0.92f else 0.98f,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "ANSWER & RECALL",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0x201E293B) else Color(0x60E2E8F0))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Flip to Prompt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Back text
                        Text(
                            text = currentCard.back,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // SM-2 Spaced Repetition Grading
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Rate your active recall quality:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SpacedGradeButton(
                                    label = "Again",
                                    subtext = "<1d",
                                    color = CoralRose,
                                    modifier = Modifier.weight(1f),
                                    testTag = "spaced_btn_again",
                                    onClick = {
                                        onReviewSpaced(currentCard.id, 1)
                                        if (currentIndex < cards.size - 1) currentIndex++
                                    }
                                )

                                SpacedGradeButton(
                                    label = "Hard",
                                    subtext = "2d",
                                    color = GoldenSpark,
                                    modifier = Modifier.weight(1f),
                                    testTag = "spaced_btn_hard",
                                    onClick = {
                                        onReviewSpaced(currentCard.id, 2)
                                        if (currentIndex < cards.size - 1) currentIndex++
                                    }
                                )

                                SpacedGradeButton(
                                    label = "Good",
                                    subtext = "${currentCard.intervalDays.coerceAtLeast(1) * 2}d",
                                    color = NeonCyan,
                                    modifier = Modifier.weight(1f),
                                    testTag = "spaced_btn_good",
                                    onClick = {
                                        onReviewSpaced(currentCard.id, 3)
                                        if (currentIndex < cards.size - 1) currentIndex++
                                    }
                                )

                                SpacedGradeButton(
                                    label = "Easy",
                                    subtext = "${currentCard.intervalDays.coerceAtLeast(1) * 3 + 4}d",
                                    color = EmeraldSuccess,
                                    modifier = Modifier.weight(1f),
                                    testTag = "spaced_btn_easy",
                                    onClick = {
                                        onReviewSpaced(currentCard.id, 5)
                                        if (currentIndex < cards.size - 1) currentIndex++
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpacedGradeButton(
    label: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.18f),
            contentColor = color
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = color.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun FlashcardListItem(
    card: FlashcardItem,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "list_card_flip"
    )
    val radians = Math.toRadians(rotation.toDouble())
    val depthScale = (1f - (0.04f * kotlin.math.sin(radians))).toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * density
                scaleX = depthScale
                scaleY = depthScale
            }
            .springClickable(testTag = "flashcard_list_item_${card.id}") {
                isFlipped = !isFlipped
            }
    ) {
        if (rotation <= 90f) {
            // FRONT
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                fillAlpha = if (isDark) 0.65f else 0.88f
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubjectBadge(subject = card.subject)
                            DifficultyBadge(difficulty = card.difficulty)
                            StatusBadge(status = card.status)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit Flashcard",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete Flashcard",
                                    tint = CoralRose,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.topic,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold
                        )

                        if (card.sourceDocTitle.isNotBlank()) {
                            Text(
                                text = "📄 ${card.sourceDocTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = NeonCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = card.front,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    if (card.hint.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = GoldenSpark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hint: ${card.hint}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
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
                            text = "Next review: in ${card.intervalDays}d • Reps: ${card.repetitions}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tap to flip",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            // BACK
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationY = 180f },
                elevation = 6.dp,
                fillAlpha = if (isDark) 0.85f else 0.95f
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
                        Text(
                            text = "ANSWER & EXPLANATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Flip back",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = card.back,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Ease factor: ${String.format(java.util.Locale.US, "%.2f", card.easeFactor)} • Interval: ${card.intervalDays} days",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiDeckGeneratorView(
    isGenerating: Boolean,
    isDark: Boolean,
    onGenerateTopic: (subject: String, topic: String) -> Unit,
    onGenerateNotes: (title: String, notesText: String, subject: String, count: Int) -> Unit,
    onGenerateDocument: (Uri, subject: String, count: Int) -> Unit
) {
    var activeSource by remember { mutableStateOf(AiGeneratorSource.NOTES_TEXT) }
    var subject by remember { mutableStateOf("Physics") }
    var topic by remember { mutableStateOf("") }
    var notesTitle by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var cardCount by remember { mutableIntStateOf(6) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        selectedFileName = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                selectedFileName = "Selected Document"
            }
        }
    }

    val presetTopics = remember(subject) {
        when {
            subject.contains("Physics", ignoreCase = true) -> listOf(
                "Photoelectric Effect & Quantum Physics",
                "Electromagnetic Induction & Lenz's Law",
                "Thermodynamics & Heat Engines",
                "Rotational Dynamics & Moment of Inertia"
            )
            subject.contains("Chemistry", ignoreCase = true) -> listOf(
                "Organic Reaction Mechanisms (SN1 vs SN2)",
                "Chemical Kinetics & Rate Laws",
                "Coordination Compounds & Hybridization",
                "Electrochemistry & Nernst Equation"
            )
            subject.contains("Math", ignoreCase = true) -> listOf(
                "Integration by Parts & Trigonometric Substitutions",
                "Matrices & Determinant Properties",
                "Vectors & 3D Geometry Equations",
                "Differential Equations & Integrating Factor"
            )
            else -> listOf(
                "Genetics & DNA Replication",
                "Cell Respiration & Krebs Cycle",
                "Plant Physiology & Photosynthesis",
                "Data Structures & Complexity"
            )
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 8.dp,
        fillAlpha = if (isDark) 0.75f else 0.9f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Gemini Flashcard Creator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Generate active-recall spaced flashcards from notes, docs, or topics",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            // Source Switcher Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0x301E293B) else Color(0x60E2E8F0))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AiGeneratorSource.entries.forEach { source ->
                    val isSelected = activeSource == source
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonCyan else Color.Transparent)
                            .clickable { activeSource = source }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = source.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else (if (isDark) Color.White else Color(0xFF0F172A)),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = source.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else (if (isDark) Color.White else Color(0xFF0F172A))
                            )
                        }
                    }
                }
            }

            // Subject Selector
            Text(
                text = "Target Subject",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Physics", "Chemistry", "Mathematics", "Biology").forEach { subj ->
                    val isSelected = subject == subj
                    FilterChip(
                        selected = isSelected,
                        onClick = { subject = subj },
                        label = { Text(subj, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = if (isDark) Color(0x301E293B) else Color(0xFFE2E8F0),
                            labelColor = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    )
                }
            }

            // Source-specific inputs
            when (activeSource) {
                AiGeneratorSource.NOTES_TEXT -> {
                    Text(
                        text = "Notes Title / Chapter",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    OutlinedTextField(
                        value = notesTitle,
                        onValueChange = { notesTitle = it },
                        modifier = Modifier.fillMaxWidth().testTag("ai_notes_title_input"),
                        placeholder = { Text("e.g. Thermodynamics Chapter Summary") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "Paste Study Notes or Lecture Summary",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        modifier = Modifier.fillMaxWidth().testTag("ai_notes_body_input"),
                        placeholder = { Text("Paste textbook excerpt, revision notes, derivations, or formulas here...") },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                AiGeneratorSource.DOCUMENT_FILE -> {
                    Text(
                        text = "Select Document (PDF / TXT / Markdown)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                docPickerLauncher.launch(
                                    arrayOf("application/pdf", "text/plain", "text/markdown", "*/*")
                                )
                            }
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        color = if (isDark) Color(0x301E293B) else Color(0x50E2E8F0)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.UploadFile,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedFileName ?: "Tap to choose a study document",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedFileName != null) NeonCyan else (if (isDark) Color.White else Color(0xFF0F172A)),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Gemini will extract key concepts and form questions with spaced review schedules",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                AiGeneratorSource.TOPIC_CONCEPT -> {
                    Text(
                        text = "Chapter or Concept Topic",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        modifier = Modifier.fillMaxWidth().testTag("ai_deck_topic_input"),
                        placeholder = { Text("e.g. Electromagnetism or Thermodynamics") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "Popular Exam Topics:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetTopics.forEach { preset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0x201E293B) else Color(0x60E2E8F0))
                                    .clickable { topic = preset }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddCircleOutline,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }

            // Card Count Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cards to Generate: $cardCount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(4, 6, 8, 10).forEach { count ->
                        val isSelected = cardCount == count
                        FilterChip(
                            selected = isSelected,
                            onClick = { cardCount = count },
                            label = { Text("$count", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Button
            if (isGenerating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x3038BDF8)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Synthesizing Flashcards with Gemini...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            } else {
                GlassButton(
                    text = when (activeSource) {
                        AiGeneratorSource.NOTES_TEXT -> "⚡ Generate from Notes"
                        AiGeneratorSource.DOCUMENT_FILE -> "⚡ Extract Cards from Document"
                        AiGeneratorSource.TOPIC_CONCEPT -> "⚡ Generate Flashcard Deck"
                    },
                    onClick = {
                        when (activeSource) {
                            AiGeneratorSource.NOTES_TEXT -> {
                                val title = notesTitle.ifBlank { "Study Notes" }
                                onGenerateNotes(title, notesText, subject, cardCount)
                            }
                            AiGeneratorSource.DOCUMENT_FILE -> {
                                selectedUri?.let { onGenerateDocument(it, subject, cardCount) }
                            }
                            AiGeneratorSource.TOPIC_CONCEPT -> {
                                val finalTopic = topic.ifBlank { presetTopics.first() }
                                onGenerateTopic(subject, finalTopic)
                            }
                        }
                    },
                    icon = Icons.Filled.AutoAwesome,
                    isPrimary = true,
                    testTag = "generate_ai_deck_btn"
                )
            }
        }
    }
}

@Composable
private fun FlashcardEditDialog(
    existingCard: FlashcardItem?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (subject: String, topic: String, front: String, back: String, hint: String, difficulty: String, sourceDoc: String) -> Unit
) {
    var subject by remember { mutableStateOf(existingCard?.subject ?: "Physics") }
    var topic by remember { mutableStateOf(existingCard?.topic ?: "") }
    var front by remember { mutableStateOf(existingCard?.front ?: "") }
    var back by remember { mutableStateOf(existingCard?.back ?: "") }
    var hint by remember { mutableStateOf(existingCard?.hint ?: "") }
    var difficulty by remember { mutableStateOf(existingCard?.difficulty ?: "Medium") }
    var sourceDoc by remember { mutableStateOf(existingCard?.sourceDocTitle ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF0F172A) else Color.White)
                .border(
                    1.dp,
                    if (isDark) Color(0x4038BDF8) else Color(0xFFCBD5E1),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingCard != null) "Edit Flashcard" else "Create New Flashcard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                // Subject row
                Text(
                    text = "Subject",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Physics", "Chemistry", "Mathematics", "Biology").forEach { subj ->
                        val isSelected = subject == subj
                        FilterChip(
                            selected = isSelected,
                            onClick = { subject = subj },
                            label = { Text(subj, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = if (isDark) Color(0x301E293B) else Color(0xFFE2E8F0),
                                labelColor = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        )
                    }
                }

                // Topic field
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Chapter / Topic") },
                    placeholder = { Text("e.g. Electromagnetism or Calculus") },
                    modifier = Modifier.fillMaxWidth().testTag("flashcard_dialog_topic"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Front
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front: Question / Formula / Concept") },
                    placeholder = { Text("State Lenz's law and its conservation law...") },
                    modifier = Modifier.fillMaxWidth().testTag("flashcard_dialog_front"),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                // Back
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back: Answer / Derivation / Key Points") },
                    placeholder = { Text("The induced EMF opposes the change in flux...") },
                    modifier = Modifier.fillMaxWidth().testTag("flashcard_dialog_back"),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )

                // Hint
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Hint / Memory Clue (Optional)") },
                    placeholder = { Text("e.g. Conservation of Energy in closed loop") },
                    modifier = Modifier.fillMaxWidth().testTag("flashcard_dialog_hint"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Source Document (Optional)
                OutlinedTextField(
                    value = sourceDoc,
                    onValueChange = { sourceDoc = it },
                    label = { Text("Source Document / Notes Ref (Optional)") },
                    placeholder = { Text("e.g. Chapter 4 Lecture Notes") },
                    modifier = Modifier.fillMaxWidth().testTag("flashcard_dialog_source_doc"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Difficulty selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Difficulty:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Easy", "Medium", "Hard").forEach { diff ->
                            val isSelected = difficulty == diff
                            val diffColor = when (diff) {
                                "Easy" -> EmeraldSuccess
                                "Medium" -> GoldenSpark
                                else -> CoralRose
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { difficulty = diff },
                                label = { Text(diff, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = diffColor,
                                    selectedLabelColor = Color.Black,
                                    containerColor = if (isDark) Color(0x201E293B) else Color(0xFFE2E8F0),
                                    labelColor = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (front.isNotBlank() && back.isNotBlank()) {
                                onSave(subject, topic.ifBlank { "Core Syllabus" }, front, back, hint, difficulty, sourceDoc)
                            }
                        },
                        enabled = front.isNotBlank() && back.isNotBlank(),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("flashcard_dialog_save_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (existingCard != null) "Update Card" else "Save Card",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectBadge(subject: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x3038BDF8))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = subject,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DifficultyBadge(difficulty: String) {
    val color = when (difficulty.lowercase()) {
        "easy" -> EmeraldSuccess
        "hard" -> CoralRose
        else -> GoldenSpark
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = difficulty,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StatusBadge(status: RevisionCategory) {
    val (label, color) = when (status) {
        RevisionCategory.STRONG -> "Strong" to EmeraldSuccess
        RevisionCategory.PRACTICE_SOON -> "Practice" to GoldenSpark
        RevisionCategory.REVISE_NOW -> "Revise" to CoralRose
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "● $label",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 11.sp
        )
    }
}

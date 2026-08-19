package com.example.ui.screens.planner

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.screens.flashcards.FlashcardScreen
import com.example.ui.theme.*

@Composable
fun StudyPlannerScreen(
    planItems: List<StudyPlanItem>,
    flashcards: List<FlashcardItem>,
    user: UserProfile?,
    isGenerating: Boolean,
    onGenerateAiPlan: () -> Unit,
    onTogglePlanItem: (Long, Boolean) -> Unit,
    onAddPlanItem: (subject: String, chapter: String, topic: String, minutes: Int, priority: PlanPriority) -> Unit,
    onUpdatePlanItem: (StudyPlanItem) -> Unit = {},
    onDeletePlanItem: (Long) -> Unit,
    onStartFocusSession: (subject: String, topic: String) -> Unit,
    onRecoverMissedSessions: (mode: String) -> Unit = {},
    onUpdateDailyAvailableTime: (Int) -> Unit = {},
    onAddFlashcard: (subject: String, topic: String, front: String, back: String, hint: String, difficulty: String, sourceDoc: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onUpdateFlashcard: (FlashcardItem) -> Unit = {},
    onDeleteFlashcard: (Long) -> Unit = {},
    onReviewFlashcard: (id: Long, status: RevisionCategory, confidence: Int) -> Unit = { _, _, _ -> },
    onReviewSpaced: (id: Long, ratingQuality: Int) -> Unit = { _, _ -> },
    onGenerateAiCards: (subject: String, topic: String) -> Unit = { _, _ -> },
    onGenerateFromNotes: (title: String, notesText: String, subject: String, count: Int) -> Unit = { _, _, _, _ -> },
    onGenerateFromDocumentUri: (android.net.Uri, subject: String, count: Int) -> Unit = { _, _, _ -> },
    flashcardStatusMessage: String? = null,
    onClearFlashcardStatusMessage: () -> Unit = {},
    isFlashcardGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Study Plan, 1: Digital Flashcards, 2: Exam Roadmap
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlanItem by remember { mutableStateOf<StudyPlanItem?>(null) }
    var previewingPlanItems by remember { mutableStateOf<List<StudyPlanItem>?>(null) }
    var selectedTodayMinutes by remember { mutableIntStateOf(user?.dailyTargetMinutes ?: 180) }

    var newSubject by remember { mutableStateOf(user?.subjects?.firstOrNull() ?: "Physics") }
    var newChapter by remember { mutableStateOf("") }
    var newTopic by remember { mutableStateOf("") }
    var newMinutes by remember { mutableIntStateOf(45) }
    var newPriority by remember { mutableStateOf(PlanPriority.HIGH) }

    val totalPendingMinutes = remember(planItems) {
        planItems.filter { !it.isCompleted }.sumOf { it.targetMinutes }
    }
    val missedCount = remember(planItems) {
        planItems.count { !it.isCompleted && it.priority == PlanPriority.HIGH }
    }

    if (selectedSection == 1) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(appBackgroundGradient(isDark))
        ) {
            // Section Switcher Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0x301E293B) else Color(0x60E2E8F0))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Plan Timetable", "Digital Flashcards", "Exam Roadmap").forEachIndexed { idx, label ->
                    val isSelected = selectedSection == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .springClickable(testTag = "planner_tab_$idx") {
                                selectedSection = idx
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color(0xFF070B19) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            FlashcardScreen(
                flashcards = flashcards,
                isAiGenerating = isFlashcardGenerating,
                onAddFlashcard = onAddFlashcard,
                onUpdateFlashcard = onUpdateFlashcard,
                onDeleteFlashcard = onDeleteFlashcard,
                onReviewFlashcard = onReviewFlashcard,
                onReviewSpaced = onReviewSpaced,
                onGenerateAiCards = onGenerateAiCards,
                onGenerateFromNotes = onGenerateFromNotes,
                onGenerateFromDocumentUri = onGenerateFromDocumentUri,
                statusMessage = flashcardStatusMessage,
                onClearStatusMessage = onClearFlashcardStatusMessage,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundGradient(isDark))
            .padding(horizontal = 18.dp)
            .testTag("study_planner_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Section Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📚 Study Planner & Revision",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "AI Adaptive Timetable & Spaced Recall",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeToggleButton(testTag = "planner_theme_toggle")

                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x3038BDF8))
                            .testTag("add_custom_plan_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Task", tint = NeonCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0x301E293B) else Color(0x60E2E8F0))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Plan Timetable", "Digital Flashcards", "Exam Roadmap").forEachIndexed { idx, label ->
                    val isSelected = selectedSection == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .springClickable(testTag = "planner_tab_$idx") {
                                selectedSection = idx
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color(0xFF070B19) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        when (selectedSection) {
            0 -> {
                // --- SECTION 0: STUDY PLAN TIMETABLE ---

                // Daily Availability Check-In
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.75f,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Today's Study Availability",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "${selectedTodayMinutes / 60}h ${selectedTodayMinutes % 60}m limit",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(30 to "30m", 60 to "1h", 120 to "2h", 180 to "3h", 240 to "4h").forEach { (mins, label) ->
                                    val isSel = selectedTodayMinutes == mins
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            selectedTodayMinutes = mins
                                            onUpdateDailyAvailableTime(mins)
                                        },
                                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonCyan,
                                            selectedLabelColor = Color(0xFF070B19),
                                            containerColor = Color(0x301E293B),
                                            labelColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Plan Transparency Warning if work > available time
                if (totalPendingMinutes > selectedTodayMinutes) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CoralRose.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, CoralRose.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = CoralRose, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Schedule Transparency Notice",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CoralRose
                                    )
                                    Text(
                                        text = "Your pending tasks (${totalPendingMinutes}m) exceed today's target (${selectedTodayMinutes}m). Nova will spread remaining sessions smoothly across upcoming days without overloading tomorrow.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }
                }

                // Missed Session Recovery Bar
                if (missedCount > 0) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            fillAlpha = 0.85f,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ScheduleSend, contentDescription = null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Missed / Overdue Session Recovery",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "$missedCount High Priority",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldenSpark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onRecoverMissedSessions("LATER_TODAY") },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF070B19)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 6.dp)
                                    ) {
                                        Text("Move Later Today", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onRecoverMissedSessions("SPREAD_WEEK") },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 6.dp)
                                    ) {
                                        Text("Spread Across Week", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    GlassButton(
                        text = "⚡ Regenerate AI Plan with Gemini",
                        onClick = onGenerateAiPlan,
                        icon = Icons.Filled.AutoAwesome,
                        isPrimary = false,
                        isLoading = isGenerating,
                        testTag = "generate_ai_plan_btn"
                    )
                }

                if (planItems.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            fillAlpha = 0.6f
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.EventNote, null, tint = NeonCyan, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No study plan created yet.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap the button above to let Gemini build your balanced curriculum schedule!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(planItems, key = { it.id }) { item ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            fillAlpha = if (item.isCompleted) 0.35f else 0.75f
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when (item.priority) {
                                                PlanPriority.HIGH -> CoralRose.copy(alpha = 0.2f)
                                                PlanPriority.MEDIUM -> GoldenSpark.copy(alpha = 0.2f)
                                                PlanPriority.LOW -> EmeraldSuccess.copy(alpha = 0.2f)
                                            }
                                        ) {
                                            Text(
                                                text = item.priority.name,
                                                color = when (item.priority) {
                                                    PlanPriority.HIGH -> CoralRose
                                                    PlanPriority.MEDIUM -> GoldenSpark
                                                    PlanPriority.LOW -> EmeraldSuccess
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = item.subject,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = "• ${item.targetMinutes} min",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onStartFocusSession(item.subject, item.topic) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.PlayCircle, "Focus", tint = NeonCyan)
                                        }

                                        IconButton(
                                            onClick = { editingPlanItem = item },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.Edit, "Edit", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                                        }

                                        Checkbox(
                                            checked = item.isCompleted,
                                            onCheckedChange = { onTogglePlanItem(item.id, !item.isCompleted) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = EmeraldSuccess,
                                                checkmarkColor = Color(0xFF070B19)
                                            )
                                        )

                                        IconButton(
                                            onClick = { onDeletePlanItem(item.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.Delete, "Delete", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${item.chapter} — ${item.topic}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (item.isCompleted) Color(0xFF94A3B8) else Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.Medium
                                )

                                if (item.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "💡 ${item.notes}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // --- SECTION 2: EXAM MODE COUNTDOWN & ROADMAP ---
                item {
                    val daysRemaining = remember(user?.examDateMillis) {
                        val diff = (user?.examDateMillis ?: (System.currentTimeMillis() + 30L * 86400000)) - System.currentTimeMillis()
                        (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1).toInt()
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 10.dp,
                        fillAlpha = 0.85f
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.EmojiEvents, null, tint = GoldenSpark, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = user?.examName?.ifBlank { "Board & Competitive Exam" } ?: "Board & Competitive Exam",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🎯 $daysRemaining Days Remaining",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = CoralRose
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Target: Complete 1 Full Mock Test & 2 Chapter Revisions per week.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Milestone Roadmap 🗺️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                items(
                    listOf(
                        Triple("Phase 1: High-Weightage Chapters", "Physics Electromagnetism & Math Calculus mastery", true),
                        Triple("Phase 2: Targeted PYQ Drills", "Solve 10 years of authorized previous questions", false),
                        Triple("Phase 3: Mistake Book Liquidation", "Eliminate frequent conceptual and sign errors", false),
                        Triple("Phase 4: Full-Length Timed Mocks", "Simulated exam conditions and speed strategies", false)
                    )
                ) { (title, desc, isCurrent) ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        fillAlpha = if (isCurrent) 0.8f else 0.45f
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) NeonCyan else Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NeonCyan.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "In Progress",
                                        color = NeonCyan,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Plan Item Dialog
    editingPlanItem?.let { itemToEdit ->
        var editSubject by remember { mutableStateOf(itemToEdit.subject) }
        var editChapter by remember { mutableStateOf(itemToEdit.chapter) }
        var editTopic by remember { mutableStateOf(itemToEdit.topic) }
        var editMinutes by remember { mutableIntStateOf(itemToEdit.targetMinutes) }
        var editNotes by remember { mutableStateOf(itemToEdit.notes) }

        AlertDialog(
            onDismissRequest = { editingPlanItem = null },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Edit Study Session", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editSubject,
                        onValueChange = { editSubject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editChapter,
                        onValueChange = { editChapter = it },
                        label = { Text("Chapter") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editTopic,
                        onValueChange = { editTopic = it },
                        label = { Text("Topic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Duration: ${editMinutes} min", color = Color.White, fontSize = 14.sp)
                        Row {
                            IconButton(onClick = { if (editMinutes > 15) editMinutes -= 15 }) {
                                Icon(Icons.Default.RemoveCircleOutline, null, tint = NeonCyan)
                            }
                            IconButton(onClick = { editMinutes += 15 }) {
                                Icon(Icons.Default.AddCircleOutline, null, tint = NeonCyan)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes / Strategy") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePlanItem(
                            itemToEdit.copy(
                                subject = editSubject,
                                chapter = editChapter,
                                topic = editTopic,
                                targetMinutes = editMinutes,
                                notes = editNotes
                            )
                        )
                        editingPlanItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19))
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlanItem = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Add Custom Plan Item Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Add Study Target", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newSubject,
                        onValueChange = { newSubject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newChapter,
                        onValueChange = { newChapter = it },
                        label = { Text("Chapter") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newTopic,
                        onValueChange = { newTopic = it },
                        label = { Text("Topic / Activity") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubject.isNotBlank() && newChapter.isNotBlank()) {
                            onAddPlanItem(newSubject, newChapter, newTopic.ifBlank { "Core Practice" }, newMinutes, newPriority)
                            showAddDialog = false
                            newChapter = ""
                            newTopic = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19))
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
fun RevisionStatusCard(
    title: String,
    badgeColor: Color,
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassEffect(shape = RoundedCornerShape(16.dp), fillAlpha = 0.6f)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}

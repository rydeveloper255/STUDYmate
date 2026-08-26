package com.example.ui.screens.planner

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyScheduleItem
import com.example.data.model.StudyScheduleLog
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

val WarmAmber = Color(0xFFF59E0B)
val SoftPurple = Color(0xFF8B5CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScheduleScreen(
    scheduleItems: List<StudyScheduleItem>,
    scheduleLogs: List<StudyScheduleLog>,
    isSchedulePaused: Boolean,
    onSaveScheduleItem: (StudyScheduleItem) -> Unit,
    onDeleteScheduleItem: (String) -> Unit,
    onToggleGlobalPause: () -> Unit,
    onRescheduleMissed: (logId: String, newDateMillis: Long, newTime: String) -> Unit,
    onSkipMissed: (logId: String) -> Unit,
    onStartFocusSession: (subject: String, topic: String, minutes: Int, isStrict: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var selectedDay by remember { mutableStateOf("MON") }
    val daysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<StudyScheduleItem?>(null) }
    var showDeleteConfirmId by remember { mutableStateOf<String?>(null) }
    var showSmartSuggestion by remember { mutableStateOf(true) }

    // Filter schedule items for selected day
    val dayItems = remember(scheduleItems, selectedDay) {
        scheduleItems.filter { item ->
            item.dayOfWeek.equals(selectedDay, ignoreCase = true) ||
                    item.repeatType == "DAILY" ||
                    item.repeatDaysJson.contains(selectedDay, ignoreCase = true)
        }.sortedBy { it.startTime }
    }

    // Missed session logs
    val missedLogs = remember(scheduleLogs) {
        scheduleLogs.filter { it.status == "MISSED" || it.status == "PLANNED" }
    }

    // Analytics calculations
    val totalPlannedMins = remember(scheduleItems) { scheduleItems.sumOf { it.durationMinutes } }
    val totalCompletedMins = remember(scheduleLogs) { scheduleLogs.filter { it.status == "COMPLETED" }.sumOf { it.actualMinutesSpent } }
    val completionPercent = if (totalPlannedMins > 0) ((totalCompletedMins.toFloat() / totalPlannedMins.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📅 Study Schedule",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        if (isSchedulePaused) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WarmAmber.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WarmAmber)
                            ) {
                                Text(
                                    text = "PAUSED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("schedule_back_btn")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = if (isDark) Color.White else Color(0xFF0F172A))
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleGlobalPause,
                        modifier = Modifier.testTag("toggle_global_schedule_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (isSchedulePaused) Icons.Filled.PlayCircle else Icons.Filled.PauseCircle,
                            contentDescription = "Toggle Schedule Pause",
                            tint = if (isSchedulePaused) EmeraldSuccess else WarmAmber
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItem = null
                    showAddDialog = true
                },
                containerColor = NeonCyan,
                contentColor = Color(0xFF0F172A),
                shape = CircleShape,
                modifier = Modifier
                    .shadow(12.dp, CircleShape, spotColor = NeonCyan)
                    .testTag("add_schedule_item_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Session")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // -----------------------------------------------------------------
            // 1. WEEKLY PROGRESS & ANALYTICS HEADER
            // -----------------------------------------------------------------
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    fillAlpha = if (isDark) 0.5f else 0.8f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Weekly Schedule Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Planned vs Actual Study Time",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NeonCyan.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "$completionPercent% Done",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { completionPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NeonCyan,
                            trackColor = if (isDark) Color(0x20FFFFFF) else Color(0x15000000)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Schedule, contentDescription = null, tint = SoftPurple, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Planned: ${totalPlannedMins / 60}h ${totalPlannedMins % 60}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Completed: ${totalCompletedMins / 60}h ${totalCompletedMins % 60}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2. MISSED SESSION ALERT BANNER (If any)
            // -----------------------------------------------------------------
            if (missedLogs.isNotEmpty()) {
                item {
                    val log = missedLogs.first()
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        borderColor = CoralRose.copy(alpha = 0.6f),
                        fillAlpha = 0.6f
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CoralRose.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.NotificationImportant, contentDescription = null, tint = CoralRose)
                            }
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Missed Study Session",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${log.subject} • Scheduled for ${log.scheduledStartTime}",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = { onSkipMissed(log.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Skip", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                }
                                Button(
                                    onClick = { onRescheduleMissed(log.id, System.currentTimeMillis() + 86400000L, "07:00 PM") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Reschedule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 3. SMART NOVA RECOMMENDATION CARD
            // -----------------------------------------------------------------
            if (showSmartSuggestion) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        borderColor = GoldenSpark.copy(alpha = 0.5f),
                        fillAlpha = 0.5f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Nova Smart Schedule Recommendation",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldenSpark
                                )
                            }

                            Text(
                                text = "Reasoning speed can improve with focused practice. Would you like to schedule a 30-minute session for tomorrow at 7:00 PM?",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showSmartSuggestion = false }) {
                                    Text("Not Now", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onSaveScheduleItem(
                                            StudyScheduleItem(
                                                dayOfWeek = "WED",
                                                startTime = "07:00 PM",
                                                endTime = "07:30 PM",
                                                durationMinutes = 30,
                                                subject = "Reasoning",
                                                topic = "Logical Reasoning Sprint",
                                                isAutoFocus = true,
                                                isStrictMode = false,
                                                repeatType = "WEEKLY"
                                            )
                                        )
                                        showSmartSuggestion = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Add to Schedule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 4. DAY OF WEEK SELECTOR TABS
            // -----------------------------------------------------------------
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(daysOfWeek) { day ->
                        val isSelected = selectedDay == day
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) NeonCyan else if (isDark) Color(0x15FFFFFF) else Color(0x10000000),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan else if (isDark) Color(0x25FFFFFF) else Color(0x15000000)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedDay = day }
                                .testTag("day_tab_$day")
                        ) {
                            Text(
                                text = day,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF0F172A) else if (isDark) Color.White else Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 5. TIMETABLE SESSION LIST FOR SELECTED DAY
            // -----------------------------------------------------------------
            if (dayItems.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        fillAlpha = 0.3f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.EventBusy, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Study Sessions Scheduled for $selectedDay",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the '+' button below to create your study schedule.",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(dayItems, key = { it.id }) { item ->
                    ScheduleItemCard(
                        item = item,
                        isDark = isDark,
                        onEdit = {
                            editingItem = item
                            showAddDialog = true
                        },
                        onDelete = { showDeleteConfirmId = item.id },
                        onTogglePause = {
                            onSaveScheduleItem(item.copy(isPaused = !item.isPaused))
                        },
                        onStartNow = {
                            onStartFocusSession(item.subject, item.topic, item.durationMinutes, item.isStrictMode)
                        }
                    )
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // ADD / EDIT DIALOG WITH CONFLICT DETECTION
    // -----------------------------------------------------------------
    if (showAddDialog) {
        AddEditScheduleDialog(
            existingItem = editingItem,
            defaultDay = selectedDay,
            allScheduleItems = scheduleItems,
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                onSaveScheduleItem(newItem)
                showAddDialog = false
            }
        )
    }

    // DELETE CONFIRMATION DIALOG
    if (showDeleteConfirmId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmId = null },
            title = { Text("Delete Scheduled Session?") },
            text = { Text("Are you sure you want to remove this session from your study timetable?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmId?.let { onDeleteScheduleItem(it) }
                        showDeleteConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ScheduleItemCard(
    item: StudyScheduleItem,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePause: () -> Unit,
    onStartNow: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        fillAlpha = if (item.isPaused) 0.3f else if (isDark) 0.5f else 0.8f
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeonCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${item.startTime} – ${item.endTime}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (item.isPaused) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PAUSED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmAmber)
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = CoralRose.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Column {
                Text(
                    text = item.subject,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                if (item.topic.isNotBlank()) {
                    Text(
                        text = item.topic,
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                }
            }

            // Badges row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.isAutoFocus) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldSuccess.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "⚡ Auto Focus",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.isStrictMode) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CoralRose.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "🔒 Strict Mode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralRose,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SoftPurple.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "🛡️ ${item.blockedAppsCount} Apps Blocked",
                        fontSize = 11.sp,
                        color = SoftPurple,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onTogglePause) {
                    Text(if (item.isPaused) "Resume Session" else "Pause Session", fontSize = 12.sp, color = WarmAmber)
                }

                Button(
                    onClick = onStartNow,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScheduleDialog(
    existingItem: StudyScheduleItem?,
    defaultDay: String,
    allScheduleItems: List<StudyScheduleItem>,
    onDismiss: () -> Unit,
    onSave: (StudyScheduleItem) -> Unit
) {
    val isDark = isAppInDarkTheme()
    var selectedDay by remember { mutableStateOf(existingItem?.dayOfWeek ?: defaultDay) }
    var startTime by remember { mutableStateOf(existingItem?.startTime ?: "07:00 PM") }
    var endTime by remember { mutableStateOf(existingItem?.endTime ?: "08:00 PM") }
    var subject by remember { mutableStateOf(existingItem?.subject ?: "Mathematics") }
    var topic by remember { mutableStateOf(existingItem?.topic ?: "") }
    var isAutoFocus by remember { mutableStateOf(existingItem?.isAutoFocus ?: true) }
    var isStrictMode by remember { mutableStateOf(existingItem?.isStrictMode ?: false) }
    var repeatType by remember { mutableStateOf(existingItem?.repeatType ?: "WEEKLY") }

    val daysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val defaultSubjects = listOf("Mathematics", "Reasoning", "English", "General Science", "General Knowledge")

    // Check conflict
    val hasConflict = remember(selectedDay, startTime, endTime, existingItem) {
        allScheduleItems.any { item ->
            item.id != existingItem?.id &&
                    item.dayOfWeek.equals(selectedDay, ignoreCase = true) &&
                    item.startTime.equals(startTime, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingItem == null) "Add Study Session" else "Edit Study Session",
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Day selector
                Text("Day of Week", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(daysOfWeek) { day ->
                        val sel = selectedDay == day
                        FilterChip(
                            selected = sel,
                            onClick = { selectedDay = day },
                            label = { Text(day, fontSize = 11.sp) }
                        )
                    }
                }

                // Times
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                if (hasConflict) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CoralRose.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = CoralRose, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚠️ Time conflict with an existing session on $selectedDay",
                                fontSize = 11.sp,
                                color = CoralRose,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Subject Dropdown / Selection
                Text("Subject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(defaultSubjects) { subj ->
                        val sel = subject == subj
                        FilterChip(
                            selected = sel,
                            onClick = { subject = subj },
                            label = { Text(subj, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto Focus Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Automatically launches session at start time", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = isAutoFocus,
                        onCheckedChange = { isAutoFocus = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Strict Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Locks app blocking during session", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = isStrictMode,
                        onCheckedChange = { isStrictMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CoralRose)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = existingItem?.copy(
                        dayOfWeek = selectedDay,
                        startTime = startTime,
                        endTime = endTime,
                        subject = subject,
                        topic = topic,
                        isAutoFocus = isAutoFocus,
                        isStrictMode = isStrictMode,
                        repeatType = repeatType
                    ) ?: StudyScheduleItem(
                        dayOfWeek = selectedDay,
                        startTime = startTime,
                        endTime = endTime,
                        durationMinutes = 60,
                        subject = subject,
                        topic = topic,
                        isAutoFocus = isAutoFocus,
                        isStrictMode = isStrictMode,
                        repeatType = repeatType
                    )
                    onSave(item)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0F172A))
            ) {
                Text("Save Session", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
        shape = RoundedCornerShape(22.dp)
    )
}

package com.example.ui.screens.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.data.model.NotificationPreference
import com.example.data.model.UserProfile
import com.example.notification.AppNotificationSettings
import com.example.notification.StudyNotificationManager
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    user: UserProfile?,
    currentPrefs: NotificationPreference,
    onSavePrefs: (NotificationPreference) -> Unit,
    onBack: () -> Unit,
    onTestStudyReminder: () -> Unit = {},
    onTestExamCountdown: () -> Unit = {},
    onTestDailyGoal: () -> Unit = {},
    onTestMissedStudy: () -> Unit = {},
    onTestBreakReminder: () -> Unit = {},
    onTestFocusStarted: () -> Unit = {},
    onTestFocusCompleted: () -> Unit = {},
    onTestDailyMotivation: () -> Unit = {}
) {
    val context = LocalContext.current
    var hasPostNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPostNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted! 🔔", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notifications may be blocked by system settings", Toast.LENGTH_LONG).show()
        }
    }

    // Editable state
    var masterEnabled by remember(currentPrefs) { mutableStateOf(currentPrefs.masterEnabled) }
    var studyReminders by remember(currentPrefs) { mutableStateOf(currentPrefs.studyReminders) }
    var examCountdownAlerts by remember(currentPrefs) { mutableStateOf(currentPrefs.examCountdownAlerts) }
    var dailyGoalReminders by remember(currentPrefs) { mutableStateOf(currentPrefs.dailyGoalReminders) }
    var missedStudyReminders by remember(currentPrefs) { mutableStateOf(currentPrefs.missedStudyReminders) }
    var breakReminders by remember(currentPrefs) { mutableStateOf(currentPrefs.breakReminders) }
    var focusStartedAlerts by remember(currentPrefs) { mutableStateOf(currentPrefs.focusStartedAlerts) }
    var focusCompletedAlerts by remember(currentPrefs) { mutableStateOf(currentPrefs.focusCompletedAlerts) }
    var motivationalQuotes by remember(currentPrefs) { mutableStateOf(currentPrefs.motivationalQuotes) }

    var reminderHour by remember(currentPrefs) { mutableIntStateOf(currentPrefs.reminderHour) }
    var reminderMinute by remember(currentPrefs) { mutableIntStateOf(currentPrefs.reminderMinute) }
    var dailyGoalHour by remember(currentPrefs) { mutableIntStateOf(currentPrefs.dailyGoalHour) }
    var dailyGoalMinute by remember(currentPrefs) { mutableIntStateOf(currentPrefs.dailyGoalMinute) }
    var motivationHour by remember(currentPrefs) { mutableIntStateOf(currentPrefs.motivationHour) }
    var motivationMinute by remember(currentPrefs) { mutableIntStateOf(currentPrefs.motivationMinute) }

    var activeDays by remember(currentPrefs) { mutableStateOf(currentPrefs.activeDays) }
    var motivationFreq by remember(currentPrefs) { mutableStateOf(currentPrefs.motivationFrequency) }

    var quietHoursEnabled by remember(currentPrefs) { mutableStateOf(currentPrefs.quietHoursEnabled) }
    var quietStartHour by remember(currentPrefs) { mutableIntStateOf(currentPrefs.quietStartHour) }
    var quietStartMinute by remember(currentPrefs) { mutableIntStateOf(currentPrefs.quietStartMinute) }
    var quietEndHour by remember(currentPrefs) { mutableIntStateOf(currentPrefs.quietEndHour) }
    var quietEndMinute by remember(currentPrefs) { mutableIntStateOf(currentPrefs.quietEndMinute) }

    // Dialog pickers
    var showStudyTimePicker by remember { mutableStateOf(false) }
    var showGoalTimePicker by remember { mutableStateOf(false) }
    var showMotivationTimePicker by remember { mutableStateOf(false) }
    var showQuietStartTimePicker by remember { mutableStateOf(false) }
    var showQuietEndTimePicker by remember { mutableStateOf(false) }

    val studentName = user?.name?.ifBlank { "Rahul" } ?: "Rahul"
    val examName = user?.examName?.ifBlank { "JEE / Board Exam" } ?: "JEE / Board Exam"

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val motivationFrequencies = listOf(
        "Once Daily (Morning)",
        "Twice Daily (Morning & Evening)",
        "3x Daily (Morning, Noon, Night)",
        "After Focus Sessions"
    )

    fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, amPm)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notification & Reminder Center",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Smart schedules, countdowns & quiet hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val newPrefs = NotificationPreference(
                                masterEnabled = masterEnabled,
                                studyReminders = studyReminders,
                                examCountdownAlerts = examCountdownAlerts,
                                dailyGoalReminders = dailyGoalReminders,
                                missedStudyReminders = missedStudyReminders,
                                breakReminders = breakReminders,
                                focusStartedAlerts = focusStartedAlerts,
                                focusCompletedAlerts = focusCompletedAlerts,
                                motivationalQuotes = motivationalQuotes,
                                reminderHour = reminderHour,
                                reminderMinute = reminderMinute,
                                dailyGoalHour = dailyGoalHour,
                                dailyGoalMinute = dailyGoalMinute,
                                motivationHour = motivationHour,
                                motivationMinute = motivationMinute,
                                activeDays = activeDays,
                                motivationFrequency = motivationFreq,
                                quietHoursEnabled = quietHoursEnabled,
                                quietStartHour = quietStartHour,
                                quietStartMinute = quietStartMinute,
                                quietEndHour = quietEndHour,
                                quietEndMinute = quietEndMinute
                            )
                            onSavePrefs(newPrefs)
                            Toast.makeText(context, "Notification preferences saved & scheduled! ✅", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier.testTag("save_notifications_button")
                    ) {
                        Icon(Icons.Filled.Check, "Save", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B1021)
                )
            )
        },
        containerColor = Color(0xFF070B19)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Alert Banner if Android 13+ permission not granted
            if (!hasPostNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF3B1A1A))
                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.NotificationsOff, null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Notification Permission Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8A80)
                            )
                        }
                        Text(
                            text = "Android requires explicit permission for StudyMate AI to send you scheduled study alarms, exam countdowns, and break reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("grant_notification_permission_button")
                        ) {
                            Icon(Icons.Filled.NotificationsActive, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Notification Permission", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Master Switch Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (masterEnabled) NeonCyan.copy(alpha = 0.2f) else Color(0x20FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (masterEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                                contentDescription = null,
                                tint = if (masterEnabled) NeonCyan else Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (masterEnabled) "All Reminders Active 🔔" else "All Reminders Paused 🔕",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (masterEnabled) "StudyMate is actively managing your study schedules" else "No background alerts will be delivered",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Switch(
                        checked = masterEnabled,
                        onCheckedChange = { masterEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF070B19),
                            checkedTrackColor = NeonCyan
                        ),
                        modifier = Modifier.testTag("master_notification_switch")
                    )
                }
            }

            // =========================================================================
            // ⚡ Instant Notification Testing Station
            // =========================================================================
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bolt, null, tint = GoldenSpark, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Instant Notification Tester",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark
                            )
                        }
                        Text(
                            text = "Live preview on device",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Text(
                        text = "Tap any button below to immediately dispatch and test that exact personalized notification:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    // 8 Quick test buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TestNotifChip(
                                label = "1. Study Reminder",
                                icon = Icons.Filled.Alarm,
                                modifier = Modifier.weight(1f),
                                onClick = onTestStudyReminder
                            )
                            TestNotifChip(
                                label = "2. Exam Countdown",
                                icon = Icons.Filled.HourglassBottom,
                                modifier = Modifier.weight(1f),
                                onClick = onTestExamCountdown
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TestNotifChip(
                                label = "3. Daily Goal",
                                icon = Icons.Filled.TrackChanges,
                                modifier = Modifier.weight(1f),
                                onClick = onTestDailyGoal
                            )
                            TestNotifChip(
                                label = "4. Missed Study",
                                icon = Icons.Filled.Visibility,
                                modifier = Modifier.weight(1f),
                                onClick = onTestMissedStudy
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TestNotifChip(
                                label = "5. Break Reminder",
                                icon = Icons.Filled.SelfImprovement,
                                modifier = Modifier.weight(1f),
                                onClick = onTestBreakReminder
                            )
                            TestNotifChip(
                                label = "6. Focus Started",
                                icon = Icons.Filled.PlayArrow,
                                modifier = Modifier.weight(1f),
                                onClick = onTestFocusStarted
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TestNotifChip(
                                label = "7. Focus Finished (+XP)",
                                icon = Icons.Filled.Stars,
                                modifier = Modifier.weight(1f),
                                onClick = onTestFocusCompleted
                            )
                            TestNotifChip(
                                label = "8. Daily Motivation",
                                icon = Icons.Filled.AutoAwesome,
                                modifier = Modifier.weight(1f),
                                onClick = onTestDailyMotivation
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // Dynamic Personalized Preview Card
            // =========================================================================
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Preview, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Personalized Notification Preview",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.School, null, tint = GoldenSpark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("StudyMate AI • Now", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            }
                            Text(
                                text = "Hey $studentName 👋, your ${formatTime(reminderHour, reminderMinute)} study session starts now 📚🔥",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Only 30 days left for your $examName ⏳. One focused session today can make a difference. 💙",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "[ Start Focus ]",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "[ Snooze 15m ]",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // Timing & Scheduling Configuration
            // =========================================================================
            Text(
                text = "Reminder Timings & Days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Study Session Time Picker Tile
            TimeSettingTile(
                title = "Study Session Reminder Time",
                subtitle = "Triggers right when your scheduled study session is due",
                timeDisplay = formatTime(reminderHour, reminderMinute),
                icon = Icons.Filled.AccessTime,
                onClick = { showStudyTimePicker = true }
            )

            // Days of the Week Selector
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Reminder Days",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "All",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                modifier = Modifier.clickable { activeDays = daysOfWeek.toSet() }
                            )
                            Text("•", color = Color(0xFF64748B))
                            Text(
                                text = "Weekdays",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                modifier = Modifier.clickable { activeDays = setOf("Mon", "Tue", "Wed", "Thu", "Fri") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysOfWeek.forEach { day ->
                            val isSelected = activeDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) NeonCyan else Color(0x20FFFFFF))
                                    .clickable {
                                        activeDays = if (isSelected) {
                                            if (activeDays.size > 1) activeDays - day else activeDays
                                        } else {
                                            activeDays + day
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.take(2),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF070B19) else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Daily Goal Reminder Time
            TimeSettingTile(
                title = "Daily Study Goal Check-in Time",
                subtitle = "Evening progress report against your target minutes",
                timeDisplay = formatTime(dailyGoalHour, dailyGoalMinute),
                icon = Icons.Filled.TrackChanges,
                onClick = { showGoalTimePicker = true }
            )

            // Motivation Timing & Frequency
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Motivation Timing",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Primary morning quote time",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x30FFFFFF))
                                .clickable { showMotivationTimePicker = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = formatTime(motivationHour, motivationMinute),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0x20FFFFFF))

                    Text(
                        text = "Motivation Frequency",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        motivationFrequencies.forEach { freq ->
                            val isSelected = motivationFreq == freq
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color(0x10FFFFFF))
                                    .border(1.dp, if (isSelected) NeonCyan else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable { motivationFreq = freq }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = freq,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 8 Individual Notification Type Toggles
            // =========================================================================
            Text(
                text = "Notification Types & Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            NotificationTypeSwitchTile(
                title = "1. Study Session Reminder",
                description = "Personalized alerts when your scheduled study session begins",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                checked = studyReminders,
                onCheckedChange = { studyReminders = it }
            )

            NotificationTypeSwitchTile(
                title = "2. Exam Countdown Reminder",
                description = "Days remaining countdowns and high-yield topic prompts",
                icon = Icons.Filled.HourglassTop,
                checked = examCountdownAlerts,
                onCheckedChange = { examCountdownAlerts = it }
            )

            NotificationTypeSwitchTile(
                title = "3. Daily Study Goal Reminder",
                description = "Daily target minute checks and mission reminders",
                icon = Icons.Filled.TrackChanges,
                checked = dailyGoalReminders,
                onCheckedChange = { dailyGoalReminders = it }
            )

            NotificationTypeSwitchTile(
                title = "4. Missed-Study Reminder",
                description = "Gentle, non-intrusive nudges if you missed your scheduled study slot",
                icon = Icons.Filled.Visibility,
                checked = missedStudyReminders,
                onCheckedChange = { missedStudyReminders = it }
            )

            NotificationTypeSwitchTile(
                title = "5. Break Reminder",
                description = "Prompts to stretch, hydrate, and relax your eyes after study sprints",
                icon = Icons.Filled.SelfImprovement,
                checked = breakReminders,
                onCheckedChange = { breakReminders = it }
            )

            NotificationTypeSwitchTile(
                title = "6. Focus Session Started Alert",
                description = "Live ongoing status banner when deep focus timer is active",
                icon = Icons.Filled.PlayCircle,
                checked = focusStartedAlerts,
                onCheckedChange = { focusStartedAlerts = it }
            )

            NotificationTypeSwitchTile(
                title = "7. Focus Session Completed Alert",
                description = "XP rewards and session summary right after finishing a focus session",
                icon = Icons.Filled.Stars,
                checked = focusCompletedAlerts,
                onCheckedChange = { focusCompletedAlerts = it }
            )

            NotificationTypeSwitchTile(
                title = "8. Daily Motivational Boost",
                description = "Inspiring study quotes personalized with your name and exam",
                icon = Icons.Filled.AutoAwesome,
                checked = motivationalQuotes,
                onCheckedChange = { motivationalQuotes = it }
            )

            // =========================================================================
            // Quiet Hours / Do Not Disturb
            // =========================================================================
            Text(
                text = "Quiet Hours & Anti-Spam",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bedtime, null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Quiet Hours (Do Not Disturb)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Mutes study reminders during sleep hours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        Switch(
                            checked = quietHoursEnabled,
                            onCheckedChange = { quietHoursEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF070B19), checkedTrackColor = Color(0xFFD0BCFF))
                        )
                    }

                    if (quietHoursEnabled) {
                        HorizontalDivider(color = Color(0x20FFFFFF))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start Time
                            Column {
                                Text("Quiet Start", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x30FFFFFF))
                                        .clickable { showQuietStartTimePicker = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = formatTime(quietStartHour, quietStartMinute),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))

                            // End Time
                            Column {
                                Text("Quiet End", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x30FFFFFF))
                                        .clickable { showQuietEndTimePicker = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = formatTime(quietEndHour, quietEndMinute),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Save Preferences Button
            Button(
                onClick = {
                    val newPrefs = NotificationPreference(
                        masterEnabled = masterEnabled,
                        studyReminders = studyReminders,
                        examCountdownAlerts = examCountdownAlerts,
                        dailyGoalReminders = dailyGoalReminders,
                        missedStudyReminders = missedStudyReminders,
                        breakReminders = breakReminders,
                        focusStartedAlerts = focusStartedAlerts,
                        focusCompletedAlerts = focusCompletedAlerts,
                        motivationalQuotes = motivationalQuotes,
                        reminderHour = reminderHour,
                        reminderMinute = reminderMinute,
                        dailyGoalHour = dailyGoalHour,
                        dailyGoalMinute = dailyGoalMinute,
                        motivationHour = motivationHour,
                        motivationMinute = motivationMinute,
                        activeDays = activeDays,
                        motivationFrequency = motivationFreq,
                        quietHoursEnabled = quietHoursEnabled,
                        quietStartHour = quietStartHour,
                        quietStartMinute = quietStartMinute,
                        quietEndHour = quietEndHour,
                        quietEndMinute = quietEndMinute
                    )
                    onSavePrefs(newPrefs)
                    Toast.makeText(context, "Notification preferences saved & scheduled! ✅", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apply_notification_settings_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Save, null, tint = Color(0xFF070B19), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Apply Notification Preferences", fontWeight = FontWeight.Bold, color = Color(0xFF070B19))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ==========================================
    // Time Picker Dialogs
    // ==========================================
    if (showStudyTimePicker) {
        SimpleTimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            title = "Set Study Session Reminder Time",
            onConfirm = { h, m ->
                reminderHour = h
                reminderMinute = m
                showStudyTimePicker = false
            },
            onDismiss = { showStudyTimePicker = false }
        )
    }

    if (showGoalTimePicker) {
        SimpleTimePickerDialog(
            initialHour = dailyGoalHour,
            initialMinute = dailyGoalMinute,
            title = "Set Daily Goal Reminder Time",
            onConfirm = { h, m ->
                dailyGoalHour = h
                dailyGoalMinute = m
                showGoalTimePicker = false
            },
            onDismiss = { showGoalTimePicker = false }
        )
    }

    if (showMotivationTimePicker) {
        SimpleTimePickerDialog(
            initialHour = motivationHour,
            initialMinute = motivationMinute,
            title = "Set Daily Motivation Time",
            onConfirm = { h, m ->
                motivationHour = h
                motivationMinute = m
                showMotivationTimePicker = false
            },
            onDismiss = { showMotivationTimePicker = false }
        )
    }

    if (showQuietStartTimePicker) {
        SimpleTimePickerDialog(
            initialHour = quietStartHour,
            initialMinute = quietStartMinute,
            title = "Set Quiet Hours Start Time",
            onConfirm = { h, m ->
                quietStartHour = h
                quietStartMinute = m
                showQuietStartTimePicker = false
            },
            onDismiss = { showQuietStartTimePicker = false }
        )
    }

    if (showQuietEndTimePicker) {
        SimpleTimePickerDialog(
            initialHour = quietEndHour,
            initialMinute = quietEndMinute,
            title = "Set Quiet Hours End Time",
            onConfirm = { h, m ->
                quietEndHour = h
                quietEndMinute = m
                showQuietEndTimePicker = false
            },
            onDismiss = { showQuietEndTimePicker = false }
        )
    }
}

@Composable
fun TestNotifChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x18FFFFFF))
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TimeSettingTile(
    title: String,
    subtitle: String,
    timeDisplay: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x30FFFFFF))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }
    }
}

@Composable
fun NotificationTypeSwitchTile(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (checked) NeonCyan.copy(alpha = 0.2f) else Color(0x15FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) NeonCyan else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF070B19),
                    checkedTrackColor = NeonCyan
                )
            )
        }
    }
}

@Composable
fun SimpleTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    title: String,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val amPm = if (selectedHour >= 12) "PM" else "AM"
                val displayHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12

                Text(
                    text = String.format(Locale.US, "%02d:%02d %s", displayHour, selectedMinute, amPm),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                // Hour Slider (0-23)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Hour (24h format: $selectedHour:00)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                    Slider(
                        value = selectedHour.toFloat(),
                        onValueChange = { selectedHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }

                // Minute Slider (0-55, step by 5)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Minute (${String.format(Locale.US, "%02d", selectedMinute)})", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                    Slider(
                        value = selectedMinute.toFloat(),
                        onValueChange = { selectedMinute = (it / 5).toInt() * 5 },
                        valueRange = 0f..55f,
                        steps = 10,
                        colors = SliderDefaults.colors(thumbColor = GoldenSpark, activeTrackColor = GoldenSpark)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                Text("Set Time", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

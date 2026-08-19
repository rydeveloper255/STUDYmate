package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.NotificationPreference
import com.example.data.model.UserProfile
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.screens.focus.FocusShieldSettingsScreen
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileSettingsDialog(
    user: UserProfile?,
    isDarkTheme: Boolean,
    notificationPrefs: NotificationPreference,
    onToggleDarkTheme: (Boolean) -> Unit,
    onUpdateNotificationPrefs: (NotificationPreference) -> Unit,
    onUpdateProfile: (UserProfile) -> Unit = {},
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDismiss: () -> Unit,
    onTestStudyReminder: () -> Unit = {},
    onTestExamCountdown: () -> Unit = {},
    onTestDailyGoal: () -> Unit = {},
    onTestMissedStudy: () -> Unit = {},
    onTestBreakReminder: () -> Unit = {},
    onTestFocusStarted: () -> Unit = {},
    onTestFocusCompleted: () -> Unit = {},
    onTestDailyMotivation: () -> Unit = {}
) {
    var isEditingProfile by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFocusShieldSettings by remember { mutableStateOf(false) }
    var showNotificationCenter by remember { mutableStateOf(false) }
    var showPermissionSetup by remember { mutableStateOf(false) }

    // ==========================================
    // Editable Profile State (Steps 1 to 5)
    // ==========================================
    var editName by remember(user) { mutableStateOf(user?.name ?: "Student") }
    var editPhotoUrl by remember(user) { mutableStateOf(user?.photoUrl ?: "") }
    var editGrade by remember(user) { mutableStateOf(user?.grade ?: "Class 12") }
    var editLanguage by remember(user) { mutableStateOf(user?.languagePreference ?: "English") }

    var editExamName by remember(user) { mutableStateOf(user?.examName ?: "Board & Entrance") }
    var editTargetScore by remember(user) { mutableStateOf(user?.targetScore ?: "Top 500 AIR") }
    
    // Days until exam calculation
    val defaultDays = remember(user) {
        val diff = (user?.examDateMillis ?: (System.currentTimeMillis() + 60L * 86400000L)) - System.currentTimeMillis()
        (diff / 86400000L).coerceIn(7L, 365L).toFloat()
    }
    var editDaysAhead by remember(user) { mutableFloatStateOf(defaultDays) }

    var editSubjects by remember(user) { mutableStateOf(user?.subjects?.toSet() ?: setOf("Mathematics", "Physics", "Chemistry")) }
    var editCustomSubject by remember { mutableStateOf("") }
    
    var editSubjectPriorities by remember(user) {
        val map = mutableMapOf<String, String>()
        user?.highPrioritySubjects?.forEach { map[it] = "HIGH" }
        user?.mediumPrioritySubjects?.forEach { map[it] = "MEDIUM" }
        user?.lowPrioritySubjects?.forEach { map[it] = "LOW" }
        mutableStateOf(map.ifEmpty { mapOf("Physics" to "HIGH", "Mathematics" to "HIGH", "Chemistry" to "MEDIUM") })
    }
    
    var editPrepLevel by remember(user) { mutableStateOf(user?.preparationLevel ?: "Intermediate (Practicing questions & concepts)") }

    var editStudyHours by remember(user) { mutableFloatStateOf(user?.availableStudyHours ?: 3.5f) }
    var editStartTime by remember(user) { mutableStateOf(user?.preferredStudyStartTime ?: "06:00 PM") }
    var editEndTime by remember(user) { mutableStateOf(user?.preferredStudyEndTime ?: "10:00 PM") }
    var editStudyDays by remember(user) { mutableStateOf(user?.preferredStudyDays?.toSet() ?: setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) }
    var editBreakMinutes by remember(user) { mutableIntStateOf(user?.breakDurationMinutes ?: 15) }

    var editWeakSubjects by remember(user) { mutableStateOf(user?.weakSubjects?.toSet() ?: setOf("Chemistry")) }
    var editWeakTopics by remember(user) { mutableStateOf(user?.weakTopics ?: listOf("Organic Reaction Mechanisms", "Rotational Dynamics")) }
    var editCustomWeakTopic by remember { mutableStateOf("") }
    var editDailyGoal by remember(user) { mutableStateOf(user?.dailyStudyGoal ?: "Complete daily scheduled topics & 20 flashcards") }

    val presetAvatars = listOf(
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar1",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar2",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar3",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar4",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar5",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar6"
    )

    if (showFocusShieldSettings) {
        Dialog(onDismissRequest = { showFocusShieldSettings = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                color = Color(0xFF070B19)
            ) {
                FocusShieldSettingsScreen(
                    onBack = { showFocusShieldSettings = false }
                )
            }
        }
    } else if (showNotificationCenter) {
        Dialog(onDismissRequest = { showNotificationCenter = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                color = Color(0xFF070B19)
            ) {
                com.example.ui.screens.notification.NotificationSettingsScreen(
                    user = user,
                    currentPrefs = notificationPrefs,
                    onSavePrefs = {
                        onUpdateNotificationPrefs(it)
                        showNotificationCenter = false
                    },
                    onBack = { showNotificationCenter = false },
                    onTestStudyReminder = onTestStudyReminder,
                    onTestExamCountdown = onTestExamCountdown,
                    onTestDailyGoal = onTestDailyGoal,
                    onTestMissedStudy = onTestMissedStudy,
                    onTestBreakReminder = onTestBreakReminder,
                    onTestFocusStarted = onTestFocusStarted,
                    onTestFocusCompleted = onTestFocusCompleted,
                    onTestDailyMotivation = onTestDailyMotivation
                )
            }
        }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .testTag("profile_settings_dialog"),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF111827),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x35FFFFFF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEditingProfile) "Edit Study Profile 📝" else "Profile & Settings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, "Close", tint = Color(0xFF94A3B8))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isEditingProfile) {
                            // ====================================================
                            // VIEW MODE: Comprehensive Profile Overview
                            // ====================================================

                            // User Identity Card
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                fillAlpha = 0.7f
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x3338BDF8))
                                            .border(1.5.dp, NeonCyan, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!user?.photoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = user?.photoUrl,
                                                contentDescription = "Avatar",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user?.name ?: "Student",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (user?.isGuest == true) "Guest Session (Local Room Storage)" else (user?.email ?: "Google Scholar"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${user?.grade} • ${user?.examName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { isEditingProfile = true },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0x2538BDF8))
                                    ) {
                                        Icon(Icons.Filled.Edit, "Edit Profile", tint = NeonCyan, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Academic Blueprint Summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Academic Blueprint & Goal 🎯",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                TextButton(onClick = { isEditingProfile = true }) {
                                    Text("Edit All", color = GoldenSpark, style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x18FFFFFF),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x25FFFFFF))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Class / Level:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                        Text(user?.grade ?: "Class 12", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Language:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                        Text(user?.languagePreference ?: "English", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Target Exam:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                        Text(user?.examName ?: "Entrance Exam", color = GoldenSpark, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    if (!user?.targetScore.isNullOrBlank()) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Target Score:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                            Text(user?.targetScore ?: "99%ile", color = EmeraldSuccess, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Subjects:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                        Text(user?.subjects?.joinToString(", ") ?: "PCM", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Daily Study Time:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                        Text("${user?.availableStudyHours ?: 3.5f} hrs/day", color = NeonCyan, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Daily Time Slot:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                        Text("${user?.preferredStudyStartTime ?: "06:00 PM"} – ${user?.preferredStudyEndTime ?: "10:00 PM"}", color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Study Days & Breaks:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                        Text("${user?.preferredStudyDays?.size ?: 6} days/wk (${user?.breakDurationMinutes ?: 15}m break)", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Weak Focus Area:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                        Text(user?.weakSubjects?.joinToString(", ") ?: "None", color = CoralRose, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    if (!user?.dailyStudyGoal.isNullOrBlank()) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Daily Goal:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                            Text(user?.dailyStudyGoal ?: "", color = GoldenSpark, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Theme & Display
                            Text(
                                text = "Appearance & Interface",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x18FFFFFF))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.DarkMode, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = "Dark Mode (Recommended)", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(
                                    checked = isDarkTheme,
                                    onCheckedChange = onToggleDarkTheme,
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF070B19), checkedTrackColor = NeonCyan)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Notification Preferences
                            Text(
                                text = "Smart Study Notifications & Reminders",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            PermissionRow(
                                title = "Notification Center & 8 Reminder Types 🔔",
                                subtitle = "Manage study alerts, exam countdown, daily goals, quiet hours & test live",
                                status = if (notificationPrefs.masterEnabled) "Active" else "Paused",
                                isGranted = notificationPrefs.masterEnabled,
                                onClick = { showNotificationCenter = true }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                NotificationRow(
                                    title = "📚 Study Session Reminders (${notificationPrefs.reminderHour % 12}:${String.format(Locale.US, "%02d", notificationPrefs.reminderMinute)} ${if (notificationPrefs.reminderHour >= 12) "PM" else "AM"})",
                                    checked = notificationPrefs.studyReminders && notificationPrefs.masterEnabled,
                                    onChecked = { onUpdateNotificationPrefs(notificationPrefs.copy(studyReminders = it)) }
                                )
                                NotificationRow(
                                    title = "⏳ Exam Countdown Alerts",
                                    checked = notificationPrefs.examCountdownAlerts && notificationPrefs.masterEnabled,
                                    onChecked = { onUpdateNotificationPrefs(notificationPrefs.copy(examCountdownAlerts = it)) }
                                )
                                NotificationRow(
                                    title = "🎯 Daily Study Goal Check-in",
                                    checked = notificationPrefs.dailyGoalReminders && notificationPrefs.masterEnabled,
                                    onChecked = { onUpdateNotificationPrefs(notificationPrefs.copy(dailyGoalReminders = it)) }
                                )
                                NotificationRow(
                                    title = "✨ Daily Motivational Boosts",
                                    checked = notificationPrefs.motivationalQuotes && notificationPrefs.masterEnabled,
                                    onChecked = { onUpdateNotificationPrefs(notificationPrefs.copy(motivationalQuotes = it)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Focus Shield Center
                            PermissionRow(
                                title = "Focus Shield & App Blocker 🛡️",
                                subtitle = "Manage restricted apps during deep focus timers",
                                status = "Configure",
                                isGranted = true,
                                onClick = { showFocusShieldSettings = true }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // App Permissions & Setup Center
                            PermissionRow(
                                title = "App Permissions & System Access ⚙️",
                                subtitle = "Manage Microphone, Camera, Storage, Notifications & Accessibility",
                                status = "Review",
                                isGranted = true,
                                onClick = { showPermissionSetup = true }
                            )
                        } else {
                            // ====================================================
                            // EDIT MODE: 5-Step Configurable Fields
                            // ====================================================
                            Text(
                                text = "Update your profile parameters below and tap Save Changes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Step 1 Section: Name, Photo, Level, Language
                            Text("1️⃣ Personal & Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Avatar selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                presetAvatars.forEach { avatarUrl ->
                                    val isSelected = editPhotoUrl == avatarUrl
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) NeonCyan.copy(alpha = 0.3f) else Color(0x20FFFFFF))
                                            .border(2.dp, if (isSelected) NeonCyan else Color.Transparent, CircleShape)
                                            .springClickable { editPhotoUrl = avatarUrl },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x40FFFFFF)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editGrade,
                                    onValueChange = { editGrade = it },
                                    label = { Text("Class / Academic Level") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = Color(0x40FFFFFF)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = editLanguage,
                                    onValueChange = { editLanguage = it },
                                    label = { Text("Language") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = Color(0x40FFFFFF)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Step 2 Section: Target Exam & Date
                            Text("2️⃣ Target Exam & Countdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldenSpark)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editExamName,
                                onValueChange = { editExamName = it },
                                label = { Text("Target Exam Name (Custom)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = GoldenSpark,
                                    unfocusedBorderColor = Color(0x40FFFFFF)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = editTargetScore,
                                onValueChange = { editTargetScore = it },
                                label = { Text("Target Score / Rank Goal (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = GoldenSpark,
                                    unfocusedBorderColor = Color(0x40FFFFFF)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val examDateFormatted = remember(editDaysAhead) {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, editDaysAhead.toInt())
                                SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.time)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Days to Exam:", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Text("${editDaysAhead.toInt()} Days ($examDateFormatted)", color = GoldenSpark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = editDaysAhead,
                                onValueChange = { editDaysAhead = it },
                                valueRange = 7f..365f,
                                colors = SliderDefaults.colors(thumbColor = GoldenSpark, activeTrackColor = GoldenSpark)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Step 3 Section: Subjects & Priorities
                            Text("3️⃣ Subjects & Priority", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Subject chips with priority
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                editSubjects.forEach { sub ->
                                    val priority = editSubjectPriorities[sub] ?: "MEDIUM"
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0x18FFFFFF),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Close,
                                                    "Remove",
                                                    tint = CoralRose,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .springClickable {
                                                            editSubjects = editSubjects - sub
                                                        }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(sub, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("HIGH" to "🔥 High", "MEDIUM" to "⚡ Med", "LOW" to "📘 Low").forEach { (pKey, pLabel) ->
                                                    val isCurr = priority == pKey
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (isCurr) {
                                                            if (pKey == "HIGH") CoralRose else if (pKey == "MEDIUM") GoldenSpark else EmeraldSuccess
                                                        } else Color(0x20FFFFFF),
                                                        modifier = Modifier.springClickable {
                                                            val nextMap = editSubjectPriorities.toMutableMap()
                                                            nextMap[sub] = pKey
                                                            editSubjectPriorities = nextMap
                                                        }
                                                    ) {
                                                        Text(
                                                            text = pLabel,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isCurr) Color(0xFF070B19) else Color(0xFFCBD5E1),
                                                            fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editCustomSubject,
                                        onValueChange = { editCustomSubject = it },
                                        label = { Text("Add Subject") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = EmeraldSuccess,
                                            unfocusedBorderColor = Color(0x40FFFFFF)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (editCustomSubject.isNotBlank()) {
                                                val s = editCustomSubject.trim()
                                                editSubjects = editSubjects + s
                                                val nextMap = editSubjectPriorities.toMutableMap()
                                                nextMap[s] = "HIGH"
                                                editSubjectPriorities = nextMap
                                                editCustomSubject = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF070B19)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text("+ Add", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = editPrepLevel,
                                onValueChange = { editPrepLevel = it },
                                label = { Text("Preparation Level") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = EmeraldSuccess,
                                    unfocusedBorderColor = Color(0x40FFFFFF)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Step 4 Section: Study Schedule, Times & Breaks
                            Text("4️⃣ Schedule, Timing & Breaks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricViolet)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Available Study Hours:", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Text("${editStudyHours} hrs/day", color = ElectricViolet, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = editStudyHours,
                                onValueChange = { editStudyHours = (it * 2).toInt() / 2f },
                                valueRange = 1f..12f,
                                colors = SliderDefaults.colors(thumbColor = ElectricViolet, activeTrackColor = ElectricViolet)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editStartTime,
                                    onValueChange = { editStartTime = it },
                                    label = { Text("Start Time") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = ElectricViolet,
                                        unfocusedBorderColor = Color(0x40FFFFFF)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = editEndTime,
                                    onValueChange = { editEndTime = it },
                                    label = { Text("End Time") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = ElectricViolet,
                                        unfocusedBorderColor = Color(0x40FFFFFF)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Break Duration selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Break Duration:", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(5, 10, 15, 20, 30).forEach { b ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (editBreakMinutes == b) ElectricViolet else Color(0x18FFFFFF),
                                            modifier = Modifier.springClickable { editBreakMinutes = b }
                                        ) {
                                            Text(
                                                text = "${b}m",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (editBreakMinutes == b) Color.White else Color(0xFFCBD5E1),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Step 5 Section: Weak Areas & Daily Goals
                            Text("5️⃣ Weak Areas & Daily Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CoralRose)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Weak subjects chips
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                editSubjects.forEach { s ->
                                    val isWeak = editWeakSubjects.contains(s)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isWeak) CoralRose else Color(0x18FFFFFF),
                                        modifier = Modifier.springClickable {
                                            val next = editWeakSubjects.toMutableSet()
                                            if (isWeak) next.remove(s) else next.add(s)
                                            editWeakSubjects = next
                                        }
                                    ) {
                                        Text(
                                            text = "$s ${if (isWeak) "🔥" else ""}",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isWeak) Color.White else Color(0xFFCBD5E1),
                                            fontWeight = if (isWeak) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Weak topic tags
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                editWeakTopics.chunked(2).forEach { rowTopics ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        rowTopics.forEach { topic ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0x28F43F5E),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.4f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(topic, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.weight(1f))
                                                    Icon(
                                                        Icons.Filled.Close,
                                                        "Remove",
                                                        tint = CoralRose,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .springClickable { editWeakTopics = editWeakTopics - topic }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editCustomWeakTopic,
                                        onValueChange = { editCustomWeakTopic = it },
                                        label = { Text("Add weak topic") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = CoralRose,
                                            unfocusedBorderColor = Color(0x40FFFFFF)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (editCustomWeakTopic.isNotBlank()) {
                                                editWeakTopics = editWeakTopics + editCustomWeakTopic.trim()
                                                editCustomWeakTopic = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CoralRose),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text("+ Tag", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = editDailyGoal,
                                onValueChange = { editDailyGoal = it },
                                label = { Text("Daily Study Goal") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CoralRose,
                                    unfocusedBorderColor = Color(0x40FFFFFF)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Bottom Action Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isEditingProfile) {
                            Button(
                                onClick = {
                                    val highPrio = editSubjectPriorities.filter { it.value == "HIGH" }.keys.toList()
                                    val medPrio = editSubjectPriorities.filter { it.value == "MEDIUM" }.keys.toList()
                                    val lowPrio = editSubjectPriorities.filter { it.value == "LOW" }.keys.toList()
                                    val finalExamMillis = System.currentTimeMillis() + (editDaysAhead.toLong() * 86400000L)

                                    val updated = (user ?: UserProfile()).copy(
                                        name = editName.ifBlank { "Student" },
                                        photoUrl = editPhotoUrl.ifBlank { null },
                                        grade = editGrade.ifBlank { "Class 12" },
                                        educationLevel = editGrade.ifBlank { "Class 12" },
                                        languagePreference = editLanguage.ifBlank { "English" },
                                        examName = editExamName.ifBlank { "Target Exam" },
                                        goal = editExamName.ifBlank { "Target Exam" },
                                        targetScore = editTargetScore,
                                        examDateMillis = finalExamMillis,
                                        subjects = if (editSubjects.isNotEmpty()) editSubjects.toList() else listOf("Mathematics", "Physics", "Chemistry"),
                                        highPrioritySubjects = highPrio,
                                        mediumPrioritySubjects = medPrio,
                                        lowPrioritySubjects = lowPrio,
                                        strongSubjects = editSubjects.filter { it !in editWeakSubjects },
                                        preparationLevel = editPrepLevel,
                                        availableStudyHours = editStudyHours,
                                        dailyTargetMinutes = (editStudyHours * 60).toInt(),
                                        preferredStudyStartTime = editStartTime,
                                        preferredStudyEndTime = editEndTime,
                                        preferredStudyDays = editStudyDays.toList(),
                                        breakDurationMinutes = editBreakMinutes,
                                        preferredStudyTime = "$editStartTime - $editEndTime",
                                        weakSubjects = editWeakSubjects.toList(),
                                        weakTopics = editWeakTopics,
                                        dailyStudyGoal = editDailyGoal,
                                        isOnboardingCompleted = true
                                    )

                                    onUpdateProfile(updated)
                                    isEditingProfile = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_profile_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Save, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Profile Changes 💾", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { isEditingProfile = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x35FFFFFF))
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onSignOut()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("sign_out_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x35FFFFFF))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            TextButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("delete_account_button")
                            ) {
                                Text("Delete Account & Local Data", color = CoralRose, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = Color(0xFF131C2E),
                title = { Text("Delete Account?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("This will permanently clear all your study plans, mock tests, and progress.", color = Color(0xFFCBD5E1)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            onDismiss()
                            onDeleteAccount()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                    ) {
                        Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        if (showFocusShieldSettings) {
            Dialog(
                onDismissRequest = { showFocusShieldSettings = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FocusShieldSettingsScreen(onBack = { showFocusShieldSettings = false })
                }
            }
        }

        if (showPermissionSetup) {
            Dialog(
                onDismissRequest = { showPermissionSetup = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    com.example.ui.screens.auth.PermissionSetupScreen(onCompleteSetup = { showPermissionSetup = false })
                }
            }
        }
    }
}

@Composable
fun NotificationRow(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x18FFFFFF))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF070B19), checkedTrackColor = NeonCyan)
        )
    }
}

@Composable
fun PermissionRow(
    title: String,
    subtitle: String,
    status: String,
    isGranted: Boolean,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x18FFFFFF))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isGranted) EmeraldSuccess.copy(alpha = 0.2f) else CoralRose.copy(alpha = 0.2f)
        ) {
            Text(
                text = status,
                color = if (isGranted) EmeraldSuccess else CoralRose,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

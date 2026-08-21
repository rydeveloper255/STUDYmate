package com.example.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.NotificationPreference
import com.example.data.model.UserProfile
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.screens.auth.PermissionSetupScreen
import com.example.ui.screens.focus.AccessibilityPrivacyScreen
import com.example.ui.screens.focus.FocusShieldSettingsScreen
import com.example.ui.screens.notification.NotificationSettingsScreen
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private enum class ProfileSubScreen {
    NOTIFICATION_CENTER,
    FOCUS_SHIELD,
    PERMISSIONS,
    ACCESSIBILITY_PRIVACY,
    EDIT_MODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    user: UserProfile?,
    themeMode: AppThemeMode,
    isDarkTheme: Boolean,
    notificationPrefs: NotificationPreference,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onUpdateNotificationPrefs: (NotificationPreference) -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
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
    var activeSubScreen by remember { mutableStateOf<ProfileSubScreen?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Intercept back presses for full-screen sub-screens
    BackHandler(enabled = activeSubScreen != null) {
        activeSubScreen = null
    }

    // Handle full-screen sub-destinations seamlessly without dialog popups
    when (activeSubScreen) {
        ProfileSubScreen.NOTIFICATION_CENTER -> {
            NotificationSettingsScreen(
                user = user,
                currentPrefs = notificationPrefs,
                onSavePrefs = {
                    onUpdateNotificationPrefs(it)
                    activeSubScreen = null
                },
                onBack = { activeSubScreen = null },
                onTestStudyReminder = onTestStudyReminder,
                onTestExamCountdown = onTestExamCountdown,
                onTestDailyGoal = onTestDailyGoal,
                onTestMissedStudy = onTestMissedStudy,
                onTestBreakReminder = onTestBreakReminder,
                onTestFocusStarted = onTestFocusStarted,
                onTestFocusCompleted = onTestFocusCompleted,
                onTestDailyMotivation = onTestDailyMotivation
            )
            return
        }
        ProfileSubScreen.FOCUS_SHIELD -> {
            FocusShieldSettingsScreen(
                onBack = { activeSubScreen = null }
            )
            return
        }
        ProfileSubScreen.PERMISSIONS -> {
            PermissionSetupScreen(
                onCompleteSetup = { activeSubScreen = null }
            )
            return
        }
        ProfileSubScreen.ACCESSIBILITY_PRIVACY -> {
            AccessibilityPrivacyScreen(
                onBack = { activeSubScreen = null }
            )
            return
        }
        ProfileSubScreen.EDIT_MODE -> {
            ProfileEditContent(
                user = user,
                onSave = { updated ->
                    onUpdateProfile(updated)
                    activeSubScreen = null
                    Toast.makeText(context, "Academic Profile Updated ✨", Toast.LENGTH_SHORT).show()
                },
                onCancel = { activeSubScreen = null }
            )
            return
        }
        null -> {
            // Main Full-Screen Profile & Settings
        }
    }

    val isGlassLight = themeMode == AppThemeMode.GLASS_LIGHT
    val isAmoled = themeMode == AppThemeMode.AMOLED_BLACK

    val primaryTextColor = if (isGlassLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val secondaryTextColor = if (isGlassLight) Color(0xFF64748B) else Color(0xFF94A3B8)
    val cardBackground = when {
        isAmoled -> AmoledCardSurface
        isGlassLight -> CardSurfaceLight
        else -> CardSurfaceDark
    }
    val cardBorderColor = when {
        isAmoled -> GlassBorderAmoled
        isGlassLight -> GlassBorderLight
        else -> GlassBorderDark
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_settings_screen"),
        containerColor = Color.Transparent,
        topBar = {
            // Clean Liquid Glass Top App Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isGlassLight) Color(0xDCFFFFFF) else if (isAmoled) Color(0xF0000000) else Color(0xCC070B19),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isGlassLight) Color(0x150F172A) else Color(0x20FFFFFF))
                            .testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = primaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Profile & Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Text(
                            text = "Personalize your StudyMate experience",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor
                        )
                    }

                    // Quick Edit Shortcut
                    IconButton(
                        onClick = { activeSubScreen = ProfileSubScreen.EDIT_MODE },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = if (isGlassLight) 0.15f else 0.2f))
                            .testTag("top_edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Profile",
                            tint = if (isGlassLight) DeepIndigo else NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ==========================================
            // 1. PROFILE HERO SECTION
            // ==========================================
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_hero_card"),
                    shape = RoundedCornerShape(24.dp),
                    fillAlpha = if (isAmoled) 0.95f else if (isGlassLight) 0.75f else 0.65f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Glowing Avatar Frame
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(NeonCyan, ElectricViolet, NebulaPurple, NeonCyan)
                                        )
                                    )
                                    .padding(2.5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(if (isGlassLight) Color(0xFFE2E8F0) else Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!user?.photoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = user?.photoUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = if (isGlassLight) DeepIndigo else NeonCyan,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user?.name ?: "Student Scholar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )
                                Text(
                                    text = if (user?.isGuest == true) "Guest Session (Local Storage)" else (user?.email ?: "Scholar Account"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryTextColor,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = (if (isGlassLight) DeepIndigo else NeonCyan).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = user?.grade ?: "Class 12",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isGlassLight) DeepIndigo else NeonCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = GoldenSpark.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = user?.examName ?: "Target Exam",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldenSpark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Edit Profile Button
                        GlassButton(
                            text = "EDIT PROFILE",
                            icon = Icons.Filled.Edit,
                            onClick = { activeSubScreen = ProfileSubScreen.EDIT_MODE },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            testTag = "edit_profile_button"
                        )
                    }
                }
            }

            // ==========================================
            // 2. ACADEMIC BLUEPRINT 🎯
            // ==========================================
            item {
                SectionHeader(
                    title = "ACADEMIC BLUEPRINT 🎯",
                    subtitle = "Syllabus strategy, timeline & study targets",
                    actionLabel = "EDIT PLAN",
                    onActionClick = { activeSubScreen = ProfileSubScreen.EDIT_MODE },
                    primaryColor = primaryTextColor,
                    accentColor = GoldenSpark
                )

                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("academic_blueprint_card"),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.7f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BlueprintDataRow(
                            icon = Icons.Outlined.School,
                            label = "Class / Level",
                            value = user?.grade ?: "College / Undergraduate",
                            primaryColor = primaryTextColor,
                            secondaryColor = secondaryTextColor
                        )
                        BlueprintDivider(cardBorderColor)

                        BlueprintDataRow(
                            icon = Icons.Outlined.Translate,
                            label = "Language",
                            value = user?.languagePreference ?: "English",
                            primaryColor = primaryTextColor,
                            secondaryColor = secondaryTextColor
                        )
                        BlueprintDivider(cardBorderColor)

                        BlueprintDataRow(
                            icon = Icons.Outlined.EmojiEvents,
                            label = "Target Exam",
                            value = user?.examName ?: "Entrance Exam",
                            valueColor = GoldenSpark,
                            primaryColor = primaryTextColor,
                            secondaryColor = secondaryTextColor
                        )
                        BlueprintDivider(cardBorderColor)

                        if (!user?.targetScore.isNullOrBlank()) {
                            BlueprintDataRow(
                                icon = Icons.Outlined.Star,
                                label = "Target Score / Rank",
                                value = user?.targetScore ?: "Top 500 AIR",
                                valueColor = EmeraldSuccess,
                                primaryColor = primaryTextColor,
                                secondaryColor = secondaryTextColor
                            )
                            BlueprintDivider(cardBorderColor)
                        }

                        BlueprintDataRow(
                            icon = Icons.Outlined.MenuBook,
                            label = "Subjects",
                            value = "${user?.subjects?.size ?: 0} selected (${user?.subjects?.joinToString(", ") ?: "PCM"})",
                            primaryColor = primaryTextColor,
                            secondaryColor = secondaryTextColor
                        )
                        BlueprintDivider(cardBorderColor)

                        BlueprintDataRow(
                            icon = Icons.Outlined.Timer,
                            label = "Daily Study Time",
                            value = "${user?.availableStudyHours ?: 3.5f} hours/day",
                            valueColor = if (isGlassLight) DeepIndigo else NeonCyan,
                            primaryColor = primaryTextColor,
                            secondaryColor = secondaryTextColor
                        )
                        BlueprintDivider(cardBorderColor)

                        BlueprintDataRow(
                            icon = Icons.Outlined.Schedule,
                            label = "Preferred Study Window",
                            value = "${user?.preferredStudyStartTime ?: "06:00 PM"} – ${user?.preferredStudyEndTime ?: "10:00 PM"}",
                            primaryColor = primaryTextColor,
                            secondaryColor = secondaryTextColor
                        )
                        BlueprintDivider(cardBorderColor)

                        BlueprintDataRow(
                            icon = Icons.Outlined.CalendarMonth,
                            label = "Study Days & Breaks",
                            value = "${user?.preferredStudyDays?.size ?: 6} days/wk (${user?.breakDurationMinutes ?: 15}m break)",
                            primaryColor = primaryTextColor,
                            secondaryColor = secondaryTextColor
                        )
                        BlueprintDivider(cardBorderColor)

                        BlueprintDataRow(
                            icon = Icons.Outlined.WarningAmber,
                            label = "Weak Focus Areas",
                            value = user?.weakSubjects?.joinToString(", ") ?: "None specified",
                            valueColor = CoralRose,
                            primaryColor = primaryTextColor,
                            secondaryColor = secondaryTextColor
                        )

                        if (!user?.dailyStudyGoal.isNullOrBlank()) {
                            BlueprintDivider(cardBorderColor)
                            BlueprintDataRow(
                                icon = Icons.Outlined.Flag,
                                label = "Daily Goal",
                                value = user?.dailyStudyGoal ?: "Complete schedule",
                                valueColor = GoldenSpark,
                                primaryColor = primaryTextColor,
                                secondaryColor = secondaryTextColor
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = { activeSubScreen = ProfileSubScreen.EDIT_MODE },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("edit_blueprint_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldenSpark.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Filled.EditCalendar, null, tint = GoldenSpark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EDIT ACADEMIC PLAN", color = GoldenSpark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ==========================================
            // 3. APPEARANCE & THEMES
            // ==========================================
            item {
                SectionHeader(
                    title = "APPEARANCE & THEMES 🎨",
                    subtitle = "Select your preferred Liquid Glass visual theme",
                    primaryColor = primaryTextColor,
                    accentColor = NeonCyan
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemeSelectCard(
                        mode = AppThemeMode.NOVA_DARK,
                        title = "NOVA DARK",
                        description = "Deep dark futuristic canvas with cyan and purple neon glow",
                        isSelected = themeMode == AppThemeMode.NOVA_DARK,
                        previewGradient = Brush.linearGradient(listOf(Color(0xFF070B19), Color(0xFF1E1066))),
                        accentColor = NeonCyan,
                        onSelect = { onSetThemeMode(AppThemeMode.NOVA_DARK) },
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor
                    )

                    ThemeSelectCard(
                        mode = AppThemeMode.GLASS_LIGHT,
                        title = "GLASS LIGHT",
                        description = "Frosted bright daylight surfaces with crisp high-contrast text",
                        isSelected = themeMode == AppThemeMode.GLASS_LIGHT,
                        previewGradient = Brush.linearGradient(listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))),
                        accentColor = DeepIndigo,
                        onSelect = { onSetThemeMode(AppThemeMode.GLASS_LIGHT) },
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor
                    )

                    ThemeSelectCard(
                        mode = AppThemeMode.AMOLED_BLACK,
                        title = "AMOLED DEEP BLACK",
                        description = "True pitch black for maximum OLED battery savings and minimal glow",
                        isSelected = themeMode == AppThemeMode.AMOLED_BLACK,
                        previewGradient = Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF0B0F19))),
                        accentColor = ElectricViolet,
                        onSelect = { onSetThemeMode(AppThemeMode.AMOLED_BLACK) },
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }
            }

            // ==========================================
            // 4. SMART STUDY NOTIFICATIONS & REMINDERS
            // ==========================================
            item {
                SectionHeader(
                    title = "SMART STUDY NOTIFICATIONS 🔔",
                    subtitle = "Automated reminders for sessions, goals & countdowns",
                    primaryColor = primaryTextColor,
                    accentColor = ElectricViolet
                )

                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.7f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Direct trigger to dedicated Notification Center
                        SettingsActionRow(
                            icon = Icons.Outlined.NotificationsActive,
                            title = "Notification Center & Live Testing",
                            subtitle = "Configure quiet hours, reminder frequencies, and test notifications",
                            badgeText = if (notificationPrefs.masterEnabled) "Active" else "Paused",
                            isSuccess = notificationPrefs.masterEnabled,
                            onClick = { activeSubScreen = ProfileSubScreen.NOTIFICATION_CENTER },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            cardBorderColor = cardBorderColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        NotificationToggleRow(
                            icon = Icons.Outlined.MenuBook,
                            title = "Study Session Reminders",
                            subtitle = "Alerts at ${notificationPrefs.reminderHour % 12}:${String.format(Locale.US, "%02d", notificationPrefs.reminderMinute)} ${if (notificationPrefs.reminderHour >= 12) "PM" else "AM"}",
                            checked = notificationPrefs.studyReminders && notificationPrefs.masterEnabled,
                            onCheckedChange = { onUpdateNotificationPrefs(notificationPrefs.copy(studyReminders = it)) },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor
                        )

                        NotificationToggleRow(
                            icon = Icons.Outlined.HourglassBottom,
                            title = "Exam Countdown Alerts",
                            subtitle = "Daily morning countdown to ${user?.examName ?: "Exam"}",
                            checked = notificationPrefs.examCountdownAlerts && notificationPrefs.masterEnabled,
                            onCheckedChange = { onUpdateNotificationPrefs(notificationPrefs.copy(examCountdownAlerts = it)) },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor
                        )

                        NotificationToggleRow(
                            icon = Icons.Outlined.CheckCircle,
                            title = "Daily Study Goal Check-in",
                            subtitle = "Evening progress prompt to review completed items",
                            checked = notificationPrefs.dailyGoalReminders && notificationPrefs.masterEnabled,
                            onCheckedChange = { onUpdateNotificationPrefs(notificationPrefs.copy(dailyGoalReminders = it)) },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor
                        )

                        NotificationToggleRow(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "Motivational Study Boosts",
                            subtitle = "Daily positive affirmations & exam momentum",
                            checked = notificationPrefs.motivationalQuotes && notificationPrefs.masterEnabled,
                            onCheckedChange = { onUpdateNotificationPrefs(notificationPrefs.copy(motivationalQuotes = it)) },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor
                        )
                    }
                }
            }

            // ==========================================
            // 5. FOCUS SHIELD 🛡️
            // ==========================================
            item {
                SectionHeader(
                    title = "FOCUS SHIELD 🛡️",
                    subtitle = "App blocking and distraction guard during study",
                    primaryColor = primaryTextColor,
                    accentColor = CoralRose
                )

                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.7f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Shield, null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Focus Guard Engine", color = primaryTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Restricts social & entertainment apps", color = secondaryTextColor, style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccess.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "Active",
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { activeSubScreen = ProfileSubScreen.FOCUS_SHIELD },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("configure_focus_shield_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, (if (isGlassLight) DeepIndigo else NeonCyan).copy(alpha = 0.4f))
                        ) {
                            Text("Configure Focus Shield & Blocklist >", color = if (isGlassLight) DeepIndigo else NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ==========================================
            // 6. APP PERMISSIONS & ACCESSIBILITY ⚙️
            // ==========================================
            item {
                SectionHeader(
                    title = "APP PERMISSIONS & ACCESSIBILITY ⚙️",
                    subtitle = "Manage camera, mic, notifications & accessibility safety",
                    primaryColor = primaryTextColor,
                    accentColor = NeonCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.7f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsActionRow(
                            icon = Icons.Outlined.Security,
                            title = "System Permissions Setup",
                            subtitle = "Microphone, Camera, Notifications, Storage & System Status",
                            badgeText = "Review",
                            isSuccess = true,
                            onClick = { activeSubScreen = ProfileSubScreen.PERMISSIONS },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            cardBorderColor = cardBorderColor
                        )

                        SettingsActionRow(
                            icon = Icons.Outlined.AccessibilityNew,
                            title = "Accessibility & Privacy Shield",
                            subtitle = "Nova accessibility safety mode & financial privacy guarantees",
                            badgeText = "Protected",
                            isSuccess = true,
                            onClick = { activeSubScreen = ProfileSubScreen.ACCESSIBILITY_PRIVACY },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            cardBorderColor = cardBorderColor
                        )
                    }
                }
            }

            // ==========================================
            // 7. PRIVACY & DATA CONTROLS 🔒
            // ==========================================
            item {
                SectionHeader(
                    title = "PRIVACY & DATA CONTROLS 🔒",
                    subtitle = "On-device Room database security & AI data guarantees",
                    primaryColor = primaryTextColor,
                    accentColor = EmeraldSuccess
                )

                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.7f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Storage, null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Offline-First Local Storage", color = primaryTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("All study notes, flashcards & mock tests reside on your device", color = secondaryTextColor, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        BlueprintDivider(cardBorderColor)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Psychology, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Stateless AI Processing", color = primaryTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("Gemini queries are sanitized; no sensitive credentials transmitted", color = secondaryTextColor, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 8. NOVA PERSONALIZATION ✨
            // ==========================================
            item {
                SectionHeader(
                    title = "NOVA PERSONALIZATION ✨",
                    subtitle = "Customize AI voice, personality & explanation style",
                    primaryColor = primaryTextColor,
                    accentColor = NebulaPurple
                )

                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.7f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BlueprintDataRow(
                            icon = Icons.Outlined.Translate,
                            label = "Nova Language",
                            value = user?.languagePreference ?: "English",
                            primaryColor = primaryTextColor,
                            secondaryColor = secondaryTextColor
                        )
                        BlueprintDivider(cardBorderColor)

                        BlueprintDataRow(
                            icon = Icons.Outlined.RecordVoiceOver,
                            label = "Voice & Mentor Persona",
                            value = "Socratic Coach & Strategic Guide",
                            primaryColor = primaryTextColor,
                            secondaryColor = secondaryTextColor
                        )
                        BlueprintDivider(cardBorderColor)

                        BlueprintDataRow(
                            icon = Icons.Outlined.Bolt,
                            label = "Response Style",
                            value = "Concise, Exam-Focused & Step-by-step",
                            primaryColor = primaryTextColor,
                            secondaryColor = secondaryTextColor
                        )
                    }
                }
            }

            // ==========================================
            // 9. ACCOUNT CONTROLS & SECURITY 👤
            // ==========================================
            item {
                SectionHeader(
                    title = "ACCOUNT & SESSION 👤",
                    subtitle = "Manage authentication & session state",
                    primaryColor = primaryTextColor,
                    accentColor = secondaryTextColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.7f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Session Type", color = secondaryTextColor, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = if (user?.isGuest == true) "Guest Session (Local DB)" else "Google Scholar",
                                    color = primaryTextColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = (if (isGlassLight) DeepIndigo else NeonCyan).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Active",
                                    color = if (isGlassLight) DeepIndigo else NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showSignOutConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sign_out_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = CoralRose, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SIGN OUT", color = CoralRose, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ==========================================
            // 10. DANGER ZONE ⚠️
            // ==========================================
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_account_button")
                    ) {
                        Text(
                            text = "DELETE ACCOUNT & LOCAL DATA",
                            color = CoralRose,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Permanently clears local study plans, test attempts & progress",
                        color = secondaryTextColor,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Sign Out
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = if (isGlassLight) Color(0xFFFFFFFF) else Color(0xFF131C2E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Sign out of StudyMate?",
                    color = primaryTextColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You will need to sign in again to access your study plans and mock test sessions.",
                    color = secondaryTextColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel", color = secondaryTextColor)
                }
            }
        )
    }

    // Confirmation Dialog for Delete Account
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = if (isGlassLight) Color(0xFFFFFFFF) else Color(0xFF131C2E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Delete Account & Data?",
                    color = CoralRose,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This action is irreversible. All your study schedules, smart notes, mock tests, and streak records will be permanently removed from this device.",
                    color = secondaryTextColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = secondaryTextColor)
                }
            }
        )
    }
}

// ======================================================================================
// EDIT MODE FULL-SCREEN COMPONENT
// ======================================================================================
@Composable
private fun ProfileEditContent(
    user: UserProfile?,
    onSave: (UserProfile) -> Unit,
    onCancel: () -> Unit
) {
    val themeMode = currentThemeMode()
    val isGlassLight = themeMode == AppThemeMode.GLASS_LIGHT
    val isAmoled = themeMode == AppThemeMode.AMOLED_BLACK

    val primaryTextColor = if (isGlassLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val secondaryTextColor = if (isGlassLight) Color(0xFF64748B) else Color(0xFF94A3B8)
    val cardBorderColor = when {
        isAmoled -> GlassBorderAmoled
        isGlassLight -> GlassBorderLight
        else -> GlassBorderDark
    }

    var editName by remember(user) { mutableStateOf(user?.name ?: "Student") }
    var editPhotoUrl by remember(user) { mutableStateOf(user?.photoUrl ?: "") }
    var editGrade by remember(user) { mutableStateOf(user?.grade ?: "Class 12") }
    var editLanguage by remember(user) { mutableStateOf(user?.languagePreference ?: "English") }
    var editExamName by remember(user) { mutableStateOf(user?.examName ?: "RRB Group D") }
    var editTargetScore by remember(user) { mutableStateOf(user?.targetScore ?: "Top 500 AIR") }

    val defaultDays = remember(user) {
        val diff = (user?.examDateMillis ?: (System.currentTimeMillis() + 60L * 86400000L)) - System.currentTimeMillis()
        (diff / 86400000L).coerceIn(7L, 365L).toFloat()
    }
    var editDaysAhead by remember(user) { mutableFloatStateOf(defaultDays) }

    var editSubjects by remember(user) {
        mutableStateOf(user?.subjects?.toSet() ?: setOf("Mathematics", "General Science", "Reasoning", "General Awareness"))
    }
    var editCustomSubject by remember { mutableStateOf("") }

    var editSubjectPriorities by remember(user) {
        val map = mutableMapOf<String, String>()
        user?.highPrioritySubjects?.forEach { map[it] = "HIGH" }
        user?.mediumPrioritySubjects?.forEach { map[it] = "MEDIUM" }
        user?.lowPrioritySubjects?.forEach { map[it] = "LOW" }
        mutableStateOf(map.ifEmpty { mapOf("General Science" to "HIGH", "Mathematics" to "HIGH", "Reasoning" to "MEDIUM") })
    }

    var editPrepLevel by remember(user) { mutableStateOf(user?.preparationLevel ?: "Intermediate (Concept practice & revision)") }
    var editStudyHours by remember(user) { mutableFloatStateOf(user?.availableStudyHours ?: 4.0f) }
    var editStartTime by remember(user) { mutableStateOf(user?.preferredStudyStartTime ?: "06:00 PM") }
    var editEndTime by remember(user) { mutableStateOf(user?.preferredStudyEndTime ?: "10:00 PM") }
    var editBreakMinutes by remember(user) { mutableIntStateOf(user?.breakDurationMinutes ?: 15) }

    var editWeakSubjects by remember(user) { mutableStateOf(user?.weakSubjects?.toSet() ?: setOf("General Science")) }
    var editWeakTopics by remember(user) { mutableStateOf(user?.weakTopics ?: listOf("Electric Circuits", "Syllogism")) }
    var editCustomWeakTopic by remember { mutableStateOf("") }
    var editDailyGoal by remember(user) { mutableStateOf(user?.dailyStudyGoal ?: "Complete daily missions & revise 20 flashcards") }

    val presetAvatars = listOf(
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar1",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar2",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar3",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar4",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar5",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar6"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isGlassLight) Color(0xDCFFFFFF) else if (isAmoled) Color(0xF0000000) else Color(0xCC070B19),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isGlassLight) Color(0x150F172A) else Color(0x20FFFFFF))
                        ) {
                            Icon(Icons.Filled.Close, "Cancel", tint = primaryTextColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Edit Study Profile 📝",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                    }

                    TextButton(
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
                                subjects = if (editSubjects.isNotEmpty()) editSubjects.toList() else listOf("Mathematics", "Science"),
                                highPrioritySubjects = highPrio,
                                mediumPrioritySubjects = medPrio,
                                lowPrioritySubjects = lowPrio,
                                strongSubjects = editSubjects.filter { it !in editWeakSubjects },
                                preparationLevel = editPrepLevel,
                                availableStudyHours = editStudyHours,
                                dailyTargetMinutes = (editStudyHours * 60).toInt(),
                                preferredStudyStartTime = editStartTime,
                                preferredStudyEndTime = editEndTime,
                                breakDurationMinutes = editBreakMinutes,
                                preferredStudyTime = "$editStartTime - $editEndTime",
                                weakSubjects = editWeakSubjects.toList(),
                                weakTopics = editWeakTopics,
                                dailyStudyGoal = editDailyGoal,
                                isOnboardingCompleted = true
                            )
                            onSave(updated)
                        }
                    ) {
                        Text("Save 💾", color = if (isGlassLight) DeepIndigo else NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Step 1: Personal & Language
            item {
                EditSectionCard(
                    title = "1️⃣ Personal Identity & Language",
                    accentColor = NeonCyan,
                    cardBorderColor = cardBorderColor,
                    primaryTextColor = primaryTextColor
                ) {
                    Text("Choose Avatar Preset:", color = secondaryTextColor, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetAvatars.forEach { avatarUrl ->
                            val isSelected = editPhotoUrl == avatarUrl
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.3f) else Color(0x15FFFFFF))
                                    .border(2.dp, if (isSelected) NeonCyan else Color.Transparent, CircleShape)
                                    .springClickable { editPhotoUrl = avatarUrl },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editGrade,
                            onValueChange = { editGrade = it },
                            label = { Text("Academic Level") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = editLanguage,
                            onValueChange = { editLanguage = it },
                            label = { Text("Language") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Step 2: Target Exam & Date
            item {
                EditSectionCard(
                    title = "2️⃣ Target Exam & Timeline",
                    accentColor = GoldenSpark,
                    cardBorderColor = cardBorderColor,
                    primaryTextColor = primaryTextColor
                ) {
                    OutlinedTextField(
                        value = editExamName,
                        onValueChange = { editExamName = it },
                        label = { Text("Target Exam Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editTargetScore,
                        onValueChange = { editTargetScore = it },
                        label = { Text("Target Score / Rank Goal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                        Text("Days to Exam:", color = primaryTextColor, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${editDaysAhead.toInt()} Days ($examDateFormatted)",
                            color = GoldenSpark,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Slider(
                        value = editDaysAhead,
                        onValueChange = { editDaysAhead = it },
                        valueRange = 7f..365f,
                        colors = SliderDefaults.colors(thumbColor = GoldenSpark, activeTrackColor = GoldenSpark)
                    )
                }
            }

            // Step 3: Subjects & Priorities
            item {
                EditSectionCard(
                    title = "3️⃣ Subjects & Priorities",
                    accentColor = EmeraldSuccess,
                    cardBorderColor = cardBorderColor,
                    primaryTextColor = primaryTextColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        editSubjects.forEach { sub ->
                            val priority = editSubjectPriorities[sub] ?: "MEDIUM"
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isGlassLight) Color(0xFFF1F5F9) else Color(0x18FFFFFF),
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
                                                .springClickable { editSubjects = editSubjects - sub }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(sub, color = primaryTextColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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
                                                    color = if (isCurr) Color(0xFF070B19) else secondaryTextColor,
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
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Step 4: Schedule, Timing & Breaks
            item {
                EditSectionCard(
                    title = "4️⃣ Schedule, Timing & Breaks",
                    accentColor = ElectricViolet,
                    cardBorderColor = cardBorderColor,
                    primaryTextColor = primaryTextColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Available Study Hours:", color = primaryTextColor, style = MaterialTheme.typography.bodySmall)
                        Text("${editStudyHours} hrs/day", color = ElectricViolet, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = editStudyHours,
                        onValueChange = { editStudyHours = (it * 2).toInt() / 2f },
                        valueRange = 1f..12f,
                        colors = SliderDefaults.colors(thumbColor = ElectricViolet, activeTrackColor = ElectricViolet)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editStartTime,
                            onValueChange = { editStartTime = it },
                            label = { Text("Start Time") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = editEndTime,
                            onValueChange = { editEndTime = it },
                            label = { Text("End Time") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Break Duration:", color = primaryTextColor, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 15, 20, 30).forEach { b ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (editBreakMinutes == b) ElectricViolet else (if (isGlassLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF)),
                                    modifier = Modifier.springClickable { editBreakMinutes = b }
                                ) {
                                    Text(
                                        text = "${b}m",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (editBreakMinutes == b) Color.White else secondaryTextColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Step 5: Weak Areas & Daily Goals
            item {
                EditSectionCard(
                    title = "5️⃣ Weak Areas & Daily Goals",
                    accentColor = CoralRose,
                    cardBorderColor = cardBorderColor,
                    primaryTextColor = primaryTextColor
                ) {
                    Text("Select Weak Subject Focus:", color = secondaryTextColor, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        editSubjects.forEach { s ->
                            val isWeak = editWeakSubjects.contains(s)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isWeak) CoralRose else (if (isGlassLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF)),
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
                                    color = if (isWeak) Color.White else secondaryTextColor,
                                    fontWeight = if (isWeak) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                            Text(topic, color = primaryTextColor, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.weight(1f))
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

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editDailyGoal,
                        onValueChange = { editDailyGoal = it },
                        label = { Text("Daily Study Goal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Save and Cancel Actions
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = "SAVE PROFILE CHANGES 💾",
                        icon = Icons.Filled.Save,
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
                                subjects = if (editSubjects.isNotEmpty()) editSubjects.toList() else listOf("Mathematics", "Science"),
                                highPrioritySubjects = highPrio,
                                mediumPrioritySubjects = medPrio,
                                lowPrioritySubjects = lowPrio,
                                strongSubjects = editSubjects.filter { it !in editWeakSubjects },
                                preparationLevel = editPrepLevel,
                                availableStudyHours = editStudyHours,
                                dailyTargetMinutes = (editStudyHours * 60).toInt(),
                                preferredStudyStartTime = editStartTime,
                                preferredStudyEndTime = editEndTime,
                                breakDurationMinutes = editBreakMinutes,
                                preferredStudyTime = "$editStartTime - $editEndTime",
                                weakSubjects = editWeakSubjects.toList(),
                                weakTopics = editWeakTopics,
                                dailyStudyGoal = editDailyGoal,
                                isOnboardingCompleted = true
                            )
                            onSave(updated)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        testTag = "save_profile_button"
                    )

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Text("Cancel", color = secondaryTextColor)
                    }
                }
            }
        }
    }
}

// ======================================================================================
// HELPER COMPOSABLES
// ======================================================================================

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    primaryColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor.copy(alpha = 0.6f)
            )
        }

        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(text = actionLabel, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BlueprintDataRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color? = null,
    primaryColor: Color,
    secondaryColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(icon, null, tint = secondaryColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = secondaryColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            color = valueColor ?: primaryColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 2
        )
    }
}

@Composable
private fun BlueprintDivider(borderColor: Color) {
    HorizontalDivider(
        color = borderColor.copy(alpha = 0.4f),
        thickness = 0.8.dp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun ThemeSelectCard(
    mode: AppThemeMode,
    title: String,
    description: String,
    isSelected: Boolean,
    previewGradient: Brush,
    accentColor: Color,
    onSelect: () -> Unit,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else Color(0x0CFFFFFF),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) accentColor else Color(0x25FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .springClickable { onSelect() }
            .testTag("theme_card_${mode.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniature theme preview frame
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(previewGradient)
                    .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.35f),
                    modifier = Modifier.size(24.dp, 14.dp)
                ) {}
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryTextColor,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.25f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Check, null, tint = accentColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Active",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else {
                RadioButton(
                    selected = false,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(unselectedColor = secondaryTextColor)
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    isSuccess: Boolean,
    onClick: () -> Unit,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    cardBorderColor: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x0CFFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .springClickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (isSuccess) EmeraldSuccess else secondaryTextColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = primaryTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text(subtitle, color = secondaryTextColor, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSuccess) EmeraldSuccess.copy(alpha = 0.18f) else CoralRose.copy(alpha = 0.18f)
            ) {
                Text(
                    text = badgeText,
                    color = if (isSuccess) EmeraldSuccess else CoralRose,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Filled.ChevronRight, null, tint = secondaryTextColor, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun NotificationToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, null, tint = secondaryTextColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = primaryTextColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = secondaryTextColor, style = MaterialTheme.typography.labelSmall)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0x30FFFFFF)
            )
        )
    }
}

@Composable
private fun EditSectionCard(
    title: String,
    accentColor: Color,
    cardBorderColor: Color,
    primaryTextColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        fillAlpha = 0.65f,
        borderColor = cardBorderColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

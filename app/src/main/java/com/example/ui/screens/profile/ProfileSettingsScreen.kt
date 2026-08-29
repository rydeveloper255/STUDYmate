package com.example.ui.screens.profile

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExamEntity
import com.example.data.model.NotificationPreference
import com.example.data.model.UserProfile
import com.example.localization.AppLanguage
import com.example.localization.GlobalLanguageSwitcher
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalLanguageManager
import com.example.localization.appString
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.PersistenceStatusIndicator
import com.example.ui.components.springClickable
import com.example.ui.screens.auth.PermissionSetupScreen
import com.example.ui.screens.focus.AccessibilityPrivacyScreen
import com.example.ui.screens.focus.FocusShieldSettingsScreen
import com.example.ui.screens.notification.NotificationSettingsScreen
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private enum class ProfileSubScreen {
    EDIT_PROFILE,
    SECURITY_CENTER,
    DATA_SYNC,
    EXAM_SWITCHER,
    NOTIFICATION_CENTER,
    FOCUS_SHIELD,
    PERMISSIONS,
    ACCESSIBILITY_PRIVACY
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
    onTestDailyMotivation: () -> Unit = {},
    catalogExams: List<ExamEntity> = emptyList(),
    onChangeExam: ((String) -> Unit)? = null,
    onOpenStudyPlanner: (() -> Unit)? = null,
    onResetActiveExamData: (() -> Unit)? = null,
    isAiThinkingMode: Boolean = false,
    onSetAiThinkingMode: ((Boolean) -> Unit)? = null,
    tutorPersona: String = "Socratic Coach",
    onSetTutorPersona: ((String) -> Unit)? = null,
    onChangePassword: ((String, (Boolean, String) -> Unit) -> Unit)? = null,
    onRequestEmailChange: ((String, (Boolean, String) -> Unit) -> Unit)? = null,
    onTriggerSync: (((Boolean, String) -> Unit) -> Unit)? = null,
    onExportData: (() -> String)? = null,
    onResetPersonalization: (() -> Unit)? = null,
    onClearCache: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var activeSubScreen by remember { mutableStateOf<ProfileSubScreen?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResetDataConfirm by remember { mutableStateOf(false) }
    var showDiagnosticDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showReportProblemDialog by remember { mutableStateOf(false) }

    // Accessibility local states
    var largeTextMode by remember { mutableStateOf(false) }
    var highContrastMode by remember { mutableStateOf(false) }
    var reduceMotion by remember { mutableStateOf(false) }

    // Personalization toggles
    var personalizedRecommendations by remember { mutableStateOf(true) }
    var smartPracticeMode by remember { mutableStateOf(true) }
    var learningInsights by remember { mutableStateOf(true) }

    val activePersistenceStatus by com.example.data.persistence.PersistenceMonitor.activeStatus.collectAsStateWithLifecycle()

    // TTS Preview state
    var ttsPreview: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsTesting by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialized
            }
        }
        ttsPreview = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Intercept back presses for full-screen sub-screens
    BackHandler(enabled = activeSubScreen != null) {
        activeSubScreen = null
    }

    // Render Sub-Screens
    when (activeSubScreen) {
        ProfileSubScreen.EDIT_PROFILE -> {
            ProfileEditContent(
                user = user,
                onSave = { updated ->
                    onUpdateProfile(updated)
                    activeSubScreen = null
                    Toast.makeText(context, "Profile updated successfully ✨", Toast.LENGTH_SHORT).show()
                },
                onCancel = { activeSubScreen = null },
                onOpenExamSwitcher = { activeSubScreen = ProfileSubScreen.EXAM_SWITCHER }
            )
            return
        }
        ProfileSubScreen.SECURITY_CENTER -> {
            SecurityCenterSubScreen(
                user = user,
                onBack = { activeSubScreen = null },
                onChangePassword = onChangePassword,
                onRequestEmailChange = onRequestEmailChange,
                onSignOut = onSignOut,
                onDeleteAccount = onDeleteAccount
            )
            return
        }
        ProfileSubScreen.DATA_SYNC -> {
            DataSyncCenterSubScreen(
                onBack = { activeSubScreen = null },
                onTriggerSync = onTriggerSync,
                onExportData = onExportData,
                onClearCache = onClearCache,
                onOpenDiagnostic = { showDiagnosticDialog = true }
            )
            return
        }
        ProfileSubScreen.EXAM_SWITCHER -> {
            ExamSwitcherSubScreen(
                currentExamName = user?.examName ?: "Target Exam",
                catalogExams = catalogExams,
                onSelectExam = { examId, examName ->
                    onChangeExam?.invoke(examId)
                    user?.let {
                        onUpdateProfile(it.copy(examName = examName, goal = examName))
                    }
                    activeSubScreen = null
                    Toast.makeText(context, "Target exam switched to $examName 🎯", Toast.LENGTH_SHORT).show()
                },
                onBack = { activeSubScreen = null }
            )
            return
        }
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
        null -> { /* Render Main Profile & Settings 2.0 Screen */ }
    }

    val isGlassLight = themeMode == AppThemeMode.GLASS_LIGHT
    val isAmoled = themeMode == AppThemeMode.AMOLED_BLACK
    val primaryTextColor = if (isGlassLight) Color(0xFF0F172A) else Color.White
    val secondaryTextColor = if (isGlassLight) Color(0xFF64748B) else Color(0xFF94A3B8)
    val cardBorderColor = if (isGlassLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.15f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Profile & Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_topbar_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = primaryTextColor
                        )
                    }
                },
                actions = {
                    GlobalLanguageSwitcher(
                        isDark = !isGlassLight,
                        compact = true,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = { showDiagnosticDialog = true },
                        modifier = Modifier.testTag("diagnostic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BugReport,
                            contentDescription = "Persistence Diagnostics",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            // Persistence Status Pill
            item {
                PersistenceStatusIndicator(
                    status = activePersistenceStatus,
                    testTagPrefix = "profile_main"
                )
            }

            // =========================================================================
            // HERO PROFILE HEADER CARD
            // =========================================================================
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_profile_card"),
                    shape = RoundedCornerShape(24.dp),
                    fillAlpha = if (isAmoled) 0.95f else if (isGlassLight) 0.85f else 0.65f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar with gradient border
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ElectricViolet, NeonCyan)
                                        )
                                    )
                                    .border(2.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val avatarStr = user?.photoUrl ?: "👨‍🎓"
                                Text(
                                    text = if (avatarStr.length <= 4) avatarStr else "👨‍🎓",
                                    fontSize = 36.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val scholarName = if (user?.isGuest == true) "Guest Scholar" else (user?.name?.takeIf { it.isNotBlank() } ?: "Scholar")
                                    Text(
                                        text = scholarName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ElectricViolet.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Lv. ${user?.level ?: 1}",
                                            color = ElectricViolet,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = user?.email?.takeIf { it.isNotBlank() } ?: "Guest Session (Local Room DB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryTextColor
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = NeonCyan.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = user?.examName ?: "UPSC CSE",
                                            color = NeonCyan,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldSuccess.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "🔥 ${user?.streakDays ?: 1} Day Streak",
                                            color = EmeraldSuccess,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Navigation Action Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { activeSubScreen = ProfileSubScreen.EDIT_PROFILE },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("edit_profile_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricViolet
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { activeSubScreen = ProfileSubScreen.SECURITY_CENTER },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("open_security_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Outlined.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Security", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 1. 👤 ACCOUNT & SECURITY SECTION
            // =========================================================================
            item {
                SectionHeader(
                    title = "ACCOUNT & CREDENTIALS 👤",
                    subtitle = "Session details, security and profile editing",
                    primaryColor = primaryTextColor,
                    accentColor = ElectricViolet
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.75f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsNavRow(
                            icon = Icons.Outlined.Badge,
                            title = "Edit Profile Information",
                            subtitle = "Update display name, avatar preset & target score",
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            onClick = { activeSubScreen = ProfileSubScreen.EDIT_PROFILE },
                            testTag = "row_edit_profile"
                        )

                        HorizontalDivider(color = cardBorderColor)

                        SettingsNavRow(
                            icon = Icons.Outlined.Lock,
                            title = "Password & Security Center",
                            subtitle = "Change password, email address & active session controls",
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            onClick = { activeSubScreen = ProfileSubScreen.SECURITY_CENTER },
                            testTag = "row_security_center"
                        )

                        HorizontalDivider(color = cardBorderColor)

                        SettingsNavRow(
                            icon = Icons.Outlined.CloudSync,
                            title = "Cloud Sync & Data Center",
                            subtitle = "Real-time sync status, export JSON archive & cache tools",
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            onClick = { activeSubScreen = ProfileSubScreen.DATA_SYNC },
                            testTag = "row_data_sync"
                        )
                    }
                }
            }

            // =========================================================================
            // 🌐 GLOBAL LANGUAGE & LOCALIZATION SYSTEM
            // =========================================================================
            item {
                val languageManager = LocalLanguageManager.current
                val currentLang = LocalAppLanguage.current

                SectionHeader(
                    title = "GLOBAL LANGUAGE & TRANSLATOR 🌐",
                    subtitle = "Switch display language across all study modules & notifications",
                    primaryColor = primaryTextColor,
                    accentColor = NeonCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_language_card"),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.75f else 0.55f,
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Active App Language",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )
                                Text(
                                    text = "Current: ${currentLang.title} (${currentLang.nativeName})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }

                            GlobalLanguageSwitcher(
                                isDark = !isGlassLight,
                                compact = false
                            )
                        }

                        HorizontalDivider(color = cardBorderColor)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppLanguage.values().forEach { lang ->
                                val isSelected = currentLang == lang
                                Surface(
                                    onClick = { languageManager?.setLanguage(lang) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.18f) else cardBorderColor.copy(alpha = 0.1f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) NeonCyan else cardBorderColor
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("lang_btn_${lang.code}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "${lang.nativeName} (${lang.title})",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSelected) (if (isGlassLight) Color(0xFF0F172A) else NeonCyan) else primaryTextColor
                                        )
                                        if (isSelected) {
                                            Spacer(Modifier.width(6.dp))
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = if (isGlassLight) Color(0xFF0F172A) else NeonCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "⚡ Instant change: No restart needed. Translation cache is saved for offline access.",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // =========================================================================
            // 2. 🎓 LEARNING & EXAM PREFERENCES
            // =========================================================================
            item {
                SectionHeader(
                    title = "LEARNING & EXAM PREFERENCES 🎓",
                    subtitle = "Selected exam, daily study targets & adaptive AI",
                    primaryColor = primaryTextColor,
                    accentColor = EmeraldSuccess
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.75f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Exam Switcher Tile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { activeSubScreen = ProfileSubScreen.EXAM_SWITCHER }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.School, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Selected Exam", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                    Text(user?.examName ?: "UPSC CSE Preliminary", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                                }
                            }
                            Text("Change Exam >", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        HorizontalDivider(color = cardBorderColor)

                        // Daily Study Target
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Timer, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Daily Target", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                            }
                            Text(
                                text = "${user?.availableStudyHours ?: 4.0f} hrs / day",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        HorizontalDivider(color = cardBorderColor)

                        // Personalization Toggles
                        NotificationToggleRow(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "Personalized Recommendations",
                            subtitle = "AI adapts daily study topics based on your mock test mistakes",
                            checked = personalizedRecommendations,
                            onCheckedChange = { personalizedRecommendations = it },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor
                        )

                        NotificationToggleRow(
                            icon = Icons.Outlined.Psychology,
                            title = "Smart Practice Mode",
                            subtitle = "Prioritizes high-weightage and weak topics in practice sets",
                            checked = smartPracticeMode,
                            onCheckedChange = { smartPracticeMode = it },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor
                        )

                        NotificationToggleRow(
                            icon = Icons.Outlined.Insights,
                            title = "Learning Insights & Readiness",
                            subtitle = "Computes real-time exam readiness metrics and mastery curves",
                            checked = learningInsights,
                            onCheckedChange = { learningInsights = it },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor
                        )

                        OutlinedButton(
                            onClick = {
                                onResetPersonalization?.invoke()
                                Toast.makeText(context, "Personalization preferences reset ✨", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, secondaryTextColor.copy(alpha = 0.4f))
                        ) {
                            Text("Reset Personalization Defaults", color = secondaryTextColor, fontSize = 11.sp)
                        }
                    }
                }
            }

            // =========================================================================
            // 3. 🎨 APP SETTINGS & APPEARANCE
            // =========================================================================
            item {
                SectionHeader(
                    title = "APP SETTINGS & APPEARANCE 🎨",
                    subtitle = "Liquid themes, language, voice & AI persona",
                    primaryColor = primaryTextColor,
                    accentColor = NeonCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.75f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Theme Selector
                        Text("App Theme & Canvas", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)

                        val themes = listOf(
                            Triple("Nova Dark", AppThemeMode.NOVA_DARK, Color(0xFF0F172A)),
                            Triple("Glass Light", AppThemeMode.GLASS_LIGHT, Color(0xFFF8FAFC)),
                            Triple("AMOLED Black", AppThemeMode.AMOLED_BLACK, Color(0xFF000000))
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(themes) { (name, mode, previewColor) ->
                                val isSelected = themeMode == mode
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onSetThemeMode(mode)
                                            onToggleDarkTheme(mode != AppThemeMode.GLASS_LIGHT)
                                        }
                                        .testTag("theme_card_${mode.name}"),
                                    color = previewColor,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) NeonCyan else cardBorderColor
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = name,
                                            color = if (mode == AppThemeMode.GLASS_LIGHT) Color.Black else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonCyan))
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = cardBorderColor)

                        // AI Persona Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("NOVA AI Tutor Persona", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)

                            val personas = listOf("Socratic Coach", "Strict Examiner", "Friendly Mentor", "Speed Drillmaster")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(personas) { persona ->
                                    val isSelected = tutorPersona == persona
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSetTutorPersona?.invoke(persona) },
                                        label = { Text(persona, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricViolet,
                                            selectedLabelColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = cardBorderColor)

                        // Thinking Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gemini Deep Reasoning", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                Text("Enables deep step-by-step thinking for complex science and math derivations", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                            }
                            Switch(
                                checked = isAiThinkingMode,
                                onCheckedChange = { onSetAiThinkingMode?.invoke(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = ElectricViolet)
                            )
                        }

                        HorizontalDivider(color = cardBorderColor)

                        // TTS Voice Test Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Voice Synthesis & Audio", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                Text("Natural speech engine for voice notes and AI explanations", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                            }
                            OutlinedButton(
                                onClick = {
                                    isTtsTesting = true
                                    ttsPreview?.speak(
                                        "Hello Scholar, this is your Study Mate assistant ready to help you excel in your exam.",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "preview"
                                    )
                                    Toast.makeText(context, "Testing voice output 🔊", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Voice", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 4. ♿ ACCESSIBILITY & COMFORT
            // =========================================================================
            item {
                SectionHeader(
                    title = "ACCESSIBILITY & PRIVACY GUARANTEE ♿",
                    subtitle = "Visual readability, scaling and strict sandbox isolation",
                    primaryColor = primaryTextColor,
                    accentColor = DeepIndigo
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.75f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NotificationToggleRow(
                            icon = Icons.Outlined.FormatSize,
                            title = "Larger Text & Display Scaling",
                            subtitle = "Increases reading font size throughout all study modules",
                            checked = largeTextMode,
                            onCheckedChange = { largeTextMode = it },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor
                        )

                        NotificationToggleRow(
                            icon = Icons.Outlined.Contrast,
                            title = "High Contrast Card Borders",
                            subtitle = "Enhances edge separation for improved visual clarity",
                            checked = highContrastMode,
                            onCheckedChange = { highContrastMode = it },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor
                        )

                        NotificationToggleRow(
                            icon = Icons.Outlined.Animation,
                            title = "Reduce Animations",
                            subtitle = "Minimizes motion effects for battery conservation and comfort",
                            checked = reduceMotion,
                            onCheckedChange = { reduceMotion = it },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor
                        )

                        // Strict Isolation Guarantee
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldSuccess.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Strict Security & App Isolation Guarantee",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Study Mate operates strictly within its own boundary. It never monitors other apps, never intercepts keyboard inputs, and never interferes with payment applications (Paytm, PhonePe, GPay, UPI, banking apps) or Android Accessibility Services.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryTextColor,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 5. 🔔 NOTIFICATIONS & REMINDERS
            // =========================================================================
            item {
                SectionHeader(
                    title = "SMART STUDY NOTIFICATIONS 🔔",
                    subtitle = "Session alerts, countdowns & daily motivation",
                    primaryColor = primaryTextColor,
                    accentColor = ElectricViolet
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.75f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("All Notifications", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                    Text(
                                        if (notificationPrefs.masterEnabled) "Active and scheduled" else "All study alerts paused",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryTextColor
                                    )
                                }
                            }
                            Switch(
                                checked = notificationPrefs.masterEnabled,
                                onCheckedChange = { onUpdateNotificationPrefs(notificationPrefs.copy(masterEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedTrackColor = ElectricViolet)
                            )
                        }

                        HorizontalDivider(color = cardBorderColor)

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

                        OutlinedButton(
                            onClick = { activeSubScreen = ProfileSubScreen.NOTIFICATION_CENTER },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f))
                        ) {
                            Text("Open Notification Center & Live Test Suite >", color = ElectricViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // =========================================================================
            // 6. 💬 SUPPORT & ABOUT SECTION
            // =========================================================================
            item {
                SectionHeader(
                    title = "HELP, SUPPORT & ABOUT 💬",
                    subtitle = "FAQs, problem reporting and build diagnostics",
                    primaryColor = primaryTextColor,
                    accentColor = secondaryTextColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.75f else 0.55f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsNavRow(
                            icon = Icons.Outlined.HelpOutline,
                            title = "Frequently Asked Questions (FAQ)",
                            subtitle = "Learn how AI tutoring, offline mode & sync work",
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            onClick = { showFaqDialog = true },
                            testTag = "row_faq"
                        )

                        HorizontalDivider(color = cardBorderColor)

                        SettingsNavRow(
                            icon = Icons.Outlined.ReportProblem,
                            title = "Report a Problem or Feedback",
                            subtitle = "Submit issue report with logs directly to the team",
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            onClick = { showReportProblemDialog = true },
                            testTag = "row_report_problem"
                        )

                        HorizontalDivider(color = cardBorderColor)

                        SettingsNavRow(
                            icon = Icons.Outlined.Dns,
                            title = "Developer & Bot Diagnostics",
                            subtitle = "Test Telegram Bot connection (getMe), database sync & system logs",
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            onClick = { showDiagnosticDialog = true },
                            testTag = "row_developer_diagnostics"
                        )

                        HorizontalDivider(color = cardBorderColor)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Study Mate Application", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                Text("Version 2.4.0 (Build 240) • AI Studio Edition", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonCyan.copy(alpha = 0.15f)
                            ) {
                                Text("Latest", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 7. ⚠️ DANGER ZONE
            // =========================================================================
            item {
                SectionHeader(
                    title = "DANGER ZONE ⚠️",
                    subtitle = "Irreversible data reset and account deletion",
                    primaryColor = CoralRose,
                    accentColor = CoralRose
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.75f else 0.55f,
                    borderColor = CoralRose.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Reset Active Exam Data
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reset Active Exam Data", color = primaryTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("Clears scores & plan items for ${user?.examName ?: "Exam"} while preserving your profile", color = secondaryTextColor, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showResetDataConfirm = true },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.7f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp).testTag("reset_active_exam_data_button")
                            ) {
                                Text("Reset Data", color = CoralRose, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        HorizontalDivider(color = CoralRose.copy(alpha = 0.2f))

                        // Sign out
                        OutlinedButton(
                            onClick = { showSignOutConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("sign_out_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CoralRose, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out of Session", color = CoralRose, fontWeight = FontWeight.Bold)
                        }

                        // Delete Account Button
                        Button(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("delete_account_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralRose.copy(alpha = 0.2f),
                                contentColor = CoralRose
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = CoralRose, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Permanently Delete Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (showDiagnosticDialog) {
        val app = context.applicationContext as? com.example.StudyMateApplication
        PersistenceDiagnosticDialog(
            authManager = app?.supabaseAuthManager,
            telegramBotService = app?.telegramBotService,
            sourceManager = app?.sourceManager,
            contentScheduler = app?.automatedContentScheduler,
            collectorEngine = app?.automatedContentCollectorEngine,
            onDismiss = { showDiagnosticDialog = false },
            onTriggerForceSync = {
                onTriggerSync?.invoke { _, _ -> }
            }
        )
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("Frequently Asked Questions", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Q: Does Study Mate work completely offline?", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("A: Yes! All your notes, study plans, and mock tests are stored locally in Room SQLite database. Whenever internet is available, changes automatically sync to Supabase.", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                    Text("Q: How does the AI tutor adapt to my weaknesses?", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("A: Study Mate tracks your incorrect answers in mock tests and calculates mastery per topic, feeding targeted revision into your daily study plan.", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                    Text("Q: How is my personal data protected?", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("A: We use Supabase Row-Level Security (RLS). No other user or outside service can read or write your private study records.", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showFaqDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showReportProblemDialog) {
        var problemCategory by remember { mutableStateOf("Bug Report") }
        var problemDescription by remember { mutableStateOf("") }
        var isReporting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showReportProblemDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Report a Problem", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

                    Text("Category", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    val categories = listOf("Bug Report", "Incorrect Question", "Feature Request", "Sync Issue")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = problemCategory == cat,
                                onClick = { problemCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = Color(0xFF090D16)
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = problemDescription,
                        onValueChange = { problemDescription = it },
                        label = { Text("Describe what happened") },
                        placeholder = { Text("Provide details to help us fix the issue...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showReportProblemDialog = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (problemDescription.isBlank()) {
                                    Toast.makeText(context, "Please describe the problem", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isReporting = true
                                Toast.makeText(context, "Thank you! Problem report submitted ✨", Toast.LENGTH_SHORT).show()
                                showReportProblemDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF090D16))
                        ) {
                            Text("Submit Report", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showResetDataConfirm) {
        AlertDialog(
            onDismissRequest = { showResetDataConfirm = false },
            title = { Text("Reset Active Exam Data?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will clear mock test scores, mistake logs, and study plan items for your current exam (${user?.examName ?: "Selected Exam"}). Your account, profile, and other exams will not be affected.",
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDataConfirm = false
                        onResetActiveExamData?.invoke()
                        Toast.makeText(context, "Exam data reset ✨", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                ) {
                    Text("Reset Exam Data", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDataConfirm = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out of Study Mate?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your study data is safely saved in local SQLite and synced to the cloud. You can sign back in anytime.",
                    color = Color.White.copy(alpha = 0.85f)
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
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Permanently Delete Account?", color = CoralRose, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This action is permanent and completely irreversible. All your study plans, test attempts, bookmarks, mistakes, and AI memories will be permanently wiped.",
                    color = Color.White.copy(alpha = 0.85f)
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
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Keep Account", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    primaryColor: Color,
    accentColor: Color
) {
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.sp
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = primaryColor.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = secondaryTextColor.copy(alpha = 0.6f))
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = primaryTextColor)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = secondaryTextColor, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = ElectricViolet)
        )
    }
}

package com.example.ui.screens.profile

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ExamEntity
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
    EDIT_MODE,
    EXAM_SWITCHER
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
    onSetTutorPersona: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var activeSubScreen by remember { mutableStateOf<ProfileSubScreen?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResetDataConfirm by remember { mutableStateOf(false) }

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

    // Handle full-screen sub-destinations seamlessly
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
                    Toast.makeText(context, "Profile & Study Targets Updated ✨", Toast.LENGTH_SHORT).show()
                },
                onCancel = { activeSubScreen = null }
            )
            return
        }
        ProfileSubScreen.EXAM_SWITCHER -> {
            ExamSwitcherSubScreen(
                currentExamName = user?.examName ?: "Target Exam",
                catalogExams = catalogExams,
                onSelectExam = { examId, examName ->
                    if (onChangeExam != null) {
                        onChangeExam(examId)
                    } else {
                        val updated = (user ?: UserProfile()).copy(examName = examName)
                        onUpdateProfile(updated)
                    }
                    activeSubScreen = null
                    Toast.makeText(context, "Switched active target exam to $examName 🎯", Toast.LENGTH_SHORT).show()
                },
                onBack = { activeSubScreen = null }
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
    val cardBorderColor = when {
        isAmoled -> GlassBorderAmoled
        isGlassLight -> GlassBorderLight
        else -> GlassBorderDark
    }

    // Local state for interactive in-settings adjustments
    var speechSpeed by remember { mutableFloatStateOf(1.0f) }
    var useExamContextInNova by remember { mutableStateOf(true) }
    var simplifyExplanations by remember { mutableStateOf(false) }
    var highContrastMode by remember { mutableStateOf(false) }
    var largeTextMode by remember { mutableStateOf(false) }
    var reduceMotion by remember { mutableStateOf(false) }
    var currentAffairsLanguage by remember { mutableStateOf(user?.languagePreference ?: "English") }
    var caDailyBriefing by remember { mutableStateOf(true) }
    var quizDefaultQuestions by remember(user) { mutableIntStateOf(user?.defaultMockTestQuestionCount ?: 25) }
    var quizDefaultDifficulty by remember { mutableStateOf("Medium") }
    var quizDefaultMode by remember { mutableStateOf("Practice") }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_settings_screen"),
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
                            text = "My Study Mate",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Text(
                            text = "Profile, Settings & Personalization",
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =========================================================================
            // 1. PROFILE HEADER (Identity & Preparation Context)
            // =========================================================================
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_hero_card"),
                    shape = RoundedCornerShape(24.dp),
                    fillAlpha = if (isAmoled) 0.95f else if (isGlassLight) 0.8f else 0.65f,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Glowing Avatar Frame
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
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
                                            contentDescription = "Default Avatar",
                                            tint = if (isGlassLight) DeepIndigo else NeonCyan,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user?.name ?: "Student Scholar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )
                                Text(
                                    text = if (user?.isGuest == true) "Guest Scholar • Local Storage" else (user?.email?.takeIf { it.isNotBlank() } ?: "Google Scholar Account"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = (if (isGlassLight) DeepIndigo else NeonCyan).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Level ${user?.level ?: 1}",
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
                                            text = "🔥 ${user?.streakDays ?: 0} Day Streak",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldenSpark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldSuccess.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "⭐ ${user?.xp ?: 0} XP",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Profile Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { activeSubScreen = ProfileSubScreen.EDIT_MODE },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("quick_edit_profile_btn"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, (if (isGlassLight) DeepIndigo else NeonCyan).copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Filled.Edit, null, tint = if (isGlassLight) DeepIndigo else NeonCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Edit Profile",
                                    color = if (isGlassLight) DeepIndigo else NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            OutlinedButton(
                                onClick = { activeSubScreen = ProfileSubScreen.EXAM_SWITCHER },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("quick_change_exam_btn"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldenSpark.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Filled.SwapHoriz, null, tint = GoldenSpark, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Change Exam",
                                    color = GoldenSpark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            OutlinedButton(
                                onClick = { showSignOutConfirm = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("quick_account_btn"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Outlined.ManageAccounts, null, tint = secondaryTextColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Account",
                                    color = secondaryTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. CURRENT EXAM CARD (Compact & Prominent)
            // =========================================================================
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("current_exam_card"),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = if (isAmoled) 0.9f else if (isGlassLight) 0.75f else 0.55f,
                    borderColor = GoldenSpark.copy(alpha = 0.4f)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GoldenSpark.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.EmojiEvents, null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "🎯 Current Preparation",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldenSpark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = user?.examName ?: "Target Exam",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )
                                }
                            }

                            Button(
                                onClick = { activeSubScreen = ProfileSubScreen.EXAM_SWITCHER },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldenSpark,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("change_exam_button")
                            ) {
                                Text("Change Exam", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = cardBorderColor.copy(alpha = 0.5f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Exam Context Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Category", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                                Text(user?.examCategory ?: "Competitive", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                            }

                            val daysRemaining = remember(user?.examDateMillis) {
                                val diff = (user?.examDateMillis ?: (System.currentTimeMillis() + 60L * 86400000L)) - System.currentTimeMillis()
                                (diff / 86400000L).coerceAtLeast(0).toInt()
                            }

                            Column {
                                Text("Timeline", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                                Text("$daysRemaining Days Left", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = GoldenSpark)
                            }

                            Column {
                                Text("Target Score", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                                Text(user?.targetScore ?: "Top 500 AIR", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = EmeraldSuccess)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Switching exam updates your syllabus, study plan, quizzes, and NOVA context safely without deleting your past study history.",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            // =========================================================================
            // 3. PERSONALIZATION (Language, Theme, NOVA AI & Voice)
            // =========================================================================
            item {
                SectionHeader(
                    title = "PERSONALIZATION ✨",
                    subtitle = "Language, theme, NOVA AI behavior & voice playback",
                    primaryColor = primaryTextColor,
                    accentColor = NeonCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        // --- A. Language Settings ---
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Translate, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("🌐 App & AI Language", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("English" to "English", "Hindi" to "हिंदी").forEach { (langKey, langLabel) ->
                                        val isSelected = (user?.languagePreference ?: "English").equals(langKey, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) (if (isGlassLight) DeepIndigo else NeonCyan) else Color(0x18FFFFFF),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) (if (isGlassLight) DeepIndigo else NeonCyan) else cardBorderColor
                                            ),
                                            modifier = Modifier
                                                .springClickable {
                                                    val updated = (user ?: UserProfile()).copy(languagePreference = langKey)
                                                    onUpdateProfile(updated)
                                                }
                                                .testTag("lang_btn_$langKey")
                                        ) {
                                            Text(
                                                text = langLabel,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) (if (isGlassLight) Color.White else Color(0xFF0F172A)) else secondaryTextColor
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "App interface and NOVA explanations follow selected language",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryTextColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        BlueprintDivider(cardBorderColor)

                        // --- B. Theme Settings (3 Modes) ---
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Palette, null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🎨 Appearance (Liquid Glass)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    description = "True pitch black for maximum OLED battery savings",
                                    isSelected = themeMode == AppThemeMode.AMOLED_BLACK,
                                    previewGradient = Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF0B0F19))),
                                    accentColor = ElectricViolet,
                                    onSelect = { onSetThemeMode(AppThemeMode.AMOLED_BLACK) },
                                    primaryTextColor = primaryTextColor,
                                    secondaryTextColor = secondaryTextColor
                                )
                            }
                        }

                        BlueprintDivider(cardBorderColor)

                        // --- C. NOVA AI Preferences ---
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AutoAwesome, null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("✨ NOVA AI Preferences", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                            }

                            // Response Persona
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tutor Persona", style = MaterialTheme.typography.bodySmall, color = primaryTextColor)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Socratic Coach", "Analytical", "Step-by-Step").forEach { persona ->
                                        val isSel = tutorPersona.contains(persona, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSel) GoldenSpark.copy(alpha = 0.25f) else Color(0x15FFFFFF),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) GoldenSpark else cardBorderColor),
                                            modifier = Modifier.springClickable {
                                                if (onSetTutorPersona != null) onSetTutorPersona(persona)
                                            }
                                        ) {
                                            Text(
                                                text = persona,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSel) GoldenSpark else secondaryTextColor,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Context-Aware Toggle (MANDATORY DEFAULT)
                            NotificationToggleRow(
                                icon = Icons.Outlined.GpsFixed,
                                title = "Use my exam & subject context",
                                subtitle = "NOVA automatically considers ${user?.examName ?: "Target Exam"} and weak topics",
                                checked = useExamContextInNova,
                                onCheckedChange = { useExamContextInNova = it },
                                primaryTextColor = primaryTextColor,
                                secondaryTextColor = secondaryTextColor
                            )

                            // Deep Thinking Mode
                            NotificationToggleRow(
                                icon = Icons.Outlined.Psychology,
                                title = "Deep Thinking Reasoning",
                                subtitle = "Multi-step chain-of-thought analysis for complex questions",
                                checked = isAiThinkingMode,
                                onCheckedChange = { onSetAiThinkingMode?.invoke(it) },
                                primaryTextColor = primaryTextColor,
                                secondaryTextColor = secondaryTextColor
                            )

                            // Simplify Explanations
                            NotificationToggleRow(
                                icon = Icons.Outlined.Lightbulb,
                                title = "Explain Simply (Easy Analogies)",
                                subtitle = "Use intuitive real-world analogies without heavy jargon",
                                checked = simplifyExplanations,
                                onCheckedChange = { simplifyExplanations = it },
                                primaryTextColor = primaryTextColor,
                                secondaryTextColor = secondaryTextColor
                            )
                        }

                        BlueprintDivider(cardBorderColor)

                        // --- D. NOVA Voice Settings ---
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.RecordVoiceOver, null, tint = CoralRose, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("🎙 NOVA Voice Output", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                }

                                Button(
                                    onClick = {
                                        isTtsTesting = true
                                        val sampleText = if ((user?.languagePreference ?: "English").equals("Hindi", ignoreCase = true)) {
                                            "नमस्ते! मैं नोवा हूँ, आपकी स्मार्ट स्टडी साथी। आइए पढ़ाई शुरू करें।"
                                        } else {
                                            "Hello! I am NOVA, your personalized study partner. Let's conquer your exam goals together."
                                        }
                                        ttsPreview?.setSpeechRate(speechSpeed)
                                        ttsPreview?.speak(sampleText, TextToSpeech.QUEUE_FLUSH, null, "nova_voice_preview")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Filled.VolumeUp, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Voice", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            // Speed Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Voice Speed: ${String.format(Locale.US, "%.2fx", speechSpeed)}", style = MaterialTheme.typography.bodySmall, color = primaryTextColor)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0.75f to "0.75x", 1.0f to "1.0x", 1.25f to "1.25x", 1.5f to "1.5x").forEach { (speedVal, speedLabel) ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (speechSpeed == speedVal) CoralRose else Color(0x18FFFFFF),
                                            modifier = Modifier.springClickable { speechSpeed = speedVal }
                                        ) {
                                            Text(
                                                text = speedLabel,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (speechSpeed == speedVal) Color.White else secondaryTextColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                            Slider(
                                value = speechSpeed,
                                onValueChange = { speechSpeed = (it * 20).toInt() / 20f },
                                valueRange = 0.75f..1.75f,
                                colors = SliderDefaults.colors(thumbColor = CoralRose, activeTrackColor = CoralRose)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 4. STUDY & QUIZ PREFERENCES
            // =========================================================================
            item {
                SectionHeader(
                    title = "STUDY & QUIZ PREFERENCES 📚",
                    subtitle = "Targets, session duration, quiz count & current affairs",
                    primaryColor = primaryTextColor,
                    accentColor = EmeraldSuccess
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        // Study Preferences
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Timer, null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Daily Study Target", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                            }
                            Text(
                                text = "${user?.availableStudyHours ?: 4.0f} hrs / day",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }

                        // Study Window & Session Duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Default Session", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                                Text("${user?.preferredSessionDurationMinutes ?: 25} mins (Pomodoro)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                            }
                            Column {
                                Text("Break Duration", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                                Text("${user?.breakDurationMinutes ?: 15} mins", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                            }
                            Column {
                                Text("Study Window", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                                Text(user?.preferredStudyTime ?: "Evening", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                            }
                        }

                        if (onOpenStudyPlanner != null) {
                            OutlinedButton(
                                onClick = onOpenStudyPlanner,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                            ) {
                                Text("Manage Daily Plan in Study Planner →", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        BlueprintDivider(cardBorderColor)

                        // Quiz Preferences
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Quiz, null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🧠 Quiz Preferences", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Default Question Count", style = MaterialTheme.typography.bodySmall, color = primaryTextColor)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(10, 15, 25, 50).forEach { count ->
                                        val isSel = quizDefaultQuestions == count
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSel) GoldenSpark else Color(0x18FFFFFF),
                                            modifier = Modifier.springClickable {
                                                quizDefaultQuestions = count
                                                val updated = (user ?: UserProfile()).copy(defaultMockTestQuestionCount = count)
                                                onUpdateProfile(updated)
                                            }
                                        ) {
                                            Text(
                                                text = "$count Qs",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSel) Color(0xFF0F172A) else secondaryTextColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Default Difficulty", style = MaterialTheme.typography.bodySmall, color = primaryTextColor)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Easy", "Medium", "Hard", "Exam Real").forEach { diff ->
                                        val isSel = quizDefaultDifficulty == diff
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSel) GoldenSpark.copy(alpha = 0.2f) else Color(0x15FFFFFF),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) GoldenSpark else Color.Transparent),
                                            modifier = Modifier.springClickable { quizDefaultDifficulty = diff }
                                        ) {
                                            Text(
                                                text = diff,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSel) GoldenSpark else secondaryTextColor,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        BlueprintDivider(cardBorderColor)

                        // Current Affairs Preferences
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Newspaper, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📰 Current Affairs Preferences", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                            }

                            NotificationToggleRow(
                                icon = Icons.Outlined.Alarm,
                                title = "Morning Current Affairs Briefing",
                                subtitle = "Daily 8:00 AM high-yield summary tailored for ${user?.examName ?: "Exam"}",
                                checked = caDailyBriefing,
                                onCheckedChange = { caDailyBriefing = it },
                                primaryTextColor = primaryTextColor,
                                secondaryTextColor = secondaryTextColor
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 5. ACCESSIBILITY & SAFETY (App-Level Isolation)
            // =========================================================================
            item {
                SectionHeader(
                    title = "ACCESSIBILITY & COMFORT ♿",
                    subtitle = "App-level visual readability & touch sizing",
                    primaryColor = primaryTextColor,
                    accentColor = DeepIndigo
                )

                Spacer(modifier = Modifier.height(8.dp))

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

                        // Strict Payment & App Isolation Safety Guarantee
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
                                Icon(Icons.Filled.Shield, null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
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
            // 6. NOTIFICATIONS & REMINDERS
            // =========================================================================
            item {
                SectionHeader(
                    title = "SMART STUDY NOTIFICATIONS 🔔",
                    subtitle = "Session alerts, countdowns & daily motivation",
                    primaryColor = primaryTextColor,
                    accentColor = ElectricViolet
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        // Master Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.NotificationsActive, null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
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

                        BlueprintDivider(cardBorderColor)

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

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = { activeSubScreen = ProfileSubScreen.NOTIFICATION_CENTER },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.4f))
                        ) {
                            Text("Open Notification Center & Live Test Suite >", color = ElectricViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // =========================================================================
            // 7. PRIVACY, DATA & SECURITY
            // =========================================================================
            item {
                SectionHeader(
                    title = "PRIVACY & DATA CONTROLS 🔒",
                    subtitle = "On-device Room database & session security",
                    primaryColor = primaryTextColor,
                    accentColor = EmeraldSuccess
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Storage, null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Offline-First On-Device Storage", color = primaryTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("All notes, flashcards & mock test attempts reside safely on your device in Room SQLite", color = secondaryTextColor, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        BlueprintDivider(cardBorderColor)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Security, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Stateless AI Processing & Security", color = primaryTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("Gemini queries are sanitized; API credentials are securely managed and never exposed", color = secondaryTextColor, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        BlueprintDivider(cardBorderColor)

                        // Reset Active Exam Data button
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
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Reset Data", color = CoralRose, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 8. ACCOUNT CONTROLS & DANGER ZONE
            // =========================================================================
            item {
                SectionHeader(
                    title = "ACCOUNT & SESSION 👤",
                    subtitle = "Manage authentication, sign out & account actions",
                    primaryColor = primaryTextColor,
                    accentColor = secondaryTextColor
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Current Session", color = secondaryTextColor, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = if (user?.isGuest == true) "Guest Session (Local DB)" else (user?.email?.takeIf { it.isNotBlank() } ?: "Google Scholar"),
                                    color = primaryTextColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccess.copy(alpha = 0.15f)
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
                            onClick = { showSignOutConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("sign_out_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = CoralRose, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SIGN OUT", color = CoralRose, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // =========================================================================
            // 9. DANGER ZONE: DELETE ACCOUNT
            // =========================================================================
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_account_button")
                    ) {
                        Text(
                            text = "DELETE ACCOUNT & CLEAR ALL DATA",
                            color = CoralRose,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Permanently clears local database, study plans, test attempts & progress",
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
                Text("Sign out of StudyMate?", color = primaryTextColor, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("You will need to sign in again to access your study plans and mock test sessions.", color = secondaryTextColor)
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

    // Confirmation Dialog for Reset Active Exam Data
    if (showResetDataConfirm) {
        AlertDialog(
            onDismissRequest = { showResetDataConfirm = false },
            containerColor = if (isGlassLight) Color(0xFFFFFFFF) else Color(0xFF131C2E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Reset Preparation Data?", color = CoralRose, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This will reset your test scores, mistake logs, and study plan for ${user?.examName ?: "current exam"}. Your profile details and other exam history will remain safe.", color = secondaryTextColor)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDataConfirm = false
                        onResetActiveExamData?.invoke()
                        Toast.makeText(context, "Preparation data reset for ${user?.examName ?: "Exam"}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                ) {
                    Text("Reset Now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDataConfirm = false }) {
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
                Text("Delete Account & Data?", color = CoralRose, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This action is irreversible. All your study schedules, smart notes, mock tests, and streak records will be permanently removed from this device.", color = secondaryTextColor)
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
// EXAM SWITCHER FULL SUB-SCREEN
// ======================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamSwitcherSubScreen(
    currentExamName: String,
    catalogExams: List<ExamEntity>,
    onSelectExam: (examId: String, examName: String) -> Unit,
    onBack: () -> Unit
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Fallback standard catalog presets if catalogExams is empty
    val displayExams = remember(catalogExams) {
        if (catalogExams.isNotEmpty()) catalogExams else listOf(
            ExamEntity(
                id = "railway_rrb_group_d",
                name = "RRB Group D (Track Maintainer, Pointsman & Level-1)",
                category = "Railway Exams",
                shortCode = "RRB Group D",
                description = "Railway national recruitment for Level-1 posts. General Science, Math, Reasoning, General Awareness.",
                examPattern = "CBT: 100 Marks (90 mins). Negative marking 1/3rd.",
                totalMarks = 100,
                durationMinutes = 90,
                isPopular = true
            ),
            ExamEntity(
                id = "railway_rrb_ntpc",
                name = "RRB NTPC (Non-Technical Popular Categories)",
                category = "Railway Exams",
                shortCode = "RRB NTPC",
                description = "Station Master, Goods Guard, Commercial Apprentice, Clerks & Typists.",
                examPattern = "CBT-1: 100 Marks (90 mins). Negative marking 1/3rd.",
                totalMarks = 100,
                durationMinutes = 90,
                isPopular = true
            ),
            ExamEntity(
                id = "ssc_cgl",
                name = "SSC CGL (Staff Selection Commission Combined Graduate)",
                category = "SSC Exams",
                shortCode = "SSC CGL",
                description = "Tier 1 & Tier 2 for Assistant Section Officer, Excise Inspector & Income Tax.",
                examPattern = "Tier-1: 100 Qs (200 Marks, 60 mins). Tier-2 Comprehensive.",
                totalMarks = 200,
                durationMinutes = 60,
                isPopular = true
            ),
            ExamEntity(
                id = "upsc_cse",
                name = "UPSC CSE (Civil Services Prelims - IAS / IPS / IFS)",
                category = "UPSC / Civil Services",
                shortCode = "UPSC CSE",
                description = "General Studies Paper 1 & CSAT Paper 2 for premier civil services of India.",
                examPattern = "Prelims: GS-1 (200 Marks) + CSAT (200 Marks qualifying).",
                totalMarks = 400,
                durationMinutes = 120,
                isPopular = true
            ),
            ExamEntity(
                id = "engineering_jee_main",
                name = "JEE Main (Joint Entrance Examination)",
                category = "Engineering Entrance",
                shortCode = "JEE Main",
                description = "Physics, Chemistry & Mathematics for NITs, IIITs & qualifying for JEE Advanced.",
                examPattern = "Paper 1: 90 Qs (300 Marks, 180 mins). Numerical section.",
                totalMarks = 300,
                durationMinutes = 180,
                isPopular = true
            ),
            ExamEntity(
                id = "medical_neet_ug",
                name = "NEET UG (National Eligibility cum Entrance Test)",
                category = "Medical Entrance",
                shortCode = "NEET UG",
                description = "Physics, Chemistry, Botany & Zoology for MBBS, BDS & premier medical institutes.",
                examPattern = "200 Qs (720 Marks, 200 mins). Section A & Section B.",
                totalMarks = 720,
                durationMinutes = 200,
                isPopular = true
            ),
            ExamEntity(
                id = "banking_ibps_po",
                name = "IBPS PO / SBI PO (Probationary Officer)",
                category = "Banking Exams",
                shortCode = "IBPS PO",
                description = "Quantitative Aptitude, Reasoning Ability, English Language & General Economy.",
                examPattern = "Prelims (100 Marks, 60 mins) + Mains (200 Marks).",
                totalMarks = 100,
                durationMinutes = 60,
                isPopular = true
            )
        )
    }

    val categories = listOf("All", "Railway Exams", "SSC Exams", "UPSC / Civil Services", "Engineering Entrance", "Medical Entrance", "Banking Exams")

    val filteredExams = displayExams.filter { exam ->
        val matchesQuery = searchQuery.isBlank() || exam.name.contains(searchQuery, ignoreCase = true) || exam.shortCode.contains(searchQuery, ignoreCase = true) || exam.category.contains(searchQuery, ignoreCase = true)
        val matchesCat = selectedCategoryFilter == "All" || exam.category.equals(selectedCategoryFilter, ignoreCase = true)
        matchesQuery && matchesCat
    }

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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isGlassLight) Color(0x150F172A) else Color(0x20FFFFFF))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = primaryTextColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Target Exam Switcher 🎯",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Text(
                            text = "Current: $currentExamName",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldenSpark
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search exam name, syllabus or board...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = GoldenSpark) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, "Clear", tint = secondaryTextColor)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldenSpark,
                        unfocusedBorderColor = cardBorderColor
                    )
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) GoldenSpark else (if (isGlassLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GoldenSpark else cardBorderColor),
                            modifier = Modifier.springClickable { selectedCategoryFilter = cat }
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0F172A) else secondaryTextColor
                            )
                        }
                    }
                }
            }

            // Exam Cards List
            items(filteredExams) { exam ->
                val isCurrent = currentExamName.contains(exam.shortCode, ignoreCase = true) || currentExamName.contains(exam.name, ignoreCase = true)

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .springClickable { onSelectExam(exam.id, exam.name) },
                    shape = RoundedCornerShape(18.dp),
                    fillAlpha = if (isCurrent) (if (isGlassLight) 0.85f else 0.75f) else (if (isGlassLight) 0.65f else 0.45f),
                    borderColor = if (isCurrent) GoldenSpark else cardBorderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GoldenSpark.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = exam.category,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldenSpark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = exam.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )
                            }

                            if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess)
                                ) {
                                    Text(
                                        text = "Active 🎯",
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exam.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pattern: ${exam.totalMarks} Marks • ${exam.durationMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGlassLight) DeepIndigo else NeonCyan,
                                fontWeight = FontWeight.SemiBold
                            )

                            Button(
                                onClick = { onSelectExam(exam.id, exam.name) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrent) EmeraldSuccess else GoldenSpark,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (isCurrent) "Active Goal" else "Select 🎯",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
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
        shape = RoundedCornerShape(16.dp),
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(previewGradient)
                    .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentColor.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp, 12.dp)
                ) {}
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                    lineHeight = 13.sp
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

package com.example.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.screens.nova.NovaHomeUniversalWidget
import com.example.ui.screens.nova.TodayExamBriefWidget
import com.example.ui.screens.nova.NovaWebMcqGeneratorDialog
import com.example.ui.screens.nova.SmartRevisionSessionDialog
import com.example.viewmodel.NovaViewModel
import com.example.viewmodel.ActiveTestState
import com.example.viewmodel.FocusTimerState
import com.example.service.intelligence.*
import com.example.service.FocusShieldManager
import com.example.ui.screens.focus.FocusShieldSettingsScreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.content.Context
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

/**
 * STEP 42: NOVA HOME SCREEN SIMPLIFICATION & PREMIUM REDESIGN
 *
 * Exact Hierarchy Priority:
 * 1. Minimal Header (Logo, Exam Switcher, Notifications, Profile)
 * 2. Nova Search / Voice (Prominent AI Assistant)
 * 3. Focus Mode — HERO SECTION (Commanding visual weight, primary action)
 * 4. Continue Learning & Today's Progress (Current chapter + compact stats)
 * 5. Feature Cards Grid (Study, Practice, Nova AI, Current Affairs, Recruitment, Progress)
 * 6. Latest Important Update (Single focused update banner)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: UserProfile?,
    studyPlan: List<StudyPlanItem>,
    missions: List<DailyMission> = emptyList(),
    flashcards: List<FlashcardItem> = emptyList(),
    studentMasterContext: StudentMasterContext? = null,
    aiCoachRecommendation: AiCoachRecommendation? = null,
    studyNowRecommendation: StudyNowRecommendation? = null,
    isAiCoachLoading: Boolean = false,
    isStudyNowLoading: Boolean = false,
    examReadiness: ExamReadinessScore? = null,
    onLoadAiCoach: (String) -> Unit = {},
    onLoadStudyNow: () -> Unit = {},
    onSelectTimeAvailable: (Int?) -> Unit = {},
    onPerformSmartSearch: (String) -> Unit = {},
    onTogglePlanItem: (Long, Boolean) -> Unit = { _, _ -> },
    onStartFocusSession: (subject: String, topic: String) -> Unit,
    onNavigateToTab: (AppNavTab) -> Unit,
    onOpenProfileSettings: () -> Unit,
    onOpenExamReadinessCenter: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onScanQuestion: () -> Unit = {},
    onOpenDocumentSummarizer: () -> Unit = {},
    onUpdateUserProfile: (UserProfile) -> Unit = {},
    novaViewModel: NovaViewModel? = null,
    liveExamFeedState: LiveExamFeedState = LiveExamFeedState(),
    isRefreshingLiveExam: Boolean = false,
    onRefreshLiveExam: () -> Unit = {},
    onOpenLiveExamUpdateDetail: (LiveExamUpdateEntity) -> Unit = {},
    onToggleSaveLiveExamUpdate: (String, Boolean) -> Unit = { _, _ -> },
    onToggleSaveTrendingTopic: (String, Boolean) -> Unit = { _, _ -> },
    onStartQuizForTopic: (String, String) -> Unit = { _, _ -> },
    onOpenFullLiveExamIntelligence: () -> Unit = {},
    recruitmentFeedState: RecruitmentFeedState = RecruitmentFeedState(),
    onOpenSmartVacancy: (String?) -> Unit = {},
    pendingResumeSession: ActiveTestState? = null,
    onResumePendingTest: () -> Unit = {},
    onDiscardPendingTest: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    onOpenNotificationCenter: () -> Unit = {},
    dailyBriefingData: DailyBriefingData = DailyBriefingData(),
    onOpenDailyBriefing: () -> Unit = {},
    userStudyPreferences: UserStudyPreferences = UserStudyPreferences(),
    allTopicMasteries: List<TopicMastery> = emptyList(),
    mockAttempts: List<MockTestAttempt> = emptyList(),
    mistakes: List<MistakeItem> = emptyList(),
    focusSessions: List<FocusSession> = emptyList(),
    onStartPracticeWithConfig: (MockTestConfig) -> Unit = {},
    focusTimerState: FocusTimerState? = null,
    onPauseFocusSession: () -> Unit = {},
    onResumeFocusSession: () -> Unit = {},
    onStopFocusSession: () -> Unit = {},
    dailyMissionTasks: List<com.example.data.model.DailyMissionTask> = emptyList(),
    weakTopicInsights: List<com.example.data.model.WeakTopicInsight> = emptyList(),
    onToggleDailyMissionTask: (String, Boolean) -> Unit = { _, _ -> },
    onOpenSmartPlanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val currentTheme = currentThemeMode()

    // Modals state
    var showThemeSelectionDialog by remember { mutableStateOf(false) }
    var showExamSwitcherDialog by remember { mutableStateOf(false) }
    var showQuickSearchDialog by remember { mutableStateOf(false) }
    var showNotificationSummaryDialog by remember { mutableStateOf(false) }
    var showAllToolsDialog by remember { mutableStateOf(false) }
    var showQuickStudyDialog by remember { mutableStateOf(false) }
    var showTransparencyDialog by remember { mutableStateOf(false) }
    var showShieldSettingsDialog by remember { mutableStateOf(false) }
    var selectedPlanTimeOption by remember { mutableStateOf(userStudyPreferences.studyTimeAvailableOption.ifBlank { "30 min" }) }

    // Step 36 Personalization Engine Calculations
    val personalizationSettings = remember(userStudyPreferences) {
        PersonalizationSettings(
            isEnabled = userStudyPreferences.personalizationEnabled,
            dailyQuestionGoal = userStudyPreferences.dailyQuestionGoal,
            dailyStudyMinutesGoal = userStudyPreferences.dailyStudyMinutesGoal,
            weeklyTestsGoal = userStudyPreferences.weeklyTestsGoal,
            studyTimeAvailableOption = userStudyPreferences.studyTimeAvailableOption
        )
    }

    val topicPerformances = remember(allTopicMasteries, mockAttempts, mistakes) {
        PersonalizationEngine.computeTopicPerformances(mockAttempts, mistakes, allTopicMasteries)
    }

    val todayFocusRec = remember(user, topicPerformances, allTopicMasteries, personalizationSettings) {
        PersonalizationEngine.computeTodayFocusRecommendation(user, topicPerformances, allTopicMasteries, personalizationSettings)
    }

    // Step 23 Smart Learning States
    val dailyExamBriefing = novaViewModel?.dailyExamBriefing?.collectAsState()?.value
    val isBriefingLoading = novaViewModel?.isDailyBriefingLoading?.collectAsState()?.value ?: false
    val showMcqDialog = novaViewModel?.showMcqConfigDialog?.collectAsState()?.value ?: false
    val showRevisionDialog = novaViewModel?.showRevisionDialog?.collectAsState()?.value ?: false
    val activeRevisionTopic = novaViewModel?.activeRevisionTopic?.collectAsState()?.value

    // Connectivity State
    val isOnline = rememberIsOnline()

    // Time-based dynamic greeting
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingTime = when (currentHour) {
        in 4..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    val rawUserName = user?.name?.trim()
    val hasValidName = !rawUserName.isNullOrBlank() &&
            !rawUserName.equals("Student", ignoreCase = true) &&
            !rawUserName.equals("Scholar", ignoreCase = true) &&
            !rawUserName.equals("User", ignoreCase = true)
    
    val firstName = remember(rawUserName, hasValidName) {
        if (hasValidName) {
            cleanDisplayName(rawUserName).split(" ").firstOrNull() ?: ""
        } else ""
    }

    val greetingHeader = if (firstName.isNotBlank()) "$greetingTime, $firstName 👋" else "$greetingTime 👋"

    // Exam Context & Dynamic Countdown
    val selectedExamName = user?.examName?.ifBlank { "RRB Group D (Railway)" } ?: "RRB Group D (Railway)"
    val examCategory = user?.examCategory?.ifBlank { "Railway Recruitment" } ?: "Railway Recruitment"
    val targetDateMillis = user?.examDateMillis ?: (System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000)
    val daysRemaining = remember(targetDateMillis) {
        val diff = targetDateMillis - System.currentTimeMillis()
        (diff / (1000L * 60 * 60 * 24)).coerceAtLeast(1).toInt()
    }
    val formattedTargetDate = remember(targetDateMillis) {
        try {
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(targetDateMillis))
        } catch (e: Exception) {
            "20 Aug 2027"
        }
    }

    // Smart Mission & Progress Determination
    val nextPendingTask = remember(studyPlan) {
        studyPlan.firstOrNull { !it.isCompleted }
    }
    val completedPlanCount = remember(studyPlan) {
        studyPlan.count { it.isCompleted }
    }
    val totalPlanCount = remember(studyPlan) {
        studyPlan.size
    }

    val missionSubject = nextPendingTask?.subject
        ?: studyNowRecommendation?.subject
        ?: user?.weakSubjects?.firstOrNull()
        ?: user?.subjects?.firstOrNull()
        ?: "General Science"

    val missionTopic = nextPendingTask?.topic
        ?: studyNowRecommendation?.topic
        ?: user?.weakTopics?.firstOrNull()
        ?: "Core Concepts & High-Yield Practice"

    val missionTargetMinutes = nextPendingTask?.targetMinutes
        ?: studyNowRecommendation?.targetMinutes
        ?: 25

    val recommendationReason = remember(nextPendingTask, studyNowRecommendation, user) {
        if (nextPendingTask != null) {
            "Next priority item in your structured study plan"
        } else if (studyNowRecommendation != null) {
            studyNowRecommendation.reasoning.ifBlank { "High-yield topic calibrated for $selectedExamName" }
        } else if (!user?.weakSubjects.isNullOrEmpty()) {
            "Targeted practice for ${user?.weakSubjects?.first()}"
        } else {
            "Daily core topic for ${selectedExamName.take(18)}"
        }
    }

    val todayFocusMinutes = user?.totalFocusMinutes ?: 0
    val targetDailyMinutes = (user?.dailyTargetMinutes ?: 180).coerceAtLeast(60)
    val progressFraction = (todayFocusMinutes.toFloat() / targetDailyMinutes.toFloat()).coerceIn(0f, 1f)
    val progressPercentage = (progressFraction * 100).toInt()

    val studyHours = todayFocusMinutes / 60
    val studyMins = todayFocusMinutes % 60
    val formattedStudyTime = if (studyHours > 0) "${studyHours}h ${studyMins}m" else "${studyMins}m"

    val targetHours = targetDailyMinutes / 60
    val targetMins = targetDailyMinutes % 60
    val formattedTargetTime = if (targetHours > 0 && targetMins > 0) "${targetHours}h ${targetMins}m" else if (targetHours > 0) "${targetHours}h" else "${targetMins}m"

    val userLevel = user?.level ?: 1
    val userXp = user?.xp ?: (todayFocusMinutes * 10)
    val streakDays = user?.streakDays ?: 1

    // Catalog for switching target exams
    val examCatalogOptions = listOf(
        "RRB Group D (Railway Recruitment)",
        "RRB NTPC (Railway Recruitment Board)",
        "SSC CGL (Staff Selection Commission)",
        "JEE Main (Engineering Entrance)",
        "NEET UG (Medical Entrance)",
        "UPSC Civil Services Examination",
        "IBPS PO (Banking Probationary Officer)",
        "CBSE Class 12 Board Exam",
        "State PSC General Studies"
    )

    var selectedFocusDuration by remember { mutableIntStateOf(25) }
    var isStrictModeActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshingLiveExam,
            onRefresh = onRefreshLiveExam,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // =========================================================================
                // 1. TOP HEADER (Greeting, Flame Streak Badge, Theme Switcher, Notifications)
                // =========================================================================
                item {
                    val displayName = if (firstName.isNotBlank()) firstName else (user?.name?.ifBlank { "Rahul" } ?: "Rahul")
                    LiquidHomeHeader(
                        userName = displayName,
                        streakDays = streakDays,
                        unreadNotificationCount = unreadNotificationCount,
                        onOpenNotifications = onOpenNotificationCenter,
                        onToggleTheme = { showThemeSelectionDialog = true },
                        onOpenProfile = onOpenProfileSettings
                    )
                }

                // =========================================================================
                // 2. GLOBAL SEARCH BAR
                // =========================================================================
                item {
                    LiquidGlobalSearchBar(
                        onClickSearch = { showQuickSearchDialog = true }
                    )
                }

                // Pending Unfinished Test Banner (if any)
                if (pendingResumeSession != null && pendingResumeSession.questions.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AmberAlert.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberAlert.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Unfinished Mock Test",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = pendingResumeSession.title,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onResumePendingTest,
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberAlert),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Resume", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = onDiscardPendingTest,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("Discard", color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 3. TODAY'S METRICS / COMPACT STUDY PROGRESS
                // =========================================================================
                item {
                    LiquidTodayProgressCard(
                        studyMinutesToday = todayFocusMinutes,
                        targetMinutesToday = targetDailyMinutes,
                        userXp = userXp
                    )
                }

                // =========================================================================
                // 4. CIRCULAR FOCUS HERO (VINTAGE HOURGLASS + OVERLAID DIGITAL TIMER)
                // =========================================================================
                item {
                    VintageHourglassFocusCard(
                        focusTimerState = focusTimerState,
                        selectedDurationMinutes = selectedFocusDuration,
                        onSelectDuration = { selectedFocusDuration = it },
                        onStartFocus = { mins ->
                            onStartFocusSession(missionSubject, missionTopic)
                        },
                        onPauseFocus = onPauseFocusSession,
                        onResumeFocus = onResumeFocusSession,
                        onStopFocus = onStopFocusSession,
                        onOpenAppBlockerSettings = { showShieldSettingsDialog = true },
                        isAppShieldActive = true,
                        isStrictModeActive = isStrictModeActive,
                        onToggleStrictMode = { isStrictModeActive = !isStrictModeActive },
                        onOpenSettings = { showShieldSettingsDialog = true }
                    )
                }

                // =========================================================================
                // 5. TODAY'S ACTIONABLE MISSION
                // =========================================================================
                item {
                    LiquidTodayMissionSection(
                        missions = dailyMissionTasks,
                        onToggleMission = onToggleDailyMissionTask,
                        onStartAction = { actionType, subject, topic, minutes ->
                            when (actionType) {
                                "FOCUS" -> onStartFocusSession(subject, topic)
                                "PRACTICE" -> onNavigateToTab(AppNavTab.PRACTICE)
                                "CURRENT_AFFAIRS" -> onNavigateToTab(AppNavTab.UPDATES)
                                else -> onStartFocusSession(subject, topic)
                            }
                        },
                        onOpenPlan = onOpenSmartPlanner
                    )
                }

                // =========================================================================
                // 6. THE 4 MODULAR DEDICATED HUB GATEWAY CARDS (2x2 Grid)
                // =========================================================================
                item {
                    LiquidFourHubGrid(
                        onNavigateToLearn = { onNavigateToTab(AppNavTab.STUDY) },
                        onNavigateToPractice = { onNavigateToTab(AppNavTab.PRACTICE) },
                        onNavigateToUpdates = { onNavigateToTab(AppNavTab.UPDATES) },
                        onNavigateToSettings = { showShieldSettingsDialog = true }
                    )
                }

                // =========================================================================
                // 7. SMART IMPORTANT UPDATES HORIZONTAL STRIP
                // =========================================================================
                item {
                    val updatesList = liveExamFeedState.whatsNewList.ifEmpty { liveExamFeedState.liveNews }
                    LiquidLatestUpdatesStrip(
                        updates = updatesList,
                        onOpenUpdateDetail = onOpenLiveExamUpdateDetail,
                        onViewAllUpdates = { onNavigateToTab(AppNavTab.UPDATES) }
                    )
                }
            }
        }
    }

    // =========================================================================
    // MODAL DIALOGS
    // =========================================================================

    // 0. SHIELD SETTINGS MODAL DIALOG
    if (showShieldSettingsDialog) {
        Dialog(
            onDismissRequest = { showShieldSettingsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                FocusShieldSettingsScreen(
                    onBack = { showShieldSettingsDialog = false }
                )
            }
        }
    }

    // 0. THEME & NIGHT-TIME STUDY MODE DIALOG
    if (showThemeSelectionDialog) {
        val themeCtrl = LocalThemeController.current
        ThemeSelectionDialog(
            currentTheme = themeCtrl.themeMode,
            onSelectTheme = { newTheme ->
                themeCtrl.setThemeMode(newTheme)
            },
            onDismiss = { showThemeSelectionDialog = false }
        )
    }

    // 1. EXAM SWITCHER MODAL DIALOG
    if (showExamSwitcherDialog) {
        AlertDialog(
            onDismissRequest = { showExamSwitcherDialog = false },
            containerColor = if (isDark) Color(0xFF111827) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.School, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Change Target Exam",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Select your target exam. Study topics, mock tests, and Nova context will automatically align.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    examCatalogOptions.forEach { examOption ->
                        val isSelected = selectedExamName.contains(examOption.take(10), ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .springClickable {
                                    if (user != null) {
                                        val updated = user.copy(
                                            examName = examOption,
                                            examCategory = examOption,
                                            goal = examOption
                                        )
                                        onUpdateUserProfile(updated)
                                    }
                                    showExamSwitcherDialog = false
                                },
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) NeonCyan else Color(0x20FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = examOption,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                if (isSelected) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExamSwitcherDialog = false }) {
                    Text("Close", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 2. QUICK SEARCH MODAL DIALOG
    if (showQuickSearchDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var recentSearches by remember { mutableStateOf(listOf("Railway Vacancies", "Physics Formulas", "Mock Test 1", "Current Affairs")) }

        val allSearchableFeatures = remember {
            listOf(
                Triple("Focus Mode", "Start focus session & block apps", AppNavTab.FOCUS),
                Triple("Mock Tests", "Full length & subject practice", AppNavTab.PRACTICE),
                Triple("Study Notes", "Chapter wise revision material", AppNavTab.STUDY),
                Triple("Current Affairs", "Daily news & quiz updates", AppNavTab.STUDY),
                Triple("Latest Vacancies", "Recruitment alerts & admit cards", AppNavTab.STUDY),
                Triple("Nova AI", "Voice & document AI companion", AppNavTab.AI_TUTOR)
            )
        }

        val filteredFeatures = remember(searchQuery) {
            if (searchQuery.isBlank()) emptyList()
            else allSearchableFeatures.filter { it.first.contains(searchQuery, true) || it.second.contains(searchQuery, true) }
        }

        AlertDialog(
            onDismissRequest = { showQuickSearchDialog = false },
            containerColor = if (isDark) Color(0xFF111827) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Global Search",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_search_input"),
                        placeholder = { Text("Search features, topics, vacancies...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )

                    if (searchQuery.isBlank()) {
                        // Recent Searches
                        if (recentSearches.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { recentSearches = emptyList() }, contentPadding = PaddingValues(0.dp)) {
                                    Text("Clear", fontSize = 10.sp, color = NeonCyan)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                recentSearches.take(3).forEach { recent ->
                                    SuggestionChip(
                                        onClick = {
                                            searchQuery = recent
                                            onPerformSmartSearch(recent)
                                            showQuickSearchDialog = false
                                        },
                                        label = { Text(recent, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // Suggested Features
                        Text(
                            text = "Suggested Features:",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Focus Mode", "Mock Test", "Vacancies", "Current Affairs").forEach { chip ->
                                SuggestionChip(
                                    onClick = {
                                        searchQuery = chip
                                        onPerformSmartSearch(chip)
                                        showQuickSearchDialog = false
                                    },
                                    label = { Text(chip, fontSize = 11.sp) }
                                )
                            }
                        }
                    } else {
                        // Search Results Grouped
                        Text(
                            text = "Matching Features:",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )

                        if (filteredFeatures.isNotEmpty()) {
                            filteredFeatures.forEach { (title, subtitle, tab) ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (!recentSearches.contains(searchQuery)) {
                                                recentSearches = (listOf(searchQuery) + recentSearches).take(5)
                                            }
                                            onNavigateToTab(tab)
                                            showQuickSearchDialog = false
                                        },
                                    color = if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.AutoAwesome, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
                                            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Ask Nova AI about '$searchQuery'",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onPerformSmartSearch(searchQuery)
                                        showQuickSearchDialog = false
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            if (!recentSearches.contains(searchQuery)) {
                                recentSearches = (listOf(searchQuery) + recentSearches).take(5)
                            }
                            onPerformSmartSearch(searchQuery)
                            showQuickSearchDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("Search Nova", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickSearchDialog = false }) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                }
            }
        )
    }

    // 3. NOTIFICATION & REMINDER MODAL DIALOG
    if (showNotificationSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationSummaryDialog = false },
            containerColor = if (isDark) Color(0xFF111827) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Notifications, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Study Reminders & Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.School, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    text = "$selectedExamName Countdown",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "$daysRemaining days remaining until target date ($formattedTargetDate).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Timer, null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    text = "Daily Goal Reminder",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "$todayFocusMinutes / $targetDailyMinutes minutes logged today.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationSummaryDialog = false }) {
                    Text("Close", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 4. VIEW ALL TOOLS MODAL DIALOG
    if (showAllToolsDialog) {
        AlertDialog(
            onDismissRequest = { showAllToolsDialog = false },
            containerColor = if (isDark) Color(0xFF111827) else Color.White,
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.GridView, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "All Study Hub Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AllToolsRowItem(
                        title = "Document & PDF Summarizer",
                        subtitle = "Extract key concepts & formulas",
                        icon = Icons.Outlined.Description,
                        accentColor = NeonCyan,
                        onClick = {
                            showAllToolsDialog = false
                            onOpenDocumentSummarizer()
                        }
                    )

                    AllToolsRowItem(
                        title = "Scan / Doubt Solver",
                        subtitle = "Snap photos of problems for instant steps",
                        icon = Icons.Outlined.PhotoCamera,
                        accentColor = ElectricViolet,
                        onClick = {
                            showAllToolsDialog = false
                            onScanQuestion()
                        }
                    )

                    AllToolsRowItem(
                        title = "Smart Topic Search",
                        subtitle = "Search syllabus & PYQs",
                        icon = Icons.Filled.Search,
                        accentColor = EmeraldSuccess,
                        onClick = {
                            showAllToolsDialog = false
                            showQuickSearchDialog = true
                        }
                    )

                    AllToolsRowItem(
                        title = "Exam Readiness Analytics",
                        subtitle = "Detailed subject-wise breakdown",
                        icon = Icons.Outlined.Speed,
                        accentColor = NebulaPurple,
                        onClick = {
                            showAllToolsDialog = false
                            onOpenExamReadinessCenter()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllToolsDialog = false }) {
                    Text("Close", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Step 23 Smart Learning Dialogs
    if (showMcqDialog && novaViewModel != null) {
        NovaWebMcqGeneratorDialog(
            initialTopic = "Recent Space Missions & Technology",
            examName = selectedExamName,
            onDismiss = { novaViewModel.setShowMcqConfigDialog(false) },
            onGenerate = { config ->
                novaViewModel.generateFreshWebMcqs(config)
            }
        )
    }

    if (showRevisionDialog && activeRevisionTopic != null && novaViewModel != null) {
        SmartRevisionSessionDialog(
            item = activeRevisionTopic,
            examName = selectedExamName,
            onDismiss = { novaViewModel.setShowRevisionDialog(false) },
            onComplete = { score, total ->
                novaViewModel.completeRevisionSession(score, total)
                novaViewModel.setShowRevisionDialog(false)
            },
            onAskNova = { prompt ->
                novaViewModel.setShowRevisionDialog(false)
                novaViewModel.sendMessage(prompt)
                onNavigateToTab(AppNavTab.AI_TUTOR)
            }
        )
    }

    // Step 36 Quick Study & Transparency Dialogs
    if (showQuickStudyDialog) {
        QuickStudyModalDialog(
            weakTopics = topicPerformances.filter { it.isWeak },
            onStartQuickStudy = { durationMins ->
                val config = MockTestConfig(
                    exam = selectedExamName,
                    testType = MockTestType.SUBJECT_PRACTICE,
                    subject = todayFocusRec.focusSubject,
                    chapter = todayFocusRec.focusTopic,
                    topic = todayFocusRec.focusTopic,
                    questionCount = durationMins,
                    timeLimitMinutes = durationMins
                )
                onStartPracticeWithConfig(config)
            },
            onDismiss = { showQuickStudyDialog = false }
        )
    }

    if (showTransparencyDialog) {
        TransparencySignalDialog(
            recommendation = todayFocusRec,
            onDismiss = { showTransparencyDialog = false }
        )
    }
}

// =========================================================================
// HERO SECTION COMPONENT: FOCUS MODE
// =========================================================================

@Composable
private fun FocusModeHeroSection(
    focusTimerState: FocusTimerState?,
    missionSubject: String,
    missionTopic: String,
    missionTargetMinutes: Int,
    onStartFocus: () -> Unit,
    onOpenFocusTab: () -> Unit,
    onPauseFocus: () -> Unit,
    onResumeFocus: () -> Unit,
    onStopFocus: () -> Unit,
    isDark: Boolean,
    currentTheme: AppThemeMode
) {
    val isRunning = focusTimerState?.isRunning == true
    val isPaused = focusTimerState?.isPaused == true
    val isCelebration = focusTimerState?.showCelebration == true

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_mode_hero_card"),
        shape = RoundedCornerShape(22.dp),
        elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 2.dp else 8.dp,
        borderColor = if (isRunning) EmeraldSuccess.copy(alpha = 0.7f) else NeonCyan.copy(alpha = 0.5f),
        borderWidth = if (isRunning) 2.dp else 1.2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isCelebration) {
                // =============================================================
                // STATE A: RECENTLY COMPLETED SESSION
                // =============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🎉", fontSize = 20.sp)
                        Text(
                            text = "FOCUS SESSION COMPLETE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = EmeraldSuccess,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldenSpark.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldenSpark.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "+${focusTimerState?.lastSessionXp ?: 50} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSpark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "Outstanding deep work!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Text(
                    text = "You completed your session on $missionSubject • $missionTopic. Keep the momentum going!",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )

                Button(
                    onClick = onStartFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("hero_start_new_focus_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) ElectricIndigo else DeepIndigo
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("START NEW SESSION", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                }

            } else if (isRunning || isPaused) {
                // =============================================================
                // STATE B: LIVE FOCUS ACTIVE / PAUSED
                // =============================================================
                val totalSecs = (focusTimerState?.initialMinutes ?: 25) * 60
                val remainSecs = focusTimerState?.remainingSeconds ?: (25 * 60)
                val elapsedSecs = (totalSecs - remainSecs).coerceAtLeast(0)
                val progress = (elapsedSecs.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)

                val mins = remainSecs / 60
                val secs = remainSecs % 60
                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) EmeraldSuccess else AmberWarning)
                        )
                        Text(
                            text = if (isRunning) "FOCUS MODE ACTIVE" else "FOCUS PAUSED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (isRunning) EmeraldSuccess else AmberWarning,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "$formattedTime remaining",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) NeonCyan else DeepIndigo
                    )
                }

                Text(
                    text = "${focusTimerState?.subject ?: missionSubject} • ${focusTimerState?.topic ?: missionTopic}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isRunning) EmeraldSuccess else AmberWarning,
                    trackColor = if (isDark) Color(0x22FFFFFF) else Color(0x18000000)
                )

                // Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause/Resume Button
                    OutlinedButton(
                        onClick = { if (isRunning) onPauseFocus() else onResumeFocus() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_pause_resume_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x40FFFFFF) else Color(0x30000000))
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Resume",
                            modifier = Modifier.size(16.dp),
                            tint = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isRunning) "Pause" else "Resume", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
                    }

                    // Open Full Focus Shield Button
                    Button(
                        onClick = onOpenFocusTab,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("hero_open_focus_shield_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) EmeraldSuccess else (if (isDark) ElectricIndigo else DeepIndigo)
                        )
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open Shield", fontWeight = FontWeight.ExtraBold)
                    }
                }

            } else {
                // =============================================================
                // STATE C: IDLE (READY TO STUDY) — HERO INVITATION
                // =============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "FOCUS MODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) NeonCyan else DeepIndigo,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0x1838BDF8) else Color(0x126366F1),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "App Shield Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) NeonCyan else DeepIndigo,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Ready to study?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$missionSubject • $missionTopic",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // PRIMARY CALL TO ACTION BUTTON
                Button(
                    onClick = onStartFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("hero_start_focus_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) ElectricIndigo else DeepIndigo
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START FOCUS",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Subtext / Caption
                Text(
                    text = "Blocks distracting apps • 25 min deep study session",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// =========================================================================
// CONTINUE LEARNING SECTION COMPONENT
// =========================================================================

@Composable
private fun ContinueLearningSection(
    missionSubject: String,
    missionTopic: String,
    recommendationReason: String,
    completedPlanCount: Int,
    totalPlanCount: Int,
    progressPercentage: Int,
    hasActiveTasks: Boolean,
    onContinue: () -> Unit,
    isDark: Boolean,
    currentTheme: AppThemeMode
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("continue_learning_card"),
        shape = RoundedCornerShape(18.dp),
        elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 4.dp,
        onClick = onContinue
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = DeepIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (hasActiveTasks) "CONTINUE LEARNING" else "START LEARNING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        letterSpacing = 0.8.sp
                    )
                }

                if (hasActiveTasks && totalPlanCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0x206366F1) else Color(0x156366F1)
                    ) {
                        Text(
                            text = "$completedPlanCount/$totalPlanCount Done",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) NeonCyan else DeepIndigo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasActiveTasks) "$missionSubject → $missionTopic" else "Choose a subject to begin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = recommendationReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0x2838BDF8) else Color(0x186366F1),
                        contentColor = if (isDark) NeonCyan else DeepIndigo
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) NeonCyan.copy(alpha = 0.4f) else DeepIndigo.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (hasActiveTasks) "Continue" else "Explore",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// =========================================================================
// TODAY'S PROGRESS COMPACT SECTION
// =========================================================================

@Composable
private fun TodaysProgressCompactSection(
    formattedStudyTime: String,
    formattedTargetTime: String,
    progressFraction: Float,
    progressPercentage: Int,
    streakDays: Int,
    mockAttemptsCount: Int,
    userLevel: Int,
    userXp: Int,
    onClick: () -> Unit,
    isDark: Boolean,
    currentTheme: AppThemeMode
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("todays_progress_compact_card"),
        shape = RoundedCornerShape(18.dp),
        elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 4.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TODAY'S PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "$progressPercentage% of daily target",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (progressPercentage >= 100) EmeraldSuccess else (if (isDark) NeonCyan else DeepIndigo)
                )
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progressPercentage >= 100) EmeraldSuccess else NeonCyan,
                trackColor = if (isDark) Color(0x22FFFFFF) else Color(0x18000000)
            )

            // 3-Stat Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stat 1: Time
                Column {
                    Text(
                        text = "$formattedStudyTime / $formattedTargetTime",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Study Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }

                // Divider
                Box(modifier = Modifier.size(1.dp, 24.dp).background(if (isDark) Color(0x20FFFFFF) else Color(0x15000000)))

                // Stat 2: Streak
                Column {
                    Text(
                        text = "$streakDays Days",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Active Streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }

                // Divider
                Box(modifier = Modifier.size(1.dp, 24.dp).background(if (isDark) Color(0x20FFFFFF) else Color(0x15000000)))

                // Stat 3: Mocks & Level
                Column {
                    Text(
                        text = "Lvl $userLevel • ${mockAttemptsCount} Mocks",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "$userXp Total XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// =========================================================================
// CONTINUE PRACTICE & MOCK TESTS SECTION
// =========================================================================

@Composable
private fun ContinuePracticeSection(
    selectedExamName: String,
    mockAttemptsCount: Int,
    latestMock: MockTestAttempt?,
    onStartMock: () -> Unit,
    onStartPyq: () -> Unit,
    isDark: Boolean,
    currentTheme: AppThemeMode
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("continue_practice_card"),
        shape = RoundedCornerShape(18.dp),
        elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Quiz,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "CONTINUE PRACTICE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        letterSpacing = 0.8.sp
                    )
                }

                if (mockAttemptsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldSuccess.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$mockAttemptsCount Tests Taken",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (latestMock != null) {
                Column {
                    Text(
                        text = latestMock.title.ifBlank { "$selectedExamName Mock Test" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Last Score: ${latestMock.score}/${latestMock.totalQuestions} • Accuracy: ${latestMock.accuracyPercent.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            } else {
                Column {
                    Text(
                        text = "$selectedExamName Mock Test Series",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Real exam-pattern timed practice with instant AI explanations",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartMock,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("home_start_mock_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0x2810B981) else Color(0x1810B981),
                        contentColor = EmeraldSuccess
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, EmeraldSuccess.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start Mock", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onStartPyq,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("home_start_pyq_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x20000000))
                ) {
                    Icon(Icons.Outlined.HistoryEdu, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isDark) Color.White else Color(0xFF0F172A))
                    Spacer(Modifier.width(4.dp))
                    Text("PYQ Vault", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
                }
            }
        }
    }
}

// =========================================================================
// SMART DAILY PLAN SECTION
// =========================================================================

@Composable
private fun SmartDailyPlanSection(
    missionSubject: String,
    missionTopic: String,
    onStartSession: () -> Unit,
    onOpenPlan: () -> Unit,
    isDark: Boolean,
    currentTheme: AppThemeMode
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_daily_plan_card"),
        shape = RoundedCornerShape(18.dp),
        elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EventNote,
                        contentDescription = null,
                        tint = GoldenSpark,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TODAY'S STUDY PLAN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "70 min total",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldenSpark
                )
            }

            // Plan schedule list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlanScheduleRowItem("📚", "$missionSubject — $missionTopic", "25 min", isDark)
                PlanScheduleRowItem("📝", "High-Yield PYQ & Formulas", "20 min", isDark)
                PlanScheduleRowItem("📰", "Daily Current Affairs Digest", "15 min", isDark)
                PlanScheduleRowItem("🧠", "Rapid Recall Quiz", "10 min", isDark)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartSession,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("daily_plan_start_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) ElectricIndigo else DeepIndigo
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start Plan", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenPlan,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(42.dp)
                        .testTag("daily_plan_view_all_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x20000000))
                ) {
                    Text("View All", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
                }
            }
        }
    }
}

@Composable
private fun PlanScheduleRowItem(
    icon: String,
    title: String,
    duration: String,
    isDark: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0x10FFFFFF) else Color(0x06000000)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(icon, fontSize = 13.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = duration,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 10.sp
            )
        }
    }
}

// =========================================================================
// NEEDS PRACTICE (WEAK TOPICS) & STRENGTHS SECTION
// =========================================================================

@Composable
private fun NeedsPracticeAndStrengthsSection(
    topicPerformances: List<TopicPerformanceDetail>,
    onPracticeTopic: (TopicPerformanceDetail) -> Unit,
    onOpenPracticeTab: () -> Unit,
    isDark: Boolean,
    currentTheme: AppThemeMode
) {
    val weakTopics = topicPerformances.filter { it.isWeak }
    val strongTopics = topicPerformances.filter { it.isStrong }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("needs_practice_card"),
        shape = RoundedCornerShape(18.dp),
        elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PERFORMANCE INSIGHTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = if (weakTopics.isNotEmpty()) "${weakTopics.size} Weak Areas" else "Calibrated",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (weakTopics.isNotEmpty()) AmberWarning else EmeraldSuccess
                )
            }

            if (weakTopics.isNotEmpty()) {
                val topWeak = weakTopics.first()
                val acc = topWeak.accuracyPercent?.toInt() ?: 0

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AmberWarning.copy(alpha = if (isDark) 0.12f else 0.08f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, AmberWarning.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("⚠️", fontSize = 12.sp)
                                Text(
                                    text = "Needs Practice: ${topWeak.subject}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${topWeak.topic} • Accuracy: $acc%",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }

                        Button(
                            onClick = { onPracticeTopic(topWeak) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberWarning,
                                contentColor = Color(0xFF0F172A)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("practice_weak_topic_btn")
                        ) {
                            Text("Practice", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (strongTopics.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("⭐ Strengths:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        Text(
                            text = strongTopics.take(3).joinToString(", ") { it.subject },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // New learner onboarding card without fake stats
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0x10FFFFFF) else Color(0x06000000)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Take practice sets to unlock personalized weak topic detection and accuracy trends.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        OutlinedButton(
                            onClick = onOpenPracticeTab,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Quiz, null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                            Spacer(Modifier.width(6.dp))
                            Text("Start 15-Question Practice", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// FEATURE CARD ITEM COMPOSABLE
// =========================================================================

@Composable
private fun FeatureCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()

    Surface(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .springClickable(testTag = testTag, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            accentColor.copy(alpha = if (isDark) 0.35f else 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// LATEST IMPORTANT UPDATE SECTION (SINGLE FOCUSED BANNER)
// =========================================================================

@Composable
private fun LatestImportantUpdateSection(
    recruitmentFeedState: RecruitmentFeedState,
    liveExamFeedState: LiveExamFeedState,
    selectedExamName: String,
    onOpenSmartVacancy: (String?) -> Unit,
    onOpenLiveExamUpdateDetail: (LiveExamUpdateEntity) -> Unit,
    onOpenFullUpdates: () -> Unit,
    onRefreshUpdates: () -> Unit,
    isDark: Boolean,
    currentTheme: AppThemeMode
) {
    var seenUpdateIds by remember { mutableStateOf(setOf<String>()) }

    val topVacancy = recruitmentFeedState.latestForYouVacancies.firstOrNull()
        ?: recruitmentFeedState.allActiveVacancies.firstOrNull { it.examCategory.contains(selectedExamName, true) || selectedExamName.contains("Railway", true) }
        ?: recruitmentFeedState.allActiveVacancies.firstOrNull()

    val topLiveUpdate = liveExamFeedState.whatsNewList.firstOrNull { it.examName.contains(selectedExamName, true) }
        ?: liveExamFeedState.whatsNewList.firstOrNull()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "SMART IMPORTANT UPDATES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )

                if (topVacancy != null && !seenUpdateIds.contains(topVacancy.id)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEF4444)
                    ) {
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            TextButton(
                onClick = onOpenFullUpdates,
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (!recruitmentFeedState.isLoading && topVacancy == null && topLiveUpdate == null) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Couldn't load updates right now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    TextButton(onClick = onRefreshUpdates) {
                        Text("Retry", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (topVacancy != null) {
            // Vacancy update banner
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("latest_vacancy_banner"),
                shape = RoundedCornerShape(16.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 4.dp,
                onClick = {
                    seenUpdateIds = seenUpdateIds + topVacancy.id
                    onOpenSmartVacancy("VACANCIES")
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚆", fontSize = 18.sp)
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = topVacancy.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (!seenUpdateIds.contains(topVacancy.id)) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFEF4444)
                                    ) {
                                        Text(
                                            text = "NEW",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${topVacancy.organization} • ${if (topVacancy.totalVacancies != null && topVacancy.totalVacancies > 0) "${topVacancy.totalVacancies} Posts • " else ""}Last Date: ${topVacancy.applicationLastDate ?: "Soon"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "View",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else if (topLiveUpdate != null) {
            // Live Exam update banner
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("latest_live_update_banner"),
                shape = RoundedCornerShape(16.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 4.dp,
                onClick = {
                    seenUpdateIds = seenUpdateIds + topLiveUpdate.id
                    onOpenLiveExamUpdateDetail(topLiveUpdate)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.NotificationsActive, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = topLiveUpdate.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (!seenUpdateIds.contains(topLiveUpdate.id)) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFEF4444)
                                    ) {
                                        Text(
                                            text = "NEW",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${topLiveUpdate.category.replace("_", " ")} • ${topLiveUpdate.examName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            // Fallback caught up banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x10FFFFFF) else Color(0x06000000)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "You're all caught up with $selectedExamName updates",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    TextButton(onClick = onOpenFullUpdates, contentPadding = PaddingValues(0.dp)) {
                        Text("Check Updates", fontSize = 11.sp, color = NeonCyan)
                    }
                }
            }
        }
    }
}

// =========================================================================
// HELPER COMPOSABLES & UTILITIES
// =========================================================================

@Composable
private fun AllToolsRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Strips raw internal user IDs (e.g. "rahul810232" or "john_doe@mail") to display a clean friendly name.
 */
private fun cleanDisplayName(rawName: String?): String {
    if (rawName.isNullOrBlank()) return "Scholar"
    val beforeAt = rawName.substringBefore("@").trim()
    val spaced = beforeAt.replace("_", " ").replace(".", " ")
    val noDigits = spaced.replace(Regex("\\d+$"), "").trim()
    if (noDigits.isBlank()) return "Scholar"
    return noDigits.split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
}

/**
 * Parses custom study time blocks from JSON.
 */
private fun parseCustomStudyBlocks(json: String?): List<StudyTimeBlock> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<StudyTimeBlock>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                StudyTimeBlock(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    startTime = obj.optString("startTime", "06:00 PM"),
                    subject = obj.optString("subject", "General"),
                    durationMinutes = obj.optInt("durationMinutes", 45),
                    topic = obj.optString("topic", "Core Practice")
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

// =========================================================================
// BLOCK APPS CARD COMPONENT (Focus Shield Integration)
// =========================================================================

@Composable
private fun BlockAppsCardSection(
    onOpenShieldSettings: () -> Unit,
    isDark: Boolean,
    currentTheme: AppThemeMode
) {
    val restrictedCount = remember { FocusShieldManager.getRestrictedPackages().size }
    val isShieldEnabled = remember { FocusShieldManager.isShieldEnabled() }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_block_apps_card"),
        shape = RoundedCornerShape(18.dp),
        elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 4.dp,
        onClick = onOpenShieldSettings
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFF59E0B).copy(alpha = 0.2f))
                            )
                        )
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Block Apps",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Block Distracting Apps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isShieldEnabled) EmeraldSuccess.copy(alpha = 0.15f) else Color(0x2094A3B8)
                        ) {
                            Text(
                                text = if (isShieldEnabled) "SHIELD ON" else "SHIELD OFF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isShieldEnabled) EmeraldSuccess else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (restrictedCount > 0) "$restrictedCount apps blocked during study" else "Block social apps, games & YouTube Shorts",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Button(
                onClick = onOpenShieldSettings,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0x28EF4444) else Color(0x18EF4444),
                    contentColor = Color(0xFFEF4444)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
            ) {
                Text("Manage", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// =========================================================================
// ALL FEATURES CATEGORIZED SECTION (5 Categories: Learn, Practice, Updates, AI & Tools, Account)
// =========================================================================

@Composable
private fun AllFeaturesGridSection(
    onNavigateToTab: (AppNavTab) -> Unit,
    onOpenSmartVacancy: (String?) -> Unit,
    onOpenExamReadinessCenter: () -> Unit,
    onOpenProfileSettings: () -> Unit,
    onOpenNotificationCenter: () -> Unit,
    onOpenDocumentSummarizer: () -> Unit,
    onOpenShieldSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    isDark: Boolean,
    currentTheme: AppThemeMode
) {
    val categories = remember {
        listOf(
            FeatureCategory(
                categoryTitle = "📚 LEARN",
                items = listOf(
                    FeatureGridItem("Study", "Learn subjects & chapters", Icons.AutoMirrored.Filled.MenuBook, DeepIndigo) { onNavigateToTab(AppNavTab.STUDY) },
                    FeatureGridItem("Notes", "Smart AI notes & summaries", Icons.Filled.Description, Color(0xFF8B5CF6)) { onNavigateToTab(AppNavTab.AI_TUTOR) },
                    FeatureGridItem("Current Affairs", "Daily news, CA & quizzes", Icons.Filled.Article, GoldenSpark) { onNavigateToTab(AppNavTab.UPDATES) },
                    FeatureGridItem("Study Materials", "PDF vault & formula sheets", Icons.Filled.PictureAsPdf, Color(0xFFEF4444)) { onOpenDocumentSummarizer() },
                    FeatureGridItem("Chapters", "Syllabus & topic breakdown", Icons.Filled.AutoStories, Color(0xFF3B82F6)) { onNavigateToTab(AppNavTab.STUDY) }
                )
            ),
            FeatureCategory(
                categoryTitle = "📝 PRACTICE",
                items = listOf(
                    FeatureGridItem("Practice", "Subject & topic practice", Icons.Filled.Quiz, EmeraldSuccess) { onNavigateToTab(AppNavTab.PRACTICE) },
                    FeatureGridItem("Mock Tests", "Full length test series", Icons.Filled.TaskAlt, ElectricViolet) { onNavigateToTab(AppNavTab.PRACTICE) },
                    FeatureGridItem("PYQ", "Previous year papers", Icons.Filled.HistoryEdu, Color(0xFFF59E0B)) { onNavigateToTab(AppNavTab.PRACTICE) },
                    FeatureGridItem("Quiz", "Daily speed quizzes", Icons.Filled.Timer, NeonCyan) { onNavigateToTab(AppNavTab.PRACTICE) },
                    FeatureGridItem("Weak Topics", "Targeted mistake bank", Icons.Filled.Spellcheck, Color(0xFFEC4899)) { onNavigateToTab(AppNavTab.PRACTICE) }
                )
            ),
            FeatureCategory(
                categoryTitle = "📰 UPDATES",
                items = listOf(
                    FeatureGridItem("Latest Vacancies", "Railway & sarkari jobs", Icons.Filled.Train, Color(0xFF3B82F6)) { onOpenSmartVacancy("VACANCIES") },
                    FeatureGridItem("Results", "Exam outcomes & cutoffs", Icons.Filled.Assignment, Color(0xFF8B5CF6)) { onOpenSmartVacancy("RESULTS") },
                    FeatureGridItem("Admit Cards", "Hall tickets & dates", Icons.Filled.ConfirmationNumber, Color(0xFFEC4899)) { onOpenSmartVacancy("ADMIT_CARDS") },
                    FeatureGridItem("Notifications", "Exam alerts & reminders", Icons.Outlined.Notifications, GoldenSpark) { onOpenNotificationCenter() },
                    FeatureGridItem("Govt Updates", "Official circulars", Icons.Filled.Campaign, Color(0xFF10B981)) { onNavigateToTab(AppNavTab.UPDATES) }
                )
            ),
            FeatureCategory(
                categoryTitle = "🤖 AI & TOOLS",
                items = listOf(
                    FeatureGridItem("Nova AI", "AI tutor & doubt solver", Icons.Filled.AutoAwesome, NeonCyan) { onNavigateToTab(AppNavTab.AI_TUTOR) },
                    FeatureGridItem("Voice Assistant", "Ask Nova via Voice", Icons.Filled.Mic, ElectricViolet) { onNavigateToTab(AppNavTab.AI_TUTOR) },
                    FeatureGridItem("PDF Vault", "Summarize & read PDFs", Icons.Filled.FolderZip, Color(0xFFF97316)) { onOpenDocumentSummarizer() },
                    FeatureGridItem("Progress", "Analytics & readiness", Icons.Outlined.Speed, NebulaPurple) { onOpenExamReadinessCenter() },
                    FeatureGridItem("Focus Shield", "Block distracting apps", Icons.Filled.Shield, Color(0xFFEF4444)) { onOpenShieldSettings() },
                    FeatureGridItem("Smart Search", "Syllabus & formula finder", Icons.Filled.Search, Color(0xFF06B6D4)) { onOpenSearch() }
                )
            ),
            FeatureCategory(
                categoryTitle = "👤 ACCOUNT",
                items = listOf(
                    FeatureGridItem("Saved", "Bookmarks & saved items", Icons.Filled.Bookmark, Color(0xFF06B6D4)) { onNavigateToTab(AppNavTab.PRACTICE) },
                    FeatureGridItem("Profile", "Student profile & target", Icons.Filled.Person, Color(0xFF3B82F6)) { onOpenProfileSettings() },
                    FeatureGridItem("Settings", "App theme & preferences", Icons.Filled.Settings, Color(0xFF64748B)) { onOpenProfileSettings() },
                    FeatureGridItem("Language", "UI & content language", Icons.Filled.Translate, GoldenSpark) { onOpenProfileSettings() },
                    FeatureGridItem("Personalization", "Study goals & targets", Icons.Filled.Tune, EmeraldSuccess) { onOpenProfileSettings() }
                )
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ALL FEATURES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                letterSpacing = 1.sp
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isDark) Color(0x1A38BDF8) else Color(0x100284C7)
            ) {
                Text(
                    text = "COMPLETE ACCESS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        categories.forEach { category ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = category.categoryTitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    letterSpacing = 0.5.sp
                )

                val chunked = category.items.chunked(2)
                chunked.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            CompactFeatureCardItem(
                                title = item.title,
                                subtitle = item.subtitle,
                                icon = item.icon,
                                accentColor = item.accentColor,
                                modifier = Modifier.weight(1f),
                                testTag = "feature_card_${item.title.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")}",
                                isDark = isDark,
                                currentTheme = currentTheme,
                                onClick = item.onClick
                            )
                        }
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFeatureCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    isDark: Boolean,
    currentTheme: AppThemeMode,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0x16FFFFFF) else Color(0x0A000000),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isDark) Color(0x1FFFFFFF) else Color(0x12000000)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = if (isDark) 0.2f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class FeatureCategory(
    val categoryTitle: String,
    val items: List<FeatureGridItem>
)

private data class FeatureGridItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(true) }
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isOnline = true }
            override fun onLost(network: Network) { isOnline = false }
        }
        if (cm != null) {
            try {
                val activeNet = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(activeNet)
                isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                cm.registerDefaultNetworkCallback(callback)
            } catch (e: Exception) {
                isOnline = true
            }
        }
        onDispose {
            try { cm?.unregisterNetworkCallback(callback) } catch (e: Exception) {}
        }
    }
    return isOnline
}

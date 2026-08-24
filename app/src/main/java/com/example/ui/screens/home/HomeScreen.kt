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
import com.example.service.intelligence.*
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

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
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val themeController = LocalThemeController.current
    val currentTheme = currentThemeMode()

    // Modals state
    var showExamSwitcherDialog by remember { mutableStateOf(false) }
    var showQuickSearchDialog by remember { mutableStateOf(false) }
    var showNotificationSummaryDialog by remember { mutableStateOf(false) }
    var showAllToolsDialog by remember { mutableStateOf(false) }
    var showQuickStudyDialog by remember { mutableStateOf(false) }
    var showTransparencyDialog by remember { mutableStateOf(false) }
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

    val timeBasedPlan = remember(selectedPlanTimeOption, topicPerformances, allTopicMasteries, user) {
        PersonalizationEngine.generateTimeBasedStudyPlan(selectedPlanTimeOption, topicPerformances, allTopicMasteries, user)
    }

    // Step 23 Smart Learning States
    val dailyExamBriefing = novaViewModel?.dailyExamBriefing?.collectAsState()?.value
    val isBriefingLoading = novaViewModel?.isDailyBriefingLoading?.collectAsState()?.value ?: false
    val showMcqDialog = novaViewModel?.showMcqConfigDialog?.collectAsState()?.value ?: false
    val showRevisionDialog = novaViewModel?.showRevisionDialog?.collectAsState()?.value ?: false
    val activeRevisionTopic = novaViewModel?.activeRevisionTopic?.collectAsState()?.value

    // 1. Time-based dynamic greeting
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingTime = when (currentHour) {
        in 4..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    val studentDisplayName = remember(user?.name) {
        cleanDisplayName(user?.name)
    }

    // 2. Exam Context & Dynamic Countdown
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

    // 3. Smart Mission & Progress Determination
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
        ?: 30

    val recommendationReason = remember(nextPendingTask, studyNowRecommendation, user) {
        if (nextPendingTask != null) {
            "Next priority item in your structured study schedule"
        } else if (studyNowRecommendation != null) {
            studyNowRecommendation.reasoning.ifBlank { "High-yield topic calibrated for your exam" }
        } else if (!user?.weakSubjects.isNullOrEmpty()) {
            "Targeted practice for ${user?.weakSubjects?.first()}"
        } else {
            "Daily core topic for ${selectedExamName.take(15)}"
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

    // Custom study schedule blocks (from user profile)
    val customBlocks = remember(user?.customStudyBlocksJson) {
        parseCustomStudyBlocks(user?.customStudyBlocksJson)
    }

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // =========================================================================
        // A. COMPACT HEADER (Greeting, User Name, Motivational Indicator, Profile)
        // =========================================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Compact Greeting Hierarchy
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$greetingTime 👋",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) NeonCyan else DeepIndigo
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Small motivational flame/streak pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0x22FBBF24) else Color(0x18FBBF24),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldenSpark.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = GoldenSpark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "$streakDays d",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldenSpark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = studentDisplayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right: Compact Action Cluster (Search, Notifications, Profile)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search
                    IconButton(
                        onClick = { showQuickSearchDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1CFFFFFF) else Color(0x10000000))
                            .border(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x2064748B), CircleShape)
                            .testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search study topics",
                            tint = if (isDark) NeonCyan else DeepIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Notification
                    Box {
                        IconButton(
                            onClick = onOpenNotificationCenter,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x1CFFFFFF) else Color(0x10000000))
                                .border(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x2064748B), CircleShape)
                            .testTag("home_notification_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications & Reminders",
                                tint = if (isDark) Color.White else Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (unreadNotificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(NeonCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadNotificationCount > 9) "9+" else "$unreadNotificationCount",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF070B19)
                                )
                            }
                        }
                    }

                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.35f), ElectricViolet.copy(alpha = 0.35f)))
                            )
                            .border(1.dp, NeonCyan.copy(alpha = 0.65f), CircleShape)
                            .springClickable(testTag = "home_profile_button", onClick = onOpenProfileSettings),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user?.photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = studentDisplayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) NeonCyan else DeepIndigo
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // RESUME UNFINISHED MOCK TEST BANNER (IF ACTIVE SESSION EXISTS)
        // =========================================================================
        if (pendingResumeSession != null && pendingResumeSession.questions.isNotEmpty()) {
            item {
                val answeredCount = pendingResumeSession.selectedAnswers.size
                val totalCount = pendingResumeSession.questions.size
                val remainingSec = pendingResumeSession.remainingSeconds
                val mins = remainingSec / 60
                val secs = remainingSec % 60
                val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_mock_test_banner"),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = Color(0xFF1E1428).copy(alpha = 0.95f),
                    borderColor = AmberWarning.copy(alpha = 0.6f),
                    borderWidth = 1.5.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AmberWarning.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayCircleFilled,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Unfinished Mock Test",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberWarning,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = pendingResumeSession.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AmberWarning.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, AmberWarning.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "⏳ $timeFormatted",
                                    color = AmberWarning,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Question ${pendingResumeSession.currentQuestionIndex + 1} of $totalCount • $answeredCount answered",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onResumePendingTest,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberWarning,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("resume_test_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Resume Test", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onDiscardPendingTest,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF94A3B8)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x30FFFFFF)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("discard_test_button")
                            ) {
                                Text("Discard")
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. EXAM CONTEXT CARD (Dynamic Target, Days Left, Edit Action)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("current_exam_card"),
                shape = RoundedCornerShape(18.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 6.dp,
                onClick = { showExamSwitcherDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Exam Icon Container
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x2838BDF8) else Color(0x186366F1))
                                .border(0.5.dp, if (isDark) NeonCyan.copy(alpha = 0.4f) else DeepIndigo.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = "Exam",
                                tint = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedExamName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Switch Exam",
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$examCategory • Target: $formattedTargetDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Days Remaining Glass Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0x2238BDF8) else Color(0x156366F1),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (isDark) NeonCyan.copy(alpha = 0.5f) else DeepIndigo.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$daysRemaining",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) NeonCyan else DeepIndigo
                            )
                            Text(
                                text = "days left",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2.5. NOVA UNIVERSAL SMART WIDGET (STEP 20)
        // =========================================================================
        if (novaViewModel != null) {
            item {
                NovaHomeUniversalWidget(
                    viewModel = novaViewModel,
                    onNavigateToTab = onNavigateToTab,
                    modifier = Modifier.testTag("nova_home_widget")
                )
            }
        }

        // =========================================================================
        // 2.6. WHAT'S NEW FOR YOU (STEP 21 LIVE EXAM INTELLIGENCE)
        // =========================================================================
        item {
            WhatsNewForYouSection(
                feedState = liveExamFeedState,
                isRefreshing = isRefreshingLiveExam,
                onRefresh = onRefreshLiveExam,
                onOpenUpdateDetail = onOpenLiveExamUpdateDetail,
                onToggleSave = onToggleSaveLiveExamUpdate,
                onViewAll = onOpenFullLiveExamIntelligence
            )
        }

        // =========================================================================
        // 2.65. STEP 40: SMART VACANCY, RESULTS & ADMIT CARD RADAR
        // =========================================================================
        item {
            HomeRecruitmentRadarSection(
                feedState = recruitmentFeedState,
                onOpenHub = onOpenSmartVacancy
            )
        }

        // =========================================================================
        // 2.7. TODAY'S EXAM BRIEF (STEP 23 SMART LEARNING SYSTEM)
        // =========================================================================
        if (novaViewModel != null) {
            item {
                TodayExamBriefWidget(
                    briefing = dailyExamBriefing,
                    isLoading = isBriefingLoading,
                    onRefresh = { novaViewModel.fetchDailyExamBriefing(forceRefresh = true) },
                    onStartBriefing = onOpenDailyBriefing,
                    onStartMiniQuiz = {
                        novaViewModel.sendMessage("Aaj ka exam briefing quiz start karo")
                        onNavigateToTab(AppNavTab.AI_TUTOR) // Navigate to NOVA
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // =========================================================================
        // 2.8. STEP 36: SMART PERSONALIZATION & TODAY'S FOCUS
        // =========================================================================
        item {
            TodaysFocusWidget(
                recommendation = todayFocusRec,
                onStartPractice = { config ->
                    onStartPracticeWithConfig(config)
                },
                onStartRevision = { sub, top ->
                    novaViewModel?.startRevisionSession(sub, top)
                    onNavigateToTab(AppNavTab.STUDY)
                },
                onOpenCurrentAffairs = {
                    onOpenDailyBriefing()
                },
                onOpenQuickStudyModal = {
                    showQuickStudyDialog = true
                },
                isDark = isDark
            )
        }

        item {
            TimeBasedStudyPlanWidget(
                plan = timeBasedPlan,
                onSelectTimeOption = { selectedPlanTimeOption = it },
                onStartStep = { step ->
                    if (step.activityType == "CURRENT_AFFAIRS") {
                        onOpenDailyBriefing()
                    } else if (step.activityType == "REVISION") {
                        novaViewModel?.startRevisionSession(step.targetSubject, step.targetTopic)
                        onNavigateToTab(AppNavTab.STUDY)
                    } else {
                        onStartPracticeWithConfig(
                            MockTestConfig(
                                exam = selectedExamName,
                                testType = MockTestType.SUBJECT_PRACTICE,
                                subject = step.targetSubject,
                                chapter = step.targetTopic,
                                topic = step.targetTopic,
                                questionCount = 10,
                                timeLimitMinutes = 10
                            )
                        )
                    }
                },
                isDark = isDark
            )
        }

        item {
            DueForRevisionWidget(
                topicMasteries = allTopicMasteries,
                onReviseTopic = { sub, top ->
                    novaViewModel?.startRevisionSession(sub, top)
                    onNavigateToTab(AppNavTab.STUDY)
                },
                isDark = isDark
            )
        }

        item {
            PrimaryRecommendationCard(
                recommendation = todayFocusRec,
                onStartAction = {
                    onStartPracticeWithConfig(todayFocusRec.config)
                },
                onOpenTransparencyModal = {
                    showTransparencyDialog = true
                },
                isDark = isDark
            )
        }

        // =========================================================================
        // 3. TODAY'S STUDY STATUS (Single Unified Clean Glass Card)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("todays_progress_card"),
                shape = RoundedCornerShape(20.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 2.dp else 6.dp,
                onClick = onOpenExamReadinessCenter
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.TrendingUp,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TODAY'S PROGRESS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                letterSpacing = 0.8.sp
                            )
                        }

                        // Percentage badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0x2238BDF8) else Color(0x186366F1),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "$progressPercentage%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress time & goal headline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formattedStudyTime,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = " / $formattedTargetTime",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                            )
                        }

                        Text(
                            text = if (progressPercentage >= 100) "Goal Reached! 🎉" else "${(targetDailyMinutes - todayFocusMinutes).coerceAtLeast(0)}m remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (progressPercentage >= 100) EmeraldSuccess else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                            fontWeight = if (progressPercentage >= 100) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Liquid Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDark) Color(0x20FFFFFF) else Color(0x15000000))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(NeonCyan, ElectricViolet, NebulaPurple)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3-Metric Sub-row: Tasks Completed | Level & XP | Readiness
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Metric 1: Tasks
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(15.dp)
                            )
                            Column {
                                Text(
                                    text = "$completedPlanCount / $totalPlanCount Done",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Daily Tasks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Divider dot
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0x40888888)))

                        // Metric 2: XP & Level
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Stars,
                                contentDescription = null,
                                tint = GoldenSpark,
                                modifier = Modifier.size(15.dp)
                            )
                            Column {
                                Text(
                                    text = "Lvl $userLevel • ${userXp} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Student Rank",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Divider dot
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0x40888888)))

                        // Metric 3: Readiness Score
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                tint = NebulaPurple,
                                modifier = Modifier.size(15.dp)
                            )
                            Column {
                                Text(
                                    text = "${examReadiness?.readinessScore ?: 56}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Readiness",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3.5. EXAM RADAR (STEP 21 LIVE PULSE INTELLIGENCE)
        // =========================================================================
        item {
            ExamRadarSection(
                feedState = liveExamFeedState,
                onViewFullRadar = onOpenFullLiveExamIntelligence,
                onOpenUpdateDetail = onOpenLiveExamUpdateDetail
            )
        }

        // =========================================================================
        // 4. PRIMARY ACTION (Dominant Cyan → Violet CTA + Current Subject/Topic)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("todays_mission_card"),
                shape = RoundedCornerShape(22.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 2.dp else 10.dp,
                borderColor = NeonCyan.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Dominant Button
                    val isSessionActive = todayFocusMinutes > 0
                    val primaryCtaTitle = if (isSessionActive) "▶ Continue Studying" else "▶ Start Today's Study"

                    Button(
                        onClick = { onStartFocusSession(missionSubject, missionTopic) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_mission_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(NeonCyan, DeepIndigo, NebulaPurple)
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
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
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = primaryCtaTitle.removePrefix("▶ ").trim(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Current / Recommended Subject Sub-bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x18FFFFFF) else Color(0x0A000000))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                            Text(
                                text = "$missionSubject • $missionTopic",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "${missionTargetMinutes}m session",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 5. NEXT BEST STUDY ACTION (Recommended Next Card)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recommended_next_card"),
                shape = RoundedCornerShape(18.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 6.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨ RECOMMENDED NEXT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                letterSpacing = 0.8.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0x20818CF8) else Color(0x156366F1),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, ElectricViolet.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${missionTargetMinutes} min focused session",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) ElectricViolet else DeepIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = missionSubject,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = missionTopic,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = recommendationReason,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Start CTA
                        Button(
                            onClick = { onStartFocusSession(missionSubject, missionTopic) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0x2838BDF8) else Color(0x186366F1),
                                contentColor = if (isDark) NeonCyan else DeepIndigo
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isDark) NeonCyan.copy(alpha = 0.5f) else DeepIndigo.copy(alpha = 0.4f)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Start",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 6. NOVA QUICK ASSIST (Compact Card)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nova_ai_card"),
                shape = RoundedCornerShape(18.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 6.dp,
                onClick = { onNavigateToTab(AppNavTab.AI_TUTOR) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.3f), NebulaPurple.copy(alpha = 0.3f))))
                                .border(0.5.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Nova",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "✨ Ask NOVA",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Ask anything about your preparation...",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0x2838BDF8) else Color(0x186366F1),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Ask",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) NeonCyan else DeepIndigo
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 6.5. TRENDING TOPICS FOR MY EXAM (STEP 21)
        // =========================================================================
        item {
            TrendingExamTopicsSection(
                feedState = liveExamFeedState,
                onTopicClick = { onOpenFullLiveExamIntelligence() },
                onToggleSave = onToggleSaveTrendingTopic,
                onStartQuizForTopic = { topic -> onStartQuizForTopic(topic.category, topic.title) }
            )
        }

        // =========================================================================
        // 7. TODAY'S SCHEDULE (Compact Upcoming 2–3 Sessions)
        // =========================================================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("todays_schedule_card"),
                shape = RoundedCornerShape(18.dp),
                elevation = if (currentTheme == AppThemeMode.AMOLED_BLACK) 1.dp else 6.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TODAY'S SCHEDULE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                letterSpacing = 0.8.sp
                            )
                        }

                        TextButton(
                            onClick = { onNavigateToTab(AppNavTab.STUDY) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("view_plan_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "View Full Timetable",
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Schedule Items (Next 2-3 sessions)
                    if (customBlocks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customBlocks.take(3).forEach { block ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = block.startTime,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) NeonCyan else DeepIndigo
                                            )
                                            Text(
                                                text = "${block.subject} • ${block.topic.ifBlank { "Practice" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = "${block.durationMinutes}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    } else if (studyPlan.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            studyPlan.take(3).forEach { planItem ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onTogglePlanItem(planItem.id, !planItem.isCompleted) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (planItem.isCompleted) {
                                        if (isDark) Color(0x1810B981) else Color(0x1010B981)
                                    } else {
                                        if (isDark) Color(0x14FFFFFF) else Color(0x08000000)
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (planItem.isCompleted) EmeraldSuccess.copy(alpha = 0.4f)
                                        else if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (planItem.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                                contentDescription = null,
                                                tint = if (planItem.isCompleted) EmeraldSuccess else Color(0xFF94A3B8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "${planItem.subject} • ${planItem.topic}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (planItem.isCompleted) Color(0xFF94A3B8) else (if (isDark) Color.White else Color(0xFF0F172A)),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = "${planItem.targetMinutes}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (planItem.isCompleted) EmeraldSuccess else (if (isDark) NeonCyan else DeepIndigo),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty State
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Start your first study session",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )

                            Button(
                                onClick = { onNavigateToTab(AppNavTab.STUDY) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) ElectricIndigo else DeepIndigo
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Plan Schedule",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 8. QUICK STUDY TOOLS (Clean 2-Column Glass Grid + View All Tools)
        // =========================================================================
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "QUICK TOOLS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        letterSpacing = 0.8.sp
                    )

                    TextButton(
                        onClick = { showAllToolsDialog = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View All Tools",
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

                Spacer(modifier = Modifier.height(8.dp))

                // 2-Column Grid for the 6 core tools
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 1: Readiness & Planner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickToolGridCard(
                            title = "Readiness",
                            subtitle = "Exam Score & Gaps",
                            icon = Icons.Outlined.Speed,
                            accentColor = NebulaPurple,
                            modifier = Modifier.weight(1f),
                            testTag = "quick_action_readiness",
                            onClick = onOpenExamReadinessCenter
                        )

                        QuickToolGridCard(
                            title = "Planner",
                            subtitle = "Study Timetable",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            accentColor = DeepIndigo,
                            modifier = Modifier.weight(1f),
                            testTag = "quick_action_study",
                            onClick = { onNavigateToTab(AppNavTab.STUDY) }
                        )
                    }

                    // Row 2: Mock Tests & Revision
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickToolGridCard(
                            title = "Mock Tests",
                            subtitle = "Full-Length & Quiz",
                            icon = Icons.Filled.Quiz,
                            accentColor = EmeraldSuccess,
                            modifier = Modifier.weight(1f),
                            testTag = "quick_action_mock",
                            onClick = { onNavigateToTab(AppNavTab.PROGRESS) }
                        )

                        QuickToolGridCard(
                            title = "Revision",
                            subtitle = "Smart Spaced Review",
                            icon = Icons.Filled.Repeat,
                            accentColor = GoldenSpark,
                            modifier = Modifier.weight(1f),
                            testTag = "quick_action_revision",
                            onClick = { onNavigateToTab(AppNavTab.STUDY) }
                        )
                    }

                    // Row 3: Focus Shield & NOVA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickToolGridCard(
                            title = "Focus",
                            subtitle = "App Shield & Pomodoro",
                            icon = Icons.Filled.Shield,
                            accentColor = CoralRose,
                            modifier = Modifier.weight(1f),
                            testTag = "quick_action_focus",
                            onClick = { onNavigateToTab(AppNavTab.FOCUS) }
                        )

                        QuickToolGridCard(
                            title = "NOVA AI",
                            subtitle = "24/7 Personal Tutor",
                            icon = Icons.Filled.AutoAwesome,
                            accentColor = NeonCyan,
                            modifier = Modifier.weight(1f),
                            testTag = "quick_action_nova",
                            onClick = { onNavigateToTab(AppNavTab.AI_TUTOR) }
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // EXAM SWITCHER MODAL DIALOG
    // =========================================================================
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

    // =========================================================================
    // QUICK SEARCH MODAL DIALOG
    // =========================================================================
    if (showQuickSearchDialog) {
        var searchQuery by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQuickSearchDialog = false },
            containerColor = if (isDark) Color(0xFF111827) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart Study Search",
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
                    Text(
                        text = "Search questions, chapters, formulas, or past year topics for $selectedExamName.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_search_input"),
                        placeholder = { Text("e.g. Thermodynamics formulas, Railway GK...") },
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

                    // Popular topic chips
                    Text(
                        text = "Quick Topics:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Formulas", "Mock Test", "Weak Areas").forEach { chip ->
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
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

    // =========================================================================
    // NOTIFICATION & REMINDER MODAL DIALOG
    // =========================================================================
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

    // =========================================================================
    // VIEW ALL TOOLS MODAL DIALOG
    // =========================================================================
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
                    // Tool: Document Summarizer
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

                    // Tool: Scan Question
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

                    // Tool: Smart Search
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

                    // Tool: Exam Readiness Center
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
                onNavigateToTab(AppNavTab.AI_TUTOR) // Navigate to NOVA
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
// HELPER COMPOSABLES & FUNCTIONS
// =========================================================================

@Composable
private fun QuickToolGridCard(
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
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .springClickable(testTag = testTag, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            accentColor.copy(alpha = if (isDark) 0.35f else 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
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

@Composable
fun HomeRecruitmentRadarSection(
    feedState: RecruitmentFeedState,
    onOpenHub: (String?) -> Unit
) {
    val isDark = isAppInDarkTheme()
    val vacanciesCount = feedState.allActiveVacancies.size
    val resultsCount = feedState.resultsList.size
    val admitCardsCount = feedState.admitCardsList.size

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_recruitment_radar_card"),
        shape = RoundedCornerShape(18.dp),
        elevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚀", fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Recruitment Radar",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "LIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            "Verified Sarkari Jobs, Results & Admit Cards",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                TextButton(
                    onClick = { onOpenHub(null) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("View All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Quick Category Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickHubPill(
                    icon = "💼",
                    label = "Vacancies",
                    badge = "$vacanciesCount Active",
                    badgeColor = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenHub(RecruitmentContentType.VACANCY.name) }
                )
                QuickHubPill(
                    icon = "🏆",
                    label = "Results",
                    badge = "$resultsCount New",
                    badgeColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenHub(RecruitmentContentType.RESULT.name) }
                )
                QuickHubPill(
                    icon = "🎫",
                    label = "Admit Cards",
                    badge = "$admitCardsCount Out",
                    badgeColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenHub(RecruitmentContentType.ADMIT_CARD.name) }
                )
            }

            // Top Vacancy Teaser if available
            val topVacancy = feedState.latestForYouVacancies.firstOrNull() ?: feedState.allActiveVacancies.firstOrNull()
            if (topVacancy != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = { onOpenHub(RecruitmentContentType.VACANCY.name) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                topVacancy.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${topVacancy.organization} • ${if (topVacancy.totalVacancies != null && topVacancy.totalVacancies > 0) "${topVacancy.totalVacancies} Posts • " else ""}Closes ${topVacancy.applicationLastDate ?: "Soon"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickHubPill(
    icon: String,
    label: String,
    badge: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                badge,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

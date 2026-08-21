package com.example.ui.screens.planner

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.screens.flashcards.FlashcardScreen
import com.example.ui.theme.*
import java.util.Calendar

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
    onStartSessionTimer: (StudyPlanItem) -> Unit = {},
    deadlineWarning: String? = null,
    userPreferences: UserStudyPreferences = UserStudyPreferences(),
    onSavePreferences: (UserStudyPreferences) -> Unit = {},
    activeExamContext: ExamContext = ExamContext(),
    topicMasteries: List<TopicMastery> = emptyList(),
    focusSessions: List<FocusSession> = emptyList(),
    onApplySubjectAllocations: (subjectMinutes: Map<String, Int>, totalDailyMins: Int, startHour: Int, breakMins: Int) -> Unit = { _, _, _, _ -> },
    onOpenExamSelector: () -> Unit = {},
    // Interactive timer parameters
    activeStudySession: StudyPlanItem? = null,
    sessionRemainingSeconds: Int = 0,
    isSessionTimerRunning: Boolean = false,
    isSessionPaused: Boolean = false,
    activeSessionActualMinutes: Int = 0,
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onFinishSession: (notes: String) -> Unit = {},
    onCancelSession: () -> Unit = {},
    // Flashcard parameters
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
    val isHindi = user?.languagePreference == "Hindi"

    var selectedSection by remember { mutableIntStateOf(0) } // 0: Study Plan, 1: Flashcards, 2: Exam Roadmap
    var showEditPlanDialog by remember { mutableStateOf(false) }
    var showAddCustomTaskDialog by remember { mutableStateOf(false) }
    var showCalendarModal by remember { mutableStateOf(false) }
    var editingPlanItem by remember { mutableStateOf<StudyPlanItem?>(null) }
    var selectedCalendarDayOffset by remember { mutableIntStateOf(0) } // 0: Today, 1: Tomorrow, etc.

    // If an active session is currently running in timer mode, display the active HUD timer
    if (activeStudySession != null && (isSessionTimerRunning || isSessionPaused || sessionRemainingSeconds > 0)) {
        StudySessionTimerView(
            activeSession = activeStudySession,
            remainingSeconds = sessionRemainingSeconds,
            isTimerRunning = isSessionTimerRunning,
            isPaused = isSessionPaused,
            actualMinutesSpent = activeSessionActualMinutes,
            onPauseTimer = onPauseTimer,
            onResumeTimer = onResumeTimer,
            onFinishSession = onFinishSession,
            onCancelSession = onCancelSession,
            modifier = modifier
        )
        return
    }

    // Secondary sub-tab: Digital Flashcards
    if (selectedSection == 1) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(appBackgroundGradient(isDark))
        ) {
            PlannerSubHeader(
                selectedSection = selectedSection,
                onSelectSection = { selectedSection = it },
                isDark = isDark,
                isHindi = isHindi
            )
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

    // Compute dynamic study metrics
    val targetMinutes = userPreferences.dailyAvailableMinutes.coerceAtLeast(30)
    val todayStartMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Actual studied minutes today from focus sessions + completed plan items
    val studiedMinutesFromSessions = remember(focusSessions, todayStartMillis) {
        focusSessions.filter { it.timestamp >= todayStartMillis }.sumOf { it.actualMinutesSpent.coerceAtLeast(1) }
    }
    val studiedMinutesFromPlans = remember(planItems) {
        planItems.filter { it.isCompleted }.sumOf { it.actualMinutesSpent.coerceAtLeast(it.targetMinutes) }
    }
    val totalStudiedMinutes = (studiedMinutesFromSessions.coerceAtLeast(studiedMinutesFromPlans)).coerceAtLeast(0)
    val progressPercent = ((totalStudiedMinutes.toFloat() / targetMinutes) * 100f).coerceIn(0f, 100f).toInt()

    val completedCount = planItems.count { it.isCompleted }
    val totalSessionsCount = planItems.size
    val remainingStudyMins = (targetMinutes - totalStudiedMinutes).coerceAtLeast(0)

    // Exact next pending session
    val nextPendingSession = remember(planItems) {
        planItems.firstOrNull { !it.isCompleted }
    }

    // Filter subjects strictly for the currently selected exam
    val examSubjects = remember(activeExamContext, user) {
        if (activeExamContext.subjects.isNotEmpty()) {
            activeExamContext.subjects.map { it.name }
        } else if (!user?.subjects.isNullOrEmpty()) {
            user!!.subjects
        } else {
            listOf("General Science", "Mathematics", "General Intelligence & Reasoning", "General Awareness")
        }
    }

    // Find weak topics strictly belonging to the active exam
    val weakExamTopics = remember(topicMasteries, examSubjects, activeExamContext) {
        topicMasteries
            .filter { mastery ->
                (mastery.examId == activeExamContext.examId || examSubjects.any { it.equals(mastery.subject, ignoreCase = true) }) &&
                        (mastery.masteryScore < 70 || mastery.practiceAccuracyPercent < 70f || mastery.masteryState == "WEAK")
            }
            .sortedBy { it.masteryScore }
            .take(3)
    }

    // Primary Smart Recommendation (Best Next Step)
    val bestNextStep = remember(nextPendingSession, weakExamTopics, planItems) {
        when {
            nextPendingSession != null -> {
                Triple(
                    if (isHindi) "जारी रखें: ${nextPendingSession.subject}" else "Continue: ${nextPendingSession.subject}",
                    "${nextPendingSession.topic.ifBlank { nextPendingSession.chapter }} (${nextPendingSession.targetMinutes} min)",
                    nextPendingSession
                )
            }
            weakExamTopics.isNotEmpty() -> {
                val weak = weakExamTopics.first()
                val item = StudyPlanItem(
                    subject = weak.subject,
                    chapter = "Weak Topic Liquidation",
                    topic = weak.topic,
                    targetMinutes = 30,
                    sessionType = "WEAK_TOPIC",
                    priority = PlanPriority.HIGH,
                    aiRecommendationReason = if (isHindi) "कम सटीकता (${weak.masteryScore}%) के कारण प्राथमिकता" else "Prioritized due to low mastery score (${weak.masteryScore}%)"
                )
                Triple(
                    if (isHindi) "कमजोर विषय का अभ्यास करें" else "Strengthen Weak Topic",
                    "${weak.subject} • ${weak.topic}",
                    item
                )
            }
            planItems.isNotEmpty() -> {
                val first = planItems.first()
                Triple(
                    if (isHindi) "आज का अध्ययन पूरा हुआ! 🌟" else "Daily Plan Completed! 🌟",
                    if (isHindi) "अतिरिक्त अभ्यास या मॉक टेस्ट लें" else "Take a quick practice quiz or review flashcards",
                    first
                )
            }
            else -> {
                val firstSub = examSubjects.firstOrNull() ?: "Core Practice"
                val item = StudyPlanItem(
                    subject = firstSub,
                    chapter = "Foundations",
                    topic = "$firstSub Core Concept",
                    targetMinutes = 45,
                    sessionType = "LEARNING",
                    priority = PlanPriority.HIGH
                )
                Triple(
                    if (isHindi) "आज का प्लान बनाएं" else "Start Today's Study",
                    if (isHindi) "स्मार्ट AI टाइमटेबल तैयार करें" else "Generate your exam-grounded schedule",
                    item
                )
            }
        }
    }

    val missedItems = remember(planItems) {
        planItems.filter { !it.isCompleted && it.priority == PlanPriority.HIGH }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundGradient(isDark))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("study_screen"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. TOP HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp)
                        .testTag("study_header"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "📚 अध्ययन योजना" else "📚 Study",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isHindi) "आपकी व्यक्तिगत परीक्षा तैयारी योजना" else "Your personalized preparation plan",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Calendar Button
                        IconButton(
                            onClick = { showCalendarModal = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x3038BDF8) else Color(0x200284C7))
                                .testTag("study_calendar_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Edit Plan / Settings Button
                        IconButton(
                            onClick = { showEditPlanDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x30A855F7) else Color(0x207C3AED))
                                .testTag("study_edit_plan_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Edit Plan & Time Allocations",
                                tint = ElectricViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        ThemeToggleButton(testTag = "study_theme_toggle")
                    }
                }
            }

            // Sub-Tab Switcher (Study Plan / Flashcards / Roadmap)
            item {
                PlannerSubHeader(
                    selectedSection = selectedSection,
                    onSelectSection = { selectedSection = it },
                    isDark = isDark,
                    isHindi = isHindi
                )
            }

            if (selectedSection == 0) {
                // 2. SELECTED EXAM CONTEXT CARD
                item {
                    val examDisplayTitle = activeExamContext.examName.ifBlank {
                        user?.examName?.ifBlank { "Railway RRB NTPC / Group D" } ?: "Railway RRB NTPC / Group D"
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preparing_for_card"),
                        fillAlpha = 0.85f,
                        shape = RoundedCornerShape(20.dp),
                        borderColor = NeonCyan.copy(alpha = 0.4f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = NeonCyan.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = if (isHindi) "🎯 लक्ष्य परीक्षा" else "🎯 PREPARING FOR",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = NeonCyan,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = onOpenExamSelector,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "परीक्षा बदलें 🔄" else "Switch Exam 🔄",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricViolet
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = examDisplayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0x251E293B)
                                ) {
                                    Text(
                                        text = "${examSubjects.size} ${if (isHindi) "विषय" else "Subjects"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0x251E293B)
                                ) {
                                    Text(
                                        text = "${activeExamContext.topics.size.takeIf { it > 0 } ?: 42} ${if (isHindi) "टॉपिक्स" else "Topics"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (activeExamContext.durationMinutes > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0x251E293B)
                                    ) {
                                        Text(
                                            text = "${activeExamContext.durationMinutes}m ${if (isHindi) "परीक्षा पैटर्न" else "Pattern"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldenSpark,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. STUDY OVERVIEW (TODAY'S STUDY CARD)
                item {
                    val studiedHours = totalStudiedMinutes / 60
                    val studiedMins = totalStudiedMinutes % 60
                    val targetHours = targetMinutes / 60
                    val targetMins = targetMinutes % 60

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("today_overview_card"),
                        fillAlpha = 0.85f,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "आज का अध्ययन" else "Today's Study",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "$progressPercent%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (progressPercent >= 100) EmeraldSuccess else NeonCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Main Big Counter
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "${studiedHours}h ${studiedMins}m",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "/ ${targetHours}h ${if (targetMins > 0) "${targetMins}m" else ""}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Liquid Gradient Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isDark) Color(0x301E293B) else Color(0x40E2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (progressPercent / 100f).coerceIn(0.02f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(NeonCyan, ElectricViolet, EmeraldSuccess)
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Key Metrics Row: Sessions Completed, Remaining Time, Daily Target
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (isHindi) "सत्र पूर्ण" else "Sessions Done",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$completedCount / $totalSessionsCount",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (isHindi) "शेष समय" else "Remaining",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${remainingStudyMins / 60}h ${remainingStudyMins % 60}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldenSpark
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (isHindi) "दैनिक लक्ष्य" else "Daily Target",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${targetHours}h ${if (targetMins > 0) "${targetMins}m" else ""}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. CONTINUE STUDY (PRIMARY CTA)
                item {
                    val (actionTitle, actionSubtitle, targetItem) = bestNextStep

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("continue_studying_card")
                            .springClickable(testTag = "continue_studying_btn") {
                                if (targetItem.id != 0L) {
                                    onStartSessionTimer(targetItem)
                                } else {
                                    onStartFocusSession(targetItem.subject, targetItem.topic)
                                }
                            },
                        fillAlpha = 0.9f,
                        shape = RoundedCornerShape(20.dp),
                        borderColor = GoldenSpark.copy(alpha = 0.6f),
                        contentPadding = PaddingValues(16.dp)
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
                                Surface(
                                    shape = CircleShape,
                                    color = GoldenSpark,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color(0xFF070B19),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (nextPendingSession != null) (if (isHindi) "▶ अध्ययन जारी रखें" else "▶ Continue Studying") else actionTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (nextPendingSession != null) "${nextPendingSession.subject} • ${nextPendingSession.topic.ifBlank { nextPendingSession.chapter }}" else actionSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GoldenSpark,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (nextPendingSession != null) {
                                        Text(
                                            text = "${nextPendingSession.targetMinutes} min • ${nextPendingSession.startTimeFormatted.ifBlank { "Flexible" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Deadline Safety Warning Banner (if syllabus is at risk)
                if (deadlineWarning != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CoralRose.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, CoralRose.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CoralRose, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHindi) "परीक्षा समय-सीमा सुरक्षा चेतावनी" else "Exam Deadline Pace Advisory",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CoralRose
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = deadlineWarning,
                                        fontSize = 11.sp,
                                        color = Color(0xFFF1F5F9)
                                    )
                                }
                            }
                        }
                    }
                }

                // Missed Session Recovery Notification (if high priority tasks missed)
                if (missedItems.isNotEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            fillAlpha = 0.85f,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ScheduleSend, contentDescription = null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isHindi) "छूटे हुए अध्ययन सत्र" else "Missed Sessions Rescheduling",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "${missedItems.size} ${if (isHindi) "सत्र" else "Pending"}",
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
                                        Text(if (isHindi) "आज बाद में जोड़ें" else "Move Later Today", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onRecoverMissedSessions("SPREAD_WEEK") },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 6.dp)
                                    ) {
                                        Text(if (isHindi) "सप्ताह में बांटें" else "Spread Across Week", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. TODAY'S PLAN (TIMELINE)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isHindi) "आज की अध्ययन समय सारणी" else "Today's Plan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${planItems.count { it.isCompleted }}/${planItems.size} ${if (isHindi) "सत्र पूर्ण" else "Sessions Completed"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Quick Auto Plan Button
                            OutlinedButton(
                                onClick = onGenerateAiPlan,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = NeonCyan)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isHindi) "AI अनुकूलन" else "Auto Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Add Manual Task
                            IconButton(
                                onClick = { showAddCustomTaskDialog = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x3038BDF8))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (planItems.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            fillAlpha = 0.65f,
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(vertical = 28.dp, horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.EventNote,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isHindi) "आज की कोई अध्ययन योजना नहीं है" else "Create Your Study Plan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isHindi) "अपना उपलब्ध समय चुनें और NOVA आपकी तैयारी व्यवस्थित करेगा।" else "Choose your available time and NOVA will help organize your preparation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { showEditPlanDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isHindi) "योजना बनाएं" else "Create Plan", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(planItems, key = { it.id }) { item ->
                        CompactPlanTimelineRow(
                            item = item,
                            isDark = isDark,
                            onToggleCompletion = { onTogglePlanItem(item.id, !item.isCompleted) },
                            onStartTimer = {
                                onStartSessionTimer(item)
                                onStartFocusSession(item.subject, item.topic)
                            },
                            onEdit = { editingPlanItem = item },
                            onDelete = { onDeletePlanItem(item.id) }
                        )
                    }
                }

                // 6. SMART RECOMMENDATION (BEST NEXT STEP CARD)
                item {
                    val (recTitle, recSubtitle, recTarget) = bestNextStep

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.85f,
                        shape = RoundedCornerShape(18.dp),
                        borderColor = ElectricViolet.copy(alpha = 0.5f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = null,
                                        tint = ElectricViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "🎯 सर्वश्रेष्ठ अगला कदम" else "🎯 Best Next Step",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ElectricViolet.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "AI Optimized",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricViolet,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = recTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = recSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    if (recTarget.id != 0L) {
                                        onStartSessionTimer(recTarget)
                                    } else {
                                        onStartFocusSession(recTarget.subject, recTarget.topic)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isHindi) "अभी शुरू करें" else "Start Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 7. WEAK SUBJECT PRIORITY (NEEDS ATTENTION)
                if (weakExamTopics.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PriorityHigh,
                                        contentDescription = null,
                                        tint = CoralRose,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "ध्यान देने की आवश्यकता (कमजोर विषय)" else "Needs Attention",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = if (isHindi) "वास्तविक प्रदर्शन" else "Real Accuracy Data",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CoralRose
                                )
                            }

                            weakExamTopics.forEach { mastery ->
                                val accuracy = if (mastery.practiceAttempts > 0) {
                                    mastery.practiceAccuracyPercent.toInt()
                                } else {
                                    mastery.masteryScore
                                }

                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    fillAlpha = 0.75f,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${mastery.subject} • ${accuracy}% accuracy",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = CoralPink
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = mastery.topic,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Button(
                                            onClick = { onStartFocusSession(mastery.subject, mastery.topic) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CoralRose, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(if (isHindi) "अभ्यास करें" else "Study Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. TOPIC PROGRESSION PER SUBJECT
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isHindi) "विषयवार प्रगति" else "Subject & Topic Progression",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        examSubjects.forEach { subName ->
                            val subTopics = activeExamContext.topics.filter { top ->
                                val subEntity = activeExamContext.subjects.find { it.name.equals(subName, ignoreCase = true) }
                                if (subEntity != null) top.subjectId == subEntity.id else true
                            }
                            val totalTopics = if (subTopics.isNotEmpty()) subTopics.size else 12
                            val completedTopics = topicMasteries.count {
                                it.subject.equals(subName, ignoreCase = true) && (it.masteryScore >= 70 || it.masteryState == "MASTERED")
                            }.coerceAtMost(totalTopics)
                            val subProgressPercent = ((completedTopics.toFloat() / totalTopics) * 100f).toInt()

                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                fillAlpha = 0.7f,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = subName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "$completedTopics / $totalTopics topics completed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isDark) Color(0x301E293B) else Color(0x40E2E8F0))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = (subProgressPercent / 100f).coerceIn(0.05f, 1f))
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Brush.horizontalGradient(listOf(NeonCyan, ElectricViolet)))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 9. MANAGE STUDY PLAN FOOTER
                item {
                    Button(
                        onClick = { showEditPlanDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x2538BDF8), contentColor = NeonCyan),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("manage_plan_btn")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "दैनिक अध्ययन समय और विषय आवंटन प्रबंधित करें" else "Manage Daily Time & Subject Allocations",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // EXAM ROADMAP (SECTION 2)
                item {
                    val daysRemaining = remember(user?.examDateMillis) {
                        val diff = (user?.examDateMillis ?: (System.currentTimeMillis() + 60L * 86400000)) - System.currentTimeMillis()
                        (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1).toInt()
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 10.dp,
                        fillAlpha = 0.85f,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.EmojiEvents, null, tint = GoldenSpark, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = activeExamContext.examName.ifBlank { "Railway RRB NTPC" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🎯 $daysRemaining Days Remaining",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = CoralRose
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Target: Complete weekly high-yield chapter drills & full mock benchmarks.",
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
                        Triple("Phase 1: High-Weightage Chapters", "Physics & Mathematics core concept foundation", true),
                        Triple("Phase 2: Targeted PYQ Drills", "Solve 10 years of authorized previous questions", false),
                        Triple("Phase 3: Mistake Book Liquidation", "Eliminate frequent conceptual and calculation errors", false),
                        Triple("Phase 4: Full-Length Timed Mocks", "Simulated exam conditions and speed strategies", false)
                    )
                ) { (title, desc, isCurrent) ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        fillAlpha = if (isCurrent) 0.8f else 0.45f,
                        contentPadding = PaddingValues(14.dp)
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
                                    shape = RoundedCornerShape(8.dp),
                                    color = NeonCyan.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Active",
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

    // --- MODALS & DIALOGS ---

    // 1. Manage Study Plan & Time Allocation Dialog
    if (showEditPlanDialog) {
        DailyTimeAllocationDialog(
            examSubjects = examSubjects,
            initialTotalMinutes = userPreferences.dailyAvailableMinutes,
            initialStartHour = userPreferences.windowStartHour,
            initialBreakMinutes = userPreferences.breakMinutes,
            weakTopics = weakExamTopics.map { it.subject },
            isHindi = isHindi,
            onDismiss = { showEditPlanDialog = false },
            onApply = { subjectMinutes, totalMins, startHr, breakMins ->
                onApplySubjectAllocations(subjectMinutes, totalMins, startHr, breakMins)
                showEditPlanDialog = false
            }
        )
    }

    // 2. Calendar View Modal
    if (showCalendarModal) {
        CalendarScheduleModal(
            planItems = planItems,
            selectedDayOffset = selectedCalendarDayOffset,
            onSelectDayOffset = { selectedCalendarDayOffset = it },
            isHindi = isHindi,
            onDismiss = { showCalendarModal = false }
        )
    }

    // 3. Edit Single Plan Item Dialog
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
                Text(if (isHindi) "अध्ययन सत्र संपादित करें" else "Edit Study Session", color = Color.White, fontWeight = FontWeight.Bold)
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
                    Text(if (isHindi) "सहेजें" else "Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlanItem = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // 4. Add Custom Task Dialog
    if (showAddCustomTaskDialog) {
        var newSubject by remember { mutableStateOf(examSubjects.firstOrNull() ?: "Mathematics") }
        var newChapter by remember { mutableStateOf("") }
        var newTopic by remember { mutableStateOf("") }
        var newMinutes by remember { mutableIntStateOf(45) }

        AlertDialog(
            onDismissRequest = { showAddCustomTaskDialog = false },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(if (isHindi) "अध्ययन लक्ष्य जोड़ें" else "Add Study Target", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Subject Selector Chips
                    Text(if (isHindi) "विषय चुनें:" else "Select Subject:", color = Color.White, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(examSubjects) { sub ->
                            val isSel = newSubject == sub
                            FilterChip(
                                selected = isSel,
                                onClick = { newSubject = sub },
                                label = { Text(sub, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = Color(0xFF070B19),
                                    containerColor = Color(0x301E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newTopic,
                        onValueChange = { newTopic = it },
                        label = { Text("Topic / Chapter") },
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
                        Text("Duration: ${newMinutes} min", color = Color.White, fontSize = 14.sp)
                        Row {
                            IconButton(onClick = { if (newMinutes > 15) newMinutes -= 15 }) {
                                Icon(Icons.Default.RemoveCircleOutline, null, tint = NeonCyan)
                            }
                            IconButton(onClick = { newMinutes += 15 }) {
                                Icon(Icons.Default.AddCircleOutline, null, tint = NeonCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTopic.isNotBlank()) {
                            onAddPlanItem(newSubject, newChapter.ifBlank { "Core" }, newTopic, newMinutes, PlanPriority.HIGH)
                            showAddCustomTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19))
                ) {
                    Text(if (isHindi) "जोड़ें" else "Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomTaskDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

// Compact Sub-Header Tabs
@Composable
private fun PlannerSubHeader(
    selectedSection: Int,
    onSelectSection: (Int) -> Unit,
    isDark: Boolean,
    isHindi: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0x301E293B) else Color(0x60E2E8F0))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val tabs = listOf(
            if (isHindi) "टाइमटेबल" else "Study Plan",
            if (isHindi) "फ्लैशकार्ड" else "Digital Flashcards",
            if (isHindi) "रोडमैप" else "Exam Roadmap"
        )
        tabs.forEachIndexed { idx, label ->
            val isSelected = selectedSection == idx
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelectSection(idx) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color(0xFF070B19) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// Compact Plan Timeline Row
@Composable
private fun CompactPlanTimelineRow(
    item: StudyPlanItem,
    isDark: Boolean,
    onToggleCompletion: () -> Unit,
    onStartTimer: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        fillAlpha = if (item.isCompleted) 0.4f else 0.8f,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Checkbox
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggleCompletion() },
                colors = CheckboxDefaults.colors(
                    checkedColor = EmeraldSuccess,
                    checkmarkColor = Color(0xFF070B19)
                ),
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.startTimeFormatted.isNotBlank()) {
                        Text(
                            text = item.startTimeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSpark
                        )
                    }

                    Text(
                        text = item.subject,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyan
                    )

                    Text(
                        text = "• ${item.targetMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.topic.ifBlank { item.chapter },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isCompleted) Color(0xFF94A3B8) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (!item.isCompleted) {
                    IconButton(
                        onClick = onStartTimer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.PlayCircle, "Start Timer", tint = NeonCyan, modifier = Modifier.size(24.dp))
                    }
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Outlined.Edit, "Edit", tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// 9. DAILY TIME ALLOCATION & SUBJECT CUSTOMIZER DIALOG
@Composable
private fun DailyTimeAllocationDialog(
    examSubjects: List<String>,
    initialTotalMinutes: Int,
    initialStartHour: Int,
    initialBreakMinutes: Int,
    weakTopics: List<String>,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onApply: (Map<String, Int>, Int, Int, Int) -> Unit
) {
    var availableMinutes by remember { mutableIntStateOf(initialTotalMinutes) }
    var startHour by remember { mutableIntStateOf(initialStartHour) }
    var breakMinutes by remember { mutableIntStateOf(initialBreakMinutes) }
    var showAiSuggestionBanner by remember { mutableStateOf(weakTopics.isNotEmpty()) }

    // Map of minutes allocated per subject
    val allocations = remember {
        val initialMap = mutableStateMapOf<String, Int>()
        val baseShare = (availableMinutes / examSubjects.size.coerceAtLeast(1) / 15) * 15
        examSubjects.forEach { sub ->
            initialMap[sub] = baseShare.coerceAtLeast(30)
        }
        initialMap
    }

    val totalAllocatedMinutes = allocations.values.sum()
    val overage = totalAllocatedMinutes - availableMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111827),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    text = if (isHindi) "दैनिक अध्ययन समय और विषय आवंटन" else "Daily Study Time & Subject Allocations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isHindi) "प्रत्येक विषय के लिए अपना समय तय करें" else "Decide how much time each subject receives",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Total Daily Time Picker
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "कुल उपलब्ध समय:" else "Total Available Time:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "${availableMinutes / 60}h ${if (availableMinutes % 60 > 0) "${availableMinutes % 60}m" else ""}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(60 to "1h", 120 to "2h", 180 to "3h", 240 to "4h", 300 to "5h", 360 to "6h").forEach { (mins, label) ->
                            val isSel = availableMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) NeonCyan else Color(0x301E293B))
                                    .clickable { availableMinutes = mins },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color(0xFF070B19) else Color.White
                                )
                            }
                        }
                    }
                }

                // 2. AI Smart Suggestion Banner (✨ NOVA Suggests)
                if (showAiSuggestionBanner && weakTopics.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x25A855F7),
                        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "✨ NOVA Suggests",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricViolet
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi)
                                    "कमजोर विषय (${weakTopics.first()}) के लिए 15-30 मिनट अतिरिक्त देने से सटीकता में सुधार होगा।"
                                else
                                    "You may benefit from giving ${weakTopics.first()} 15-30 extra minutes today based on recent accuracy.",
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        // Apply boosted allocation to weak topic
                                        val weak = weakTopics.first()
                                        allocations[weak] = ((allocations[weak] ?: 45) + 30).coerceAtMost(120)
                                        showAiSuggestionBanner = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(if (isHindi) "सुझाव लागू करें" else "Apply Suggestion", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { showAiSuggestionBanner = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(if (isHindi) "मेरी योजना रखें" else "Keep My Plan", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                }

                // 3. Subject-Specific Time Allocations
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHindi) "विषय आवंटन:" else "Subject Allocations:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    examSubjects.forEach { sub ->
                        val currentMins = allocations[sub] ?: 45
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x201E293B))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$currentMins min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (currentMins >= 15) allocations[sub] = currentMins - 15
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = Color(0xFFCBD5E1))
                                }

                                Text(
                                    text = "${currentMins}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )

                                IconButton(
                                    onClick = {
                                        allocations[sub] = currentMins + 15
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = NeonCyan)
                                }
                            }
                        }
                    }
                }

                // 4. Time Validation Banner
                if (overage > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CoralRose.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, CoralRose.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CoralRose, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi)
                                    "आपने अपने दैनिक लक्ष्य से $overage मिनट अधिक आवंटित किए हैं।"
                                else
                                    "You have allocated $overage minutes more than your daily target.",
                                fontSize = 11.sp,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                    }
                }

                // 5. Custom Start Time & Break Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (isHindi) "शुरुआती समय" else "Start Time", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text("${if (startHour > 12) startHour - 12 else if (startHour == 0) 12 else startHour}:00 ${if (startHour >= 12) "PM" else "AM"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row {
                        IconButton(onClick = { if (startHour > 5) startHour-- }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Remove, null, tint = Color(0xFFCBD5E1))
                        }
                        IconButton(onClick = { if (startHour < 22) startHour++ }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, null, tint = Color(0xFFCBD5E1))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(if (isHindi) "ब्रेक समय" else "Break Time", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text("${breakMinutes} min", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row {
                        IconButton(onClick = { if (breakMinutes >= 5) breakMinutes -= 5 }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Remove, null, tint = Color(0xFFCBD5E1))
                        }
                        IconButton(onClick = { if (breakMinutes < 30) breakMinutes += 5 }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, null, tint = Color(0xFFCBD5E1))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(allocations.toMap(), availableMinutes, startHour, breakMinutes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isHindi) "योजना अपडेट करें" else "Update Plan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isHindi) "रद्द करें" else "Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}

// Calendar View Modal
@Composable
private fun CalendarScheduleModal(
    planItems: List<StudyPlanItem>,
    selectedDayOffset: Int,
    onSelectDayOffset: (Int) -> Unit,
    isHindi: Boolean,
    onDismiss: () -> Unit
) {
    val dayNames = listOf(
        if (isHindi) "आज" else "Today",
        if (isHindi) "कल" else "Tomorrow",
        if (isHindi) "परसों" else "+2 Days",
        if (isHindi) "+3 दिन" else "+3 Days"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131C2E),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "📅 अध्ययन कैलेंडर" else "📅 Study Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Day Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayNames.forEachIndexed { idx, label ->
                        val isSel = selectedDayOffset == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonCyan else Color(0x301E293B))
                                .clickable { onSelectDayOffset(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color(0xFF070B19) else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (planItems.isEmpty()) {
                    Text(
                        text = if (isHindi) "इस दिन के लिए कोई सत्र निर्धारित नहीं है।" else "No sessions scheduled for this day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    planItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x201E293B))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.subject, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NeonCyan)
                                Text(item.topic.ifBlank { item.chapter }, style = MaterialTheme.typography.bodySmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("${item.targetMinutes}m", style = MaterialTheme.typography.labelSmall, color = GoldenSpark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19))
            ) {
                Text(if (isHindi) "बंद करें" else "Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

package com.example.ui.screens.study

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.localization.GlobalLanguageSwitcher
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StreakBadge
import com.example.ui.components.springClickable
import com.example.ui.screens.learning.LearningDashboardScreen
import com.example.ui.screens.learning.SavedLearningScreen
import com.example.ui.screens.learning.SubjectDetailScreen
import com.example.ui.screens.learning.TopicDetailScreen
import com.example.ui.screens.planner.StudyPlannerScreen
import com.example.ui.screens.planner.StudySessionTimerView
import com.example.ui.theme.*

enum class StudySectionTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    EXAM_PREP("Exam Prep", Icons.Filled.TrackChanges),
    REVISION("Revision", Icons.Filled.Autorenew),
    RESOURCES("Resources", Icons.Filled.FolderZip),
    SUBJECTS("Subjects & Chapters", Icons.AutoMirrored.Filled.MenuBook),
    NOTES("Notes & Flashcards", Icons.Filled.EditNote),
    TOOLS("Study Tools & Plan", Icons.Filled.Psychology)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyHubScreen(
    user: UserProfile?,
    examContext: ExamContext,
    mainViewModel: com.example.viewmodel.MainViewModel? = null,
    onOpenRevisionHub: () -> Unit = {},
    subjectSummaries: List<SubjectProgressSummary> = emptyList(),
    allMasteries: List<TopicMastery> = emptyList(),
    bookmarks: List<UserLearningBookmark> = emptyList(),
    smartNotes: List<SmartNoteItem> = emptyList(),
    studyPlan: List<StudyPlanItem> = emptyList(),
    flashcards: List<FlashcardItem> = emptyList(),
    isPlanGenerating: Boolean = false,
    activeLearningContent: LearningTopicContent? = null,
    isLearningContentLoading: Boolean = false,
    novaDoubtResponse: String? = null,
    isNovaDoubtThinking: Boolean = false,
    deadlineWarning: String? = null,
    userPreferences: UserStudyPreferences = UserStudyPreferences(),
    activeStudySession: StudyPlanItem? = null,
    sessionRemainingSeconds: Int = 0,
    isSessionTimerRunning: Boolean = false,
    isSessionPaused: Boolean = false,
    activeSessionActualMinutes: Int = 0,
    isFlashcardGenerating: Boolean = false,
    flashcardMessage: String? = null,
    allFocusSessions: List<FocusSession> = emptyList(),
    mistakes: List<MistakeItem> = emptyList(),
    // Step 57 Exam Preparation Integration
    examPrepSummary: ExamPreparationSummary? = null,
    allExamGoals: List<ExamGoalEntity> = emptyList(),
    dailyPlanPreview: DailyStudyPlanPreview? = null,
    onSelectExamGoal: (String) -> Unit = {},
    onCreateExamGoal: (name: String, org: String, dateMillis: Long?, target: String, priority: String) -> Unit = { _, _, _, _, _ -> },
    onUpdateTopicStatus: (topicId: String, status: String) -> Unit = { _, _ -> },
    onAddCustomTopic: (examId: String, subject: String, topic: String) -> Unit = { _, _, _ -> },
    onGenerateDailyPlan: (examId: String) -> Unit = {},
    onConfirmDailyPlan: (DailyStudyPlanPreview) -> Unit = {},
    // Navigation / Action Callbacks
    onSelectTopicLearning: (subject: String, chapter: String, topic: String) -> Unit = { _, _, _ -> },
    onBackToSubjects: () -> Unit = {},
    onGenerateAiPlan: () -> Unit = {},
    onTogglePlanItem: (Long, Boolean) -> Unit = { _, _ -> },
    onAddPlanItem: (String, String, String, Int, PlanPriority) -> Unit = { _, _, _, _, _ -> },
    onUpdatePlanItem: (StudyPlanItem) -> Unit = {},
    onDeletePlanItem: (Long) -> Unit = {},
    onStartFocusSession: (String, String) -> Unit = { _, _ -> },
    onStartSessionTimer: (StudyPlanItem) -> Unit = {},
    onPauseSessionTimer: () -> Unit = {},
    onResumeSessionTimer: () -> Unit = {},
    onFinishSessionTimer: (String) -> Unit = {},
    onCancelSessionTimer: () -> Unit = {},
    onAddFlashcard: (subject: String, topic: String, front: String, back: String, hint: String, difficulty: String, sourceDoc: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onUpdateFlashcard: (FlashcardItem) -> Unit = {},
    onDeleteFlashcard: (Long) -> Unit = {},
    onReviewFlashcard: (Long, RevisionCategory, Int) -> Unit = { _, _, _ -> },
    onReviewSpaced: (Long, Int) -> Unit = { _, _ -> },
    onGenerateAiCards: (String, String) -> Unit = { _, _ -> },
    onGenerateFromNotes: (String, String, String, Int) -> Unit = { _, _, _, _ -> },
    onGenerateFromDocumentUri: (android.net.Uri, String, Int) -> Unit = { _, _, _ -> },
    onClearFlashcardMessage: () -> Unit = {},
    onSaveTopicNote: (String, String, String) -> Unit = { _, _, _ -> },
    onToggleLearningBookmark: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onCompleteQuickTest: (String, String, Int, Int) -> Unit = { _, _, _, _ -> },
    onAskNovaDoubt: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onSpeakTts: (String) -> Unit = {},
    onDeleteBookmark: (Long) -> Unit = {},
    onDeleteNote: (Long) -> Unit = {},
    onOpenDocumentSummarizer: () -> Unit = {},
    onOpenSearch: (String) -> Unit = {},
    onAskNovaGlobal: (String) -> Unit = {},
    onChangeExam: () -> Unit = {},
    onRecoverMissedSessions: (String) -> Unit = {},
    onUpdateDailyAvailableTime: (Int) -> Unit = {},
    onSavePreferences: (UserStudyPreferences) -> Unit = {},
    onApplySubjectAllocations: (Map<String, Int>, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    // Step 58 Resource Engine Integration
    resourcesList: List<com.example.data.model.ResourceSearchResult> = emptyList(),
    resourceQuery: String = "",
    selectedResourceTypeFilter: String = com.example.data.model.ResourceType.ALL.name,
    onSearchResourceQueryChange: (String) -> Unit = {},
    onSelectResourceTypeFilter: (String) -> Unit = {},
    onOpenResourceDetail: (com.example.data.model.StudyResourceEntity) -> Unit = {},
    onToggleSaveResourceItem: (String) -> Unit = {},
    onStartFocusFromResourceItem: (subject: String, topic: String, resourceId: String) -> Unit = { _, _, _ -> },
    onUploadResourceItem: (title: String, desc: String, exam: String, subject: String, topic: String, content: String) -> Unit = { _, _, _, _, _, _ -> },
    initialSectionTab: StudySectionTab = StudySectionTab.SUBJECTS,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var selectedSection by rememberSaveable { mutableStateOf(initialSectionTab.name) }
    val currentSection = runCatching { StudySectionTab.valueOf(selectedSection) }.getOrDefault(StudySectionTab.SUBJECTS)

    // Drill down states for subjects/topics
    var selectedSubject by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedChapter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTopic by rememberSaveable { mutableStateOf<String?>(null) }
    var showSavedLearningSheet by rememberSaveable { mutableStateOf(false) }

    // Active Study Session Timer View takes priority when studying
    if (activeStudySession != null) {
        StudySessionTimerView(
            activeSession = activeStudySession,
            remainingSeconds = sessionRemainingSeconds,
            isTimerRunning = isSessionTimerRunning,
            isPaused = isSessionPaused,
            actualMinutesSpent = activeSessionActualMinutes,
            onPauseTimer = onPauseSessionTimer,
            onResumeTimer = onResumeSessionTimer,
            onFinishSession = onFinishSessionTimer,
            onCancelSession = onCancelSessionTimer
        )
        return
    }

    // Drill-down to Topic Detail Screen
    if (selectedTopic != null) {
        val sub = selectedSubject ?: "General"
        val chap = selectedChapter ?: "General Chapter"
        val top = selectedTopic!!

        LaunchedEffect(sub, chap, top) {
            onSelectTopicLearning(sub, chap, top)
        }

        TopicDetailScreen(
            examContext = examContext,
            subject = sub,
            chapter = chap,
            topic = top,
            topicMastery = allMasteries.firstOrNull { it.topic.equals(top, ignoreCase = true) },
            learningContent = activeLearningContent,
            isLoading = isLearningContentLoading,
            userNotes = smartNotes.firstOrNull { it.topic.equals(top, ignoreCase = true) }?.contentMarkdown ?: "",
            userMistakes = mistakes.filter { it.topic.equals(top, ignoreCase = true) },
            onBack = { selectedTopic = null },
            onRefreshContent = { onSelectTopicLearning(sub, chap, top) },
            onSaveNote = { noteText -> onSaveTopicNote(sub, top, noteText) },
            onToggleBookmark = { title, snippet, type -> onToggleLearningBookmark(sub, top, title, snippet, type) },
            onCompleteQuickTest = { score, total -> onCompleteQuickTest(sub, top, score, total) },
            onAskNovaDoubt = { prompt -> onAskNovaDoubt(sub, chap, top, prompt) },
            novaDoubtResponse = novaDoubtResponse ?: "",
            isNovaThinking = isNovaDoubtThinking,
            onSpeakTts = onSpeakTts
        )
        return
    }

    // Drill-down to Subject Detail Screen
    if (selectedSubject != null) {
        SubjectDetailScreen(
            examContext = examContext,
            subjectName = selectedSubject!!,
            subjectSummary = subjectSummaries.firstOrNull { it.subjectName.equals(selectedSubject!!, ignoreCase = true) },
            topicMasteries = allMasteries.filter { it.subject.equals(selectedSubject!!, ignoreCase = true) },
            onBack = { selectedSubject = null },
            onSelectTopic = { chapName, topName ->
                selectedChapter = chapName
                selectedTopic = topName
            }
        )
        return
    }

    // Saved Learning / Bookmarks / Notes Screen
    if (showSavedLearningSheet) {
        SavedLearningScreen(
            bookmarks = bookmarks,
            smartNotes = smartNotes,
            onOpenTopic = { sub, top ->
                showSavedLearningSheet = false
                selectedSubject = sub
                selectedChapter = "General Chapter"
                selectedTopic = top
            },
            onDeleteBookmark = onDeleteBookmark,
            onDeleteNote = onDeleteNote,
            onBack = { showSavedLearningSheet = false }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundGradient(isDark))
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Study Hub",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else DeepIndigo
                        )
                        Text(
                            text = "${user?.examName?.ifBlank { examContext.examName } ?: "RRB NTPC"} • Comprehensive Learning",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlobalLanguageSwitcher(
                            modifier = Modifier.testTag("study_language_switcher")
                        )

                        IconButton(
                            onClick = { showSavedLearningSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                                .testTag("study_saved_notes_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BookmarkBorder,
                                contentDescription = "Saved Notes & Bookmarks",
                                tint = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenDocumentSummarizer,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                                .testTag("study_document_summarizer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = "Document Summarizer",
                                tint = if (isDark) GoldenSpark else Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary 3 Segmented Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudySectionTab.entries.forEach { tab ->
                        val isSelected = currentSection == tab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .springClickable(testTag = "study_tab_${tab.name.lowercase()}") {
                                    selectedSection = tab.name
                                },
                            color = if (isSelected) {
                                if (isDark) NeonCyan else DeepIndigo
                            } else {
                                if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        if (isDark) Color(0xFF070B19) else Color.White
                                    } else {
                                        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title.split(" ").first(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFF070B19) else Color.White
                                    } else {
                                        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentSection) {
                StudySectionTab.EXAM_PREP -> {
                    com.example.ui.screens.examprep.ExamPreparationDashboardScreen(
                        summary = examPrepSummary,
                        allGoals = allExamGoals,
                        dailyPlanPreview = dailyPlanPreview,
                        onSelectExam = onSelectExamGoal,
                        onCreateExamGoal = onCreateExamGoal,
                        onUpdateTopicStatus = onUpdateTopicStatus,
                        onAddCustomTopic = onAddCustomTopic,
                        onGenerateDailyPlan = onGenerateDailyPlan,
                        onConfirmDailyPlan = onConfirmDailyPlan,
                        onStartTopicFocus = { sub, top, _ -> onStartFocusSession(sub, top) },
                        onBack = { selectedSection = StudySectionTab.SUBJECTS.name }
                    )
                }

                StudySectionTab.REVISION -> {
                    if (mainViewModel != null) {
                        com.example.ui.screens.revision.RevisionHubScreen(
                            mainViewModel = mainViewModel,
                            onBack = { selectedSection = StudySectionTab.SUBJECTS.name },
                            onStartFocus = { sub, top, _ -> onStartFocusSession(sub, top) },
                            onStartPractice = { _, _, _ -> },
                            onOpenResources = { _ -> selectedSection = StudySectionTab.RESOURCES.name }
                        )
                    }
                }

                StudySectionTab.RESOURCES -> {
                    com.example.ui.screens.resources.StudyResourceHubScreen(
                        resources = resourcesList,
                        activeExam = examContext.examName,
                        activeSubject = examContext.subjects.firstOrNull()?.name ?: "",
                        activeTopic = examContext.topics.firstOrNull()?.name ?: "",
                        searchQuery = resourceQuery,
                        selectedResourceType = selectedResourceTypeFilter,
                        onSearchQueryChange = onSearchResourceQueryChange,
                        onSelectResourceType = onSelectResourceTypeFilter,
                        onOpenResource = onOpenResourceDetail,
                        onToggleSave = onToggleSaveResourceItem,
                        onStartFocusFromResource = onStartFocusFromResourceItem,
                        onUploadCustomResource = onUploadResourceItem,
                        onBack = { selectedSection = StudySectionTab.SUBJECTS.name }
                    )
                }

                StudySectionTab.SUBJECTS -> {
                    LearningDashboardScreen(
                        user = user,
                        examContext = examContext,
                        subjectSummaries = subjectSummaries,
                        allMasteries = allMasteries,
                        bookmarks = bookmarks,
                        smartNotes = smartNotes,
                        onSelectSubject = { selectedSubject = it },
                        onSelectTopic = { sub, chap, top ->
                            selectedSubject = sub
                            selectedChapter = chap
                            selectedTopic = top
                        },
                        onOpenSearch = onOpenSearch,
                        onChangeExam = onChangeExam,
                        onOpenSavedLearning = { showSavedLearningSheet = true },
                        onAskNova = { onAskNovaGlobal("Explain ${examContext.examName} syllabus") }
                    )
                }

                StudySectionTab.NOTES -> {
                    SavedLearningScreen(
                        bookmarks = bookmarks,
                        smartNotes = smartNotes,
                        onOpenTopic = { sub, top ->
                            selectedSubject = sub
                            selectedChapter = "General Chapter"
                            selectedTopic = top
                        },
                        onDeleteBookmark = onDeleteBookmark,
                        onDeleteNote = onDeleteNote,
                        onBack = { selectedSection = StudySectionTab.SUBJECTS.name }
                    )
                }

                StudySectionTab.TOOLS -> {
                    StudyPlannerScreen(
                        planItems = studyPlan,
                        flashcards = flashcards,
                        user = user,
                        isGenerating = isPlanGenerating,
                        onGenerateAiPlan = onGenerateAiPlan,
                        onTogglePlanItem = onTogglePlanItem,
                        onAddPlanItem = onAddPlanItem,
                        onUpdatePlanItem = onUpdatePlanItem,
                        onDeletePlanItem = onDeletePlanItem,
                        onStartFocusSession = onStartFocusSession,
                        onRecoverMissedSessions = onRecoverMissedSessions,
                        onUpdateDailyAvailableTime = onUpdateDailyAvailableTime,
                        onStartSessionTimer = onStartSessionTimer,
                        deadlineWarning = deadlineWarning,
                        userPreferences = userPreferences,
                        onSavePreferences = onSavePreferences,
                        activeExamContext = examContext,
                        topicMasteries = allMasteries,
                        focusSessions = allFocusSessions,
                        onApplySubjectAllocations = onApplySubjectAllocations,
                        onOpenExamSelector = onChangeExam,
                        activeStudySession = activeStudySession,
                        sessionRemainingSeconds = sessionRemainingSeconds,
                        isSessionTimerRunning = isSessionTimerRunning,
                        isSessionPaused = isSessionPaused,
                        activeSessionActualMinutes = activeSessionActualMinutes,
                        onPauseTimer = onPauseSessionTimer,
                        onResumeTimer = onResumeSessionTimer,
                        onFinishSession = onFinishSessionTimer,
                        onCancelSession = onCancelSessionTimer,
                        onAddFlashcard = onAddFlashcard,
                        onUpdateFlashcard = onUpdateFlashcard,
                        onDeleteFlashcard = onDeleteFlashcard,
                        onReviewFlashcard = onReviewFlashcard,
                        onReviewSpaced = onReviewSpaced,
                        onGenerateAiCards = onGenerateAiCards,
                        onGenerateFromNotes = onGenerateFromNotes,
                        onGenerateFromDocumentUri = onGenerateFromDocumentUri,
                        flashcardStatusMessage = flashcardMessage,
                        onClearFlashcardStatusMessage = onClearFlashcardMessage,
                        isFlashcardGenerating = isFlashcardGenerating
                    )
                }
            }
        }
    }
}

package com.example.ui.screens.examprep

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPreparationDashboardScreen(
    summary: ExamPreparationSummary?,
    allGoals: List<ExamGoalEntity>,
    dailyPlanPreview: DailyStudyPlanPreview?,
    onSelectExam: (String) -> Unit,
    onCreateExamGoal: (name: String, org: String, dateMillis: Long?, target: String, priority: String) -> Unit,
    onUpdateTopicStatus: (topicId: String, status: String) -> Unit,
    onAddCustomTopic: (examId: String, subject: String, topic: String) -> Unit,
    onGenerateDailyPlan: (examId: String) -> Unit,
    onConfirmDailyPlan: (DailyStudyPlanPreview) -> Unit,
    onStartTopicFocus: (subject: String, topic: String, minutes: Int) -> Unit,
    onBack: () -> Unit
) {
    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf<String?>(null) } // subjectName
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview, 1: Syllabus, 2: Daily Plan, 3: Revision Queue

    val activeGoal = summary?.examGoal ?: allGoals.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Exam Preparation & Planner",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeGoal != null) {
                            Text(
                                text = activeGoal.examName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("exam_prep_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreateGoalDialog = true },
                        modifier = Modifier.testTag("add_exam_goal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Add Goal"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Exam Switcher Horizontal Row
            if (allGoals.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allGoals) { goal ->
                        val isSelected = goal.examId == activeGoal?.examId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectExam(goal.examId) },
                            label = {
                                Text(
                                    text = goal.examName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (goal.priority == "PRIMARY") Icons.Default.Star else Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = if (goal.priority == "PRIMARY") Color(0xFFFFB800) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("exam_chip_${goal.examId}")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showCreateGoalDialog = true },
                            label = { Text("+ New Goal") }
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Active Exam Goal Set",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Set an exam target like SSC CGL or Railway to unlock countdowns and structured study plans.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showCreateGoalDialog = true },
                            modifier = Modifier.testTag("create_first_goal_button")
                        ) {
                            Text("Create Exam Goal")
                        }
                    }
                }
            }

            if (activeGoal != null) {
                // Secondary Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Syllabus (${summary?.syllabusCoveragePercentage ?: 0}%)") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Daily Plan") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Revision (${summary?.pendingRevisionTopicsCount ?: 0})") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // OVERVIEW TAB
                            item {
                                ExamHeaderCountdownCard(summary = summary, goal = activeGoal)
                            }
                            item {
                                OverallSyllabusCard(summary = summary)
                            }
                            item {
                                SubjectOverviewGridCard(
                                    summary = summary,
                                    onViewSyllabus = { selectedTab = 1 }
                                )
                            }
                            item {
                                QuickActionPlannerCard(
                                    onGeneratePlan = { onGenerateDailyPlan(activeGoal.examId) },
                                    onOpenRevision = { selectedTab = 3 }
                                )
                            }
                        }
                        1 -> {
                            // SYLLABUS TAB
                            if (summary != null) {
                                items(summary.subjectProgressList) { subjectProgress ->
                                    SubjectSyllabusDetailCard(
                                        subjectProgress = subjectProgress,
                                        examId = activeGoal.examId,
                                        onUpdateTopicStatus = onUpdateTopicStatus,
                                        onAddTopicClick = { showAddTopicDialog = subjectProgress.subjectName },
                                        onStartFocus = onStartTopicFocus
                                    )
                                }
                            }
                        }
                        2 -> {
                            // DAILY PLAN TAB
                            item {
                                DailyPlanGeneratorCard(
                                    dailyPlanPreview = dailyPlanPreview,
                                    onGeneratePlan = { onGenerateDailyPlan(activeGoal.examId) },
                                    onConfirmPlan = onConfirmDailyPlan,
                                    onStartFocus = onStartTopicFocus
                                )
                            }
                        }
                        3 -> {
                            // REVISION QUEUE TAB
                            item {
                                RevisionQueueCard(
                                    summary = summary,
                                    onStartFocus = onStartTopicFocus,
                                    onUpdateTopicStatus = onUpdateTopicStatus
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Dialog: Create Exam Goal
    if (showCreateGoalDialog) {
        CreateExamGoalDialog(
            onDismiss = { showCreateGoalDialog = false },
            onCreateGoal = { name, org, date, target, priority ->
                onCreateExamGoal(name, org, date, target, priority)
                showCreateGoalDialog = false
            }
        )
    }

    // Dialog: Add Custom Topic
    if (showAddTopicDialog != null && activeGoal != null) {
        AddCustomTopicDialog(
            subjectName = showAddTopicDialog!!,
            onDismiss = { showAddTopicDialog = null },
            onAddTopic = { topicName ->
                onAddCustomTopic(activeGoal.examId, showAddTopicDialog!!, topicName)
                showAddTopicDialog = null
            }
        )
    }
}

@Composable
fun ExamHeaderCountdownCard(
    summary: ExamPreparationSummary?,
    goal: ExamGoalEntity
) {
    val daysRem = summary?.daysRemaining
    val isDateKnown = goal.isExamDateKnown && goal.examDateMillis != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_header_countdown_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = goal.priority,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (goal.organization.isNotBlank()) {
                        Text(
                            text = goal.organization,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.examName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Target: ${goal.target}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isDateKnown && daysRem != null && daysRem >= 0) {
                        Text(
                            text = "$daysRem",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Days Left",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else if (daysRem != null && daysRem < 0) {
                        Text(
                            text = "PASSED",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Date N/A",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Not Available",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OverallSyllabusCard(summary: ExamPreparationSummary?) {
    val coveragePct = summary?.syllabusCoveragePercentage ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overall_syllabus_card"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Syllabus Coverage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "$coveragePct%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (coveragePct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Completed: ${summary?.completedTopicsCount ?: 0}/${summary?.totalTopicsCount ?: 0} topics",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Formula: Completed / Total × 100",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SubjectOverviewGridCard(
    summary: ExamPreparationSummary?,
    onViewSyllabus: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_overview_card"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject Progress",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                TextButton(onClick = onViewSyllabus) {
                    Text("Full Syllabus")
                }
            }

            summary?.subjectProgressList?.forEach { subject ->
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = subject.subjectName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${subject.coveragePercentage}% covered",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (subject.coveragePercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionPlannerCard(
    onGeneratePlan: () -> Unit,
    onOpenRevision: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onGeneratePlan,
                modifier = Modifier
                    .weight(1f)
                    .testTag("generate_today_plan_button")
            ) {
                Icon(imageVector = Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate Daily Plan", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onOpenRevision,
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_revision_queue_button")
            ) {
                Icon(imageVector = Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Revision Queue", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SubjectSyllabusDetailCard(
    subjectProgress: SubjectSyllabusProgress,
    examId: String,
    onUpdateTopicStatus: (topicId: String, status: String) -> Unit,
    onAddTopicClick: () -> Unit,
    onStartFocus: (subject: String, topic: String, minutes: Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_detail_${subjectProgress.subjectName}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subjectProgress.subjectName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${subjectProgress.completedTopicsCount} of ${subjectProgress.totalTopicsCount} topics completed (${subjectProgress.coveragePercentage}%)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Subject"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (subjectProgress.coveragePercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onAddTopicClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Custom Topic")
                    }
                }
            }
        }
    }
}

@Composable
fun DailyPlanGeneratorCard(
    dailyPlanPreview: DailyStudyPlanPreview?,
    onGeneratePlan: () -> Unit,
    onConfirmPlan: (DailyStudyPlanPreview) -> Unit,
    onStartFocus: (subject: String, topic: String, minutes: Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_plan_generator_card"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Smart Daily Study Planner",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Generates a balanced schedule matching your available daily study time without overwriting existing commitments.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (dailyPlanPreview == null) {
                Button(
                    onClick = onGeneratePlan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_plan_preview_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Today's Plan Preview")
                }
            } else {
                Text(
                    text = "Proposed Plan (${dailyPlanPreview.plannedItems.size} Sessions)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                dailyPlanPreview.plannedItems.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.startTimeFormatted} - ${item.endTimeFormatted}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${item.subjectName} — ${item.topicName}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${item.targetMinutes} min (${item.sessionType})",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onStartFocus(item.subjectName, item.topicName, item.targetMinutes) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = "Start Focus",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (dailyPlanPreview.scheduleConflicts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⚠️ Schedule Conflict Warning",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp
                            )
                            Text(
                                text = dailyPlanPreview.scheduleConflicts.first().message,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onConfirmPlan(dailyPlanPreview) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_and_schedule_plan_button")
                    ) {
                        Text("Confirm & Add to Schedule")
                    }
                    OutlinedButton(
                        onClick = onGeneratePlan,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Regenerate")
                    }
                }
            }
        }
    }
}

@Composable
fun RevisionQueueCard(
    summary: ExamPreparationSummary?,
    onStartFocus: (subject: String, topic: String, minutes: Int) -> Unit,
    onUpdateTopicStatus: (topicId: String, status: String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("revision_queue_card"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Revision Queue",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Topics marked for spaced revision or review requirement.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if ((summary?.pendingRevisionTopicsCount ?: 0) == 0) {
                Text(
                    text = "✨ All completed topics are currently up to date!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "${summary?.pendingRevisionTopicsCount} topics pending revision",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CreateExamGoalDialog(
    onDismiss: () -> Unit,
    onCreateGoal: (name: String, org: String, dateMillis: Long?, target: String, priority: String) -> Unit
) {
    var examName by remember { mutableStateOf("SSC CGL") }
    var organization by remember { mutableStateOf("SSC") }
    var target by remember { mutableStateOf("Top Rank / 85+ Marks") }
    var priority by remember { mutableStateOf("PRIMARY") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Exam Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = examName,
                    onValueChange = { examName = it },
                    label = { Text("Exam Name") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_exam_name_input")
                )
                OutlinedTextField(
                    value = organization,
                    onValueChange = { organization = it },
                    label = { Text("Organization / Board") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_exam_org_input")
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Goal") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_exam_target_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (examName.isNotBlank()) {
                        onCreateGoal(
                            examName,
                            organization,
                            System.currentTimeMillis() + (120L * 24 * 60 * 60 * 1000), // Default 120 days
                            target,
                            priority
                        )
                    }
                },
                modifier = Modifier.testTag("dialog_confirm_create_goal")
            ) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddCustomTopicDialog(
    subjectName: String,
    onDismiss: () -> Unit,
    onAddTopic: (topicName: String) -> Unit
) {
    var topicName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Topic to $subjectName") },
        text = {
            OutlinedTextField(
                value = topicName,
                onValueChange = { topicName = it },
                label = { Text("Topic Name") },
                modifier = Modifier.fillMaxWidth().testTag("dialog_topic_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (topicName.isNotBlank()) {
                        onAddTopic(topicName)
                    }
                },
                modifier = Modifier.testTag("dialog_confirm_add_topic")
            ) {
                Text("Add Topic")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

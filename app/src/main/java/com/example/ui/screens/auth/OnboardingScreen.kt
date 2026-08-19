package com.example.ui.screens.auth

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.StudyTimeBlock
import com.example.data.model.UserProfile
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StudyMateBrandLogo
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OnboardingScreen(
    initialName: String = "Student",
    onComplete: (userProfile: UserProfile) -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("studymate_onboarding_draft", Context.MODE_PRIVATE)
    }

    var step by remember {
        mutableIntStateOf(sharedPrefs.getInt("draft_step", 1).coerceIn(1, 5))
    }
    val totalSteps = 5

    // Hardware & UI Back Navigation Handler
    BackHandler(enabled = step > 1) {
        if (step > 1) {
            step--
            sharedPrefs.edit().putInt("draft_step", step).apply()
        }
    }

    // ==========================================
    // STEP 1 STATE - Profile & Language
    // ==========================================
    var nameInput by remember {
        mutableStateOf(sharedPrefs.getString("draft_name", if (initialName.isNotBlank() && initialName != "Student") initialName else "") ?: "")
    }
    var selectedAvatarIndex by remember {
        mutableIntStateOf(sharedPrefs.getInt("draft_avatar_idx", 0))
    }
    var selectedGrade by remember {
        mutableStateOf(sharedPrefs.getString("draft_grade", "Class 12 / Senior Secondary") ?: "Class 12 / Senior Secondary")
    }
    var selectedLanguage by remember {
        mutableStateOf(sharedPrefs.getString("draft_language", "English") ?: "English")
    }

    val presetAvatars = listOf(
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar1",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar2",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar3",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar4",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar5",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar6"
    )

    val gradeLevels = listOf(
        "Class 10 / Secondary",
        "Class 11 / Junior High",
        "Class 12 / Senior Secondary",
        "College / Undergraduate",
        "Graduate / Full-Time Aspirant",
        "Working Professional"
    )

    val supportedLanguages = listOf("English", "Hindi", "Hinglish")

    // ==========================================
    // STEP 2 STATE - Target Exam & Goals
    // ==========================================
    val examCatalogList = listOf(
        "RRB NTPC (Railway Non-Technical)",
        "RRB Group D (Railway Recruitment)",
        "SSC CGL (Staff Selection Commission)",
        "JEE Main & Advanced (Engineering)",
        "NEET UG (Medical Entrance)",
        "UPSC Civil Services (IAS / IPS)",
        "IBPS / SBI Banking PO & Clerk",
        "CBSE Class 12 Board Exam",
        "Custom Exam"
    )

    var selectedExamPreset by remember {
        mutableStateOf(sharedPrefs.getString("draft_exam_preset", "RRB NTPC (Railway Non-Technical)") ?: "RRB NTPC (Railway Non-Technical)")
    }
    var customExamName by remember {
        mutableStateOf(sharedPrefs.getString("draft_custom_exam", "") ?: "")
    }
    var examDaysAhead by remember {
        mutableFloatStateOf(sharedPrefs.getFloat("draft_exam_days", 60f))
    }
    var targetScoreInput by remember {
        mutableStateOf(sharedPrefs.getString("draft_target_score", "Top Merit / 90+ Score") ?: "Top Merit / 90+ Score")
    }

    fun getExamPatternInfo(preset: String): String = when {
        preset.contains("NTPC", ignoreCase = true) -> "CBT-1: General Awareness (40), Mathematics (30), Reasoning (30) = 100 Marks (90 mins)"
        preset.contains("Group D", ignoreCase = true) -> "CBT: General Science (25), Mathematics (25), Reasoning (30), GA (20) = 100 Marks (90 mins)"
        preset.contains("SSC", ignoreCase = true) -> "Tier 1: Quant (50), Reasoning (50), English (50), GA (50) = 200 Marks (60 mins)"
        preset.contains("JEE", ignoreCase = true) -> "Physics (100), Chemistry (100), Mathematics (100) = 300 Marks (180 mins)"
        preset.contains("NEET", ignoreCase = true) -> "Biology (360), Physics (180), Chemistry (180) = 720 Marks (200 mins)"
        preset.contains("UPSC", ignoreCase = true) -> "Prelims: General Studies Paper 1 (200 Marks) & CSAT Paper 2 (200 Marks)"
        preset.contains("Bank", ignoreCase = true) || preset.contains("IBPS", ignoreCase = true) -> "Prelims: Reasoning (35), Quant (35), English (30) = 100 Marks (60 mins)"
        preset.contains("CBSE", ignoreCase = true) -> "Subject-wise Theory (70/80 Marks) + Practical/Internal (30/20 Marks)"
        else -> "Comprehensive syllabus & structured topic drills"
    }

    // ==========================================
    // STEP 3 STATE - Exam-Specific Subjects & Priorities
    // ==========================================
    fun getOfficialExamSubjects(preset: String): List<String> = when {
        preset.contains("NTPC", ignoreCase = true) -> listOf("Mathematics", "General Intelligence & Reasoning", "General Awareness", "General Science")
        preset.contains("Group D", ignoreCase = true) -> listOf("General Science", "Mathematics", "General Intelligence & Reasoning", "General Awareness & Current Affairs")
        preset.contains("SSC", ignoreCase = true) -> listOf("Quantitative Aptitude", "General Intelligence & Reasoning", "English Comprehension", "General Awareness")
        preset.contains("JEE", ignoreCase = true) -> listOf("Physics", "Chemistry", "Mathematics")
        preset.contains("NEET", ignoreCase = true) -> listOf("Biology (Botany & Zoology)", "Physics", "Chemistry")
        preset.contains("UPSC", ignoreCase = true) -> listOf("General Studies I (History & Geography)", "General Studies II (Polity & Governance)", "General Studies III (Economy & Env)", "CSAT (Aptitude & Reasoning)")
        preset.contains("Bank", ignoreCase = true) || preset.contains("IBPS", ignoreCase = true) -> listOf("Reasoning Ability", "Quantitative Aptitude & Data Interpretation", "English Language", "Banking & General Awareness")
        preset.contains("CBSE", ignoreCase = true) -> listOf("Physics", "Chemistry", "Mathematics", "Biology", "English Core")
        else -> listOf("Subject 1", "Subject 2", "Subject 3")
    }

    val officialAvailableSubjects = remember(selectedExamPreset) {
        getOfficialExamSubjects(selectedExamPreset)
    }

    var selectedSubjects by remember(selectedExamPreset) {
        mutableStateOf(officialAvailableSubjects.toSet())
    }

    var customSubjectInput by remember { mutableStateOf("") }

    // User Priority Map: Subject -> "HIGH" | "MEDIUM" | "LOW"
    var subjectPriorityMap by remember(selectedExamPreset) {
        mutableStateOf(officialAvailableSubjects.associateWith { "HIGH" })
    }

    val prepLevels = listOf(
        "Beginner (Starting fresh / foundational concepts)",
        "Intermediate (Practicing questions & mock drills)",
        "Advanced (Speed drills & rapid high-yield revision)"
    )
    var selectedPrepLevel by remember { mutableStateOf("Intermediate (Practicing questions & mock drills)") }

    // ==========================================
    // STEP 4 STATE - Daily Study Time & Subject-Wise Allocation
    // ==========================================
    var dailyStudyHours by remember {
        mutableFloatStateOf(sharedPrefs.getFloat("draft_daily_hours", 4.0f))
    }
    val dailyTimePresets = listOf(0.5f to "30m", 1.0f to "1h", 2.0f to "2h", 3.0f to "3h", 4.0f to "4h", 5.0f to "5h", 6.0f to "6h+")
    var isCustomDailyTime by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedStudyDays by remember {
        mutableStateOf(setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat"))
    }

    // Subject Time Allocation in Minutes: Subject -> Minutes
    var subjectAllocationMinutes by remember(selectedSubjects, dailyStudyHours) {
        val totalMins = (dailyStudyHours * 60).toInt()
        val subs = selectedSubjects.toList()
        if (subs.isEmpty()) {
            mutableStateOf(emptyMap<String, Int>())
        } else {
            val perSub = (totalMins / subs.size).coerceAtLeast(15)
            val map = subs.associateWith { perSub }.toMutableMap()
            // Adjust remainder to first subject
            val allocatedSum = map.values.sum()
            if (allocatedSum < totalMins && subs.isNotEmpty()) {
                map[subs.first()] = (map[subs.first()] ?: perSub) + (totalMins - allocatedSum)
            }
            mutableStateOf(map.toMap())
        }
    }

    val totalAllocatedMinutes = subjectAllocationMinutes.values.sum()
    val availableMinutes = (dailyStudyHours * 60).toInt()
    val isOverAllocated = totalAllocatedMinutes > availableMinutes

    // Auto-Balance Allocation Helper
    fun performAutoBalance() {
        val subs = selectedSubjects.toList()
        if (subs.isEmpty() || availableMinutes <= 0) return

        val weights = subs.associateWith { sub ->
            when (subjectPriorityMap[sub]) {
                "HIGH" -> 3
                "LOW" -> 1
                else -> 2
            }
        }
        val totalWeight = weights.values.sum().coerceAtLeast(1)
        val newAlloc = mutableMapOf<String, Int>()
        var allocatedSoFar = 0

        subs.forEachIndexed { index, sub ->
            if (index == subs.lastIndex) {
                newAlloc[sub] = (availableMinutes - allocatedSoFar).coerceAtLeast(15)
            } else {
                val weight = weights[sub] ?: 2
                val rawMins = (availableMinutes * weight.toDouble() / totalWeight).toInt()
                val rounded = ((rawMins + 7) / 15) * 15
                val safe = rounded.coerceIn(15, (availableMinutes - allocatedSoFar - 15 * (subs.size - index - 1)).coerceAtLeast(15))
                newAlloc[sub] = safe
                allocatedSoFar += safe
            }
        }
        subjectAllocationMinutes = newAlloc
    }

    // ==========================================
    // STEP 5 STATE - Schedule Timing, Session Length, Breaks & AI Plan Suggestion
    // ==========================================
    val timingPresets = listOf(
        "Morning (06:00 AM – 10:00 AM)",
        "Afternoon (01:00 PM – 05:00 PM)",
        "Evening (06:00 PM – 10:00 PM)",
        "Night (09:00 PM – 01:00 AM)",
        "Set my own time (Custom Blocks)"
    )
    var selectedTimingPreset by remember { mutableStateOf("Evening (06:00 PM – 10:00 PM)") }

    // Custom Study Blocks:
    var customStudyBlocks by remember {
        mutableStateOf(
            listOf(
                StudyTimeBlock(id = "1", startTime = "06:30 AM", subject = officialAvailableSubjects.firstOrNull() ?: "Mathematics", durationMinutes = 60),
                StudyTimeBlock(id = "2", startTime = "07:45 PM", subject = officialAvailableSubjects.getOrNull(1) ?: "General Awareness", durationMinutes = 60)
            )
        )
    }

    var newBlockStartTime by remember { mutableStateOf("08:00 PM") }
    var newBlockSubject by remember { mutableStateOf(officialAvailableSubjects.firstOrNull() ?: "Mathematics") }
    var newBlockDurationMins by remember { mutableIntStateOf(60) }

    // Time block overlap validator
    fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val trimmed = timeStr.trim().uppercase()
            val isPm = trimmed.endsWith("PM")
            val isAm = trimmed.endsWith("AM")
            val cleanTime = trimmed.replace("AM", "").replace("PM", "").trim()
            val parts = cleanTime.split(":")
            var hour = parts[0].trim().toInt()
            val min = if (parts.size > 1) parts[1].trim().toInt() else 0
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            hour * 60 + min
        } catch (e: Exception) {
            0
        }
    }

    val overlappingBlocks = remember(customStudyBlocks) {
        val overlaps = mutableListOf<Pair<StudyTimeBlock, StudyTimeBlock>>()
        for (i in 0 until customStudyBlocks.size) {
            for (j in i + 1 until customStudyBlocks.size) {
                val b1 = customStudyBlocks[i]
                val b2 = customStudyBlocks[j]
                val s1 = parseTimeToMinutes(b1.startTime)
                val e1 = s1 + b1.durationMinutes
                val s2 = parseTimeToMinutes(b2.startTime)
                val e2 = s2 + b2.durationMinutes
                if (s1 < e2 && s2 < e1) {
                    overlaps.add(Pair(b1, b2))
                }
            }
        }
        overlaps
    }

    val sessionLengthPresets = listOf(15, 25, 30, 45, 60, 90)
    var selectedSessionLength by remember { mutableIntStateOf(25) }

    val breakPresets = listOf(5, 10, 15)
    var selectedBreakLength by remember { mutableIntStateOf(10) }

    val mockTestLanguages = listOf("Same as App Language", "English", "Hindi")
    var selectedMockTestLanguage by remember { mutableStateOf("Same as App Language") }

    val mockTestSizes = listOf(25, 50, 100)
    var selectedMockTestSize by remember { mutableIntStateOf(25) }

    var aiPlanSuggestionMsg by remember { mutableStateOf<String?>(null) }

    fun generateAiPlanSuggestion() {
        performAutoBalance()
        val subs = selectedSubjects.toList()
        val highPrioritySubs = subs.filter { subjectPriorityMap[it] == "HIGH" }
        val startHours = listOf("06:30 AM", "08:30 AM", "04:30 PM", "06:30 PM", "08:30 PM")
        val blocks = subs.mapIndexed { index, sub ->
            StudyTimeBlock(
                id = UUID.randomUUID().toString(),
                startTime = startHours.getOrElse(index % startHours.size) { "06:00 PM" },
                subject = sub,
                durationMinutes = subjectAllocationMinutes[sub] ?: selectedSessionLength,
                topic = "Core Chapter & Question Practice"
            )
        }
        customStudyBlocks = blocks
        aiPlanSuggestionMsg = "✨ AI Blueprint Generated: Prioritized ${highPrioritySubs.joinToString(", ")} with ${dailyStudyHours}h daily target across ${selectedStudyDays.size} study days!"
    }

    // Auto-save draft changes to SharedPreferences
    LaunchedEffect(step, nameInput, selectedGrade, selectedLanguage, selectedExamPreset, customExamName, examDaysAhead, targetScoreInput, dailyStudyHours) {
        sharedPrefs.edit()
            .putInt("draft_step", step)
            .putString("draft_name", nameInput)
            .putString("draft_grade", selectedGrade)
            .putString("draft_language", selectedLanguage)
            .putString("draft_exam_preset", selectedExamPreset)
            .putString("draft_custom_exam", customExamName)
            .putFloat("draft_exam_days", examDaysAhead)
            .putString("draft_target_score", targetScoreInput)
            .putFloat("draft_daily_hours", dailyStudyHours)
            .apply()
    }

    val effectiveExamName = if (selectedExamPreset == "Custom Exam" && customExamName.isNotBlank()) customExamName else selectedExamPreset

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    IconButton(
                        onClick = { step-- },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x20FFFFFF), CircleShape)
                            .testTag("onboarding_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Step ${step - 1}",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }

                StudyMateBrandLogo(modifier = Modifier.height(34.dp))

                Text(
                    text = "Step $step of $totalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 1..totalSteps) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i <= step) NeonCyan else Color(0x30FFFFFF)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Dynamic Step Content
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "OnboardingStepAnimation"
            ) { currentStep ->
                when (currentStep) {
                    1 -> Step1ProfileAndLanguage(
                        nameInput = nameInput,
                        onNameChange = { nameInput = it },
                        selectedAvatarIndex = selectedAvatarIndex,
                        onAvatarSelect = { selectedAvatarIndex = it },
                        presetAvatars = presetAvatars,
                        gradeLevels = gradeLevels,
                        selectedGrade = selectedGrade,
                        onGradeSelect = { selectedGrade = it },
                        supportedLanguages = supportedLanguages,
                        selectedLanguage = selectedLanguage,
                        onLanguageSelect = { selectedLanguage = it }
                    )

                    2 -> Step2TargetExamAndGoals(
                        examCatalogList = examCatalogList,
                        selectedExamPreset = selectedExamPreset,
                        onExamSelect = { selectedExamPreset = it },
                        customExamName = customExamName,
                        onCustomExamNameChange = { customExamName = it },
                        examDaysAhead = examDaysAhead,
                        onExamDaysChange = { examDaysAhead = it },
                        targetScoreInput = targetScoreInput,
                        onTargetScoreChange = { targetScoreInput = it },
                        examPatternInfo = getExamPatternInfo(effectiveExamName)
                    )

                    3 -> Step3ExamSubjectsAndPriorities(
                        examName = effectiveExamName,
                        availableSubjects = officialAvailableSubjects,
                        selectedSubjects = selectedSubjects,
                        onSubjectsChange = { selectedSubjects = it },
                        priorityMap = subjectPriorityMap,
                        onPriorityChange = { sub, priority ->
                            subjectPriorityMap = subjectPriorityMap.toMutableMap().apply { put(sub, priority) }
                        },
                        customSubjectInput = customSubjectInput,
                        onCustomSubjectChange = { customSubjectInput = it },
                        onAddCustomSubject = {
                            if (customSubjectInput.isNotBlank()) {
                                val s = customSubjectInput.trim()
                                selectedSubjects = selectedSubjects + s
                                subjectPriorityMap = subjectPriorityMap.toMutableMap().apply { put(s, "HIGH") }
                                customSubjectInput = ""
                            }
                        },
                        prepLevels = prepLevels,
                        selectedPrepLevel = selectedPrepLevel,
                        onPrepLevelSelect = { selectedPrepLevel = it }
                    )

                    4 -> Step4StudyTimeAndAllocation(
                        dailyStudyHours = dailyStudyHours,
                        onDailyStudyHoursChange = {
                            dailyStudyHours = it
                            isCustomDailyTime = false
                        },
                        isCustomDailyTime = isCustomDailyTime,
                        onCustomDailyTimeToggle = { isCustomDailyTime = it },
                        dailyTimePresets = dailyTimePresets,
                        selectedStudyDays = selectedStudyDays,
                        onStudyDaysChange = { selectedStudyDays = it },
                        daysOfWeek = daysOfWeek,
                        selectedSubjects = selectedSubjects.toList(),
                        subjectAllocationMinutes = subjectAllocationMinutes,
                        onAllocationMinutesChange = { sub, mins ->
                            subjectAllocationMinutes = subjectAllocationMinutes.toMutableMap().apply {
                                put(sub, mins.coerceAtLeast(15))
                            }
                        },
                        totalAllocatedMinutes = totalAllocatedMinutes,
                        availableMinutes = availableMinutes,
                        isOverAllocated = isOverAllocated,
                        onAutoBalance = { performAutoBalance() }
                    )

                    5 -> Step5ScheduleAndFinalBlueprint(
                        effectiveExamName = effectiveExamName,
                        name = nameInput.ifBlank { "Student" },
                        selectedLanguage = selectedLanguage,
                        dailyStudyHours = dailyStudyHours,
                        selectedStudyDays = selectedStudyDays,
                        selectedSubjects = selectedSubjects.toList(),
                        timingPresets = timingPresets,
                        selectedTimingPreset = selectedTimingPreset,
                        onTimingPresetSelect = { selectedTimingPreset = it },
                        customStudyBlocks = customStudyBlocks,
                        onAddBlock = {
                            customStudyBlocks = customStudyBlocks + StudyTimeBlock(
                                id = UUID.randomUUID().toString(),
                                startTime = newBlockStartTime,
                                subject = newBlockSubject,
                                durationMinutes = newBlockDurationMins
                            )
                        },
                        onDeleteBlock = { blockId ->
                            customStudyBlocks = customStudyBlocks.filter { it.id != blockId }
                        },
                        newBlockStartTime = newBlockStartTime,
                        onNewBlockStartTimeChange = { newBlockStartTime = it },
                        newBlockSubject = newBlockSubject,
                        onNewBlockSubjectChange = { newBlockSubject = it },
                        newBlockDurationMins = newBlockDurationMins,
                        onNewBlockDurationChange = { newBlockDurationMins = it },
                        availableSubjects = selectedSubjects.toList().ifEmpty { officialAvailableSubjects },
                        overlappingBlocks = overlappingBlocks,
                        sessionLengthPresets = sessionLengthPresets,
                        selectedSessionLength = selectedSessionLength,
                        onSessionLengthSelect = { selectedSessionLength = it },
                        breakPresets = breakPresets,
                        selectedBreakLength = selectedBreakLength,
                        onBreakLengthSelect = { selectedBreakLength = it },
                        mockTestLanguages = mockTestLanguages,
                        selectedMockTestLanguage = selectedMockTestLanguage,
                        onMockTestLanguageSelect = { selectedMockTestLanguage = it },
                        mockTestSizes = mockTestSizes,
                        selectedMockTestSize = selectedMockTestSize,
                        onMockTestSizeSelect = { selectedMockTestSize = it },
                        aiPlanSuggestionMsg = aiPlanSuggestionMsg,
                        onSuggestAiPlan = { generateAiPlanSuggestion() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Buttons
            if (step < totalSteps) {
                // Validation for next step
                val isNextEnabled = when (step) {
                    1 -> nameInput.isNotBlank()
                    2 -> effectiveExamName.isNotBlank() && examDaysAhead >= 1
                    3 -> selectedSubjects.isNotEmpty()
                    4 -> availableMinutes > 0
                    else -> true
                }

                GlassButton(
                    text = "Continue to Step ${step + 1}",
                    onClick = {
                        if (isNextEnabled) {
                            if (step == 3) performAutoBalance()
                            step++
                            sharedPrefs.edit().putInt("draft_step", step).apply()
                        }
                    },
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    isPrimary = isNextEnabled,
                    testTag = "onboarding_next_button"
                )
            } else {
                val finalExamDateMillis = System.currentTimeMillis() + (examDaysAhead.toLong() * 24L * 60L * 60L * 1000L)
                val highPriority = subjectPriorityMap.filter { it.value == "HIGH" }.keys.toList()
                val medPriority = subjectPriorityMap.filter { it.value == "MEDIUM" }.keys.toList()
                val lowPriority = subjectPriorityMap.filter { it.value == "LOW" }.keys.toList()

                // Encode subject time allocation as JSON
                val allocJsonObj = JSONObject().apply {
                    subjectAllocationMinutes.forEach { (sub, mins) ->
                        put(sub, mins)
                    }
                }

                // Encode custom study blocks as JSON
                val blocksJsonArr = JSONArray().apply {
                    customStudyBlocks.forEach { block ->
                        put(JSONObject().apply {
                            put("id", block.id)
                            put("startTime", block.startTime)
                            put("subject", block.subject)
                            put("durationMinutes", block.durationMinutes)
                            put("topic", block.topic)
                        })
                    }
                }

                val finalProfile = UserProfile(
                    id = "current_user",
                    name = nameInput.ifBlank { "Student" },
                    photoUrl = presetAvatars.getOrElse(selectedAvatarIndex) { presetAvatars[0] },
                    grade = selectedGrade,
                    educationLevel = selectedGrade,
                    languagePreference = selectedLanguage,
                    examCategory = selectedExamPreset,
                    examName = effectiveExamName,
                    examDateMillis = finalExamDateMillis,
                    targetScore = targetScoreInput.ifBlank { "Top Merit / 90+ Score" },
                    goal = effectiveExamName,
                    subjects = if (selectedSubjects.isNotEmpty()) selectedSubjects.toList() else officialAvailableSubjects,
                    highPrioritySubjects = highPriority.ifEmpty { selectedSubjects.take(2) },
                    mediumPrioritySubjects = medPriority,
                    lowPrioritySubjects = lowPriority,
                    strongSubjects = selectedSubjects.filter { it !in lowPriority },
                    preparationLevel = selectedPrepLevel,
                    dailyTargetMinutes = availableMinutes,
                    availableStudyHours = dailyStudyHours,
                    subjectTimeAllocationJson = allocJsonObj.toString(),
                    preferredStudyStartTime = if (customStudyBlocks.isNotEmpty()) customStudyBlocks.first().startTime else "06:00 PM",
                    preferredStudyEndTime = "10:00 PM",
                    preferredStudyDays = selectedStudyDays.toList(),
                    breakDurationMinutes = selectedBreakLength,
                    preferredSessionDurationMinutes = selectedSessionLength,
                    customStudyBlocksJson = blocksJsonArr.toString(),
                    mockTestLanguage = if (selectedMockTestLanguage == "Same as App Language") selectedLanguage else selectedMockTestLanguage,
                    defaultMockTestQuestionCount = selectedMockTestSize,
                    preferredStudyTime = selectedTimingPreset,
                    weakSubjects = lowPriority,
                    weakTopics = emptyList(),
                    dailyStudyGoal = "Complete ${availableMinutes / 60}h ${availableMinutes % 60}m daily study target across ${selectedSubjects.size} exam subjects",
                    shortTermGoal = "Master high-yield chapters for $effectiveExamName",
                    longTermGoal = "Achieve $targetScoreInput in $effectiveExamName",
                    notificationsEnabled = true,
                    isOnboardingCompleted = true
                )

                GlassButton(
                    text = "Launch My AI Study Blueprint 🚀",
                    onClick = {
                        sharedPrefs.edit().clear().apply()
                        onComplete(finalProfile)
                    },
                    icon = Icons.Filled.Check,
                    isPrimary = true,
                    testTag = "start_study_plan_button"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

// =========================================================================================
// STEP 1: SCHOLAR PROFILE & LANGUAGE
// =========================================================================================
@Composable
private fun Step1ProfileAndLanguage(
    nameInput: String,
    onNameChange: (String) -> Unit,
    selectedAvatarIndex: Int,
    onAvatarSelect: (Int) -> Unit,
    presetAvatars: List<String>,
    gradeLevels: List<String>,
    selectedGrade: String,
    onGradeSelect: (String) -> Unit,
    supportedLanguages: List<String>,
    selectedLanguage: String,
    onLanguageSelect: (String) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to StudyMate AI 👋",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Let's set up your personalized study profile & preferences",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar Selector
            Text(
                text = "Choose your Scholar Avatar:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                presetAvatars.forEachIndexed { index, avatarUrl ->
                    val isSelected = selectedAvatarIndex == index
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.3f) else Color(0x18FFFFFF))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) NeonCyan else Color(0x30FFFFFF),
                                shape = CircleShape
                            )
                            .springClickable { onAvatarSelect(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar $index",
                            modifier = Modifier.size(38.dp).clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name Field
            OutlinedTextField(
                value = nameInput,
                onValueChange = onNameChange,
                label = { Text("What should we call you? (Your Name)") },
                placeholder = { Text("e.g. Rahul Sharma") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0x40FFFFFF)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Education Level
            Text(
                text = "Current Education / Academic Stage:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                gradeLevels.forEach { grade ->
                    val isSelected = selectedGrade == grade
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ElectricIndigo.copy(alpha = 0.45f) else Color(0x12FFFFFF))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) NeonCyan else Color(0x20FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .springClickable { onGradeSelect(grade) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onGradeSelect(grade) },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan, unselectedColor = Color.Gray)
                            )
                            Text(
                                text = grade,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Language Selection
            Text(
                text = "Preferred Study & AI Tutor Language:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = "Used across Nova AI, practice quizzes, and study explanations",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supportedLanguages.forEach { lang ->
                    val isSelected = selectedLanguage == lang
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonCyan else Color(0x18FFFFFF))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) NeonCyan else Color(0x30FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .springClickable { onLanguageSelect(lang) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lang,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF070B19) else Color.White
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================================
// STEP 2: TARGET EXAM & COUNTDOWN
// =========================================================================================
@Composable
private fun Step2TargetExamAndGoals(
    examCatalogList: List<String>,
    selectedExamPreset: String,
    onExamSelect: (String) -> Unit,
    customExamName: String,
    onCustomExamNameChange: (String) -> Unit,
    examDaysAhead: Float,
    onExamDaysChange: (Float) -> Unit,
    targetScoreInput: String,
    onTargetScoreChange: (String) -> Unit,
    examPatternInfo: String
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Target Competitive / Board Exam 🎯",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select your target exam. StudyMate will automatically load the official syllabus, subjects, and topics.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Exam Presets List
            Text(
                text = "Choose Exam:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                examCatalogList.forEach { preset ->
                    val isSelected = selectedExamPreset == preset
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0x12FFFFFF))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) NeonCyan else Color(0x20FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .springClickable { onExamSelect(preset) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (preset.contains("Railway") || preset.contains("RRB")) Icons.Filled.DirectionsTransit
                                else if (preset.contains("SSC")) Icons.Filled.AccountBalance
                                else if (preset.contains("JEE") || preset.contains("Engineering")) Icons.Filled.Build
                                else if (preset.contains("NEET") || preset.contains("Medical")) Icons.Filled.LocalHospital
                                else if (preset.contains("UPSC")) Icons.Filled.Public
                                else Icons.Filled.School,
                                contentDescription = null,
                                tint = if (isSelected) NeonCyan else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (selectedExamPreset == "Custom Exam") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customExamName,
                    onValueChange = onCustomExamNameChange,
                    label = { Text("Enter your custom exam name") },
                    placeholder = { Text("e.g. State PSC / NDA / GATE") },
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
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Exam Info Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0x1800E5FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = examPatternInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE0F2FE)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Days Until Exam Slider & Date Preview
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, examDaysAhead.toInt())
            }
            val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(targetCal.time)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Days Remaining Until Exam:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${examDaysAhead.toInt()} Days (${formattedDate})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldenSpark
                )
            }

            Slider(
                value = examDaysAhead,
                onValueChange = onExamDaysChange,
                valueRange = 15f..365f,
                steps = 69,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color(0x30FFFFFF)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Target Score Input
            OutlinedTextField(
                value = targetScoreInput,
                onValueChange = onTargetScoreChange,
                label = { Text("Target Score / Dream Goal") },
                placeholder = { Text("e.g. 90+ Marks / AIR < 500 / 99%ile") },
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
        }
    }
}

// =========================================================================================
// STEP 3: EXAM-SPECIFIC SUBJECTS & USER PRIORITIES
// =========================================================================================
@Composable
private fun Step3ExamSubjectsAndPriorities(
    examName: String,
    availableSubjects: List<String>,
    selectedSubjects: Set<String>,
    onSubjectsChange: (Set<String>) -> Unit,
    priorityMap: Map<String, String>,
    onPriorityChange: (String, String) -> Unit,
    customSubjectInput: String,
    onCustomSubjectChange: (String) -> Unit,
    onAddCustomSubject: () -> Unit,
    prepLevels: List<String>,
    selectedPrepLevel: String,
    onPrepLevelSelect: (String) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Exam Context Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ElectricIndigo.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "🎯 $examName",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose the subjects you want to prepare",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Exam-specific subjects loaded from the verified catalog. Select and prioritize each subject based on your study needs.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Select All / Clear All Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedSubjects.size} of ${availableSubjects.size} subjects selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldenSpark,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { onSubjectsChange(availableSubjects.toSet()) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Select All", color = NeonCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { onSubjectsChange(emptySet()) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Clear All", color = CoralRose, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subjects List
            if (availableSubjects.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x18EF4444),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No subject data is available for this exam yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoralRose,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add custom subjects below or retry refreshing the catalog.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSubjects.forEach { sub ->
                        val isSelected = sub in selectedSubjects
                        val currentPriority = priorityMap[sub] ?: "HIGH"

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0x2038BDF8) else Color(0x0CFFFFFF),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) NeonCyan else Color(0x20FFFFFF)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                onSubjectsChange(
                                                    if (checked) selectedSubjects + sub else selectedSubjects - sub
                                                )
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = NeonCyan,
                                                checkmarkColor = Color(0xFF070B19)
                                            )
                                        )
                                        Text(
                                            text = sub,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                        )
                                    }

                                    // Priority Pills
                                    if (isSelected) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("HIGH" to "🔥 High", "MEDIUM" to "⚡ Med", "LOW" to "📘 Low").forEach { (pKey, pLabel) ->
                                                val isPSelected = currentPriority == pKey
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isPSelected) {
                                                                if (pKey == "HIGH") CoralRose else if (pKey == "MEDIUM") GoldenSpark else ElectricIndigo
                                                            } else Color(0x18FFFFFF)
                                                        )
                                                        .springClickable { onPriorityChange(sub, pKey) }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = pLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (isPSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isPSelected) {
                                                            if (pKey == "MEDIUM") Color(0xFF070B19) else Color.White
                                                        } else Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add Custom Subject Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customSubjectInput,
                    onValueChange = onCustomSubjectChange,
                    label = { Text("+ Add Custom Subject") },
                    placeholder = { Text("e.g. Current Affairs") },
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

                Button(
                    onClick = onAddCustomSubject,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("Add", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preparation Level
            Text(
                text = "Current Preparation Stage:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                prepLevels.forEach { level ->
                    val isSelected = selectedPrepLevel == level
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ElectricIndigo.copy(alpha = 0.45f) else Color(0x12FFFFFF))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) NeonCyan else Color(0x20FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .springClickable { onPrepLevelSelect(level) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = level,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================================
// STEP 4: DAILY STUDY TIME & SUBJECT-WISE TIME ALLOCATION
// =========================================================================================
@Composable
private fun Step4StudyTimeAndAllocation(
    dailyStudyHours: Float,
    onDailyStudyHoursChange: (Float) -> Unit,
    isCustomDailyTime: Boolean,
    onCustomDailyTimeToggle: (Boolean) -> Unit,
    dailyTimePresets: List<Pair<Float, String>>,
    selectedStudyDays: Set<String>,
    onStudyDaysChange: (Set<String>) -> Unit,
    daysOfWeek: List<String>,
    selectedSubjects: List<String>,
    subjectAllocationMinutes: Map<String, Int>,
    onAllocationMinutesChange: (String, Int) -> Unit,
    totalAllocatedMinutes: Int,
    availableMinutes: Int,
    isOverAllocated: Boolean,
    onAutoBalance: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Daily Study Time & Allocation ⏱️",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "How much time can you realistically study each day, and how do you want to distribute it across subjects?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Daily Study Time Presets
            Text(
                text = "Daily Available Study Time:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dailyTimePresets.take(5).forEach { (hrs, label) ->
                    val isSelected = !isCustomDailyTime && dailyStudyHours == hrs
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonCyan else Color(0x18FFFFFF))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) NeonCyan else Color(0x30FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .springClickable { onDailyStudyHoursChange(hrs) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF070B19) else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected Daily Target:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                val h = availableMinutes / 60
                val m = availableMinutes % 60
                Text(
                    text = "${if (h > 0) "${h}h " else ""}${if (m > 0) "${m}m" else ""}".ifBlank { "0m" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            Slider(
                value = dailyStudyHours,
                onValueChange = {
                    onDailyStudyHoursChange(it)
                    onCustomDailyTimeToggle(true)
                },
                valueRange = 0.5f..10.0f,
                steps = 18,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color(0x30FFFFFF)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Study Days Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Study Days:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${selectedStudyDays.size} days / week",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldenSpark,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                daysOfWeek.forEach { day ->
                    val isSelected = day in selectedStudyDays
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ElectricIndigo else Color(0x18FFFFFF))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) NeonCyan else Color(0x20FFFFFF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .springClickable {
                                onStudyDaysChange(
                                    if (isSelected) selectedStudyDays - day else selectedStudyDays + day
                                )
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Subject-Wise Time Allocation (CRITICAL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subject-Wise Time Allocation:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "How much time do you want to give each subject?",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Button(
                    onClick = onAutoBalance,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("⚡ Auto Balance", color = Color(0xFF070B19), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Allocation Meter Bar
            val allocH = totalAllocatedMinutes / 60
            val allocM = totalAllocatedMinutes % 60
            val availH = availableMinutes / 60
            val availM = availableMinutes % 60

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isOverAllocated) Color(0x25EF4444) else Color(0x1800E5FF),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isOverAllocated) CoralRose else NeonCyan.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Allocated: ${allocH}h ${allocM}m / ${availH}h ${availM}m",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverAllocated) CoralRose else NeonCyan
                        )
                        Text(
                            text = if (isOverAllocated) "⚠️ Over Limit" else if (totalAllocatedMinutes == availableMinutes) "✨ Perfectly Balanced" else "Remaining: ${(availableMinutes - totalAllocatedMinutes).coerceAtLeast(0)}m",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOverAllocated) CoralRose else if (totalAllocatedMinutes == availableMinutes) GoldenSpark else Color(0xFFCBD5E1)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val progress = if (availableMinutes > 0) (totalAllocatedMinutes.toFloat() / availableMinutes).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (isOverAllocated) CoralRose else NeonCyan,
                        trackColor = Color(0x30FFFFFF)
                    )

                    if (isOverAllocated) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You've allocated ${allocH}h ${allocM}m, but your daily available time is ${availH}h ${availM}m. Tap Auto Balance or reduce minutes.",
                            style = MaterialTheme.typography.labelSmall,
                            color = CoralRose
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subject Allocation Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedSubjects.forEach { sub ->
                    val mins = subjectAllocationMinutes[sub] ?: 30
                    val subH = mins / 60
                    val subM = mins % 60

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x14FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x25FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { onAllocationMinutesChange(sub, mins - 15) },
                                    modifier = Modifier.size(32.dp).background(Color(0x20FFFFFF), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "-15m", tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = "${if (subH > 0) "${subH}h " else ""}${subM}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    modifier = Modifier.widthIn(min = 52.dp),
                                    textAlign = TextAlign.Center
                                )

                                IconButton(
                                    onClick = { onAllocationMinutesChange(sub, mins + 15) },
                                    modifier = Modifier.size(32.dp).background(Color(0x20FFFFFF), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "+15m", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================================
// STEP 5: SCHEDULE, SESSION LENGTH, BREAKS & AI BLUEPRINT
// =========================================================================================
@Composable
private fun Step5ScheduleAndFinalBlueprint(
    effectiveExamName: String,
    name: String,
    selectedLanguage: String,
    dailyStudyHours: Float,
    selectedStudyDays: Set<String>,
    selectedSubjects: List<String>,
    timingPresets: List<String>,
    selectedTimingPreset: String,
    onTimingPresetSelect: (String) -> Unit,
    customStudyBlocks: List<StudyTimeBlock>,
    onAddBlock: () -> Unit,
    onDeleteBlock: (String) -> Unit,
    newBlockStartTime: String,
    onNewBlockStartTimeChange: (String) -> Unit,
    newBlockSubject: String,
    onNewBlockSubjectChange: (String) -> Unit,
    newBlockDurationMins: Int,
    onNewBlockDurationChange: (Int) -> Unit,
    availableSubjects: List<String>,
    overlappingBlocks: List<Pair<StudyTimeBlock, StudyTimeBlock>>,
    sessionLengthPresets: List<Int>,
    selectedSessionLength: Int,
    onSessionLengthSelect: (Int) -> Unit,
    breakPresets: List<Int>,
    selectedBreakLength: Int,
    onBreakLengthSelect: (Int) -> Unit,
    mockTestLanguages: List<String>,
    selectedMockTestLanguage: String,
    onMockTestLanguageSelect: (String) -> Unit,
    mockTestSizes: List<Int>,
    selectedMockTestSize: Int,
    onMockTestSizeSelect: (Int) -> Unit,
    aiPlanSuggestionMsg: String?,
    onSuggestAiPlan: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Study Schedule & Session Preferences 📅",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure your focus session lengths, breaks, test defaults, and study timetable.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // AI Suggest Plan Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ElectricIndigo.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✨ AI Study Timetable Suggestion",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Text(
                            text = "Auto-calculate schedule blocks based on your exam date & priorities",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }

                    Button(
                        onClick = onSuggestAiPlan,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Suggest Plan", color = Color(0xFF070B19), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (aiPlanSuggestionMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = aiPlanSuggestionMsg,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldenSpark,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Timing Presets vs Custom Blocks
            Text(
                text = "Preferred Study Timing:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                timingPresets.forEach { timing ->
                    val isSelected = selectedTimingPreset == timing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0x12FFFFFF))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) NeonCyan else Color(0x20FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .springClickable { onTimingPresetSelect(timing) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = timing,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Custom Study Blocks Editor
            if (selectedTimingPreset.contains("Custom", ignoreCase = true) || customStudyBlocks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Custom Study Blocks:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )

                if (overlappingBlocks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x20EF4444),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⚠️ Overlapping Study Sessions Detected!",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CoralRose
                            )
                            overlappingBlocks.forEach { (b1, b2) ->
                                Text(
                                    text = "• ${b1.startTime} (${b1.subject}) overlaps with ${b2.startTime} (${b2.subject})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    customStudyBlocks.forEach { block ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x14FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x25FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "${block.startTime}  →  ${block.subject}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${block.durationMinutes} min focused block",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteBlock(block.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = CoralRose, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add block row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newBlockStartTime,
                        onValueChange = onNewBlockStartTimeChange,
                        label = { Text("Start Time") },
                        placeholder = { Text("06:30 AM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x40FFFFFF)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = onAddBlock,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("+ Add Block", color = Color(0xFF070B19), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Focused Session Length (Pomodoro / Deep Work)
            Text(
                text = "Focused Session Duration:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sessionLengthPresets.forEach { mins ->
                    val isSelected = selectedSessionLength == mins
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonCyan else Color(0x18FFFFFF))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) NeonCyan else Color(0x30FFFFFF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .springClickable { onSessionLengthSelect(mins) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${mins}m",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF070B19) else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Break Duration
            Text(
                text = "Break Duration Between Sessions:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                breakPresets.forEach { mins ->
                    val isSelected = selectedBreakLength == mins
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ElectricIndigo else Color(0x18FFFFFF))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) NeonCyan else Color(0x30FFFFFF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .springClickable { onBreakLengthSelect(mins) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${mins} min break",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Mock Test Defaults
            Text(
                text = "Default Mock Test Settings:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test Size
                Column(modifier = Modifier.weight(1f)) {
                    Text("Question Count:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        mockTestSizes.forEach { size ->
                            val isSelected = selectedMockTestSize == size
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) NeonCyan else Color(0x18FFFFFF))
                                    .springClickable { onMockTestSizeSelect(size) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$size Qs",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF070B19) else Color.White
                                )
                            }
                        }
                    }
                }

                // Test Language
                Column(modifier = Modifier.weight(1f)) {
                    Text("Test Language:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        mockTestLanguages.take(2).forEach { lang ->
                            val isSelected = selectedMockTestLanguage == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) GoldenSpark else Color(0x18FFFFFF))
                                    .springClickable { onMockTestLanguageSelect(lang) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (lang.contains("Same")) "App Lang" else lang,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF070B19) else Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Final Blueprint Summary Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0x1838BDF8),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📋 Personalized Study Blueprint Ready",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Text(text = "👤 Scholar: $name • $selectedLanguage", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Text(text = "🎯 Exam: $effectiveExamName", color = GoldenSpark, style = MaterialTheme.typography.labelSmall)
                    Text(text = "📚 Subjects: ${selectedSubjects.joinToString(", ")}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Text(text = "⏱️ Schedule: ${dailyStudyHours}h / day across ${selectedStudyDays.size} days • ${selectedSessionLength}m focus sessions", color = Color(0xFFE2E8F0), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

package com.example.ui.screens.auth

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyTimeBlock
import com.example.data.model.UserProfile
import com.example.localization.AppLanguage
import com.example.localization.LanguageManager
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
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

    var step by rememberSaveable {
        mutableIntStateOf(sharedPrefs.getInt("draft_step", 1).coerceIn(1, 3))
    }
    val totalSteps = 3

    // Hardware back navigation
    BackHandler(enabled = step > 1) {
        if (step > 1) {
            step--
            sharedPrefs.edit().putInt("draft_step", step).apply()
        }
    }

    // ==========================================
    // STEP 1 STATE - Profile & Academic Path
    // ==========================================
    var nameInput by rememberSaveable {
        mutableStateOf(sharedPrefs.getString("draft_name", if (initialName.isNotBlank() && initialName != "Student") initialName else "") ?: "")
    }
    var selectedLanguage by rememberSaveable {
        mutableStateOf(sharedPrefs.getString("draft_language", "English") ?: "English")
    }
    var selectedAcademicLevel by rememberSaveable {
        mutableStateOf(sharedPrefs.getString("draft_academic_level", "Undergraduate") ?: "Undergraduate")
    }
    var selectedCourseOrExam by rememberSaveable {
        mutableStateOf(sharedPrefs.getString("draft_course_exam", "Computer Science Engineering") ?: "Computer Science Engineering")
    }
    var studyGoalInput by rememberSaveable {
        mutableStateOf(sharedPrefs.getString("draft_study_goal", "") ?: "")
    }
    var showCoursePicker by remember { mutableStateOf(false) }

    val academicLevels = listOf("High School", "Undergraduate", "Postgraduate", "Professional")

    val popularCoursesAndExams = listOf(
        "Computer Science Engineering",
        "Mechanical Engineering",
        "Electrical Engineering",
        "Civil Engineering",
        "RRB NTPC (Railway Non-Technical)",
        "RRB Group D (Railway Recruitment)",
        "SSC CGL (Staff Selection Commission)",
        "JEE Main & Advanced (Engineering)",
        "NEET UG (Medical Entrance)",
        "UPSC Civil Services (IAS / IPS)",
        "IBPS / SBI Banking PO & Clerk",
        "CBSE Class 12 Science / Commerce",
        "General Aptitude & Data Science"
    )

    // ==========================================
    // STEP 2 STATE - Target Exam & Routine
    // ==========================================
    var dailyStudyHours by rememberSaveable {
        mutableFloatStateOf(sharedPrefs.getFloat("draft_daily_hours", 4.0f))
    }
    var selectedTimingPreset by rememberSaveable {
        mutableStateOf(sharedPrefs.getString("draft_timing_preset", "Evening (06:00 PM – 10:00 PM)") ?: "Evening (06:00 PM – 10:00 PM)")
    }
    var examDaysAhead by rememberSaveable {
        mutableFloatStateOf(sharedPrefs.getFloat("draft_exam_days", 60f))
    }

    // ==========================================
    // STEP 3 STATE - Priority Subjects & Launch
    // ==========================================
    fun getSubjectsForCourse(course: String): List<String> = when {
        course.contains("Computer", ignoreCase = true) -> listOf("Data Structures & Algorithms", "Operating Systems", "Database Management", "Computer Networks")
        course.contains("NTPC", ignoreCase = true) -> listOf("Mathematics", "General Intelligence & Reasoning", "General Awareness", "General Science")
        course.contains("SSC", ignoreCase = true) -> listOf("Quantitative Aptitude", "General Intelligence & Reasoning", "English Comprehension", "General Awareness")
        course.contains("JEE", ignoreCase = true) -> listOf("Physics", "Chemistry", "Mathematics")
        course.contains("NEET", ignoreCase = true) -> listOf("Biology (Botany & Zoology)", "Physics", "Chemistry")
        course.contains("UPSC", ignoreCase = true) -> listOf("General Studies I (History & Geography)", "General Studies II (Polity)", "General Studies III (Economy)", "CSAT Aptitude")
        course.contains("Bank", ignoreCase = true) -> listOf("Reasoning Ability", "Quantitative Aptitude", "English Language", "Banking Awareness")
        else -> listOf("Core Foundations", "Problem Solving & Aptitude", "Analytical Reasoning", "Specialized Electives")
    }

    val availableSubjects = remember(selectedCourseOrExam) {
        getSubjectsForCourse(selectedCourseOrExam)
    }

    var selectedSubjects by remember(selectedCourseOrExam) {
        mutableStateOf(availableSubjects.toSet())
    }

    var subjectPriorityMap by remember(selectedCourseOrExam) {
        mutableStateOf(availableSubjects.associateWith { "HIGH" })
    }

    // Save draft on change
    LaunchedEffect(step, nameInput, selectedLanguage, selectedAcademicLevel, selectedCourseOrExam, studyGoalInput, dailyStudyHours) {
        sharedPrefs.edit()
            .putInt("draft_step", step)
            .putString("draft_name", nameInput)
            .putString("draft_language", selectedLanguage)
            .putString("draft_academic_level", selectedAcademicLevel)
            .putString("draft_course_exam", selectedCourseOrExam)
            .putString("draft_study_goal", studyGoalInput)
            .putFloat("draft_daily_hours", dailyStudyHours)
            .apply()
    }

    fun buildFinalProfile(): UserProfile {
        val totalMins = (dailyStudyHours * 60).toInt()
        val subs = if (selectedSubjects.isNotEmpty()) selectedSubjects.toList() else availableSubjects
        val perSub = if (subs.isNotEmpty()) (totalMins / subs.size).coerceAtLeast(15) else 30

        val allocJson = JSONObject().apply {
            subs.forEach { sub -> put(sub, perSub) }
        }

        val blocksJson = JSONArray().apply {
            subs.take(3).forEachIndexed { i, sub ->
                put(JSONObject().apply {
                    put("id", UUID.randomUUID().toString())
                    put("startTime", listOf("06:00 PM", "07:30 PM", "09:00 PM").getOrElse(i) { "06:00 PM" })
                    put("subject", sub)
                    put("durationMinutes", perSub)
                    put("topic", "Core Fundamentals & Practice")
                })
            }
        }

        val highPriority = subjectPriorityMap.filter { it.value == "HIGH" }.keys.toList().ifEmpty { subs.take(2) }

        return UserProfile(
            id = "current_user",
            name = nameInput.ifBlank { "Student" },
            photoUrl = "https://api.dicebear.com/7.x/bottts/png?seed=Scholar1",
            grade = selectedAcademicLevel,
            educationLevel = selectedAcademicLevel,
            languagePreference = selectedLanguage,
            examCategory = selectedCourseOrExam,
            examName = selectedCourseOrExam,
            examDateMillis = System.currentTimeMillis() + (examDaysAhead.toLong() * 24L * 60L * 60L * 1000L),
            targetScore = if (studyGoalInput.isNotBlank()) studyGoalInput else "Top Merit / 90+ Score",
            goal = selectedCourseOrExam,
            subjects = subs,
            highPrioritySubjects = highPriority,
            mediumPrioritySubjects = subjectPriorityMap.filter { it.value == "MEDIUM" }.keys.toList(),
            lowPrioritySubjects = subjectPriorityMap.filter { it.value == "LOW" }.keys.toList(),
            strongSubjects = subs.filter { it !in highPriority },
            preparationLevel = "Intermediate (Practicing questions & mock drills)",
            dailyTargetMinutes = totalMins,
            availableStudyHours = dailyStudyHours,
            subjectTimeAllocationJson = allocJson.toString(),
            preferredStudyStartTime = "06:00 PM",
            preferredStudyEndTime = "10:00 PM",
            preferredStudyDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
            breakDurationMinutes = 10,
            preferredSessionDurationMinutes = 25,
            customStudyBlocksJson = blocksJson.toString(),
            mockTestLanguage = selectedLanguage,
            defaultMockTestQuestionCount = 25,
            preferredStudyTime = selectedTimingPreset,
            weakSubjects = highPriority,
            weakTopics = emptyList(),
            dailyStudyGoal = if (studyGoalInput.isNotBlank()) studyGoalInput else "Complete ${dailyStudyHours.toInt()}h daily target across $selectedCourseOrExam",
            shortTermGoal = "Master high-yield topics for $selectedCourseOrExam",
            longTermGoal = if (studyGoalInput.isNotBlank()) studyGoalInput else "Excel in $selectedCourseOrExam",
            notificationsEnabled = true,
            isOnboardingCompleted = true
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("onboarding_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Top Stepper Navigation Row (Screenshot 2: '✕', 3 bars, '1 OF 3')
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (step > 1) {
                                step--
                            } else {
                                // Save profile and finish
                                sharedPrefs.edit().clear().apply()
                                onComplete(buildFinalProfile())
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 3 Stepper Progress Bars
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 1..totalSteps) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i <= step) NeonCyan else Color(0xFF1E2D4D)
                                    )
                            )
                        }
                    }

                    Text(
                        text = "$step OF $totalSteps",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    )
                }

                // Dynamic Step Content
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "onboarding_step_anim"
                ) { currentStep ->
                    when (currentStep) {
                        1 -> Step1StudiesView(
                            nameInput = nameInput,
                            onNameChange = { nameInput = it },
                            selectedLanguage = selectedLanguage,
                            onLanguageSelect = { lang ->
                                selectedLanguage = lang
                                try {
                                    LanguageManager.init(context).setLanguage(if (lang == "Hindi") AppLanguage.HINDI else AppLanguage.ENGLISH)
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            },
                            academicLevels = academicLevels,
                            selectedAcademicLevel = selectedAcademicLevel,
                            onAcademicLevelSelect = { selectedAcademicLevel = it },
                            selectedCourseOrExam = selectedCourseOrExam,
                            onOpenCoursePicker = { showCoursePicker = true },
                            studyGoalInput = studyGoalInput,
                            onStudyGoalChange = { studyGoalInput = it }
                        )

                        2 -> Step2TargetRoutineView(
                            dailyStudyHours = dailyStudyHours,
                            onDailyStudyHoursChange = { dailyStudyHours = it },
                            selectedTimingPreset = selectedTimingPreset,
                            onTimingPresetSelect = { selectedTimingPreset = it },
                            examDaysAhead = examDaysAhead,
                            onExamDaysChange = { examDaysAhead = it }
                        )

                        3 -> Step3PrioritiesView(
                            selectedCourse = selectedCourseOrExam,
                            availableSubjects = availableSubjects,
                            selectedSubjects = selectedSubjects,
                            onToggleSubject = { sub ->
                                selectedSubjects = if (selectedSubjects.contains(sub)) {
                                    if (selectedSubjects.size > 1) selectedSubjects - sub else selectedSubjects
                                } else {
                                    selectedSubjects + sub
                                }
                            },
                            subjectPriorityMap = subjectPriorityMap,
                            onPriorityChange = { sub, prio ->
                                subjectPriorityMap = subjectPriorityMap + (sub to prio)
                            },
                            dailyHours = dailyStudyHours
                        )
                    }
                }
            }

            // Bottom Actions (Screenshot 2: "Next Step ->" + "COMPLETE LATER")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (step < totalSteps) {
                            step++
                            sharedPrefs.edit().putInt("draft_step", step).apply()
                        } else {
                            sharedPrefs.edit().clear().apply()
                            onComplete(buildFinalProfile())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("onboarding_next_step_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF050B14)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (step < totalSteps) "Next Step" else "Complete Setup & Launch StudyMate",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        sharedPrefs.edit().clear().apply()
                        onComplete(buildFinalProfile())
                    },
                    modifier = Modifier.testTag("onboarding_complete_later_btn")
                ) {
                    Text(
                        text = "COMPLETE LATER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }

        // Course / Exam Dialog Picker
        if (showCoursePicker) {
            AlertDialog(
                onDismissRequest = { showCoursePicker = false },
                containerColor = Color(0xFF111C33),
                titleContentColor = Color.White,
                title = {
                    Text(
                        text = "Select Course / Exam Prep",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(popularCoursesAndExams) { course ->
                            val isSelected = selectedCourseOrExam == course
                            Surface(
                                onClick = {
                                    selectedCourseOrExam = course
                                    showCoursePicker = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF163244) else Color(0xFF18233C),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) NeonCyan else Color(0xFF1E2D4D)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = course,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isSelected) NeonCyan else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCoursePicker = false }) {
                        Text("Done", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

/**
 * Step 1 View: Matches Screenshot 2 Reference ("Tell us about your studies")
 */
@Composable
private fun Step1StudiesView(
    nameInput: String,
    onNameChange: (String) -> Unit,
    selectedLanguage: String,
    onLanguageSelect: (String) -> Unit,
    academicLevels: List<String>,
    selectedAcademicLevel: String,
    onAcademicLevelSelect: (String) -> Unit,
    selectedCourseOrExam: String,
    onOpenCoursePicker: () -> Unit,
    studyGoalInput: String,
    onStudyGoalChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Title
        Text(
            text = "Tell us about your studies",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                letterSpacing = (-0.3).sp
            ),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "We'll tailor your dashboard and resources based on your academic path.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Main Dark Container Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF111C33),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4D))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. PREFERRED NAME
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PREFERRED NAME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFF94A3B8)
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = onNameChange,
                        placeholder = {
                            Text("e.g. Alex Mentor", color = Color(0xFF64748B), fontSize = 14.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF162238),
                            unfocusedContainerColor = Color(0xFF162238),
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF1E2D4D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // 2. INTERFACE LANGUAGE
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "INTERFACE LANGUAGE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFF94A3B8)
                    )

                    // Segmented Toggle Container
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF162238),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // English Tab
                            val isEnglish = selectedLanguage == "English"
                            Surface(
                                onClick = { onLanguageSelect("English") },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isEnglish) NeonCyan else Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Language,
                                        contentDescription = null,
                                        tint = if (isEnglish) Color(0xFF050B14) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "English",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = if (isEnglish) Color(0xFF050B14) else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // Hindi Tab
                            val isHindi = selectedLanguage == "Hindi"
                            Surface(
                                onClick = { onLanguageSelect("Hindi") },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isHindi) NeonCyan else Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "हिंदी (Hindi)",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = if (isHindi) Color(0xFF050B14) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }

                // Horizontal Divider
                HorizontalDivider(color = Color(0xFF1E2D4D), thickness = 1.dp)

                // 3. CURRENT ACADEMIC LEVEL
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CURRENT ACADEMIC LEVEL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFF94A3B8)
                    )

                    // 2x2 Grid of Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            academicLevels.take(2).forEach { level ->
                                AcademicLevelChip(
                                    text = level,
                                    isSelected = selectedAcademicLevel == level,
                                    onSelect = { onAcademicLevelSelect(level) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            academicLevels.drop(2).take(2).forEach { level ->
                                AcademicLevelChip(
                                    text = level,
                                    isSelected = selectedAcademicLevel == level,
                                    onSelect = { onAcademicLevelSelect(level) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 4. CLASS / COURSE / EXAM PREP
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CLASS / COURSE / EXAM PREP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFF94A3B8)
                    )

                    Surface(
                        onClick = onOpenCoursePicker,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF162238),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_course_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = selectedCourseOrExam,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.UnfoldMore,
                                contentDescription = "Change",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 5. PREFERRED STUDY GOAL [OPTIONAL]
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PREFERRED STUDY GOAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF94A3B8)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = "OPTIONAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = studyGoalInput,
                        onValueChange = onStudyGoalChange,
                        placeholder = {
                            Text("e.g. Score 90% in Finals", color = Color(0xFF64748B), fontSize = 14.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_goal_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF162238),
                            unfocusedContainerColor = Color(0xFF162238),
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF1E2D4D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AcademicLevelChip(
    text: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF0E2A38) else Color(0xFF162238),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) NeonCyan else Color(0xFF1E2D4D)
        ),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = if (isSelected) NeonCyan else Color(0xFF94A3B8)
            )
        }
    }
}

/**
 * Step 2: Target Exam & Routine
 */
@Composable
private fun Step2TargetRoutineView(
    dailyStudyHours: Float,
    onDailyStudyHoursChange: (Float) -> Unit,
    selectedTimingPreset: String,
    onTimingPresetSelect: (String) -> Unit,
    examDaysAhead: Float,
    onExamDaysChange: (Float) -> Unit
) {
    val timingOptions = listOf(
        "Morning (06:00 AM – 10:00 AM)",
        "Afternoon (01:00 PM – 05:00 PM)",
        "Evening (06:00 PM – 10:00 PM)",
        "Night (09:00 PM – 01:00 AM)"
    )

    val hourPresets = listOf(2.0f, 3.0f, 4.0f, 6.0f, 8.0f)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Daily Target & Study Routine",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp
            ),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Set your daily focus commitment and preferred study window.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF111C33),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4D))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Daily Study Target
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DAILY STUDY COMMITMENT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${dailyStudyHours.toInt()} Hours / Day",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonCyan
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        hourPresets.forEach { hrs ->
                            val isSelected = dailyStudyHours == hrs
                            Surface(
                                onClick = { onDailyStudyHoursChange(hrs) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NeonCyan else Color(0xFF162238),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0xFF1E2D4D)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${hrs.toInt()}h",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color(0xFF050B14) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E2D4D))

                // Preferred Study Window
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PREFERRED STUDY WINDOW",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )

                    timingOptions.forEach { timing ->
                        val isSelected = selectedTimingPreset == timing
                        Surface(
                            onClick = { onTimingPresetSelect(timing) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF0E2A38) else Color(0xFF162238),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0xFF1E2D4D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = timing,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 3: Priorities & Launch
 */
@Composable
private fun Step3PrioritiesView(
    selectedCourse: String,
    availableSubjects: List<String>,
    selectedSubjects: Set<String>,
    onToggleSubject: (String) -> Unit,
    subjectPriorityMap: Map<String, String>,
    onPriorityChange: (String, String) -> Unit,
    dailyHours: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Prioritize Focus Subjects",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp
            ),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Select high-priority subjects for your AI roadmap in $selectedCourse.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF111C33),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4D))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                availableSubjects.forEach { sub ->
                    val isIncluded = selectedSubjects.contains(sub)
                    val currentPriority = subjectPriorityMap[sub] ?: "HIGH"

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF162238),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isIncluded) NeonCyan.copy(alpha = 0.5f) else Color(0xFF1E2D4D)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Checkbox(
                                        checked = isIncluded,
                                        onCheckedChange = { onToggleSubject(sub) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = NeonCyan,
                                            checkmarkColor = Color(0xFF050B14)
                                        )
                                    )
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }

                            if (isIncluded) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("HIGH" to "High Focus", "MEDIUM" to "Moderate", "LOW" to "Light").forEach { (prioKey, prioLabel) ->
                                        val isPrioSelected = currentPriority == prioKey
                                        Surface(
                                            onClick = { onPriorityChange(sub, prioKey) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isPrioSelected) {
                                                if (prioKey == "HIGH") Color(0xFF0E2A38) else Color(0xFF1E293B)
                                            } else Color.Transparent,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isPrioSelected) NeonCyan else Color(0xFF2A3B5D)
                                            ),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = prioLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isPrioSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = if (isPrioSelected) NeonCyan else Color(0xFF94A3B8)
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
    }
}

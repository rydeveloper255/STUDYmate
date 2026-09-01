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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.persistence.PersistenceMonitor
import com.example.data.persistence.PersistenceStatus
import com.example.localization.AppLanguage
import com.example.localization.LanguageManager
import com.example.service.analytics.DiagnosticCategory
import com.example.service.analytics.DiagnosticLogger
import com.example.ui.components.StudyMateBrandLogo
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

data class OnboardingScheduleSlot(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val iconName: String, // "morning", "afternoon", "evening", "night", "custom"
    val startTime: String,
    val durationMinutes: Int,
    val breakMinutes: Int,
    val isEnabled: Boolean = true
)

@Composable
fun OnboardingScreen(
    initialName: String = "Student",
    onComplete: (userProfile: UserProfile) -> Unit = {},
    onSaveProfile: ((UserProfile, (Result<UserProfile>) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sharedPrefs = remember {
        context.getSharedPreferences("studymate_onboarding_draft", Context.MODE_PRIVATE)
    }

    var step by rememberSaveable {
        mutableIntStateOf(sharedPrefs.getInt("draft_step", 1).coerceIn(1, 3))
    }
    val totalSteps = 3

    // Real Persistence & Verification State
    var isSaving by remember { mutableStateOf(false) }
    var saveErrorMessage by remember { mutableStateOf<String?>(null) }
    var saveAffectedFields by remember { mutableStateOf<List<String>>(emptyList()) }
    var saveSuccessConfirmed by remember { mutableStateOf(false) }

    // Hardware back navigation
    BackHandler(enabled = step > 1 && !isSaving) {
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
        mutableStateOf(sharedPrefs.getString("draft_course_exam", "RRB NTPC (Railway Non-Technical)") ?: "RRB NTPC (Railway Non-Technical)")
    }
    var isCustomExam by rememberSaveable {
        mutableStateOf(sharedPrefs.getBoolean("draft_is_custom_exam", false))
    }
    var customExamNameInput by rememberSaveable {
        mutableStateOf(sharedPrefs.getString("draft_custom_exam_name", "") ?: "")
    }
    var customExamAuthorityInput by rememberSaveable {
        mutableStateOf(sharedPrefs.getString("draft_custom_exam_auth", "") ?: "")
    }
    var studyGoalInput by rememberSaveable {
        mutableStateOf(sharedPrefs.getString("draft_study_goal", "") ?: "")
    }
    var showCoursePicker by remember { mutableStateOf(false) }

    val academicLevels = listOf("High School", "Undergraduate", "Postgraduate", "Professional")

    val popularCoursesAndExams = listOf(
        "RRB NTPC (Railway Non-Technical)",
        "RRB Group D (Railway Recruitment)",
        "SSC CGL (Staff Selection Commission)",
        "JEE Main & Advanced (Engineering)",
        "NEET UG (Medical Entrance)",
        "UPSC Civil Services (IAS / IPS)",
        "IBPS / SBI Banking PO & Clerk",
        "CBSE Class 12 Science / Commerce",
        "Computer Science Engineering",
        "Mechanical Engineering",
        "Electrical Engineering",
        "Civil Engineering",
        "Custom Exam (Specify your syllabus)"
    )

    // ==========================================
    // STEP 2 STATE - Target Exam & Customizable Schedule
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

    // Interactive Customizable Schedule Slots
    var scheduleSlots by remember {
        mutableStateOf(
            listOf(
                OnboardingScheduleSlot(
                    id = "slot_morning",
                    title = "Morning Focus Slot",
                    iconName = "morning",
                    startTime = "06:30 AM",
                    durationMinutes = 60,
                    breakMinutes = 10,
                    isEnabled = true
                ),
                OnboardingScheduleSlot(
                    id = "slot_afternoon",
                    title = "Afternoon Practice Slot",
                    iconName = "afternoon",
                    startTime = "02:00 PM",
                    durationMinutes = 60,
                    breakMinutes = 10,
                    isEnabled = true
                ),
                OnboardingScheduleSlot(
                    id = "slot_evening",
                    title = "Evening Deep Revision",
                    iconName = "evening",
                    startTime = "06:00 PM",
                    durationMinutes = 90,
                    breakMinutes = 15,
                    isEnabled = true
                ),
                OnboardingScheduleSlot(
                    id = "slot_night",
                    title = "Night Flashcards & Recall",
                    iconName = "night",
                    startTime = "09:00 PM",
                    durationMinutes = 45,
                    breakMinutes = 10,
                    isEnabled = false
                )
            )
        )
    }

    // ==========================================
    // STEP 3 STATE - Priority Subjects & Custom Subjects
    // ==========================================
    fun getSubjectsForCourse(course: String, isCustom: Boolean): List<String> {
        if (isCustom) return emptyList()
        return when {
            course.contains("NTPC", ignoreCase = true) -> listOf("Mathematics", "General Intelligence & Reasoning", "General Awareness", "General Science")
            course.contains("Group D", ignoreCase = true) -> listOf("Mathematics", "General Intelligence & Reasoning", "General Science", "Current Affairs")
            course.contains("SSC", ignoreCase = true) -> listOf("Quantitative Aptitude", "General Intelligence & Reasoning", "English Comprehension", "General Awareness")
            course.contains("JEE", ignoreCase = true) -> listOf("Physics", "Chemistry", "Mathematics")
            course.contains("NEET", ignoreCase = true) -> listOf("Biology (Botany & Zoology)", "Physics", "Chemistry")
            course.contains("UPSC", ignoreCase = true) -> listOf("General Studies I (History & Geography)", "General Studies II (Polity)", "General Studies III (Economy)", "CSAT Aptitude")
            course.contains("Bank", ignoreCase = true) || course.contains("IBPS", ignoreCase = true) -> listOf("Reasoning Ability", "Quantitative Aptitude", "English Language", "Banking Awareness")
            course.contains("Computer", ignoreCase = true) -> listOf("Data Structures & Algorithms", "Operating Systems", "Database Management", "Computer Networks")
            course.contains("Mechanical", ignoreCase = true) -> listOf("Thermodynamics", "Fluid Mechanics", "Strength of Materials", "Manufacturing Tech")
            course.contains("Electrical", ignoreCase = true) -> listOf("Circuit Theory", "Control Systems", "Power Systems", "Electromagnetics")
            course.contains("Civil", ignoreCase = true) -> listOf("Structural Engineering", "Geotechnical Engineering", "Transportation", "Surveying")
            else -> listOf("Core Foundations", "Problem Solving & Aptitude", "Analytical Reasoning", "Specialized Electives")
        }
    }

    var customSubjectsList by remember {
        mutableStateOf(
            if (sharedPrefs.contains("draft_custom_subjects")) {
                sharedPrefs.getStringSet("draft_custom_subjects", emptySet())?.toList() ?: listOf("Subject 1", "Subject 2")
            } else {
                listOf("Core Subject 1", "Analytical Aptitude", "Specialized Paper")
            }
        )
    }
    var newCustomSubjectInput by remember { mutableStateOf("") }

    val activeExamName = if (isCustomExam && customExamNameInput.isNotBlank()) customExamNameInput.trim() else selectedCourseOrExam

    val availableSubjects = remember(selectedCourseOrExam, isCustomExam, customSubjectsList) {
        if (isCustomExam) customSubjectsList else getSubjectsForCourse(selectedCourseOrExam, false)
    }

    var selectedSubjects by remember(availableSubjects) {
        mutableStateOf(availableSubjects.toSet())
    }

    var subjectPriorityMap by remember(availableSubjects) {
        mutableStateOf(
            availableSubjects.mapIndexed { index, sub ->
                sub to if (index == 0) "HIGH" else if (index == 1) "HIGH" else "MEDIUM"
            }.toMap()
        )
    }

    // Helper: Build the final UserProfile entity
    fun buildFinalProfile(): UserProfile {
        val highPriority = subjectPriorityMap.filter { it.value == "HIGH" && selectedSubjects.contains(it.key) }.keys.toList()
        val mediumPriority = subjectPriorityMap.filter { it.value == "MEDIUM" && selectedSubjects.contains(it.key) }.keys.toList()
        val lowPriority = subjectPriorityMap.filter { it.value == "LOW" && selectedSubjects.contains(it.key) }.keys.toList()

        // Serialize Custom Study Schedule Slots
        val activeSlots = scheduleSlots.filter { it.isEnabled }
        val slotsJsonArray = JSONArray()
        activeSlots.forEach { slot ->
            val obj = JSONObject().apply {
                put("id", slot.id)
                put("subject", highPriority.firstOrNull() ?: selectedSubjects.firstOrNull() ?: "General Studies")
                put("startTime", slot.startTime)
                put("durationMinutes", slot.durationMinutes)
                put("breakMinutes", slot.breakMinutes)
                put("enabled", slot.isEnabled)
                put("topic", slot.title)
            }
            slotsJsonArray.put(obj)
        }

        val totalDailyMins = (dailyStudyHours * 60).toInt().coerceAtLeast(60)

        return UserProfile(
            id = "current_user",
            name = if (nameInput.isNotBlank()) nameInput.trim() else "Student",
            appLanguage = selectedLanguage,
            languagePreference = selectedLanguage,
            educationLevel = selectedAcademicLevel,
            examName = activeExamName,
            examCategory = if (isCustomExam) "Custom Goal / Exam" else "Competitive / Academic",
            examDateMillis = System.currentTimeMillis() + (examDaysAhead.toLong() * 24 * 60 * 60 * 1000L),
            dailyTargetMinutes = totalDailyMins,
            availableStudyHours = dailyStudyHours,
            preferredStudyStartTime = scheduleSlots.firstOrNull { it.isEnabled }?.startTime ?: "06:00 PM",
            preferredStudyTime = selectedTimingPreset,
            subjects = if (selectedSubjects.isNotEmpty()) selectedSubjects.toList() else listOf("General Studies"),
            highPrioritySubjects = highPriority.ifEmpty { selectedSubjects.take(2).toList() },
            mediumPrioritySubjects = mediumPriority,
            lowPrioritySubjects = lowPriority,
            customStudyBlocksJson = slotsJsonArray.toString(),
            dailyStudyGoal = if (studyGoalInput.isNotBlank()) studyGoalInput.trim() else "Master key chapters & complete daily practice targets",
            isOnboardingCompleted = true
        )
    }

    // Real Save Trigger
    fun executeSave() {
        isSaving = true
        saveErrorMessage = null
        saveAffectedFields = emptyList()

        val profileToSave = buildFinalProfile()

        if (onSaveProfile != null) {
            onSaveProfile(profileToSave) { result ->
                isSaving = false
                result.onSuccess {
                    saveSuccessConfirmed = true
                    sharedPrefs.edit().clear().apply()
                    onComplete(it)
                }.onFailure { error ->
                    saveErrorMessage = error.message ?: "Failed to save profile. Please verify your connection."
                    val affected = mutableListOf<String>()
                    if (nameInput.isBlank()) affected.add("Preferred Name")
                    if (isCustomExam && customExamNameInput.isBlank()) affected.add("Custom Exam Title")
                    if (selectedSubjects.isEmpty()) affected.add("Subjects Selection")
                    saveAffectedFields = if (affected.isNotEmpty()) affected else listOf("Profile Details", "Database Storage")
                    DiagnosticLogger.logError(
                        category = DiagnosticCategory.ONBOARDING_SAVE_ERROR,
                        operation = "OnboardingScreenSave",
                        resource = "UserProfile",
                        affectedFields = saveAffectedFields,
                        errorMessage = saveErrorMessage ?: "Save failed"
                    )
                }
            }
        } else {
            // Standard fallback
            sharedPrefs.edit().clear().apply()
            isSaving = false
            saveSuccessConfirmed = true
            onComplete(profileToSave)
        }
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
                                executeSave()
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

                // In-Place Save Error Card on Current Step
                AnimatedVisibility(
                    visible = !saveErrorMessage.isNullOrBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("onboarding_save_error_card"),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2D1217),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = "Save Error",
                                    tint = CoralRose,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Couldn't Save Study Routine",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }

                            Text(
                                text = saveErrorMessage ?: "An error occurred while saving your academic plan to the local database.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFFCA5A5),
                                    lineHeight = 18.sp
                                )
                            )

                            if (saveAffectedFields.isNotEmpty()) {
                                Text(
                                    text = "Affected Areas: " + saveAffectedFields.joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { executeSave() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CoralRose,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.height(36.dp).testTag("onboarding_retry_save_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Save", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
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
                            isCustomExam = isCustomExam,
                            onToggleCustomExam = { isCustomExam = it },
                            customExamNameInput = customExamNameInput,
                            onCustomExamNameChange = { customExamNameInput = it },
                            customExamAuthorityInput = customExamAuthorityInput,
                            onCustomExamAuthorityChange = { customExamAuthorityInput = it },
                            studyGoalInput = studyGoalInput,
                            onStudyGoalChange = { studyGoalInput = it }
                        )

                        2 -> Step2TargetRoutineView(
                            dailyStudyHours = dailyStudyHours,
                            onDailyStudyHoursChange = { dailyStudyHours = it },
                            selectedTimingPreset = selectedTimingPreset,
                            onTimingPresetSelect = { selectedTimingPreset = it },
                            examDaysAhead = examDaysAhead,
                            onExamDaysChange = { examDaysAhead = it },
                            scheduleSlots = scheduleSlots,
                            onToggleSlot = { id, enabled ->
                                scheduleSlots = scheduleSlots.map { if (it.id == id) it.copy(isEnabled = enabled) else it }
                            },
                            onUpdateSlotDuration = { id, duration ->
                                scheduleSlots = scheduleSlots.map { if (it.id == id) it.copy(durationMinutes = duration) else it }
                            },
                            onAddCustomSlot = { title, time, duration, breakMins ->
                                scheduleSlots = scheduleSlots + OnboardingScheduleSlot(
                                    title = title,
                                    iconName = "custom",
                                    startTime = time,
                                    durationMinutes = duration,
                                    breakMinutes = breakMins,
                                    isEnabled = true
                                )
                            }
                        )

                        3 -> Step3PrioritiesView(
                            selectedCourse = activeExamName,
                            isCustomExam = isCustomExam,
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
                            onAddCustomSubject = { newSub ->
                                if (newSub.isNotBlank() && !customSubjectsList.contains(newSub.trim())) {
                                    val updated = customSubjectsList + newSub.trim()
                                    customSubjectsList = updated
                                    selectedSubjects = selectedSubjects + newSub.trim()
                                    subjectPriorityMap = subjectPriorityMap + (newSub.trim() to "HIGH")
                                    sharedPrefs.edit().putStringSet("draft_custom_subjects", updated.toSet()).apply()
                                }
                            },
                            onRemoveCustomSubject = { sub ->
                                val updated = customSubjectsList - sub
                                customSubjectsList = updated
                                selectedSubjects = selectedSubjects - sub
                                subjectPriorityMap = subjectPriorityMap - sub
                                sharedPrefs.edit().putStringSet("draft_custom_subjects", updated.toSet()).apply()
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
                            executeSave()
                        }
                    },
                    enabled = !isSaving,
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
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color(0xFF050B14),
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Saving to Database...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        )
                    } else {
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
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        executeSave()
                    },
                    enabled = !isSaving,
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

        // Course / Exam Dialog Picker with Custom Exam option
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
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(popularCoursesAndExams) { course ->
                            val isThisCustom = course.startsWith("Custom Exam")
                            val isSelected = if (isThisCustom) isCustomExam else (selectedCourseOrExam == course && !isCustomExam)

                            Surface(
                                onClick = {
                                    if (isThisCustom) {
                                        isCustomExam = true
                                    } else {
                                        isCustomExam = false
                                        selectedCourseOrExam = course
                                    }
                                    showCoursePicker = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF163244) else Color(0xFF18233C),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) NeonCyan else Color(0xFF1E2D4D)
                                ),
                                modifier = Modifier.fillMaxWidth()
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
                                            imageVector = if (isThisCustom) Icons.Outlined.EditCalendar else Icons.Outlined.School,
                                            contentDescription = null,
                                            tint = if (isSelected) NeonCyan else Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = course,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isSelected) NeonCyan else Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
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
 * Step 1 View: Matches Screenshot 2 Reference ("Tell us about your studies") + Custom Exam Option
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
    isCustomExam: Boolean,
    onToggleCustomExam: (Boolean) -> Unit,
    customExamNameInput: String,
    onCustomExamNameChange: (String) -> Unit,
    customExamAuthorityInput: String,
    onCustomExamAuthorityChange: (String) -> Unit,
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
                        text = "TARGET EXAM / SYLLABUS",
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
                                    imageVector = if (isCustomExam) Icons.Outlined.EditNote else Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isCustomExam) "Custom Exam (${customExamNameInput.ifBlank { "Unspecified" }})" else selectedCourseOrExam,
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

                    // If Custom Exam is active, render custom fields
                    if (isCustomExam) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customExamNameInput,
                            onValueChange = onCustomExamNameChange,
                            placeholder = { Text("Enter Exam Name (e.g. GATE CS, UGC NET)", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_custom_exam_name_input"),
                            shape = RoundedCornerShape(10.dp),
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
                            Text("e.g. Score Top 100 Rank in Finals", color = Color(0xFF64748B), fontSize = 14.sp)
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
 * Step 2: Target Routine & Fully Customizable Study Schedule
 */
@Composable
private fun Step2TargetRoutineView(
    dailyStudyHours: Float,
    onDailyStudyHoursChange: (Float) -> Unit,
    selectedTimingPreset: String,
    onTimingPresetSelect: (String) -> Unit,
    examDaysAhead: Float,
    onExamDaysChange: (Float) -> Unit,
    scheduleSlots: List<OnboardingScheduleSlot>,
    onToggleSlot: (id: String, enabled: Boolean) -> Unit,
    onUpdateSlotDuration: (id: String, duration: Int) -> Unit,
    onAddCustomSlot: (title: String, time: String, duration: Int, breakMins: Int) -> Unit
) {
    val timingOptions = listOf(
        "Morning (06:00 AM – 10:00 AM)",
        "Afternoon (01:00 PM – 05:00 PM)",
        "Evening (06:00 PM – 10:00 PM)",
        "Night (09:00 PM – 01:00 AM)"
    )

    val hourPresets = listOf(2.0f, 3.0f, 4.0f, 6.0f, 8.0f)
    var showAddSlotDialog by remember { mutableStateOf(false) }

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
            text = "Configure your daily study commitments and active time windows.",
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Daily Study Target Hours Stepper
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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

                // Customizable Study Schedule Slots
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CUSTOMIZABLE STUDY SLOTS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = Color(0xFF94A3B8)
                        )

                        TextButton(
                            onClick = { showAddSlotDialog = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Slot", style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontWeight = FontWeight.Bold))
                        }
                    }

                    scheduleSlots.forEach { slot ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (slot.isEnabled) Color(0xFF162238) else Color(0xFF111827),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (slot.isEnabled) NeonCyan.copy(alpha = 0.4f) else Color(0xFF1E2D4D)
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = when (slot.iconName) {
                                                "morning" -> Icons.Outlined.WbSunny
                                                "afternoon" -> Icons.Outlined.LightMode
                                                "evening" -> Icons.Outlined.WbTwilight
                                                "night" -> Icons.Outlined.Bedtime
                                                else -> Icons.Outlined.AccessTime
                                            },
                                            contentDescription = null,
                                            tint = if (slot.isEnabled) NeonCyan else Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = slot.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (slot.isEnabled) Color.White else Color(0xFF64748B)
                                                )
                                            )
                                            Text(
                                                text = "${slot.startTime} • ${slot.durationMinutes}m focus (${slot.breakMinutes}m break)",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = slot.isEnabled,
                                        onCheckedChange = { onToggleSlot(slot.id, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF050B14),
                                            checkedTrackColor = NeonCyan,
                                            uncheckedThumbColor = Color(0xFF64748B),
                                            uncheckedTrackColor = Color(0xFF1E2D4D)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E2D4D))

                // Target Exam Countdown Days
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TARGET EXAM TIMELINE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${examDaysAhead.toInt()} Days Ahead",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonCyan
                        )
                    }

                    Slider(
                        value = examDaysAhead,
                        onValueChange = onExamDaysChange,
                        valueRange = 15f..365f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color(0xFF1E2D4D)
                        )
                    )
                }
            }
        }
    }

    if (showAddSlotDialog) {
        var customTitle by remember { mutableStateOf("") }
        var customTime by remember { mutableStateOf("07:00 PM") }
        var customDuration by remember { mutableFloatStateOf(60f) }

        AlertDialog(
            onDismissRequest = { showAddSlotDialog = false },
            containerColor = Color(0xFF111C33),
            title = { Text("Add Custom Study Slot", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        placeholder = { Text("Slot Title (e.g. Mock Test Revision)", color = Color(0xFF64748B), fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = customTime,
                        onValueChange = { customTime = it },
                        placeholder = { Text("Start Time (e.g. 08:30 PM)", color = Color(0xFF64748B), fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text("Duration: ${customDuration.toInt()} Minutes", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)))
                    Slider(
                        value = customDuration,
                        onValueChange = { customDuration = it },
                        valueRange = 20f..180f,
                        steps = 7,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customTitle.isNotBlank()) {
                            onAddCustomSlot(customTitle.trim(), customTime.trim(), customDuration.toInt(), 10)
                            showAddSlotDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF050B14))
                ) {
                    Text("Add Slot", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSlotDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

/**
 * Step 3: Priorities & Launch (Supports Standard & Custom Exams)
 */
@Composable
private fun Step3PrioritiesView(
    selectedCourse: String,
    isCustomExam: Boolean,
    availableSubjects: List<String>,
    selectedSubjects: Set<String>,
    onToggleSubject: (String) -> Unit,
    subjectPriorityMap: Map<String, String>,
    onPriorityChange: (String, String) -> Unit,
    onAddCustomSubject: (String) -> Unit,
    onRemoveCustomSubject: (String) -> Unit,
    dailyHours: Float
) {
    var newSubjectInput by remember { mutableStateOf("") }

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
            text = if (isCustomExam) "Add and customize subjects for $selectedCourse." else "Select focus subjects for your AI roadmap in $selectedCourse.",
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
                // If custom exam, allow adding custom subjects
                if (isCustomExam) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSubjectInput,
                            onValueChange = { newSubjectInput = it },
                            placeholder = { Text("Add New Subject...", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF162238),
                                unfocusedContainerColor = Color(0xFF162238),
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0xFF1E2D4D),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = {
                                if (newSubjectInput.isNotBlank()) {
                                    onAddCustomSubject(newSubjectInput.trim())
                                    newSubjectInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF050B14)),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("Add", fontWeight = FontWeight.Bold)
                        }
                    }
                }

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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
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

                                if (isCustomExam && availableSubjects.size > 1) {
                                    IconButton(
                                        onClick = { onRemoveCustomSubject(sub) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove",
                                            tint = CoralRose,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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

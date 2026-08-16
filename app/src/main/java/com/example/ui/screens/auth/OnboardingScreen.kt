package com.example.ui.screens.auth

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StudyMateBrandLogo
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(
    initialName: String = "Student",
    onComplete: (
        name: String,
        grade: String,
        subjects: List<String>,
        goal: String,
        examName: String,
        dailyMinutes: Int,
        preferredTime: String,
        notifs: Boolean
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 10 // 1 to 9 + Final Step

    var nameInput by remember { mutableStateOf(if (initialName.isNotBlank() && initialName != "Student") initialName else "") }
    var selectedGrade by remember { mutableStateOf("Class 12") }
    var selectedSubjects by remember { mutableStateOf(setOf("Mathematics", "Physics", "Chemistry")) }
    var customSubjectInput by remember { mutableStateOf("") }
    var selectedGoal by remember { mutableStateOf("Competitive Exam") }
    var examNameInput by remember { mutableStateOf("National Entrance Exam") }
    var selectedDailyMinutes by remember { mutableIntStateOf(180) } // 3 hours default
    var selectedStudyTime by remember { mutableStateOf("Evening") }
    var notifStudy by remember { mutableStateOf(true) }
    var notifFocus by remember { mutableStateOf(true) }
    var notifMotivation by remember { mutableStateOf(true) }
    var notifExam by remember { mutableStateOf(true) }

    val gradeOptions = listOf(
        "Class 6", "Class 7", "Class 8", "Class 9",
        "Class 10", "Class 11", "Class 12", "College", "Other"
    )

    val subjectOptions = listOf(
        "Mathematics", "Physics", "Chemistry", "Biology",
        "English", "Computer Science", "Social Science"
    )

    val goalOptions = listOf(
        "School Exams", "Board Exams", "Competitive Exam",
        "College", "General Learning", "Custom Goal"
    )

    val dailyTimeOptions = listOf(
        30 to "30 minutes",
        60 to "1 hour",
        120 to "2 hours",
        180 to "3 hours"
    )

    val studySlotOptions = listOf("Morning", "Afternoon", "Evening", "Night / Custom")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(20.dp)
            .testTag("onboarding_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Nav & Progress Bar
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (step > 1 && step < totalSteps) {
                        IconButton(
                            onClick = { step-- },
                            modifier = Modifier.testTag("onboarding_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Text(
                        text = "Step $step of $totalSteps",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )

                    if (step in 2..8) {
                        TextButton(
                            onClick = { step++ },
                            modifier = Modifier.testTag("onboarding_skip_button")
                        ) {
                            Text("Skip", color = NeonCyan)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Smooth Progress Bar
                LinearProgressIndicator(
                    progress = { (step.toFloat() / totalSteps.toFloat()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = NeonCyan,
                    trackColor = Color(0x30FFFFFF)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Content Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                elevation = 10.dp,
                fillAlpha = 0.8f
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        1 -> {
                            // Step 1 - Welcome & Branding
                            StudyMateBrandLogo(
                                size = 130.dp,
                                showTypography = false,
                                animated = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Welcome to StudyMate AI 👋",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Let's personalize your study curriculum, revision timetable, and AI tutor for your exact academic goals.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }

                        2 -> {
                            // Step 2 - Name
                            Text(
                                text = "What should we call you?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Name / Nickname") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("name_input_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x40FFFFFF)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        3 -> {
                            // Step 3 - Class
                            Text(
                                text = "What do you study?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                gradeOptions.chunked(3).forEach { rowGrades ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowGrades.forEach { g ->
                                            val isSelected = selectedGrade == g
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) NeonCyan else Color(0x20FFFFFF))
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) Color.White else Color(0x30FFFFFF),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .springClickable(testTag = "grade_${g.lowercase().replace(" ", "_")}") {
                                                        selectedGrade = g
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = g,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (isSelected) Color(0xFF070B19) else Color.White,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        4 -> {
                            // Step 4 - Subjects
                            Text(
                                text = "Choose your subjects",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select all that apply for personalized plans",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                subjectOptions.forEach { sub ->
                                    val isSelected = selectedSubjects.contains(sub)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0x3038BDF8) else Color(0x15FFFFFF))
                                            .border(
                                                1.dp,
                                                if (isSelected) NeonCyan else Color(0x25FFFFFF),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .springClickable(testTag = "subject_${sub.lowercase().replace(" ", "_")}") {
                                                val next = selectedSubjects.toMutableSet()
                                                if (isSelected) next.remove(sub) else next.add(sub)
                                                selectedSubjects = next
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = sub,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                val next = selectedSubjects.toMutableSet()
                                                if (checked) next.add(sub) else next.remove(sub)
                                                selectedSubjects = next
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = NeonCyan,
                                                checkmarkColor = Color(0xFF070B19)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        5 -> {
                            // Step 5 - Goal
                            Text(
                                text = "What are you preparing for?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                goalOptions.forEach { goal ->
                                    val isSelected = selectedGoal == goal
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) ElectricViolet else Color(0x18FFFFFF))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color.White else Color(0x30FFFFFF),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .springClickable(testTag = "goal_${goal.lowercase().replace(" ", "_")}") {
                                                selectedGoal = goal
                                            }
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = goal,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        6 -> {
                            // Step 6 - Exam Information
                            Text(
                                text = "Exam Details (Optional)",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Used to create your countdown roadmap",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = examNameInput,
                                onValueChange = { examNameInput = it },
                                label = { Text("Exam Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("exam_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x40FFFFFF)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        7 -> {
                            // Step 7 - Daily Study Target
                            Text(
                                text = "Daily Study Target",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Balanced, sustainable targets build lasting mastery.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                dailyTimeOptions.forEach { (mins, label) ->
                                    val isSelected = selectedDailyMinutes == mins
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) NeonCyan else Color(0x18FFFFFF))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color.White else Color(0x30FFFFFF),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .springClickable(testTag = "time_${mins}m") {
                                                selectedDailyMinutes = mins
                                            }
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) Color(0xFF070B19) else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        8 -> {
                            // Step 8 - Preferred Study Slot
                            Text(
                                text = "Preferred Study Slot",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                studySlotOptions.forEach { slot ->
                                    val isSelected = selectedStudyTime == slot
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) NebulaPurple else Color(0x18FFFFFF))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color.White else Color(0x30FFFFFF),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .springClickable(testTag = "slot_${slot.lowercase().replace(" ", "_")}") {
                                                selectedStudyTime = slot
                                            }
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = slot,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        9 -> {
                            // Step 9 - Notifications
                            Text(
                                text = "Smart Reminders",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Choose what helpful alerts you'd like to receive:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple("Study reminders", notifStudy, { v: Boolean -> notifStudy = v }),
                                    Triple("Focus reminders", notifFocus, { v: Boolean -> notifFocus = v }),
                                    Triple("Motivational quotes", notifMotivation, { v: Boolean -> notifMotivation = v }),
                                    Triple("Exam countdown alerts", notifExam, { v: Boolean -> notifExam = v })
                                ).forEach { (title, stateVal, setter) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x18FFFFFF))
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                        Switch(
                                            checked = stateVal,
                                            onCheckedChange = setter,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF070B19),
                                                checkedTrackColor = NeonCyan
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        10 -> {
                            // Final Step
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(GoldenSpark, NeonCyan))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.RocketLaunch, null, tint = Color(0xFF070B19), modifier = Modifier.size(44.dp))
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Your StudyMate is ready! 🚀",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "We've assembled your customized curriculum plan and AI Tutor setup for $selectedGrade.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom CTA
            if (step < totalSteps) {
                GlassButton(
                    text = "Next",
                    onClick = { step++ },
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    isPrimary = true,
                    testTag = "onboarding_next_button"
                )
            } else {
                GlassButton(
                    text = "Start My Study Plan",
                    onClick = {
                        onComplete(
                            nameInput,
                            selectedGrade,
                            selectedSubjects.toList(),
                            selectedGoal,
                            examNameInput,
                            selectedDailyMinutes,
                            selectedStudyTime,
                            notifStudy || notifFocus
                        )
                    },
                    icon = Icons.Filled.Check,
                    isPrimary = true,
                    testTag = "start_study_plan_button"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

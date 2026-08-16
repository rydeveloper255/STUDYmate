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
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StudyMateBrandLogo
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OnboardingScreen(
    initialName: String = "Student",
    onComplete: (userProfile: UserProfile) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 5

    // ==========================================
    // STEP 1 STATE - Profile & Identity
    // ==========================================
    var nameInput by remember { mutableStateOf(if (initialName.isNotBlank() && initialName != "Student") initialName else "") }
    var selectedAvatarIndex by remember { mutableIntStateOf(0) }
    var customPhotoUrl by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf("Class 12") }
    var customGradeInput by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var customLanguageInput by remember { mutableStateOf("") }

    val presetAvatars = listOf(
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar1",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar2",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar3",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar4",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar5",
        "https://api.dicebear.com/7.x/bottts/png?seed=Scholar6"
    )

    val classLevels = listOf(
        "Class 9", "Class 10", "Class 11", "Class 12",
        "Undergraduate (B.Tech / B.Sc / MBBS / B.A)", "Postgraduate",
        "Self-Taught / Professional", "Custom"
    )

    val languageOptions = listOf("English", "Spanish", "Hindi", "French", "German", "Mandarin", "Other")

    // ==========================================
    // STEP 2 STATE - Target Exam & Goals
    // ==========================================
    val examPresets = listOf(
        "JEE (Main & Advanced)", "NEET UG", "SAT / ACT",
        "AP / IB Diploma", "GCSE / A-Levels", "GRE / GMAT",
        "UPSC / Civil Services", "College Semester Exams", "Custom Exam"
    )
    var selectedExamPreset by remember { mutableStateOf("JEE (Main & Advanced)") }
    var customExamName by remember { mutableStateOf("") }
    var examDaysAhead by remember { mutableFloatStateOf(60f) }
    var targetScoreInput by remember { mutableStateOf("Top 500 AIR / 99%ile") }

    // ==========================================
    // STEP 3 STATE - Subjects & Priorities
    // ==========================================
    val defaultSubjectOptions = listOf(
        "Mathematics", "Physics", "Chemistry", "Biology",
        "Computer Science", "Economics", "History", "Literature"
    )
    var selectedSubjects by remember { mutableStateOf(setOf("Mathematics", "Physics", "Chemistry")) }
    var customSubjectText by remember { mutableStateOf("") }
    
    // Priority per subject (Map: Subject -> "HIGH" | "MEDIUM" | "LOW")
    var subjectPriorityMap by remember {
        mutableStateOf(
            mapOf(
                "Mathematics" to "HIGH",
                "Physics" to "HIGH",
                "Chemistry" to "MEDIUM"
            )
        )
    }

    val prepLevels = listOf(
        "Beginner (Starting from basics / fresh chapters)",
        "Intermediate (Concepts understood, solving question sets)",
        "Advanced (Speed drills, full-length mocks & rapid revision)"
    )
    var selectedPrepLevel by remember { mutableStateOf("Intermediate (Concepts understood, solving question sets)") }

    // ==========================================
    // STEP 4 STATE - Study Time & Schedule
    // ==========================================
    var availableStudyHours by remember { mutableFloatStateOf(3.5f) }
    var preferredStartTime by remember { mutableStateOf("06:00 PM") }
    var customStartTime by remember { mutableStateOf("") }
    var preferredEndTime by remember { mutableStateOf("10:00 PM") }
    var customEndTime by remember { mutableStateOf("") }
    var selectedStudyDays by remember { mutableStateOf(setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) }
    var breakDurationMinutes by remember { mutableIntStateOf(15) }

    val startTimeOptions = listOf("06:00 AM", "08:00 AM", "10:00 AM", "02:00 PM", "06:00 PM", "08:00 PM", "Custom")
    val endTimeOptions = listOf("09:00 AM", "12:00 PM", "05:00 PM", "08:00 PM", "10:00 PM", "01:00 AM", "Custom")
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val breakOptions = listOf(5, 10, 15, 20, 30)

    // ==========================================
    // STEP 5 STATE - Weak Topics & Daily Goal
    // ==========================================
    var selectedWeakSubjects by remember { mutableStateOf(setOf("Chemistry")) }
    var weakTopicInput by remember { mutableStateOf("") }
    var weakTopicsList by remember {
        mutableStateOf(listOf("Organic Reaction Mechanisms", "Rotational Dynamics", "Integration by Parts"))
    }
    
    val dailyGoalPresets = listOf(
        "Complete scheduled topics & 20 flashcards",
        "3 hours deep focus timer + 1 practice quiz",
        "Solve 40 MCQs & review formula mistakes",
        "Complete high-yield chapters + active recall"
    )
    var selectedDailyGoal by remember { mutableStateOf("Complete scheduled topics & 20 flashcards") }
    var customDailyGoal by remember { mutableStateOf("") }

    // Calculate effective values
    val effectiveGrade = if (selectedGrade == "Custom" && customGradeInput.isNotBlank()) customGradeInput.trim() else selectedGrade
    val effectiveLanguage = if (selectedLanguage == "Other" && customLanguageInput.isNotBlank()) customLanguageInput.trim() else selectedLanguage
    val effectiveExamName = if (selectedExamPreset == "Custom Exam" || customExamName.isNotBlank()) {
        if (customExamName.isNotBlank()) customExamName.trim() else selectedExamPreset
    } else selectedExamPreset
    val effectiveStartTime = if (preferredStartTime == "Custom" && customStartTime.isNotBlank()) customStartTime.trim() else preferredStartTime
    val effectiveEndTime = if (preferredEndTime == "Custom" && customEndTime.isNotBlank()) customEndTime.trim() else preferredEndTime
    val effectiveDailyGoal = if (customDailyGoal.isNotBlank()) customDailyGoal.trim() else selectedDailyGoal
    val effectivePhotoUrl = if (customPhotoUrl.isNotBlank()) customPhotoUrl.trim() else presetAvatars.getOrNull(selectedAvatarIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(18.dp)
            .testTag("onboarding_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==========================================
            // Top Navigation & Step Indicator
            // ==========================================
            Column {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (step > 1) {
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

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Step $step of $totalSteps",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (step) {
                                1 -> "Personal & Language"
                                2 -> "Target Exam & Date"
                                3 -> "Subjects & Priorities"
                                4 -> "Schedule & Times"
                                else -> "Weak Areas & Goals"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    if (step < totalSteps) {
                        TextButton(
                            onClick = { step++ },
                            modifier = Modifier.testTag("onboarding_skip_button")
                        ) {
                            Text("Next", color = GoldenSpark, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
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

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // Main Step Card
            // ==========================================
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = 12.dp,
                fillAlpha = 0.82f
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        // ----------------------------------------------------
                        // STEP 1: Name, Avatar, Class/Level, Language
                        // ----------------------------------------------------
                        1 -> {
                            Text(
                                text = "Welcome to StudyMate AI 👋",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Personalize your scholar profile and language preference",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Avatar Selection
                            Text(
                                text = "Choose Profile Avatar (Optional):",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                presetAvatars.forEachIndexed { index, avatarUrl ->
                                    val isSelected = selectedAvatarIndex == index && customPhotoUrl.isBlank()
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) NeonCyan.copy(alpha = 0.3f) else Color(0x20FFFFFF))
                                            .border(
                                                2.dp,
                                                if (isSelected) NeonCyan else Color.Transparent,
                                                CircleShape
                                            )
                                            .springClickable {
                                                selectedAvatarIndex = index
                                                customPhotoUrl = ""
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = "Avatar $index",
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Name Field
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Your Full Name or Nickname") },
                                placeholder = { Text("e.g. Alex Hunter") },
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
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = {
                                    Icon(Icons.Filled.Person, null, tint = NeonCyan)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Class / College Level
                            Text(
                                text = "Class / Academic Level:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                classLevels.chunked(2).forEach { rowLevels ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowLevels.forEach { lvl ->
                                            val isSelected = selectedGrade == lvl
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(42.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) NeonCyan else Color(0x18FFFFFF))
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) Color.White else Color(0x25FFFFFF),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .springClickable { selectedGrade = lvl }
                                                    .padding(horizontal = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = lvl,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) Color(0xFF070B19) else Color.White,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (selectedGrade == "Custom") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customGradeInput,
                                    onValueChange = { customGradeInput = it },
                                    label = { Text("Enter your custom class/level/stream") },
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

                            Spacer(modifier = Modifier.height(16.dp))

                            // Language Preference
                            Text(
                                text = "Preferred Learning Language:",
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
                                languageOptions.take(4).forEach { lang ->
                                    val isSelected = selectedLanguage == lang
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) ElectricViolet else Color(0x18FFFFFF))
                                            .springClickable { selectedLanguage = lang },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lang,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                languageOptions.drop(4).forEach { lang ->
                                    val isSelected = selectedLanguage == lang
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) ElectricViolet else Color(0x18FFFFFF))
                                            .springClickable { selectedLanguage = lang },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lang,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            if (selectedLanguage == "Other") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customLanguageInput,
                                    onValueChange = { customLanguageInput = it },
                                    label = { Text("Specify your language") },
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

                        // ----------------------------------------------------
                        // STEP 2: Exam selection, Custom exam, Date, Target Score
                        // ----------------------------------------------------
                        2 -> {
                            Text(
                                text = "Target Exam & Countdown 🎯",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select or enter your goal exam, date, and target score/rank",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Select Exam Program:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                examPresets.chunked(3).forEach { rowExams ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowExams.forEach { exam ->
                                            val isSelected = selectedExamPreset == exam
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) GoldenSpark else Color(0x18FFFFFF))
                                                    .springClickable {
                                                        selectedExamPreset = exam
                                                        if (exam != "Custom Exam") customExamName = exam
                                                    }
                                                    .padding(horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = exam,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) Color(0xFF070B19) else Color.White,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom Exam Name Input Field
                            OutlinedTextField(
                                value = customExamName,
                                onValueChange = { customExamName = it },
                                label = { Text("Exam Name (or type custom)") },
                                placeholder = { Text("e.g. JEE Advanced 2026, MCAT, CBSE Board") },
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
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = {
                                    Icon(Icons.Filled.School, null, tint = GoldenSpark)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Exam Date & Countdown Slider
                            val targetDateFormatted = remember(examDaysAhead) {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, examDaysAhead.toInt())
                                SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.time)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Days until Exam:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "${examDaysAhead.toInt()} Days ($targetDateFormatted)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldenSpark
                                )
                            }

                            Slider(
                                value = examDaysAhead,
                                onValueChange = { examDaysAhead = it },
                                valueRange = 7f..365f,
                                steps = 50,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldenSpark,
                                    activeTrackColor = GoldenSpark,
                                    inactiveTrackColor = Color(0x30FFFFFF)
                                )
                            )

                            // Quick Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(30, 60, 90, 180, 365).forEach { d ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (examDaysAhead.toInt() == d) GoldenSpark else Color(0x18FFFFFF),
                                        modifier = Modifier
                                            .weight(1f)
                                            .springClickable { examDaysAhead = d.toFloat() }
                                    ) {
                                        Text(
                                            text = "${d}d",
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                            color = if (examDaysAhead.toInt() == d) Color(0xFF070B19) else Color(0xFFCBD5E1),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Target Score (Optional)
                            OutlinedTextField(
                                value = targetScoreInput,
                                onValueChange = { targetScoreInput = it },
                                label = { Text("Target Score / Rank Goal (Optional)") },
                                placeholder = { Text("e.g. 99.5%ile / Top 100 Rank / Grade A*") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x40FFFFFF)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = {
                                    Icon(Icons.Filled.EmojiEvents, null, tint = GoldenSpark)
                                }
                            )
                        }

                        // ----------------------------------------------------
                        // STEP 3: Subjects, Custom Subjects, Priority, Prep Level
                        // ----------------------------------------------------
                        3 -> {
                            Text(
                                text = "Subjects & Priorities 📚",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select subjects, add custom topics, and assign study priority",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Subject List with Priority Selectors
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val allSubjects = (defaultSubjectOptions + selectedSubjects.filter { it !in defaultSubjectOptions }).distinct()

                                allSubjects.forEach { subject ->
                                    val isSelected = selectedSubjects.contains(subject)
                                    val priority = subjectPriorityMap[subject] ?: "MEDIUM"

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0x2838BDF8) else Color(0x14FFFFFF),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) NeonCyan else Color(0x20FFFFFF)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        val next = selectedSubjects.toMutableSet()
                                                        if (checked) next.add(subject) else next.remove(subject)
                                                        selectedSubjects = next
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = NeonCyan,
                                                        checkmarkColor = Color(0xFF070B19)
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = subject,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }

                                            if (isSelected) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    listOf("HIGH" to "🔥 High", "MEDIUM" to "⚡ Med", "LOW" to "📘 Low").forEach { (pKey, pLabel) ->
                                                        val isCurr = priority == pKey
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = if (isCurr) {
                                                                if (pKey == "HIGH") CoralRose else if (pKey == "MEDIUM") GoldenSpark else EmeraldSuccess
                                                            } else Color(0x18FFFFFF),
                                                            modifier = Modifier.springClickable {
                                                                val nextMap = subjectPriorityMap.toMutableMap()
                                                                nextMap[subject] = pKey
                                                                subjectPriorityMap = nextMap
                                                            }
                                                        ) {
                                                            Text(
                                                                text = pLabel,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = if (isCurr) Color(0xFF070B19) else Color(0xFFE2E8F0),
                                                                fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Normal,
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Add Custom Subject Input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customSubjectText,
                                        onValueChange = { customSubjectText = it },
                                        label = { Text("Add Custom Subject / Module") },
                                        placeholder = { Text("e.g. Organic Chemistry, Microeconomics") },
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
                                        onClick = {
                                            if (customSubjectText.isNotBlank()) {
                                                val s = customSubjectText.trim()
                                                selectedSubjects = selectedSubjects + s
                                                val nextMap = subjectPriorityMap.toMutableMap()
                                                nextMap[s] = "HIGH"
                                                subjectPriorityMap = nextMap
                                                customSubjectText = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text("+ Add", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Current Preparation Level
                            Text(
                                text = "Current Preparation Level:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                prepLevels.forEach { prep ->
                                    val isSelected = selectedPrepLevel == prep
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) ElectricViolet else Color(0x18FFFFFF))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color.White else Color(0x25FFFFFF),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .springClickable { selectedPrepLevel = prep }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = prep,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // STEP 4: Daily Study Time, Start/End Time, Days, Breaks
                        // ----------------------------------------------------
                        4 -> {
                            Text(
                                text = "Study Schedule & Timing ⏱️",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Set sustainable study hours, preferred time slots, and breaks",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Daily Target Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Available Study Time:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "${availableStudyHours} hrs (${(availableStudyHours * 60).toInt()} mins)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }

                            Slider(
                                value = availableStudyHours,
                                onValueChange = { availableStudyHours = (it * 2).toInt() / 2f },
                                valueRange = 1f..12f,
                                steps = 21,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan,
                                    inactiveTrackColor = Color(0x30FFFFFF)
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Preferred Start Time
                            Text(
                                text = "Preferred Study Start Time:",
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
                                startTimeOptions.take(4).forEach { time ->
                                    val isSelected = preferredStartTime == time
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) GoldenSpark else Color(0x18FFFFFF))
                                            .springClickable { preferredStartTime = time },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = time,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color(0xFF070B19) else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                startTimeOptions.drop(4).forEach { time ->
                                    val isSelected = preferredStartTime == time
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) GoldenSpark else Color(0x18FFFFFF))
                                            .springClickable { preferredStartTime = time },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = time,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color(0xFF070B19) else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            if (preferredStartTime == "Custom") {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customStartTime,
                                    onValueChange = { customStartTime = it },
                                    label = { Text("Enter custom start time (e.g. 07:30 AM)") },
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

                            // Preferred End Time
                            Text(
                                text = "Preferred Study End Time:",
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
                                endTimeOptions.take(4).forEach { time ->
                                    val isSelected = preferredEndTime == time
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) CoralRose else Color(0x18FFFFFF))
                                            .springClickable { preferredEndTime = time },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = time,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                endTimeOptions.drop(4).forEach { time ->
                                    val isSelected = preferredEndTime == time
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) CoralRose else Color(0x18FFFFFF))
                                            .springClickable { preferredEndTime = time },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = time,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            if (preferredEndTime == "Custom") {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customEndTime,
                                    onValueChange = { customEndTime = it },
                                    label = { Text("Enter custom end time (e.g. 11:30 PM)") },
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

                            // Study Days Multi-select
                            Text(
                                text = "Active Study Days (${selectedStudyDays.size} selected):",
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
                                daysOfWeek.forEach { day ->
                                    val isSelected = selectedStudyDays.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) NeonCyan else Color(0x18FFFFFF))
                                            .springClickable {
                                                val next = selectedStudyDays.toMutableSet()
                                                if (isSelected) next.remove(day) else next.add(day)
                                                selectedStudyDays = next
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF070B19) else Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Break Duration
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                breakOptions.forEach { bMin ->
                                    val isSelected = breakDurationMinutes == bMin
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) EmeraldSuccess else Color(0x18FFFFFF))
                                            .springClickable { breakDurationMinutes = bMin },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${bMin}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color(0xFF070B19) else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // STEP 5: Weak Subjects/Topics, Daily Study Goal, Final Summary
                        // ----------------------------------------------------
                        5 -> {
                            Text(
                                text = "Weak Areas & Daily Goal 🚀",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pinpoint weak topics for AI active recall & set your daily target",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Weak Subject Chips
                            Text(
                                text = "Subjects Needing Most Attention:",
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
                                selectedSubjects.forEach { s ->
                                    val isWeak = selectedWeakSubjects.contains(s)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isWeak) CoralRose else Color(0x18FFFFFF),
                                        modifier = Modifier.springClickable {
                                            val next = selectedWeakSubjects.toMutableSet()
                                            if (isWeak) next.remove(s) else next.add(s)
                                            selectedWeakSubjects = next
                                        }
                                    ) {
                                        Text(
                                            text = "$s ${if (isWeak) "🔥" else ""}",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isWeak) Color.White else Color(0xFFCBD5E1),
                                            fontWeight = if (isWeak) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Weak Topic Custom Tags
                            Text(
                                text = "Specific Difficult Topics / Chapters:",
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
                                weakTopicsList.chunked(2).forEach { rowTopics ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
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
                                                    Text(
                                                        text = topic,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = "Remove",
                                                        tint = CoralRose,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .springClickable {
                                                                weakTopicsList = weakTopicsList - topic
                                                            }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Add custom weak topic
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = weakTopicInput,
                                        onValueChange = { weakTopicInput = it },
                                        label = { Text("Add weak topic (e.g. Thermodynamics)") },
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
                                        onClick = {
                                            if (weakTopicInput.isNotBlank()) {
                                                weakTopicsList = weakTopicsList + weakTopicInput.trim()
                                                weakTopicInput = ""
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

                            Spacer(modifier = Modifier.height(14.dp))

                            // Daily Study Goal
                            Text(
                                text = "Daily Study Goal:",
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
                                dailyGoalPresets.forEach { goal ->
                                    val isSelected = selectedDailyGoal == goal
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) GoldenSpark else Color(0x18FFFFFF))
                                            .springClickable {
                                                selectedDailyGoal = goal
                                                customDailyGoal = ""
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = goal,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) Color(0xFF070B19) else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = customDailyGoal,
                                onValueChange = { customDailyGoal = it },
                                label = { Text("Or type your custom daily goal") },
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

                            Spacer(modifier = Modifier.height(14.dp))

                            // Final Blueprint Summary Box
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x1838BDF8),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "📋 Your Personalized Study Blueprint",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    Text(text = "👤 Scholar: ${nameInput.ifBlank { "Student" }} • $effectiveGrade ($effectiveLanguage)", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    Text(text = "🎯 Exam: $effectiveExamName (${examDaysAhead.toInt()} days left)", color = GoldenSpark, style = MaterialTheme.typography.labelSmall)
                                    Text(text = "📚 Subjects: ${selectedSubjects.joinToString(", ")}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    Text(text = "⏱️ Time: ${availableStudyHours} hrs/day ($effectiveStartTime – $effectiveEndTime, ${breakDurationMinutes}m break)", color = Color(0xFFE2E8F0), style = MaterialTheme.typography.labelSmall)
                                    Text(text = "🔥 Weak Focus: ${if (selectedWeakSubjects.isNotEmpty()) selectedWeakSubjects.joinToString(", ") else "None specified"} (${weakTopicsList.size} topics tagged)", color = CoralRose, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // Bottom Action Buttons
            // ==========================================
            if (step < totalSteps) {
                GlassButton(
                    text = "Continue to Step ${step + 1}",
                    onClick = { step++ },
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    isPrimary = true,
                    testTag = "onboarding_next_button"
                )
            } else {
                val finalExamDateMillis = System.currentTimeMillis() + (examDaysAhead.toLong() * 24L * 60L * 60L * 1000L)
                val highPriority = subjectPriorityMap.filter { it.value == "HIGH" }.keys.toList()
                val medPriority = subjectPriorityMap.filter { it.value == "MEDIUM" }.keys.toList()
                val lowPriority = subjectPriorityMap.filter { it.value == "LOW" }.keys.toList()

                val finalProfile = UserProfile(
                    id = "current_user",
                    name = nameInput.ifBlank { "Student" },
                    photoUrl = effectivePhotoUrl,
                    grade = effectiveGrade,
                    educationLevel = effectiveGrade,
                    languagePreference = effectiveLanguage,
                    examCategory = selectedExamPreset,
                    examName = effectiveExamName,
                    examDateMillis = finalExamDateMillis,
                    targetScore = targetScoreInput.ifBlank { "Top Percentile" },
                    goal = effectiveExamName,
                    subjects = if (selectedSubjects.isNotEmpty()) selectedSubjects.toList() else listOf("Mathematics", "Physics", "Chemistry"),
                    highPrioritySubjects = highPriority,
                    mediumPrioritySubjects = medPriority,
                    lowPrioritySubjects = lowPriority,
                    strongSubjects = selectedSubjects.filter { it !in selectedWeakSubjects },
                    preparationLevel = selectedPrepLevel,
                    dailyTargetMinutes = (availableStudyHours * 60).toInt(),
                    availableStudyHours = availableStudyHours,
                    preferredStudyStartTime = effectiveStartTime,
                    preferredStudyEndTime = effectiveEndTime,
                    preferredStudyDays = selectedStudyDays.toList(),
                    breakDurationMinutes = breakDurationMinutes,
                    preferredStudyTime = "$effectiveStartTime - $effectiveEndTime",
                    weakSubjects = selectedWeakSubjects.toList(),
                    weakTopics = weakTopicsList,
                    dailyStudyGoal = effectiveDailyGoal,
                    shortTermGoal = "Master $effectiveExamName high-yield topics & complete daily $effectiveDailyGoal",
                    longTermGoal = "Crack $effectiveExamName with $targetScoreInput and achieve admission",
                    notificationsEnabled = true,
                    isOnboardingCompleted = true
                )

                GlassButton(
                    text = "Launch My AI Study Blueprint 🚀",
                    onClick = { onComplete(finalProfile) },
                    icon = Icons.Filled.Check,
                    isPrimary = true,
                    testTag = "start_study_plan_button"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

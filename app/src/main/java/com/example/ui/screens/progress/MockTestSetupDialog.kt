package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestSetupDialog(
    userProfile: UserProfile?,
    userMaterials: List<UserQuestionMaterial>,
    onDismiss: () -> Unit,
    onStartTest: (MockTestConfig) -> Unit,
    onManageMaterials: () -> Unit
) {
    val defaultExam = userProfile?.examName?.ifBlank { "RRB NTPC (Railway)" } ?: "RRB NTPC (Railway)"

    var selectedExam by remember { mutableStateOf(defaultExam) }
    var selectedQuestionSource by remember { mutableStateOf(QuestionSourceType.PYQ) }
    var selectedTestType by remember { mutableStateOf(MockTestType.FULL_MOCK) }
    var selectedSubject by remember { mutableStateOf("All Subjects") }
    var selectedChapter by remember { mutableStateOf("All Chapters") }
    var selectedTopic by remember { mutableStateOf("All Topics") }
    var selectedPyqYear by remember { mutableStateOf("All Years") }
    var selectedPyqShift by remember { mutableStateOf("All Shifts") }
    var selectedLanguage by remember { mutableStateOf(userProfile?.languagePreference ?: "English") }
    var selectedDifficulty by remember { mutableStateOf("Medium") }
    var questionCount by remember { mutableIntStateOf(20) }
    var timeLimitMinutes by remember { mutableIntStateOf(25) }
    var selectedCustomMaterialId by remember { mutableStateOf<Long?>(null) }

    val examOptions = listOf(
        "RRB NTPC (Railway)",
        "SSC CGL Tier-1",
        "JEE Main & Advanced",
        "NEET-UG",
        "UPSC CSE Prelims",
        "CBSE Class 12 Boards"
    )

    val isRailway = selectedExam.contains("Railway", ignoreCase = true) || selectedExam.contains("RRB", ignoreCase = true)
    val isSsc = selectedExam.contains("SSC", ignoreCase = true) || selectedExam.contains("CGL", ignoreCase = true)

    val subjectOptions = when {
        isRailway -> listOf("All Subjects", "Mathematics", "General Intelligence & Reasoning", "General Awareness")
        isSsc -> listOf("All Subjects", "Quantitative Aptitude", "General Intelligence & Reasoning", "English Comprehension", "General Awareness")
        selectedExam.contains("JEE", ignoreCase = true) -> listOf("All Subjects", "Physics", "Chemistry", "Mathematics")
        selectedExam.contains("NEET", ignoreCase = true) -> listOf("All Subjects", "Physics", "Chemistry", "Biology")
        selectedExam.contains("UPSC", ignoreCase = true) -> listOf("All Subjects", "General Studies Paper 1", "CSAT Paper 2")
        else -> (listOf("All Subjects") + (userProfile?.subjects ?: listOf("Mathematics", "Physics", "General Studies"))).distinct()
    }

    val chapterOptions = when (selectedSubject) {
        "Mathematics", "Quantitative Aptitude" -> listOf("All Chapters", "Number System", "Arithmetic", "Algebra", "Geometry & Mensuration", "Trigonometry")
        "General Intelligence & Reasoning" -> listOf("All Chapters", "Analogies & Classification", "Coding-Decoding", "Syllogism", "Blood Relations", "Direction Sense")
        "General Awareness", "General Studies Paper 1" -> listOf("All Chapters", "Modern Indian History", "Indian Polity & Constitution", "Geography", "General Science", "Economy")
        "Physics" -> listOf("All Chapters", "Electrostatics & Current", "Mechanics & Laws of Motion", "Optics", "Thermodynamics", "Modern Physics")
        "Chemistry" -> listOf("All Chapters", "Chemical Bonding", "Organic Chemistry Basics", "Coordination Compounds", "Electrochemistry")
        "Biology" -> listOf("All Chapters", "Genetics & Evolution", "Human Physiology", "Plant Physiology", "Cell Structure & Function")
        else -> listOf("All Chapters", "Core Concepts", "Previous Year Topics", "High Yield Concepts")
    }

    val pyqYears = listOf("All Years", "2024", "2023", "2022", "2021", "2019", "2018")
    val pyqShifts = listOf("All Shifts", "Shift 1 (Morning)", "Shift 2 (Afternoon)", "Shift 3 (Evening)")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE0050814))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .testTag("mock_test_setup_dialog"),
                shape = RoundedCornerShape(24.dp),
                fillAlpha = 0.95f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(NeonCyan, ElectricViolet))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Quiz, null, tint = Color(0xFF050814), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Configure CBT Test",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Authentic PYQs & Timed Exam Drills",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_mock_setup_btn")
                        ) {
                            Icon(Icons.Filled.Close, "Close", tint = Color(0xFF94A3B8))
                        }
                    }

                    HorizontalDivider(color = Color(0x20FFFFFF), modifier = Modifier.padding(vertical = 10.dp))

                    // Scrollable Configuration Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Target Exam Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ConfigSectionTitle(icon = Icons.Filled.School, title = "Target Examination")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(examOptions) { exam ->
                                    val isSelected = selectedExam == exam
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x14FFFFFF),
                                        border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0x25FFFFFF)),
                                        modifier = Modifier.springClickable {
                                            selectedExam = exam
                                            selectedSubject = "All Subjects"
                                            selectedChapter = "All Chapters"
                                        }
                                    ) {
                                        Text(
                                            text = exam,
                                            color = if (isSelected) NeonCyan else Color(0xFFE2E8F0),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Question Source Engine Selector (PYQ, Chapter Practice, Exam Pattern, Current Affairs, Mixed)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ConfigSectionTitle(icon = Icons.Filled.Source, title = "Question Source Engine")
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                QuestionSourceOptionCard(
                                    title = "🎯 Authentic Previous Year Questions (PYQs)",
                                    description = "Real, verified exam questions tagged by official year & shift",
                                    badgeText = "Verified Official",
                                    isSelected = selectedQuestionSource == QuestionSourceType.PYQ,
                                    accentColor = GoldenSpark,
                                    onClick = { selectedQuestionSource = QuestionSourceType.PYQ }
                                )

                                QuestionSourceOptionCard(
                                    title = "📖 Chapter-wise Practice Drill",
                                    description = "Target specific chapters and syllabus topics with structured MCQs",
                                    badgeText = "Syllabus Focused",
                                    isSelected = selectedQuestionSource == QuestionSourceType.CHAPTER_PRACTICE,
                                    accentColor = ElectricViolet,
                                    onClick = { selectedQuestionSource = QuestionSourceType.CHAPTER_PRACTICE }
                                )

                                QuestionSourceOptionCard(
                                    title = "⚡ Full Exam Pattern Simulation",
                                    description = "Standard exam weightage, section ratio, and real negative marking",
                                    badgeText = "Real CBT",
                                    isSelected = selectedQuestionSource == QuestionSourceType.EXAM_PATTERN,
                                    accentColor = NeonCyan,
                                    onClick = { selectedQuestionSource = QuestionSourceType.EXAM_PATTERN }
                                )

                                QuestionSourceOptionCard(
                                    title = "📰 Current Affairs 2024 & GK",
                                    description = "Latest national, international, science & sports events for exams",
                                    badgeText = "2024 Updated",
                                    isSelected = selectedQuestionSource == QuestionSourceType.CURRENT_AFFAIRS,
                                    accentColor = Color(0xFF38BDF8),
                                    onClick = { selectedQuestionSource = QuestionSourceType.CURRENT_AFFAIRS }
                                )
                            }
                        }

                        // 3. PYQ Specific Filters (Year & Shift)
                        if (selectedQuestionSource == QuestionSourceType.PYQ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GoldenSpark.copy(alpha = 0.08f))
                                    .border(1.dp, GoldenSpark.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "PYQ Filter: Exam Year & Shift",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldenSpark
                                )

                                // Year chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(pyqYears) { yr ->
                                        val isSel = selectedPyqYear == yr
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) GoldenSpark else Color(0x18FFFFFF),
                                            modifier = Modifier.springClickable { selectedPyqYear = yr }
                                        ) {
                                            Text(
                                                text = yr,
                                                color = if (isSel) Color(0xFF050814) else Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }

                                // Shift chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(pyqShifts) { shift ->
                                        val isSel = selectedPyqShift == shift
                                        val shortShift = shift.split(" ").take(2).joinToString(" ")
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) GoldenSpark else Color(0x18FFFFFF),
                                            modifier = Modifier.springClickable { selectedPyqShift = shift }
                                        ) {
                                            Text(
                                                text = shortShift,
                                                color = if (isSel) Color(0xFF050814) else Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Subject & Chapter Cascading Filters
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ConfigSectionTitle(icon = Icons.Filled.MenuBook, title = "Subject & Chapter Scope")
                            
                            // Subject Selector
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(subjectOptions) { subj ->
                                    val isSelected = selectedSubject == subj
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) ElectricViolet.copy(alpha = 0.25f) else Color(0x14FFFFFF),
                                        border = BorderStroke(1.dp, if (isSelected) ElectricViolet else Color(0x25FFFFFF)),
                                        modifier = Modifier.springClickable {
                                            selectedSubject = subj
                                            selectedChapter = "All Chapters"
                                        }
                                    ) {
                                        Text(
                                            text = subj,
                                            color = if (isSelected) ElectricViolet else Color(0xFFE2E8F0),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            // Chapter Selector
                            if (selectedSubject != "All Subjects") {
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(chapterOptions) { ch ->
                                        val isSel = selectedChapter == ch
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) Color(0xFF8B5CF6) else Color(0x14FFFFFF),
                                            modifier = Modifier.springClickable { selectedChapter = ch }
                                        ) {
                                            Text(
                                                text = ch,
                                                color = if (isSel) Color.White else Color(0xFFCBD5E1),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Test Parameters: Language, Difficulty, Question Count, Time Limit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Language
                            Column(modifier = Modifier.weight(1f)) {
                                ConfigSectionTitle(icon = Icons.Filled.Language, title = "Language")
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("English", "Hindi").forEach { lang ->
                                        val isSel = selectedLanguage.equals(lang, ignoreCase = true)
                                        val label = if (lang == "Hindi") "हिंदी" else "English"
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) NeonCyan else Color(0x18FFFFFF))
                                                .springClickable { selectedLanguage = lang },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSel) Color(0xFF050814) else Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Difficulty
                            Column(modifier = Modifier.weight(1f)) {
                                ConfigSectionTitle(icon = Icons.Filled.Speed, title = "Difficulty")
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("Easy", "Medium", "Hard").forEach { diff ->
                                        val isSel = selectedDifficulty == diff
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) ElectricViolet else Color(0x18FFFFFF))
                                                .springClickable { selectedDifficulty = diff },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = diff,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Question Count & Duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Question Count
                            Column(modifier = Modifier.weight(1f)) {
                                ConfigSectionTitle(icon = Icons.Filled.FormatListNumbered, title = "Questions: $questionCount")
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(10, 20, 25, 50).forEach { cnt ->
                                        val isSel = questionCount == cnt
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) NeonCyan else Color(0x18FFFFFF))
                                                .springClickable {
                                                    questionCount = cnt
                                                    timeLimitMinutes = when (cnt) {
                                                        10 -> 12
                                                        20 -> 25
                                                        25 -> 30
                                                        50 -> 60
                                                        else -> 30
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$cnt",
                                                color = if (isSel) Color(0xFF050814) else Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Time limit
                            Column(modifier = Modifier.weight(1f)) {
                                ConfigSectionTitle(icon = Icons.Filled.Timer, title = "Timer: ${timeLimitMinutes}m")
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(10, 15, 25, 45, 60).forEach { mins ->
                                        val isSel = timeLimitMinutes == mins
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) CoralRose else Color(0x18FFFFFF))
                                                .springClickable { timeLimitMinutes = mins },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${mins}m",
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Launch CTA Button
                    GlassButton(
                        text = "🚀 Launch CBT Test ($questionCount Qs • ${timeLimitMinutes} mins)",
                        onClick = {
                            val config = MockTestConfig(
                                exam = selectedExam,
                                testType = selectedTestType,
                                questionSource = selectedQuestionSource,
                                subject = selectedSubject,
                                chapter = selectedChapter,
                                topic = if (selectedChapter != "All Chapters") selectedChapter else selectedTopic,
                                pyqYear = selectedPyqYear,
                                pyqShift = selectedPyqShift,
                                difficulty = selectedDifficulty,
                                language = selectedLanguage,
                                questionCount = questionCount,
                                timeLimitMinutes = timeLimitMinutes,
                                customMaterialId = selectedCustomMaterialId
                            )
                            onStartTest(config)
                        },
                        isPrimary = true,
                        testTag = "start_configured_mock_test_btn"
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigSectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun QuestionSourceOptionCard(
    title: String,
    description: String,
    badgeText: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color(0x12FFFFFF))
            .border(
                1.5.dp,
                if (isSelected) accentColor else Color(0x20FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .springClickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) accentColor else Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = accentColor.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
            ) {
                Text(
                    text = badgeText,
                    color = accentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

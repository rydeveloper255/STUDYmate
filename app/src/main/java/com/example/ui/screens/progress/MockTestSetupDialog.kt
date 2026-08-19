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
    var selectedTestType by remember { mutableStateOf(MockTestType.FULL_MOCK) }
    var selectedSubject by remember { mutableStateOf("All Subjects") }
    var selectedTopic by remember { mutableStateOf("All Topics") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var selectedDifficulty by remember { mutableStateOf("Medium") }
    var questionCount by remember { mutableIntStateOf(25) }
    var timeLimitMinutes by remember { mutableIntStateOf(30) }
    var selectedSourceFilter by remember { mutableStateOf(QuestionSourceFilter.BALANCED_MIX) }
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

    val topicSuggestions = when (selectedSubject) {
        "Mathematics" -> listOf("All Topics", "Speed, Distance & Time", "Compound Interest", "Time & Work", "Averages & Percentages")
        "General Intelligence & Reasoning" -> listOf("All Topics", "Analogies", "Coding-Decoding", "Syllogism", "Blood Relations")
        "General Awareness" -> listOf("All Topics", "Railway GK & History", "Indian Polity & Governance", "General Science - Physics")
        "Physics" -> listOf("All Topics", "Electrostatics", "Current Electricity", "Mechanics", "Optics")
        "Chemistry" -> listOf("All Topics", "Coordination Compounds", "Chemical Bonding", "Organic Reactions")
        "Biology" -> listOf("All Topics", "Cell Structure & Function", "Genetics & Evolution", "Human Physiology")
        else -> listOf("All Topics", "High Yield Concepts", "Core Syllabus", "Past Exam Questions")
    }

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
                    .fillMaxHeight(0.92f)
                    .testTag("mock_test_setup_dialog"),
                shape = RoundedCornerShape(24.dp),
                fillAlpha = 0.95f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
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
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(NeonCyan, ElectricViolet))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Quiz, null, tint = Color(0xFF050814), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Configure Mock Test",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Timed, authentic exam simulation",
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

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color(0x20FFFFFF)
                    )

                    // Scrollable Config Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Target Exam
                        ConfigSectionTitle(icon = Icons.Filled.School, title = "Target Exam")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(examOptions) { exam ->
                                val isSel = selectedExam == exam
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) NeonCyan else Color(0x18FFFFFF),
                                    border = if (isSel) null else BorderStroke(1.dp, Color(0x20FFFFFF)),
                                    modifier = Modifier.springClickable(testTag = "setup_exam_${exam.take(4)}") {
                                        selectedExam = exam
                                    }
                                ) {
                                    Text(
                                        text = exam,
                                        color = if (isSel) Color(0xFF050814) else Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // 2. Subject
                        ConfigSectionTitle(icon = Icons.Filled.MenuBook, title = "Subject")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(subjectOptions) { sub ->
                                val isSel = selectedSubject.equals(sub, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) ElectricViolet else Color(0x18FFFFFF),
                                    border = if (isSel) null else BorderStroke(1.dp, Color(0x20FFFFFF)),
                                    modifier = Modifier.springClickable(testTag = "setup_sub_$sub") {
                                        selectedSubject = sub
                                    }
                                ) {
                                    Text(
                                        text = sub,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // 3. Topic / Chapter
                        ConfigSectionTitle(icon = Icons.Filled.Topic, title = "Chapter / Topic")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(topicSuggestions) { topic ->
                                val isSel = selectedTopic == topic
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) GoldenSpark else Color(0x18FFFFFF),
                                    border = if (isSel) null else BorderStroke(1.dp, Color(0x20FFFFFF)),
                                    modifier = Modifier.springClickable {
                                        selectedTopic = topic
                                    }
                                ) {
                                    Text(
                                        text = topic,
                                        color = if (isSel) Color(0xFF050814) else Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // 4. Question Source Selection (CRITICAL)
                        ConfigSectionTitle(icon = Icons.Filled.Source, title = "Question Source & Authenticity")
                        Text(
                            text = "Every question is labeled with its verified origin.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuestionSourceOptionCard(
                                title = "⚖️ Balanced Mix (Recommended)",
                                description = "50% authentic Previous-Year Questions + 50% AI Practice questions",
                                badgeText = "PYQ + AI",
                                isSelected = selectedSourceFilter == QuestionSourceFilter.BALANCED_MIX,
                                accentColor = NeonCyan,
                                onClick = {
                                    selectedSourceFilter = QuestionSourceFilter.BALANCED_MIX
                                    selectedCustomMaterialId = null
                                }
                            )

                            QuestionSourceOptionCard(
                                title = "🏛️ Official Previous-Year Questions Only",
                                description = "Authentic questions from JEE, NEET, CBSE Boards & SAT archives",
                                badgeText = "Previous Year",
                                isSelected = selectedSourceFilter == QuestionSourceFilter.PREVIOUS_YEAR_ONLY,
                                accentColor = GoldenSpark,
                                onClick = {
                                    selectedSourceFilter = QuestionSourceFilter.PREVIOUS_YEAR_ONLY
                                    selectedCustomMaterialId = null
                                }
                            )

                            QuestionSourceOptionCard(
                                title = "🤖 AI-Generated Practice Only",
                                description = "Tailored exam-pattern questions generated fresh by Gemini AI",
                                badgeText = "AI Generated",
                                isSelected = selectedSourceFilter == QuestionSourceFilter.AI_GENERATED_ONLY,
                                accentColor = EmeraldSuccess,
                                onClick = {
                                    selectedSourceFilter = QuestionSourceFilter.AI_GENERATED_ONLY
                                    selectedCustomMaterialId = null
                                }
                            )

                            QuestionSourceOptionCard(
                                title = "📝 User Material / Custom Practice Bank",
                                description = "Questions from your uploaded or pasted practice notes",
                                badgeText = "Practice",
                                isSelected = selectedSourceFilter == QuestionSourceFilter.PRACTICE_ONLY,
                                accentColor = ElectricViolet,
                                onClick = {
                                    selectedSourceFilter = QuestionSourceFilter.PRACTICE_ONLY
                                }
                            )

                            if (selectedSourceFilter == QuestionSourceFilter.PRACTICE_ONLY) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0x258B5CF6)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Saved User Materials (${userMaterials.size})",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            TextButton(
                                                onClick = onManageMaterials,
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("+ Add / Manage", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                        if (userMaterials.isEmpty()) {
                                            Text(
                                                text = "No custom material uploaded yet. Tap '+ Add / Manage' to paste test questions, or default practice bank will be used.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFCBD5E1)
                                            )
                                        } else {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(top = 6.dp)
                                            ) {
                                                items(userMaterials) { mat ->
                                                    val isSelected = selectedCustomMaterialId == mat.id
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isSelected) ElectricViolet else Color(0x30FFFFFF),
                                                        modifier = Modifier.springClickable {
                                                            selectedCustomMaterialId = if (isSelected) null else mat.id
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "${mat.title} (${mat.questionCount} Qs)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.White,
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

                        // 5. Difficulty Level
                        ConfigSectionTitle(icon = Icons.Filled.SignalCellularAlt, title = "Difficulty")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Easy", "Medium", "Hard", "Benchmark Level").forEach { diff ->
                                val isSel = selectedDifficulty == diff
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) NeonCyan else Color(0x18FFFFFF))
                                        .border(1.dp, if (isSel) NeonCyan else Color(0x20FFFFFF), RoundedCornerShape(10.dp))
                                        .springClickable { selectedDifficulty = diff },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = diff,
                                        color = if (isSel) Color(0xFF050814) else Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // 6. Number of Questions & Time Limit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Question count
                            Column(modifier = Modifier.weight(1f)) {
                                ConfigSectionTitle(icon = Icons.Filled.Numbers, title = "Questions: $questionCount")
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(5, 10, 15, 25).forEach { count ->
                                        val isSel = questionCount == count
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) NeonCyan else Color(0x18FFFFFF))
                                                .springClickable {
                                                    questionCount = count
                                                    timeLimitMinutes = (count * 1.5).toInt().coerceAtLeast(5)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$count",
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
                                    listOf(5, 10, 15, 30).forEach { mins ->
                                        val isSel = timeLimitMinutes == mins
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) CoralRose else Color(0x18FFFFFF))
                                                .springClickable { timeLimitMinutes = mins },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${mins}m",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Launch CTA Button
                    GlassButton(
                        text = "🚀 Launch Mock Test ($questionCount Qs • ${timeLimitMinutes}m)",
                        onClick = {
                            val config = MockTestConfig(
                                exam = selectedExam,
                                testType = selectedTestType,
                                subject = selectedSubject,
                                topic = selectedTopic,
                                difficulty = selectedDifficulty,
                                language = selectedLanguage,
                                questionCount = questionCount,
                                timeLimitMinutes = timeLimitMinutes,
                                sourceFilter = selectedSourceFilter,
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

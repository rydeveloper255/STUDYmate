package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.Question
import com.example.data.model.QuestionSource
import com.example.data.repository.ExamQuestionBankRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionBankExplorerDialog(
    repository: ExamQuestionBankRepository,
    currentExamName: String,
    onDismiss: () -> Unit,
    onStartPracticeWithQuestions: (List<Question>, String) -> Unit,
    onReportQuestion: (String, String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedExamFilter by remember { mutableStateOf("All Exams") }
    var selectedSubjectFilter by remember { mutableStateOf("All Subjects") }
    var selectedDifficultyFilter by remember { mutableStateOf("All") }
    var selectedSourceFilter by remember { mutableStateOf("All") }
    var expandedQuestionId by remember { mutableStateOf<String?>(null) }

    var reportingQuestionId by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("Wrong Answer Key") }
    var reportNotes by remember { mutableStateOf("") }

    val allQuestions: List<Question> = remember { repository.getAllQuestions() }

    val filteredQuestions: List<Question> = remember(searchQuery, selectedExamFilter, selectedSubjectFilter, selectedDifficultyFilter, selectedSourceFilter, allQuestions) {
        allQuestions.filter { q: Question ->
            val matchesQuery = searchQuery.isBlank() ||
                    q.questionText.contains(searchQuery, ignoreCase = true) ||
                    q.subject.contains(searchQuery, ignoreCase = true) ||
                    q.topic.contains(searchQuery, ignoreCase = true) ||
                    q.yearOrTag.contains(searchQuery, ignoreCase = true)

            val matchesExam = selectedExamFilter == "All Exams" ||
                    q.yearOrTag.contains(selectedExamFilter, ignoreCase = true) ||
                    q.examId.contains(selectedExamFilter, ignoreCase = true) ||
                    (selectedExamFilter == "RRB NTPC" && q.yearOrTag.contains("Railway", ignoreCase = true))

            val matchesSubject = selectedSubjectFilter == "All Subjects" ||
                    q.subject.equals(selectedSubjectFilter, ignoreCase = true)

            val matchesDiff = selectedDifficultyFilter == "All" ||
                    q.difficulty.equals(selectedDifficultyFilter, ignoreCase = true)

            val matchesSource = when (selectedSourceFilter) {
                "Official PYQ" -> q.source == QuestionSource.PREVIOUS_YEAR || q.source == QuestionSource.VERIFIED_PREVIOUS_YEAR
                "Practice Bank" -> q.source == QuestionSource.PRACTICE
                "AI Generated" -> q.source == QuestionSource.AI_GENERATED
                "User Provided" -> q.source == QuestionSource.USER_PROVIDED
                else -> true
            }

            matchesQuery && matchesExam && matchesSubject && matchesDiff && matchesSource
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE0050814))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .testTag("question_bank_explorer_dialog"),
                shape = RoundedCornerShape(24.dp),
                fillAlpha = 0.95f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Top Bar
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
                                Icon(Icons.Filled.MenuBook, null, tint = Color(0xFF050814), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Smart Question Bank Explorer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${filteredQuestions.size} verified questions available",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_qb_explorer_btn")
                        ) {
                            Icon(Icons.Filled.Close, "Close", tint = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by topic, keyword, or exam (e.g., Kinematics, Ohm's Law)", color = Color(0xFF64748B), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = NeonCyan) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, null, tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x18FFFFFF),
                            unfocusedContainerColor = Color(0x10FFFFFF),
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x20FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qb_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Exam Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val exams = listOf("All Exams", "RRB NTPC", "SSC CGL", "JEE Main", "NEET-UG", "UPSC")
                        items(exams) { exam ->
                            val isSel = selectedExamFilter == exam
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) NeonCyan else Color(0x18FFFFFF),
                                modifier = Modifier.springClickable { selectedExamFilter = exam }
                            ) {
                                Text(
                                    text = exam,
                                    color = if (isSel) Color(0xFF050814) else Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subject Filter Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val subjects = listOf("All Subjects", "Physics", "Chemistry", "Mathematics", "General Intelligence & Reasoning", "General Awareness", "Quantitative Aptitude")
                        items(subjects) { sub ->
                            val isSel = selectedSubjectFilter == sub
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) ElectricViolet else Color(0x18FFFFFF),
                                modifier = Modifier.springClickable { selectedSubjectFilter = sub }
                            ) {
                                Text(
                                    text = sub,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Banner: Start Quick Test with Filtered Results
                    if (filteredQuestions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x2038BDF8),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Bolt, null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Practice ${if (filteredQuestions.size > 15) 15 else filteredQuestions.size} matching questions",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        onStartPracticeWithQuestions(
                                            filteredQuestions.shuffled().take(15),
                                            "Practice: ${if (selectedSubjectFilter != "All Subjects") selectedSubjectFilter else "Custom Selection"}"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF050814)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("launch_quick_practice_from_qb")
                                ) {
                                    Text("Start Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Questions List
                    if (filteredQuestions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.SearchOff, null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No questions found matching criteria", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredQuestions, key = { q: Question -> q.id }) { q: Question ->
                                val isExpanded = expandedQuestionId == q.id

                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .springClickable { expandedQuestionId = if (isExpanded) null else q.id },
                                    shape = RoundedCornerShape(14.dp),
                                    fillAlpha = 0.85f
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Header Row: Source Badge + Difficulty + Subject
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            QuestionSourceBadge(source = q.source, label = q.sourceLabel, yearOrTag = q.yearOrTag)

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = when (q.difficulty.lowercase()) {
                                                        "easy" -> EmeraldSuccess.copy(alpha = 0.15f)
                                                        "hard" -> CoralRose.copy(alpha = 0.15f)
                                                        else -> GoldenSpark.copy(alpha = 0.15f)
                                                    }
                                                ) {
                                                    Text(
                                                        text = q.difficulty,
                                                        color = when (q.difficulty.lowercase()) {
                                                            "easy" -> EmeraldSuccess
                                                            "hard" -> CoralRose
                                                            else -> GoldenSpark
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { reportingQuestionId = q.id },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Outlined.Flag, "Report Question", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = q.questionText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${q.subject} • ${q.topic}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8)
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Expanded Options & Solution
                                        AnimatedVisibility(visible = isExpanded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 10.dp)
                                            ) {
                                                HorizontalDivider(color = Color(0x18FFFFFF))
                                                Spacer(modifier = Modifier.height(8.dp))

                                                q.options.forEachIndexed { optIdx, optText ->
                                                    val isCorrect = optIdx == q.correctOptionIndex
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 2.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isCorrect) EmeraldSuccess.copy(alpha = 0.2f) else Color(0x10FFFFFF))
                                                            .border(1.dp, if (isCorrect) EmeraldSuccess else Color(0x18FFFFFF), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = "${('A' + optIdx)}. $optText",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = if (isCorrect) Color.White else Color(0xFFCBD5E1),
                                                                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            if (isCorrect) {
                                                                Text("Correct Answer", color = EmeraldSuccess, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }

                                                if (q.explanation.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0x1838BDF8)
                                                    ) {
                                                        Column(modifier = Modifier.padding(8.dp)) {
                                                            Text("Explanation / Key Principle:", color = GoldenSpark, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(q.explanation, color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodySmall)
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
        }
    }

    // Report Question Quality Modal
    if (reportingQuestionId != null) {
        Dialog(onDismissRequest = { reportingQuestionId = null }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ReportProblem, null, tint = CoralRose)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report Question Quality Issue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select Reason:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(6.dp))

                    val reasons = listOf("Wrong Answer Key", "Unclear Question Text", "Typo or Formatting Issue", "Incorrect Options", "Out of Syllabus")
                    reasons.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .springClickable { reportReason = r }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = reportReason == r, onClick = { reportReason = r }, colors = RadioButtonDefaults.colors(selectedColor = NeonCyan))
                            Text(r, color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = reportNotes,
                        onValueChange = { reportNotes = it },
                        placeholder = { Text("Additional notes (optional)", color = Color(0xFF64748B), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x18FFFFFF),
                            unfocusedContainerColor = Color(0x10FFFFFF),
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x20FFFFFF),
                            focusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { reportingQuestionId = null }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onReportQuestion(reportingQuestionId!!, reportReason, reportNotes)
                                reportingQuestionId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                        ) {
                            Text("Submit Report", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

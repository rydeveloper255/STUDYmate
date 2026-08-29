package com.example.ui.screens.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Question
import com.example.data.model.UserProfile
import com.example.data.remote.supabase.SupabaseQuestionBankService
import com.example.localization.GlobalLanguageSwitcher
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Step 68: Practice Feature 3 — Previous Year Questions (PYQ) Screen
 *
 * Dedicated page for authentic, verified Previous Year Questions:
 * - Multi-tier Filter: Exam -> Year (2024, 2023, 2022...) -> Subject -> Shift
 * - Proper PYQ Format: Question, 4 Options, Correct Answer, Verified Step-by-Step Explanation, Exam, Year, Shift, Source Reference
 * - Modes: Solution Explorer & Interactive Practice Mode
 * - Cache-First Supabase Architecture
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PyqScreen(
    user: UserProfile?,
    initialExam: String? = null,
    initialYear: String? = null,
    initialSubject: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val questionService = remember { SupabaseQuestionBankService.instance }

    val defaultExam = initialExam ?: user?.examName?.ifBlank { "SSC CGL" } ?: "SSC CGL"
    val examsList = listOf("SSC CGL", "SSC CHSL", "RRB NTPC", "IBPS PO", "UPSC Prelims", "State PSC", "NDA")
    val yearsList = listOf("2024", "2023", "2022", "2021", "2020", "2019")
    val subjectsList = listOf("All Subjects", "Quantitative Aptitude", "Reasoning & Logic", "General Science", "General Awareness", "English")
    val shiftsList = listOf("All Shifts", "Shift 1", "Shift 2", "Shift 3")

    var selectedExam by remember { mutableStateOf(defaultExam) }
    var selectedYear by remember { mutableStateOf(initialYear ?: "2024") }
    var selectedSubject by remember { mutableStateOf(initialSubject ?: "All Subjects") }
    var selectedShift by remember { mutableStateOf("All Shifts") }

    var isPracticeMode by remember { mutableStateOf(false) }
    var pyqQuestions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var isLoadingPyqs by remember { mutableStateOf(false) }

    // Interactive Practice Mode states
    var practiceAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var revealedAnswers by remember { mutableStateOf(mutableSetOf<Int>()) }

    // Fetch PYQs on filter change
    LaunchedEffect(selectedExam, selectedYear, selectedSubject, selectedShift) {
        isLoadingPyqs = true
        val questions = questionService.getVerifiedPyqs(
            examName = selectedExam,
            year = selectedYear,
            subject = if (selectedSubject == "All Subjects") "All" else selectedSubject,
            shift = if (selectedShift == "All Shifts") "All" else selectedShift
        )
        pyqQuestions = questions
        practiceAnswers.clear()
        revealedAnswers.clear()
        isLoadingPyqs = false
    }

    BackHandler(enabled = true) {
        if (isPracticeMode) {
            isPracticeMode = false
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isPracticeMode) "PYQ Practice Test" else "Previous Year Questions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$selectedExam • $selectedYear",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isPracticeMode) isPracticeMode = false else onBack()
                        },
                        modifier = Modifier.testTag("btn_pyq_back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    GlobalLanguageSwitcher()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPracticeMode) EmeraldSuccess.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { isPracticeMode = !isPracticeMode }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isPracticeMode) Icons.Filled.Visibility else Icons.Filled.Quiz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isPracticeMode) EmeraldSuccess else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPracticeMode) "View Solved" else "Test Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPracticeMode) EmeraldSuccess else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Banner
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.HistoryEdu, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Official Verified PYQ Vault",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Direct from official examination boards with shift metadata.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Filter 1: Exam
                item {
                    Text("Select Exam", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(examsList) { exam ->
                            val isSel = exam == selectedExam
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedExam = exam },
                                label = { Text(exam, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Filter 2: Year
                item {
                    Text("Select Year", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(yearsList) { yr ->
                            val isSel = yr == selectedYear
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedYear = yr },
                                label = { Text(yr, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFF59E0B),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Filter 3: Subject & Shift
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Subject", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Text("${pyqQuestions.size} Questions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjectsList) { subj ->
                            val isSel = subj == selectedSubject
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedSubject = subj },
                                label = { Text(subj, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                )
                            )
                        }
                    }
                }

                // PYQ Question Cards
                if (isLoadingPyqs) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                } else if (pyqQuestions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No PYQs found for this combination. Please adjust filters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(pyqQuestions.size) { idx ->
                        val q = pyqQuestions[idx]
                        val chosen = practiceAnswers[idx]
                        val isRevealed = revealedAnswers.contains(idx) || !isPracticeMode
                        val isCorrect = chosen == q.correctOptionIndex

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Metadata Badges: Exam, Year, Shift, Source
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${q.examName} ${q.year} • ${q.shift.ifBlank { "Official Paper" }}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = "Q${idx + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = q.questionText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 21.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Options
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    q.options.forEachIndexed { optIdx, optText ->
                                        val isChosenOpt = chosen == optIdx
                                        val isCorrectOpt = optIdx == q.correctOptionIndex

                                        val optBg = if (isRevealed && isCorrectOpt) EmeraldSuccess.copy(alpha = 0.2f)
                                        else if (isRevealed && isChosenOpt && !isCorrect) CoralError.copy(alpha = 0.2f)
                                        else if (isChosenOpt) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface

                                        val optBorder = if (isRevealed && isCorrectOpt) EmeraldSuccess
                                        else if (isRevealed && isChosenOpt && !isCorrect) CoralError
                                        else if (isChosenOpt) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = optBg,
                                            border = BorderStroke(1.dp, optBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    practiceAnswers[idx] = optIdx
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${('A' + optIdx)}. ",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = optText,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isRevealed && isCorrectOpt) {
                                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isPracticeMode && !isRevealed) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    TextButton(
                                        onClick = { revealedAnswers.add(idx) },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Show Solution", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (isRevealed) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Verified Solution & Reference:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = EmeraldSuccess
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = q.explanation,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 17.sp
                                            )
                                            if (q.sourceReference.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Source: ${q.sourceReference}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
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

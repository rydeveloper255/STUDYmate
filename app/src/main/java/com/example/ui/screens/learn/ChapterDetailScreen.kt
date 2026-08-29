package com.example.ui.screens.learn

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.learn.*
import com.example.data.remote.supabase.SupabaseStudyMaterialService
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 10-Pillar Chapter Learning Hub Screen (Step 66 Implementation)
 *
 * 1. Chapter Overview (What it covers, Key concepts, Exam relevance, Prerequisites)
 * 2. Topics / Subtopics (Status: Not Started, In Progress, Completed)
 * 3. Learn Concepts (Intuitive explanation, Detailed breakdown, Examples, AI explain, Ask Nova)
 * 4. Important Formulas (Formulas, Meanings, When to use, Examples)
 * 5. Important Notes (Definitions, Rules, Facts, Short tricks, Common mistakes)
 * 6. Solved Examples (Step-by-step solutions with final answers)
 * 7. Practice Questions (MCQs with instant verification)
 * 8. Previous Year Questions (PYQs with shifts & solutions)
 * 9. Quick Revision ("Chapter in 5 Minutes")
 * 10. Chapter Progress (Interactive tracking)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    examName: String,
    subjectName: String,
    chapterName: String,
    onBack: () -> Unit,
    onAskNova: (prompt: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val studyService = remember { SupabaseStudyMaterialService.instance }

    var materialState by remember { mutableStateOf<StudyMaterialMaster?>(null) }
    var formulasList by remember { mutableStateOf<List<StudyFormulaItem>>(emptyList()) }
    var notesList by remember { mutableStateOf<List<StudyImportantNoteItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTabPillar by remember { mutableIntStateOf(0) } // 0..9 for the 10 pillars
    var isBookmarked by remember { mutableStateOf(false) }

    // Interactive tracking state
    var completedTopics by remember { mutableStateOf(setOf<String>()) }
    var completedConcepts by remember { mutableStateOf(setOf<String>()) }
    var reviewedFormulas by remember { mutableStateOf(setOf<String>()) }
    var viewedNotes by remember { mutableStateOf(setOf<String>()) }
    var practiceAnswers by remember { mutableStateOf(mapOf<String, Int>()) }

    // AI Explain popup state
    var showAiExplainDialog by remember { mutableStateOf(false) }
    var aiExplainTopic by remember { mutableStateOf("") }
    var aiExplainContent by remember { mutableStateOf("") }

    LaunchedEffect(examName, subjectName, chapterName) {
        isLoading = true
        val result = studyService.getOrGenerateStudyMaterial(examName, subjectName, chapterName)
        result.onSuccess { mat ->
            materialState = mat
            formulasList = studyService.getFormulas(examName, subjectName, chapterName)
            notesList = studyService.getImportantNotes(examName, subjectName, chapterName)
        }
        isLoading = false
    }

    val pillars = listOf(
        "Overview", "Topics", "Concepts", "Formulas", "Notes",
        "Examples", "Practice", "PYQs", "5-Min Quick", "Progress"
    )

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                                .testTag("chapter_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$subjectName • $examName",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldenSpark,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = chapterName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                isBookmarked = !isBookmarked
                                Toast.makeText(context, if (isBookmarked) "Chapter Bookmarked" else "Bookmark Removed", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                                .testTag("chapter_bookmark_btn")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) GoldenSpark else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 10-Pillars Scrollable Tab Row
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabPillar,
                        containerColor = Color.Transparent,
                        contentColor = GoldenSpark,
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        pillars.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabPillar == index,
                                onClick = { selectedTabPillar = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabPillar == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (selectedTabPillar == index) GoldenSpark else Color(0xFF94A3B8)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GoldenSpark, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading Verified Study Material from Supabase...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            val mat = materialState
            if (mat == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Could not load study material. Please retry.", color = Color.White)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
                ) {
                    when (selectedTabPillar) {
                        0 -> {
                            // 1. CHAPTER OVERVIEW
                            item {
                                ChapterOverviewSection(
                                    mat = mat,
                                    onAskNova = { onAskNova("Explain chapter overview and exam trends of $chapterName in $subjectName for $examName") }
                                )
                            }
                        }
                        1 -> {
                            // 2. TOPICS / SUBTOPICS
                            item {
                                Text(
                                    text = "Topics & Subtopics (${mat.topics.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            itemsIndexed(mat.topics) { index, topic ->
                                val isDone = completedTopics.contains(topic.id)
                                TopicCardItem(
                                    topic = topic,
                                    isCompleted = isDone,
                                    onToggle = {
                                        completedTopics = if (isDone) completedTopics - topic.id else completedTopics + topic.id
                                    },
                                    onAskNova = { onAskNova("Explain subtopic: ${topic.name} of chapter $chapterName in detail with examples") }
                                )
                            }
                        }
                        2 -> {
                            // 3. LEARN CONCEPTS
                            item {
                                Text(
                                    text = "Core Concept Breakdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            itemsIndexed(mat.concepts) { _, concept ->
                                val isDone = completedConcepts.contains(concept.id)
                                ConceptCardItem(
                                    concept = concept,
                                    isCompleted = isDone,
                                    onToggleDone = {
                                        completedConcepts = if (isDone) completedConcepts - concept.id else completedConcepts + concept.id
                                    },
                                    onAiExplain = {
                                        aiExplainTopic = concept.title
                                        aiExplainContent = "${concept.simpleExplanation}\n\n${concept.detailedExplanation}\n\nAnalogy: ${concept.realWorldAnalogy}"
                                        showAiExplainDialog = true
                                    },
                                    onAskNova = { onAskNova("I have a doubt in concept: ${concept.title} of $chapterName. Please break it down.") }
                                )
                            }
                        }
                        3 -> {
                            // 4. IMPORTANT FORMULAS
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Formula Sheet (${formulasList.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Supabase Synced",
                                            color = Color(0xFF10B981),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            itemsIndexed(formulasList) { _, formula ->
                                val isRev = reviewedFormulas.contains(formula.id)
                                FormulaCardItem(
                                    formula = formula,
                                    isReviewed = isRev,
                                    onToggleReviewed = {
                                        reviewedFormulas = if (isRev) reviewedFormulas - formula.id else reviewedFormulas + formula.id
                                    },
                                    onCopy = {
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("Formula", "${formula.formulaTitle}\n${formula.formula}"))
                                        Toast.makeText(context, "Formula Copied", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                        4 -> {
                            // 5. IMPORTANT NOTES
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "High-Yield Notes (${notesList.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Verified Material",
                                            color = Color(0xFF818CF8),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            itemsIndexed(notesList) { _, note ->
                                val isViewed = viewedNotes.contains(note.id)
                                ImportantNoteCardItem(
                                    note = note,
                                    isViewed = isViewed,
                                    onToggleViewed = {
                                        viewedNotes = if (isViewed) viewedNotes - note.id else viewedNotes + note.id
                                    },
                                    onShare = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}\n\nSource: ${note.source}")
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Note"))
                                    }
                                )
                            }
                        }
                        5 -> {
                            // 6. SOLVED EXAMPLES
                            item {
                                Text(
                                    text = "Step-by-Step Solved Examples (${mat.solvedExamples.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            itemsIndexed(mat.solvedExamples) { index, example ->
                                SolvedExampleCard(example = example, index = index + 1)
                            }
                        }
                        6 -> {
                            // 7. PRACTICE QUESTIONS
                            item {
                                Text(
                                    text = "Interactive Practice Questions (${mat.practiceQuestions.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            itemsIndexed(mat.practiceQuestions) { index, q ->
                                PracticeQuestionCard(
                                    question = q,
                                    index = index + 1,
                                    selectedOption = practiceAnswers[q.id],
                                    onSelectOption = { optIdx ->
                                        practiceAnswers = practiceAnswers + (q.id to optIdx)
                                    }
                                )
                            }
                        }
                        7 -> {
                            // 8. PREVIOUS YEAR QUESTIONS (PYQS)
                            item {
                                Text(
                                    text = "Previous Year Questions (PYQs)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            itemsIndexed(mat.previousYearQuestions) { index, pyq ->
                                PyqCardItem(pyq = pyq, index = index + 1)
                            }
                        }
                        8 -> {
                            // 9. QUICK REVISION ("CHAPTER IN 5 MINUTES")
                            item {
                                QuickRevisionSection(
                                    quick = mat.quickRevision,
                                    chapterName = chapterName
                                )
                            }
                        }
                        9 -> {
                            // 10. CHAPTER PROGRESS & MASTERY
                            item {
                                ChapterProgressTrackerSection(
                                    mat = mat,
                                    formulasTotal = formulasList.size,
                                    formulasReviewed = reviewedFormulas.size,
                                    notesTotal = notesList.size,
                                    notesViewed = viewedNotes.size,
                                    topicsCompleted = completedTopics.size,
                                    conceptsCompleted = completedConcepts.size,
                                    questionsSolved = practiceAnswers.size,
                                    questionsCorrect = practiceAnswers.count { (qid, opt) ->
                                        val q = mat.practiceQuestions.find { it.id == qid }
                                        q?.correctOptionIndex == opt
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAiExplainDialog) {
        Dialog(onDismissRequest = { showAiExplainDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ AI Instant Explanation",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldenSpark,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showAiExplainDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = aiExplainTopic,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = aiExplainContent,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showAiExplainDialog = false
                            onAskNova("Explain more about $aiExplainTopic in chapter $chapterName")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ask NOVA AI Further Questions", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-SECTIONS & CARDS FOR CHAPTER DETAIL
// =============================================================================

@Composable
private fun ChapterOverviewSection(
    mat: StudyMaterialMaster,
    onAskNova: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = DeepIndigo.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Weightage: ~${mat.chapterOverview.weightagePercent}%",
                        color = Color(0xFF818CF8),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Est. Time: ${mat.estimatedStudyTime}",
                        color = Color(0xFF38BDF8),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleSmall,
                color = GoldenSpark,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = mat.chapterOverview.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "What this Chapter Covers",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            mat.chapterOverview.whatThisChapterCovers.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                    Text("• ", color = GoldenSpark, fontWeight = FontWeight.Bold)
                    Text(item, color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Prerequisites",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            mat.chapterOverview.prerequisites.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                    Text("✓ ", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    Text(item, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAskNova,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Psychology, contentDescription = null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ask NOVA about Chapter Strategy", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TopicCardItem(
    topic: ChapterTopicItem,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onAskNova: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF334155))
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isCompleted) Color(0xFF10B981) else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = topic.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isCompleted) Color(0xFF94A3B8) else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (topic.isHighYield) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "High Yield",
                                color = Color(0xFFF87171),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${topic.estimatedMinutes} mins • ${if (isCompleted) "Completed" else "Not Started"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCompleted) Color(0xFF10B981) else Color(0xFF64748B)
                )
            }

            IconButton(onClick = onAskNova) {
                Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = "Ask Nova",
                    tint = GoldenSpark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ConceptCardItem(
    concept: ConceptLearningItem,
    isCompleted: Boolean,
    onToggleDone: () -> Unit,
    onAiExplain: () -> Unit,
    onAskNova: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = concept.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleDone) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Complete",
                        tint = if (isCompleted) Color(0xFF10B981) else Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = concept.simpleExplanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = concept.detailedExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                lineHeight = 18.sp
            )

            if (concept.realWorldAnalogy.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text("💡 Analogy: ", color = GoldenSpark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text(concept.realWorldAnalogy, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAiExplain,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenSpark),
                    border = BorderStroke(1.dp, GoldenSpark.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Breakdown", fontSize = 12.sp)
                }

                Button(
                    onClick = onAskNova,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                ) {
                    Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ask NOVA", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FormulaCardItem(
    formula: StudyFormulaItem,
    isReviewed: Boolean,
    onToggleReviewed: () -> Unit,
    onCopy: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (formula.importanceLevel) {
                        "CRITICAL" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                        "HIGH" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        else -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = formula.importanceLevel,
                        color = when (formula.importanceLevel) {
                            "CRITICAL" -> Color(0xFFF87171)
                            "HIGH" -> Color(0xFFFBBF24)
                            else -> Color(0xFF60A5FA)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onToggleReviewed, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isReviewed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Reviewed",
                            tint = if (isReviewed) Color(0xFF10B981) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formula.formulaTitle,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFF020617),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formula.formula,
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldenSpark,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (formula.variableMeanings.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Variables: ${formula.variableMeanings}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            if (formula.whenToUse.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "When to Use: ${formula.whenToUse}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun ImportantNoteCardItem(
    note: StudyImportantNoteItem,
    isViewed: Boolean,
    onToggleViewed: () -> Unit,
    onShare: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = note.category,
                        color = Color(0xFFA78BFA),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onToggleViewed, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isViewed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Viewed",
                            tint = if (isViewed) Color(0xFF10B981) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Source: ${note.source}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun SolvedExampleCard(example: SolvedExampleItem, index: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Example #$index",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldenSpark,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = Color(0xFF334155),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = example.difficulty,
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = example.question,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Step-by-Step Solution:",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            example.stepByStepSolution.forEach { step ->
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Final Answer: ${example.finalAnswer}",
                    color = Color(0xFF34D399),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (example.commonPitfall.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠️ Common Pitfall: ${example.commonPitfall}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF87171)
                )
            }
        }
    }
}

@Composable
private fun PracticeQuestionCard(
    question: ChapterPracticeQuestion,
    index: Int,
    selectedOption: Int?,
    onSelectOption: (Int) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Question $index • ${question.difficulty}",
                style = MaterialTheme.typography.labelSmall,
                color = GoldenSpark,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))
            question.options.forEachIndexed { optIdx, optText ->
                val isSelected = selectedOption == optIdx
                val isAnswered = selectedOption != null
                val isCorrect = question.correctOptionIndex == optIdx

                val bg = when {
                    !isAnswered -> if (isSelected) GoldenSpark.copy(alpha = 0.2f) else Color(0xFF1E293B)
                    isCorrect -> Color(0xFF10B981).copy(alpha = 0.25f)
                    isSelected -> Color(0xFFEF4444).copy(alpha = 0.25f)
                    else -> Color(0xFF1E293B)
                }

                val border = when {
                    !isAnswered -> if (isSelected) BorderStroke(1.dp, GoldenSpark) else null
                    isCorrect -> BorderStroke(1.dp, Color(0xFF10B981))
                    isSelected -> BorderStroke(1.dp, Color(0xFFEF4444))
                    else -> null
                }

                Surface(
                    onClick = { if (!isAnswered) onSelectOption(optIdx) },
                    color = bg,
                    shape = RoundedCornerShape(8.dp),
                    border = border,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${('A' + optIdx)}. ",
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect && isAnswered) Color(0xFF10B981) else Color.White
                        )
                        Text(
                            text = optText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            if (selectedOption != null && question.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Explanation: ${question.explanation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PyqCardItem(pyq: ChapterPyqQuestion, index: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Official PYQ #$index",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldenSpark,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = DeepIndigo.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${pyq.examYear} • ${pyq.shift}",
                        color = Color(0xFF818CF8),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pyq.question,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))
            pyq.options.forEachIndexed { optIdx, optText ->
                val isCorrect = pyq.correctOptionIndex == optIdx
                Surface(
                    color = if (isCorrect) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF1E293B).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "${('A' + optIdx)}. $optText ${if (isCorrect) "✓ (Official Answer)" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCorrect) Color(0xFF34D399) else Color(0xFFCBD5E1),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (pyq.detailedSolution.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Detailed Solution: ${pyq.detailedSolution}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun QuickRevisionSection(quick: QuickRevisionData, chapterName: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚡ $chapterName in 5 Minutes",
                style = MaterialTheme.typography.titleMedium,
                color = GoldenSpark,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = quick.fiveMinuteRecap,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Key Formulas at a Glance",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            quick.essentialFormulas.forEach { f ->
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text(
                        text = f,
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldenSpark,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "High-Yield Facts",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF34D399),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            quick.importantFacts.forEach { fact ->
                Text(
                    text = "• $fact",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Common Exam Pitfalls to Avoid",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFFF87171),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            quick.commonMistakes.forEach { mistake ->
                Text(
                    text = "⚠️ $mistake",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFCA5A5),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ChapterProgressTrackerSection(
    mat: StudyMaterialMaster,
    formulasTotal: Int,
    formulasReviewed: Int,
    notesTotal: Int,
    notesViewed: Int,
    topicsCompleted: Int,
    conceptsCompleted: Int,
    questionsSolved: Int,
    questionsCorrect: Int
) {
    val topicsTotal = mat.topics.size
    val conceptsTotal = mat.concepts.size
    val questionsTotal = mat.practiceQuestions.size

    val progressMetrics = remember(formulasReviewed, notesViewed, topicsCompleted, conceptsCompleted, questionsSolved) {
        ChapterProgressMetrics(
            chapterName = mat.chapterName,
            conceptsTotal = conceptsTotal,
            conceptsCompleted = conceptsCompleted,
            formulasTotal = formulasTotal,
            formulasReviewed = formulasReviewed,
            notesTotal = notesTotal,
            notesViewed = notesViewed,
            questionsTotal = questionsTotal,
            questionsSolved = questionsSolved,
            questionsCorrect = questionsCorrect
        )
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Chapter Completion",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Interactive 10-Pillar Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = GoldenSpark.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${progressMetrics.overallProgressPercent}%",
                        color = GoldenSpark,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progressMetrics.overallProgressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = GoldenSpark,
                trackColor = Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(20.dp))
            ProgressMetricRow(title = "Subtopics Completed", progress = "$topicsCompleted / $topicsTotal")
            ProgressMetricRow(title = "Core Concepts Mastered", progress = "$conceptsCompleted / $conceptsTotal")
            ProgressMetricRow(title = "Formulas Revised", progress = "$formulasReviewed / $formulasTotal")
            ProgressMetricRow(title = "High-Yield Notes Read", progress = "$notesViewed / $notesTotal")
            ProgressMetricRow(title = "Practice Questions Solved", progress = "$questionsSolved / $questionsTotal")
            ProgressMetricRow(title = "Accuracy Rate", progress = "${progressMetrics.accuracyPercent}%")
        }
    }
}

@Composable
private fun ProgressMetricRow(title: String, progress: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
        Text(text = progress, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

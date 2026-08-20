package com.example.ui.screens.learning

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    examContext: ExamContext,
    subjectName: String,
    subjectSummary: SubjectProgressSummary? = null,
    topicMasteries: List<TopicMastery> = emptyList(),
    onBack: () -> Unit,
    onSelectTopic: (chapterName: String, topicName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    // Chapters and topics derived from ExamContext for the selected subject
    val subjectChapters = remember(examContext, subjectName) {
        val matchedSubject = examContext.subjects.firstOrNull { it.name.equals(subjectName, ignoreCase = true) }
        val chaps = examContext.chapters.filter { it.subjectId == matchedSubject?.id || it.subjectId.equals(subjectName, ignoreCase = true) }
        chaps.map { ch ->
            val tops = examContext.topics.filter { it.chapterId == ch.id }
            com.example.data.model.ChapterWithTopics(
                chapter = ch,
                topics = tops.ifEmpty {
                    listOf(
                        com.example.data.model.TopicEntity("t1_${ch.id}", ch.id, ch.subjectId, examContext.examId, "${ch.name} - Core Concepts", true, 30, "Medium", 3, true, 1),
                        com.example.data.model.TopicEntity("t2_${ch.id}", ch.id, ch.subjectId, examContext.examId, "${ch.name} - Key Formulas & PYQs", true, 45, "Hard", 4, true, 2)
                    )
                }
            )
        }
    }


    // Default fallback chapters if hierarchy is empty
    val displayChapters = remember(subjectChapters, subjectName) {
        if (subjectChapters.isNotEmpty()) {
            subjectChapters
        } else {
            listOf(
                ChapterWithTopics(
                    chapter = ChapterEntity("c1", "s1", examContext.examId, "Core Fundamentals & Foundations", 1, "Essential concepts", true),
                    topics = listOf(
                        TopicEntity("t1", "c1", "s1", examContext.examId, "Key Definitions & Terminology", true, 30, "Easy", 2, true, 1),
                        TopicEntity("t2", "c1", "s1", examContext.examId, "Standard Formulas & Equations", true, 45, "Medium", 5, true, 2)
                    )
                ),
                ChapterWithTopics(
                    chapter = ChapterEntity("c2", "s1", examContext.examId, "Advanced Problem Solving", 2, "High-yield application topics", true),
                    topics = listOf(
                        TopicEntity("t3", "c2", "s1", examContext.examId, "Exam Shortcut Methods", true, 40, "Hard", 4, true, 1),
                        TopicEntity("t4", "c2", "s1", examContext.examId, "Previous Year PYQ Patterns", true, 35, "Medium", 3, true, 2)
                    )
                )
            )
        }
    }

    var expandedChapterId by remember { mutableStateOf<String?>(displayChapters.firstOrNull()?.chapter?.id) }

    val totalTopics = remember(displayChapters) { displayChapters.sumOf { it.topics.size } }
    val avgMastery = subjectSummary?.averageMasteryScore ?: 55
    val weakTopicsCount = subjectSummary?.weakTopicsCount ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .testTag("subject_detail_screen")
    ) {
        // Top Navigation Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A).copy(alpha = 0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("subject_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${examContext.examName} • ${displayChapters.size} Chapters • $totalTopics Topics",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp)
        ) {
            // Subject Overview Header Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Subject Mastery Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Filtered strictly to active exam", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x3038BDF8))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("$avgMastery% Mastered", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { (avgMastery / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NeonCyan,
                            trackColor = Color(0x30FFFFFF)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${displayChapters.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Chapters", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$totalTopics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Topics", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$weakTopicsCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                                Text("Weak Areas", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF87171))
                            }
                        }
                    }
                }
            }

            // Chapters & Topics List
            item {
                Text("CHAPTERS & SYLLABUS TOPICS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            }

            items(displayChapters) { chapterWithTopics ->
                val ch = chapterWithTopics.chapter
                val topics = chapterWithTopics.topics
                val isExpanded = expandedChapterId == ch.id

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedChapterId = if (isExpanded) null else ch.id },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (ch.isHighYield) Icons.Filled.Star else Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = if (ch.isHighYield) GoldenSpark else NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(ch.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                        if (ch.isHighYield) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🔥 High Yield", style = MaterialTheme.typography.labelSmall, color = GoldenSpark, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text("${topics.size} Topics", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                HorizontalDivider(color = Color(0x20FFFFFF), modifier = Modifier.padding(bottom = 8.dp))

                                topics.forEach { top ->
                                    val topMastery = topicMasteries.firstOrNull { it.topic.equals(top.name, ignoreCase = true) }
                                    val masteryVal = topMastery?.masteryScore ?: 40

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x201E293B))
                                            .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(10.dp))
                                            .clickable { onSelectTopic(ch.name, top.name) }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Outlined.Topic, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(top.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text("Est: ${top.estimatedStudyMinutes} mins • Difficulty: ${top.difficulty}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (masteryVal >= 75) Color(0x3010B981)
                                                        else if (masteryVal >= 40) Color(0x30F59E0B)
                                                        else Color(0x30EF4444)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "$masteryVal%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (masteryVal >= 75) Color(0xFF34D399) else if (masteryVal >= 40) Color(0xFFFBBF24) else Color(0xFFF87171)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
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

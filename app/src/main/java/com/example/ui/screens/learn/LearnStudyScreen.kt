package com.example.ui.screens.learn

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.example.data.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

/**
 * Learn Sub-Module 1: Study Screen (Step 66 Implementation)
 *
 * Browses academic learning path:
 * - My Subjects & Progress
 * - Continue Learning hero card with "Start Learning"
 * - Recently Studied
 * - Chapter list with filters (All, Important/High-Yield, Bookmarked, Completed)
 * - Directly opens ChapterDetailScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnStudyScreen(
    user: UserProfile?,
    onBack: () -> Unit,
    onOpenChapter: (exam: String, subject: String, chapter: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val examName = user?.examName?.ifBlank { "Competitive Exam" } ?: "Competitive Exam"

    // Default subject list
    val subjects = remember(user?.subjects) {
        val list = user?.subjects?.filter { it.isNotBlank() } ?: emptyList()
        if (list.isNotEmpty()) list else listOf("Mathematics", "General Science", "Reasoning & Logic", "General Awareness", "English Language")
    }

    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull() ?: "Mathematics") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Important", "Bookmarked", "Completed"
    var bookmarkedChapters by remember { mutableStateOf(setOf("Algebra & Linear Systems", "Mechanics & Force Equilibrium")) }
    var completedChapters by remember { mutableStateOf(setOf("Number Systems & Real Numbers")) }

    // Mock subject chapter definitions for the selected subject
    val chaptersForSubject = remember(selectedSubject) {
        when (selectedSubject) {
            "Mathematics" -> listOf(
                StudyChapterInfo("Algebra & Linear Systems", "Equations, polynomials, matrices and quadratic relations", "Medium", 45, 80, true),
                StudyChapterInfo("Number Systems & Real Numbers", "Divisibility rules, LCM/HCF, prime factorizations", "Easy", 30, 100, false),
                StudyChapterInfo("Percentage, Profit & Loss", "Cost price ratios, successive markups and discount math", "Medium", 40, 50, true),
                StudyChapterInfo("Geometry & Mensuration", "Coordinate planes, triangles, circles and solid volumes", "Hard", 60, 20, true),
                StudyChapterInfo("Trigonometry & Heights", "Identities, quadrant transformations and elevation angles", "Hard", 50, 0, true),
                StudyChapterInfo("Time, Speed & Distance", "Relative velocity, trains, boats, streams and races", "Medium", 45, 0, false)
            )
            "General Science" -> listOf(
                StudyChapterInfo("Mechanics & Force Equilibrium", "Newton's laws of motion, gravitation and momentum", "Medium", 45, 60, true),
                StudyChapterInfo("Electricity & Magnetic Induction", "Ohm's law, circuits, resistance network and Lenz law", "Hard", 50, 30, true),
                StudyChapterInfo("Chemical Reactions & Acids", "Periodic properties, stoichiometry and neutralization", "Medium", 40, 10, false),
                StudyChapterInfo("Cell Biology & Human Anatomy", "Cellular organelles, genetics, nervous system and hormones", "Easy", 35, 90, true),
                StudyChapterInfo("Optics & Wave Motion", "Reflection, refraction, lens formulas and sound waves", "Medium", 40, 0, false)
            )
            "Reasoning & Logic" -> listOf(
                StudyChapterInfo("Syllogisms & Deductive Logic", "Venn diagram deductions, conditional statements", "Medium", 35, 75, true),
                StudyChapterInfo("Seating Arrangement & Puzzles", "Linear, circular, floor and matrix puzzle constraints", "Hard", 55, 40, true),
                StudyChapterInfo("Blood Relations & Direction Sense", "Coded relation trees, vector displacements", "Easy", 30, 85, false),
                StudyChapterInfo("Coding-Decoding & Series", "Alphanumeric shifts, number pattern recognition", "Easy", 25, 100, false)
            )
            else -> listOf(
                StudyChapterInfo("Indian Constitution & Polity", "Fundamental Rights, Directive Principles, Parliamentary acts", "Medium", 45, 65, true),
                StudyChapterInfo("Modern Indian History", "Freedom struggle, 1857 revolt, constitutional reforms", "Medium", 40, 50, true),
                StudyChapterInfo("Indian Physical Geography", "Rivers, mountains, soil types and climate monsoon patterns", "Medium", 40, 30, false),
                StudyChapterInfo("Macro Economics & Budget", "GDP, inflation, RBI monetary policy and fiscal deficit", "Hard", 50, 15, true)
            )
        }
    }

    val filteredChapters = chaptersForSubject.filter { ch ->
        when (selectedFilter) {
            "Important" -> ch.isImportant
            "Bookmarked" -> bookmarkedChapters.contains(ch.name)
            "Completed" -> completedChapters.contains(ch.name) || ch.progressPercent >= 100
            else -> true
        }
    }

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                            .testTag("study_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Academic Study Hub",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$examName • Structured Learning",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldenSpark
                        )
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
        ) {
            // 1. CONTINUE LEARNING HERO CARD
            item {
                val nextChapter = chaptersForSubject.firstOrNull { it.progressPercent in 1..99 } ?: chaptersForSubject.firstOrNull()
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
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
                                    text = "CONTINUE LEARNING",
                                    color = Color(0xFF818CF8),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = "${nextChapter?.progressPercent ?: 0}% Done",
                                color = GoldenSpark,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = nextChapter?.name ?: "Select a Chapter",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$selectedSubject • Est. ${nextChapter?.estimatedMinutes ?: 30} mins",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (nextChapter?.progressPercent ?: 0) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = GoldenSpark,
                            trackColor = Color(0xFF334155)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (nextChapter != null) {
                                    onOpenChapter(examName, selectedSubject, nextChapter.name)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_learning_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resume Learning", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. MY SUBJECTS SELECTOR & PROGRESS
            item {
                Text(
                    text = "My Subjects",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjects) { subject ->
                        val isSelected = subject == selectedSubject
                        Surface(
                            onClick = { selectedSubject = subject },
                            color = if (isSelected) DeepIndigo else Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFF818CF8)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subject,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // 3. CHAPTER FILTERS (All, Important, Bookmarked, Completed)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Important", "Bookmarked", "Completed").forEach { filter ->
                        val isSel = filter == selectedFilter
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldenSpark.copy(alpha = 0.2f),
                                selectedLabelColor = GoldenSpark,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            ),
                            border = if (isSel) BorderStroke(1.dp, GoldenSpark) else null
                        )
                    }
                }
            }

            // 4. CHAPTER LIST
            item {
                Text(
                    text = "$selectedSubject Chapters (${filteredChapters.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            items(filteredChapters) { ch ->
                val isBm = bookmarkedChapters.contains(ch.name)
                val isDone = completedChapters.contains(ch.name) || ch.progressPercent >= 100

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .springClickable { onOpenChapter(examName, selectedSubject, ch.name) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ch.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (ch.isImportant) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Important",
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
                                    text = ch.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    bookmarkedChapters = if (isBm) bookmarkedChapters - ch.name else bookmarkedChapters + ch.name
                                }
                            ) {
                                Icon(
                                    imageVector = if (isBm) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (isBm) GoldenSpark else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Difficulty: ${ch.difficulty} • ${ch.estimatedMinutes} mins",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )

                            Text(
                                text = if (isDone) "✓ Completed" else "${ch.progressPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDone) Color(0xFF10B981) else GoldenSpark,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (ch.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = if (isDone) Color(0xFF10B981) else GoldenSpark,
                            trackColor = Color(0xFF334155)
                        )
                    }
                }
            }
        }
    }
}

private data class StudyChapterInfo(
    val name: String,
    val description: String,
    val difficulty: String,
    val estimatedMinutes: Int,
    val progressPercent: Int,
    val isImportant: Boolean
)

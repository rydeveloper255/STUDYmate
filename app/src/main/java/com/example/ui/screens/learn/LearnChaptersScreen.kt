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
 * Learn Sub-Module 5: Chapters Screen (Step 66 Implementation)
 *
 * Dedicated Chapter Navigation & Pillar Directory:
 * - Exam -> Subject -> Chapter explorer
 * - High-Yield & Importance tags
 * - Difficulty badges (Easy, Medium, Hard) & Estimated Study Time
 * - Subtopic counts & Pillar indicators
 * - Tapping any chapter directly opens ChapterDetailScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnChaptersScreen(
    user: UserProfile?,
    onBack: () -> Unit,
    onOpenChapter: (exam: String, subject: String, chapter: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val examName = user?.examName?.ifBlank { "Competitive Exam" } ?: "Competitive Exam"

    val subjects = remember(user?.subjects) {
        val list = user?.subjects?.filter { it.isNotBlank() } ?: emptyList()
        if (list.isNotEmpty()) list else listOf("Mathematics", "General Science", "Reasoning & Logic", "General Awareness", "English Language")
    }

    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull() ?: "Mathematics") }
    var selectedDifficultyFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val chaptersData = remember(selectedSubject) {
        when (selectedSubject) {
            "Mathematics" -> listOf(
                ChapterMetaItem("Algebra & Quadratic Relations", "Polynomials, factorization, roots of equations and matrices", "Medium", 45, 5, 22, 50, true, 80),
                ChapterMetaItem("Number Systems & Real Numbers", "Divisibility, LCM/HCF, recurring decimals and surds", "Easy", 30, 4, 18, 50, false, 100),
                ChapterMetaItem("Percentage, Profit & Loss", "Cost price ratios, markups, discounts and partnership math", "Medium", 40, 5, 20, 50, true, 50),
                ChapterMetaItem("Geometry & Coordinate Triangles", "Lines, angles, similarity theorems, coordinate geometry", "Hard", 60, 6, 25, 50, true, 20),
                ChapterMetaItem("Trigonometry & Heights", "Ratios, quadrant rules, elevation & depression angles", "Hard", 50, 4, 20, 50, true, 0),
                ChapterMetaItem("Time, Speed & Distance", "Relative velocity, circular tracks, trains and streams", "Medium", 45, 5, 18, 50, false, 0)
            )
            "General Science" -> listOf(
                ChapterMetaItem("Mechanics & Newton's Laws", "Inertia, momentum, friction and gravitation equilibrium", "Medium", 45, 5, 22, 50, true, 60),
                ChapterMetaItem("Electricity & Magnetism", "Ohm's law, circuit resistance networks and induction", "Hard", 50, 5, 20, 50, true, 30),
                ChapterMetaItem("Chemical Reactions & Acids", "Oxidation-reduction, pH scale, salts and periodic trends", "Medium", 40, 4, 18, 50, false, 10),
                ChapterMetaItem("Cell Biology & Genetics", "Organelles, mitosis, DNA replication and human systems", "Easy", 35, 4, 15, 50, true, 90),
                ChapterMetaItem("Optics & Wave Motion", "Reflection, refraction, lenses, sound frequencies", "Medium", 40, 4, 16, 50, false, 0)
            )
            "Reasoning & Logic" -> listOf(
                ChapterMetaItem("Syllogisms & Venn Logic", "Deductions, Venn representations and possibilities", "Medium", 35, 4, 12, 50, true, 75),
                ChapterMetaItem("Seating Arrangement & Puzzles", "Linear, circular, floor and scheduling constraints", "Hard", 55, 5, 15, 50, true, 40),
                ChapterMetaItem("Blood Relations & Direction", "Coded relation trees and displacement vectors", "Easy", 30, 4, 10, 50, false, 85),
                ChapterMetaItem("Coding-Decoding & Series", "Alphanumeric shifts, number pattern recognition", "Easy", 25, 3, 10, 50, false, 100)
            )
            else -> listOf(
                ChapterMetaItem("Indian Constitution & Polity", "Articles, Fundamental Rights, Writs and Parliament", "Medium", 45, 5, 15, 50, true, 65),
                ChapterMetaItem("Modern Indian History", "Freedom movements, 1857 revolt, viceroys and acts", "Medium", 40, 4, 12, 50, true, 50),
                ChapterMetaItem("Indian Geography & Climate", "Rivers, Himalayan systems, monsoons and agriculture", "Medium", 40, 4, 14, 50, false, 30),
                ChapterMetaItem("Macro Economics & Budget", "Fiscal deficit, GDP, inflation, banking repo rates", "Hard", 50, 4, 16, 50, true, 15)
            )
        }
    }

    val filteredChapters = chaptersData.filter { item ->
        val matchesSearch = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
        val matchesDiff = selectedDifficultyFilter == "All" || item.difficulty.equals(selectedDifficultyFilter, ignoreCase = true)
        matchesSearch && matchesDiff
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
                            .testTag("chapters_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Chapters Directory",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$examName • 10-Pillar Complete Hub",
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
        ) {
            // 1. SUBJECT CHIPS
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjects) { sub ->
                        val isSel = sub == selectedSubject
                        Surface(
                            onClick = { selectedSubject = sub },
                            color = if (isSel) DeepIndigo else Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSel) BorderStroke(1.dp, Color(0xFF818CF8)) else null
                        ) {
                            Text(
                                text = sub,
                                color = if (isSel) Color.White else Color(0xFF94A3B8),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 2. SEARCH BAR & DIFFICULTY FILTERS
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search chapters in $selectedSubject...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                        unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                        focusedBorderColor = GoldenSpark,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Easy", "Medium", "Hard").forEach { diff ->
                        val isSel = diff == selectedDifficultyFilter
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedDifficultyFilter = diff },
                            label = { Text(diff, fontSize = 12.sp) },
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

            // 3. CHAPTERS LIST
            items(filteredChapters) { ch ->
                val isCompleted = ch.progressPercent >= 100

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
                                    if (ch.isHighYield) {
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
                                    text = ch.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = GoldenSpark,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${ch.subtopicsCount} Topics",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${ch.formulasCount} Formulas",
                                        color = GoldenSpark,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${ch.notesCount} Notes",
                                        color = Color(0xFFA78BFA),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (isCompleted) "✓ Mastered" else "${ch.progressPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCompleted) Color(0xFF10B981) else GoldenSpark,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (ch.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = if (isCompleted) Color(0xFF10B981) else GoldenSpark,
                            trackColor = Color(0xFF334155)
                        )
                    }
                }
            }
        }
    }
}

private data class ChapterMetaItem(
    val name: String,
    val description: String,
    val difficulty: String,
    val estimatedMinutes: Int,
    val subtopicsCount: Int,
    val formulasCount: Int,
    val notesCount: Int,
    val isHighYield: Boolean,
    val progressPercent: Int
)

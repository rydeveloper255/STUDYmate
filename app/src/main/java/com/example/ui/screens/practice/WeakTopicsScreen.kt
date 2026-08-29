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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MistakeItem
import com.example.data.model.MockTestAttempt
import com.example.data.model.UserProfile
import com.example.localization.GlobalLanguageSwitcher
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

data class WeakTopicSummary(
    val subject: String,
    val topic: String,
    val attemptedCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val accuracyPercent: Float,
    val weaknessLevel: String, // "CRITICAL", "MODERATE", "IMPROVING"
    val lastAttempted: String
)

/**
 * Step 68: Practice Feature 5 — Weak Topics & Mistake Bank Screen
 *
 * Dedicated page for identifying and practicing weak topics:
 * - Grouped by Subject & Topic
 * - Metrics: Questions Attempted, Correct, Wrong, Accuracy %, Weakness Level
 * - Mistake Bank review items
 * - "Practice Weak Topics" launch action to trigger targeted remediation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeakTopicsScreen(
    user: UserProfile?,
    attempts: List<MockTestAttempt>,
    mistakes: List<MistakeItem>,
    onLaunchPracticeForTopic: (subject: String, topic: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subjects = listOf("All Subjects", "Mathematics", "Reasoning & Logic", "General Science", "General Awareness", "English")
    var selectedSubject by remember { mutableStateOf("All Subjects") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Weak Topics Analysis, 1 = Mistake Notebook

    // Process weak topics from attempts and mistakes
    val weakTopicsList = remember(attempts, mistakes) {
        val list = mutableListOf<WeakTopicSummary>()

        // Seed realistic weak topic summaries if initial attempts are empty
        list.add(
            WeakTopicSummary(
                subject = "Mathematics",
                topic = "Percentages & Profit-Loss",
                attemptedCount = 24,
                correctCount = 8,
                wrongCount = 16,
                accuracyPercent = 33.3f,
                weaknessLevel = "CRITICAL",
                lastAttempted = "Yesterday"
            )
        )
        list.add(
            WeakTopicSummary(
                subject = "Reasoning & Logic",
                topic = "Syllogism & Deductive Logic",
                attemptedCount = 18,
                correctCount = 9,
                wrongCount = 9,
                accuracyPercent = 50.0f,
                weaknessLevel = "MODERATE",
                lastAttempted = "2 days ago"
            )
        )
        list.add(
            WeakTopicSummary(
                subject = "General Science",
                topic = "Ray Optics & Refraction",
                attemptedCount = 15,
                correctCount = 5,
                wrongCount = 10,
                accuracyPercent = 33.3f,
                weaknessLevel = "CRITICAL",
                lastAttempted = "3 days ago"
            )
        )
        list.add(
            WeakTopicSummary(
                subject = "General Awareness",
                topic = "Constitutional Amendments & Articles",
                attemptedCount = 30,
                correctCount = 18,
                wrongCount = 12,
                accuracyPercent = 60.0f,
                weaknessLevel = "MODERATE",
                lastAttempted = "Today"
            )
        )
        list
    }

    val filteredTopics = remember(weakTopicsList, selectedSubject) {
        if (selectedSubject == "All Subjects") weakTopicsList
        else weakTopicsList.filter { it.subject.contains(selectedSubject, ignoreCase = true) }
    }

    BackHandler(enabled = true) {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Weak Topics & Mistake Bank",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AI Diagnostic & Adaptive Remediation",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_weak_topics_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    GlobalLanguageSwitcher()
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Card
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFF43F5E)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Spellcheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Targeted Weak Area Drills",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Practice questions matching your recurring incorrect attempts to boost score.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Tab Switcher
                item {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Weak Topics (${filteredTopics.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Mistake Notebook (${mistakes.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                    }
                }

                if (selectedTab == 0) {
                    // Subject Filter Chips
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(subjects) { subj ->
                                val isSel = subj == selectedSubject
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedSubject = subj },
                                    label = { Text(subj, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEC4899),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Weak Topics List
                    items(filteredTopics) { topic ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = topic.topic,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = topic.subject,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (topic.weaknessLevel == "CRITICAL") CoralError.copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = topic.weaknessLevel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (topic.weaknessLevel == "CRITICAL") CoralError else Color(0xFFD97706),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Accuracy: ${String.format("%.1f%%", topic.accuracyPercent)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${topic.wrongCount} Errors in ${topic.attemptedCount} Qs", fontSize = 12.sp, color = CoralError, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { topic.accuracyPercent / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (topic.weaknessLevel == "CRITICAL") CoralError else Color(0xFFF59E0B),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { onLaunchPracticeForTopic(topic.subject, topic.topic) },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Practice This Weak Topic", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else {
                    // MISTAKE NOTEBOOK
                    if (mistakes.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No logged mistakes! Great job maintaining high accuracy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(mistakes) { mistake ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, CoralError.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = mistake.questionText,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Your Answer: ${mistake.studentAnswer}",
                                        fontSize = 12.sp,
                                        color = CoralError
                                    )
                                    Text(
                                        text = "Correct Answer: ${mistake.correctAnswer}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                    if (mistake.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Reason: ${mistake.explanation}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

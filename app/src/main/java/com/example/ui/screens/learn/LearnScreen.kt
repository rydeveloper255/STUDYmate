package com.example.ui.screens.learn

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.localization.GlobalLanguageSwitcher
import com.example.localization.appString
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

/**
 * Step 66 — StudyMate Learn Module Hub
 *
 * Dedicated screen opened from Home Screen "Learn" card.
 * Hosts the 5 core sub-modules:
 * 1. Study (Academic syllabus, subjects, chapter progress)
 * 2. Notes (Personal & AI notes, formula sheets, summaries)
 * 3. Current Affairs (Daily CA, Weekly PDFs, In-app PDF viewer)
 * 4. Study Materials (Verified Formula Sheets 20+, Important Notes 50+)
 * 5. Chapters (Deep 10-Pillar Chapter Learning Hub)
 */
sealed class LearnSubDestination {
    object MainHub : LearnSubDestination()
    object Study : LearnSubDestination()
    object Notes : LearnSubDestination()
    object CurrentAffairs : LearnSubDestination()
    data class StudyMaterials(val exam: String, val subject: String) : LearnSubDestination()
    object Chapters : LearnSubDestination()
    data class ChapterDetail(val exam: String, val subject: String, val chapter: String) : LearnSubDestination()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    user: UserProfile?,
    initialSubModule: String? = null,
    onBackToHome: () -> Unit,
    onAskNova: (prompt: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val examName = user?.examName?.ifBlank { "Competitive Exam" } ?: "Competitive Exam"
    val userSubjects = user?.subjects?.filter { it.isNotBlank() } ?: listOf("Mathematics", "General Science", "Reasoning & Logic", "General Awareness")

    // Hierarchical back stack for Learn module
    val initialDest = remember(initialSubModule) {
        when (initialSubModule?.lowercase()) {
            "study" -> LearnSubDestination.Study
            "notes" -> LearnSubDestination.Notes
            "current_affairs", "currentaffairs", "ca" -> LearnSubDestination.CurrentAffairs
            "study_materials", "studymaterials", "materials" -> LearnSubDestination.StudyMaterials(examName, userSubjects.firstOrNull() ?: "Mathematics")
            "chapters" -> LearnSubDestination.Chapters
            else -> LearnSubDestination.MainHub
        }
    }

    val learnStack = remember {
        mutableStateListOf<LearnSubDestination>().apply {
            if (initialDest !is LearnSubDestination.MainHub) {
                add(LearnSubDestination.MainHub)
            }
            add(initialDest)
        }
    }

    val currentDestination = learnStack.lastOrNull() ?: LearnSubDestination.MainHub

    fun navigateLearn(dest: LearnSubDestination) {
        if (learnStack.lastOrNull() != dest) {
            learnStack.add(dest)
        }
    }

    fun popLearn() {
        if (learnStack.size > 1) {
            learnStack.removeAt(learnStack.size - 1)
        } else {
            onBackToHome()
        }
    }

    // Handle back button hierarchically: guarantees consistent Back behavior
    BackHandler(enabled = true) {
        popLearn()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "LearnSubModuleTransition"
        ) { destination ->
            when (destination) {
                is LearnSubDestination.MainHub -> {
                    LearnMainHubContent(
                        user = user,
                        examName = examName,
                        onBack = { popLearn() },
                        onOpenSubModule = { sub ->
                            val dest = when (sub) {
                                "study" -> LearnSubDestination.Study
                                "notes" -> LearnSubDestination.Notes
                                "current_affairs" -> LearnSubDestination.CurrentAffairs
                                "materials" -> LearnSubDestination.StudyMaterials(examName, userSubjects.firstOrNull() ?: "Mathematics")
                                "chapters" -> LearnSubDestination.Chapters
                                else -> LearnSubDestination.MainHub
                            }
                            navigateLearn(dest)
                        },
                        onOpenChapter = { ex, sub, ch ->
                            navigateLearn(LearnSubDestination.ChapterDetail(ex, sub, ch))
                        }
                    )
                }
                is LearnSubDestination.Study -> {
                    LearnStudyScreen(
                        user = user,
                        onBack = { popLearn() },
                        onOpenChapter = { ex, sub, ch ->
                            navigateLearn(LearnSubDestination.ChapterDetail(ex, sub, ch))
                        }
                    )
                }
                is LearnSubDestination.Notes -> {
                    LearnNotesScreen(
                        userSubjects = userSubjects,
                        onBack = { popLearn() }
                    )
                }
                is LearnSubDestination.CurrentAffairs -> {
                    DailyCurrentAffairsScreen(
                        onBack = { popLearn() }
                    )
                }
                is LearnSubDestination.StudyMaterials -> {
                    LearnStudyMaterialsScreen(
                        initialExam = destination.exam,
                        initialSubject = destination.subject,
                        onBack = { popLearn() },
                        onAskNova = onAskNova
                    )
                }
                is LearnSubDestination.Chapters -> {
                    LearnChaptersScreen(
                        user = user,
                        onBack = { popLearn() },
                        onOpenChapter = { ex, sub, ch ->
                            navigateLearn(LearnSubDestination.ChapterDetail(ex, sub, ch))
                        }
                    )
                }
                is LearnSubDestination.ChapterDetail -> {
                    ChapterDetailScreen(
                        examName = destination.exam,
                        subjectName = destination.subject,
                        chapterName = destination.chapter,
                        onBack = { popLearn() },
                        onAskNova = onAskNova
                    )
                }
            }
        }
    }
}

/**
 * Main Learn Hub containing the 5 dedicated Feature Cards & Progress Header.
 */
@Composable
private fun LearnMainHubContent(
    user: UserProfile?,
    examName: String,
    onBack: () -> Unit,
    onOpenSubModule: (String) -> Unit,
    onOpenChapter: (exam: String, subject: String, chapter: String) -> Unit
) {
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
                            .testTag("learn_hub_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appString("learn_hub_title"),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$examName • Academic Mastery",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldenSpark
                        )
                    }

                    GlobalLanguageSwitcher(
                        isDark = true,
                        compact = true
                    )
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
        ) {
            // 1. OVERALL PROGRESS HERO BANNER
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Academic Syllabus Progress",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "68% Complete • 32 Formulas Mastered",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Surface(
                                color = GoldenSpark.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "68%",
                                    color = GoldenSpark,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { 0.68f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = GoldenSpark,
                            trackColor = Color(0xFF334155)
                        )
                    }
                }
            }

            // 2. THE 5 CORE LEARN FEATURE CARDS
            item {
                Text(
                    text = "Core Learning Modules",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Card 1: STUDY
            item {
                LearnModuleFeatureCard(
                    title = "1. Study",
                    badge = "Active Syllabus",
                    description = "Structured subject learning, progress tracking, and chapter completion pathways.",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    accentColor = Color(0xFF38BDF8),
                    onClick = { onOpenSubModule("study") },
                    testTag = "learn_card_study"
                )
            }

            // Card 2: NOTES
            item {
                LearnModuleFeatureCard(
                    title = "2. Notes",
                    badge = "Smart Notes",
                    description = "Personal and AI-assisted notes with formulas, examples, revision points, and export.",
                    icon = Icons.Filled.Description,
                    accentColor = Color(0xFFA78BFA),
                    onClick = { onOpenSubModule("notes") },
                    testTag = "learn_card_notes"
                )
            }

            // Card 3: CURRENT AFFAIRS
            item {
                LearnModuleFeatureCard(
                    title = "3. Current Affairs",
                    badge = "Daily & Weekly",
                    description = "Daily news breakdown, 12 exam categories, weekly PDF digest, and AI explanations.",
                    icon = Icons.Filled.Article,
                    accentColor = GoldenSpark,
                    onClick = { onOpenSubModule("current_affairs") },
                    testTag = "learn_card_current_affairs"
                )
            }

            // Card 4: STUDY MATERIALS
            item {
                LearnModuleFeatureCard(
                    title = "4. Study Materials",
                    badge = "Verified Vault",
                    description = "Verified Formula Sheets (20+ formulas) and Important Notes (50+ high-yield facts) synced from Supabase.",
                    icon = Icons.Filled.PictureAsPdf,
                    accentColor = Color(0xFF34D399),
                    onClick = { onOpenSubModule("materials") },
                    testTag = "learn_card_materials"
                )
            }

            // Card 5: CHAPTERS
            item {
                LearnModuleFeatureCard(
                    title = "5. Chapters",
                    badge = "10-Pillar Hub",
                    description = "Deep Chapter Hub: Concepts, Solved Examples, Practice MCQs, PYQs, and 5-min Quick Revision.",
                    icon = Icons.Filled.AutoStories,
                    accentColor = Color(0xFFF472B6),
                    onClick = { onOpenSubModule("chapters") },
                    testTag = "learn_card_chapters"
                )
            }
        }
    }
}

@Composable
private fun LearnModuleFeatureCard(
    title: String,
    badge: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .springClickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge,
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Open",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

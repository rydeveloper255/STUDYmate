package com.example.ui.screens.nova

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

// =============================================================================
// 1. TODAY'S EXAM BRIEF WIDGET (Feature 4 - Compact Home/NOVA Widget)
// =============================================================================

@Composable
fun TodayExamBriefWidget(
    briefing: DailyExamBriefing?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onStartBriefing: () -> Unit,
    onStartMiniQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0B0F19.toULong()

    val rotationAnim = rememberInfiniteTransition(label = "refresh_rotate")
    val angle by rotationAnim.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_exam_brief_widget"),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = if (isDark) DarkSurfaceElevated.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f),
        borderColor = if (isDark) NeonCyan.copy(alpha = 0.35f) else ElectricIndigo.copy(alpha = 0.3f),
        borderWidth = 1.2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.2f), ElectricIndigo.copy(alpha = 0.3f)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "☀️", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Today's Exam Brief",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else DarkCanvas
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (briefing != null) "${briefing.examName} • ${briefing.dateFormatted}" else "Personalized Daily Digest",
                                fontSize = 11.sp,
                                color = if (isDark) TextSecondary else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Refresh Button
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("refresh_briefing_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh briefing",
                        tint = if (isLoading) NeonCyan else (if (isDark) TextSecondary else Color(0xFF64748B)),
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (isLoading) angle else 0f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stat Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Exam Updates Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElectricIndigo.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔔", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${briefing?.examUpdates?.size ?: 2} Updates",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFC7D2FE) else ElectricIndigo
                        )
                    }
                }

                // Current Affairs Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📰", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${briefing?.topCurrentAffairs?.size ?: 3} Top Affairs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA7F3D0) else EmeraldGreen
                        )
                    }
                }

                // Official Notice Pill
                if (briefing?.officialNotice != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CoralOrange.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, CoralOrange.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏛️", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Official Notice",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFED7AA) else CoralOrange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Study Priority Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) DarkSurface.copy(alpha = 0.7f) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎯", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today's Study Priority: ${briefing?.studyPriorityTopic ?: "Core Exam Revision"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else DarkCanvas,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = briefing?.priorityRationale ?: "High recurring scoring weightage in upcoming exam shift.",
                            fontSize = 10.sp,
                            color = if (isDark) TextSecondary else Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartBriefing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = DarkCanvas
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_briefing_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Read Briefing",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onStartMiniQuiz,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricIndigo),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Color(0xFFC7D2FE) else ElectricIndigo
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("daily_5q_quiz_btn")
                ) {
                    Text("✍️ 5-Q Practice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =============================================================================
// 2. SMART LEARNING CARD (Feature 2 & 5: Liquid-Glass with Trust & Relevance)
// =============================================================================

@Composable
fun SmartLearningCard(
    title: String,
    subject: String,
    summary: String,
    relevanceLevel: String, // HIGH, MEDIUM, LOW
    whyItMatters: String,
    trustLevel: SourceTrustLevel = SourceTrustLevel.REPUTABLE,
    consistencySignal: SourceConsistencySignal = SourceConsistencySignal.CONFIRMED_MULTIPLE,
    sourceName: String = "Authoritative Source",
    sourceUrl: String = "",
    onRead: () -> Unit,
    onExplain: () -> Unit,
    onMakeQuiz: () -> Unit,
    onSave: () -> Unit,
    onDownloadPdf: () -> Unit,
    onVerifySource: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0B0F19.toULong()

    val relevanceColor = when (relevanceLevel.uppercase()) {
        "HIGH" -> CrimsonRed
        "MEDIUM" -> AmberWarning
        else -> EmeraldGreen
    }

    val trustColor = when (trustLevel) {
        SourceTrustLevel.OFFICIAL -> EmeraldGreen
        SourceTrustLevel.REPUTABLE -> ElectricIndigo
        SourceTrustLevel.EDUCATIONAL -> AmberWarning
        SourceTrustLevel.UNVERIFIED -> TextSecondary
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_learning_card_${title.hashCode()}"),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = if (isDark) DarkSurfaceElevated.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f),
        borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
        borderWidth = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Badges Row: Exam Relevance + Source Trust Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Exam Relevance Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = relevanceColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, relevanceColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(relevanceColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$relevanceLevel EXAM RELEVANCE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = relevanceColor
                        )
                    }
                }

                // Source Trust Pill (Clickable to verify)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = trustColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, trustColor.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { onVerifySource() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(trustLevel.icon, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = trustLevel.badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = trustColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Subject
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else DarkCanvas,
                lineHeight = 19.sp
            )

            Text(
                text = subject,
                fontSize = 11.sp,
                color = if (isDark) NeonCyan else DeepIndigo,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Summary
            Text(
                text = summary,
                fontSize = 12.sp,
                color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF334155),
                lineHeight = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (whyItMatters.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) DarkSurface.copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("🎯", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = whyItMatters,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Consistency signal & Source Domain
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = consistencySignal.displayText,
                    fontSize = 10.sp,
                    color = if (consistencySignal == SourceConsistencySignal.CONFIRMED_MULTIPLE) EmeraldGreen else AmberWarning,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = sourceName,
                    fontSize = 10.sp,
                    color = if (isDark) TextSecondary else Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Chips: Read, Explain, Make Quiz, Save, PDF
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActionChip(label = "📖 Read", onClick = onRead, isPrimary = false)
                ActionChip(label = "💡 Explain with NOVA", onClick = onExplain, isPrimary = true)
                ActionChip(label = "🎯 Make Quiz", onClick = onMakeQuiz, isPrimary = false)
                ActionChip(label = "💾 Save", onClick = onSave, isPrimary = false)
                ActionChip(label = "📄 PDF", onClick = onDownloadPdf, isPrimary = false)
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit,
    isPrimary: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isPrimary) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
        border = BorderStroke(1.dp, if (isPrimary) NeonCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.springClickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
            color = if (isPrimary) NeonCyan else Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

// =============================================================================
// 3. WEB MCQ GENERATOR CONFIGURATION DIALOG (Feature 1 & 2)
// =============================================================================

@Composable
fun NovaWebMcqGeneratorDialog(
    initialTopic: String = "",
    examName: String = "Competitive Exam",
    onDismiss: () -> Unit,
    onGenerate: (SmartMcqConfig) -> Unit
) {
    var topicText by remember { mutableStateOf(initialTopic) }
    var selectedCount by remember { mutableStateOf(10) }
    var selectedDifficulty by remember { mutableStateOf("Mixed") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var selectedType by remember { mutableStateOf("MCQ") }

    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0B0F19.toULong()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) DarkSurfaceElevated else Color.White,
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonCyan, ElectricIndigo))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎯", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Fresh Practice MCQ Generator",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else DarkCanvas
                        )
                        Text(
                            text = "AI-generated practice based on live web facts",
                            fontSize = 11.sp,
                            color = if (isDark) TextSecondary else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Topic Input
                Text(
                    text = "Topic or Search Term",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DarkCanvas
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = topicText,
                    onValueChange = { topicText = it },
                    placeholder = { Text("e.g. Recent space missions, PM Surya Ghar, Budget 2026") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mcq_topic_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Number of Questions
                Text(
                    text = "Number of Questions",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DarkCanvas
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 20, 30).forEach { count ->
                        val isSelected = selectedCount == count
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NeonCyan else (if (isDark) DarkSurface else Color(0xFFF1F5F9)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCount = count }
                        ) {
                            Text(
                                text = "$count",
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DarkCanvas else (if (isDark) Color.White else DarkCanvas),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Difficulty
                Text(
                    text = "Difficulty Level",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DarkCanvas
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Easy", "Medium", "Hard", "Mixed").forEach { diff ->
                        val isSelected = selectedDifficulty == diff
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ElectricIndigo else (if (isDark) DarkSurface else Color(0xFFF1F5F9)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDifficulty = diff }
                        ) {
                            Text(
                                text = diff,
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else (if (isDark) Color.White else DarkCanvas),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Language
                Text(
                    text = "Language",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DarkCanvas
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("English", "Hindi").forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) EmeraldGreen else (if (isDark) DarkSurface else Color(0xFFF1F5F9)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedLanguage = lang }
                        ) {
                            Text(
                                text = lang,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DarkCanvas else (if (isDark) Color.White else DarkCanvas),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val config = SmartMcqConfig(
                                questionCount = selectedCount,
                                difficulty = selectedDifficulty,
                                language = selectedLanguage,
                                questionType = selectedType,
                                examName = examName,
                                topicQuery = topicText
                            )
                            onGenerate(config)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkCanvas),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("generate_start_quiz_btn")
                    ) {
                        Text("Generate & Quiz", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =============================================================================
// 4. 6-STEP SMART REVISION SESSION DIALOG (Feature 3 - Multi-Step Spaced Loop)
// =============================================================================

@Composable
fun SmartRevisionSessionDialog(
    item: SmartRevisionTopicItem,
    examName: String,
    onDismiss: () -> Unit,
    onComplete: (score: Int, total: Int) -> Unit,
    onAskNova: (prompt: String) -> Unit
) {
    var currentStep by remember { mutableStateOf(1) } // 1: Recap, 2: Facts, 3: Why it matters, 4: MCQs, 5: Mistake Review, 6: Status
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    val submittedAnswers = remember { mutableStateMapOf<Int, Boolean>() }
    var saveStatus by remember { mutableStateOf("") }

    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0B0F19.toULong()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) DarkSurfaceElevated else Color.White,
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header with Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔄 Smart Revision",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else DarkCanvas
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricIndigo.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Step $currentStep/6",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricIndigo,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { currentStep / 6f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = NeonCyan,
                    trackColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Step Content Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (currentStep) {
                        1 -> RevisionStep1Recap(item, isDark)
                        2 -> RevisionStep2Facts(item, isDark)
                        3 -> RevisionStep3WhyItMatters(item, examName, isDark)
                        4 -> RevisionStep4ShortMcqs(
                            questions = item.miniQuizQuestions,
                            selectedAnswers = selectedAnswers,
                            submittedAnswers = submittedAnswers,
                            onSelect = { qIdx, optIdx ->
                                selectedAnswers[qIdx] = optIdx
                                submittedAnswers[qIdx] = true
                            },
                            isDark = isDark
                        )
                        5 -> RevisionStep5MistakeReview(item, isDark)
                        6 -> RevisionStep6SaveStatus(item, saveStatus, isDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive [Ask NOVA] contextual suggestions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Ye simple language me samjhao",
                        "Example do",
                        "Iska shortcut kya hai",
                        "Similar question do"
                    ).forEach { prompt ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) DarkSurface else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.springClickable {
                                onAskNova("Context: ${item.title}\nStudent Prompt: $prompt")
                            }
                        ) {
                            Text(
                                text = "💬 $prompt",
                                fontSize = 11.sp,
                                color = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Controls (Back / Next / Finish)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("← Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (currentStep < 6) {
                        Button(
                            onClick = { currentStep++ },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkCanvas),
                            modifier = Modifier.testTag("revision_next_btn")
                        ) {
                            Text("Next Step →", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                saveStatus = "✓ Saved to Smart Notes & Spaced Repetition"
                                onComplete(selectedAnswers.size, item.miniQuizQuestions.size)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = DarkCanvas),
                            modifier = Modifier.testTag("revision_complete_btn")
                        ) {
                            Text("Complete Revision ✓", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Step 1: Quick Recap ----------------
@Composable
private fun RevisionStep1Recap(item: SmartRevisionTopicItem, isDark: Boolean) {
    Column {
        Text(
            text = "Step 1: 📖 Quick Recap",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) NeonCyan else DeepIndigo
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color.White else DarkCanvas
        )
        Text(
            text = "${item.subject} • ${item.priority.label}",
            fontSize = 12.sp,
            color = if (isDark) TextSecondary else Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) DarkSurface else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = item.recapSummary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1E293B),
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

// ---------------- Step 2: Important Facts ----------------
@Composable
private fun RevisionStep2Facts(item: SmartRevisionTopicItem, isDark: Boolean) {
    Column {
        Text(
            text = "Step 2: 📌 Important Facts",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) NeonCyan else DeepIndigo
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "High-Yield Facts to Remember:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color.White else DarkCanvas
        )
        Spacer(modifier = Modifier.height(10.dp))
        item.importantFacts.forEachIndexed { idx, fact ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) DarkSurface else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fact,
                        fontSize = 12.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF334155),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ---------------- Step 3: Why It Matters ----------------
@Composable
private fun RevisionStep3WhyItMatters(item: SmartRevisionTopicItem, examName: String, isDark: Boolean) {
    Column {
        Text(
            text = "Step 3: 🎯 Why It Matters (Exam Relevance)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) NeonCyan else DeepIndigo
        )
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) DarkSurface else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, CoralOrange.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Why this is tested in $examName:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else DarkCanvas
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.whyItMatters,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF334155)
                )
            }
        }
    }
}

// ---------------- Step 4: Short Practice MCQs ----------------
@Composable
private fun RevisionStep4ShortMcqs(
    questions: List<Question>,
    selectedAnswers: Map<Int, Int>,
    submittedAnswers: Map<Int, Boolean>,
    onSelect: (qIdx: Int, optIdx: Int) -> Unit,
    isDark: Boolean
) {
    Column {
        Text(
            text = "Step 4: ✍️ Quick Practice Question",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) NeonCyan else DeepIndigo
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (questions.isEmpty()) {
            Text("No practice questions attached for this topic.", color = TextSecondary, fontSize = 12.sp)
        } else {
            questions.take(2).forEachIndexed { qIdx, q ->
                val selected = selectedAnswers[qIdx]
                val submitted = submittedAnswers[qIdx] == true

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) DarkSurface else Color(0xFFF8FAFC),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${qIdx + 1}. ${q.questionText}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else DarkCanvas
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        q.options.forEachIndexed { optIdx, optText ->
                            val isChosen = selected == optIdx
                            val isCorrect = optIdx == q.correctOptionIndex
                            val optBg = when {
                                submitted && isCorrect -> EmeraldGreen.copy(alpha = 0.2f)
                                submitted && isChosen && !isCorrect -> CrimsonRed.copy(alpha = 0.2f)
                                isChosen -> NeonCyan.copy(alpha = 0.15f)
                                else -> if (isDark) DarkSurfaceElevated else Color.White
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = optBg,
                                border = BorderStroke(1.dp, if (isChosen) NeonCyan else Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { if (!submitted) onSelect(qIdx, optIdx) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${('A' + optIdx)}. $optText",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color.White else DarkCanvas
                                    )
                                }
                            }
                        }

                        if (submitted) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 ${q.explanation}",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Step 5: Mistake Review ----------------
@Composable
private fun RevisionStep5MistakeReview(item: SmartRevisionTopicItem, isDark: Boolean) {
    Column {
        Text(
            text = "Step 5: 🔍 Mistake Analysis & Retention",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) NeonCyan else DeepIndigo
        )
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) DarkSurface else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Common Pitfalls to Avoid in ${item.topic}:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DarkCanvas
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Confusing similar ministry names or executing bodies\n• Forgetting threshold dates or exact financial budget allocations\n• Missing boundary exceptions in numerical and formula questions",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            }
        }
    }
}

// ---------------- Step 6: Save & Spaced Status ----------------
@Composable
private fun RevisionStep6SaveStatus(item: SmartRevisionTopicItem, saveStatus: String, isDark: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(EmeraldGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Step 6: Revision Session Finished!",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color.White else DarkCanvas
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your spaced repetition interval has been updated.",
            fontSize = 12.sp,
            color = if (isDark) TextSecondary else Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        if (saveStatus.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    text = saveStatus,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// =============================================================================
// 5. POST-QUIZ RECOMMENDED REVISION BANNER (Feature 3 & 4)
// =============================================================================

@Composable
fun RecommendedRevisionBanner(
    weakTopicCount: Int,
    onStartRevision: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (weakTopicCount <= 0) return

    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0B0F19.toULong()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CoralOrange.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, CoralOrange.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recommended_revision_banner")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔄", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Recommended Revision",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else DarkCanvas
                    )
                    Text(
                        text = "$weakTopicCount topics need revision from this quiz.",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFFED7AA) else CoralOrange
                    )
                }
            }

            Button(
                onClick = onStartRevision,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralOrange, contentColor = Color.White),
                modifier = Modifier.testTag("start_recommended_revision_btn")
            ) {
                Text("Revise Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =============================================================================
// 7. FULL DAILY EXAM BRIEFING DETAIL DIALOG (Feature 4)
// =============================================================================

@Composable
fun DailyExamBriefingDialog(
    briefing: DailyExamBriefing?,
    onDismiss: () -> Unit,
    onStartPractice: (String) -> Unit,
    onAskNova: (String) -> Unit
) {
    if (briefing == null) return

    val isDark = isAppInDarkTheme()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) DarkSurface else Color.White,
            border = BorderStroke(1.dp, if (isDark) NeonCyan.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.1f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .testTag("daily_exam_briefing_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📰", fontSize = 18.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Today's Exam Briefing",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else DarkCanvas
                            )
                            Text(
                                text = "${briefing.dateFormatted} • ${briefing.examName}",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                HorizontalDivider(
                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Scrollable Briefing Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Study Priority Focus
                    if (briefing.studyPriorityTopic.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) DarkSurfaceElevated else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("🎯", fontSize = 15.sp)
                                        Text(
                                            text = "Today's Focus: ${briefing.studyPriorityTopic}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        )
                                    }
                                    if (briefing.prioritySubject.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Subject: ${briefing.prioritySubject}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ElectricViolet
                                        )
                                    }
                                    if (briefing.priorityRationale.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = briefing.priorityRationale,
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Official Notice (if any)
                    if (briefing.officialNotice != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CoralOrange.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, CoralOrange.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("🏛️", fontSize = 14.sp)
                                        Text(
                                            text = "Official Exam Update",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFFED7AA) else CoralOrange
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = briefing.officialNotice.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color.White else DarkCanvas
                                    )
                                    if (briefing.officialNotice.summary.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = briefing.officialNotice.summary,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Top Current Affairs / Exam Highlights
                    if (briefing.topCurrentAffairs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Top Current Affairs & Developments",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else DarkCanvas,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        items(briefing.topCurrentAffairs) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) DarkSurfaceElevated else Color(0xFFF8FAFC),
                                border = BorderStroke(0.5.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else DarkCanvas,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ElectricViolet.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = item.category,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ElectricViolet,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (item.summary.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.summary,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                            lineHeight = 16.sp
                                        )
                                    }

                                    if (item.examRelevance.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = GoldenSpark.copy(alpha = 0.1f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "🎯 ${item.examRelevance}",
                                                fontSize = 10.sp,
                                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                                                lineHeight = 14.sp,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                    }

                                    // Action
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { onAskNova("Explain ${item.title} in detail for ${briefing.examName}") }
                                        ) {
                                            Text("Ask NOVA", fontSize = 11.sp, color = NeonCyan)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Button(
                                            onClick = { onStartPractice(item.title) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Practice MCQs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action: Practice Daily Briefing Quiz
                Button(
                    onClick = {
                        onDismiss()
                        val targetTopic = if (briefing.studyPriorityTopic.isNotBlank()) briefing.studyPriorityTopic else briefing.examName
                        onStartPractice(targetTopic)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("🎯 Start Today's Briefing Practice", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

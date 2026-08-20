package com.example.ui.screens.learning

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun LearningDashboardScreen(
    user: UserProfile?,
    examContext: ExamContext,
    subjectSummaries: List<SubjectProgressSummary> = emptyList(),
    allMasteries: List<TopicMastery> = emptyList(),
    bookmarks: List<UserLearningBookmark> = emptyList(),
    smartNotes: List<SmartNoteItem> = emptyList(),
    onSelectSubject: (String) -> Unit,
    onSelectTopic: (subject: String, chapter: String, topic: String) -> Unit,
    onOpenSearch: (query: String) -> Unit,
    onChangeExam: () -> Unit,
    onOpenSavedLearning: () -> Unit = {},
    onAskNova: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var searchQuery by remember { mutableStateOf("") }

    val activeExamName = user?.examName?.ifBlank { examContext.examName } ?: "RRB NTPC"

    // Derive active exam subjects
    val activeSubjects = remember(examContext) {
        val mapped = examContext.subjects.map { it.name }
        if (mapped.isNotEmpty()) mapped else listOf("Mathematics", "General Intelligence & Reasoning", "General Awareness", "General Science")
    }

    // Last active topic for "Continue Learning"
    val continueTopic = remember(allMasteries) {
        allMasteries.sortedByDescending { it.lastStudiedMillis }.firstOrNull()
    }

    // Weak topics
    val weakTopicsList = remember(allMasteries, user?.weakTopics) {
        val weakFromMastery = allMasteries.filter { it.masteryScore < 45 || it.masteryState == "WEAK" }
        if (weakFromMastery.isNotEmpty()) weakFromMastery else emptyList()
    }

    // Personalized Recommendations (Max 3)
    val recommendations = remember(allMasteries, weakTopicsList) {
        val recs = mutableListOf<Triple<String, String, String>>() // Subject, Topic, Reason
        weakTopicsList.take(2).forEach { wt ->
            val reason = if (wt.masteryScore < 30) "Low mastery (${wt.masteryScore}%)" else "Repeated mistakes"
            recs.add(Triple(wt.subject, wt.topic, reason))
        }
        if (recs.size < 3) {
            val highYield = allMasteries.filter { it.masteryScore in 45..70 }.take(3 - recs.size)
            highYield.forEach { hy ->
                recs.add(Triple(hy.subject, hy.topic, "High yield exam topic"))
            }
        }
        if (recs.isEmpty()) {
            recs.add(Triple("Mathematics", "Percentage & Applications", "Fundamental syllabus topic"))
        }
        recs.take(3)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundGradient(isDark))
            .padding(horizontal = 16.dp)
            .testTag("learning_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // ==========================================
        // 1. ACTIVE EXAM BANNER
        // ==========================================
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF0F172A).copy(alpha = 0.85f),
                borderColor = NeonCyan
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NeonCyan, Color(0xFF2563EB)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.School, "Exam", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("PREPARING FOR:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = activeExamName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onChangeExam,
                        modifier = Modifier.testTag("change_exam_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(NeonCyan, ElectricViolet)))
                    ) {
                        Text("Change ✎", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==========================================
        // 2. SEARCH IN STUDY CONTENT
        // ==========================================
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search formulas, concepts, or topics in $activeExamName...", color = Color(0xFF64748B), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = NeonCyan) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onOpenSearch(searchQuery) }) {
                            Icon(Icons.Filled.ArrowForward, "Search", tint = NeonCyan)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study_search_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0x35FFFFFF),
                    focusedContainerColor = Color(0x30111827),
                    unfocusedContainerColor = Color(0x20111827)
                ),
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )
        }

        // ==========================================
        // 3. RECOMMENDED FOR YOU (Max 3)
        // ==========================================
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recommended for You", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text("Top 3 Targets", style = MaterialTheme.typography.labelSmall, color = GoldenSpark, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    recommendations.forEach { (sub, top, reason) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x2038BDF8))
                                .clickable { onSelectTopic(sub, "General", top) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(top, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("$sub • $reason", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                            }
                            Icon(Icons.Filled.ArrowForward, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. ASK NOVA AI TUTOR BANNER
        // ==========================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAskNova() },
                backgroundColor = Color(0xFF1E1B4B).copy(alpha = 0.9f),
                borderColor = ElectricViolet
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ElectricViolet, NeonCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Psychology, "Nova AI", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ask Nova AI Tutor 💬", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Doubt solving, simple explanations & Socratic mode", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC7D2FE))
                        }
                    }

                    Button(
                        onClick = onAskNova,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("ask_nova_banner_btn")
                    ) {
                        Text("Chat →", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PlayCircle, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continue Learning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x3010B981))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Active Topic", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val topicName = continueTopic?.topic ?: "Percentage & Applications"
                    val subName = continueTopic?.subject ?: activeSubjects.firstOrNull() ?: "Mathematics"
                    val mScore = continueTopic?.masteryScore ?: 50

                    Text(text = topicName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NeonCyan)
                    Text(text = "$subName • Active Syllabus", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { (mScore / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NeonCyan,
                            trackColor = Color(0x30FFFFFF)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onSelectTopic(subName, "General Chapter", topicName) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.testTag("resume_learning_btn")
                        ) {
                            Text("Resume", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. SUBJECTS GRID (Strictly for Active Exam)
        // ==========================================
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("EXAM SUBJECTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (subName in activeSubjects) {
                        val summary = subjectSummaries.firstOrNull { it.subjectName.equals(subName, ignoreCase = true) }

                        val mastery = summary?.averageMasteryScore ?: 60

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSelectSubject(subName) },
                            testTag = "subject_card_${subName.lowercase().replace(" ", "_")}"
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x3038BDF8)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Book, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(subName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Mastery: $mastery% • Tap to view chapters", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                    }
                                }

                                Icon(Icons.Filled.ChevronRight, null, tint = NeonCyan)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. WEAK TOPICS ATTENTION
        // ==========================================
        if (weakTopicsList.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Weak Topics Needing Attention", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        weakTopicsList.take(3).forEach { wt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x30DC2626))
                                    .clickable { onSelectTopic(wt.subject, wt.chapter.ifBlank { "General" }, wt.topic) }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(wt.topic, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${wt.subject} • Mastery: ${wt.masteryScore}%", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFCA5A5))
                                }
                                Text("Fix Now →", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. BOOKMARKS & SAVED NOTES
        // ==========================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSavedLearning() }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saved Notes & Bookmarks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        TextButton(
                            onClick = onOpenSavedLearning,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.testTag("open_saved_learning_btn")
                        ) {
                            Text("View All (${bookmarks.size + smartNotes.size}) →", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (bookmarks.isEmpty() && smartNotes.isEmpty()) {
                        Text("No saved bookmarks or notes yet. Bookmark topics or save private notes during study.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    } else {
                        bookmarks.take(2).forEach { bm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Bookmark, null, tint = GoldenSpark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${bm.topic}: ${bm.title}", style = MaterialTheme.typography.bodySmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

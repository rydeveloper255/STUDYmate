package com.example.ui.screens.learning

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    examContext: ExamContext,
    subject: String,
    chapter: String,
    topic: String,
    topicMastery: TopicMastery? = null,
    learningContent: LearningTopicContent? = null,
    isLoading: Boolean = false,
    userNotes: String = "",
    userMistakes: List<MistakeItem> = emptyList(),
    onBack: () -> Unit,
    onRefreshContent: () -> Unit,
    onSaveNote: (String) -> Unit,
    onToggleBookmark: (title: String, snippet: String, type: String) -> Unit,
    onCompleteQuickTest: (score: Int, total: Int) -> Unit,
    onAskNovaDoubt: (prompt: String) -> Unit,
    novaDoubtResponse: String = "",
    isNovaThinking: Boolean = false,
    onSpeakTts: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Concept, 1: Formulas, 2: Examples, 3: Practice, 4: Common Mistakes, 5: Quick Test, 6: Ask Nova, 7: Notes
    var explanationLevel by remember { mutableStateOf("Normal") } // Quick, Normal, Detailed
    var selectedLanguage by remember { mutableStateOf("English") } // English, Hindi, Hinglish, Bilingual

    var isBookmarked by remember { mutableStateOf(false) }
    var noteInput by remember(userNotes) { mutableStateOf(userNotes) }

    val tabs = listOf(
        "📖 Concept",
        "📐 Formulas",
        "✍️ Examples",
        "🎯 Practice",
        "⚠️ Mistakes",
        "⚡ Quick Test",
        "✨ Ask Nova",
        "📝 Notes"
    )

    // Parsed Content JSON Structures
    val keyPointsList = remember(learningContent?.keyPointsJson) {
        try {
            val arr = JSONArray(learningContent?.keyPointsJson ?: "[]")
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val formulasList = remember(learningContent?.formulasJson) {
        try {
            val arr = JSONArray(learningContent?.formulasJson ?: "[]")
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val workedExamplesList = remember(learningContent?.workedExamplesJson) {
        try {
            val arr = JSONArray(learningContent?.workedExamplesJson ?: "[]")
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                val stepsArr = obj.optJSONArray("steps")
                val stepsList = if (stepsArr != null) List(stepsArr.length()) { stepsArr.getString(it) } else emptyList()
                WorkedExampleItem(
                    question = obj.optString("question", ""),
                    approach = obj.optString("approach", ""),
                    steps = stepsList,
                    finalAnswer = obj.optString("finalAnswer", ""),
                    shortcutTip = obj.optString("shortcutTip", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val commonMistakesList = remember(learningContent?.commonMistakesJson) {
        try {
            val arr = JSONArray(learningContent?.commonMistakesJson ?: "[]")
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val practiceQuestionsList = remember(learningContent?.practiceQuestionsJson) {
        try {
            val arr = JSONArray(learningContent?.practiceQuestionsJson ?: "[]")
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                val optsArr = obj.optJSONArray("options")
                val opts = if (optsArr != null) List(optsArr.length()) { optsArr.getString(it) } else listOf("A", "B", "C", "D")
                val hintsArr = obj.optJSONArray("hints")
                val hints = if (hintsArr != null) List(hintsArr.length()) { hintsArr.getString(it) } else emptyList()
                PracticeQuestionWithHints(
                    questionText = obj.optString("questionText", ""),
                    options = opts,
                    correctOptionIndex = obj.optInt("correctOptionIndex", 0),
                    hints = hints,
                    fullExplanation = obj.optString("fullExplanation", ""),
                    difficulty = obj.optString("difficulty", "Medium"),
                    sourceBadge = obj.optString("sourceBadge", "✨ AI Practice")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val quickTestList = remember(learningContent?.quickTestQuestionsJson) {
        try {
            val arr = JSONArray(learningContent?.quickTestQuestionsJson ?: "[]")
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                val optsArr = obj.optJSONArray("options")
                val opts = if (optsArr != null) List(optsArr.length()) { optsArr.getString(it) } else listOf("A", "B", "C", "D")
                QuickTestQuestion(
                    questionText = obj.optString("questionText", ""),
                    options = opts,
                    correctOptionIndex = obj.optInt("correctOptionIndex", 0),
                    explanation = obj.optString("explanation", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .testTag("topic_detail_screen")
    ) {
        // ==========================================
        // 1. TOP BAR (Back, Title, Mastery Badge, Bookmark)
        // ==========================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A).copy(alpha = 0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("topic_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "${examContext.examName} • $subject • $chapter",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mastery Score Badge
                    val mScore = topicMastery?.masteryScore ?: 50
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (mScore >= 75) Color(0x3010B981)
                                else if (mScore >= 40) Color(0x30F59E0B)
                                else Color(0x30EF4444)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Mastery: $mScore%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (mScore >= 75) Color(0xFF34D399) else if (mScore >= 40) Color(0xFFFBBF24) else Color(0xFFF87171)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            isBookmarked = !isBookmarked
                            onToggleBookmark(topic, learningContent?.conceptSummary ?: topic, "TOPIC")
                            Toast.makeText(context, if (isBookmarked) "Saved to Bookmarks" else "Removed Bookmark", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("topic_bookmark_btn")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) GoldenSpark else Color.White
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. TABS SCROLLABLE ROW
        // ==========================================
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.indices.toList()) { idx ->
                val isSelected = selectedTab == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Brush.linearGradient(listOf(NeonCyan, Color(0xFF2563EB)))
                            else Brush.linearGradient(listOf(Color(0x20FFFFFF), Color(0x10FFFFFF)))
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0x8038BDF8) else Color(0x15FFFFFF),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTab = idx }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("learning_tab_$idx")
                ) {
                    Text(
                        text = tabs[idx],
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF070B19) else Color(0xFFCBD5E1)
                    )
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Generating structured AI learning material for $topic...", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                }
            }
            return
        }

        // ==========================================
        // 3. TAB CONTENT VIEWER
        // ==========================================
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> ConceptExplanationView(
                    learningContent = learningContent,
                    explanationLevel = explanationLevel,
                    selectedLanguage = selectedLanguage,
                    keyPoints = keyPointsList,
                    onLevelChange = { explanationLevel = it },
                    onLanguageChange = { selectedLanguage = it },
                    onRefreshContent = onRefreshContent,
                    onSpeakTts = onSpeakTts
                )
                1 -> FormulasView(formulasList = formulasList)
                2 -> WorkedExamplesView(examples = workedExamplesList)
                3 -> PracticeWithHintsView(questions = practiceQuestionsList)
                4 -> CommonMistakesView(generalMistakes = commonMistakesList, userMistakes = userMistakes)
                5 -> QuickTestView(questions = quickTestList, onComplete = onCompleteQuickTest)
                6 -> AskNovaDoubtView(
                    topic = topic,
                    onAskDoubt = onAskNovaDoubt,
                    novaResponse = novaDoubtResponse,
                    isNovaThinking = isNovaThinking
                )
                7 -> TopicNotesView(
                    noteInput = noteInput,
                    onNoteChange = { noteInput = it },
                    onSaveNote = {
                        onSaveNote(noteInput)
                        Toast.makeText(context, "Note saved privately", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// =========================================================================
// TAB 0: CONCEPT & EXPLANATION (Quick/Normal/Detailed + Language + TTS)
// =========================================================================
@Composable
private fun ConceptExplanationView(
    learningContent: LearningTopicContent?,
    explanationLevel: String,
    selectedLanguage: String,
    keyPoints: List<String>,
    onLevelChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onRefreshContent: () -> Unit,
    onSpeakTts: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Control Bar: Explanation Level + Language Selector
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("EXPLANATION DEPTH:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Quick", "Normal", "Detailed").forEach { lvl ->
                                FilterChip(
                                    selected = explanationLevel == lvl,
                                    onClick = { onLevelChange(lvl) },
                                    label = { Text(lvl, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = Color(0xFF070B19),
                                        containerColor = Color(0x20FFFFFF),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LANGUAGE:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(listOf("English", "Hindi", "Hinglish", "Bilingual")) { lang ->
                                FilterChip(
                                    selected = selectedLanguage == lang,
                                    onClick = { onLanguageChange(lang) },
                                    label = { Text(lang, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricViolet,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0x20FFFFFF),
                                        labelColor = Color(0xFFCBD5E1)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // AI Labeling Badge & Action Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x306366F1))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("✨ AI-Generated Explanation", style = MaterialTheme.typography.labelSmall, color = ElectricIndigo, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("• Verified Structure", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                }

                Row {
                    val currentExplanationText = when (explanationLevel) {
                        "Quick" -> learningContent?.explanationQuick ?: ""
                        "Detailed" -> learningContent?.explanationDetailed ?: ""
                        else -> learningContent?.explanationNormal ?: ""
                    }

                    IconButton(onClick = { onSpeakTts(currentExplanationText) }) {
                        Icon(Icons.AutoMirrored.Outlined.VolumeUp, "Listen", tint = NeonCyan)
                    }

                    IconButton(onClick = onRefreshContent) {
                        Icon(Icons.Filled.Refresh, "Regenerate", tint = Color.White)
                    }
                }
            }
        }

        // Core Concept Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("💡 Core Concept Intuition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldenSpark)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = learningContent?.conceptSummary ?: "Core concept summary for this topic.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Active Explanation Section
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "$explanationLevel Explanation ($selectedLanguage)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val explanationBody = when (explanationLevel) {
                        "Quick" -> learningContent?.explanationQuick ?: ""
                        "Detailed" -> learningContent?.explanationDetailed ?: ""
                        else -> learningContent?.explanationNormal ?: ""
                    }

                    Text(
                        text = explanationBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Key Points Takeaways
        if (keyPoints.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📌 High-Yield Key Points", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        keyPoints.forEach { point ->
                            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Text("• ", color = NeonCyan, fontWeight = FontWeight.Bold)
                                Text(text = point, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 1: FORMULAS & KEY RULES
// =========================================================================
@Composable
private fun FormulasView(formulasList: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text("📐 Important Formulas & Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (formulasList.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No explicit formulas listed for this topic. Review concept and worked examples.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), modifier = Modifier.padding(12.dp))
                }
            }
        } else {
            items(formulasList) { formula ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Functions, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = formula, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: WORKED EXAMPLES (Step-by-Step Solutions)
// =========================================================================
@Composable
private fun WorkedExamplesView(examples: List<WorkedExampleItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text("✍️ Step-by-Step Worked Examples", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        items(examples) { item ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Q: ${item.question}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NeonCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Approach: ${item.approach}", style = MaterialTheme.typography.labelMedium, color = GoldenSpark, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    item.steps.forEachIndexed { sIdx, step ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("${sIdx + 1}. ", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            Text(step, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE2E8F0))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3010B981))
                            .padding(8.dp)
                    ) {
                        Text("Final Answer: ${item.finalAnswer}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                    }

                    if (item.shortcutTip.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("⚡ Pro Trick: ${item.shortcutTip}", style = MaterialTheme.typography.labelSmall, color = ElectricViolet, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 3: PRACTICE WITH PROGRESSIVE HINTS
// =========================================================================
@Composable
private fun PracticeWithHintsView(questions: List<PracticeQuestionWithHints>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text("🎯 Practice Questions with Progressive Hints", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        items(questions) { q ->
            var selectedOpt by remember { mutableIntStateOf(-1) }
            var hintLevel by remember { mutableIntStateOf(0) } // 0: No hints, 1: Hint 1, 2: Hint 2, 3: Full Solution

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(q.sourceBadge, style = MaterialTheme.typography.labelSmall, color = ElectricIndigo, fontWeight = FontWeight.Bold)
                        Text(q.difficulty, style = MaterialTheme.typography.labelSmall, color = GoldenSpark)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(q.questionText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))

                    q.options.forEachIndexed { oIdx, opt ->
                        val isSelected = selectedOpt == oIdx
                        val isCorrect = oIdx == q.correctOptionIndex
                        val isChecked = selectedOpt != -1

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isChecked && isCorrect) Color(0x4010B981)
                                    else if (isChecked && isSelected) Color(0x40EF4444)
                                    else if (isSelected) Color(0x3038BDF8)
                                    else Color(0x20FFFFFF)
                                )
                                .border(
                                    1.dp,
                                    if (isChecked && isCorrect) Color(0xFF10B981)
                                    else if (isChecked && isSelected) Color(0xFFEF4444)
                                    else Color(0x15FFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { if (selectedOpt == -1) selectedOpt = oIdx }
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "${('A' + oIdx)}. $opt",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progressive Hint Reveal Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { if (hintLevel < 3) hintLevel++ },
                            enabled = hintLevel < 3
                        ) {
                            Text(
                                text = when (hintLevel) {
                                    0 -> "💡 Need Hint 1"
                                    1 -> "💡 Show Hint 2"
                                    2 -> "📖 Show Full Solution"
                                    else -> "Solution Revealed"
                                },
                                color = GoldenSpark,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedOpt != -1) {
                            Text(
                                text = if (selectedOpt == q.correctOptionIndex) "Correct! 🎉" else "Incorrect ❌",
                                color = if (selectedOpt == q.correctOptionIndex) Color(0xFF34D399) else Color(0xFFF87171),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Display Hint or Solution depending on hintLevel
                    if (hintLevel >= 1 && q.hints.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x25F59E0B))
                                .padding(8.dp)
                        ) {
                            Text("Hint 1: ${q.hints[0]}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFBBF24))
                        }
                    }

                    if (hintLevel >= 2 && q.hints.size >= 2) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x25F59E0B))
                                .padding(8.dp)
                        ) {
                            Text("Hint 2: ${q.hints[1]}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFBBF24))
                        }
                    }

                    if (hintLevel >= 3 || selectedOpt != -1) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x2038BDF8))
                                .padding(8.dp)
                        ) {
                            Text("Full Solution: ${q.fullExplanation}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE2E8F0))
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 4: COMMON MISTAKES (General vs Your Personal Mistakes)
// =========================================================================
@Composable
private fun CommonMistakesView(generalMistakes: List<String>, userMistakes: List<MistakeItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text("⚠️ Common Pitfalls & Personal Mistakes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Your Personal Recorded Mistakes (from database)
        if (userMistakes.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🛑 Your Personal Past Mistakes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                        Spacer(modifier = Modifier.height(6.dp))
                        for (m in userMistakes) {
                            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Text("• ", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                Column {
                                    Text(m.questionText, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Category: ${m.mistakeCategory.ifBlank { "Conceptual / Calculation" }}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFCA5A5))
                                }
                            }
                        }

                    }
                }
            }
        }

        // General Common Mistakes
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("💡 General Exam Traps to Avoid", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldenSpark)
                    Spacer(modifier = Modifier.height(6.dp))
                    generalMistakes.forEach { pitfall ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Text("⚠️ ", fontSize = 12.sp)
                            Text(pitfall, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE2E8F0))
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 5: QUICK TEST (5 Check Questions)
// =========================================================================
@Composable
private fun QuickTestView(questions: List<QuickTestQuestion>, onComplete: (score: Int, total: Int) -> Unit) {
    var userAnswers by remember { mutableStateOf(IntArray(questions.size) { -1 }) }
    var isSubmitted by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("⚡ Quick Understanding Check (5 Questions)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                if (isSubmitted) {
                    val correct = userAnswers.indices.count { idx -> userAnswers[idx] == questions[idx].correctOptionIndex }
                    Text("Score: $correct/${questions.size}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }
        }

        items(questions.indices.toList()) { qIdx ->
            val q = questions[qIdx]
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${qIdx + 1}. ${q.questionText}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))

                    q.options.forEachIndexed { oIdx, opt ->
                        val isSelected = userAnswers[qIdx] == oIdx
                        val isCorrect = oIdx == q.correctOptionIndex

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSubmitted && isCorrect) Color(0x4010B981)
                                    else if (isSubmitted && isSelected) Color(0x40EF4444)
                                    else if (isSelected) Color(0x3038BDF8)
                                    else Color(0x20FFFFFF)
                                )
                                .clickable {
                                    if (!isSubmitted) {
                                        val newAns = userAnswers.copyOf()
                                        newAns[qIdx] = oIdx
                                        userAnswers = newAns
                                    }
                                }
                                .padding(8.dp)
                        ) {
                            Text("${('A' + oIdx)}. $opt", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }

                    if (isSubmitted) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Explanation: ${q.explanation}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    }
                }
            }
        }

        item {
            if (!isSubmitted) {
                Button(
                    onClick = {
                        isSubmitted = true
                        val correct = userAnswers.indices.count { idx -> userAnswers[idx] == questions[idx].correctOptionIndex }
                        onComplete(correct, questions.size)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Submit Check", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// TAB 6: ASK NOVA (Contextual Doubt Solver)
// =========================================================================
@Composable
private fun AskNovaDoubtView(
    topic: String,
    onAskDoubt: (prompt: String) -> Unit,
    novaResponse: String,
    isNovaThinking: Boolean
) {
    var doubtInput by remember { mutableStateOf("") }

    val quickPrompts = listOf(
        "Iska easy example do",
        "Easy language mein samjhao",
        "Iska shortcut formula hai?",
        "Exam mein ye kaise poochha ja sakta hai?",
        "Ek practice question do"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("✨ Ask Nova Contextual Doubt Solver", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldenSpark)
        Text("Nova understands your context for $topic. Ask any doubt:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(quickPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x251E293B))
                        .border(1.dp, Color(0x3538BDF8), RoundedCornerShape(12.dp))
                        .clickable { onAskDoubt(prompt) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(prompt, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }
            }
        }

        GlassCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isNovaThinking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nova is analyzing your doubt on $topic...", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    }
                } else if (novaResponse.isNotBlank()) {
                    Text("Nova Tutor:", style = MaterialTheme.typography.labelSmall, color = GoldenSpark, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(novaResponse, style = MaterialTheme.typography.bodySmall, color = Color.White, lineHeight = 20.sp)
                } else {
                    Text("Ask Nova anything about $topic to get step-by-step explanations, shortcuts, or analogies.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = doubtInput,
                onValueChange = { doubtInput = it },
                placeholder = { Text("Ask doubt about $topic...", color = Color(0xFF64748B)) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonCyan)
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = {
                    if (doubtInput.isNotBlank()) {
                        val toSend = doubtInput
                        doubtInput = ""
                        onAskDoubt(toSend)
                    }
                },
                modifier = Modifier.size(48.dp).clip(CircleShape).background(NeonCyan)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color(0xFF070B19))
            }
        }
    }
}

// =========================================================================
// TAB 7: PRIVATE TOPIC NOTES
// =========================================================================
@Composable
private fun TopicNotesView(noteInput: String, onNoteChange: (String) -> Unit, onSaveNote: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("📝 Personal Notes for Topic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Private notes saved locally to your device:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))

        OutlinedTextField(
            value = noteInput,
            onValueChange = onNoteChange,
            placeholder = { Text("Type your personal formulas, memory hooks, or notes here...", color = Color(0xFF64748B)) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color(0x30FFFFFF)
            )
        )

        Button(
            onClick = onSaveNote,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Text("Save Private Note", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
        }
    }
}

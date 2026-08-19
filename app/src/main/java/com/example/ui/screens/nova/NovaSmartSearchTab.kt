package com.example.ui.screens.nova

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Question
import com.example.data.model.SmartSearchResult
import com.example.data.model.WebSearchSource
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaViewModel

@Composable
fun NovaSmartSearchTab(
    viewModel: NovaViewModel,
    onNavigateToFocus: (subject: String, topic: String, duration: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val smartResult by viewModel.smartSearchResult.collectAsState()
    val isSearching by viewModel.isSmartSearching.collectAsState()
    val studyContext by viewModel.studyContext.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(studyContext.subjects.firstOrNull() ?: "Physics") }

    val presetQueries = remember {
        listOf(
            "Bernoulli's Theorem & Derivation",
            "Carnot Engine Efficiency Formula",
            "ISRO Gaganyaan Mission Key Milestones",
            "JEE Main / NEET Syllabus Rationalization",
            "Mendel's Law of Independent Assortment",
            "Photoelectric Effect & Einstein Equation"
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Search Bar Header
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                fillAlpha = 0.85f,
                elevation = 6.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NeonCyan, ElectricIndigo))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Smart Academic Search",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "AI Search + Source Verification for ${studyContext.targetExam}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subject Selector Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val subjects = if (studyContext.subjects.isNotEmpty()) studyContext.subjects else listOf("Physics", "Chemistry", "Mathematics", "Biology", "General Awareness")
                        items(subjects) { sub ->
                            val isSelected = sub == selectedSubject
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0x18FFFFFF))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) NeonCyan else Color(0x33FFFFFF),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .springClickable(
                                        testTag = "sub_chip_$sub",
                                        onClick = { selectedSubject = sub }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) NeonCyan else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Field & Search Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search concepts, derivations, formulas...",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("smart_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedContainerColor = Color(0x15000000),
                                unfocusedContainerColor = Color(0x15000000)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray)
                                    }
                                }
                            }
                        )

                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    viewModel.performSmartSearch(
                                        query = searchQuery,
                                        subject = selectedSubject
                                    )
                                }
                            },
                            enabled = searchQuery.isNotBlank() && !isSearching,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("smart_search_btn")
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.ArrowForward, contentDescription = "Search")
                            }
                        }
                    }
                }
            }
        }

        // 2. Preset Query Pills
        if (smartResult == null && !isSearching) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "🔥 High-Yield Academic Queries",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    presetQueries.forEach { preset ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            fillAlpha = 0.5f
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .springClickable(
                                        testTag = "preset_query_$preset",
                                        onClick = {
                                            searchQuery = preset
                                            viewModel.performSmartSearch(preset, subject = selectedSubject)
                                        }
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Search Result Display
        smartResult?.let { result ->
            // Action Bar: Save to Notes, Quiz, Plan
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    fillAlpha = 0.9f
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 Quick 1-Tap Actions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            IconButton(onClick = { viewModel.clearSmartSearch() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveSearchResultAsSmartNote(result, subject = selectedSubject)
                                    Toast.makeText(context, "Saved to Smart Notes 📝", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_as_smart_note_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x3338BDF8),
                                    contentColor = NeonCyan
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.saveSearchResultAsFlashcards(result, subject = selectedSubject)
                                    Toast.makeText(context, "Extracted Flashcards for Revision 🎴", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("extract_flashcards_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x33818CF8),
                                    contentColor = ElectricIndigo
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Flashcards", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onNavigateToFocus(selectedSubject, result.query, 25)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("schedule_focus_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x334ADE80),
                                    contentColor = Color(0xFF4ADE80)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("25m Focus", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Core Concept Synthesis
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    fillAlpha = 0.85f
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONCEPT SYNTHESIS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = result.studentFriendlyAnswer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Key Takeaways
            if (result.keyPoints.isNotEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.85f
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "🔑 High-Yield Key Points",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            result.keyPoints.forEachIndexed { i, point ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(NeonCyan.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${i + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = point,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Formulas & Definitions
            if (result.formulasAndDefinitions.isNotEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.85f
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "📐 Formulas & Core Definitions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            result.formulasAndDefinitions.forEach { formula ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x22000000))
                                        .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = formula,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Verified Web Sources & Citations
            if (result.sources.isNotEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.85f
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Verified Sources & Educational Authority",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            result.sources.forEach { source ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x18FFFFFF))
                                        .springClickable(
                                            testTag = "source_${source.domain}",
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Could not open source URL", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = source.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (source.isOfficial) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(NeonCyan.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "OFFICIAL",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NeonCyan
                                                    )
                                                }
                                            }
                                        }
                                        if (source.snippet.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = source.snippet,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.OpenInNew,
                                        contentDescription = "Open",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Generated Practice Questions
            if (result.generatedPracticeQuestions.isNotEmpty()) {
                item {
                    Text(
                        text = "🎯 Practice Quiz (${result.generatedPracticeQuestions.size} Questions)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                items(result.generatedPracticeQuestions) { q ->
                    SmartPracticeQuestionCard(question = q)
                }
            }

            // Suggested Follow-Up Inquiries
            if (result.suggestedQuestions.isNotEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.85f
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "💬 Follow-Up Queries for NOVA",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            result.suggestedQuestions.forEach { followUp ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x18FFFFFF))
                                        .springClickable(
                                            testTag = "followup_$followUp",
                                            onClick = {
                                                searchQuery = followUp
                                                viewModel.performSmartSearch(followUp, subject = selectedSubject)
                                            }
                                        )
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = followUp,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeonCyan,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(14.dp)
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

@Composable
private fun SmartPracticeQuestionCard(question: Question) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.8f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            question.options.forEachIndexed { idx, opt ->
                val isSelected = selectedOption == idx
                val isCorrect = question.correctOptionIndex == idx

                val bgColor = when {
                    isSubmitted && isCorrect -> Color(0xFF166534)
                    isSubmitted && isSelected && !isCorrect -> Color(0xFF991B1B)
                    isSelected -> NeonCyan.copy(alpha = 0.25f)
                    else -> Color(0x15FFFFFF)
                }

                val borderColor = when {
                    isSubmitted && isCorrect -> Color(0xFF4ADE80)
                    isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
                    isSelected -> NeonCyan
                    else -> Color(0x22FFFFFF)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .springClickable(
                            testTag = "opt_${question.id}_$idx",
                            onClick = {
                                if (!isSubmitted) {
                                    selectedOption = idx
                                    isSubmitted = true
                                }
                            }
                        )
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${('A' + idx)}. $opt",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSubmitted && isCorrect) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Correct", tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                        } else if (isSubmitted && isSelected && !isCorrect) {
                            Icon(Icons.Filled.Cancel, contentDescription = "Incorrect", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (isSubmitted && question.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22000000))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "💡 Explanation:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.screens.nova

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.NovaSearchHistoryItem
import com.example.data.model.Question
import com.example.data.model.SmartSearchResult
import com.example.data.model.WebSearchSource
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NovaSmartSearchTab(
    viewModel: NovaViewModel,
    onNavigateToFocus: (subject: String, topic: String, duration: Int) -> Unit,
    onBackToHub: () -> Unit = {},
    onRequestMicPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isDark = isAppInDarkTheme()

    val smartResult by viewModel.smartSearchResult.collectAsState()
    val isSearching by viewModel.isSmartSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val studyContext by viewModel.studyContext.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val searchLanguage by viewModel.searchLanguage.collectAsState()
    val selectedSubject by viewModel.searchSubject.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showSubjectDialog by remember { mutableStateOf(false) }

    // Dynamic context-aware preset suggestions based on target exam and subject
    val contextSuggestions = remember(studyContext.targetExam, selectedSubject) {
        when {
            selectedSubject.contains("Physics", ignoreCase = true) -> listOf(
                "Newton's Laws & Momentum Derivation",
                "Bernoulli's Theorem & Applications",
                "Carnot Engine & Thermodynamic Efficiency",
                "Photoelectric Effect & Einstein Equation",
                "Doppler Effect in Sound & Light Waves"
            )
            selectedSubject.contains("Chemistry", ignoreCase = true) -> listOf(
                "Periodic Trends in Ionization Energy",
                "Le Chatelier's Principle in Equilibrium",
                "Hybridization & VSEPR Molecular Geometry",
                "Faraday's Laws of Electrolysis",
                "SN1 vs SN2 Reaction Mechanisms"
            )
            selectedSubject.contains("Math", ignoreCase = true) -> listOf(
                "Integration by Parts Standard Formulas",
                "Matrix Determinants & Eigenvalues",
                "Bayes' Theorem & Probability Distribution",
                "Conic Sections Eccentricity Formulas",
                "Taylor Series Approximation Tricks"
            )
            selectedSubject.contains("Bio", ignoreCase = true) -> listOf(
                "Mendel's Laws of Heredity & Dihybrid Cross",
                "Light vs Dark Reactions in Photosynthesis",
                "Neuron Action Potential Transmission",
                "DNA Replication Enzymes & Steps",
                "Human Heart Double Circulation Flow"
            )
            else -> listOf(
                "Newton's Laws of Motion & Momentum",
                "Ohm's Law & Circuit Calculations",
                "Photosynthesis Light vs Dark Reaction",
                "Gaganyaan & ISRO Mission Milestones",
                "Indian Constitution Fundamental Rights"
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        DarkCanvas,
                        Color(0xFF0D1322),
                        DarkCanvas
                    ) else listOf(
                        BackgroundLight,
                        Color(0xFFF1F5F9),
                        BackgroundLight
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. TOP COMPACT HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackToHub,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x18FFFFFF) else Color(0x0E000000))
                                .testTag("smart_search_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isDark) Color.White else Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🔎 Smart Search",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AI + VERIFIED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) NeonCyan else DeepIndigo
                                    )
                                }
                            }
                            Text(
                                text = "Search anything related to your preparation",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (smartResult != null) {
                        IconButton(
                            onClick = {
                                viewModel.clearSmartSearch()
                                searchQuery = ""
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x15FFFFFF) else Color(0x0A000000))
                                .testTag("clear_all_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reset Search",
                                tint = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. CURRENT CONTEXT CHIP (Dynamic Exam • Subject • Topic)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    fillAlpha = 0.75f,
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .springClickable(
                                testTag = "context_chip_selector",
                                onClick = { showSubjectDialog = true }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    tint = if (isDark) NeonCyan else DeepIndigo,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Current Context",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "${studyContext.targetExam.ifBlank { "Competitive Exam" }} • $selectedSubject",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) NeonCyan else DeepIndigo,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Change Context",
                                tint = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // 3. PRIMARY LIQUID GLASS SEARCH BAR
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    fillAlpha = 0.88f,
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0x22000000) else Color(0x0A000000))
                                .border(
                                    1.dp,
                                    if (isDark) Color(0x3338BDF8) else Color(0x224338CA),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = if (isDark) NeonCyan else DeepIndigo,
                                modifier = Modifier.size(22.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search your question...",
                                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                        fontSize = 14.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("smart_search_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                    unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A)
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (searchQuery.isNotBlank() && !isSearching) {
                                        focusManager.clearFocus()
                                        viewModel.performSmartSearch(
                                            query = searchQuery,
                                            subject = selectedSubject,
                                            language = searchLanguage
                                        )
                                    }
                                })
                            )

                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Voice Search Button
                            IconButton(
                                onClick = {
                                    onRequestMicPermission()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                                    .testTag("smart_search_voice_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "Voice Search",
                                    tint = if (isDark) NeonCyan else DeepIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Submit Button
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (searchQuery.isNotBlank())
                                            Brush.linearGradient(listOf(NeonCyan, ElectricIndigo))
                                        else
                                            Brush.linearGradient(listOf(Color(0x3338BDF8), Color(0x33818CF8)))
                                    )
                                    .springClickable(
                                        testTag = "smart_search_submit_btn",
                                        onClick = {
                                            if (searchQuery.isNotBlank() && !isSearching) {
                                                focusManager.clearFocus()
                                                viewModel.performSmartSearch(
                                                    query = searchQuery,
                                                    subject = selectedSubject,
                                                    language = searchLanguage
                                                )
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowForward,
                                        contentDescription = "Submit Search",
                                        tint = if (searchQuery.isNotBlank()) Color.Black else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // 4. LANGUAGE SELECTOR PILL
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Translate,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Language:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("English", "हिंदी", "Hinglish").forEach { lang ->
                                    val isSelected = searchLanguage.equals(lang, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) (if (isDark) NeonCyan.copy(alpha = 0.25f) else DeepIndigo.copy(alpha = 0.15f))
                                                else (if (isDark) Color(0x12FFFFFF) else Color(0x08000000))
                                            )
                                            .border(
                                                width = if (isSelected) 1.dp else 0.5.dp,
                                                color = if (isSelected) (if (isDark) NeonCyan else DeepIndigo) else Color(0x22FFFFFF),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .springClickable(
                                                testTag = "lang_pill_$lang",
                                                onClick = { viewModel.setSearchLanguage(lang) }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = lang,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) (if (isDark) NeonCyan else DeepIndigo) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. LOADING STATE
            if (isSearching) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.9f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val glowAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "glow"
                            )

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                NeonCyan.copy(alpha = glowAlpha),
                                                ElectricIndigo.copy(alpha = 0.2f),
                                                Color.Transparent
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(34.dp),
                                    color = NeonCyan,
                                    strokeWidth = 3.dp
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🔎 Searching verified academic sources...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "NOVA is synthesizing key takeaways for ${studyContext.targetExam}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // 6. ERROR STATE
            if (searchError != null && !isSearching && smartResult == null) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.85f
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Search is temporarily unavailable",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = searchError ?: "Unable to complete search request. Please check your connection and try again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.performSmartSearch(
                                            query = searchQuery,
                                            subject = selectedSubject,
                                            language = searchLanguage
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Try Again", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 7. PRE-SEARCH CONTEXT SUGGESTIONS & RECENT SEARCHES (When no active result)
            if (smartResult == null && !isSearching) {
                // High-Yield Context Suggestions
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isDark) NeonCyan else DeepIndigo,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Suggested for $selectedSubject",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                )
                            }
                        }

                        contextSuggestions.forEach { suggestion ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                fillAlpha = 0.6f
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .springClickable(
                                            testTag = "suggestion_$suggestion",
                                            onClick = {
                                                searchQuery = suggestion
                                                viewModel.performSmartSearch(
                                                    query = suggestion,
                                                    subject = selectedSubject,
                                                    language = searchLanguage
                                                )
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
                                            imageVector = Icons.Outlined.Lightbulb,
                                            contentDescription = null,
                                            tint = if (isDark) NeonCyan else DeepIndigo,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDark) Color.White else Color(0xFF0F172A),
                                            fontSize = 13.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Recent Searches
                if (searchHistory.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.clearSearchHistory() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "Clear History",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) NeonCyan else DeepIndigo
                                    )
                                }
                            }

                            searchHistory.take(5).forEach { item ->
                                SearchHistoryRow(
                                    item = item,
                                    isDark = isDark,
                                    onSelect = { query ->
                                        searchQuery = query
                                        viewModel.performSmartSearch(
                                            query = query,
                                            subject = item.subject.ifBlank { selectedSubject },
                                            language = searchLanguage
                                        )
                                    },
                                    onRemove = { query ->
                                        viewModel.removeSearchHistoryItem(query)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 8. SEARCH RESULT DISPLAY
            smartResult?.let { result ->
                // EXAM RELEVANCE & TOPIC INTENT BADGE
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.8f
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "🎯 Exam Relevance",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4ADE80),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = result.examRelevance.ifBlank { "High relevance for ${studyContext.targetExam} • $selectedSubject" },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = result.intentType.ifBlank { "Concept" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) NeonCyan else DeepIndigo,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // QUICK 1-TAP ACTIONS (Ask NOVA, Save Note, Flashcards, 25m Focus)
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
                                    color = if (isDark) NeonCyan else DeepIndigo
                                )
                                IconButton(
                                    onClick = { viewModel.clearSmartSearch() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close Result",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Ask NOVA Action
                                Button(
                                    onClick = {
                                        viewModel.askNovaAboutSearchResult(result)
                                    },
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("ask_nova_from_search_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Brush.linearGradient(listOf(NeonCyan, ElectricIndigo)).run {
                                            Color(0xFF0284C7)
                                        },
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ask NOVA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Save Note Action
                                Button(
                                    onClick = {
                                        viewModel.saveSearchResultAsSmartNote(result, subject = selectedSubject)
                                        Toast.makeText(context, "Saved to Smart Notes 📝", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_as_smart_note_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0x2838BDF8) else Color(0x184338CA),
                                        contentColor = if (isDark) NeonCyan else DeepIndigo
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Flashcards Action
                                Button(
                                    onClick = {
                                        viewModel.saveSearchResultAsFlashcards(result, subject = selectedSubject)
                                        Toast.makeText(context, "Extracted Flashcards for Revision 🎴", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("extract_flashcards_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0x28818CF8) else Color(0x186366F1),
                                        contentColor = if (isDark) ElectricIndigo else Color(0xFF6366F1)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.Style, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Flashcards", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // 25m Focus Session
                                Button(
                                    onClick = {
                                        onNavigateToFocus(selectedSubject, result.query, 25)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("schedule_focus_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0x284ADE80) else Color(0x1816A34A),
                                        contentColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("25m Focus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // STRUCTURED AI ANSWER (CONCEPT SYNTHESIS)
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fillAlpha = 0.88f
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = if (isDark) NeonCyan else DeepIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CONCEPT SYNTHESIS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) NeonCyan else DeepIndigo
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = result.studentFriendlyAnswer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                lineHeight = 23.sp
                            )
                        }
                    }
                }

                // HIGH-YIELD KEY TAKEAWAYS
                if (result.keyPoints.isNotEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            fillAlpha = 0.88f
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Key,
                                        contentDescription = null,
                                        tint = if (isDark) NeonCyan else DeepIndigo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "High-Yield Key Takeaways",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }

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
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDark) NeonCyan.copy(alpha = 0.2f)
                                                    else DeepIndigo.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${i + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) NeonCyan else DeepIndigo
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = point,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                            modifier = Modifier.weight(1f),
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // FORMULAS & CORE DEFINITIONS
                if (result.formulasAndDefinitions.isNotEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            fillAlpha = 0.88f
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Calculate,
                                        contentDescription = null,
                                        tint = if (isDark) NeonCyan else DeepIndigo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Formulas & Core Definitions",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) NeonCyan else DeepIndigo
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                result.formulasAndDefinitions.forEach { formula ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0x28000000) else Color(0x0C000000))
                                            .border(
                                                1.dp,
                                                if (isDark) Color(0x3338BDF8) else Color(0x224338CA),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = formula,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // VERIFIED WEB SOURCES & EDUCATIONAL CITATIONS
                if (result.sources.isNotEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            fillAlpha = 0.88f
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = if (isDark) NeonCyan else DeepIndigo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Verified Sources & Authority Citations",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                result.sources.forEach { source ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000))
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
                                                    color = if (isDark) Color.White else Color(0xFF0F172A),
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
                                                            color = if (isDark) NeonCyan else DeepIndigo
                                                        )
                                                    }
                                                }
                                            }
                                            if (source.snippet.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = source.snippet,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = source.domain,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isDark) NeonCyan.copy(alpha = 0.8f) else DeepIndigo.copy(alpha = 0.8f),
                                                fontSize = 10.sp
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(start = 6.dp)
                                        ) {
                                            Text(
                                                text = "Open",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) NeonCyan else DeepIndigo,
                                                fontSize = 11.sp
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.OpenInNew,
                                                contentDescription = "Open Source",
                                                tint = if (isDark) NeonCyan else DeepIndigo,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // PRACTICE QUIZ MCQs (Interactive Knowledge Check)
                if (result.generatedPracticeQuestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "🎯 Practice Quiz (${result.generatedPracticeQuestions.size} Questions)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }

                    items(result.generatedPracticeQuestions) { q ->
                        SmartPracticeQuestionCard(question = q, isDark = isDark)
                    }
                }

                // FOLLOW-UP INQUIRIES
                if (result.suggestedQuestions.isNotEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            fillAlpha = 0.88f
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = if (isDark) NeonCyan else DeepIndigo,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "💬 Follow-Up Queries for NOVA",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                result.suggestedQuestions.forEach { followUp ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                                            .springClickable(
                                                testTag = "followup_$followUp",
                                                onClick = {
                                                    searchQuery = followUp
                                                    viewModel.performSmartSearch(
                                                        query = followUp,
                                                        subject = selectedSubject,
                                                        language = searchLanguage
                                                    )
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
                                                color = if (isDark) NeonCyan else DeepIndigo,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = if (isDark) NeonCyan else DeepIndigo,
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

    // SUBJECT / CONTEXT SELECTOR DIALOG
    if (showSubjectDialog) {
        Dialog(onDismissRequest = { showSubjectDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                fillAlpha = 0.95f
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Study Subject",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        IconButton(
                            onClick = { showSubjectDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Text(
                        text = "Target Exam: ${studyContext.targetExam}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) NeonCyan else DeepIndigo,
                        fontWeight = FontWeight.SemiBold
                    )

                    val availableSubjects = remember(studyContext.subjects) {
                        if (studyContext.subjects.isNotEmpty()) studyContext.subjects
                        else listOf("General Science", "Physics", "Chemistry", "Mathematics", "Biology", "General Awareness", "Reasoning")
                    }

                    availableSubjects.forEach { sub ->
                        val isSelected = sub == selectedSubject
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) (if (isDark) NeonCyan.copy(alpha = 0.25f) else DeepIndigo.copy(alpha = 0.15f))
                                    else (if (isDark) Color(0x15FFFFFF) else Color(0x0C000000))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) (if (isDark) NeonCyan else DeepIndigo) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .springClickable(
                                    testTag = "dialog_subject_$sub",
                                    onClick = {
                                        viewModel.setSearchSubject(sub)
                                        showSubjectDialog = false
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) (if (isDark) NeonCyan else DeepIndigo) else (if (isDark) Color.White else Color(0xFF0F172A))
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = if (isDark) NeonCyan else DeepIndigo,
                                        modifier = Modifier.size(18.dp)
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

@Composable
private fun SearchHistoryRow(
    item: NovaSearchHistoryItem,
    isDark: Boolean,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val dateStr = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.5f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .springClickable(
                    testTag = "recent_search_${item.query}",
                    onClick = { onSelect(item.query) }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = item.query,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${item.subject} • $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }

            IconButton(
                onClick = { onRemove(item.query) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SmartPracticeQuestionCard(
    question: Question,
    isDark: Boolean
) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.85f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(12.dp))

            question.options.forEachIndexed { idx, opt ->
                val isSelected = selectedOption == idx
                val isCorrect = question.correctOptionIndex == idx

                val bgColor = when {
                    isSubmitted && isCorrect -> Color(0xFF166534)
                    isSubmitted && isSelected && !isCorrect -> Color(0xFF991B1B)
                    isSelected -> (if (isDark) NeonCyan.copy(alpha = 0.25f) else DeepIndigo.copy(alpha = 0.15f))
                    else -> (if (isDark) Color(0x15FFFFFF) else Color(0x0C000000))
                }

                val borderColor = when {
                    isSubmitted && isCorrect -> Color(0xFF4ADE80)
                    isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
                    isSelected -> (if (isDark) NeonCyan else DeepIndigo)
                    else -> (if (isDark) Color(0x22FFFFFF) else Color(0x12000000))
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
                            color = if (isSubmitted && (isCorrect || isSelected)) Color.White else (if (isDark) Color.White else Color(0xFF0F172A)),
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
                        .background(if (isDark) Color(0x28000000) else Color(0x0C000000))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "💡 Explanation:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) NeonCyan else DeepIndigo
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

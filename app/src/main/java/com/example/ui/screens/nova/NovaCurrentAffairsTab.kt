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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CurrentAffairsItem
import com.example.data.model.CurrentAffairsQuizSession
import com.example.data.model.ExamUpdateItem
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NovaCurrentAffairsTab(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentAffairs by viewModel.allCurrentAffairs.collectAsState()
    val examUpdates by viewModel.allExamUpdates.collectAsState()
    val studyContext by viewModel.studyContext.collectAsState()

    val isRefreshing by viewModel.isRefreshingCurrentAffairs.collectAsState()
    val selectedCategory by viewModel.currentAffairsFilterCategory.collectAsState()
    val searchQuery by viewModel.currentAffairsSearchQuery.collectAsState()
    val currentLang by viewModel.currentAffairsLanguage.collectAsState()
    val selectedDate by viewModel.selectedCurrentAffairsDate.collectAsState()
    val examMode by viewModel.currentAffairsExamMode.collectAsState()
    val isFreshWebSearching by viewModel.isFreshWebSearching.collectAsState()
    val quizState by viewModel.currentAffairsQuizState.collectAsState()

    val selectedAffairForNova by viewModel.selectedAffairForNova.collectAsState()
    val novaAnalysis by viewModel.novaAffairAnalysis.collectAsState()
    val isAnalyzingAffair by viewModel.isAnalyzingAffair.collectAsState()
    val lastRefreshedTime by viewModel.lastRefreshedTime.collectAsState()

    var selectedSection by remember { mutableStateOf(0) } // 0: Current Affairs, 1: Official Exam Notices
    var showDetailDialog by remember { mutableStateOf<CurrentAffairsItem?>(null) }
    var showAskNovaDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showPdfExportDialog by remember { mutableStateOf(false) }
    var showTopOnly by remember { mutableStateOf(true) }

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date())
    }

    val lastUpdatedFormatted = remember(lastRefreshedTime) {
        val diffMins = (System.currentTimeMillis() - lastRefreshedTime) / (60 * 1000)
        if (diffMins < 1) "Updated just now"
        else if (diffMins < 60) "Updated $diffMins min ago"
        else "Last updated: " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastRefreshedTime))
    }

    // Standardized Categories List
    val categories = remember {
        listOf(
            "All" to "🌐 All",
            "🔖 Saved" to "🔖 Saved",
            "⭐ Important" to "⭐ Important",
            "National" to "🇮🇳 National",
            "International" to "🌍 International",
            "Government & Policy" to "🏛️ Government & Policy",
            "Economy" to "💰 Economy",
            "Banking" to "🏦 Banking",
            "Science & Tech" to "🚀 Science & Tech",
            "Defence" to "🛡️ Defence",
            "Sports" to "🏆 Sports",
            "Environment" to "🌿 Environment",
            "Awards" to "🏅 Awards",
            "Appointments" to "👔 Appointments",
            "Important Days" to "📅 Important Days",
            "Schemes" to "📋 Schemes",
            "Business" to "💼 Business",
            "Education" to "🎓 Education",
            "Miscellaneous" to "📦 Misc"
        )
    }

    // Quick Date Options
    val dateOptions = remember {
        listOf("Today", "Yesterday", "Aug 22", "Aug 21", "Aug 20", "Last 7 Days", "Last 30 Days")
    }

    // Filter Current Affairs
    val filteredAffairs = remember(
        currentAffairs, selectedCategory, searchQuery, selectedDate, examMode, studyContext.targetExam
    ) {
        currentAffairs.filter { item ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "🔖 Saved", "Saved" -> item.isSavedForRevision
                "⭐ Important" -> item.isImportant
                else -> item.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                        item.summary.contains(searchQuery, ignoreCase = true) ||
                        item.examRelevance.contains(searchQuery, ignoreCase = true) ||
                        item.sourceName.contains(searchQuery, ignoreCase = true) ||
                        item.keyPoints.any { it.contains(searchQuery, ignoreCase = true) }
            }
            val matchesDate = when (selectedDate) {
                "Today" -> item.publishedDate.contains("Today", ignoreCase = true) || item.publishedDate.contains("2026", ignoreCase = true) || item.publishedDate.contains("Aug 24", ignoreCase = true)
                "Yesterday" -> item.publishedDate.contains("Yesterday", ignoreCase = true) || item.publishedDate.contains("Aug 23", ignoreCase = true)
                "Last 7 Days" -> true
                "Last 30 Days" -> true
                else -> item.publishedDate.contains(selectedDate, ignoreCase = true) || true
            }
            val matchesExam = if (examMode == "For You" && studyContext.targetExam.isNotBlank()) {
                item.targetExams.any { it.contains(studyContext.targetExam, ignoreCase = true) } ||
                        item.examRelevance.contains(studyContext.targetExam, ignoreCase = true) ||
                        item.category in listOf("National", "Government & Policy", "Economy", "Science & Tech", "Defence")
            } else true

            matchesCategory && matchesSearch && matchesDate && matchesExam
        }
    }

    val topUpdates = remember(filteredAffairs) {
        filteredAffairs.take(6)
    }

    val displayedAffairs = if (selectedDate == "Today" && showTopOnly && searchQuery.isBlank() && selectedCategory == "All") topUpdates else filteredAffairs

    val savedCount = remember(currentAffairs) {
        currentAffairs.count { it.isSavedForRevision }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotate")
    val refreshRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- 1. HEADER: Today's Current Affairs & Language Toggle ---
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    fillAlpha = 0.88f
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(NeonCyan.copy(alpha = 0.35f), ElectricIndigo.copy(alpha = 0.45f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📰", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Today's Current Affairs",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "$todayFormatted • $lastUpdatedFormatted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Today reset button
                                if (selectedDate != "Today") {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NeonCyan.copy(alpha = 0.2f))
                                            .springClickable(
                                                testTag = "btn_today_reset",
                                                onClick = { viewModel.setSelectedCurrentAffairsDate("Today") }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                    }
                                }

                                // Refresh Button
                                IconButton(
                                    onClick = { viewModel.refreshCurrentAffairs(forceRefresh = true) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x22FFFFFF))
                                        .testTag("btn_refresh_current_affairs"),
                                    enabled = !isRefreshing
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Refresh Feed",
                                        tint = if (isRefreshing) NeonCyan else Color.White,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .then(if (isRefreshing) Modifier.rotate(refreshRotation) else Modifier)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Language Toggle Bar (हिंदी | English)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x1A000000))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp)) {
                                Text("भाषा", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Language:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("English", "Hindi", "Hinglish").forEach { lang ->
                                    val isSelected = currentLang.equals(lang, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                                            .border(
                                                width = if (isSelected) 1.dp else 0.dp,
                                                color = if (isSelected) NeonCyan else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .springClickable(
                                                testTag = "lang_select_$lang",
                                                onClick = {
                                                    viewModel.setCurrentAffairsLanguage(lang)
                                                    viewModel.refreshCurrentAffairs(forceRefresh = true)
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (lang) {
                                                "Hindi" -> "🇮🇳 हिंदी"
                                                "Hinglish" -> "🌐 Hinglish"
                                                else -> "🇬🇧 English"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) NeonCyan else Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Search Input Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setCurrentAffairsSearchQuery(it) },
                            placeholder = { Text("Search Current Affairs (e.g. ISRO, Budget, Railway)...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setCurrentAffairsSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedContainerColor = Color(0x1A000000),
                                unfocusedContainerColor = Color(0x1A000000),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("search_current_affairs")
                        )

                        // Web search prompt if search is entered
                        if (searchQuery.isNotBlank() && filteredAffairs.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.searchFreshWebCurrentAffairs(searchQuery) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isFreshWebSearching
                            ) {
                                if (isFreshWebSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Searching Latest Web Updates...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Search Fresh Web Updates for \"$searchQuery\"", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Section Switcher Tabs (Current Affairs vs Official Exam Notices)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x22000000))
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedSection == 0) NeonCyan else Color.Transparent)
                                    .springClickable(testTag = "tab_current_affairs", onClick = { selectedSection = 0 })
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📰 Current Affairs (${filteredAffairs.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedSection == 0) Color.Black else Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedSection == 1) NeonCyan else Color.Transparent)
                                    .springClickable(testTag = "tab_exam_notices", onClick = { selectedSection = 1 })
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📢 Official Notices (${examUpdates.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedSection == 1) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. DATE NAVIGATION & QUICK ACTION BAR ---
            if (selectedSection == 0) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Date Navigation Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📅 Date Navigation:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            TextButton(
                                onClick = { showDatePickerDialog = true },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Select Date", fontSize = 11.sp, color = NeonCyan)
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dateOptions) { dateOpt ->
                                val isSelected = selectedDate.equals(dateOpt, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) NeonCyan else Color(0x22FFFFFF))
                                        .border(1.dp, if (isSelected) NeonCyan else Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                                        .springClickable(
                                            testTag = "date_opt_$dateOpt",
                                            onClick = { viewModel.setSelectedCurrentAffairsDate(dateOpt) }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dateOpt,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        // Exam Relevance & Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "For You" vs "All" Exam Toggle
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, if (examMode == "For You") NeonCyan.copy(alpha = 0.5f) else Color(0x22FFFFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .springClickable(
                                        testTag = "btn_exam_toggle",
                                        onClick = {
                                            viewModel.setCurrentAffairsExamMode(if (examMode == "For You") "All" else "For You")
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("🎯", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (examMode == "For You") "For You (${studyContext.targetExam.take(10)})" else "All Exams",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (examMode == "For You") NeonCyan else Color.White
                                    )
                                }
                            }

                            // Today's Quiz Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .springClickable(
                                        testTag = "btn_today_quiz",
                                        onClick = { viewModel.startDailyCurrentAffairsQuiz(5) }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = AmberGold, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Today's Quiz", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // Download PDF Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, CoralPink.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .springClickable(
                                        testTag = "btn_pdf_dialog",
                                        onClick = { showPdfExportDialog = true }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CoralPink, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PDF Export", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // --- 3. CATEGORY FILTERS RAIL ---
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { (key, label) ->
                            val isSelected = selectedCategory.equals(key, ignoreCase = true) || (key == "🔖 Saved" && selectedCategory == "Saved") || (key == "⭐ Important" && selectedCategory == "Important")
                            val countLabel = if (key == "🔖 Saved" || key == "Saved") " ($savedCount)" else ""
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) Brush.horizontalGradient(
                                            listOf(NeonCyan.copy(alpha = 0.85f), ElectricIndigo.copy(alpha = 0.85f))
                                        ) else Brush.horizontalGradient(
                                            listOf(Color(0x22FFFFFF), Color(0x11FFFFFF))
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) NeonCyan else Color(0x22FFFFFF),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .springClickable(
                                        testTag = "chip_category_$key",
                                        onClick = {
                                            val filterKey = when (key) {
                                                "🔖 Saved" -> "Saved"
                                                "⭐ Important" -> "Important"
                                                else -> key
                                            }
                                            viewModel.setCurrentAffairsFilter(filterKey)
                                        }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$label$countLabel",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                // --- 4. TOP UPDATES HEADER / VIEW ALL TOGGLE ---
                if (selectedDate == "Today" && searchQuery.isBlank() && selectedCategory == "All") {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔥", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (showTopOnly) "Top 6 High-Yield Updates" else "All Today's Current Affairs (${filteredAffairs.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            if (filteredAffairs.size > 6) {
                                TextButton(
                                    onClick = { showTopOnly = !showTopOnly },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = if (showTopOnly) "View All (${filteredAffairs.size})" else "Show Top 6",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 5. CURRENT AFFAIRS LIST CARDS / SKELETON ---
                if (isRefreshing && currentAffairs.isEmpty()) {
                    items(4) {
                        CurrentAffairsSkeletonCard()
                    }
                } else if (displayedAffairs.isEmpty()) {
                    item {
                        EmptyCurrentAffairsView(
                            category = selectedCategory,
                            searchQuery = searchQuery,
                            onReset = {
                                viewModel.setCurrentAffairsFilter("All")
                                viewModel.setCurrentAffairsSearchQuery("")
                                viewModel.setSelectedCurrentAffairsDate("Today")
                                viewModel.refreshCurrentAffairs(forceRefresh = true)
                            }
                        )
                    }
                } else {
                    items(displayedAffairs, key = { it.id }) { item ->
                        CompactCurrentAffairsCard(
                            item = item,
                            onOpenDetail = { showDetailDialog = item },
                            onToggleSaved = { viewModel.toggleCurrentAffairsSaved(item.id, !item.isSavedForRevision) },
                            onStartQuiz = { viewModel.startArticleQuiz(item) },
                            onAskNova = {
                                viewModel.askNovaAboutAffair(item, "exam angle")
                                showAskNovaDialog = true
                            },
                            onOpenSource = {
                                try {
                                    val targetUrl = if (item.canonicalUrl.isNotBlank()) item.canonicalUrl else item.sourceUrl
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open source link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // --- OFFICIAL EXAM NOTICES TAB (IF SELECTED SECTION = 1) ---
            if (selectedSection == 1) {
                if (examUpdates.isEmpty()) {
                    item {
                        EmptyNoticesView(onRefresh = { viewModel.refreshCurrentAffairs(forceRefresh = true) })
                    }
                } else {
                    items(examUpdates) { update ->
                        ExamNoticeCard(update = update)
                    }
                }
            }
        }

        // --- DIALOGS & OVERLAYS ---

        // 1. Full Article Detail Dialog
        showDetailDialog?.let { article ->
            ArticleDetailDialog(
                article = article,
                onDismiss = { showDetailDialog = null },
                onToggleSaved = {
                    viewModel.toggleCurrentAffairsSaved(article.id, !article.isSavedForRevision)
                    showDetailDialog = article.copy(isSavedForRevision = !article.isSavedForRevision)
                },
                onStartQuiz = {
                    showDetailDialog = null
                    viewModel.startArticleQuiz(article)
                },
                onAskNova = {
                    showDetailDialog = null
                    viewModel.askNovaAboutAffair(article, "exam angle")
                    showAskNovaDialog = true
                },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, article.title)
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "📌 ${article.title}\n\nSummary: ${article.summary}\n\nExam Relevance: ${article.examRelevance}\nSource: ${article.sourceName}"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Current Affairs"))
                }
            )
        }

        // 2. Ask NOVA Dialogue
        if (showAskNovaDialog && selectedAffairForNova != null) {
            AskNovaAffairDialog(
                item = selectedAffairForNova!!,
                analysis = novaAnalysis,
                isAnalyzing = isAnalyzingAffair,
                onDismiss = {
                    showAskNovaDialog = false
                    viewModel.clearNovaAffairAnalysis()
                },
                onSaveAsNote = {
                    viewModel.saveAffairAsSmartNote(selectedAffairForNova!!)
                }
            )
        }

        // 3. Quiz Overlay
        quizState?.let { quiz ->
            CurrentAffairsQuizDialog(
                quiz = quiz,
                onSelectAnswer = { qIdx, optIdx -> viewModel.selectCurrentAffairsQuizAnswer(qIdx, optIdx) },
                onSubmit = { viewModel.submitCurrentAffairsQuiz() },
                onDismiss = { viewModel.closeCurrentAffairsQuiz() }
            )
        }

        // 4. Date Picker Dialog
        if (showDatePickerDialog) {
            DatePickerModal(
                onDateSelected = { dateStr ->
                    viewModel.setSelectedCurrentAffairsDate(dateStr)
                    showDatePickerDialog = false
                },
                onDismiss = { showDatePickerDialog = false }
            )
        }

        // 5. PDF Export Modal
        if (showPdfExportDialog) {
            PdfExportModal(
                onExport = { dateRange ->
                    showPdfExportDialog = false
                    viewModel.exportCurrentAffairsPdf(context, dateRange)
                },
                onDismiss = { showPdfExportDialog = false }
            )
        }
    }
}

// =========================================================================
// COMPACT ARTICLE CARD COMPONENT
// =========================================================================

@Composable
private fun CompactCurrentAffairsCard(
    item: CurrentAffairsItem,
    onOpenDetail: () -> Unit,
    onToggleSaved: () -> Unit,
    onStartQuiz: () -> Unit,
    onAskNova: () -> Unit,
    onOpenSource: () -> Unit
) {
    val categoryIcon = when (item.category.lowercase()) {
        "science & tech", "science" -> "🚀"
        "economy", "banking" -> "💰"
        "polity", "government & policy" -> "🏛️"
        "international" -> "🌍"
        "defence" -> "🛡️"
        "sports" -> "🏆"
        "environment" -> "🌿"
        "awards" -> "🏅"
        "schemes" -> "📋"
        else -> "🇮🇳"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .springClickable(testTag = "card_ca_${item.id}", onClick = onOpenDetail),
        fillAlpha = 0.85f
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Category & Source Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$categoryIcon ${item.category}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    if (item.isImportant) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmberGold.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("⭐ Important", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.publishedDate} • ${item.sourceName.take(16)}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onToggleSaved,
                        modifier = Modifier.size(28.dp).testTag("save_btn_${item.id}")
                    ) {
                        Icon(
                            imageVector = if (item.isSavedForRevision) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (item.isSavedForRevision) AmberGold else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headline
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Short Summary
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Exam Relevance Chip
            if (item.examRelevance.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x15FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "🎯 Exam Relevance: ${item.examRelevance}",
                        fontSize = 10.5.sp,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Quick Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Read More Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan)
                            .springClickable(testTag = "read_btn_${item.id}", onClick = onOpenDetail)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("📖 Read", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    // Quiz Me Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberGold.copy(alpha = 0.2f))
                            .border(1.dp, AmberGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .springClickable(testTag = "quiz_btn_${item.id}", onClick = onStartQuiz)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("🧠 Quiz", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                    }

                    // Ask NOVA Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricIndigo.copy(alpha = 0.25f))
                            .border(1.dp, ElectricIndigo.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .springClickable(testTag = "nova_btn_${item.id}", onClick = onAskNova)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("🤖 Ask NOVA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }
                }

                // Source Link
                IconButton(onClick = onOpenSource, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Source", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// =========================================================================
// ARTICLE DETAIL DIALOG
// =========================================================================

@Composable
private fun ArticleDetailDialog(
    article: CurrentAffairsItem,
    onDismiss: () -> Unit,
    onToggleSaved: () -> Unit,
    onStartQuiz: () -> Unit,
    onAskNova: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(article.category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }

                    Row {
                        IconButton(onClick = onToggleSaved) {
                            Icon(
                                imageVector = if (article.isSavedForRevision) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (article.isSavedForRevision) AmberGold else Color.White
                            )
                        }
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Headline
                    item {
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Published: ${article.publishedDate} • Source: ${article.sourceName}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Executive Summary
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), fillAlpha = 0.7f) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("📄 Short Summary", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = NeonCyan)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }

                    // Key Points
                    if (article.keyPoints.isNotEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth(), fillAlpha = 0.7f) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("📌 Key Facts", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AmberGold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    article.keyPoints.forEach { point ->
                                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                            Text("• ", color = AmberGold, fontWeight = FontWeight.Bold)
                                            Text(point, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Why It Matters for Exams
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), fillAlpha = 0.7f) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🎯 Why It Matters for Exams", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (article.whyItMatters.isNotBlank()) article.whyItMatters else article.examRelevance,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val targetUrl = if (article.canonicalUrl.isNotBlank()) article.canonicalUrl else article.sourceUrl
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Original Source", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onStartQuiz,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create Quiz", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onAskNova,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ask NOVA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// QUIZ DIALOG
// =========================================================================

@Composable
private fun CurrentAffairsQuizDialog(
    quiz: CurrentAffairsQuizSession,
    onSelectAnswer: (Int, Int) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quiz.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (quiz.isSubmitted) {
                    // QUIZ SCORE SUMMARY
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(if (quiz.score >= 60) EmeraldGreen.copy(alpha = 0.2f) else CoralPink.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${quiz.score}%", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = if (quiz.score >= 60) EmeraldGreen else CoralPink)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Quiz Completed!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("✅ Correct", fontSize = 11.sp, color = Color.Gray)
                                Text("${quiz.correctCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("❌ Incorrect", fontSize = 11.sp, color = Color.Gray)
                                Text("${quiz.incorrectCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CoralPink)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⏸️ Skipped", fontSize = 11.sp, color = Color.Gray)
                                Text("${quiz.unansweredCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // QUESTION LIST FLOW
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(quiz.questions.size) { idx ->
                            val q = quiz.questions[idx]
                            val selectedOpt = quiz.selectedAnswers[idx]

                            GlassCard(modifier = Modifier.fillMaxWidth(), fillAlpha = 0.7f) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Q${idx + 1}. ${q.questionText}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    q.options.forEachIndexed { optIdx, optionText ->
                                        val isSelected = selectedOpt == optIdx
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0x1AFFFFFF))
                                                .border(1.dp, if (isSelected) NeonCyan else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                                .springClickable(testTag = "quiz_opt_${idx}_$optIdx", onClick = { onSelectAnswer(idx, optIdx) })
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "${('A' + optIdx)}. $optionText",
                                                fontSize = 12.sp,
                                                color = if (isSelected) NeonCyan else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Quiz (${quiz.selectedAnswers.size}/${quiz.questions.size} Answered)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// HELPER DIALOGS & CARDS
// =========================================================================

@Composable
private fun AskNovaAffairDialog(
    item: CurrentAffairsItem,
    analysis: String?,
    isAnalyzing: Boolean,
    onDismiss: () -> Unit,
    onSaveAsNote: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🤖 NOVA Exam Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)

                Spacer(modifier = Modifier.height(12.dp))

                if (isAnalyzing) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                } else if (!analysis.isNullOrBlank()) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), fillAlpha = 0.7f) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(analysis, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontSize = 12.5.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            onSaveAsNote()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo, contentColor = Color.White)
                    ) {
                        Text("Save as Smart Note", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DatePickerModal(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sampleDates = listOf("Aug 24, 2026", "Aug 23, 2026", "Aug 22, 2026", "Aug 21, 2026", "Aug 20, 2026", "Aug 19, 2026", "Aug 18, 2026")
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF0F172A)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📅 Select Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                sampleDates.forEach { dt ->
                    TextButton(
                        onClick = { onDateSelected(dt) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(dt, color = Color.White, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfExportModal(
    onExport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF0F172A)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📄 Export Current Affairs PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                listOf("Today's Updates", "Last 7 Days", "Last 30 Days", "Saved Articles").forEach { range ->
                    Button(
                        onClick = { onExport(range) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF), contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(range, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamNoticeCard(update: ExamUpdateItem) {
    val context = LocalContext.current
    GlassCard(modifier = Modifier.fillMaxWidth(), fillAlpha = 0.85f) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("📢 ${update.noticeType}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                Text(update.publishDate, fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(update.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(update.summary, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (update.officialLink.isNotBlank()) {
                TextButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.officialLink)))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                        }
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Open Official Notice 🔗", fontSize = 11.sp, color = NeonCyan)
                }
            }
        }
    }
}

@Composable
private fun CurrentAffairsSkeletonCard() {
    GlassCard(modifier = Modifier.fillMaxWidth().height(110.dp), fillAlpha = 0.5f) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0x11FFFFFF)))
    }
}

@Composable
private fun EmptyCurrentAffairsView(
    category: String,
    searchQuery: String,
    onReset: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        fillAlpha = 0.8f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔍", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "No updates match \"$searchQuery\"" else "No $category Updates Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try adjusting filters or fetch the latest web updates.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
            ) {
                Text("Reset Filters & Refresh Radar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyNoticesView(onRefresh: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        fillAlpha = 0.8f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📢", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text("No Official Exam Notices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Official testing agency notices will appear here.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo, contentColor = Color.White)
            ) {
                Text("Check for Updates", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CurrentAffairsItem
import com.example.data.model.ExamUpdateItem
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaViewModel

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
    val selectedAffairForNova by viewModel.selectedAffairForNova.collectAsState()
    val novaAnalysis by viewModel.novaAffairAnalysis.collectAsState()
    val isAnalyzingAffair by viewModel.isAnalyzingAffair.collectAsState()

    var selectedSection by remember { mutableStateOf(0) } // 0: 30-Day Current Affairs, 1: Official Exam Notices
    var showAskNovaDialog by remember { mutableStateOf(false) }

    // Categories list
    val categories = listOf(
        "All" to "🌐 All",
        "Saved" to "🔖 Saved",
        "National" to "🇮🇳 National",
        "Science & Tech" to "🚀 Science & Tech",
        "Economy" to "💰 Economy",
        "Environment" to "🌿 Environment",
        "Polity" to "⚖️ Polity",
        "International" to "🌍 International",
        "Defense" to "🛡️ Defense"
    )

    // Filter Current Affairs
    val filteredAffairs = remember(currentAffairs, selectedCategory, searchQuery) {
        currentAffairs.filter { item ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Saved" -> item.isSavedForRevision
                else -> item.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                        item.summary.contains(searchQuery, ignoreCase = true) ||
                        item.examRelevance.contains(searchQuery, ignoreCase = true) ||
                        item.sourceName.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    // Filter Exam Updates
    val filteredUpdates = remember(examUpdates, searchQuery) {
        if (searchQuery.isBlank()) examUpdates else {
            examUpdates.filter { update ->
                update.title.contains(searchQuery, ignoreCase = true) ||
                        update.summary.contains(searchQuery, ignoreCase = true) ||
                        update.examName.contains(searchQuery, ignoreCase = true) ||
                        update.noticeType.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Date grouping for 30-day timeline
    val groupedAffairs = remember(filteredAffairs) {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 3600 * 1000L
        val groups = linkedMapOf<String, MutableList<CurrentAffairsItem>>()

        filteredAffairs.forEach { item ->
            val age = now - item.createdAt
            val groupKey = when {
                age <= 1.5 * dayMillis || item.publishedDate.contains("Today", ignoreCase = true) || item.publishedDate.contains("Yesterday", ignoreCase = true) -> "🔥 Today & Yesterday"
                age <= 7 * dayMillis || item.publishedDate.contains("days ago", ignoreCase = true) -> "📅 This Week"
                age <= 14 * dayMillis -> "📆 Last 2 Weeks"
                else -> "📜 Earlier This Month (30-Day Archive)"
            }
            groups.getOrPut(groupKey) { mutableListOf() }.add(item)
        }
        groups
    }

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. Compact Liquid Glass Header & Exam Radar Bar ---
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(NeonCyan.copy(alpha = 0.3f), ElectricIndigo.copy(alpha = 0.4f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📰", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Current Affairs Radar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "30-Day Live Feed • ${studyContext.targetExam}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Refresh Button with smooth rotation animation
                        IconButton(
                            onClick = { viewModel.refreshCurrentAffairs(forceRefresh = true) },
                            modifier = Modifier
                                .size(38.dp)
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
                                    .size(20.dp)
                                    .then(if (isRefreshing) Modifier.rotate(refreshRotation) else Modifier)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Selector Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1A000000))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Presentation:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
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
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
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

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setCurrentAffairsSearchQuery(it) },
                        placeholder = { Text("Search topics, policies, PIB or exams...", fontSize = 12.sp, color = Color(0xFF64748B)) },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Section Switcher Tabs
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
                                text = "📰 30-Day Feed (${filteredAffairs.size})",
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
                                text = "📢 Official Notices (${filteredUpdates.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSection == 1) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Category Filter Rail (When in Current Affairs tab) ---
        if (selectedSection == 0) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { (key, label) ->
                        val isSelected = selectedCategory.equals(key, ignoreCase = true)
                        val countLabel = if (key == "Saved") " ($savedCount)" else ""
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
                                    onClick = { viewModel.setCurrentAffairsFilter(key) }
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

            // --- 3. Featured Update Hero Card (if available and no specific search) ---
            if (searchQuery.isBlank() && filteredAffairs.isNotEmpty() && selectedCategory == "All") {
                val heroItem = filteredAffairs.first()
                item {
                    FeaturedHeroCard(
                        item = heroItem,
                        onAskNova = {
                            viewModel.askNovaAboutAffair(heroItem, "exam angle")
                            showAskNovaDialog = true
                        },
                        onSaveToNotes = {
                            viewModel.saveAffairAsSmartNote(heroItem)
                        },
                        onToggleSaved = {
                            viewModel.toggleCurrentAffairsSaved(heroItem.id, !heroItem.isSavedForRevision)
                        },
                        onOpenSource = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(heroItem.sourceUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // --- 4. 30-Day Grouped Feed ---
            if (filteredAffairs.isEmpty()) {
                item {
                    EmptyCurrentAffairsView(
                        category = selectedCategory,
                        searchQuery = searchQuery,
                        onReset = {
                            viewModel.setCurrentAffairsFilter("All")
                            viewModel.setCurrentAffairsSearchQuery("")
                            viewModel.refreshCurrentAffairs(forceRefresh = true)
                        }
                    )
                }
            } else {
                groupedAffairs.forEach { (groupTitle, itemsInGroup) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = groupTitle.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(Color(0x3338BDF8))
                            )
                        }
                    }

                    items(itemsInGroup, key = { it.id }) { item ->
                        CurrentAffairsItemCard(
                            item = item,
                            onToggleSaved = {
                                viewModel.toggleCurrentAffairsSaved(item.id, !item.isSavedForRevision)
                            },
                            onAskNova = {
                                viewModel.askNovaAboutAffair(item, "exam angle")
                                showAskNovaDialog = true
                            },
                            onSaveToNotes = {
                                viewModel.saveAffairAsSmartNote(item)
                            },
                            onOpenSource = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // --- 5. Official Exam Notices Feed ---
            if (filteredUpdates.isEmpty()) {
                item {
                    EmptyNoticesView(
                        onRefresh = { viewModel.refreshCurrentAffairs(forceRefresh = true) }
                    )
                }
            } else {
                items(filteredUpdates, key = { it.id }) { update ->
                    ExamUpdateItemCard(
                        update = update,
                        onOpenOfficialLink = {
                            viewModel.markExamUpdateRead(update.id)
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.officialLink))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // --- Ask NOVA AI Intelligence Modal / Dialog ---
    if (showAskNovaDialog && selectedAffairForNova != null) {
        val affair = selectedAffairForNova!!
        AskNovaAffairDialog(
            affair = affair,
            analysis = novaAnalysis,
            isAnalyzing = isAnalyzingAffair,
            onDismiss = {
                showAskNovaDialog = false
                viewModel.clearNovaAffairAnalysis()
            },
            onRequestAction = { actionType ->
                viewModel.askNovaAboutAffair(affair, actionType)
            },
            onSaveToSmartNotes = {
                viewModel.saveAffairAsSmartNote(affair)
                showAskNovaDialog = false
            }
        )
    }
}

// =========================================================================
// FEATURED HERO CARD
// =========================================================================

@Composable
private fun FeaturedHeroCard(
    item: CurrentAffairsItem,
    onAskNova: () -> Unit,
    onSaveToNotes: () -> Unit,
    onToggleSaved: () -> Unit,
    onOpenSource: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.92f,
        borderColor = AmberAlert
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AmberAlert.copy(alpha = 0.3f), AmberAlert.copy(alpha = 0.15f))
                            )
                        )
                        .border(1.dp, AmberAlert.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "FEATURED EXAM UPDATE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberAlert,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleSaved, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (item.isSavedForRevision) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save for Revision",
                            tint = if (item.isSavedForRevision) NeonCyan else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (item.sourceUrl.isNotBlank()) {
                        IconButton(onClick = onOpenSource, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Filled.OpenInNew,
                                contentDescription = "View Source",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE2E8F0),
                lineHeight = 18.sp
            )

            if (item.examRelevance.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x331E293B))
                        .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("🎯", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.examRelevance,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAskNova,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_hero_ask_nova")
                ) {
                    Text("🤖 Ask NOVA AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSaveToNotes,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_hero_save_notes")
                ) {
                    Text("📝 Add to Notes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// =========================================================================
// CURRENT AFFAIRS ITEM CARD
// =========================================================================

@Composable
private fun CurrentAffairsItemCard(
    item: CurrentAffairsItem,
    onToggleSaved: () -> Unit,
    onAskNova: () -> Unit,
    onSaveToNotes: () -> Unit,
    onOpenSource: () -> Unit
) {
    val categoryColor = when (item.category.lowercase()) {
        "science & tech" -> NeonCyan
        "economy" -> AmberAlert
        "environment" -> EmeraldGreen
        "polity" -> ElectricIndigo
        "defense" -> Color(0xFFF43F5E)
        else -> Color(0xFF38BDF8)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.85f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Category & Source Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.2f))
                            .border(1.dp, categoryColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.publishedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleSaved, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (item.isSavedForRevision) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (item.isSavedForRevision) NeonCyan else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (item.sourceUrl.isNotBlank()) {
                        IconButton(onClick = onOpenSource, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Filled.OpenInNew,
                                contentDescription = "Source",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Summary
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                lineHeight = 18.sp
            )

            // Exam Relevance Box
            if (item.examRelevance.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x22000000))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("🎯", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.examRelevance,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer action chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source badge
                Text(
                    text = "Source: ${item.sourceName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Save to Smart Notes Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x22FFFFFF))
                            .springClickable(testTag = "btn_card_notes_${item.id}", onClick = onSaveToNotes)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.NoteAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Note", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Ask NOVA Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .springClickable(testTag = "btn_card_ask_nova_${item.id}", onClick = onAskNova)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ask NOVA", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// OFFICIAL EXAM NOTICES ITEM CARD
// =========================================================================

@Composable
private fun ExamUpdateItemCard(
    update: ExamUpdateItem,
    onOpenOfficialLink: () -> Unit
) {
    val noticeColor = when (update.noticeType.lowercase()) {
        "admit card" -> AmberAlert
        "exam date" -> Color(0xFFF43F5E)
        "syllabus update" -> EmeraldGreen
        else -> ElectricIndigo
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.85f
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
                            .clip(RoundedCornerShape(6.dp))
                            .background(noticeColor.copy(alpha = 0.2f))
                            .border(1.dp, noticeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = update.noticeType.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = noticeColor,
                            fontSize = 10.sp
                        )
                    }

                    if (update.isVerifiedOfficial) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified Official",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = update.publishDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "[${update.examName}] ${update.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = update.summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )

            if (update.officialLink.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenOfficialLink,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x3338BDF8),
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Official Portal Notice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// ASK NOVA INTELLIGENCE DIALOG
// =========================================================================

@Composable
private fun AskNovaAffairDialog(
    affair: CurrentAffairsItem,
    analysis: String?,
    isAnalyzing: Boolean,
    onDismiss: () -> Unit,
    onRequestAction: (String) -> Unit,
    onSaveToSmartNotes: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                fillAlpha = 0.95f,
                borderColor = NeonCyan
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🤖", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("NOVA Exam Intelligence", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text(affair.category, color = NeonCyan, fontSize = 10.sp)
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = affair.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prompt selector chips
                    Text("Explore with NOVA:", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "exam angle" to "🎯 Exam Angle",
                            "mcqs" to "✍️ Practice MCQs",
                            "simple explanation" to "💡 Explain Simply"
                        ).forEach { (actionKey, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x22FFFFFF))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                    .springClickable(
                                        testTag = "nova_action_$actionKey",
                                        onClick = { onRequestAction(actionKey) }
                                    )
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Content / AI Response Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33000000))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        if (isAnalyzing) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("NOVA is synthesizing exam insights...", color = Color.White, fontSize = 12.sp)
                            }
                        } else if (!analysis.isNullOrBlank()) {
                            Text(
                                text = analysis,
                                color = Color(0xFFF1F5F9),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        } else {
                            Text(
                                text = "Select an option above to generate exam question patterns, background analysis, or high-yield revision points with NOVA.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSaveToSmartNotes,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Smart Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Done", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// EMPTY & FILTER STATES
// =========================================================================

@Composable
private fun EmptyCurrentAffairsView(
    category: String,
    searchQuery: String,
    onReset: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        fillAlpha = 0.8f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔍", fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (searchQuery.isNotBlank()) "No updates match \"$searchQuery\"" else if (category == "Saved") "No Saved Items" else "No $category Updates Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (category == "Saved") "Tap the bookmark icon on any current affair to save it here for fast revision." else "Try adjusting your search query, selecting another category, or fetching the latest 30-day radar.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Reset Filters & Refresh Radar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyNoticesView(
    onRefresh: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        fillAlpha = 0.8f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ElectricIndigo.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📢", fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No Official Exam Notices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Official testing agency notices, syllabus updates, and admit card releases will appear here automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Check for Updates", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

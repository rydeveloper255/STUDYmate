package com.example.ui.screens.intelligence

import android.content.Context
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.screens.home.openUrlInBrowser
import com.example.ui.theme.*

enum class LiveFeedTab(val title: String, val hindiTitle: String, val icon: String) {
    ALL("All", "सभी", "🌐"),
    OFFICIAL("Official", "आधिकारिक", "🏛️"),
    EXAM_UPDATES("Exam Updates", "परीक्षा अपडेट", "📢"),
    CURRENT_AFFAIRS("Current Affairs", "करेंट अफेयर्स", "📰"),
    TRENDING("Trending", "ट्रेंडिंग", "🔥"),
    SAVED("Saved", "सेव्ड", "🔖")
}

enum class TimeFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveExamIntelligenceScreen(
    feedState: LiveExamFeedState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onToggleSaveUpdate: (String, Boolean) -> Unit,
    onToggleSaveTrending: (String, Boolean) -> Unit,
    onStartQuizForTopic: (String, String) -> Unit,
    onAskNovaAboutUpdate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    var selectedTab by remember { mutableStateOf(LiveFeedTab.ALL) }
    var selectedTimeFilter by remember { mutableStateOf(TimeFilter.ALL) }
    var language by remember { mutableStateOf("English") } // "English" or "हिंदी"
    var searchQuery by remember { mutableStateOf("") }
    var activeDetailUpdate by remember { mutableStateOf<LiveExamUpdateEntity?>(null) }
    var activeDetailTrending by remember { mutableStateOf<TrendingExamTopicEntity?>(null) }

    // Filter items
    val filteredUpdates = remember(feedState.liveNews, selectedTab, selectedTimeFilter, searchQuery) {
        feedState.liveNews.filter { item ->
            val matchesTab = when (selectedTab) {
                LiveFeedTab.ALL -> true
                LiveFeedTab.OFFICIAL -> item.isVerifiedOfficial || item.category == LiveExamCategory.OFFICIAL_NOTIFICATION.name
                LiveFeedTab.EXAM_UPDATES -> item.category == LiveExamCategory.EXAM_UPDATE.name || item.category == LiveExamCategory.SYLLABUS_PATTERN.name || item.category == LiveExamCategory.URGENT_UPDATE.name
                LiveFeedTab.CURRENT_AFFAIRS -> item.category == LiveExamCategory.CURRENT_AFFAIRS.name
                LiveFeedTab.TRENDING -> false
                LiveFeedTab.SAVED -> item.isSaved
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                        item.summary.contains(searchQuery, ignoreCase = true) ||
                        item.sourceName.contains(searchQuery, ignoreCase = true)
            }

            matchesTab && matchesSearch
        }
    }

    val filteredTrending = remember(feedState.trendingTopics, selectedTab, searchQuery) {
        if (selectedTab != LiveFeedTab.ALL && selectedTab != LiveFeedTab.TRENDING) {
            emptyList()
        } else {
            feedState.trendingTopics.filter { topic ->
                if (searchQuery.isBlank()) true else {
                    topic.title.contains(searchQuery, ignoreCase = true) ||
                            topic.whyItMatters.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.95f) else Color(0xFFF8FAFC).copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(36.dp).testTag("live_exam_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Exam Intelligence",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Your Exam: ${feedState.examName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) NeonCyan else DeepIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Language Selector Pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDark) Color(0x3038BDF8) else Color(0x206366F1),
                                border = BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.springClickable {
                                    language = if (language == "English") "हिंदी" else "English"
                                }
                            ) {
                                Text(
                                    text = if (language == "English") "🇬🇧 EN" else "🇮🇳 HI",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) NeonCyan else DeepIndigo,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp
                                )
                            }

                            // Refresh Button
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.size(36.dp).testTag("live_exam_screen_refresh_btn")
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NeonCyan)
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Refresh",
                                        tint = if (isDark) NeonCyan else DeepIndigo
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("live_exam_search_bar"),
                        placeholder = {
                            Text(
                                text = "Search official notices, dates, schemes...",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = if (isDark) Color(0x30FFFFFF) else Color(0x20000000),
                            focusedContainerColor = if (isDark) Color(0x201E293B) else Color(0x08000000),
                            unfocusedContainerColor = if (isDark) Color(0x151E293B) else Color(0x05000000)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = {}
                    ) {
                        LiveFeedTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                label = {
                                    Text(
                                        text = "${tab.icon} ${if (language == "English") tab.title else tab.hindiTitle}",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.padding(end = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isDark) NeonCyan.copy(alpha = 0.25f) else DeepIndigo.copy(alpha = 0.15f),
                                    selectedLabelColor = if (isDark) NeonCyan else DeepIndigo,
                                    containerColor = if (isDark) Color(0x15FFFFFF) else Color(0x08000000),
                                    labelColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                ),
                                border = if (isSelected) BorderStroke(1.dp, NeonCyan) else null
                            )
                        }
                    }
                }
            }
        },
        containerColor = if (isDark) Color(0xFF0B0F19) else Color(0xFFF1F5F9)
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // Status & Time filter row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = feedState.statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold
                    )

                    // Time filter chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TimeFilter.values().forEach { filter ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedTimeFilter == filter) (if (isDark) Color(0x3038BDF8) else Color(0x206366F1)) else Color.Transparent,
                                modifier = Modifier.springClickable { selectedTimeFilter = filter }
                            ) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTimeFilter == filter) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTimeFilter == filter) (if (isDark) NeonCyan else DeepIndigo) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Trending section (if tab is ALL or TRENDING)
            if (filteredTrending.isNotEmpty()) {
                item {
                    Text(
                        text = "🔥 High-Yield Trending Topics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }

                items(filteredTrending, key = { it.id }) { topic ->
                    TrendingTopicFeedCard(
                        topic = topic,
                        isDark = isDark,
                        language = language,
                        onClick = { activeDetailTrending = topic },
                        onToggleSave = { onToggleSaveTrending(topic.id, !topic.isSaved) },
                        onStartQuiz = { onStartQuizForTopic(topic.category, topic.title) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📢 Latest Verified Exam Updates & Notices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            }

            // Live updates list
            if (isRefreshing && filteredUpdates.isEmpty() && filteredTrending.isEmpty()) {
                item {
                    GlassListSkeleton(itemCount = 4)
                }
            } else if (filteredUpdates.isEmpty() && filteredTrending.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🔍", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No matching updates found for ${feedState.examName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try refreshing or changing the filter criteria.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRefresh,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) NeonCyan else DeepIndigo)
                            ) {
                                Text("Refresh from Web & Official Sources", color = if (isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredUpdates, key = { it.id }) { update ->
                    LiveUpdateDetailedFeedCard(
                        update = update,
                        isDark = isDark,
                        language = language,
                        onClick = { activeDetailUpdate = update },
                        onToggleSave = { onToggleSaveUpdate(update.id, !update.isSaved) },
                        onOpenSource = { openUrlInBrowser(context, update.sourceUrl) }
                    )
                }
            }
        }
    }

    // Detail Bottom Sheet Modal for Updates
    if (activeDetailUpdate != null) {
        val update = activeDetailUpdate!!
        LiveUpdateDetailModal(
            update = update,
            isDark = isDark,
            language = language,
            onDismiss = { activeDetailUpdate = null },
            onToggleSave = { onToggleSaveUpdate(update.id, !update.isSaved) },
            onOpenSource = { openUrlInBrowser(context, update.sourceUrl) },
            onMakePracticeQuiz = {
                activeDetailUpdate = null
                onStartQuizForTopic("Live Intelligence", update.title)
            },
            onAskNova = {
                activeDetailUpdate = null
                onAskNovaAboutUpdate("Explain relevance and make key points for: ${update.title}")
            },
            onShare = {
                shareUpdate(context, update)
            }
        )
    }

    // Detail Bottom Sheet Modal for Trending Topics
    if (activeDetailTrending != null) {
        val topic = activeDetailTrending!!
        TrendingTopicDetailModal(
            topic = topic,
            isDark = isDark,
            language = language,
            onDismiss = { activeDetailTrending = null },
            onToggleSave = { onToggleSaveTrending(topic.id, !topic.isSaved) },
            onStartQuiz = {
                activeDetailTrending = null
                onStartQuizForTopic(topic.category, topic.title)
            },
            onAskNova = {
                activeDetailTrending = null
                onAskNovaAboutUpdate("Explain why this trending topic is critical for my exam: ${topic.title}")
            }
        )
    }
}

@Composable
private fun LiveUpdateDetailedFeedCard(
    update: LiveExamUpdateEntity,
    isDark: Boolean,
    language: String,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
    onOpenSource: () -> Unit
) {
    val categoryColor = when (update.category) {
        LiveExamCategory.OFFICIAL_NOTIFICATION.name -> Color(0xFF10B981)
        LiveExamCategory.EXAM_UPDATE.name -> Color(0xFF3B82F6)
        LiveExamCategory.CURRENT_AFFAIRS.name -> Color(0xFFF59E0B)
        LiveExamCategory.SYLLABUS_PATTERN.name -> Color(0xFF8B5CF6)
        else -> Color(0xFFEF4444)
    }

    val categoryLabel = when (update.category) {
        LiveExamCategory.OFFICIAL_NOTIFICATION.name -> if (language == "English") "🟢 Official Notification" else "🟢 आधिकारिक अधिसूचना"
        LiveExamCategory.EXAM_UPDATE.name -> if (language == "English") "🔵 Exam Update" else "🔵 परीक्षा अपडेट"
        LiveExamCategory.CURRENT_AFFAIRS.name -> if (language == "English") "🟠 Current Affairs" else "🟠 समसामयिकी"
        LiveExamCategory.SYLLABUS_PATTERN.name -> if (language == "English") "🟣 Syllabus & Pattern" else "🟣 पाठ्यक्रम पैटर्न"
        else -> if (language == "English") "🔴 Urgent Notice" else "🔴 अति आवश्यक सूचना"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feed_card_${update.id}"),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Category & Save Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, categoryColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }

                    if (update.isVerifiedOfficial) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0x2210B981)
                        ) {
                            Text(
                                text = "✓ Verified Official",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (update.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (update.isSaved) Color(0xFFF59E0B) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Headline
            Text(
                text = update.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Summary
            Text(
                text = update.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                lineHeight = 19.sp,
                fontSize = 13.sp
            )

            if (update.whyItMatters.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0x2038BDF8) else Color(0x106366F1)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "💡", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = update.whyItMatters,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) NeonCyan else DeepIndigo,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meta & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Source: ${update.sourceName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${update.publishedAt} • Score ${update.importanceScore}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        fontSize = 9.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Details", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) NeonCyan else DeepIndigo)
                    }

                    if (update.sourceUrl.isNotBlank()) {
                        FilledTonalButton(
                            onClick = onOpenSource,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = "Open Source", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingTopicFeedCard(
    topic: TrendingExamTopicEntity,
    isDark: Boolean,
    language: String,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
    onStartQuiz: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trending_feed_card_${topic.id}"),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x20F59E0B),
                    border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "🔥 ${topic.category}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (topic.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (topic.isSaved) Color(0xFFF59E0B) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = topic.whyItMatters,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "High exam probability",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Read", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) NeonCyan else DeepIndigo)
                    }

                    Button(
                        onClick = onStartQuiz,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) NeonCyan else DeepIndigo)
                    ) {
                        Text("✍️ Practice Quiz", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.Black else Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Detailed Modal Bottom Sheet for an Update
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveUpdateDetailModal(
    update: LiveExamUpdateEntity,
    isDark: Boolean,
    language: String,
    onDismiss: () -> Unit,
    onToggleSave: () -> Unit,
    onOpenSource: () -> Unit,
    onMakePracticeQuiz: () -> Unit,
    onAskNova: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (update.isVerifiedOfficial) {
                        Surface(shape = CircleShape, color = Color(0x2210B981)) {
                            Text(
                                text = "✓ Verified Official Authority",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp
                            )
                        }
                    } else {
                        Surface(shape = CircleShape, color = Color(0x223B82F6)) {
                            Text(
                                text = "Reported Update",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Outlined.Share, contentDescription = "Share", tint = if (isDark) Color.White else Color(0xFF0F172A))
                    }
                    IconButton(onClick = onToggleSave, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (update.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (update.isSaved) Color(0xFFF59E0B) else (if (isDark) Color.White else Color(0xFF0F172A))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = update.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Published & Source details
            Text(
                text = "Source: ${update.sourceName} • ${update.publishedAt}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x251E293B) else Color(0x08000000),
                border = BorderStroke(0.5.dp, if (isDark) Color(0x30FFFFFF) else Color(0x20000000))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Executive Summary",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) NeonCyan else DeepIndigo,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = update.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        lineHeight = 22.sp
                    )
                }
            }

            if (update.whyItMatters.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0x2038BDF8) else Color(0x106366F1)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "💡 Why It Matters For Your Exam",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) NeonCyan else DeepIndigo
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = update.whyItMatters,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (update.keyTakeaways.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Key Takeaways for Aspirants:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                update.keyTakeaways.forEach { takeaway ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "• ", color = NeonCyan, fontWeight = FontWeight.Bold)
                        Text(
                            text = takeaway,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (update.sourceUrl.isNotBlank()) {
                    Button(
                        onClick = onOpenSource,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) NeonCyan else DeepIndigo)
                    ) {
                        Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = null, tint = if (isDark) Color.Black else Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (update.isVerifiedOfficial) "Open Official Portal Notice" else "Open Verified Web Source",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.Black else Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onMakePracticeQuiz,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("✍️ Practice Quiz", fontWeight = FontWeight.Bold, color = if (isDark) NeonCyan else DeepIndigo)
                    }

                    OutlinedButton(
                        onClick = onAskNova,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🤖 Ask NOVA", fontWeight = FontWeight.Bold, color = if (isDark) NeonCyan else DeepIndigo)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Detailed Modal Bottom Sheet for Trending Topics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendingTopicDetailModal(
    topic: TrendingExamTopicEntity,
    isDark: Boolean,
    language: String,
    onDismiss: () -> Unit,
    onToggleSave: () -> Unit,
    onStartQuiz: () -> Unit,
    onAskNova: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x20F59E0B)
                ) {
                    Text(
                        text = "🔥 ${topic.category}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                IconButton(onClick = onToggleSave, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (topic.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (topic.isSaved) Color(0xFFF59E0B) else (if (isDark) Color.White else Color(0xFF0F172A))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x2038BDF8) else Color(0x106366F1)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "💡 Why It Matters For ${topic.examName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) NeonCyan else DeepIndigo
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = topic.whyItMatters,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStartQuiz,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) NeonCyan else DeepIndigo)
            ) {
                Text("✍️ Start 5-Q Practice Quiz on This Topic", fontWeight = FontWeight.Bold, color = if (isDark) Color.Black else Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onAskNova,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🤖 Ask NOVA to Deep-Dive", fontWeight = FontWeight.Bold, color = if (isDark) NeonCyan else DeepIndigo)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun shareUpdate(context: Context, update: LiveExamUpdateEntity) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, update.title)
            putExtra(
                Intent.EXTRA_TEXT,
                "${update.title}\n\n${update.summary}\n\nSource: ${update.sourceName}\nRead more: ${update.sourceUrl}"
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Exam Update")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share update", Toast.LENGTH_SHORT).show()
    }
}

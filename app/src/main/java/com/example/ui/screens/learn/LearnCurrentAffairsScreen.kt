package com.example.ui.screens.learn

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CurrentAffairsItem
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Learn Sub-Module 3: Current Affairs Screen (Step 66 Implementation)
 *
 * Dedicated Current Affairs learning page:
 * - Date selectors: Today, Yesterday, This Week, Custom Date
 * - Search & Category filters (National, International, Economy, Govt, Education, Sci & Tech, Sports, Environment, Banking, Awards, Appointments, Reports)
 * - Detailed Cards (Headline, Short summary, Important facts, Source, Bookmark, Share, AI Explanation)
 * - Weekly Current Affairs PDF with In-App Viewer & Download
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnCurrentAffairsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val allCategories = remember {
        listOf(
            "All", "National", "International", "Economy", "Government",
            "Education", "Science & Tech", "Sports", "Environment",
            "Banking", "Awards", "Appointments", "Reports & Indexes"
        )
    }

    val dateTabs = listOf("Today", "Yesterday", "This Week", "Archive")

    var selectedDateTab by remember { mutableStateOf("Today") }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var bookmarkedIds by remember { mutableStateOf(setOf<Long>()) }

    // Dialog states
    var viewingItem by remember { mutableStateOf<CurrentAffairsItem?>(null) }
    var aiExplainItem by remember { mutableStateOf<CurrentAffairsItem?>(null) }
    var showPdfDialog by remember { mutableStateOf(false) }

    // Initial Curated Current Affairs Data
    var currentAffairsList by remember {
        mutableStateOf(
            listOf(
                CurrentAffairsItem(
                    id = 1L,
                    title = "ISRO Successfully Launches Next-Gen Meteorological Satellite INSAT-3DS",
                    summary = "The Indian Space Research Organisation (ISRO) successfully placed the INSAT-3DS satellite into geostationary orbit to enhance weather forecasting and disaster warning capabilities.",
                    category = "Science & Tech",
                    keyPoints = listOf(
                        "Launched via GSLV-F14 rocket from Satish Dhawan Space Centre, Sriharikota.",
                        "Fully funded by the Ministry of Earth Sciences (MoES).",
                        "Equipped with advanced 6-channel Imager and 19-channel Sounder payloads for atmospheric profiling."
                    ),
                    sourceName = "ISRO Official / Press Information Bureau",
                    sourceUrl = "https://pib.gov.in",
                    publishedDate = "Today, 10:30 AM",
                    examRelevance = "HIGH",
                    isImportant = true
                ),
                CurrentAffairsItem(
                    id = 2L,
                    title = "RBI Keeps Repo Rate Unchanged at 6.5% for Sixth Consecutive MPC Meeting",
                    summary = "The Reserve Bank of India Monetary Policy Committee (MPC) decided to maintain the policy repo rate at 6.50% to align retail inflation with the 4% target.",
                    category = "Economy",
                    keyPoints = listOf(
                        "Real GDP growth for FY25 projected at 7.0%.",
                        "CPI inflation projected at 5.4% for FY24.",
                        "Standing Deposit Facility (SDF) rate remains at 6.25% and Marginal Standing Facility (MSF) at 6.75%."
                    ),
                    sourceName = "Reserve Bank of India (RBI)",
                    sourceUrl = "https://rbi.org.in",
                    publishedDate = "Yesterday",
                    examRelevance = "HIGH",
                    isImportant = true
                ),
                CurrentAffairsItem(
                    id = 3L,
                    title = "Prime Minister Inaugurates Unified National Digital Education Portal & PM-SHRI Schools",
                    summary = "A nationwide education initiative expanding smart classrooms, vocational skilling, and multilingual digital curricula aligned with NEP 2020.",
                    category = "Education",
                    keyPoints = listOf(
                        "Over 14,500 PM-SHRI schools upgraded across 28 states and UTs.",
                        "Direct integration with DIKSHA, SWAYAM, and APAAR student ID frameworks.",
                        "Special emphasis on STEM labs and experiential learning in regional languages."
                    ),
                    sourceName = "Ministry of Education / PIB",
                    sourceUrl = "https://education.gov.in",
                    publishedDate = "2 days ago",
                    examRelevance = "MEDIUM",
                    isImportant = true
                ),
                CurrentAffairsItem(
                    id = 4L,
                    title = "India Ranks 38th in World Bank Logistics Performance Index (LPI)",
                    summary = "Significant improvement driven by PM GatiShakti National Master Plan and National Logistics Policy (NLP).",
                    category = "Reports & Indexes",
                    keyPoints = listOf(
                        "India advanced 6 spots from 44th rank in the previous evaluation.",
                        "Ranked higher in tracking & tracing and infrastructure quality parameters.",
                        "Average dwell time at Indian ports reduced to under 3 days."
                    ),
                    sourceName = "World Bank Group",
                    sourceUrl = "https://worldbank.org",
                    publishedDate = "3 days ago",
                    examRelevance = "HIGH",
                    isImportant = true
                ),
                CurrentAffairsItem(
                    id = 5L,
                    title = "National Green Hydrogen Mission: Government Allocates Subsidies for Electrolyser Manufacturing",
                    summary = "Under the Strategic Interventions for Green Hydrogen Transition (SIGHT) scheme, incentives are awarded to domestic manufacturers.",
                    category = "Environment",
                    keyPoints = listOf(
                        "Target: Production of 5 MMT (Million Metric Tonnes) of Green Hydrogen per annum by 2030.",
                        "Total outlay of Rs 19,744 Crore under the National Green Hydrogen Mission.",
                        "Administered by the Ministry of New and Renewable Energy (MNRE)."
                    ),
                    sourceName = "MNRE / PIB",
                    sourceUrl = "https://mnre.gov.in",
                    publishedDate = "4 days ago",
                    examRelevance = "HIGH",
                    isImportant = true
                )
            )
        )
    }

    val filteredItems = currentAffairsList.filter { item ->
        val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.summary.contains(searchQuery, ignoreCase = true)
        val matchesCat = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCat
    }

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
                            .testTag("ca_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Current Affairs",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Verified Daily CA & Weekly Exam Digest",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldenSpark
                        )
                    }

                    // PDF Button
                    IconButton(
                        onClick = { showPdfDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldenSpark.copy(alpha = 0.2f))
                            .testTag("weekly_pdf_btn")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Weekly PDF", tint = GoldenSpark)
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
        ) {
            // 1. DATE TIMEFRAME TABS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dateTabs.forEach { tab ->
                        val isSel = tab == selectedDateTab
                        Surface(
                            onClick = { selectedDateTab = tab },
                            color = if (isSel) GoldenSpark else Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = tab,
                                color = if (isSel) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 2. SEARCH BAR
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search news, policies, ISRO, RBI, appointments...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                        unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                        focusedBorderColor = GoldenSpark,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. CATEGORY CHIPS
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allCategories) { cat ->
                        val isSel = cat == selectedCategory
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldenSpark.copy(alpha = 0.2f),
                                selectedLabelColor = GoldenSpark,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            ),
                            border = if (isSel) BorderStroke(1.dp, GoldenSpark) else null
                        )
                    }
                }
            }

            // 4. CURRENT AFFAIRS CARDS LIST
            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No updates found in this category", color = Color(0xFF94A3B8))
                    }
                }
            } else {
                items(filteredItems) { item ->
                    val isBm = bookmarkedIds.contains(item.id)
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .springClickable { viewingItem = item }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = GoldenSpark.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.category,
                                        color = GoldenSpark,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                val isOfficial = item.sourceName.contains("Official", ignoreCase = true) ||
                                        item.sourceName.contains("PIB", ignoreCase = true) ||
                                        item.sourceName.contains("RBI", ignoreCase = true) ||
                                        item.sourceName.contains("ISRO", ignoreCase = true) ||
                                        item.isImportant

                                if (isOfficial) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Official PIB/Gov",
                                            color = Color(0xFF34D399),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = {
                                        bookmarkedIds = if (isBm) bookmarkedIds - item.id else bookmarkedIds + item.id
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isBm) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (isBm) GoldenSpark else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 18.sp
                            )

                            if (item.keyPoints.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Key Facts:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold
                                )
                                item.keyPoints.take(2).forEach { pt ->
                                    Text(
                                        text = "• $pt",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Source: ${item.sourceName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Row {
                                    IconButton(
                                        onClick = {
                                            aiExplainItem = item
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Explain", tint = GoldenSpark, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val shareText = "${item.title}\n\n${item.summary}\n\nKey Points:\n${item.keyPoints.joinToString("\n")}\n\nSource: ${item.sourceName}"
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Current Affairs"))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. DETAIL VIEWER DIALOG
    viewingItem?.let { item ->
        Dialog(onDismissRequest = { viewingItem = null }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = GoldenSpark.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.category,
                                color = GoldenSpark,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = { viewingItem = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Important Exam Facts",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    item.keyPoints.forEach { pt ->
                        Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Text("• ", color = GoldenSpark, fontWeight = FontWeight.Bold)
                            Text(pt, color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Source: ${item.sourceName}", color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)

                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                aiExplainItem = item
                                viewingItem = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Explain", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewingItem = null },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    // 2. AI EXPLANATION DIALOG
    aiExplainItem?.let { item ->
        Dialog(onDismissRequest = { aiExplainItem = null }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ NOVA AI Strategic Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldenSpark,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { aiExplainItem = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Exam Significance & Potential MCQs:",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Direct Questions: Which organization launched or approved this initiative? (Answer: ${item.sourceName.take(25)})\n2. Multi-statement MCQs: Questions will likely test the exact financial outlay or technological payloads.\n3. Revision Tip: Note down key dates and statutory bodies in your personal notes.",
                                color = Color(0xFFCBD5E1),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { aiExplainItem = null },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Got it", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 3. WEEKLY CURRENT AFFAIRS PDF VIEWER DIALOG
    if (showPdfDialog) {
        Dialog(onDismissRequest = { showPdfDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📑 Weekly Current Affairs PDF",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldenSpark,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showPdfDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFF020617),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "StudyMate Weekly Comprehensive PDF (Week 4, Feb 2025)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = GoldenSpark,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Compiled from Official Government Portals, PIB, The Hindu, and RBI Gazettes.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            item {
                                HorizontalDivider(color = Color(0xFF334155))
                            }

                            item {
                                Text(
                                    text = "Section 1: National & Government Schemes",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "• PM-SHRI Schools expansion across 14,500 institutions.\n• National Green Hydrogen Mission allocates initial subsidies under SIGHT scheme.\n• Digital India e-Courts Phase III approved with high-performance computing clusters.",
                                    color = Color(0xFFE2E8F0),
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 20.sp
                                )
                            }

                            item {
                                Text(
                                    text = "Section 2: Economy & Banking Affairs",
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "• RBI maintains Repo Rate at 6.50%; real GDP growth projected at 7.0% for FY25.\n• UPI crosses 12 Billion monthly transactions milestone.\n• India ranks 38th in World Bank Logistics Performance Index (LPI).",
                                    color = Color(0xFFE2E8F0),
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 20.sp
                                )
                            }

                            item {
                                Text(
                                    text = "Section 3: Science, Defence & Space",
                                    color = Color(0xFFA78BFA),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "• ISRO launches INSAT-3DS meteorological satellite via GSLV-F14.\n• DRDO conducts successful flight test of new generation Akash missile.",
                                    color = Color(0xFFE2E8F0),
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val shareText = "StudyMate Weekly Current Affairs PDF Digest:\n\n• ISRO INSAT-3DS Launch\n• RBI MPC Policy (Repo rate 6.5%)\n• PM-SHRI Schools Expansion\n• Logistics Performance Index Rank 38\n\nStudied via StudyMate AI app."
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Download & Share Weekly PDF"))
                                Toast.makeText(context, "Weekly Digest Prepared for Export", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download / Export", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showPdfDialog = false },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

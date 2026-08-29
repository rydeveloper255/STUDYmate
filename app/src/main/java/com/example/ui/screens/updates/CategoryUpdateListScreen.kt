package com.example.ui.screens.updates

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.updates.LatestUpdateItem
import com.example.data.model.updates.UpdateCategory
import com.example.localization.GlobalLanguageSwitcher
import com.example.localization.appString
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryUpdateListScreen(
    category: UpdateCategory,
    items: List<LatestUpdateItem>,
    isLoading: Boolean,
    errorMessage: String?,
    searchQuery: String,
    selectedOrg: String,
    selectedExam: String,
    selectedSort: String,
    onSearchChange: (String) -> Unit,
    onOrgFilterChange: (String) -> Unit,
    onExamFilterChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectDetail: (LatestUpdateItem) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onBackToLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val context = LocalContext.current
    val accent = category.accentColor
    var localSearchText by remember { mutableStateOf(searchQuery) }

    // Debounced search trigger
    LaunchedEffect(localSearchText) {
        delay(350)
        if (localSearchText != searchQuery) {
            onSearchChange(localSearchText)
        }
    }

    BackHandler {
        onBackToLauncher()
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f),
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            IconButton(
                                onClick = onBackToLauncher,
                                modifier = Modifier.testTag("category_back_button_${category.key}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Latest Updates",
                                    tint = if (isDark) Color.White else DeepIndigo
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = category.titleEn,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else DeepIndigo
                                )
                                Text(
                                    text = category.subtitleEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.testTag("refresh_category_${category.key}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh",
                                    tint = accent
                                )
                            }
                            GlobalLanguageSwitcher(modifier = Modifier.testTag("category_lang_switcher_${category.key}"))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Debounced Search Field
                    OutlinedTextField(
                        value = localSearchText,
                        onValueChange = { localSearchText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_field_${category.key}"),
                        placeholder = {
                            Text(
                                text = when (category) {
                                    UpdateCategory.VACANCY -> "Search job title, SSC, RRB, Post..."
                                    UpdateCategory.ADMIT_CARD -> "Search exam, roll no slip, SSC, RRB..."
                                    UpdateCategory.RESULT -> "Search result, cutoff, scorecard..."
                                    UpdateCategory.ANSWER_KEY -> "Search answer key, objection link..."
                                    UpdateCategory.ADMISSION -> "Search university, course, CUET..."
                                },
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (localSearchText.isNotBlank()) {
                                IconButton(onClick = { localSearchText = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                            focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                            unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val orgList = when (category) {
                            UpdateCategory.VACANCY -> listOf("All", "SSC", "RRB", "BPSC", "UPSC", "Banking", "Defence")
                            UpdateCategory.ADMIT_CARD -> listOf("All", "SSC", "IBPS", "RRB", "UPSC", "NTA")
                            UpdateCategory.RESULT -> listOf("All", "SSC", "RRB", "CBSE", "UPSC", "State PSC")
                            UpdateCategory.ANSWER_KEY -> listOf("All", "SSC", "NTA", "GATE", "State Board")
                            UpdateCategory.ADMISSION -> listOf("All", "NTA", "IGNOU", "IIT", "Central Univ")
                        }

                        orgList.forEach { org ->
                            val isSelected = (org == "All" && selectedOrg.isBlank()) || selectedOrg.equals(org, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onOrgFilterChange(if (org == "All") "" else org) },
                                label = { Text(org, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.2f),
                                    selectedLabelColor = accent
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("filter_org_${org.lowercase()}")
                            )
                        }

                        // Date sort chip
                        val isSortDeadline = selectedSort == "DEADLINE_SOON"
                        FilterChip(
                            selected = isSortDeadline,
                            onClick = { onSortChange(if (isSortDeadline) "NEWEST" else "DEADLINE_SOON") },
                            label = { Text(if (isSortDeadline) "Ending Soon ⏳" else "Latest First 📅", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGold.copy(alpha = 0.2f),
                                selectedLabelColor = AmberGold
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("sort_filter_button")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(appBackgroundGradient(isDark))
        ) {
            when {
                isLoading && items.isEmpty() -> {
                    // Loading State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("category_loading_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = accent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading latest updates…",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else DeepIndigo
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Fetching verified ${category.titleEn} notices",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                errorMessage != null && items.isEmpty() -> {
                    // Error State with Retry
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                            .testTag("category_error_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = CoralRose,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Unable to load updates.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else DeepIndigo,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("category_retry_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items.isEmpty() -> {
                    // Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                            .testTag("category_empty_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = accent.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No updates available.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else DeepIndigo
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (localSearchText.isNotBlank()) "No records match '$localSearchText'. Try adjusting search or filters."
                                   else "No active ${category.titleEn.lowercase()} announcements found right now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        if (localSearchText.isNotBlank() || selectedOrg.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    localSearchText = ""
                                    onSearchChange("")
                                    onOrgFilterChange("")
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear Filters")
                            }
                        }
                    }
                }

                else -> {
                    // List View
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Showing ${items.size} verified updates",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Surface(
                                    color = accent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = category.badgeTextEn,
                                        color = accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        items(
                            items = items,
                            key = { it.id }
                        ) { updateItem ->
                            CategoryUpdateCard(
                                item = updateItem,
                                category = category,
                                isDark = isDark,
                                onOpenDetail = { onSelectDetail(updateItem) },
                                onToggleSave = { onToggleSave(updateItem.id, !updateItem.isSaved) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryUpdateCard(
    item: LatestUpdateItem,
    category: UpdateCategory,
    isDark: Boolean,
    onOpenDetail: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = category.accentColor

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() }
            .testTag("update_card_${item.id}"),
        backgroundColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Organization + Save Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.organization,
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(32.dp).testTag("save_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save Update",
                        tint = if (item.isSaved) AmberGold else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else DeepIndigo,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            // Post name or Exam name if available
            if (item.postName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Post: ${item.postName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Short Description
            if (item.shortDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.shortDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Key Dates & Tags Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Specific Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (category) {
                            UpdateCategory.VACANCY -> "Last Date: ${item.lastDate ?: "Open"}"
                            UpdateCategory.ADMIT_CARD -> "Exam: ${item.examDate ?: item.publishedDate ?: "Available"}"
                            UpdateCategory.RESULT -> "Declared: ${item.publishedDate ?: "Declared"}"
                            UpdateCategory.ANSWER_KEY -> "Key Date: ${item.publishedDate ?: "Released"}"
                            UpdateCategory.ADMISSION -> "Last Date: ${item.lastDate ?: "Open"}"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )
                }

                if (item.totalVacancies != null && item.totalVacancies > 0) {
                    Text(
                        text = "${item.totalVacancies} Posts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: Direct Action (Apply/Download/Result) + Read More
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val directUrl = item.applyUrl.ifBlank { item.downloadUrl }
                if (directUrl.isNotBlank()) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(directUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("card_action_btn_${item.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = when (category) {
                                UpdateCategory.VACANCY -> "Apply Now"
                                UpdateCategory.ADMIT_CARD -> "Download"
                                UpdateCategory.RESULT -> "View Result"
                                UpdateCategory.ANSWER_KEY -> "Answer Key"
                                UpdateCategory.ADMISSION -> "Apply Online"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenDetail,
                    modifier = Modifier
                        .weight(if (directUrl.isNotBlank()) 1f else 2f)
                        .height(38.dp)
                        .testTag("card_detail_btn_${item.id}"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Read More",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else DeepIndigo
                    )
                }
            }
        }
    }
}

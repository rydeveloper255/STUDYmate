package com.example.ui.screens.vacancy

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
import com.example.data.model.*
import com.example.localization.GlobalLanguageSwitcher
import com.example.localization.appString
import com.example.ui.theme.*

fun parseHexColor(hex: String, defaultColor: Color = Color(0xFF10B981)): Color {
    return try {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(clean))
    } catch (e: Exception) {
        defaultColor
    }
}

val INDIAN_STATES = listOf(
    "All India",
    "Uttar Pradesh",
    "Bihar",
    "Rajasthan",
    "Madhya Pradesh",
    "Maharashtra",
    "Karnataka",
    "Tamil Nadu",
    "West Bengal",
    "Delhi",
    "Haryana",
    "Punjab",
    "Gujarat",
    "Odisha",
    "Jharkhand",
    "Chhattisgarh",
    "Assam",
    "Uttarakhand"
)

val EDUCATION_QUALIFICATIONS = listOf(
    "Graduation / Any Degree",
    "12th Pass (Intermediate / 10+2)",
    "10th Pass (Matriculation)",
    "ITI / Technical Trade",
    "Diploma in Engineering",
    "B.Ed / D.El.Ed / CTET Qualified",
    "Post Graduate / Master's Degree"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartVacancyScreen(
    feedState: RecruitmentFeedState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onStateSelected: (String) -> Unit,
    onTabSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSortOptionSelected: (RecruitmentSortOption) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onSetReminder: (String, Boolean, Int) -> Unit,
    selectedDetailItem: RecruitmentEntity?,
    onSelectDetailItem: (RecruitmentEntity?) -> Unit,
    onUpdateProfile: ((UserRecruitmentProfile) -> Unit)? = null,
    onUpdateApplicationStatus: ((String, UserApplicationStatus, String, String, String, String) -> Unit)? = null,
    onUpdateDocumentsReady: ((String, List<String>) -> Unit)? = null,
    onUpdateChecklistChecked: ((String, List<String>) -> Unit)? = null,
    onFindJobsForMe: ((String?, String?, String?, Int?) -> Unit)? = null,
    onSubmitReport: ((String, String, String) -> Unit)? = null,
    notificationSettings: RecruitmentNotificationSettings = RecruitmentNotificationSettings(),
    onUpdateNotificationSettings: ((RecruitmentNotificationSettings) -> Unit)? = null,
    outboxItems: List<RecruitmentOutboxItem> = emptyList(),
    dailyDigest: DailyRecruitmentDigest? = null,
    diagnostics: AdminRecruitmentDiagnostics = AdminRecruitmentDiagnostics(),
    onMuteRecruitment: ((String) -> Unit)? = null,
    onUnmuteRecruitment: ((String) -> Unit)? = null,
    onMuteCategory: ((String) -> Unit)? = null,
    onUnmuteCategory: ((String) -> Unit)? = null,
    onMarkOutboxRead: ((String) -> Unit)? = null,
    onMarkAllOutboxRead: (() -> Unit)? = null,
    onDeleteOutboxItem: ((String) -> Unit)? = null,
    onClearAllOutbox: (() -> Unit)? = null,
    onNovaQuery: ((String) -> Unit)? = null,
    onBack: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val context = LocalContext.current
    var showStateSelector by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showFindJobsDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    // Spin animation for refresh button
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    // Intelligent hierarchical BackHandler to prevent accidental screen exits
    BackHandler(enabled = true) {
        when {
            selectedDetailItem != null -> onSelectDetailItem(null)
            showFindJobsDialog -> showFindJobsDialog = false
            showProfileDialog -> showProfileDialog = false
            showStateSelector -> showStateSelector = false
            showNotificationSettingsDialog -> showNotificationSettingsDialog = false
            showDiagnosticsDialog -> showDiagnosticsDialog = false
            else -> onBack()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_vacancy_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Recruitment Intelligence 3.0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        "VERIFIED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                        Text(
                            "Primary: sarkariresult.com + Official Boards",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedDetailItem != null) {
                                onSelectDetailItem(null)
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("vacancy_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Global Language Switcher
                    GlobalLanguageSwitcher(
                        isDark = isDark,
                        compact = true,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // System Diagnostics Button
                    IconButton(
                        onClick = { showDiagnosticsDialog = true },
                        modifier = Modifier.testTag("recruitment_diagnostics_button")
                    ) {
                        Icon(
                            Icons.Outlined.Analytics,
                            contentDescription = "System Health & Diagnostics",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Notification Engine Settings Button
                    IconButton(
                        onClick = { showNotificationSettingsDialog = true },
                        modifier = Modifier.testTag("recruitment_notif_settings_button")
                    ) {
                        Icon(
                            Icons.Outlined.NotificationsActive,
                            contentDescription = "Recruitment Notification Rules",
                            tint = if (notificationSettings.recruitmentNotificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Profile / Preference Button
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.testTag("vacancy_profile_button")
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Preferences",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("vacancy_refresh_button")
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh Updates",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = if (isRefreshing) Modifier.rotate(spinAngle) else Modifier
                        )
                    }
                    // Saved Items Filter
                    IconButton(
                        onClick = { onTabSelected("SAVED") },
                        modifier = Modifier.testTag("vacancy_saved_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (feedState.savedItems.isNotEmpty() || feedState.activeTrackedApplications.isNotEmpty()) {
                                    val count = feedState.savedItems.size + feedState.activeTrackedApplications.size
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("$count")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (feedState.selectedTab == "SAVED") Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Saved Watchlist & Tracker",
                                tint = if (feedState.selectedTab == "SAVED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) listOf(Color(0xFF0F172A), Color(0xFF020617))
                        else listOf(Color(0xFFF8FAFC), Color(0xFFEEF2F6))
                    )
                )
        ) {
            // Search Bar, State Selector & Sort
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = feedState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search RRB, SSC, Police, UPSC...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (feedState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vacancy_search_input")
                )

                Spacer(Modifier.width(6.dp))

                // State Filter Button
                Surface(
                    onClick = { showStateSelector = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (feedState.selectedState != "All India") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (feedState.selectedState != "All India") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.height(52.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = if (feedState.selectedState != "All India") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (feedState.selectedState.length > 8) feedState.selectedState.take(6) + ".." else feedState.selectedState,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (feedState.selectedState != "All India") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Sort Button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.Sort, contentDescription = "Sort Options", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        RecruitmentSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.label,
                                        fontWeight = if (feedState.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (feedState.sortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onSortOptionSelected(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // User Preference Summary / "Find Jobs For Me" Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clickable { showFindJobsDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎯", fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                "Personalized for: ${feedState.userProfile.selectedExam} • ${feedState.userProfile.educationQualification.take(16)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Tap to open 'Find Jobs For Me' smart match wizard",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Primary Navigation Tabs: Vacancies | Results | Admit Cards | Notifications | Alerts | Saved & Tracker
            val tabs = listOf(
                Triple(RecruitmentContentType.VACANCY.name, "🚀 Vacancies", feedState.allActiveVacancies.size),
                Triple(RecruitmentContentType.RESULT.name, "🏆 Results", feedState.resultsList.size),
                Triple(RecruitmentContentType.ADMIT_CARD.name, "🎫 Admit Cards", feedState.admitCardsList.size),
                Triple(RecruitmentContentType.NOTIFICATION.name, "📢 Notices", feedState.notificationsList.size),
                Triple("ALERTS", "🔔 Alerts (${outboxItems.size})", outboxItems.size),
                Triple("SAVED", "⭐ Tracker (${feedState.savedItems.size + feedState.activeTrackedApplications.size})", feedState.savedItems.size)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { (tabKey, title, count) ->
                    val isSelected = feedState.selectedTab == tabKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTabSelected(tabKey) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            // Category Chips (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RecruitmentCategory.values().forEach { cat ->
                    val isSelected = feedState.selectedCategory == cat.name
                    Surface(
                        onClick = { onCategorySelected(cat.name) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.icon, fontSize = 11.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                cat.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Main Content Lists
            when (feedState.selectedTab) {
                RecruitmentContentType.VACANCY.name -> {
                    VacanciesTabContent(
                        feedState = feedState,
                        dailyDigest = dailyDigest,
                        onSelectItem = onSelectDetailItem,
                        onToggleSave = onToggleSave,
                        onApply = { url -> openExternalBrowser(context, url) }
                    )
                }
                RecruitmentContentType.RESULT.name -> {
                    ResultsTabContent(
                        results = feedState.resultsList,
                        onSelectItem = onSelectDetailItem,
                        onOpenResult = { url -> openExternalBrowser(context, url) },
                        onToggleSave = onToggleSave
                    )
                }
                RecruitmentContentType.ADMIT_CARD.name -> {
                    AdmitCardsTabContent(
                        admitCards = feedState.admitCardsList,
                        onSelectItem = onSelectDetailItem,
                        onDownload = { url -> openExternalBrowser(context, url) },
                        onToggleSave = onToggleSave
                    )
                }
                RecruitmentContentType.NOTIFICATION.name -> {
                    NotificationsTabContent(
                        notifications = feedState.notificationsList,
                        onSelectItem = onSelectDetailItem,
                        onOpenNotice = { url -> openExternalBrowser(context, url) },
                        onToggleSave = onToggleSave
                    )
                }
                "ALERTS" -> {
                    AlertsTabContent(
                        outboxItems = outboxItems,
                        onMarkAsRead = { onMarkOutboxRead?.invoke(it) },
                        onMarkAllAsRead = { onMarkAllOutboxRead?.invoke() },
                        onDeleteItem = { onDeleteOutboxItem?.invoke(it) },
                        onClearAll = { onClearAllOutbox?.invoke() },
                        onActionClick = { item ->
                            onMarkOutboxRead?.invoke(item.id)
                            val deepLink = item.deepLink
                            if (deepLink.startsWith("recruitment://vacancy/")) {
                                val vId = deepLink.removePrefix("recruitment://vacancy/")
                                onTabSelected(RecruitmentContentType.VACANCY.name)
                                val entity = feedState.allActiveVacancies.find { it.id == vId }
                                    ?: feedState.latestForYouVacancies.find { it.id == vId }
                                if (entity != null) onSelectDetailItem(entity)
                            } else if (deepLink.startsWith("recruitment://result/")) {
                                val rId = deepLink.removePrefix("recruitment://result/")
                                onTabSelected(RecruitmentContentType.RESULT.name)
                                val entity = feedState.resultsList.find { it.id == rId }
                                if (entity != null) onSelectDetailItem(entity)
                            } else if (deepLink.startsWith("recruitment://admit_card/")) {
                                val aId = deepLink.removePrefix("recruitment://admit_card/")
                                onTabSelected(RecruitmentContentType.ADMIT_CARD.name)
                                val entity = feedState.admitCardsList.find { it.id == aId }
                                if (entity != null) onSelectDetailItem(entity)
                            }
                        }
                    )
                }
                "SAVED" -> {
                    SavedAndTrackerTabContent(
                        savedItems = feedState.savedItems,
                        trackedApplications = feedState.activeTrackedApplications,
                        onSelectItem = onSelectDetailItem,
                        onToggleSave = onToggleSave,
                        onSetReminder = onSetReminder
                    )
                }
            }
        }
    }

    // Notification Engine Settings Dialog
    if (showNotificationSettingsDialog && onUpdateNotificationSettings != null) {
        RecruitmentNotificationSettingsDialog(
            currentSettings = notificationSettings,
            onDismiss = { showNotificationSettingsDialog = false },
            onSave = { updated ->
                onUpdateNotificationSettings(updated)
                showNotificationSettingsDialog = false
            },
            onUnmuteCategory = { cat -> onUnmuteCategory?.invoke(cat) },
            onUnmuteRecruitment = { id -> onUnmuteRecruitment?.invoke(id) }
        )
    }

    // System Diagnostics Dialog
    if (showDiagnosticsDialog) {
        AdminDiagnosticsDialog(
            diagnostics = diagnostics,
            onDismiss = { showDiagnosticsDialog = false }
        )
    }

    // State Selector Dialog
    if (showStateSelector) {
        Dialog(onDismissRequest = { showStateSelector = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Target State", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showStateSelector = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    Text(
                        "Showing vacancies & state PSC jobs tailored for your region.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(INDIAN_STATES) { st ->
                            val isSelected = feedState.selectedState == st
                            Surface(
                                onClick = {
                                    onStateSelected(st)
                                    showStateSelector = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        st,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Profile / Preference Dialog
    if (showProfileDialog && onUpdateProfile != null) {
        UserRecruitmentProfileDialog(
            currentProfile = feedState.userProfile,
            onDismiss = { showProfileDialog = false },
            onSave = { updated ->
                onUpdateProfile(updated)
                showProfileDialog = false
            }
        )
    }

    // "Find Jobs For Me" Interactive Search Wizard
    if (showFindJobsDialog) {
        FindJobsForMeDialog(
            currentProfile = feedState.userProfile,
            onDismiss = { showFindJobsDialog = false },
            onSearch = { category, state, qual, age ->
                onFindJobsForMe?.invoke(category, state, qual, age)
                showFindJobsDialog = false
            }
        )
    }

    // Full Recruitment Details Dialog / Viewer
    if (selectedDetailItem != null) {
        RecruitmentDetailDialog(
            item = selectedDetailItem,
            userProfile = feedState.userProfile,
            onDismiss = { onSelectDetailItem(null) },
            onToggleSave = { onToggleSave(selectedDetailItem.id, !selectedDetailItem.isSaved) },
            onSetReminder = { enabled, days -> onSetReminder(selectedDetailItem.id, enabled, days) },
            onUpdateApplicationStatus = { status, appNo, rollNo, post, notes ->
                onUpdateApplicationStatus?.invoke(selectedDetailItem.id, status, appNo, rollNo, post, notes)
            },
            onUpdateDocumentsReady = { docs ->
                onUpdateDocumentsReady?.invoke(selectedDetailItem.id, docs)
            },
            onUpdateChecklistChecked = { list ->
                onUpdateChecklistChecked?.invoke(selectedDetailItem.id, list)
            },
            onSubmitReport = { category, comment ->
                onSubmitReport?.invoke(selectedDetailItem.id, category, comment)
            },
            onOpenUrl = { url -> openExternalBrowser(context, url) }
        )
    }
}

@Composable
fun VacanciesTabContent(
    feedState: RecruitmentFeedState,
    dailyDigest: DailyRecruitmentDigest? = null,
    onSelectItem: (RecruitmentEntity) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onApply: (String) -> Unit
) {
    if (feedState.allActiveVacancies.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.WorkOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "No Active Vacancies Matching Filters",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Expired & closed vacancies are automatically excluded by our hard validation rules.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Daily Recruitment Digest Banner
        if (dailyDigest != null) {
            item {
                DailyDigestHeroBanner(
                    digest = dailyDigest,
                    onOpenCategory = {}
                )
            }
        }

        // Section 1: Latest For You (Personalized for user target exam)
        if (feedState.latestForYouVacancies.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎯", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Latest For You (${feedState.userTargetExam})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(feedState.latestForYouVacancies) { vacancy ->
                VacancyCard(
                    item = vacancy,
                    onOpen = { onSelectItem(vacancy) },
                    onToggleSave = { onToggleSave(vacancy.id, !vacancy.isSaved) },
                    onApply = { onApply(vacancy.applicationUrl.ifBlank { vacancy.sourceUrl }) }
                )
            }
        }

        // Section 2: Other Vacancies & State Jobs
        if (feedState.otherStateVacancies.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌍", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (feedState.selectedState != "All India") "Other Vacancies (${feedState.selectedState})" else "Other Central & State Vacancies",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(feedState.otherStateVacancies) { vacancy ->
                VacancyCard(
                    item = vacancy,
                    onOpen = { onSelectItem(vacancy) },
                    onToggleSave = { onToggleSave(vacancy.id, !vacancy.isSaved) },
                    onApply = { onApply(vacancy.applicationUrl.ifBlank { vacancy.sourceUrl }) }
                )
            }
        }
    }
}

@Composable
fun VacancyCard(
    item: RecruitmentEntity,
    onOpen: () -> Unit,
    onToggleSave: () -> Unit,
    onApply: () -> Unit
) {
    val computedStatus = item.getComputedStatus()
    val isDark = isAppInDarkTheme()
    val isClosingSoon = item.isClosingSoon()
    val isClosingToday = item.isClosingToday()
    val elig = item.getEligibilityStatusEnum()

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isClosingToday) Color(0xFFEF4444).copy(alpha = 0.6f)
            else if (isClosingSoon) Color(0xFFF59E0B).copy(alpha = 0.5f)
            else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        ),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vacancy_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Org & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            item.state,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        item.organization,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (item.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (item.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Recruitment Title
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Why recommended / Grounded explanation
            if (item.whyRecommended.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 10.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        item.whyRecommended,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Key Quick Facts (Vacancies, Salary, Qualification, Eligibility)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.totalVacancies != null && item.totalVacancies > 0) {
                    QuickFactBadge(
                        icon = "👥",
                        label = "${item.totalVacancies} Posts",
                        containerColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
                        contentColor = Color(0xFF2563EB)
                    )
                }

                // Eligibility Badge
                val eligColor = parseHexColor(elig.colorHex)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = eligColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        elig.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = eligColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Application Status Badge if tracked
                if (item.applicationStatus != UserApplicationStatus.NONE.name) {
                    val appStatus = item.getApplicationStatusEnum()
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                    ) {
                        Text(
                            "📌 ${appStatus.label}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Deadline & Deterministic Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val deadlineLabel = RecruitmentDateLogic.formatDeadlineDisplay(item.applicationLastDate)
                    Text(
                        deadlineLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isClosingToday || isClosingSoon) FontWeight.Bold else FontWeight.Medium,
                        color = if (isClosingToday) Color(0xFFEF4444) else if (isClosingSoon) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.isDeadlineExtended && item.previousLastDate != null) {
                        Text(
                            "Extended from ${item.previousLastDate}",
                            fontSize = 10.sp,
                            color = Color(0xFF8B5CF6),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onOpen,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Details", fontSize = 12.sp)
                    }

                    if (computedStatus.isApplyActive && item.applicationUrl.isNotBlank()) {
                        Button(
                            onClick = onApply,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsTabContent(
    results: List<RecruitmentEntity>,
    onSelectItem: (RecruitmentEntity) -> Unit,
    onOpenResult: (String) -> Unit,
    onToggleSave: (String, Boolean) -> Unit
) {
    if (results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Results found for current filters", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(results) { res ->
            ResultCard(
                item = res,
                onOpen = { onSelectItem(res) },
                onViewResult = { onOpenResult(res.applicationUrl.ifBlank { res.sourceUrl }) },
                onToggleSave = { onToggleSave(res.id, !res.isSaved) }
            )
        }
    }
}

@Composable
fun ResultCard(
    item: RecruitmentEntity,
    onOpen: () -> Unit,
    onViewResult: () -> Unit,
    onToggleSave: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1E293B) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        item.resultType ?: "Result Declared",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    item.resultDate ?: "",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                item.organization,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onViewResult,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Check Result & Merit List", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdmitCardsTabContent(
    admitCards: List<RecruitmentEntity>,
    onSelectItem: (RecruitmentEntity) -> Unit,
    onDownload: (String) -> Unit,
    onToggleSave: (String, Boolean) -> Unit
) {
    if (admitCards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Admit Cards released yet for selected exam", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(admitCards) { card ->
            AdmitCardItemView(
                item = card,
                onOpen = { onSelectItem(card) },
                onDownload = { onDownload(card.applicationUrl.ifBlank { card.sourceUrl }) }
            )
        }
    }
}

@Composable
fun AdmitCardItemView(
    item: RecruitmentEntity,
    onOpen: () -> Unit,
    onDownload: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1E293B) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF3B82F6).copy(alpha = 0.15f)
                ) {
                    Text(
                        item.admitCardStatus ?: "Admit Card Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (item.examDate != null) {
                    Text(
                        "Exam: ${item.examDate}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                item.organization,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDownload,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Download Hall Ticket", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NotificationsTabContent(
    notifications: List<RecruitmentEntity>,
    onSelectItem: (RecruitmentEntity) -> Unit,
    onOpenNotice: (String) -> Unit,
    onToggleSave: (String, Boolean) -> Unit
) {
    if (notifications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No recent official notices found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(notifications) { notice ->
            NotificationCard(
                item = notice,
                onOpen = { onSelectItem(notice) },
                onOpenNotice = { onOpenNotice(notice.officialPdfUrl.ifBlank { notice.sourceUrl }) }
            )
        }
    }
}

@Composable
fun NotificationCard(
    item: RecruitmentEntity,
    onOpen: () -> Unit,
    onOpenNotice: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1E293B) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isCorrectionNotice) Color(0xFF8B5CF6).copy(alpha = 0.5f)
            else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.isCorrectionNotice) Color(0xFF8B5CF6).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                ) {
                    Text(
                        if (item.isCorrectionNotice) "Corrigendum / Correction" else "Official Notice",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isCorrectionNotice) Color(0xFF7C3AED) else Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    item.organization,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.correctionDetails.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    item.correctionDetails,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onOpenNotice,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Official PDF", fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun SavedAndTrackerTabContent(
    savedItems: List<RecruitmentEntity>,
    trackedApplications: List<RecruitmentEntity>,
    onSelectItem: (RecruitmentEntity) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onSetReminder: (String, Boolean, Int) -> Unit
) {
    if (savedItems.isEmpty() && trackedApplications.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Your Watchlist & Tracker is Empty",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Save vacancies to receive deadline reminders and manage your application numbers, roll numbers, and stages privately.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section: Active Applications Tracker
        if (trackedApplications.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Application Tracker (${trackedApplications.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(trackedApplications) { app ->
                TrackedApplicationCard(
                    item = app,
                    onOpen = { onSelectItem(app) }
                )
            }
        }

        // Section: Saved Watchlist
        if (savedItems.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Watchlist & Deadline Reminders (${savedItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(savedItems) { item ->
                SavedItemCard(
                    item = item,
                    onOpen = { onSelectItem(item) },
                    onToggleSave = { onToggleSave(item.id, false) },
                    onSetReminder = { enabled, days -> onSetReminder(item.id, enabled, days) }
                )
            }
        }
    }
}

@Composable
fun TrackedApplicationCard(
    item: RecruitmentEntity,
    onOpen: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val appStatus = item.getApplicationStatusEnum()

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1E293B) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                ) {
                    Text(
                        "Status: ${appStatus.label}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    item.organization,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (item.userApplicationNumber.isNotBlank() || item.userRollNumber.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (item.userApplicationNumber.isNotBlank()) {
                        Text(
                            "App No: ${item.userApplicationNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (item.userRollNumber.isNotBlank()) {
                        Text(
                            "Roll No: ${item.userRollNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedItemCard(
    item: RecruitmentEntity,
    onOpen: () -> Unit,
    onToggleSave: () -> Unit,
    onSetReminder: (Boolean, Int) -> Unit
) {
    val isDark = isAppInDarkTheme()
    val daysRemaining = RecruitmentDateLogic.calculateDaysRemaining(item.applicationLastDate)

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1E293B) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.organization,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onToggleSave, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Bookmark, contentDescription = "Unsave", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            // Deadline & Reminder Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val deadlineText = if (daysRemaining != null) {
                    if (daysRemaining > 0) "⏳ $daysRemaining days left" else if (daysRemaining == 0) "🚨 Last day today" else "Closed"
                } else "Last Date: ${item.applicationLastDate ?: "Notice"}"

                Text(
                    deadlineText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (daysRemaining != null && daysRemaining <= 3) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Remind me", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = item.hasDeadlineReminder,
                        onCheckedChange = { enabled -> onSetReminder(enabled, 3) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickFactBadge(icon: String, label: String, containerColor: Color, contentColor: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecruitmentDetailDialog(
    item: RecruitmentEntity,
    userProfile: UserRecruitmentProfile,
    onDismiss: () -> Unit,
    onToggleSave: () -> Unit,
    onSetReminder: (Boolean, Int) -> Unit,
    onUpdateApplicationStatus: (UserApplicationStatus, String, String, String, String) -> Unit,
    onUpdateDocumentsReady: (List<String>) -> Unit,
    onUpdateChecklistChecked: (List<String>) -> Unit,
    onSubmitReport: (String, String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    var selectedLanguageHi by remember { mutableStateOf(true) }
    var showTrackerEditor by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()
    val computedStatus = item.getComputedStatus()
    val eligEnum = item.getEligibilityStatusEnum()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                item.postName.ifBlank { item.title },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                item.organization,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Outlined.Flag, contentDescription = "Report Issue", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onToggleSave) {
                            Icon(
                                if (item.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save Watchlist",
                                tint = if (item.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTrackerEditor = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Track App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (computedStatus.isApplyActive && item.applicationUrl.isNotBlank()) {
                            Button(
                                onClick = { onOpenUrl(item.applicationUrl) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                            ) {
                                Text("Apply Online", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        } else if (item.officialSourceUrl.isNotBlank()) {
                            Button(
                                onClick = { onOpenUrl(item.officialSourceUrl) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                            ) {
                                Text("Official Portal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 1. SMART ELIGIBILITY MATCH CARD
                val eligDetailColor = parseHexColor(eligEnum.colorHex)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = eligDetailColor.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, eligDetailColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = eligDetailColor, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Eligibility Match: ${eligEnum.label}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = eligDetailColor
                                )
                            }
                            Text(
                                "Profile Match",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (item.eligibilityExplanation.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                item.eligibilityExplanation,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 2. Grounded AI Summary Card ("In Simple Words")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (selectedLanguageHi) "हिंदी में आसान जानकारी" else "In Simple Words",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Language Toggle
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Row(modifier = Modifier.padding(2.dp)) {
                                    Surface(
                                        onClick = { selectedLanguageHi = false },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (!selectedLanguageHi) MaterialTheme.colorScheme.primary else Color.Transparent
                                    ) {
                                        Text(
                                            "EN",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!selectedLanguageHi) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        onClick = { selectedLanguageHi = true },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selectedLanguageHi) MaterialTheme.colorScheme.primary else Color.Transparent
                                    ) {
                                        Text(
                                            "हिन्दी",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedLanguageHi) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val summaryText = if (selectedLanguageHi && item.summaryHi.isNotBlank()) item.summaryHi else item.summaryEn
                        Text(
                            summaryText.ifBlank { "Verified recruitment notification directly summarized from official recruitment documents." },
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 3. INTERACTIVE RECRUITMENT TIMELINE
                DetailSectionCard(title = "⏳ Interactive Timeline") {
                    val events = remember(item) {
                        listOf(
                            Triple("Official Advertisement Published", item.applicationStartDate ?: "Released", "COMPLETED"),
                            Triple("Application Window", "${item.applicationStartDate ?: "Open"} to ${item.applicationLastDate ?: "Cutoff"}", if (computedStatus.isApplyActive) "ACTIVE" else "COMPLETED"),
                            Triple("Correction Window", item.correctionDate ?: "Post Application", "UPCOMING"),
                            Triple("Admit Card & City Intimation", item.admitCardDate ?: "7-10 Days before exam", if (item.admitCardStatus != null) "COMPLETED" else "UPCOMING"),
                            Triple("Examination / CBT", item.examDate ?: "As per official schedule", "UPCOMING"),
                            Triple("Result Declaration & Scorecard", item.resultDate ?: "Post Examination", if (item.resultDate != null) "COMPLETED" else "UPCOMING")
                        )
                    }

                    events.forEachIndexed { idx, (stage, dateStr, status) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when (status) {
                                    "COMPLETED" -> Color(0xFF10B981)
                                    "ACTIVE" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (status == "COMPLETED") {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stage, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (status) {
                                    "COMPLETED" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    "ACTIVE" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ) {
                                Text(
                                    status,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (status) {
                                        "COMPLETED" -> Color(0xFF059669)
                                        "ACTIVE" -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 4. Important Dates Card
                DetailSectionCard(title = "📅 Important Dates & Deadlines") {
                    DateRow("Application Start Date", item.applicationStartDate ?: "Not specified")
                    DateRow("Application Last Date", item.applicationLastDate ?: "Not specified", isHighlight = true)
                    if (item.previousLastDate != null) {
                        DateRow("Original Last Date", item.previousLastDate)
                    }
                    if (item.correctionDate != null) {
                        DateRow("Correction Window", item.correctionDate)
                    }
                    if (item.examDate != null) {
                        DateRow("Exam Date", item.examDate)
                    }
                    if (item.admitCardDate != null) {
                        DateRow("Admit Card Date", item.admitCardDate)
                    }
                    if (item.resultDate != null) {
                        DateRow("Result Declaration", item.resultDate)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 5. Vacancy & Post Breakdown
                DetailSectionCard(title = "👥 Vacancy & Post Breakdown") {
                    DateRow("Post Name", item.postName)
                    DateRow("Total Vacancies", if (item.totalVacancies != null && item.totalVacancies > 0) "${item.totalVacancies} Posts" else "Not specified")
                    DateRow("Job Location / State", item.state)
                    DateRow("Salary / Pay Scale", item.salary)
                }

                Spacer(Modifier.height(14.dp))

                // 6. Eligibility & Age Criteria
                DetailSectionCard(title = "🎓 Eligibility & Age Criteria") {
                    DateRow("Educational Qualification", item.educationalQualification)
                    val ageRange = if (item.ageMin != null && item.ageMax != null) "${item.ageMin} to ${item.ageMax} Years" else "Not specified"
                    DateRow("Age Limit", ageRange)
                    DateRow("Age Relaxation", item.ageRelaxation)
                    DateRow("Experience Requirement", item.experienceRequired)
                }

                Spacer(Modifier.height(14.dp))

                // 7. Application Fee Details
                DetailSectionCard(title = "💳 Application Fee & Concessions") {
                    DateRow("Fee Structure", item.feeDetails)
                }

                Spacer(Modifier.height(14.dp))

                // 8. Selection Process Steps
                if (item.selectionProcess.isNotEmpty()) {
                    DetailSectionCard(title = "📝 Selection Process") {
                        item.selectionProcess.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(step, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // 9. Required Documents & Application Checklist
                val allDocs = if (item.documentsRequired.isNotEmpty()) item.documentsRequired else listOf(
                    "10th Marksheet (Date of Birth Proof)",
                    "Educational Certificates & Marksheets",
                    "Aadhaar Card / Photo ID",
                    "Recent Passport Size Photograph",
                    "Scanned Signature",
                    "Category (OBC/SC/ST/EWS) Certificate"
                )

                DetailSectionCard(title = "📁 Document Preparation Checklist") {
                    allDocs.forEach { doc ->
                        val isChecked = item.documentsReadyList.contains(doc)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = if (isChecked) item.documentsReadyList - doc else item.documentsReadyList + doc
                                    onUpdateDocumentsReady(updated)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val updated = if (checked) item.documentsReadyList + doc else item.documentsReadyList - doc
                                    onUpdateDocumentsReady(updated)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(doc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 10. Action Guidance: "What Should I Do?"
                if (item.whatShouldIDo.isNotEmpty()) {
                    DetailSectionCard(title = "💡 What Should You Do Now?") {
                        item.whatShouldIDo.forEach { action ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text(action, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // 11. Source Attribution & Official Verification
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Official Source Attribution & Trust",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Primary Discovery: ${item.sourceUrl}\nOfficial Portal: ${item.officialSourceUrl.ifBlank { "Verified Official Board Portal" }}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // Application Tracker Edit Dialog
    if (showTrackerEditor) {
        ApplicationTrackerEditorDialog(
            item = item,
            onDismiss = { showTrackerEditor = false },
            onSave = { status, appNo, rollNo, post, notes ->
                onUpdateApplicationStatus(status, appNo, rollNo, post, notes)
                showTrackerEditor = false
            }
        )
    }

    // Report / Feedback Dialog
    if (showReportDialog) {
        RecruitmentReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { category, comment ->
                onSubmitReport(category, comment)
                showReportDialog = false
            }
        )
    }
}

@Composable
fun ApplicationTrackerEditorDialog(
    item: RecruitmentEntity,
    onDismiss: () -> Unit,
    onSave: (UserApplicationStatus, String, String, String, String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(item.getApplicationStatusEnum()) }
    var appNo by remember { mutableStateOf(item.userApplicationNumber) }
    var rollNo by remember { mutableStateOf(item.userRollNumber) }
    var postName by remember { mutableStateOf(item.userAppliedPost.ifBlank { item.postName }) }
    var notes by remember { mutableStateOf(item.userNotes) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Private Application Tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "All application data is securely kept on your device.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                Text("Application Status", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                UserApplicationStatus.values().forEach { st ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = st }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedStatus == st, onClick = { selectedStatus = st })
                        Spacer(Modifier.width(8.dp))
                        Text(st.label, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = appNo,
                    onValueChange = { appNo = it },
                    label = { Text("Application / Registration Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rollNo,
                    onValueChange = { rollNo = it },
                    label = { Text("Roll Number (if allotted)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Personal Notes / Exam Center") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(selectedStatus, appNo, rollNo, postName, notes) }) {
                        Text("Save Status")
                    }
                }
            }
        }
    }
}

@Composable
fun UserRecruitmentProfileDialog(
    currentProfile: UserRecruitmentProfile,
    onDismiss: () -> Unit,
    onSave: (UserRecruitmentProfile) -> Unit
) {
    var exam by remember { mutableStateOf(currentProfile.selectedExam) }
    var state by remember { mutableStateOf(currentProfile.state) }
    var qualification by remember { mutableStateOf(currentProfile.educationQualification) }
    var ageStr by remember { mutableStateOf(currentProfile.age?.toString() ?: "22") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Recruitment Match Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Used to calculate smart eligibility & relevance scores.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = exam,
                    onValueChange = { exam = it },
                    label = { Text("Target Exam (e.g. Railway, SSC, Banking, Police)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Text("Highest Qualification", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                EDUCATION_QUALIFICATIONS.forEach { q ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { qualification = q }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = qualification == q, onClick = { qualification = q })
                        Spacer(Modifier.width(6.dp))
                        Text(q, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = ageStr,
                    onValueChange = { ageStr = it },
                    label = { Text("Your Age (Years)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val parsedAge = ageStr.toIntOrNull() ?: 22
                        onSave(
                            currentProfile.copy(
                                selectedExam = exam,
                                state = state,
                                educationQualification = qualification,
                                age = parsedAge
                            )
                        )
                    }) {
                        Text("Save & Apply")
                    }
                }
            }
        }
    }
}

@Composable
fun FindJobsForMeDialog(
    currentProfile: UserRecruitmentProfile,
    onDismiss: () -> Unit,
    onSearch: (String?, String?, String?, Int?) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All Sectors") }
    var selectedState by remember { mutableStateOf("All India") }
    var selectedQual by remember { mutableStateOf(currentProfile.educationQualification) }
    var ageStr by remember { mutableStateOf(currentProfile.age?.toString() ?: "23") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔍", fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Find Jobs For Me", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Smart multi-criteria vacancy matcher.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                Text("Select Sector", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(listOf("All Sectors", "Railway", "SSC", "Banking", "Defence", "Police", "Teaching")) { sec ->
                        FilterChip(
                            selected = selectedCategory == sec,
                            onClick = { selectedCategory = sec },
                            label = { Text(sec, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Select Qualification", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                EDUCATION_QUALIFICATIONS.take(4).forEach { q ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedQual = q }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedQual == q, onClick = { selectedQual = q })
                        Spacer(Modifier.width(6.dp))
                        Text(q, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val parsedAge = ageStr.toIntOrNull()
                        onSearch(
                            if (selectedCategory == "All Sectors") null else selectedCategory,
                            if (selectedState == "All India") null else selectedState,
                            selectedQual,
                            parsedAge
                        )
                    }) {
                        Text("Match Vacancies")
                    }
                }
            }
        }
    }
}

@Composable
fun RecruitmentReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf("Date / Deadline Correction") }
    var comment by remember { mutableStateOf("") }

    val categories = listOf(
        "Date / Deadline Correction",
        "Result / Cutoff Updated",
        "Link Not Working / Changed",
        "Eligibility Details Discrepancy"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Report Update or Correction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = cat }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedType == cat, onClick = { selectedType = cat })
                        Spacer(Modifier.width(6.dp))
                        Text(cat, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Details / Official Notice Link") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSubmit(selectedType, comment) }) {
                        Text("Submit Report")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val isDark = isAppInDarkTheme()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1E293B) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun DateRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
    }
}

fun openExternalBrowser(context: android.content.Context, url: String) {
    if (url.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open link: $url", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun DailyDigestHeroBanner(
    digest: DailyRecruitmentDigest?,
    onOpenCategory: (String) -> Unit
) {
    if (digest == null) return
    val isDark = isAppInDarkTheme()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color(0xFFBFDBFE)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("☀️", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${digest.greeting} • ${digest.dateStr}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        "Daily Intelligence",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                digest.summaryEn,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (digest.newMatchesCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${digest.newMatchesCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text("New Matches", fontSize = 10.sp, color = Color(0xFF10B981))
                        }
                    }
                }

                if (digest.closingSoonCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${digest.closingSoonCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            Text("Closing Soon", fontSize = 10.sp, color = Color(0xFFEF4444))
                        }
                    }
                }

                if (digest.resultsCount > 0 || digest.admitCardsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${digest.resultsCount + digest.admitCardsCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                            Text("Updates Out", fontSize = 10.sp, color = Color(0xFF8B5CF6))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertsTabContent(
    outboxItems: List<RecruitmentOutboxItem>,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onActionClick: (RecruitmentOutboxItem) -> Unit
) {
    val isDark = isAppInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Smart Verified Alerts (${outboxItems.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (outboxItems.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onMarkAllAsRead) {
                        Text("Mark all read", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onClearAll) {
                        Text("Clear", fontSize = 12.sp, color = Color(0xFFEF4444))
                    }
                }
            }
        }

        if (outboxItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔔", fontSize = 36.sp)
                    Text(
                        "No Alerts Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Verified deadline extensions, closing alerts, admit cards, and results will appear here in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(outboxItems, key = { it.id }) { item ->
                    val isCritical = item.priority == NotificationPriority.CRITICAL.name
                    val isHigh = item.priority == NotificationPriority.HIGH.name

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (item.isRead) {
                            if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9)
                        } else {
                            if (isDark) Color(0xFF1E293B) else Color.White
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCritical) Color(0xFFEF4444).copy(alpha = 0.6f)
                            else if (isHigh) Color(0xFF3B82F6).copy(alpha = 0.5f)
                            else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActionClick(item) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCritical) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            if (isCritical) "CRITICAL" else if (isHigh) "HIGH" else "NORMAL",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCritical) Color(0xFFEF4444) else Color(0xFF3B82F6)
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        item.targetExamCategory.ifBlank { "All India" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onDeleteItem(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Delete Alert", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                item.titleEn,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                item.messageEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Verified Source • sarkariresult.com",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color(0xFF10B981)
                                )

                                Button(
                                    onClick = { onActionClick(item) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(item.actionText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
fun RecruitmentNotificationSettingsDialog(
    currentSettings: RecruitmentNotificationSettings,
    onDismiss: () -> Unit,
    onSave: (RecruitmentNotificationSettings) -> Unit,
    onUnmuteCategory: (String) -> Unit,
    onUnmuteRecruitment: (String) -> Unit
) {
    var masterEnabled by remember { mutableStateOf(currentSettings.recruitmentNotificationsEnabled) }
    var newVacancies by remember { mutableStateOf(currentSettings.newVacanciesEnabled) }
    var results by remember { mutableStateOf(currentSettings.resultsEnabled) }
    var admitCards by remember { mutableStateOf(currentSettings.admitCardsEnabled) }
    var deadlineAlerts by remember { mutableStateOf(currentSettings.deadlineAlertsEnabled) }
    var examChanges by remember { mutableStateOf(currentSettings.examChangesEnabled) }
    var quietHoursEnabled by remember { mutableStateOf(currentSettings.quietHoursEnabled) }
    var selectedDigestMode by remember { mutableStateOf(currentSettings.digestMode) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Notification Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Smart anti-spam & verified delivery", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Master Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recruitment Alerts", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Receive verified job updates", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = masterEnabled,
                            onCheckedChange = { masterEnabled = it }
                        )
                    }
                }

                if (masterEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Text("ALERT CATEGORIES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))

                    SettingSwitchRow("New Vacancies", "Verified jobs matching target exam", newVacancies) { newVacancies = it }
                    SettingSwitchRow("Admit Cards & City Slips", "Hall ticket downloads & exam slips", admitCards) { admitCards = it }
                    SettingSwitchRow("Results & Merit Lists", "Scorecards, cutoffs & answer keys", results) { results = it }
                    SettingSwitchRow("Deadline Closing Reminders", "Closing soon (3 days, 1 day & today)", deadlineAlerts) { deadlineAlerts = it }
                    SettingSwitchRow("Exam Date & Deadline Extensions", "Corrigendums & official date extensions", examChanges) { examChanges = it }

                    Spacer(Modifier.height(12.dp))
                    Text("DELIVERY MODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))

                    DigestMode.values().forEach { mode ->
                        val isSel = selectedDigestMode == mode.name
                        Surface(
                            onClick = { selectedDigestMode = mode.name },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mode.label, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp)
                                    Text(mode.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RadioButton(selected = isSel, onClick = { selectedDigestMode = mode.name })
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("QUIET HOURS (10:00 PM – 07:00 AM)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    SettingSwitchRow("Enable Quiet Hours", "Suppress non-critical alerts at night", quietHoursEnabled) { quietHoursEnabled = it }

                    if (currentSettings.mutedCategories.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("MUTED CATEGORIES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        Spacer(Modifier.height(6.dp))
                        currentSettings.mutedCategories.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat, fontSize = 12.sp)
                                TextButton(onClick = { onUnmuteCategory(cat) }) {
                                    Text("Unmute", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                currentSettings.copy(
                                    recruitmentNotificationsEnabled = masterEnabled,
                                    newVacanciesEnabled = newVacancies,
                                    resultsEnabled = results,
                                    admitCardsEnabled = admitCards,
                                    deadlineAlertsEnabled = deadlineAlerts,
                                    examChangesEnabled = examChanges,
                                    quietHoursEnabled = quietHoursEnabled,
                                    digestMode = selectedDigestMode
                                )
                            )
                        }
                    ) {
                        Text("Save Rules")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
fun AdminDiagnosticsDialog(
    diagnostics: AdminRecruitmentDiagnostics,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("System Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Recruitment Intelligence Platform 3.0", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Source Health
                Text("SOURCE DISCOVERY & BOARD HEALTH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                diagnostics.sourceHealthList.forEach { src ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (src.isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(src.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text(src.url, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("HTTP ${src.httpStatus}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (src.httpStatus == 200) Color(0xFF10B981) else Color(0xFFEF4444))
                                Text("${src.latencyMs}ms", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // API & Cost Optimization Metrics
                Text("AI & EXTRACTION PIPELINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox("Serper API", "${diagnostics.serperSuccess}/${diagnostics.serperRequests}", "${diagnostics.serperLatencyMs}ms", Color(0xFF3B82F6), Modifier.weight(1f))
                    MetricBox("Gemini AI", "${diagnostics.geminiSuccess}/${diagnostics.geminiRequests}", "${diagnostics.geminiLatencyMs}ms", Color(0xFF8B5CF6), Modifier.weight(1f))
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox("Deduplication", "${diagnostics.duplicateCandidatesResolved} Merged", "100% Unique", Color(0xFF10B981), Modifier.weight(1f))
                    MetricBox("Outbox Sent", "${diagnostics.outboxDeliveredCount}/${diagnostics.outboxTotalGenerated}", "${diagnostics.outboxSuppressedCount} Suppressed", Color(0xFFF59E0B), Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Diagnostics")
                }
            }
        }
    }
}

@Composable
fun MetricBox(title: String, stat: String, subtext: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            Text(stat, fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(subtext, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


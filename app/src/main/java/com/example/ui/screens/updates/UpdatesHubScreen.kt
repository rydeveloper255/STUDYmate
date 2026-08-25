package com.example.ui.screens.updates

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.model.*
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StreakBadge
import com.example.ui.components.springClickable
import com.example.ui.screens.intelligence.LiveExamIntelligenceScreen
import com.example.ui.screens.notification.NotificationCenterScreen
import com.example.ui.screens.vacancy.SmartVacancyScreen
import com.example.ui.theme.*

enum class UpdatesTabType(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val initialSubTab: String) {
    VACANCIES("Vacancies", Icons.Filled.WorkOutline, "VACANCY"),
    RESULTS("Results", Icons.Filled.EmojiEvents, "RESULT"),
    ADMIT_CARDS("Admit Cards", Icons.Filled.ConfirmationNumber, "ADMIT_CARD"),
    CURRENT_AFFAIRS("Current Affairs", Icons.Filled.Article, "CA"),
    NOTIFICATIONS("Alerts", Icons.Filled.NotificationsActive, "ALERTS")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesHubScreen(
    // Recruitment State
    recruitmentFeedState: RecruitmentFeedState,
    isRefreshingRecruitment: Boolean,
    onRefreshRecruitment: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onStateSelected: (String) -> Unit,
    onTabSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSortOptionSelected: (RecruitmentSortOption) -> Unit,
    onToggleSaveRecruitment: (String, Boolean) -> Unit,
    onSetReminder: (String, Boolean, Int) -> Unit,
    selectedDetailItem: RecruitmentEntity?,
    onSelectDetailItem: (RecruitmentEntity?) -> Unit,
    onUpdateProfile: ((UserRecruitmentProfile) -> Unit)? = null,
    onUpdateApplicationStatus: ((String, UserApplicationStatus, String, String, String, String) -> Unit)? = null,
    onUpdateDocumentsReady: ((String, List<String>) -> Unit)? = null,
    onUpdateChecklistChecked: ((String, List<String>) -> Unit)? = null,
    onFindJobsForMe: ((String?, String?, String?, Int?) -> Unit)? = null,
    recruitmentNotificationSettings: RecruitmentNotificationSettings = RecruitmentNotificationSettings(),
    onUpdateNotificationSettings: ((RecruitmentNotificationSettings) -> Unit)? = null,
    recruitmentOutbox: List<RecruitmentOutboxItem> = emptyList(),
    recruitmentDailyDigest: DailyRecruitmentDigest? = null,
    recruitmentDiagnostics: AdminRecruitmentDiagnostics = AdminRecruitmentDiagnostics(),
    onMuteRecruitment: ((String) -> Unit)? = null,
    onUnmuteRecruitment: ((String) -> Unit)? = null,
    onMuteCategory: ((String) -> Unit)? = null,
    onUnmuteCategory: ((String) -> Unit)? = null,
    onMarkOutboxRead: ((String) -> Unit)? = null,
    onMarkAllOutboxRead: (() -> Unit)? = null,
    onDeleteOutboxItem: ((String) -> Unit)? = null,
    onClearAllOutbox: (() -> Unit)? = null,
    onNovaQuery: ((String) -> Unit)? = null,
    // Live Exam Intelligence / Current Affairs State
    liveExamFeedState: LiveExamFeedState,
    isRefreshingLiveExam: Boolean,
    onRefreshLiveExam: () -> Unit,
    onToggleSaveLiveUpdate: (String, Boolean) -> Unit,
    onToggleSaveTrending: (String, Boolean) -> Unit,
    onStartQuizForTopic: (String, String) -> Unit,
    onAskNovaAboutUpdate: (String) -> Unit,
    // Notifications State
    notifications: List<AppNotification> = emptyList(),
    onMarkNotificationAsRead: (String) -> Unit = {},
    onMarkAllNotificationsAsRead: () -> Unit = {},
    onDeleteNotification: (String) -> Unit = {},
    onClearAllNotifications: () -> Unit = {},
    onNavigateDeepLink: (String, String?) -> Unit = { _, _ -> },
    onOpenNotificationSettings: () -> Unit = {},
    initialTab: UpdatesTabType = UpdatesTabType.VACANCIES,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    var currentTab by rememberSaveable { mutableStateOf(initialTab.name) }
    val activeTab = runCatching { UpdatesTabType.valueOf(currentTab) }.getOrDefault(UpdatesTabType.VACANCIES)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundGradient(isDark))
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f),
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Updates Hub",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else DeepIndigo
                        )
                        Text(
                            text = "Vacancies • Results • Admit Cards • Current Affairs",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    val unreadAlerts = notifications.count { !it.isRead }
                    if (unreadAlerts > 0) {
                        Surface(
                            color = CoralRose,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                currentTab = UpdatesTabType.NOTIFICATIONS.name
                            }
                        ) {
                            Text(
                                text = "$unreadAlerts new",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Top Tabs / Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UpdatesTabType.entries.forEach { tab ->
                        val isSelected = activeTab == tab
                        Surface(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .springClickable(testTag = "updates_tab_${tab.name.lowercase()}") {
                                    currentTab = tab.name
                                    if (tab == UpdatesTabType.VACANCIES) onTabSelected("VACANCY")
                                    else if (tab == UpdatesTabType.RESULTS) onTabSelected("RESULT")
                                    else if (tab == UpdatesTabType.ADMIT_CARDS) onTabSelected("ADMIT_CARD")
                                },
                            color = if (isSelected) {
                                if (isDark) NeonCyan else DeepIndigo
                            } else {
                                if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        if (isDark) Color(0xFF070B19) else Color.White
                                    } else {
                                        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFF070B19) else Color.White
                                    } else {
                                        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                UpdatesTabType.VACANCIES,
                UpdatesTabType.RESULTS,
                UpdatesTabType.ADMIT_CARDS -> {
                    SmartVacancyScreen(
                        feedState = recruitmentFeedState,
                        isRefreshing = isRefreshingRecruitment,
                        onRefresh = onRefreshRecruitment,
                        onCategorySelected = onCategorySelected,
                        onStateSelected = onStateSelected,
                        onTabSelected = onTabSelected,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onSortOptionSelected = onSortOptionSelected,
                        onToggleSave = onToggleSaveRecruitment,
                        onSetReminder = onSetReminder,
                        selectedDetailItem = selectedDetailItem,
                        onSelectDetailItem = onSelectDetailItem,
                        onUpdateProfile = onUpdateProfile,
                        onUpdateApplicationStatus = onUpdateApplicationStatus,
                        onUpdateDocumentsReady = onUpdateDocumentsReady,
                        onUpdateChecklistChecked = onUpdateChecklistChecked,
                        onFindJobsForMe = onFindJobsForMe,
                        onSubmitReport = { _, _, _ ->
                            Toast.makeText(context, "Thank you. Your report has been recorded.", Toast.LENGTH_SHORT).show()
                        },
                        notificationSettings = recruitmentNotificationSettings,
                        onUpdateNotificationSettings = onUpdateNotificationSettings,
                        outboxItems = recruitmentOutbox,
                        dailyDigest = recruitmentDailyDigest,
                        diagnostics = recruitmentDiagnostics,
                        onMuteRecruitment = onMuteRecruitment,
                        onUnmuteRecruitment = onUnmuteRecruitment,
                        onMuteCategory = onMuteCategory,
                        onUnmuteCategory = onUnmuteCategory,
                        onMarkOutboxRead = onMarkOutboxRead,
                        onMarkAllOutboxRead = onMarkAllOutboxRead,
                        onDeleteOutboxItem = onDeleteOutboxItem,
                        onClearAllOutbox = onClearAllOutbox,
                        onNovaQuery = onNovaQuery,
                        onBack = { currentTab = UpdatesTabType.VACANCIES.name }
                    )
                }

                UpdatesTabType.CURRENT_AFFAIRS -> {
                    LiveExamIntelligenceScreen(
                        feedState = liveExamFeedState,
                        isRefreshing = isRefreshingLiveExam,
                        onRefresh = onRefreshLiveExam,
                        onToggleSaveUpdate = onToggleSaveLiveUpdate,
                        onToggleSaveTrending = onToggleSaveTrending,
                        onStartQuizForTopic = onStartQuizForTopic,
                        onAskNovaAboutUpdate = onAskNovaAboutUpdate,
                        onBack = { currentTab = UpdatesTabType.VACANCIES.name }
                    )
                }

                UpdatesTabType.NOTIFICATIONS -> {
                    NotificationCenterScreen(
                        notifications = notifications,
                        onMarkAsRead = onMarkNotificationAsRead,
                        onMarkAllAsRead = onMarkAllNotificationsAsRead,
                        onDeleteNotification = onDeleteNotification,
                        onClearAll = onClearAllNotifications,
                        onNavigateDeepLink = onNavigateDeepLink,
                        onOpenSettings = onOpenNotificationSettings,
                        onBack = { currentTab = UpdatesTabType.VACANCIES.name }
                    )
                }
            }
        }
    }
}

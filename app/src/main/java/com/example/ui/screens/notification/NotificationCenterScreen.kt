package com.example.ui.screens.notification

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.NotificationCategory
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    notifications: List<AppNotification>,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onClearAll: () -> Unit,
    onNavigateDeepLink: (deepLink: String, payload: String) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(enabled = true) {
        onBack()
    }

    var selectedCategory by remember { mutableStateOf<NotificationCategory?>(null) } // null = All

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    val filteredNotifications = remember(notifications, selectedCategory) {
        val now = System.currentTimeMillis()
        notifications
            .filter { notif -> notif.expiresAt == null || notif.expiresAt > now }
            .filter { notif -> selectedCategory == null || notif.category == selectedCategory }
            .sortedByDescending { it.timestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$unreadCount new",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF070B19)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = onMarkAllAsRead,
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Text("Mark All Read", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, "Settings", tint = Color(0xFF94A3B8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B1021)
                )
            )
        },
        containerColor = Color(0xFF070B19)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (${notifications.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color(0xFF070B19),
                            containerColor = Color(0x20FFFFFF),
                            labelColor = Color.White
                        )
                    )
                }

                items(NotificationCategory.values()) { category ->
                    val catCount = notifications.count { it.category == category }
                    val isSelected = selectedCategory == category

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = if (isSelected) null else category },
                        label = { Text("${category.displayName} ($catCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                getCategoryIcon(category),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) Color(0xFF070B19) else Color(0xFF94A3B8)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getCategoryColor(category),
                            selectedLabelColor = Color(0xFF070B19),
                            containerColor = Color(0x15FFFFFF),
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            // Notification List
            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x15FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.NotificationsOff, null, tint = Color(0xFF64748B), modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = "No Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (selectedCategory != null) "No updates in ${selectedCategory?.displayName}." else "You're all caught up! No recent study reminders or updates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { item ->
                        NotificationItemCard(
                            notification = item,
                            onMarkAsRead = { onMarkAsRead(item.id) },
                            onDelete = { onDeleteNotification(item.id) },
                            onActionClick = {
                                onMarkAsRead(item.id)
                                onNavigateDeepLink(item.deepLink, item.payload)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: AppNotification,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onActionClick: () -> Unit
) {
    val catColor = getCategoryColor(notification.category)
    val formattedTime = remember(notification.timestamp) {
        val diff = System.currentTimeMillis() - notification.timestamp
        when {
            diff < 60_000L -> "Just now"
            diff < 3600_000L -> "${diff / 60_000L}m ago"
            diff < 86400_000L -> "${diff / 3600_000L}h ago"
            else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(notification.timestamp))
        }
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onMarkAsRead()
                onActionClick()
            }
            .testTag("notification_item_${notification.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.18f))
                    .border(1.dp, catColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(notification.category),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Content Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = notification.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = catColor
                        )
                        Text("•", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                    }
                }

                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Action Button
                if (notification.actionText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(catColor.copy(alpha = 0.15f))
                            .clickable { onActionClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = notification.actionText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = catColor
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            null,
                            tint = catColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Dismiss Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Filled.Close, "Dismiss", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun getCategoryIcon(category: NotificationCategory): ImageVector {
    return when (category) {
        NotificationCategory.STUDY -> Icons.AutoMirrored.Filled.MenuBook
        NotificationCategory.TESTS -> Icons.Filled.Quiz
        NotificationCategory.VACANCY -> Icons.Filled.Work
        NotificationCategory.RESULTS -> Icons.Filled.EmojiEvents
        NotificationCategory.ADMIT_CARD -> Icons.Filled.Badge
        NotificationCategory.CURRENT_AFFAIRS -> Icons.Filled.Newspaper
        NotificationCategory.EXAM_UPDATES -> Icons.Filled.Verified
        NotificationCategory.NOVA -> Icons.Filled.AutoAwesome
        NotificationCategory.SYSTEM -> Icons.Filled.Info
    }
}

private fun getCategoryColor(category: NotificationCategory): Color {
    return when (category) {
        NotificationCategory.STUDY -> NeonCyan
        NotificationCategory.TESTS -> ElectricViolet
        NotificationCategory.VACANCY -> GoldenSpark
        NotificationCategory.RESULTS -> Color(0xFF00E676) // Green
        NotificationCategory.ADMIT_CARD -> Color(0xFFFF9100) // Amber Orange
        NotificationCategory.CURRENT_AFFAIRS -> GoldenSpark
        NotificationCategory.EXAM_UPDATES -> Color(0xFF00E676) // Bright Green
        NotificationCategory.NOVA -> Color(0xFFFF4081) // Neon Pink
        NotificationCategory.SYSTEM -> Color(0xFF90CAF9)
    }
}

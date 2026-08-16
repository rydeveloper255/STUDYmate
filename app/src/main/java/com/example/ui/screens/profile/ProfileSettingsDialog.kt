package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.NotificationPreference
import com.example.data.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.screens.focus.FocusShieldSettingsScreen
import com.example.ui.theme.*

@Composable
fun ProfileSettingsDialog(
    user: UserProfile?,
    isDarkTheme: Boolean,
    notificationPrefs: NotificationPreference,
    onToggleDarkTheme: (Boolean) -> Unit,
    onUpdateNotificationPrefs: (NotificationPreference) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFocusShieldSettings by remember { mutableStateOf(false) }

    if (showFocusShieldSettings) {
        Dialog(onDismissRequest = { showFocusShieldSettings = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                color = Color(0xFF070B19)
            ) {
                FocusShieldSettingsScreen(
                    onBack = { showFocusShieldSettings = false }
                )
            }
        }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .testTag("profile_settings_dialog"),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF111827),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x35FFFFFF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Profile & Settings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, "Close", tint = Color(0xFF94A3B8))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // User Identity Card
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            fillAlpha = 0.7f
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x3338BDF8))
                                        .border(1.5.dp, NeonCyan, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!user?.photoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = user?.photoUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(30.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = user?.name ?: "Student",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (user?.isGuest == true) "Guest Session (Local Data)" else (user?.email ?: "Google Account"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${user?.grade} • ${user?.goal}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Theme Settings
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x18FFFFFF))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.DarkMode, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "Dark Mode (Default)", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = onToggleDarkTheme,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF070B19), checkedTrackColor = NeonCyan)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Notification Preferences
                        Text(
                            text = "Notifications & Reminders",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NotificationRow(
                                title = "📚 Study Schedule Reminders",
                                checked = notificationPrefs.studyReminders,
                                onChecked = { onUpdateNotificationPrefs(notificationPrefs.copy(studyReminders = it)) }
                            )
                            NotificationRow(
                                title = "🛡️ Focus Shield Alerts",
                                checked = notificationPrefs.focusReminders,
                                onChecked = { onUpdateNotificationPrefs(notificationPrefs.copy(focusReminders = it)) }
                            )
                            NotificationRow(
                                title = "🚀 Exam Countdown Motivation",
                                checked = notificationPrefs.examCountdownAlerts,
                                onChecked = { onUpdateNotificationPrefs(notificationPrefs.copy(examCountdownAlerts = it)) }
                            )
                            NotificationRow(
                                title = "📊 Daily Progress Insights",
                                checked = notificationPrefs.weeklyReport,
                                onChecked = { onUpdateNotificationPrefs(notificationPrefs.copy(weeklyReport = it)) }
                            )
                            NotificationRow(
                                title = "🏆 Streak & Achievement Alerts",
                                checked = notificationPrefs.streakAlerts,
                                onChecked = { onUpdateNotificationPrefs(notificationPrefs.copy(streakAlerts = it)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quiet Hours Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x18FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x25FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.NightsStay, null, tint = GoldenSpark, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Quiet Hours", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("10:00 PM – 07:00 AM (Auto-silenced)", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Switch(
                                    checked = true,
                                    onCheckedChange = {},
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF070B19), checkedTrackColor = NeonCyan)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Permission Center & Privacy Settings
                        Text(
                            text = "Permissions & Privacy Center 🛡️",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PermissionRow(
                                title = "Notifications",
                                subtitle = "For study reminders & quiet hour notifications",
                                status = "Granted",
                                isGranted = true
                            )
                            PermissionRow(
                                title = "Focus Shield (Accessibility)",
                                subtitle = "Configure restricted apps & accessibility status",
                                status = "Active",
                                isGranted = true,
                                onClick = { showFocusShieldSettings = true }
                            )
                            PermissionRow(
                                title = "Camera Access",
                                subtitle = "Used exclusively for scanning math & chemistry questions",
                                status = "Granted",
                                isGranted = true
                            )
                            PermissionRow(
                                title = "Microphone Access",
                                subtitle = "Used for voice-activated AI study queries",
                                status = "Granted",
                                isGranted = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x1538BDF8)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🔒 Your Privacy First",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "StudyMate AI never sells your data, logs keystrokes, or reads private messages. All accessibility permissions are strictly bound to Focus Shield blocking.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Bottom Account Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onSignOut()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sign_out_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x35FFFFFF))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_account_button")
                        ) {
                            Text("Delete Account & Local Data", color = CoralRose, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = Color(0xFF131C2E),
                title = { Text("Delete Account?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("This will permanently clear all your study plans, mock tests, and progress.", color = Color(0xFFCBD5E1)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            onDismiss()
                            onDeleteAccount()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                    ) {
                        Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

@Composable
fun NotificationRow(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x18FFFFFF))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF070B19), checkedTrackColor = NeonCyan)
        )
    }
}

@Composable
fun PermissionRow(
    title: String,
    subtitle: String,
    status: String,
    isGranted: Boolean,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x18FFFFFF))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isGranted) EmeraldSuccess.copy(alpha = 0.2f) else CoralRose.copy(alpha = 0.2f)
        ) {
            Text(
                text = status,
                color = if (isGranted) EmeraldSuccess else CoralRose,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}


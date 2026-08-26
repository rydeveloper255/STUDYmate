package com.example.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.AppNavTab
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import java.util.Calendar

/**
 * REFACTOR: LIQUID GLASS HOME SECTIONS
 *
 * Implements the 100% video-accurate components:
 * 1. LiquidHomeHeader: Greeting + Flame Streak badge + Theme toggle + Notification bell.
 * 2. LiquidGlobalSearchBar: Frosted glass search bar ("Ask Nova about subjects, exams, or settings...").
 * 3. LiquidTodayProgressCard: Compact study stats (Time, Target %, XP).
 * 4. LiquidTodayMissionSection: Video-accurate list of actionable tasks (Checkbox + Title + Start/Read button).
 * 5. LiquidFourHubGrid: 2x2 dedicated Gateway Cards (Learn Hub, Practice Hub, Updates Hub, Settings & Tools).
 * 6. LiquidLatestUpdatesStrip: Horizontal scrolling urgent exam updates + "View All Updates" button.
 */

// =========================================================================
// 1. TOP HEADER (Greeting, Streak Badge, Theme Switcher, Notifications)
// =========================================================================
@Composable
fun LiquidHomeHeader(
    userName: String,
    streakDays: Int,
    unreadNotificationCount: Int,
    onOpenNotifications: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingTime = when (currentHour) {
        in 4..11 -> "Good morning,"
        in 12..16 -> "Good afternoon,"
        in 17..21 -> "Good evening,"
        else -> "Good night,"
    }

    val isDark = isAppInDarkTheme()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Greeting + User Name + Streak Capsule
        Column {
            Text(
                text = greetingTime,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = userName.ifBlank { "Rahul" },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Amber Flame Streak Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberAlert.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, AmberAlert.copy(alpha = 0.5f)),
                    modifier = Modifier.springClickable { onOpenProfile() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔥", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${streakDays.coerceAtLeast(1)}d",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AmberAlert,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        // Right Action Controls: Night mode pill toggle & Notification Bell with counter
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme Mode Pill Toggle
            Surface(
                shape = CircleShape,
                color = DarkSurface.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .size(40.dp)
                    .springClickable { onToggleTheme() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.NightlightRound else Icons.Default.WbSunny,
                        contentDescription = "Toggle Theme",
                        tint = if (isDark) PrimaryCyan else AmberAlert,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Notification Bell with unread counter
            Surface(
                shape = CircleShape,
                color = DarkSurface.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .size(40.dp)
                    .springClickable { onOpenNotifications() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (unreadNotificationCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = CrimsonRed,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .size(14.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. GLOBAL SEARCH BAR
// =========================================================================
@Composable
fun LiquidGlobalSearchBar(
    onClickSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .springClickable { onClickSearch() },
        shape = RoundedCornerShape(16.dp),
        borderColor = Color.White.copy(alpha = 0.1f),
        fillAlpha = 0.5f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = PrimaryCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Ask Nova about subjects, exams, or settings...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 13.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = PrimaryCyan.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 3. TODAY'S PROGRESS COMPACT CARD
// =========================================================================
@Composable
fun LiquidTodayProgressCard(
    studyMinutesToday: Int,
    targetMinutesToday: Int = 180,
    userXp: Int,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (targetMinutesToday > 0) {
        (studyMinutesToday.toFloat() / targetMinutesToday.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val studyHours = studyMinutesToday / 60
    val studyMins = studyMinutesToday % 60
    val targetHours = targetMinutesToday / 60

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        borderColor = Color.White.copy(alpha = 0.08f),
        fillAlpha = 0.45f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📊", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Today's Study Progress",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "⭐ $userXp XP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = PrimaryCyan,
                    trackColor = DarkSurface
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${studyHours}h ${studyMins}m / ${targetHours}h (${(progressFraction * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

// =========================================================================
// 4. TODAY'S MISSION SECTION (Actionable Tasks)
// =========================================================================
@Composable
fun LiquidTodayMissionSection(
    missions: List<DailyMissionTask>,
    onToggleMission: (String, Boolean) -> Unit,
    onStartAction: (actionType: String, subject: String, topic: String, minutes: Int) -> Unit,
    onOpenPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayMissions = if (missions.isNotEmpty()) {
        missions.take(3)
    } else {
        listOf(
            DailyMissionTask(
                id = "def_1",
                title = "Chemistry Revision - 45 min",
                subject = "Chemistry",
                topic = "Organic Reactions",
                targetMinutes = 45,
                actionType = "FOCUS"
            ),
            DailyMissionTask(
                id = "def_2",
                title = "Practice PYQ on Algebra",
                subject = "Mathematics",
                topic = "Algebra",
                targetMinutes = 30,
                actionType = "PRACTICE"
            ),
            DailyMissionTask(
                id = "def_3",
                title = "Read Daily Current Affairs",
                subject = "Current Affairs",
                topic = "National & Defence",
                targetMinutes = 20,
                actionType = "CURRENT_AFFAIRS"
            )
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header Row: Today's Mission ➔ | View Plan ➔
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.springClickable { onOpenPlan() }
            ) {
                Text(
                    text = "Today's Mission",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            TextButton(
                onClick = onOpenPlan,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "View Plan",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Mission Task Cards List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            displayMissions.forEach { task ->
                LiquidMissionTaskCard(
                    task = task,
                    onToggle = { isChecked -> onToggleMission(task.id, isChecked) },
                    onStart = {
                        onStartAction(task.actionType, task.subject, task.topic, task.targetMinutes)
                    }
                )
            }
        }
    }
}

@Composable
private fun LiquidMissionTaskCard(
    task: DailyMissionTask,
    onToggle: (Boolean) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderColor = if (task.isCompleted) EmeraldGreen.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
        fillAlpha = if (task.isCompleted) 0.35f else 0.5f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Checkbox Ring
            Surface(
                shape = CircleShape,
                color = if (task.isCompleted) EmeraldGreen else Color.Transparent,
                border = BorderStroke(
                    1.5.dp,
                    if (task.isCompleted) EmeraldGreen else PrimaryCyan.copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .size(24.dp)
                    .springClickable { onToggle(!task.isCompleted) }
            ) {
                if (task.isCompleted) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task Title & Duration
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) TextMuted else TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${task.subject} • ${task.targetMinutes}m target",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action Pill Button (Start / Read)
            if (!task.isCompleted) {
                val actionBtnText = when (task.actionType) {
                    "FOCUS" -> "Start"
                    "PRACTICE" -> "Practice"
                    "CURRENT_AFFAIRS" -> "Read"
                    else -> "Start"
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryCyan.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.springClickable { onStart() }
                ) {
                    Text(
                        text = actionBtnText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Done ✓",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 5. THE 4 DEDICATED MODULAR HUB GATEWAY CARDS (2x2 Grid)
// =========================================================================
@Composable
fun LiquidFourHubGrid(
    onNavigateToLearn: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Learn Hub & Practice Hub
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Blueprint / Learn Hub
            LiquidHubCard(
                title = "Blueprint Hub",
                subtitle = "Notes, syllabus & true study material",
                actionLabel = "Start",
                iconVector = Icons.Default.MenuBook,
                accentColor = PrimaryCyan,
                onAction = onNavigateToLearn,
                modifier = Modifier.weight(1f)
            )

            // 2. Practice Hub
            LiquidHubCard(
                title = "Practice Hub",
                subtitle = "Mock tests & weak topics, targeted practice",
                actionLabel = "Read",
                iconVector = Icons.Default.CheckCircleOutline,
                accentColor = EmeraldGreen,
                onAction = onNavigateToPractice,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Updates Hub & Settings & Tools
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3. Updates Hub
            LiquidHubCard(
                title = "Updates Hub",
                subtitle = "Government job vacancies, results & alerts",
                actionLabel = "Read",
                iconVector = Icons.Default.Campaign,
                accentColor = AmberAlert,
                onAction = onNavigateToUpdates,
                modifier = Modifier.weight(1f)
            )

            // 4. Settings & Tools
            LiquidHubCard(
                title = "Settings & Tools",
                subtitle = "App Shield, reminders & Nova AI tools",
                actionLabel = "Open",
                iconVector = Icons.Default.Settings,
                accentColor = ElectricViolet,
                onAction = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LiquidHubCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    iconVector: ImageVector,
    accentColor: Color,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .springClickable { onAction() },
        shape = RoundedCornerShape(20.dp),
        borderColor = Color.White.copy(alpha = 0.08f),
        fillAlpha = 0.5f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Icon + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Subtitle
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .springClickable { onAction() }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

// =========================================================================
// 6. LATEST UPDATES HORIZONTAL STRIP
// =========================================================================
@Composable
fun LiquidLatestUpdatesStrip(
    updates: List<LiveExamUpdateEntity>,
    onOpenUpdateDetail: (LiveExamUpdateEntity) -> Unit,
    onViewAllUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayUpdates = if (updates.isNotEmpty()) {
        updates.take(5)
    } else {
        listOf(
            LiveExamUpdateEntity(
                id = "up_1",
                examId = "rrb_alp_2026",
                examName = "RRB ALP Recruitment 2026",
                title = "Application Window Closing Soon - 18,799 Vacancies",
                summary = "Railway Recruitment Board is closing online registrations. Ensure application fees and zonal choices are finalized.",
                category = "RECRUITMENT",
                sourceName = "Official Portal",
                sourceUrl = "https://rrbcdg.gov.in",
                publishedAt = "Today, 10:00 AM",
                relevance = "URGENT"
            ),
            LiveExamUpdateEntity(
                id = "up_2",
                examId = "ssc_cgl_2026",
                examName = "SSC CGL Tier 1 Exam",
                title = "Official Shift Timing & City Intimation Released",
                summary = "Staff Selection Commission released official reporting shifts and examination venue details.",
                category = "ADMIT_CARD",
                sourceName = "SSC Official",
                sourceUrl = "https://ssc.gov.in",
                publishedAt = "Yesterday",
                relevance = "HIGH"
            )
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header Row: Latest Updates | View All Updates ➔
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Latest Updates",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PrimaryCyan.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.6f)),
                modifier = Modifier.springClickable { onViewAllUpdates() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View All Updates",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal Carousel of Update Cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(displayUpdates, key = { it.id }) { item ->
                LiquidUpdateMiniCard(
                    update = item,
                    onClick = { onOpenUpdateDetail(item) }
                )
            }
        }
    }
}

@Composable
private fun LiquidUpdateMiniCard(
    update: LiveExamUpdateEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .width(260.dp)
            .springClickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        borderColor = Color.White.copy(alpha = 0.08f),
        fillAlpha = 0.5f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeColor = when (update.relevance) {
                    "URGENT" -> CrimsonRed
                    "HIGH" -> AmberAlert
                    else -> PrimaryCyan
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = update.relevance,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = update.publishedAt.ifBlank { "Recently" },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = update.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = update.summary,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

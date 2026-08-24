package com.example.ui.screens.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

/**
 * 🔔 "What's New for You" Section for HomeScreen (Step 21)
 */
@Composable
fun WhatsNewForYouSection(
    feedState: LiveExamFeedState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenUpdateDetail: (LiveExamUpdateEntity) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("whats_new_section")
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔔",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "What's New for You",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Exam: ${feedState.examName} • ${feedState.whatsNewList.size} updates",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Refresh Icon
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp).testTag("refresh_live_exam_btn")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = NeonCyan
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh Updates",
                            tint = if (isDark) NeonCyan else DeepIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                TextButton(
                    onClick = onViewAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("view_all_live_updates_btn")
                ) {
                    Text(
                        text = "View Feed →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) NeonCyan else DeepIndigo
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (feedState.whatsNewList.isEmpty() && isRefreshing) {
            // Skeleton Loader
            LiveExamSkeletonLoader(isDark = isDark)
        } else if (feedState.whatsNewList.isEmpty()) {
            // Empty State
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏛️ Checking latest notices for ${feedState.examName}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) NeonCyan else DeepIndigo)
                    ) {
                        Text("Fetch Live Exam News", color = if (isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Compact Horizontal Carousel
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
            ) {
                items(feedState.whatsNewList, key = { it.id }) { update ->
                    CompactWhatsNewCard(
                        update = update,
                        isDark = isDark,
                        onClick = { onOpenUpdateDetail(update) },
                        onToggleSave = { onToggleSave(update.id, !update.isSaved) },
                        onOpenSource = { openUrlInBrowser(context, update.sourceUrl) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactWhatsNewCard(
    update: LiveExamUpdateEntity,
    isDark: Boolean,
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
        LiveExamCategory.OFFICIAL_NOTIFICATION.name -> "🟢 Official Notification"
        LiveExamCategory.EXAM_UPDATE.name -> "🔵 Exam Update"
        LiveExamCategory.CURRENT_AFFAIRS.name -> "🟠 Current Affairs"
        LiveExamCategory.SYLLABUS_PATTERN.name -> "🟣 Syllabus & Pattern"
        else -> "🔴 Urgent Notice"
    }

    GlassCard(
        modifier = Modifier
            .width(290.dp)
            .testTag("whats_new_card_${update.id}"),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (update.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save Update",
                        tint = if (update.isSaved) Color(0xFFF59E0B) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headline Title
            Text(
                text = update.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Concise 2-3 Line Summary
            Text(
                text = update.summary,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Source & Verification Meta
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (update.isVerifiedOfficial) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x2210B981)
                        ) {
                            Text(
                                text = "✓ Official",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = update.sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp),
                        fontSize = 10.sp
                    )
                }

                // Relevance level tag
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isDark) Color(0x2038BDF8) else Color(0x156366F1)
                ) {
                    Text(
                        text = if (update.relevance == ExamRelevanceLevel.HIGH.name) "🔥 High" else "⚡ Medium",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) NeonCyan else DeepIndigo,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions: [Read Details] & [Open Source]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f).height(30.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    border = BorderStroke(0.5.dp, if (isDark) Color(0x4038BDF8) else Color(0x406366F1))
                ) {
                    Text(
                        text = "Read Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) NeonCyan else DeepIndigo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (update.sourceUrl.isNotBlank()) {
                    FilledTonalButton(
                        onClick = onOpenSource,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isDark) Color(0x30FFFFFF) else Color(0x15000000)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = "Open Source",
                            tint = if (isDark) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🎯 "Exam Radar" Section for HomeScreen (Step 21)
 */
@Composable
fun ExamRadarSection(
    feedState: LiveExamFeedState,
    onViewFullRadar: () -> Unit,
    onOpenUpdateDetail: (LiveExamUpdateEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Pulse animation for Radar Beacon
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("exam_radar_card"),
        shape = RoundedCornerShape(20.dp),
        onClick = onViewFullRadar
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Animated Pulse Beacon
                    Box(
                        modifier = Modifier.size(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((14 * pulseScale).dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.3f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXAM RADAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0x2238BDF8) else Color(0x186366F1),
                    border = BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Live Pulse",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) NeonCyan else DeepIndigo,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Radar Status Headline
            Text(
                text = "${feedState.examName} Active Intelligence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Breakdown Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadarMetricChip(
                    icon = "🏛️",
                    label = "Official",
                    count = feedState.officialNotices.size,
                    isDark = isDark
                )
                RadarMetricChip(
                    icon = "📢",
                    label = "Updates",
                    count = feedState.radarUpdates.size,
                    isDark = isDark
                )
                RadarMetricChip(
                    icon = "🔥",
                    label = "Trending",
                    count = feedState.trendingTopics.size,
                    isDark = isDark
                )
                RadarMetricChip(
                    icon = "🔖",
                    label = "Saved",
                    count = feedState.savedUpdates.size,
                    isDark = isDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Top Priority Alert Banner (if available)
            val topAlert = feedState.radarUpdates.firstOrNull()
            if (topAlert != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .springClickable { onOpenUpdateDetail(topAlert) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0x301E293B) else Color(0x10000000),
                    border = BorderStroke(0.5.dp, if (isDark) Color(0x4038BDF8) else Color(0x306366F1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topAlert.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${topAlert.sourceName} • ${topAlert.publishedAt}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (isDark) NeonCyan else DeepIndigo,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // View Full Radar Feed Button
            Button(
                onClick = onViewFullRadar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("open_full_exam_radar_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) NeonCyan else DeepIndigo
                )
            ) {
                Text(
                    text = "Open Live Exam Radar & Feed →",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.Black else Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun RadarMetricChip(
    icon: String,
    label: String,
    count: Int,
    isDark: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$icon $count", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isDark) Color.White else Color(0xFF0F172A))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B), fontSize = 10.sp)
    }
}

/**
 * 🔥 "Trending Topics for My Exam" Section (Step 21)
 */
@Composable
fun TrendingExamTopicsSection(
    feedState: LiveExamFeedState,
    onTopicClick: (TrendingExamTopicEntity) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onStartQuizForTopic: (TrendingExamTopicEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (feedState.trendingTopics.isEmpty()) return
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trending_topics_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔥", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Trending for Your Exam",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = "High-yield developments testable in ${feedState.examName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
        ) {
            items(feedState.trendingTopics, key = { it.id }) { topic ->
                CompactTrendingTopicCard(
                    topic = topic,
                    isDark = isDark,
                    onClick = { onTopicClick(topic) },
                    onToggleSave = { onToggleSave(topic.id, !topic.isSaved) },
                    onStartQuiz = { onStartQuizForTopic(topic) }
                )
            }
        }
    }
}

@Composable
private fun CompactTrendingTopicCard(
    topic: TrendingExamTopicEntity,
    isDark: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
    onStartQuiz: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .width(270.dp)
            .testTag("trending_topic_card_${topic.id}"),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
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
                        text = topic.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (topic.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save Topic",
                        tint = if (topic.isSaved) Color(0xFFF59E0B) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = topic.whyItMatters,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f).height(30.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Learn More",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) NeonCyan else DeepIndigo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalButton(
                    onClick = onStartQuiz,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "✍️ Quiz",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveExamSkeletonLoader(isDark: Boolean) {
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by shimmerTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(3) {
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background((if (isDark) Color(0x30FFFFFF) else Color(0x15000000)).copy(alpha = alpha))
            )
        }
    }
}

fun openUrlInBrowser(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val safeUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
    }
}

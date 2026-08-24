package com.example.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.service.intelligence.*
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

/**
 * Section 1: Today's Focus Card (Step 36)
 */
@Composable
fun TodaysFocusWidget(
    recommendation: TodayFocusRecommendation,
    onStartPractice: (MockTestConfig) -> Unit,
    onStartRevision: (String, String) -> Unit,
    onOpenCurrentAffairs: () -> Unit,
    onOpenQuickStudyModal: () -> Unit,
    isDark: Boolean = true
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("todays_focus_card"),
        shape = RoundedCornerShape(20.dp),
        borderColor = NeonCyan.copy(alpha = 0.6f),
        borderWidth = 1.5.dp
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.3f), ElectricViolet.copy(alpha = 0.3f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CenterFocusStrong,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "TODAY'S FOCUS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) NeonCyan else DeepIndigo,
                        letterSpacing = 0.8.sp
                    )
                }

                // ⚡ Quick Study Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldenSpark.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, GoldenSpark.copy(alpha = 0.6f)),
                    modifier = Modifier.springClickable(testTag = "quick_study_pill_button", onClick = onOpenQuickStudyModal)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FlashOn,
                            contentDescription = null,
                            tint = GoldenSpark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "⚡ Quick Study",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSpark,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Focus Title & Subject
            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricViolet.copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, ElectricViolet.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = recommendation.focusSubject,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricViolet,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "• ${recommendation.focusTopic}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reason sentence
            Text(
                text = recommendation.reason,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Cluster Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onStartPractice(recommendation.config) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF070B19)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("focus_start_practice_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start Practice", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onStartRevision(recommendation.focusSubject, recommendation.focusTopic) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Color.White else DeepIndigo
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0x40FFFFFF) else Color(0x30000000)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("focus_revise_button")
                ) {
                    Icon(Icons.Outlined.Autorenew, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Revise", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenCurrentAffairs,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GoldenSpark
                    ),
                    border = BorderStroke(1.dp, GoldenSpark.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("focus_ca_button")
                ) {
                    Text("📰 CA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Time-Based Daily Study Plan Widget (Step 36)
 */
@Composable
fun TimeBasedStudyPlanWidget(
    plan: TimeBasedStudyPlan,
    onSelectTimeOption: (String) -> Unit,
    onStartStep: (PlanBreakdownItem) -> Unit,
    isDark: Boolean = true
) {
    val options = listOf("15 min", "30 min", "60 min", "90+ min")

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("time_based_plan_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header & Time Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TODAY'S STUDY PLAN",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "${plan.totalMinutes} Min Plan",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0x201E293B) else Color(0x0A000000))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { opt ->
                    val isSelected = plan.availableTimeOption == opt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .springClickable(testTag = "time_plan_option_$opt") {
                                onSelectTimeOption(opt)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color(0xFF070B19) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Proportional Steps List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.breakdownItems.forEach { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                        border = BorderStroke(0.5.dp, if (isDark) Color(0x1CFFFFFF) else Color(0x15000000))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (item.activityType) {
                                                "REVISION" -> ElectricViolet.copy(alpha = 0.2f)
                                                "PRACTICE" -> NeonCyan.copy(alpha = 0.2f)
                                                "CURRENT_AFFAIRS" -> GoldenSpark.copy(alpha = 0.2f)
                                                else -> EmeraldSuccess.copy(alpha = 0.2f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${item.stepNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when (item.activityType) {
                                            "REVISION" -> ElectricViolet
                                            "PRACTICE" -> NeonCyan
                                            "CURRENT_AFFAIRS" -> GoldenSpark
                                            else -> EmeraldSuccess
                                        }
                                    )
                                }

                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { onStartStep(item) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Start step",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Due For Revision List Card (Step 36)
 */
@Composable
fun DueForRevisionWidget(
    topicMasteries: List<TopicMastery>,
    onReviseTopic: (String, String) -> Unit,
    isDark: Boolean = true
) {
    val dueList = remember(topicMasteries) {
        topicMasteries.filter {
            it.masteryState == "REVISION_DUE" ||
            it.masteryState == "WEAK" ||
            (it.recommendedReviewDateMillis > 0 && it.recommendedReviewDateMillis <= System.currentTimeMillis())
        }.take(3)
    }

    if (dueList.isEmpty()) return

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("due_for_revision_card"),
        shape = RoundedCornerShape(20.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Autorenew,
                        contentDescription = null,
                        tint = GoldenSpark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DUE FOR REVISION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "${dueList.size} Pending",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldenSpark,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dueList.forEach { tm ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                        border = BorderStroke(0.5.dp, GoldenSpark.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tm.topic,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${tm.subject} • ${if (tm.masteryState == "WEAK") "Weak Area" else "Spaced Schedule Due"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { onReviseTopic(tm.subject, tm.topic) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldenSpark,
                                    contentColor = Color(0xFF070B19)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Revise Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Primary Recommendation Card with Transparency CTA (Step 36 & Step 50)
 */
@Composable
fun PrimaryRecommendationCard(
    recommendation: TodayFocusRecommendation,
    onStartAction: () -> Unit,
    onOpenTransparencyModal: () -> Unit,
    isDark: Boolean = true
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("primary_recommendation_card"),
        shape = RoundedCornerShape(20.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PRIMARY RECOMMENDATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) ElectricViolet else DeepIndigo,
                        letterSpacing = 0.8.sp
                    )
                }

                // Why am I seeing this? Button
                TextButton(
                    onClick = onOpenTransparencyModal,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.testTag("why_am_i_seeing_this_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Why am I seeing this?", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = recommendation.reason,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStartAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricViolet,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(recommendation.primaryActionText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Quick Study Selection Dialog Modal (Step 36)
 */
@Composable
fun QuickStudyModalDialog(
    weakTopics: List<TopicPerformanceDetail>,
    onStartQuickStudy: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(22.dp),
            borderColor = GoldenSpark.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.FlashOn, null, tint = GoldenSpark, modifier = Modifier.size(22.dp))
                        Text(
                            text = "⚡ Quick Study Drill",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Targeted micro-practice prioritizing your weak areas and spaced revisions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(16.dp))

                listOf(
                    Triple(5, "5-Min Micro Drill", "5 Questions • Fast Concept Refresh"),
                    Triple(10, "10-Min Target Sprint", "10 Questions • Speed & Accuracy"),
                    Triple(15, "15-Min Focused Session", "15 Questions • High-Yield Practice")
                ).forEach { (duration, title, desc) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x1FFFFFFF),
                        border = BorderStroke(0.5.dp, GoldenSpark.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    onDismiss()
                                    onStartQuickStudy(duration)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = GoldenSpark, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * "Why am I seeing this?" Transparency Dialog Modal (Step 50)
 */
@Composable
fun TransparencySignalDialog(
    recommendation: TodayFocusRecommendation,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(22.dp),
            borderColor = NeonCyan.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Info, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Recommendation Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x1538BDF8),
                    border = BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Primary Signal:",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = recommendation.transparencySignal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "NOVA Personalization Logic:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Recommendations are calculated using your actual performance data (test attempts, accuracy %, logged mistakes, and spaced repetition schedules). No fabricated stats are used.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

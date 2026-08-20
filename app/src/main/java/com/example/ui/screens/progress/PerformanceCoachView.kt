package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.service.intelligence.*
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun PerformanceCoachView(
    report: PerformanceReport,
    onStartNextBestAction: (NextBestAction) -> Unit,
    onStartQuickPractice: (subject: String, topic: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Overall Trend & Comparison Banner
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            fillAlpha = 0.9f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NeonCyan, ElectricViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Psychology,
                                contentDescription = null,
                                tint = Color(0xFF050814),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI PERFORMANCE COACH",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = "Real Preparation Intelligence",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Trend Badge
                    val trendColor = when (report.overallTrend) {
                        PerformanceTrend.IMPROVING -> EmeraldSuccess
                        PerformanceTrend.DECLINING -> CoralRose
                        PerformanceTrend.STABLE -> AmberGold
                        PerformanceTrend.INSUFFICIENT_DATA -> Color(0xFF94A3B8)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = trendColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, trendColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = report.overallTrend.displayName,
                            color = trendColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Score + Comparison Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overall Test Accuracy",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = if (report.totalMocksTaken > 0) "${report.overallAccuracyPercent.toInt()}%" else "No Mocks Yet",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = report.accuracyDeltaText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (report.overallTrend == PerformanceTrend.IMPROVING) EmeraldSuccess else Color(0xFFCBD5E1)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Mocks Attempted: ${report.totalMocksTaken}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Questions Solved: ${report.totalQuestionsAttempted}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Status Chips (Strong, Weak, Improving)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (report.strongAreas.isNotEmpty()) {
                        StatusChip(
                            title = "Strong: ${report.strongAreas.first()}",
                            color = EmeraldSuccess,
                            icon = Icons.Filled.CheckCircle
                        )
                    }
                    if (report.weakAreas.isNotEmpty() && report.weakAreas.first() != "None — Keep up the good work!") {
                        StatusChip(
                            title = "Weak: ${report.weakAreas.first()}",
                            color = CoralRose,
                            icon = Icons.Filled.Warning
                        )
                    }
                }
            }
        }

        // 2. Next Best Action Card (Single Highest Leverage Task)
        val nba = report.nextBestAction
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            fillAlpha = 0.95f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AdsClick,
                            contentDescription = null,
                            tint = GoldenSpark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NEXT BEST ACTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSpark
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CoralRose.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = nba.urgencyTag,
                            color = CoralRose,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = nba.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "${nba.subject} • ${nba.topic} (${nba.durationMinutes} mins)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = nba.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(12.dp))

                GlassButton(
                    text = "Execute Next Action (${nba.durationMinutes}m)",
                    onClick = { onStartNextBestAction(nba) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("execute_next_best_action_btn")
                )
            }
        }

        // 3. High-Value Actionable Recommendations
        if (report.topRecommendations.isNotEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🎯 Coach Action Plan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    report.topRecommendations.forEachIndexed { idx, rec ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ElectricViolet.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = ElectricViolet,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = rec,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 4. Subject Trends Section
        if (report.subjectTrends.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "📚 Subject Performance & Trends",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                report.subjectTrends.forEach { subTrend ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subTrend.subject,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = subTrend.statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (subTrend.trend) {
                                        PerformanceTrend.IMPROVING -> EmeraldSuccess.copy(alpha = 0.15f)
                                        PerformanceTrend.DECLINING -> CoralRose.copy(alpha = 0.15f)
                                        PerformanceTrend.STABLE -> AmberGold.copy(alpha = 0.15f)
                                        PerformanceTrend.INSUFFICIENT_DATA -> Color(0x20FFFFFF)
                                    }
                                ) {
                                    Text(
                                        text = if (subTrend.testCount > 0) "${subTrend.currentAccuracyPercent.toInt()}%" else "N/A",
                                        color = when (subTrend.trend) {
                                            PerformanceTrend.IMPROVING -> EmeraldSuccess
                                            PerformanceTrend.DECLINING -> CoralRose
                                            PerformanceTrend.STABLE -> AmberGold
                                            PerformanceTrend.INSUFFICIENT_DATA -> Color.White
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onStartQuickPractice(subTrend.subject, "All Topics") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Practice ${subTrend.subject}",
                                        tint = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Difficulty & Time Insights Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Difficulty Breakdown
            GlassCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📊 Difficulty Accuracy",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Easy: ${report.difficultyAnalysis.easyAccuracyPercent.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldSuccess
                    )
                    Text(
                        text = "Medium: ${report.difficultyAnalysis.mediumAccuracyPercent.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberGold
                    )
                    Text(
                        text = "Hard: ${report.difficultyAnalysis.hardAccuracyPercent.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralRose
                    )
                }
            }

            // Time Management Breakdown
            GlassCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "⏱️ Time Management",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Avg Speed: ${report.timeAnalysis.avgSecondsPerQuestion.toInt()}s / Q",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                    Text(
                        text = if (report.timeAnalysis.hasTimePressureRisk) "⚠️ Late section drop detected" else "✅ Balanced pacing",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (report.timeAnalysis.hasTimePressureRisk) CoralRose else EmeraldSuccess
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    title: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Text(text = title, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}

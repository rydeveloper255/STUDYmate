package com.example.ui.screens.nova

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.viewmodel.NovaViewModel

@Composable
fun NovaStrategyAnalyticsTab(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val analytics by viewModel.analyticsData.collectAsState()
    val studyContext by viewModel.studyContext.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Header ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f),
            borderColor = NeonCyan.copy(alpha = 0.35f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("STUDY STRATEGY & ANALYTICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Preparation Velocity & Weak Areas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${studyContext.targetExam} • ${analytics.daysUntilExam} Days Remaining", fontSize = 12.sp, color = TextSecondary)
            }
        }

        // --- Metric Badges Grid ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Total Focus",
                value = String.format("%.1f hrs", analytics.totalFocusHours),
                icon = Icons.Default.Timer,
                tint = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Quiz Accuracy",
                value = "${analytics.averageQuizAccuracy.toInt()}%",
                icon = Icons.Default.Psychology,
                tint = AmberGold,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Consistency",
                value = "${analytics.weeklyConsistencyScore}%",
                icon = Icons.Default.LocalFireDepartment,
                tint = EmeraldGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // --- Subject Distribution ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.8f),
            borderColor = Color.White.copy(alpha = 0.1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Subject Time Allocation", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                val totalMins = analytics.subjectMinutes.values.sum().coerceAtLeast(1)
                analytics.subjectMinutes.forEach { (subject, mins) ->
                    val fraction = (mins.toFloat() / totalMins).coerceIn(0f, 1f)
                    val percent = (fraction * 100).toInt()

                    val color = when (subject) {
                        "Physics" -> NeonCyan
                        "Mathematics" -> ElectricIndigo
                        "Chemistry" -> CyberPink
                        else -> AmberGold
                    }

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(subject, fontSize = 13.sp, color = Color.White)
                            Text("${mins}m ($percent%)", fontSize = 12.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = color,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }

        // --- Diagnostic Weak Areas & Recommendations ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.8f),
            borderColor = CoralPink.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CoralPink, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Top Weak Topic Diagnosis", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${analytics.topWeakSubject}: ${analytics.topWeakTopic}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoralPink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Identified through recurring mistake patterns in mock tests and quizzes. Practice 5 numericals to solidify concepts.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        // --- Healthy Consistency & Rest Advice ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurface.copy(alpha = 0.8f),
            borderColor = EmeraldGreen.copy(alpha = 0.25f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Spa, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Healthy Consistency & Rest", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Consistent 3-hour daily focus with regular 10-minute breaks yields 4x higher retention than late-night cramming.", fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(title, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

package com.example.ui.screens.nova

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.springClickable
import com.example.ui.theme.*

// =============================================================================
// 1. WEB SEARCH MODE SELECTOR BAR
// =============================================================================
@Composable
fun NovaWebSearchModeBar(
    isActive: Boolean,
    currentMode: NovaWebSearchMode,
    onToggleActive: (Boolean) -> Unit,
    onSelectMode: (NovaWebSearchMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceElevated.copy(alpha = 0.9f),
        border = BorderStroke(
            1.dp,
            if (isActive) NeonCyan.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.springClickable { onToggleActive(!isActive) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Brush.linearGradient(listOf(NeonCyan, ElectricIndigo))
                                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = if (isActive) DarkCanvas else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Web Study Search",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) NeonCyan else Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isActive) EmeraldGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                    ) {
                        Text(
                            text = if (isActive) "ON" else "OFF",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) EmeraldGreen else TextSecondary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Mode Toggle Switch
                Switch(
                    checked = isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            // Mode Filter Chips
            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NovaWebSearchMode.values().forEach { mode ->
                        val selected = currentMode == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) NeonCyan.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.springClickable { onSelectMode(mode) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.icon,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = mode.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) NeonCyan else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 2. VERIFICATION RESULT CARD (Step 22 Feature 3)
// =============================================================================
@Composable
fun NovaVerificationResultCard(
    result: VerificationResult,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (result.status) {
        VerificationStatus.SUPPORTED -> EmeraldGreen
        VerificationStatus.PARTIALLY_SUPPORTED -> AmberGold
        VerificationStatus.UNCLEAR -> TextSecondary
        VerificationStatus.CONTRADICTED -> CoralPink
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Status Badge + Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result.status.badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                Text(
                    text = "Fact Check",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Claim statement
            Text(
                text = "“${result.claim}”",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Summary verdict
            Text(
                text = result.statusSummary,
                fontSize = 12.sp,
                color = statusColor,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )

            // Disagreement Warning Box
            if (result.sourcesDisagree && !result.disagreementDetails.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CoralPink.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, CoralPink.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("⚠️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Sources Disagree:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralPink
                            )
                            Text(
                                text = result.disagreementDetails ?: "",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Sources List Preview
            if (result.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "VERIFIED SOURCES:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                result.sources.take(3).forEach { src ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .springClickable { onOpenSource(src.url) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌐", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = src.title.ifBlank { src.domain },
                            fontSize = 11.sp,
                            color = NeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open",
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 3. NEWS EXPLANATION CARD (Step 22 Feature 2)
// =============================================================================
@Composable
fun NovaNewsExplanationCard(
    explanation: NewsExplanationResult,
    onOpenSource: (String) -> Unit,
    onStartQuiz: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showQuizAnswer by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricIndigo.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "📰 NEWS EXPLAINED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (explanation.sourceUrl.isNotBlank()) {
                    Text(
                        text = explanation.sourceUrl.substringBefore("/").substringAfter("://"),
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = explanation.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // What Happened
            Text(
                text = "What happened?",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Text(
                text = explanation.whatHappened,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Why it matters
            Text(
                text = "Why is it important?",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AmberGold
            )
            Text(
                text = explanation.whyImportant,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 16.sp
            )

            // Key facts bullets
            if (explanation.keyFacts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Key Takeaways:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
                explanation.keyFacts.forEach { fact ->
                    Row(
                        modifier = Modifier.padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("• ", fontSize = 11.sp, color = EmeraldGreen)
                        Text(
                            text = fact,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // AI-Generated Practice Question (Step 22 mandate)
            if (explanation.practiceQuestion != null) {
                val q = explanation.practiceQuestion
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ AI-generated practice question (Not a PYQ)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = q.questionText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        q.options.forEachIndexed { idx, opt ->
                            val isCorrect = idx == q.correctOptionIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${('A' + idx)}. $opt",
                                    fontSize = 11.sp,
                                    color = if (showQuizAnswer && isCorrect) EmeraldGreen else Color.White.copy(alpha = 0.8f),
                                    fontWeight = if (showQuizAnswer && isCorrect) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showQuizAnswer = !showQuizAnswer },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (showQuizAnswer) "Hide Answer" else "Reveal Answer",
                                    fontSize = 10.sp,
                                    color = NeonCyan
                                )
                            }
                        }

                        if (showQuizAnswer && q.explanation.isNotBlank()) {
                            Text(
                                text = "Explanation: ${q.explanation}",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 4. WHY SHOULD I STUDY THIS? CARD (Step 22 Feature 5)
// =============================================================================
@Composable
fun NovaWhyStudyCard(
    result: WhyStudyThisResult,
    onStartPractice: () -> Unit,
    onAddToRevision: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (result.priority.uppercase()) {
        "HIGH" -> CoralPink
        "MEDIUM" -> AmberGold
        else -> EmeraldGreen
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.2.dp, priorityColor.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = priorityColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "🎯 ${result.priority.uppercase()} PRIORITY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${result.targetExam} • ${result.subject}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Topic title
            Text(
                text = "Why study “${result.topic}”?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Weightage / Priority rationale
            Text(
                text = "Exam Pattern & Weightage:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Text(
                text = result.priorityRationale,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // How questions appear
            Text(
                text = "How questions appear in exam:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AmberGold
            )
            Text(
                text = result.examRelevance,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 16.sp
            )

            // Personalized Insight (if available)
            if (result.isPersonalized && result.personalizationContext.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElectricIndigo.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📊", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = result.personalizationContext,
                            fontSize = 11.sp,
                            color = NeonCyan,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Recommendations Checklist
            if (result.studyRecommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recommended Action:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
                result.studyRecommendations.forEach { rec ->
                    Row(
                        modifier = Modifier.padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("✓ ", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        Text(
                            text = rec,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 5. WEB SOURCES COMPONENT
// =============================================================================
@Composable
fun NovaWebSourcesCard(
    sources: List<WebSearchSource>,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sources.isEmpty()) return

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌐 SOURCES & CITATIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "${sources.size} sources",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            sources.take(3).forEach { src ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .springClickable { onOpenSource(src.url) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔗", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = src.title.ifBlank { src.domain },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = src.domain,
                                    fontSize = 9.sp,
                                    color = NeonCyan
                                )
                                if (src.isOfficial) {
                                    Text(
                                        text = " • 🏛️ Official Source",
                                        fontSize = 9.sp,
                                        color = EmeraldGreen
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open",
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

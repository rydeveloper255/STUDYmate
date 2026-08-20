package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.MistakeItem
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MistakeReviewView(
    mistakes: List<MistakeItem>,
    onDiagnoseMistakes: (subject: String) -> Unit,
    onMarkMistakeMastered: (Long, Boolean) -> Unit,
    onRequestAiExplanation: (MistakeItem) -> Unit,
    aiExplanationText: String? = null,
    isAiExplaining: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedFilterTab by remember { mutableStateOf("All") } // All, Unmastered, Repeated, By Subject
    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var expandedAiMistakeId by remember { mutableStateOf<Long?>(null) }

    val subjects = remember(mistakes) {
        listOf("All") + mistakes.map { it.subject }.distinct().filter { it.isNotBlank() }
    }

    val filteredMistakes = remember(mistakes, selectedFilterTab, selectedSubjectFilter) {
        mistakes.filter { m ->
            val matchesTab = when (selectedFilterTab) {
                "Unmastered" -> !m.isMastered
                "Mastered" -> m.isMastered
                "Repeated" -> {
                    val countForTopic = mistakes.count { it.topic.equals(m.topic, ignoreCase = true) && !it.isMastered }
                    countForTopic >= 2
                }
                else -> true
            }
            val matchesSubject = if (selectedSubjectFilter == "All") true else m.subject.equals(selectedSubjectFilter, ignoreCase = true)
            matchesTab && matchesSubject
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CoralRose.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = CoralRose,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MISTAKE BOOK & DIAGNOSTICS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CoralRose
                            )
                            Text(
                                text = "Targeted Error Remediation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Button(
                        onClick = { onDiagnoseMistakes(selectedSubjectFilter.ifBlank { "All" }) },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRose, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("diagnose_mistake_patterns_btn")
                    ) {
                        Icon(Icons.Filled.Psychology, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Diagnose", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("All", "Unmastered", "Repeated", "Mastered").forEach { tab ->
                val isSel = selectedFilterTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) CoralRose else Color(0x18FFFFFF))
                        .springClickable(testTag = "mistake_tab_$tab") {
                            selectedFilterTab = tab
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Subject Filter Row
        if (subjects.size > 2) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(subjects) { sub ->
                    val isSel = selectedSubjectFilter == sub
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) NeonCyan else Color(0x15FFFFFF),
                        modifier = Modifier.springClickable { selectedSubjectFilter = sub }
                    ) {
                        Text(
                            text = sub,
                            color = if (isSel) Color(0xFF050814) else Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (filteredMistakes.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Zero Mistakes Found! 🎉",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Your mistake log is clean for this selection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            filteredMistakes.forEach { mistake ->
                val isAiExpanded = expandedAiMistakeId == mistake.id

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x2038BDF8)
                            ) {
                                Text(
                                    text = "${mistake.subject} • ${mistake.topic}",
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = mistake.isMastered,
                                    onCheckedChange = { onMarkMistakeMastered(mistake.id, it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = EmeraldSuccess,
                                        uncheckedColor = Color(0xFF94A3B8)
                                    ),
                                    modifier = Modifier.testTag("mark_mastered_chk_${mistake.id}")
                                )
                                Text(
                                    text = if (mistake.isMastered) "Mastered" else "Mark Mastered",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (mistake.isMastered) EmeraldSuccess else Color(0xFF94A3B8)
                                )
                            }
                        }

                        Text(
                            text = mistake.questionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Answer Comparison Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CoralRose.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Your Answer:", style = MaterialTheme.typography.labelSmall, color = CoralRose)
                                    Text(mistake.studentAnswer, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccess.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Correct Answer:", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                    Text(mistake.correctAnswer, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                }
                            }
                        }

                        if (mistake.explanation.isNotBlank()) {
                            Text(
                                text = "Explanation: ${mistake.explanation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        // AI Concept Explanation Button
                        OutlinedButton(
                            onClick = {
                                if (isAiExpanded) {
                                    expandedAiMistakeId = null
                                } else {
                                    expandedAiMistakeId = mistake.id
                                    onRequestAiExplanation(mistake)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ElectricViolet),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("ai_explain_mistake_btn_${mistake.id}")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = ElectricViolet, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAiExpanded) "Hide AI Concept Analysis" else "AI Concept & Step-by-Step Explanation",
                                color = ElectricViolet,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        AnimatedVisibility(visible = isAiExpanded) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x256366F1),
                                border = BorderStroke(1.dp, Color(0x45818CF8)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (isAiExplaining) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(color = ElectricViolet, modifier = Modifier.size(16.dp))
                                            Text("Generating pedagogical explanation...", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                        }
                                    } else {
                                        Text(
                                            text = aiExplanationText ?: mistake.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.service.intelligence.RevisionPriority
import com.example.service.intelligence.RevisionState
import com.example.service.intelligence.SmartRevisionItem
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun SmartRevisionQueueView(
    queue: List<SmartRevisionItem>,
    onStartRevisionSession: (SmartRevisionItem) -> Unit,
    onStartQuickRevisionTest: (SmartRevisionItem, questionCount: Int) -> Unit,
    onRecordFeedback: (subject: String, topic: String, feedback: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedPriorityFilter by remember { mutableStateOf("All") }

    val filteredQueue = remember(queue, selectedPriorityFilter) {
        if (selectedPriorityFilter == "All") queue
        else queue.filter { it.priority.name.equals(selectedPriorityFilter, ignoreCase = true) }
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
                                .background(AmberGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Autorenew,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SMART REVISION QUEUE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                            Text(
                                text = "Spaced Repetition & Forgetting Curve",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricViolet.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${queue.size} Topics Due",
                            color = ElectricViolet,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Priority Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("All", "URGENT", "HIGH", "MEDIUM", "LOW").forEach { filter ->
                val isSel = selectedPriorityFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) ElectricViolet else Color(0x18FFFFFF))
                        .springClickable(testTag = "revision_filter_$filter") {
                            selectedPriorityFilter = filter
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        if (filteredQueue.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All Revisions Up to Date! 🎉",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "No topics reached forgetting curve threshold right now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            filteredQueue.forEach { item ->
                RevisionCard(
                    item = item,
                    onStartRevisionSession = onStartRevisionSession,
                    onStartQuickRevisionTest = onStartQuickRevisionTest,
                    onRecordFeedback = onRecordFeedback
                )
            }
        }
    }
}

@Composable
private fun RevisionCard(
    item: SmartRevisionItem,
    onStartRevisionSession: (SmartRevisionItem) -> Unit,
    onStartQuickRevisionTest: (SmartRevisionItem, Int) -> Unit,
    onRecordFeedback: (subject: String, topic: String, feedback: String) -> Unit = { _, _, _ -> }
) {
    var showQuickTestOptions by remember { mutableStateOf(false) }

    val priorityColor = when (item.priority) {
        RevisionPriority.URGENT -> CoralRose
        RevisionPriority.HIGH -> AmberGold
        RevisionPriority.MEDIUM -> NeonCyan
        RevisionPriority.LOW -> Color(0xFF94A3B8)
    }

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
                    color = priorityColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = item.priority.displayName,
                        color = priorityColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "Mastery: ${item.masteryScore}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.masteryScore >= 75) EmeraldSuccess else CoralRose,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${item.subject}: ${item.topic}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = item.reason,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            // Smart Review Ladder Step
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0x15FFFFFF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Recommended Strategy: ${item.recommendedLadderStep}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onStartRevisionSession(item) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ElectricViolet),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("start_revision_session_${item.topic}")
                ) {
                    Icon(Icons.Filled.Book, null, tint = ElectricViolet, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Study (${item.recommendedDurationMins}m)", color = ElectricViolet, style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = { showQuickTestOptions = !showQuickTestOptions },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF050814)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("quick_revision_test_${item.topic}")
                ) {
                    Icon(Icons.Filled.Quiz, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quick Test", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(visible = showQuickTestOptions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 10, 15).forEach { count ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x2538BDF8))
                                .springClickable {
                                    showQuickTestOptions = false
                                    onStartQuickRevisionTest(item, count)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count Qs",
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Spaced Review Feedback Row (Step 36)
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rate Understanding:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple("UNDERSTOOD", "😄 Easy (+7d)", EmeraldSuccess),
                        Triple("NEEDS_PRACTICE", "😐 Practice (+2d)", AmberGold),
                        Triple("DIFFICULT", "🙁 Hard (+1d)", CoralRose)
                    ).forEach { (fbKey, label, color) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = color.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, color.copy(alpha = 0.4f)),
                            modifier = Modifier.springClickable {
                                onRecordFeedback(item.subject, item.topic, fbKey)
                            }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

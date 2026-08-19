package com.example.ui.screens.nova

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrentAffairsItem
import com.example.data.model.ExamUpdateItem
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaViewModel

@Composable
fun NovaCurrentAffairsTab(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentAffairs by viewModel.allCurrentAffairs.collectAsState()
    val examUpdates by viewModel.allExamUpdates.collectAsState()
    val studyContext by viewModel.studyContext.collectAsState()

    var selectedSection by remember { mutableStateOf(0) } // 0: Current Affairs, 1: Exam Notices

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Switcher
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                fillAlpha = 0.85f
            ) {
                Column {
                    Text(
                        text = "Current Affairs & Exam Radar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Verified official notifications for ${studyContext.targetExam}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22000000))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedSection == 0) NeonCyan else Color.Transparent)
                                .springClickable(testTag = "tab_current_affairs", onClick = { selectedSection = 0 })
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📰 Current Affairs (${currentAffairs.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSection == 0) Color.Black else Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedSection == 1) NeonCyan else Color.Transparent)
                                .springClickable(testTag = "tab_exam_notices", onClick = { selectedSection = 1 })
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📢 Official Notices (${examUpdates.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSection == 1) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }

        if (selectedSection == 0) {
            // Current Affairs List
            items(currentAffairs, key = { it.id }) { item ->
                CurrentAffairsItemCard(
                    item = item,
                    onToggleSaved = {
                        viewModel.toggleCurrentAffairsSaved(item.id, !item.isSavedForRevision)
                    },
                    onOpenSource = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        } else {
            // Exam Notices List
            items(examUpdates, key = { it.id }) { update ->
                ExamUpdateItemCard(
                    update = update,
                    onOpenOfficialLink = {
                        viewModel.markExamUpdateRead(update.id)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.officialLink))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CurrentAffairsItemCard(
    item: CurrentAffairsItem,
    onToggleSaved: () -> Unit,
    onOpenSource: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.8f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleSaved, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (item.isSavedForRevision) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (item.isSavedForRevision) NeonCyan else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (item.sourceUrl.isNotBlank()) {
                        IconButton(onClick = onOpenSource, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Filled.OpenInNew,
                                contentDescription = "Source",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )

            if (item.examRelevance.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x22000000))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "🎯 ${item.examRelevance}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamUpdateItemCard(
    update: ExamUpdateItem,
    onOpenOfficialLink: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        fillAlpha = 0.8f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33818CF8))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = update.noticeType.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricIndigo,
                            fontSize = 10.sp
                        )
                    }

                    if (update.isVerifiedOfficial) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified Official",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = update.publishDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "[${update.examName}] ${update.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = update.summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )

            if (update.officialLink.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenOfficialLink,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x3338BDF8),
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Official Portal Notice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

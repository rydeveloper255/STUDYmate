package com.example.ui.screens.updates

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.updates.LatestUpdateItem
import com.example.data.model.updates.UpdateCategory
import com.example.localization.GlobalLanguageSwitcher
import com.example.localization.appString
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDetailScreen(
    item: LatestUpdateItem,
    onBack: () -> Unit,
    onToggleSave: (String, Boolean) -> Unit = { _, _ -> },
    onSetReminder: (String, Boolean, Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val category = item.category
    val accent = category.accentColor

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("update_detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isDark) Color.White else DeepIndigo
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "${category.titleEn} Detail",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else DeepIndigo
                            )
                            Text(
                                text = item.organization.ifBlank { "Official Notification" },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onToggleSave(item.id, !item.isSaved) },
                            modifier = Modifier.testTag("update_detail_save_button")
                        ) {
                            Icon(
                                imageVector = if (item.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (item.isSaved) AmberGold else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                        GlobalLanguageSwitcher(modifier = Modifier.testTag("update_detail_lang_switcher"))
                    }
                }
            }
        },
        bottomBar = {
            // Bottom Action Bar with Official Links
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) Color(0xFF0F172A) else Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val primaryUrl = item.applyUrl.ifBlank { item.downloadUrl }.ifBlank { item.sourceUrl }
                    if (primaryUrl.isNotBlank()) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(primaryUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("update_detail_primary_action_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = when (category) {
                                    UpdateCategory.VACANCY -> Icons.Filled.Launch
                                    UpdateCategory.ADMIT_CARD -> Icons.Filled.Download
                                    UpdateCategory.RESULT -> Icons.Filled.Visibility
                                    UpdateCategory.ANSWER_KEY -> Icons.Filled.FileDownload
                                    UpdateCategory.ADMISSION -> Icons.Filled.AppRegistration
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (category) {
                                    UpdateCategory.VACANCY -> "Apply Online"
                                    UpdateCategory.ADMIT_CARD -> "Download Admit Card"
                                    UpdateCategory.RESULT -> "Check Result"
                                    UpdateCategory.ANSWER_KEY -> "Download Answer Key"
                                    UpdateCategory.ADMISSION -> "Apply for Admission"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }

                    if (item.sourceUrl.isNotBlank() && item.sourceUrl != primaryUrl) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("update_detail_official_site_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isDark) Color.White else DeepIndigo
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Official Site",
                                color = if (isDark) Color.White else DeepIndigo,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(appBackgroundGradient(isDark))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Title & Badge Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.85f)
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
                        Surface(
                            color = accent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.titleEn.uppercase(),
                                    color = accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (item.totalVacancies != null && item.totalVacancies > 0) {
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "🔥 ${item.totalVacancies} Vacancies",
                                    color = EmeraldSuccess,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else DeepIndigo,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.organization,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                        )
                    }

                    if (item.postName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Badge,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Post: ${item.postName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // 2. Important Dates Timeline Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Important Dates & Timeline",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else DeepIndigo
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!item.startDate.isNullOrBlank() || !item.publishedDate.isNullOrBlank()) {
                        DateTimelineRow(
                            label = if (category == UpdateCategory.ADMIT_CARD || category == UpdateCategory.RESULT || category == UpdateCategory.ANSWER_KEY) "Release Date" else "Application Begins",
                            value = item.startDate ?: item.publishedDate ?: "Available",
                            icon = Icons.Filled.PlayCircleFilled,
                            iconColor = EmeraldSuccess,
                            isDark = isDark
                        )
                    }

                    if (!item.lastDate.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DateTimelineRow(
                            label = "Last Date to Apply",
                            value = item.lastDate,
                            icon = Icons.Filled.Alarm,
                            iconColor = CoralRose,
                            isDark = isDark,
                            isHighlighted = true
                        )
                    }

                    if (!item.examDate.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DateTimelineRow(
                            label = "Examination Date",
                            value = item.examDate,
                            icon = Icons.Filled.Event,
                            iconColor = NeonCyan,
                            isDark = isDark
                        )
                    }
                }
            }

            // 3. Category Specific Eligibility & Details
            if (category == UpdateCategory.VACANCY || category == UpdateCategory.ADMISSION) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Eligibility & Qualification",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else DeepIndigo
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        DetailKeyValRow(
                            label = "Educational Qualification",
                            value = item.educationalQualification,
                            isDark = isDark
                        )

                        if (item.ageCriteria != "Not specified") {
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailKeyValRow(
                                label = "Age Limit Criteria",
                                value = item.ageCriteria,
                                isDark = isDark
                            )
                        }

                        if (item.feeDetails != "Not specified") {
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailKeyValRow(
                                label = "Application Fee",
                                value = item.feeDetails,
                                isDark = isDark
                            )
                        }
                    }
                }
            }

            // 4. Summary & Description Card
            if (item.shortDescription.isNotBlank() || item.fullContent.isNotBlank()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Overview & Important Details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else DeepIndigo
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = item.fullContent.ifBlank { item.shortDescription },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // 5. Selection Process / Important Instructions
            if (item.selectionProcess.isNotEmpty() || item.importantInstructions.isNotEmpty()) {
                val instructions = item.importantInstructions.ifEmpty { item.selectionProcess }
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircleOutline,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (category == UpdateCategory.VACANCY) "Selection Steps & Instructions" else "Candidate Guidelines",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else DeepIndigo
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        instructions.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = accent.copy(alpha = 0.2f),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // 6. Source Transparency & Verification Notice
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verified from ${item.sourceName.ifBlank { "Official Govt Source" }}. Always cross-check the official portal before making payments.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DateTimelineRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isDark: Boolean,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isHighlighted) iconColor.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) iconColor else if (isDark) Color.White else DeepIndigo
        )
    }
}

@Composable
private fun DetailKeyValRow(
    label: String,
    value: String,
    isDark: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) Color.White else DeepIndigo,
            lineHeight = 20.sp
        )
    }
}

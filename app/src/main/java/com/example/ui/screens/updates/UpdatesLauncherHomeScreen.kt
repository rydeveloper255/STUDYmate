package com.example.ui.screens.updates

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.updates.UpdateCategory
import com.example.localization.GlobalLanguageSwitcher
import com.example.localization.appString
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesLauncherHomeScreen(
    onNavigateToCategory: (UpdateCategory) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val categories = remember {
        listOf(
            UpdateCategory.VACANCY,
            UpdateCategory.ADMIT_CARD,
            UpdateCategory.RESULT,
            UpdateCategory.ANSWER_KEY,
            UpdateCategory.ADMISSION
        )
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Latest Updates",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else DeepIndigo
                            )
                        }
                        Text(
                            text = "Official Govt Notifications & Portals",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("launcher_alerts_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Alerts",
                                tint = if (isDark) Color.White else DeepIndigo
                            )
                        }
                        GlobalLanguageSwitcher(modifier = Modifier.testTag("launcher_lang_switcher"))
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
            // 1. Hero Hub Banner
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = NeonCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Live Ingestion Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }

                        Text(
                            text = "24/7 Verified",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Explore Exam Updates & Notices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else DeepIndigo
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose a dedicated category below to check active applications, release dates, scorecards, and answer keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        lineHeight = 18.sp
                    )
                }
            }

            // 2. Section Header: The 5 Dedicated Categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Update Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DeepIndigo
                )
                Text(
                    text = "5 Dedicated Hubs",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            // 3. 2-Column Professional Category Grid Cards
            // 1st Row: Vacancy (Span 2) - Primary High Value Launcher
            CategoryLauncherCard(
                category = UpdateCategory.VACANCY,
                isDark = isDark,
                isFullWidth = true,
                onClick = { onNavigateToCategory(UpdateCategory.VACANCY) },
                modifier = Modifier.testTag("launcher_card_vacancy")
            )

            // 2nd Row: Admit Card & Result (2 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryLauncherCard(
                    category = UpdateCategory.ADMIT_CARD,
                    isDark = isDark,
                    isFullWidth = false,
                    onClick = { onNavigateToCategory(UpdateCategory.ADMIT_CARD) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launcher_card_admit_card")
                )
                CategoryLauncherCard(
                    category = UpdateCategory.RESULT,
                    isDark = isDark,
                    isFullWidth = false,
                    onClick = { onNavigateToCategory(UpdateCategory.RESULT) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launcher_card_result")
                )
            }

            // 3rd Row: Answer Key & Admission (2 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryLauncherCard(
                    category = UpdateCategory.ANSWER_KEY,
                    isDark = isDark,
                    isFullWidth = false,
                    onClick = { onNavigateToCategory(UpdateCategory.ANSWER_KEY) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launcher_card_answer_key")
                )
                CategoryLauncherCard(
                    category = UpdateCategory.ADMISSION,
                    isDark = isDark,
                    isFullWidth = false,
                    onClick = { onNavigateToCategory(UpdateCategory.ADMISSION) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launcher_card_admission")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Source Trust & Verification Footer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Direct Official Portal Links",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else DeepIndigo
                        )
                        Text(
                            text = "All application & download links redirect to authentic government websites (SSC, RRB, UPSC, NTA, IBPS).",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CategoryLauncherCard(
    category: UpdateCategory,
    isDark: Boolean,
    isFullWidth: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = category.accentColor

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        backgroundColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isFullWidth) 16.dp else 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.18f),
                    modifier = Modifier.size(if (isFullWidth) 42.dp else 36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(if (isFullWidth) 24.dp else 20.dp)
                        )
                    }
                }

                Surface(
                    color = accent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = category.badgeTextEn,
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isFullWidth) 12.dp else 10.dp))

            Text(
                text = category.titleEn,
                style = if (isFullWidth) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else DeepIndigo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = category.subtitleEn,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                maxLines = if (isFullWidth) 2 else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Explore Page",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// --- Liquid Glass Modifiers ---

@Composable
fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp,
    fillAlpha: Float = 0.65f
): Modifier {
    val isDark = isAppInDarkTheme()
    val backgroundColor = if (isDark) {
        Color(0xFF131C2E).copy(alpha = fillAlpha)
    } else {
        Color(0xFFFFFFFF).copy(alpha = (fillAlpha + 0.25f).coerceAtMost(0.95f))
    }
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0x4DFFFFFF),
                Color(0x15FFFFFF),
                Color(0x3538BDF8)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0x70CBD5E1),
                Color(0x3594A3B8),
                Color(0x406366F1)
            )
        )
    }

    return this
        .shadow(elevation, shape, clip = false, spotColor = if (isDark) Color(0x3338BDF8) else Color(0x200F172A))
        .clip(shape)
        .background(backgroundColor, shape)
        .border(borderWidth, borderBrush, shape)
}

@Composable
fun Modifier.springClickable(
    testTag: String? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "click_scale"
    )

    var mod = this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
            onClick = onClick
        )
    if (testTag != null) {
        mod = mod.testTag(testTag)
    }
    return mod
}

// --- Glass Card ---

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 6.dp,
    fillAlpha: Float = 0.65f,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var cardModifier = modifier.glassEffect(shape, borderWidth, elevation, fillAlpha)
    if (onClick != null) {
        cardModifier = cardModifier.springClickable(testTag = testTag, onClick = onClick)
    } else if (testTag != null) {
        cardModifier = cardModifier.testTag(testTag)
    }

    Box(
        modifier = cardModifier.padding(16.dp),
        content = content
    )
}

// --- Glass Button ---

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    isLoading: Boolean = false,
    testTag: String = "glass_button"
) {
    val shape = RoundedCornerShape(16.dp)
    val backgroundBrush = if (isPrimary) {
        Brush.linearGradient(
            colors = listOf(NeonCyan, ElectricViolet, NebulaPurple)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0x331E293B), Color(0x221E293B))
        )
    }

    val contentColor = if (isPrimary) Color(0xFF070B19) else Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(if (isPrimary) 8.dp else 2.dp, shape, spotColor = if (isPrimary) NeonCyan else Color.Transparent)
            .clip(shape)
            .background(backgroundBrush)
            .border(
                1.dp,
                if (isPrimary) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent))
                else Brush.linearGradient(listOf(Color(0x40FFFFFF), Color(0x15FFFFFF))),
                shape
            )
            .springClickable(testTag = testTag, onClick = { if (!isLoading) onClick() }),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = contentColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- Liquid Progress Ring ---

@Composable
fun LiquidProgressRing(
    progress: Float, // 0.0 to 1.0
    currentText: String,
    targetText: String,
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    strokeWidth: Dp = 12.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "progress_ring"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val sweep = animatedProgress * 360f
            // Background track
            drawArc(
                color = Color(0x25FFFFFF),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Active Liquid Progress Arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(NeonCyan, ElectricViolet, NebulaPurple, NeonCyan)
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = targetText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// --- Floating Glass Bottom Navigation Bar ---

enum class AppNavTab(val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("Home", Icons.Outlined.Home, Icons.Filled.Home),
    AI_TUTOR("AI", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
    STUDY("Study", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    FOCUS("Focus", Icons.Outlined.TrackChanges, Icons.Filled.TrackChanges),
    PROGRESS("Progress", Icons.Outlined.Assessment, Icons.Filled.Assessment)
}

@Composable
fun FloatingGlassNavBar(
    currentTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .glassEffect(
                    shape = RoundedCornerShape(34.dp),
                    elevation = 14.dp,
                    fillAlpha = 0.85f
                ),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppNavTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                        label = "tab_scale"
                    )

                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (isSelected) Color(0x3038BDF8) else Color.Transparent
                            )
                            .springClickable(testTag = "nav_tab_${tab.name.lowercase()}") {
                                onTabSelected(tab)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) NeonCyan else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Streak and Header Badges ---

@Composable
fun StreakBadge(streakDays: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x33F59E0B))
            .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = "Streak",
            tint = GoldenSpark,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$streakDays Days",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = GoldenSpark
        )
    }
}

@Composable
fun XpBadge(xp: Int, level: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x30818CF8))
            .border(1.dp, Color(0x60818CF8), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = "XP",
            tint = NeonCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Lvl $level • $xp XP",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
        )
    }
}

@Composable
fun ThemeToggleButton(
    modifier: Modifier = Modifier,
    testTag: String = "theme_toggle_button"
) {
    val themeController = LocalThemeController.current
    val isDark = themeController.isDarkTheme

    IconButton(
        onClick = { themeController.toggleTheme() },
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isDark) Color(0x3038BDF8) else Color(0x256366F1))
            .border(1.dp, if (isDark) Color(0x5038BDF8) else Color(0x406366F1), CircleShape)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
            contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode",
            tint = if (isDark) GoldenSpark else Color(0xFFF59E0B),
            modifier = Modifier.size(20.dp)
        )
    }
}

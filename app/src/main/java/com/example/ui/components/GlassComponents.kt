package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

// =========================================================================
// 1. LIQUID GLASS MODIFIERS & INTERACTION SYSTEM
// =========================================================================

/**
 * Applies a consistent liquid glass styling:
 * - Controlled translucency adapted for Dark, Light, and AMOLED modes.
 * - Subtle ambient depth shadow.
 * - Refined glowing translucent gradient border.
 */
@Composable
fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 6.dp,
    fillAlpha: Float = 0.65f
): Modifier {
    val isDark = isAppInDarkTheme()
    val themeMode = currentThemeMode()

    val backgroundColor = when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> AmoledCardSurface.copy(alpha = 0.85f)
        AppThemeMode.GLASS_LIGHT -> SurfaceLight.copy(alpha = (fillAlpha + 0.25f).coerceAtMost(0.95f))
        AppThemeMode.NOVA_DARK -> CardSurfaceDark.copy(alpha = fillAlpha)
    }

    val borderBrush = when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> Brush.linearGradient(
            listOf(
                Color(0x30FFFFFF),
                Color(0x10FFFFFF),
                Color(0x2038BDF8)
            )
        )
        AppThemeMode.GLASS_LIGHT -> Brush.linearGradient(
            listOf(
                Color(0x80CBD5E1),
                Color(0x3594A3B8),
                Color(0x406366F1)
            )
        )
        AppThemeMode.NOVA_DARK -> Brush.linearGradient(
            listOf(
                Color(0x4DFFFFFF),
                Color(0x15FFFFFF),
                Color(0x3538BDF8)
            )
        )
    }

    val spotColor = when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> Color.Transparent
        AppThemeMode.GLASS_LIGHT -> Color(0x180F172A)
        AppThemeMode.NOVA_DARK -> Color(0x3338BDF8)
    }

    return this
        .shadow(if (themeMode == AppThemeMode.AMOLED_BLACK) 0.dp else elevation, shape, clip = false, spotColor = spotColor)
        .clip(shape)
        .background(backgroundColor, shape)
        .border(borderWidth, borderBrush, shape)
}

/**
 * Spring-based interaction feedback on click, ensuring 48dp touch accessibility
 * with smooth scaling and bounded ripple.
 */
@Composable
fun Modifier.springClickable(
    enabled: Boolean = true,
    testTag: String? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "spring_click_scale"
    )

    var mod = this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
            enabled = enabled,
            onClick = onClick
        )
    if (testTag != null) {
        mod = mod.testTag(testTag)
    }
    return mod
}

// =========================================================================
// 2. GLASS CARD COMPONENT
// =========================================================================

/**
 * Universal Liquid Glass Container for cards and sections.
 * Clean, lightweight, calm, and responsive across themes.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 6.dp,
    fillAlpha: Float = 0.65f,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isAppInDarkTheme()
    val themeMode = currentThemeMode()

    val finalBg = backgroundColor ?: when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> AmoledCardSurface.copy(alpha = 0.85f)
        AppThemeMode.GLASS_LIGHT -> SurfaceLight.copy(alpha = (fillAlpha + 0.25f).coerceAtMost(0.95f))
        AppThemeMode.NOVA_DARK -> CardSurfaceDark.copy(alpha = fillAlpha)
    }

    val finalBorderBrush = if (borderColor != null) {
        Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.35f)))
    } else when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> Brush.linearGradient(
            listOf(Color(0x30FFFFFF), Color(0x10FFFFFF), Color(0x2038BDF8))
        )
        AppThemeMode.GLASS_LIGHT -> Brush.linearGradient(
            listOf(Color(0x80CBD5E1), Color(0x3594A3B8), Color(0x406366F1))
        )
        AppThemeMode.NOVA_DARK -> Brush.linearGradient(
            listOf(Color(0x4DFFFFFF), Color(0x15FFFFFF), Color(0x3538BDF8))
        )
    }

    val spotColor = when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> Color.Transparent
        AppThemeMode.GLASS_LIGHT -> Color(0x180F172A)
        AppThemeMode.NOVA_DARK -> Color(0x3338BDF8)
    }

    var cardModifier = modifier
        .shadow(if (themeMode == AppThemeMode.AMOLED_BLACK) 0.dp else elevation, shape, clip = false, spotColor = spotColor)
        .clip(shape)
        .background(finalBg, shape)
        .border(borderWidth, finalBorderBrush, shape)

    if (onClick != null) {
        cardModifier = cardModifier.springClickable(testTag = testTag, onClick = onClick)
    } else if (testTag != null) {
        cardModifier = cardModifier.testTag(testTag)
    }

    Box(
        modifier = cardModifier.padding(contentPadding),
        content = content
    )
}

// =========================================================================
// 3. BUTTON SYSTEM (Primary, Secondary, GlassButton, IconButtons)
// =========================================================================

/**
 * Universal GlassButton supporting Primary and Secondary styling, loading state,
 * and custom color overrides while maintaining full backward compatibility.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    loadingText: String = "Please wait...",
    containerColor: Color? = null,
    contentColor: Color? = null,
    testTag: String = "glass_button"
) {
    val isDark = isAppInDarkTheme()
    val themeMode = currentThemeMode()
    val shape = RoundedCornerShape(16.dp)

    val backgroundBrush = if (containerColor != null) {
        Brush.linearGradient(listOf(containerColor, containerColor))
    } else if (isPrimary) {
        when (themeMode) {
            AppThemeMode.GLASS_LIGHT -> Brush.linearGradient(
                listOf(DeepIndigo, ElectricViolet)
            )
            else -> Brush.linearGradient(
                listOf(NeonCyan, ElectricViolet, NebulaPurple)
            )
        }
    } else {
        when (themeMode) {
            AppThemeMode.GLASS_LIGHT -> Brush.linearGradient(
                listOf(Color(0xFFE2E8F0).copy(alpha = 0.8f), Color(0xFFCBD5E1).copy(alpha = 0.6f))
            )
            AppThemeMode.AMOLED_BLACK -> Brush.linearGradient(
                listOf(Color(0x33262626), Color(0x22171717))
            )
            AppThemeMode.NOVA_DARK -> Brush.linearGradient(
                listOf(Color(0x331E293B), Color(0x221E293B))
            )
        }
    }

    val finalContentColor = contentColor ?: if (isPrimary) {
        if (themeMode == AppThemeMode.GLASS_LIGHT) Color.White else Color(0xFF070B19)
    } else {
        if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF0F172A) else Color.White
    }

    val borderBrush = if (isPrimary) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent))
    } else {
        if (themeMode == AppThemeMode.GLASS_LIGHT) {
            Brush.linearGradient(listOf(Color(0x60CBD5E1), Color(0x3094A3B8)))
        } else {
            Brush.linearGradient(listOf(Color(0x40FFFFFF), Color(0x15FFFFFF)))
        }
    }

    val shadowElevation = if (!enabled) 0.dp else if (isPrimary) 8.dp else 1.dp
    val spotColor = if (isPrimary && isDark) NeonCyan else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .shadow(shadowElevation, shape, spotColor = spotColor)
            .clip(shape)
            .background(backgroundBrush)
            .border(1.dp, borderBrush, shape)
            .springClickable(
                enabled = enabled && !isLoading,
                testTag = testTag,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = finalContentColor,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = loadingText,
                    style = MaterialTheme.typography.labelLarge,
                    color = finalContentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = finalContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = finalContentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Dedicated high-emphasis Primary Action Button.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    loadingText: String = "Please wait...",
    testTag: String = "primary_button"
) {
    GlassButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        isPrimary = true,
        isLoading = isLoading,
        enabled = enabled,
        loadingText = loadingText,
        testTag = testTag
    )
}

/**
 * Dedicated translucent Secondary Action Button.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    loadingText: String = "Please wait...",
    testTag: String = "secondary_button"
) {
    GlassButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        isPrimary = false,
        isLoading = isLoading,
        enabled = enabled,
        loadingText = loadingText,
        testTag = testTag
    )
}

/**
 * Circular / Rounded Glass Icon Button with touch target safety (min 48dp).
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    tint: Color? = null,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    shape: Shape = CircleShape,
    testTag: String? = null
) {
    val isDark = isAppInDarkTheme()
    val themeMode = currentThemeMode()

    val finalBg = backgroundColor ?: when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> Color(0x30262626)
        AppThemeMode.GLASS_LIGHT -> Color(0xFFF1F5F9).copy(alpha = 0.9f)
        AppThemeMode.NOVA_DARK -> Color(0x331E293B)
    }

    val finalBorder = borderColor ?: when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> Color(0x25FFFFFF)
        AppThemeMode.GLASS_LIGHT -> Color(0x60CBD5E1)
        AppThemeMode.NOVA_DARK -> Color(0x3538BDF8)
    }

    val finalTint = tint ?: when (themeMode) {
        AppThemeMode.GLASS_LIGHT -> Color(0xFF0F172A)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .wrapContentSize(Alignment.Center)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(finalBg)
                .border(1.dp, finalBorder, shape)
                .springClickable(testTag = testTag, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = finalTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Quick Glass Close / Dismiss Action.
 */
@Composable
fun GlassCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "close_button"
) {
    GlassIconButton(
        icon = Icons.Filled.Close,
        contentDescription = "Close",
        onClick = onClose,
        modifier = modifier,
        testTag = testTag
    )
}

// =========================================================================
// 4. GLASS INPUT FIELD
// =========================================================================

/**
 * Standardized Liquid Glass Input Field with refined glowing focus border,
 * clear label, hint, leading & trailing slots, and error handling.
 */
@Composable
fun GlassInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String = "glass_input"
) {
    val isDark = isAppInDarkTheme()
    val themeMode = currentThemeMode()

    val surfaceColor = when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> Color(0xFF0F0F0F)
        AppThemeMode.GLASS_LIGHT -> Color(0xFFFFFFFF)
        AppThemeMode.NOVA_DARK -> Color(0xFF0D1424)
    }

    val unfocusedBorderColor = when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> Color(0x30334155)
        AppThemeMode.GLASS_LIGHT -> Color(0xFFCBD5E1)
        AppThemeMode.NOVA_DARK -> Color(0x3538BDF8)
    }

    val focusedBorderColor = if (themeMode == AppThemeMode.GLASS_LIGHT) DeepIndigo else NeonCyan
    val textColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF0F172A) else Color.White
    val placeholderColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF475569) else Color(0xFF94A3B8),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            shape = RoundedCornerShape(16.dp),
            singleLine = singleLine,
            maxLines = maxLines,
            readOnly = readOnly,
            enabled = enabled,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = isError,
            placeholder = if (placeholder != null) {
                { Text(placeholder, color = placeholderColor, style = MaterialTheme.typography.bodyMedium) }
            } else null,
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isError) CoralRose else focusedBorderColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null,
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor.copy(alpha = 0.85f),
                disabledContainerColor = surfaceColor.copy(alpha = 0.5f),
                errorContainerColor = surfaceColor,
                focusedBorderColor = if (isError) CoralRose else focusedBorderColor,
                unfocusedBorderColor = if (isError) CoralRose.copy(alpha = 0.6f) else unfocusedBorderColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = focusedBorderColor
            )
        )

        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = CoralRose,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

// =========================================================================
// 5. GLASS CHIP
// =========================================================================

/**
 * Filter / Status / Tag Chip with responsive glass styling.
 */
@Composable
fun GlassChip(
    label: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selectedColor: Color? = null,
    testTag: String? = null
) {
    val isDark = isAppInDarkTheme()
    val themeMode = currentThemeMode()
    val shape = RoundedCornerShape(12.dp)

    val activeColor = selectedColor ?: if (themeMode == AppThemeMode.GLASS_LIGHT) DeepIndigo else NeonCyan

    val bgColor = if (selected) {
        activeColor.copy(alpha = if (themeMode == AppThemeMode.GLASS_LIGHT) 0.16f else 0.22f)
    } else {
        when (themeMode) {
            AppThemeMode.AMOLED_BLACK -> Color(0x30202020)
            AppThemeMode.GLASS_LIGHT -> Color(0xFFF1F5F9)
            AppThemeMode.NOVA_DARK -> Color(0x281E293B)
        }
    }

    val borderBrush = if (selected) {
        Brush.linearGradient(listOf(activeColor, activeColor.copy(alpha = 0.5f)))
    } else {
        when (themeMode) {
            AppThemeMode.AMOLED_BLACK -> Brush.linearGradient(listOf(Color(0x25FFFFFF), Color(0x10FFFFFF)))
            AppThemeMode.GLASS_LIGHT -> Brush.linearGradient(listOf(Color(0x60CBD5E1), Color(0x3094A3B8)))
            AppThemeMode.NOVA_DARK -> Brush.linearGradient(listOf(Color(0x30FFFFFF), Color(0x15FFFFFF)))
        }
    }

    val textColor = if (selected) {
        if (themeMode == AppThemeMode.GLASS_LIGHT) DeepIndigo else Color.White
    } else {
        if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF475569) else Color(0xFF94A3B8)
    }

    var chipMod = modifier
        .clip(shape)
        .background(bgColor)
        .border(1.dp, borderBrush, shape)

    if (onClick != null) {
        chipMod = chipMod.springClickable(testTag = testTag, onClick = onClick)
    } else if (testTag != null) {
        chipMod = chipMod.testTag(testTag)
    }

    Row(
        modifier = chipMod.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) activeColor else textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

// =========================================================================
// 6. SECTION HEADER
// =========================================================================

/**
 * Standardized Section Title with optional icon, subtitle, and action link.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionTestTag: String? = null
) {
    val isDark = isAppInDarkTheme()
    val themeMode = currentThemeMode()

    val primaryTextColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF0F172A) else Color.White
    val secondaryTextColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF64748B) else Color(0xFF94A3B8)
    val accentColor = if (themeMode == AppThemeMode.GLASS_LIGHT) DeepIndigo else NeonCyan

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }
            }
        }

        if (!actionText.isNullOrBlank() && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .springClickable(testTag = actionTestTag, onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// =========================================================================
// 7. TOP HEADER
// =========================================================================

/**
 * Standardized Top Navigation Bar with safe status bar padding,
 * title, optional subtitle, back button, and actions slot.
 */
@Composable
fun GlassTopHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    backTestTag: String = "top_header_back_button",
    actions: @Composable RowScope.() -> Unit = {}
) {
    val isDark = isAppInDarkTheme()
    val themeMode = currentThemeMode()

    val primaryTextColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF0F172A) else Color.White
    val secondaryTextColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF64748B) else Color(0xFF94A3B8)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate Back",
                    onClick = onBackClick,
                    size = 40.dp,
                    testTag = backTestTag
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = actions
        )
    }
}

// =========================================================================
// 8. GLASS MODAL / DIALOG
// =========================================================================

/**
 * Standardized Liquid Glass Dialog overlay.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = "Cancel",
    confirmTestTag: String = "dialog_confirm_button",
    dismissTestTag: String = "dialog_dismiss_button",
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val themeMode = currentThemeMode()
    val primaryTextColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF0F172A) else Color.White
    val secondaryTextColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF64748B) else Color(0xFF94A3B8)
    val accentColor = if (themeMode == AppThemeMode.GLASS_LIGHT) DeepIndigo else NeonCyan

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(26.dp),
                elevation = 16.dp,
                fillAlpha = 0.92f
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (icon != null) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accentColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )
                                if (!subtitle.isNullOrBlank()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = secondaryTextColor
                                    )
                                }
                            }
                        }

                        GlassCloseButton(onClose = onDismissRequest)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Body
                    content()

                    // Action Buttons
                    if (confirmText != null || dismissText != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (dismissText != null) {
                                SecondaryButton(
                                    text = dismissText,
                                    onClick = onDismissRequest,
                                    modifier = Modifier.weight(1f),
                                    testTag = dismissTestTag
                                )
                            }
                            if (confirmText != null && onConfirm != null) {
                                PrimaryButton(
                                    text = confirmText,
                                    onClick = onConfirm,
                                    modifier = Modifier.weight(1f),
                                    testTag = confirmTestTag
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 9. GLASS TOGGLE / SWITCH
// =========================================================================

/**
 * Refined Material 3 Switch with Liquid Glass styling.
 */
@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "glass_toggle"
) {
    val themeMode = currentThemeMode()
    val activeColor = if (themeMode == AppThemeMode.GLASS_LIGHT) DeepIndigo else NeonCyan

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier.testTag(testTag),
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = activeColor,
            uncheckedThumbColor = Color(0xFF94A3B8),
            uncheckedTrackColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFFE2E8F0) else Color(0xFF1E293B),
            uncheckedBorderColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFFCBD5E1) else Color(0x3538BDF8)
        )
    )
}

// =========================================================================
// 10. PROGRESS INDICATORS & LIQUID RING
// =========================================================================

/**
 * Liquid linear progress bar with glowing gradient fill and smooth animation.
 */
@Composable
fun GlassLinearProgressIndicator(
    progress: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    brush: Brush? = null,
    trackColor: Color? = null
) {
    val themeMode = currentThemeMode()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "linear_progress"
    )

    val finalBrush = brush ?: if (themeMode == AppThemeMode.GLASS_LIGHT) {
        Brush.linearGradient(listOf(DeepIndigo, ElectricViolet))
    } else {
        Brush.linearGradient(listOf(NeonCyan, ElectricViolet, NebulaPurple))
    }

    val finalTrack = trackColor ?: if (themeMode == AppThemeMode.GLASS_LIGHT) {
        Color(0xFFE2E8F0)
    } else {
        Color(0x25FFFFFF)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(finalTrack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(height / 2))
                .background(finalBrush)
        )
    }
}

/**
 * Liquid Circular Indicator for async loading or AI processing.
 */
@Composable
fun GlassCircularProgressIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    strokeWidth: Dp = 3.dp,
    color: Color? = null
) {
    val themeMode = currentThemeMode()
    val finalColor = color ?: if (themeMode == AppThemeMode.GLASS_LIGHT) DeepIndigo else NeonCyan

    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = finalColor,
        strokeWidth = strokeWidth,
        strokeCap = StrokeCap.Round
    )
}

/**
 * Circular Liquid Progress Ring for study goals, mock scores, and daily targets.
 */
@Composable
fun LiquidProgressRing(
    progress: Float, // 0.0 to 1.0
    currentText: String,
    targetText: String,
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    strokeWidth: Dp = 12.dp
) {
    val isDark = isAppInDarkTheme()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "progress_ring"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweep = animatedProgress * 360f
            // Background track
            drawArc(
                color = if (isDark) Color(0x25FFFFFF) else Color(0xFFE2E8F0),
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
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = targetText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
    }
}

// =========================================================================
// 11. EMPTY STATE & LOADING STATE
// =========================================================================

/**
 * Standardized Liquid Glass Empty State container with title, description, and action button.
 */
@Composable
fun GlassEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionTestTag: String = "empty_state_action_button"
) {
    val themeMode = currentThemeMode()
    val primaryTextColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF0F172A) else Color.White
    val secondaryTextColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF64748B) else Color(0xFF94A3B8)
    val accentColor = if (themeMode == AppThemeMode.GLASS_LIGHT) DeepIndigo else NeonCyan

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        fillAlpha = 0.5f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            if (!actionText.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = actionText,
                    onClick = onActionClick,
                    modifier = Modifier.widthIn(max = 240.dp),
                    testTag = actionTestTag
                )
            }
        }
    }
}

/**
 * Standardized Glass Loading State with pulse animation.
 */
@Composable
fun GlassLoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    val themeMode = currentThemeMode()
    val secondaryTextColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF64748B) else Color(0xFF94A3B8)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassCircularProgressIndicator(size = 40.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// =========================================================================
// 12. FLOATING GLASS BOTTOM NAVIGATION BAR
// =========================================================================

enum class AppNavTab(val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("Home", Icons.Outlined.Home, Icons.Filled.Home),
    STUDY("Study", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    PRACTICE("Practice", Icons.Outlined.Quiz, Icons.Filled.Quiz),
    UPDATES("Updates", Icons.Outlined.Campaign, Icons.Filled.Campaign),
    PROFILE("Profile", Icons.Outlined.Person, Icons.Filled.Person),
    // Backward compatibility aliases
    AI_TUTOR("Nova AI", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
    FOCUS("Focus", Icons.Outlined.TrackChanges, Icons.Filled.TrackChanges),
    PROGRESS("Practice", Icons.Outlined.Quiz, Icons.Filled.Quiz);

    companion object {
        val primaryTabs = listOf(HOME, STUDY, PRACTICE, UPDATES, PROFILE)
    }
}

@Composable
fun FloatingGlassNavBar(
    currentTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val themeMode = currentThemeMode()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .glassEffect(
                    shape = RoundedCornerShape(33.dp),
                    elevation = if (themeMode == AppThemeMode.AMOLED_BLACK) 2.dp else 12.dp,
                    fillAlpha = if (isDark) 0.88f else 0.92f
                ),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppNavTab.primaryTabs.forEach { tab ->
                    val isSelected = currentTab == tab || 
                        (tab == AppNavTab.PRACTICE && currentTab == AppNavTab.PROGRESS) ||
                        (tab == AppNavTab.HOME && currentTab == AppNavTab.FOCUS) ||
                        (tab == AppNavTab.HOME && currentTab == AppNavTab.AI_TUTOR)
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.06f else 1f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
                        label = "tab_scale"
                    )

                    val activeBg = if (isDark) NeonCyan.copy(alpha = 0.18f) else DeepIndigo.copy(alpha = 0.12f)
                    val activeTint = if (isDark) NeonCyan else DeepIndigo
                    val inactiveTint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isSelected) activeBg else Color.Transparent)
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
                                tint = if (isSelected) activeTint else inactiveTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) (if (isDark) Color.White else DeepIndigo) else inactiveTint,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 13. STREAK, XP & THEME TOGGLE BADGES
// =========================================================================

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
    GlassThemeToggle(
        modifier = modifier,
        testTag = testTag
    )
}

// =========================================================================
// 14. ADDITIONAL PRIMITIVES (BOTTOM SHEET, ERROR STATE, SKELETON, TOAST)
// =========================================================================

/**
 * Standardized Liquid Glass Bottom Sheet container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val themeMode = currentThemeMode()
    val isGlassLight = themeMode == AppThemeMode.GLASS_LIGHT
    val containerColor = when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> Color(0xF0000000)
        AppThemeMode.GLASS_LIGHT -> Color(0xF2FFFFFF)
        AppThemeMode.NOVA_DARK -> Color(0xF00F172A)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = containerColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            if (title != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background((if (isGlassLight) DeepIndigo else NeonCyan).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isGlassLight) DeepIndigo else NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isGlassLight) Color(0xFF0F172A) else Color.White
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isGlassLight) Color(0xFF64748B) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                    GlassCloseButton(onClose = onDismissRequest)
                }
            }
            content()
        }
    }
}

/**
 * Standardized Glass Error State component with retry action.
 */
@Composable
fun GlassErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    onRetry: (() -> Unit)? = null,
    retryTestTag: String = "error_retry_button"
) {
    val themeMode = currentThemeMode()
    val primaryTextColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF0F172A) else Color.White
    val secondaryTextColor = if (themeMode == AppThemeMode.GLASS_LIGHT) Color(0xFF64748B) else Color(0xFF94A3B8)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        fillAlpha = 0.6f,
        borderColor = CoralRose.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(CoralRose.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "Error",
                    tint = CoralRose,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor,
                textAlign = TextAlign.Center
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralRose,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag(retryTestTag)
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Try Again", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Standardized Glass Skeleton placeholder shimmer with Liquid Glass gradient flow.
 */
@Composable
fun Modifier.liquidGlassShimmer(
    shape: Shape = RoundedCornerShape(12.dp),
    durationMillis: Int = 1200
): Modifier {
    val themeMode = currentThemeMode()
    val isLight = themeMode == AppThemeMode.GLASS_LIGHT

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val baseGlassColor = if (isLight) Color(0x18CBD5E1) else Color(0x181E293B)
    val highlightColor = if (isLight) Color(0x35FFFFFF) else Color(0x2838BDF8)
    val borderColor = if (isLight) Color(0x30CBD5E1) else Color(0x2038BDF8)

    val brush = Brush.linearGradient(
        colors = listOf(
            baseGlassColor,
            highlightColor,
            baseGlassColor
        ),
        start = androidx.compose.ui.geometry.Offset(shimmerTranslate - 200f, shimmerTranslate - 200f),
        end = androidx.compose.ui.geometry.Offset(shimmerTranslate + 200f, shimmerTranslate + 200f)
    )

    return this
        .clip(shape)
        .background(brush)
        .border(0.5.dp, borderColor, shape)
}

@Composable
fun GlassSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier.liquidGlassShimmer(shape = shape)
    )
}

/**
 * Skeleton for feature cards (Home & Hubs)
 */
@Composable
fun GlassCardSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 88.dp,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .liquidGlassShimmer(shape = shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassSkeleton(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassSkeleton(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp), shape = RoundedCornerShape(4.dp))
                GlassSkeleton(modifier = Modifier.fillMaxWidth(0.85f).height(12.dp), shape = RoundedCornerShape(4.dp))
            }
        }
    }
}

/**
 * Skeleton for lists (Vacancies, Current Affairs, Results, PYQs)
 */
@Composable
fun GlassListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 3,
    itemHeight: Dp = 92.dp
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(itemCount) {
            GlassCardSkeleton(height = itemHeight)
        }
    }
}

/**
 * Skeleton for Dashboard Statistics / Progress
 */
@Composable
fun GlassProgressSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .liquidGlassShimmer(shape = shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSkeleton(modifier = Modifier.width(130.dp).height(14.dp))
                GlassSkeleton(modifier = Modifier.width(60.dp).height(14.dp))
            }
            GlassSkeleton(modifier = Modifier.fillMaxWidth().height(8.dp), shape = RoundedCornerShape(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassSkeleton(modifier = Modifier.width(70.dp).height(24.dp))
                GlassSkeleton(modifier = Modifier.width(70.dp).height(24.dp))
                GlassSkeleton(modifier = Modifier.width(70.dp).height(24.dp))
            }
        }
    }
}

/**
 * Skeleton for Profile header
 */
@Composable
fun GlassProfileSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp)
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .liquidGlassShimmer(shape = shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassSkeleton(modifier = Modifier.size(56.dp), shape = CircleShape)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassSkeleton(modifier = Modifier.fillMaxWidth(0.5f).height(18.dp))
                GlassSkeleton(modifier = Modifier.fillMaxWidth(0.75f).height(14.dp))
            }
        }
    }
}

/**
 * Skeleton for Article & Current Affairs Card
 */
@Composable
fun GlassArticleSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .liquidGlassShimmer(shape = shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassSkeleton(modifier = Modifier.width(90.dp).height(12.dp))
                GlassSkeleton(modifier = Modifier.width(60.dp).height(12.dp))
            }
            GlassSkeleton(modifier = Modifier.fillMaxWidth(0.9f).height(18.dp))
            GlassSkeleton(modifier = Modifier.fillMaxWidth().height(14.dp))
            GlassSkeleton(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
        }
    }
}

/**
 * Skeleton for Mock Test Card
 */
@Composable
fun GlassMockSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
            .liquidGlassShimmer(shape = shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSkeleton(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
                GlassSkeleton(modifier = Modifier.width(50.dp).height(18.dp), shape = RoundedCornerShape(6.dp))
            }
            GlassSkeleton(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSkeleton(modifier = Modifier.width(100.dp).height(12.dp))
                GlassSkeleton(modifier = Modifier.width(80.dp).height(28.dp), shape = RoundedCornerShape(8.dp))
            }
        }
    }
}

/**
 * Skeleton for Vacancy / Result Details Page
 */
@Composable
fun GlassDetailSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassSkeleton(modifier = Modifier.fillMaxWidth(0.7f).height(24.dp))
        GlassSkeleton(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp))
        GlassProgressSkeleton()
        GlassCardSkeleton(height = 120.dp)
        GlassListSkeleton(itemCount = 3, itemHeight = 70.dp)
    }
}

/**
 * Glass Toast notification banner.
 */
enum class GlassToastType { SUCCESS, WARNING, ERROR, INFO }

@Composable
fun GlassToast(
    message: String,
    type: GlassToastType = GlassToastType.INFO,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val (accentColor, icon) = when (type) {
        GlassToastType.SUCCESS -> EmeraldSuccess to Icons.Filled.CheckCircle
        GlassToastType.WARNING -> AmberWarning to Icons.Filled.Warning
        GlassToastType.ERROR -> CoralRose to Icons.Filled.Error
        GlassToastType.INFO -> NeonCyan to Icons.Filled.Info
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        color = Color(0xFF0F172A).copy(alpha = 0.92f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// GLASS THEME SWITCHER & NIGHT STUDY MODE CONTROLLER
// =========================================================================

/**
 * 1-Tap Quick Theme Toggle Button
 * Displays Sun for Light Mode, Moon for Nova Dark, and Night Sky for AMOLED Black.
 * Tap switches Light <-> Dark, Long-press or click on theme picker opens the full selector dialog.
 */
@Composable
fun GlassThemeToggle(
    modifier: Modifier = Modifier,
    onOpenFullPicker: (() -> Unit)? = null,
    testTag: String = "header_theme_toggle_btn"
) {
    val themeController = LocalThemeController.current
    val currentMode = themeController.themeMode
    val isDark = themeController.isDarkTheme

    val icon = when (currentMode) {
        AppThemeMode.GLASS_LIGHT -> Icons.Filled.WbSunny
        AppThemeMode.NOVA_DARK -> Icons.Filled.DarkMode
        AppThemeMode.AMOLED_BLACK -> Icons.Filled.NightsStay
    }

    val iconTint = when (currentMode) {
        AppThemeMode.GLASS_LIGHT -> Color(0xFFD97706) // Warm Amber
        AppThemeMode.NOVA_DARK -> NeonCyan
        AppThemeMode.AMOLED_BLACK -> ElectricViolet
    }

    val glowColor = when (currentMode) {
        AppThemeMode.GLASS_LIGHT -> Color(0xFFFEF3C7)
        AppThemeMode.NOVA_DARK -> NeonCyan.copy(alpha = 0.2f)
        AppThemeMode.AMOLED_BLACK -> ElectricViolet.copy(alpha = 0.25f)
    }

    IconButton(
        onClick = {
            themeController.toggleTheme()
        },
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isDark) Color(0x1CFFFFFF) else Color(0x10000000))
            .border(
                0.5.dp,
                if (isDark) Color(0x33FFFFFF) else Color(0x2064748B),
                CircleShape
            )
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Theme: ${currentMode.displayName}. Tap to switch mode.",
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Complete Theme & Night-Time Study Session Customization Dialog
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = currentTheme != AppThemeMode.GLASS_LIGHT
    val primaryText = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    if (isDark) Color(0x33FFFFFF) else Color(0x20000000),
                    RoundedCornerShape(24.dp)
                )
                .testTag("theme_selection_dialog"),
            color = if (currentTheme == AppThemeMode.AMOLED_BLACK) Color(0xFF0A0A0A)
                    else if (isDark) Color(0xFF0F172A).copy(alpha = 0.96f)
                    else Color(0xFFFFFFFF).copy(alpha = 0.98f),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Study Theme & Canvas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                            Text(
                                text = "Optimize for day or night sessions",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryText
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = if (isDark) Color(0x1FFFFFFF) else Color(0x10000000),
                    thickness = 0.5.dp
                )

                // Theme Option Cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeOptionCard(
                        title = "Glass Light (Daytime)",
                        subtitle = "Crisp, airy liquid glass with high-contrast text for daylight study.",
                        icon = Icons.Filled.WbSunny,
                        iconTint = Color(0xFFD97706),
                        themeMode = AppThemeMode.GLASS_LIGHT,
                        isSelected = currentTheme == AppThemeMode.GLASS_LIGHT,
                        previewBgColor = Color(0xFFF8FAFC),
                        onSelect = {
                            onSelectTheme(AppThemeMode.GLASS_LIGHT)
                            onDismiss()
                        }
                    )

                    ThemeOptionCard(
                        title = "Nova Dark (Evening Focus)",
                        subtitle = "Deep cosmic indigo gradient designed for focus and reduced blue light.",
                        icon = Icons.Filled.DarkMode,
                        iconTint = NeonCyan,
                        themeMode = AppThemeMode.NOVA_DARK,
                        isSelected = currentTheme == AppThemeMode.NOVA_DARK,
                        previewBgColor = Color(0xFF0F172A),
                        onSelect = {
                            onSelectTheme(AppThemeMode.NOVA_DARK)
                            onDismiss()
                        }
                    )

                    ThemeOptionCard(
                        title = "AMOLED Black (Night Study)",
                        subtitle = "True 0-backlight pure black for zero eye strain & battery efficiency in late night sessions.",
                        icon = Icons.Filled.NightsStay,
                        iconTint = ElectricViolet,
                        themeMode = AppThemeMode.AMOLED_BLACK,
                        isSelected = currentTheme == AppThemeMode.AMOLED_BLACK,
                        previewBgColor = Color(0xFF000000),
                        onSelect = {
                            onSelectTheme(AppThemeMode.AMOLED_BLACK)
                            onDismiss()
                        }
                    )
                }

                // Night Study Tip Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0x1538BDF8) else Color(0x1038BDF8),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        NeonCyan.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💡", fontSize = 14.sp)
                        Text(
                            text = "Tip: Tap the theme icon in the top header at any time for quick 1-tap switching during study sessions.",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    themeMode: AppThemeMode,
    isSelected: Boolean,
    previewBgColor: Color,
    onSelect: () -> Unit
) {
    val isAppDark = isAppInDarkTheme()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .testTag("theme_option_${themeMode.name}"),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            if (isAppDark) Color(0x2238BDF8) else Color(0x1538BDF8)
        } else {
            if (isAppDark) Color(0x12FFFFFF) else Color(0x06000000)
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) NeonCyan else if (isAppDark) Color(0x20FFFFFF) else Color(0x15000000)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Preview Circle
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = previewBgColor,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) NeonCyan else Color(0x33888888)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAppDark) Color.White else Color(0xFF0F172A)
                    )
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonCyan.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAppDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = NeonCyan,
                    unselectedColor = if (isAppDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            )
        }
    }
}


package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.FocusTimerState
import kotlin.math.cos
import kotlin.math.sin

/**
 * REFACTOR: LIQUID GLASS CIRCULAR FOCUS HERO & VINTAGE HOURGLASS TIMER
 *
 * Implements:
 * 1. Glowing outer cyan/electric blue circular progress boundary.
 * 2. Realistic vintage 3D glass hourglass with smooth sand physics & trickling stream.
 * 3. Translucent frosted-glass digital clock overlay (25:00 / MM:SS).
 * 4. Quick Duration Pills: 25m, 45m, 60m with active glass state.
 * 5. Status indicators: [ 🛡️ Shield: Active (X Apps) ] and [ 🔒 Strict: OFF/ON ].
 * 6. Start / Pause / Stop Focus controls.
 */
@Composable
fun VintageHourglassFocusCard(
    focusTimerState: FocusTimerState?,
    selectedDurationMinutes: Int = 25,
    onSelectDuration: (Int) -> Unit = {},
    onStartFocus: (minutes: Int) -> Unit = {},
    onPauseFocus: () -> Unit = {},
    onResumeFocus: () -> Unit = {},
    onStopFocus: () -> Unit = {},
    onOpenAppBlockerSettings: () -> Unit = {},
    isAppShieldActive: Boolean = true,
    isStrictModeActive: Boolean = false,
    onToggleStrictMode: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRunning = focusTimerState?.isRunning == true
    val isPaused = focusTimerState?.isPaused == true
    val totalSeconds = if (isRunning || isPaused) ((focusTimerState?.initialMinutes ?: selectedDurationMinutes) * 60) else (selectedDurationMinutes * 60)
    val remainingSeconds = if (isRunning || isPaused) (focusTimerState?.remainingSeconds ?: totalSeconds) else totalSeconds

    // Progress: 0f (empty/start) to 1f (completed)
    val progress = if (totalSeconds > 0) {
        1f - (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "hourglass_infinite")
    val sandTrickleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sand_trickle"
    )

    val neonGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Formatted time string
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        borderColor = PrimaryCyan.copy(alpha = 0.35f),
        fillAlpha = 0.55f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar: "Hero" Focus Mode + Settings Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryCyan.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = PrimaryCyan,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "\"Hero\" Focus Mode",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (isRunning) "Deep Focus Session Active" else "Boost concentration with Shield & AI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isRunning) EmeraldGreen else TextSecondary
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Focus Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // CIRCULAR PROGRESS & VINTAGE HOURGLASS WITH OVERLAID DIGITAL CLOCK
            // =========================================================================
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Circular Glow & Progress Ring + Vintage Hourglass
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                    val radius = (canvasWidth.coerceAtMost(canvasHeight) / 2f) - 12.dp.toPx()

                    // 1. Ambient Background Ring
                    drawCircle(
                        color = Color(0x2238BDF8),
                        radius = radius,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // 2. Active Glowing Progress Arc
                    val sweepAngle = if (isRunning || isPaused) (progress * 360f).coerceIn(4f, 360f) else 360f
                    val arcBrush = Brush.sweepGradient(
                        colors = listOf(
                            PrimaryCyan.copy(alpha = 0.4f),
                            ElectricBlue,
                            PrimaryCyan,
                            PrimaryCyan.copy(alpha = 0.4f)
                        ),
                        center = center
                    )

                    drawArc(
                        brush = arcBrush,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Progress Indicator Dot
                    if (isRunning || isPaused) {
                        val angleRad = Math.toRadians((sweepAngle - 90.0)).toFloat()
                        val dotX = center.x + radius * cos(angleRad)
                        val dotY = center.y + radius * sin(angleRad)

                        drawCircle(
                            color = PrimaryCyan.copy(alpha = neonGlowAlpha),
                            radius = 9.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.5.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }

                    // 3. VINTAGE 3D GLASS HOURGLASS
                    val hgWidth = 86.dp.toPx()
                    val hgHeight = 130.dp.toPx()
                    val hgLeft = center.x - hgWidth / 2f
                    val hgTop = center.y - hgHeight / 2f

                    drawVintageHourglass(
                        left = hgLeft,
                        top = hgTop,
                        width = hgWidth,
                        height = hgHeight,
                        sandProgress = if (isRunning || isPaused) progress else 0.25f,
                        isTrickling = isRunning,
                        tricklePhase = sandTrickleOffset
                    )
                }

                // 4. OVERLAID FROSTED-GLASS DIGITAL TIMER TEXT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCanvas.copy(alpha = 0.45f))
                        .border(1.dp, PrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            shadow = Shadow(
                                color = PrimaryCyan.copy(alpha = 0.85f),
                                offset = Offset(0f, 0f),
                                blurRadius = 14f
                            )
                        ),
                        textAlign = TextAlign.Center
                    )
                    if (isRunning) {
                        Text(
                            text = "FOCUSING...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = PrimaryCyan
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // QUICK DURATION PILLS: 25m, 45m, 60m
            // =========================================================================
            if (!isRunning && !isPaused) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurface.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(25, 45, 60).forEach { mins ->
                        val isSelected = selectedDurationMinutes == mins
                        val pillBg = if (isSelected) PrimaryCyan.copy(alpha = 0.25f) else Color.Transparent
                        val pillBorder = if (isSelected) PrimaryCyan.copy(alpha = 0.6f) else Color.Transparent
                        val textColor = if (isSelected) PrimaryCyan else TextSecondary

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(pillBg)
                                .border(1.dp, pillBorder, RoundedCornerShape(16.dp))
                                .clickable { onSelectDuration(mins) }
                                .padding(horizontal = 18.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // =========================================================================
            // STATUS INDICATORS: [ 🛡️ Shield: Active ] & [ 🔒 Strict: OFF ]
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shield Status
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = (if (isAppShieldActive) EmeraldGreen else TextSecondary).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        (if (isAppShieldActive) EmeraldGreen else TextSecondary).copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.clickable { onOpenAppBlockerSettings() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isAppShieldActive) EmeraldGreen else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Shield: ",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = if (isAppShieldActive) "Active" else "Disabled",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isAppShieldActive) EmeraldGreen else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Strict Mode Status
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = (if (isStrictModeActive) AmberAlert else DarkSurface).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        (if (isStrictModeActive) AmberAlert else TextSecondary).copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.clickable { onToggleStrictMode() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isStrictModeActive) Icons.Default.Lock else Icons.Outlined.LockOpen,
                            contentDescription = null,
                            tint = if (isStrictModeActive) AmberAlert else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Strict: ",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = if (isStrictModeActive) "ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isStrictModeActive) AmberAlert else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // PRIMARY ACTION BUTTON
            // =========================================================================
            if (!isRunning && !isPaused) {
                Button(
                    onClick = { onStartFocus(selectedDurationMinutes) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = PrimaryCyan),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCyan
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Focus",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START FOCUS SESSION (${selectedDurationMinutes}m)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            } else {
                // Active session controls: Pause / Resume / Stop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { if (isRunning) onPauseFocus() else onResumeFocus() },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) AmberAlert else EmeraldGreen
                        )
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "Pause" else "Resume",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = onStopFocus,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CrimsonRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            tint = CrimsonRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "End Focus",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = CrimsonRed,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas drawing for a Vintage 3D Glass Hourglass with sand physics.
 */
private fun DrawScope.drawVintageHourglass(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    sandProgress: Float, // 0.0 (full upper) to 1.0 (empty upper / full bottom)
    isTrickling: Boolean,
    tricklePhase: Float
) {
    val cx = left + width / 2f
    val cy = top + height / 2f
    val rimHeight = 8.dp.toPx()
    val rimWidth = width * 0.92f
    val waistWidth = 10.dp.toPx()
    val bulbPadding = 6.dp.toPx()

    // 1. Wood/Brass Top & Bottom Caps
    val brassBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF8B6B38),
            Color(0xFFE2C275),
            Color(0xFFFFF3A8),
            Color(0xFFB58E45),
            Color(0xFF5A4420)
        ),
        startX = left,
        endX = left + width
    )

    // Top Cap
    drawRoundRect(
        brush = brassBrush,
        topLeft = Offset(cx - rimWidth / 2f, top),
        size = Size(rimWidth, rimHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )

    // Bottom Cap
    drawRoundRect(
        brush = brassBrush,
        topLeft = Offset(cx - rimWidth / 2f, top + height - rimHeight),
        size = Size(rimWidth, rimHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )

    // Glass Bulb Path
    val glassPath = Path().apply {
        moveTo(cx - (width / 2f - bulbPadding), top + rimHeight)
        // Top bulb curve to center waist
        cubicTo(
            cx - (width / 2f - bulbPadding), top + height * 0.28f,
            cx - waistWidth / 2f, cy - height * 0.1f,
            cx - waistWidth / 2f, cy
        )
        // Center waist to bottom bulb curve
        cubicTo(
            cx - waistWidth / 2f, cy + height * 0.1f,
            cx - (width / 2f - bulbPadding), top + height * 0.72f,
            cx - (width / 2f - bulbPadding), top + height - rimHeight
        )
        // Bottom line
        lineTo(cx + (width / 2f - bulbPadding), top + height - rimHeight)
        // Bottom bulb right curve to waist
        cubicTo(
            cx + (width / 2f - bulbPadding), top + height * 0.72f,
            cx + waistWidth / 2f, cy + height * 0.1f,
            cx + waistWidth / 2f, cy
        )
        // Waist to top bulb right curve
        cubicTo(
            cx + waistWidth / 2f, cy - height * 0.1f,
            cx + (width / 2f - bulbPadding), top + height * 0.28f,
            cx + (width / 2f - bulbPadding), top + rimHeight
        )
        close()
    }

    // Draw Glass Bulb Translucent Fill
    drawPath(
        path = glassPath,
        color = Color(0x18FFFFFF)
    )

    // Sand color palette
    val sandColor = Color(0xFFD4AF37) // Warm golden sand
    val sandShade = Color(0xFF9E782F)

    // 2. Upper Bulb Sand Level (Decreasing as sandProgress -> 1)
    val upperBulbTop = top + rimHeight + 2.dp.toPx()
    val upperBulbHeight = (cy - upperBulbTop) * 0.9f
    val remainingUpperFraction = (1f - sandProgress).coerceIn(0f, 1f)
    val currentUpperSandTop = upperBulbTop + upperBulbHeight * (1f - remainingUpperFraction)

    if (remainingUpperFraction > 0.02f) {
        val upperSandPath = Path().apply {
            moveTo(cx - (width * 0.35f * remainingUpperFraction), currentUpperSandTop)
            lineTo(cx + (width * 0.35f * remainingUpperFraction), currentUpperSandTop)
            cubicTo(
                cx + waistWidth * 0.8f, cy - 6.dp.toPx(),
                cx + waistWidth * 0.6f, cy - 2.dp.toPx(),
                cx, cy
            )
            cubicTo(
                cx - waistWidth * 0.6f, cy - 2.dp.toPx(),
                cx - waistWidth * 0.8f, cy - 6.dp.toPx(),
                cx - (width * 0.35f * remainingUpperFraction), currentUpperSandTop
            )
            close()
        }

        drawPath(
            path = upperSandPath,
            brush = Brush.verticalGradient(
                colors = listOf(sandColor, sandShade),
                startY = currentUpperSandTop,
                endY = cy
            )
        )
    }

    // 3. Falling Sand Stream & Trickle Particles
    if (isTrickling && remainingUpperFraction > 0.01f) {
        drawLine(
            color = sandColor.copy(alpha = 0.9f),
            start = Offset(cx, cy),
            end = Offset(cx, top + height - rimHeight - 10.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Animated falling grains
        val grain1Y = cy + (top + height - rimHeight - cy) * tricklePhase
        val grain2Y = cy + (top + height - rimHeight - cy) * ((tricklePhase + 0.5f) % 1f)
        drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 1.2.dp.toPx(), center = Offset(cx, grain1Y))
        drawCircle(color = sandColor, radius = 1.4.dp.toPx(), center = Offset(cx, grain2Y))
    }

    // 4. Lower Bulb Sand Mound (Expanding as sandProgress -> 1)
    val lowerBulbBottom = top + height - rimHeight
    val bottomSandHeight = (lowerBulbBottom - cy) * 0.85f * sandProgress.coerceIn(0.08f, 1f)
    val lowerSandTop = lowerBulbBottom - bottomSandHeight

    val lowerSandPath = Path().apply {
        moveTo(cx - (width * 0.38f * sandProgress.coerceIn(0.3f, 1f)), lowerBulbBottom)
        // Mound cone peak
        cubicTo(
            cx - width * 0.15f, lowerSandTop + 4.dp.toPx(),
            cx - width * 0.05f, lowerSandTop,
            cx, lowerSandTop
        )
        cubicTo(
            cx + width * 0.05f, lowerSandTop,
            cx + width * 0.15f, lowerSandTop + 4.dp.toPx(),
            cx + (width * 0.38f * sandProgress.coerceIn(0.3f, 1f)), lowerBulbBottom
        )
        close()
    }

    drawPath(
        path = lowerSandPath,
        brush = Brush.verticalGradient(
            colors = listOf(sandColor, sandShade),
            startY = lowerSandTop,
            endY = lowerBulbBottom
        )
    )

    // 5. Glass Reflections & Outer Outline Stroke
    drawPath(
        path = glassPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x90FFFFFF),
                Color(0x2038BDF8),
                Color(0x50FFFFFF)
            ),
            startY = top,
            endY = top + height
        ),
        style = Stroke(width = 1.8.dp.toPx())
    )

    // Glass Highlight Sheen (Left curvature)
    val sheenPath = Path().apply {
        moveTo(cx - width * 0.32f, top + rimHeight + 6.dp.toPx())
        cubicTo(
            cx - width * 0.32f, top + height * 0.25f,
            cx - waistWidth * 0.8f, cy - 8.dp.toPx(),
            cx - waistWidth * 0.7f, cy
        )
    }
    drawPath(
        path = sheenPath,
        color = Color(0x60FFFFFF),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

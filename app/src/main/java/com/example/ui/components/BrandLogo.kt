package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * High-fidelity 3D Vector Emblem matching the user's StudyMate AI branding:
 * - Glowing 3D "S"
 * - Mortarboard (Academic Graduation Cap) with Golden Tassel
 * - Radiant Open Book at bottom
 * - Cute AI Robot Mascot
 * - Floating study sparkles & orbital rings
 * - Brand Typography: StudyMate AI + "LEARN • FOCUS • ACHIEVE"
 */
@Composable
fun StudyMateBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    showTypography: Boolean = true,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val pulseGlow by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_glow"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val floatOffset by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "float_offset"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Column(
        modifier = modifier.testTag("studymate_brand_logo"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main 3D Bubble Container
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(38.dp),
                    spotColor = NeonCyan.copy(alpha = 0.5f * pulseGlow)
                )
                .clip(RoundedCornerShape(38.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0F1E3D),
                            Color(0xFF0A1128),
                            Color(0xFF060B18)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.8f * pulseGlow),
                            ElectricViolet.copy(alpha = 0.6f),
                            Color(0x30FFFFFF)
                        )
                    ),
                    shape = RoundedCornerShape(38.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Canvas with orbital lines, sparkles, floating study icons
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height

                // Outer Glowing Blue Arc Ring
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(NeonCyan.copy(alpha = 0.5f), ElectricViolet.copy(alpha = 0.5f), Color.Transparent, NeonCyan.copy(alpha = 0.5f))
                    ),
                    startAngle = 30f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(w * 0.08f, h * 0.08f),
                    size = Size(w * 0.84f, h * 0.84f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Golden Glow Star (Top Left)
                val starPath = Path().apply {
                    val cx = w * 0.22f
                    val cy = h * 0.40f
                    val r = w * 0.07f
                    moveTo(cx, cy - r)
                    quadraticTo(cx, cy, cx + r, cy)
                    quadraticTo(cx, cy, cx, cy + r)
                    quadraticTo(cx, cy, cx - r, cy)
                    quadraticTo(cx, cy, cx, cy - r)
                    close()
                }
                drawPath(starPath, brush = Brush.radialGradient(listOf(GoldenSpark, Color(0xFFF59E0B), Color.Transparent)))

                // Small Star Sparkle (Top Right)
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.8f),
                    radius = 3.dp.toPx(),
                    center = Offset(w * 0.78f, h * 0.16f)
                )

                // Floating Corner Study Badges (Notebook icon box top-left, Chart top-right, Clock bottom-left, Checklist bottom-right)
                val badgeBorder = Color(0x3538BDF8)
                val badgeBg = Color(0x1838BDF8)
                
                // Top-Left Badge (Notebook)
                drawRoundRect(
                    color = badgeBg,
                    topLeft = Offset(w * 0.09f, h * 0.20f),
                    size = Size(w * 0.14f, h * 0.14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
                drawRoundRect(
                    color = badgeBorder,
                    topLeft = Offset(w * 0.09f, h * 0.20f),
                    size = Size(w * 0.14f, h * 0.14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Top-Right Badge (Analytics Chart)
                drawRoundRect(
                    color = badgeBg,
                    topLeft = Offset(w * 0.77f, h * 0.22f),
                    size = Size(w * 0.14f, h * 0.14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
                drawRoundRect(
                    color = badgeBorder,
                    topLeft = Offset(w * 0.77f, h * 0.22f),
                    size = Size(w * 0.14f, h * 0.14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Bottom-Left Badge (Clock)
                drawRoundRect(
                    color = badgeBg,
                    topLeft = Offset(w * 0.09f, h * 0.55f),
                    size = Size(w * 0.14f, h * 0.14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
                drawRoundRect(
                    color = badgeBorder,
                    topLeft = Offset(w * 0.09f, h * 0.55f),
                    size = Size(w * 0.14f, h * 0.14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Bottom-Right Badge (Checklist)
                drawRoundRect(
                    color = badgeBg,
                    topLeft = Offset(w * 0.77f, h * 0.55f),
                    size = Size(w * 0.14f, h * 0.14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
                drawRoundRect(
                    color = badgeBorder,
                    topLeft = Offset(w * 0.77f, h * 0.55f),
                    size = Size(w * 0.14f, h * 0.14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // --- 1. Luminous Open Book (Bottom Pedestal) ---
                val bookY = h * 0.64f
                val bookPathLeft = Path().apply {
                    moveTo(w * 0.50f, bookY + h * 0.08f)
                    cubicTo(w * 0.40f, bookY - h * 0.04f, w * 0.25f, bookY - h * 0.03f, w * 0.14f, bookY + h * 0.04f)
                    lineTo(w * 0.14f, bookY + h * 0.11f)
                    cubicTo(w * 0.25f, bookY + h * 0.05f, w * 0.40f, bookY + h * 0.05f, w * 0.50f, bookY + h * 0.16f)
                    close()
                }
                drawPath(
                    bookPathLeft,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFE0F2FE), NeonCyan, DeepIndigo)
                    )
                )

                val bookPathRight = Path().apply {
                    moveTo(w * 0.50f, bookY + h * 0.08f)
                    cubicTo(w * 0.60f, bookY - h * 0.04f, w * 0.75f, bookY - h * 0.03f, w * 0.86f, bookY + h * 0.04f)
                    lineTo(w * 0.86f, bookY + h * 0.11f)
                    cubicTo(w * 0.75f, bookY + h * 0.05f, w * 0.60f, bookY + h * 0.05f, w * 0.50f, bookY + h * 0.16f)
                    close()
                }
                drawPath(
                    bookPathRight,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFE0F2FE), NeonCyan, DeepIndigo)
                    )
                )

                // Golden radiant light beam between book pages
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(GoldenSpark, Color(0xFFF59E0B).copy(alpha = 0.5f), Color.Transparent),
                        radius = w * 0.14f
                    ),
                    center = Offset(w * 0.50f, bookY + h * 0.08f)
                )

                // --- 2. Central 3D Glowing "S" ---
                val sPath = Path().apply {
                    val sx = w * 0.50f
                    val sy = h * 0.42f
                    // Top loop of S
                    moveTo(sx + w * 0.14f, sy - h * 0.12f)
                    cubicTo(sx + w * 0.14f, sy - h * 0.22f, sx - w * 0.15f, sy - h * 0.22f, sx - w * 0.15f, sy - h * 0.10f)
                    cubicTo(sx - w * 0.15f, sy - h * 0.01f, sx + w * 0.16f, sy + h * 0.04f, sx + w * 0.16f, sy + h * 0.14f)
                    cubicTo(sx + w * 0.16f, sy + h * 0.25f, sx - w * 0.16f, sy + h * 0.25f, sx - w * 0.16f, sy + h * 0.15f)
                    // Inner thickness curve
                    lineTo(sx - w * 0.08f, sy + h * 0.15f)
                    cubicTo(sx - w * 0.08f, sy + h * 0.20f, sx + w * 0.08f, sy + h * 0.20f, sx + w * 0.08f, sy + h * 0.14f)
                    cubicTo(sx + w * 0.08f, sy + h * 0.06f, sx - w * 0.08f, sy + h * 0.01f, sx - w * 0.08f, sy - h * 0.10f)
                    cubicTo(sx - w * 0.08f, sy - h * 0.16f, sx + w * 0.08f, sy - h * 0.16f, sx + w * 0.08f, sy - h * 0.12f)
                    close()
                }

                // S outer glow stroke
                drawPath(
                    sPath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFA855F7), Color(0xFF38BDF8))
                    ),
                    style = Fill
                )
                drawPath(
                    sPath,
                    color = Color.White.copy(alpha = 0.7f),
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // --- 3. Academic Graduation Cap (Mortarboard) on top of "S" ---
                val capTopPath = Path().apply {
                    val capY = h * 0.20f
                    moveTo(w * 0.50f, capY - h * 0.09f) // Top apex
                    lineTo(w * 0.74f, capY - h * 0.02f) // Right corner
                    lineTo(w * 0.50f, capY + h * 0.05f) // Bottom point
                    lineTo(w * 0.26f, capY - h * 0.02f) // Left corner
                    close()
                }
                drawPath(
                    capTopPath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1E1B4B))
                    )
                )
                drawPath(
                    capTopPath,
                    brush = Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.8f), Color.White.copy(alpha = 0.9f), Color(0xFF6366F1))),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Cap skull cap base
                val capBase = Path().apply {
                    val capY = h * 0.20f
                    moveTo(w * 0.36f, capY + h * 0.01f)
                    cubicTo(w * 0.36f, capY + h * 0.07f, w * 0.64f, capY + h * 0.07f, w * 0.64f, capY + h * 0.01f)
                    close()
                }
                drawPath(capBase, color = Color(0xFF0F172A))
                drawPath(capBase, color = NeonCyan.copy(alpha = 0.7f), style = Stroke(width = 1.dp.toPx()))

                // Golden Tassel Ribbon & Hanging Fringe
                val tasselPath = Path().apply {
                    val capY = h * 0.20f
                    moveTo(w * 0.50f, capY - h * 0.04f)
                    quadraticTo(w * 0.68f, capY - h * 0.01f, w * 0.72f, capY + h * 0.09f)
                }
                drawPath(
                    tasselPath,
                    color = GoldenSpark,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Golden Tassel Pom-pom
                drawCircle(
                    color = GoldenSpark,
                    radius = 4.dp.toPx(),
                    center = Offset(w * 0.72f, h * 0.29f)
                )
                // Hanging Tassel skirt
                val tasselSkirt = Path().apply {
                    moveTo(w * 0.72f, h * 0.29f)
                    lineTo(w * 0.68f, h * 0.37f)
                    lineTo(w * 0.76f, h * 0.37f)
                    close()
                }
                drawPath(tasselSkirt, brush = Brush.verticalGradient(listOf(GoldenSpark, Color(0xFFF59E0B))))

                // --- 4. Cute AI Robot Mascot (Right of S) ---
                val botCenterX = w * 0.76f
                val botCenterY = h * 0.46f
                val botRadius = w * 0.10f

                // Robot Head Sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF334155))
                    ),
                    radius = botRadius,
                    center = Offset(botCenterX, botCenterY)
                )
                drawCircle(
                    color = NeonCyan,
                    radius = botRadius,
                    center = Offset(botCenterX, botCenterY),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Robot Antenna
                drawLine(
                    color = NeonCyan,
                    start = Offset(botCenterX, botCenterY - botRadius),
                    end = Offset(botCenterX, botCenterY - botRadius - 5.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = NeonCyan,
                    radius = 2.5.dp.toPx(),
                    center = Offset(botCenterX, botCenterY - botRadius - 5.dp.toPx())
                )

                // Robot Ears / Headphones
                drawRoundRect(
                    color = Color(0xFF64748B),
                    topLeft = Offset(botCenterX - botRadius - 3.dp.toPx(), botCenterY - 4.dp.toPx()),
                    size = Size(4.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF64748B),
                    topLeft = Offset(botCenterX + botRadius - 1.dp.toPx(), botCenterY - 4.dp.toPx()),
                    size = Size(4.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )

                // Robot Visor Screen
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(botCenterX - botRadius * 0.65f, botCenterY - botRadius * 0.45f),
                    size = Size(botRadius * 1.3f, botRadius * 0.9f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )

                // Glowing Happy Smile Eyes (Curved Arcs)
                drawArc(
                    color = NeonCyan,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(botCenterX - botRadius * 0.45f, botCenterY - botRadius * 0.25f),
                    size = Size(botRadius * 0.35f, botRadius * 0.35f),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = NeonCyan,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(botCenterX + botRadius * 0.10f, botCenterY - botRadius * 0.25f),
                    size = Size(botRadius * 0.35f, botRadius * 0.35f),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        if (showTypography) {
            Spacer(modifier = Modifier.height(14.dp))

            // Title: StudyMate AI ✨
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "StudyMate AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "✦",
                    color = GoldenSpark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline: — LEARN • FOCUS • ACHIEVE —
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.linearGradient(listOf(Color.Transparent, NeonCyan.copy(alpha = 0.7f)))
                        )
                )

                Text(
                    text = "  LEARN  •  FOCUS  •  ACHIEVE  ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1),
                    letterSpacing = 1.5.sp
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.7f), Color.Transparent))
                        )
                )
            }
        }
    }
}

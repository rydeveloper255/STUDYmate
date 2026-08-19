package com.example.ui.screens.nova

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.NovaVoiceState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.CyberPink

@Composable
fun NovaOrbVisualizer(
    voiceState: NovaVoiceState,
    audioRms: Float,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nova_orb_transition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val activeRms = if (voiceState == NovaVoiceState.LISTENING || voiceState == NovaVoiceState.SPEAKING) {
        audioRms.coerceIn(0.1f, 1.0f)
    } else {
        0.05f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = this.size.minDimension / 2.6f

            // Outer reactive glow ring
            val outerRadius = baseRadius * (1f + activeRms * 0.45f) * pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.35f),
                        ElectricIndigo.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius * 1.3f
                ),
                radius = outerRadius * 1.3f,
                center = center
            )

            // Middle energy wave
            val middleRadius = baseRadius * (1f + activeRms * 0.2f)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.8f),
                        CyberPink.copy(alpha = 0.7f),
                        ElectricIndigo.copy(alpha = 0.8f),
                        NeonCyan.copy(alpha = 0.8f)
                    ),
                    center = center
                ),
                radius = middleRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )

            // Inner solid core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        NeonCyan,
                        ElectricIndigo
                    ),
                    center = center,
                    radius = baseRadius * 0.7f
                ),
                radius = baseRadius * 0.7f,
                center = center
            )
        }
    }
}

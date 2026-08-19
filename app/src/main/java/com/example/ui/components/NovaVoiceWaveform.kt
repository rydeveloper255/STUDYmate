package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NebulaPurple
import com.example.ui.theme.NeonCyan
import kotlin.math.sin

/**
 * Reusable Compose audio visualizer component using basic animation primitives.
 * Displays dynamic animated waveform bars when NOVA is processing or speaking.
 *
 * @param isActive Whether the voice engine is actively speaking or processing.
 * @param isProcessing Whether NOVA is in the "thinking/processing" state vs speaking.
 * @param audioLevel Optional RMS amplitude level (0.0 to 1.0) to dynamically scale bar heights.
 * @param modifier Layout modifier.
 * @param barCount Number of animated bars in the visualizer (default 5).
 * @param minBarHeight Minimum resting height of each bar.
 * @param maxBarHeight Maximum peak height of animated bars.
 * @param barWidth Width of individual waveform bars.
 * @param barSpacing Horizontal space between bars.
 * @param gradient Color gradient applied across the waveform.
 */
@Composable
fun NovaVoiceWaveform(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    isProcessing: Boolean = false,
    audioLevel: Float = 0f,
    barCount: Int = 5,
    minBarHeight: Dp = 6.dp,
    maxBarHeight: Dp = 32.dp,
    barWidth: Dp = 4.dp,
    barSpacing: Dp = 4.dp,
    gradient: Brush = Brush.verticalGradient(
        listOf(NeonCyan, ElectricViolet, NebulaPurple)
    ),
    inactiveColor: Color = Color.White.copy(alpha = 0.25f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nova_waveform_anim")

    // Phase animation for rhythmic undulating wave
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isProcessing) 1400 else 850,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform_phase"
    )

    // Pulse animation for thinking/processing shimmer
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveform_pulse"
    )

    val clampedRms = audioLevel.coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .testTag("nova_voice_waveform")
            .semantics {
                contentDescription = if (isProcessing) "NOVA is processing"
                else if (isActive) "NOVA is speaking"
                else "NOVA voice idle"
            },
        horizontalArrangement = Arrangement.spacedBy(barSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / (barCount - 1).coerceAtLeast(1)
            // Symmetrical bell-curve multiplier so center bars reach higher peaks
            val centerWeight = 1f - (2f * kotlin.math.abs(normalizedIndex - 0.5f)) * 0.45f

            val animatedFraction = if (!isActive) {
                0f
            } else if (isProcessing) {
                // Gentle traveling sinusoidal pulse during processing
                val wave = (sin(phase + i * 0.9f) + 1f) / 2f
                (0.2f + 0.5f * wave) * centerWeight
            } else {
                // Dynamic audio-reactive wave with harmonic oscillations
                val wave1 = (sin(phase + i * 1.1f) + 1f) / 2f
                val wave2 = (sin(phase * 1.6f + i * 0.6f) + 1f) / 2f
                val combinedWave = (wave1 * 0.6f + wave2 * 0.4f)
                val reactiveBoost = clampedRms * 0.75f
                ((0.15f + combinedWave * 0.55f + reactiveBoost) * centerWeight).coerceIn(0f, 1f)
            }

            // Interpolate height between min and max
            val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * animatedFraction

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (isActive) gradient else Brush.linearGradient(listOf(inactiveColor, inactiveColor)),
                        alpha = if (isProcessing) pulseAlpha else 1.0f
                    )
            )
        }
    }
}

/**
 * Compact pill-shaped voice indicator badge with animated waveform.
 */
@Composable
fun NovaVoiceWaveformBadge(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    isProcessing: Boolean = false,
    audioLevel: Float = 0f,
    statusText: String? = null
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0x2B1E293B))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NovaVoiceWaveform(
                isActive = isActive,
                isProcessing = isProcessing,
                audioLevel = audioLevel,
                barCount = 4,
                minBarHeight = 4.dp,
                maxBarHeight = 16.dp,
                barWidth = 3.dp,
                barSpacing = 3.dp
            )
            if (!statusText.isNullOrBlank()) {
                androidx.compose.material3.Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) NeonCyan else Color(0xFF94A3B8)
                )
            }
        }
    }
}

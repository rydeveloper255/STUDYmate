package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Base Dark Canvas
val BackgroundDark = Color(0xFF090D16)
val SurfaceDark = Color(0xFF111827)
val CardSurfaceDark = Color(0xFF161F30)

val DarkCanvas = Color(0xFF090D16)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceElevated = Color(0xFF161F30)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Base Light Canvas
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val CardSurfaceLight = Color(0xFFF1F5F9)

// Brand Core Accents
val NeonCyan = Color(0xFF38BDF8)
val ElectricViolet = Color(0xFF818CF8)
val DeepIndigo = Color(0xFF6366F1)
val ElectricIndigo = Color(0xFF6366F1)
val NebulaPurple = Color(0xFFA855F7)
val VibrantMagenta = Color(0xFFA855F7)
val CyberPink = Color(0xFFEC4899)
val GoldenSpark = Color(0xFFFBBF24)
val CyberAmber = Color(0xFFFBBF24)
val AmberGold = Color(0xFFFBBF24)
val EmeraldSuccess = Color(0xFF10B981)
val EmeraldGreen = Color(0xFF10B981)
val MatrixGreen = Color(0xFF10B981)
val NeonGreen = Color(0xFF10B981)
val CoralRose = Color(0xFFF43F5E)
val CoralPink = Color(0xFFF43F5E)

// Liquid Glass Translucencies
val GlassFillDark = Color(0x241E293B)
val GlassFillLight = Color(0xCCFFFFFF)
val GlassBorderDark = Color(0x38FFFFFF)
val GlassBorderLight = Color(0x3064748B)

// Gradients
val GlassGradientPrimary = Brush.linearGradient(
    colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFA855F7))
)

val GlassCardGradientDark = Brush.linearGradient(
    colors = listOf(Color(0x2B38BDF8), Color(0x15818CF8), Color(0x10000000))
)

val GlassBorderGradient = Brush.linearGradient(
    colors = listOf(Color(0x55FFFFFF), Color(0x15FFFFFF), Color(0x4038BDF8))
)

val DarkBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF090D16),
        Color(0xFF0E1322),
        Color(0xFF141A2E)
    )
)

val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF8FAFC),
        Color(0xFFEEF2F7),
        Color(0xFFE2E8F0)
    )
)

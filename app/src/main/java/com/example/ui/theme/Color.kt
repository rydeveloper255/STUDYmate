package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// Base Background Canvases
// ==========================================
// Nova Dark (Default Deep Navy / Near-Black)
val BackgroundDark = Color(0xFF070B19)
val SurfaceDark = Color(0xFF0F172A)
val CardSurfaceDark = Color(0xFF131C2E)
val CardSurfaceDarkElevated = Color(0xFF182238)

val DarkCanvas = Color(0xFF070B19)
val DarkSurface = Color(0xFF0F172A)
val DarkSurfaceElevated = Color(0xFF131C2E)

// AMOLED Deep Black
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0A0A)
val AmoledCardSurface = Color(0xFF121212)
val AmoledCardSurfaceElevated = Color(0xFF1A1A1A)

// Glass Light (Frosted Daylight)
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val CardSurfaceLight = Color(0xFFF1F5F9)
val CardSurfaceLightElevated = Color(0xFFFFFFFF)

// ==========================================
// Typography & Text Colors
// ==========================================
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF94A3B8)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// ==========================================
// Semantic Accent & Brand Colors
// ==========================================
// Primary Accent: Cyan / Electric Blue
val NeonCyan = Color(0xFF38BDF8)
val PrimaryCyan = Color(0xFF38BDF8)
val ElectricBlue = Color(0xFF0284C7)
val SkyAccent = Color(0xFF0EA5E9)

// Secondary Accent: Violet / Purple
val ElectricViolet = Color(0xFF818CF8)
val DeepIndigo = Color(0xFF6366F1)
val ElectricIndigo = Color(0xFF6366F1)
val NebulaPurple = Color(0xFFA855F7)
val VibrantMagenta = Color(0xFFA855F7)
val CyberPink = Color(0xFFEC4899)

// Semantic Status Colors
// Success: Emerald Green
val EmeraldSuccess = Color(0xFF10B981)
val EmeraldGreen = Color(0xFF10B981)
val MatrixGreen = Color(0xFF10B981)
val NeonGreen = Color(0xFF10B981)
val SemanticSuccess = Color(0xFF10B981)

// Warning / Attention: Amber & Gold
val GoldenSpark = Color(0xFFFBBF24)
val CyberAmber = Color(0xFFFBBF24)
val AmberAlert = Color(0xFFF59E0B)
val AmberGold = Color(0xFFFBBF24)
val SemanticWarning = Color(0xFFF59E0B)

// Error / Danger: Coral Rose
val CoralRose = Color(0xFFF43F5E)
val CoralPink = Color(0xFFF43F5E)
val SemanticError = Color(0xFFF43F5E)

// ==========================================
// Liquid Glass Translucencies & Surfaces
// ==========================================
val GlassFillDark = Color(0x28131C2E)
val GlassFillAmoled = Color(0x33121212)
val GlassFillLight = Color(0xB8FFFFFF)

val GlassBorderDark = Color(0x28FFFFFF)
val GlassBorderAmoled = Color(0x20FFFFFF)
val GlassBorderLight = Color(0x40CBD5E1)

// Subtle Glow Accents (low alpha, non-gaming)
val SoftCyanGlow = Color(0x2238BDF8)
val SoftVioletGlow = Color(0x22818CF8)
val SoftAmberGlow = Color(0x22FBBF24)
val SoftEmeraldGlow = Color(0x2210B981)

// ==========================================
// Atmospheric Gradients
// ==========================================
val GlassGradientPrimary = Brush.linearGradient(
    colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFA855F7))
)

val GlassCardGradientDark = Brush.linearGradient(
    colors = listOf(Color(0x2538BDF8), Color(0x10818CF8), Color(0x05000000))
)

val GlassBorderGradient = Brush.linearGradient(
    colors = listOf(Color(0x55FFFFFF), Color(0x15FFFFFF), Color(0x4038BDF8))
)

val DarkBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF070B19),
        Color(0xFF0D1427),
        Color(0xFF111A33)
    )
)

val AmoledBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF000000),
        Color(0xFF050505),
        Color(0xFF0A0A0A)
    )
)

val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF8FAFC),
        Color(0xFFF1F5F9),
        Color(0xFFE2E8F0)
    )
)

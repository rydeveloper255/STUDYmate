package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class ThemeController(
    val isDarkTheme: Boolean,
    val toggleTheme: () -> Unit = {},
    val setDarkTheme: (Boolean) -> Unit = {}
)

val LocalThemeController = staticCompositionLocalOf {
    ThemeController(isDarkTheme = true)
}

@Composable
fun isAppInDarkTheme(): Boolean {
    return LocalThemeController.current.isDarkTheme
}

@Composable
fun appBackgroundGradient(isDark: Boolean = isAppInDarkTheme()): Brush {
    return if (isDark) DarkBackgroundGradient else LightBackgroundGradient
}

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00364A),
    primaryContainer = Color(0xFF004D68),
    onPrimaryContainer = Color(0xFFC2E8FF),
    secondary = ElectricViolet,
    onSecondary = Color(0xFF1E1066),
    secondaryContainer = Color(0xFF312586),
    onSecondaryContainer = Color(0xFFE2DFFF),
    tertiary = NebulaPurple,
    onTertiary = Color(0xFF4C006C),
    background = BackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = CoralRose
)

private val LightColorScheme = lightColorScheme(
    primary = DeepIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF581C87),
    tertiary = NeonCyan,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = SurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    error = CoralRose
)

@Composable
fun StudyMateTheme(
    darkTheme: Boolean = true,
    onToggleTheme: (() -> Unit)? = null,
    onSetTheme: ((Boolean) -> Unit)? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeController = remember(darkTheme) {
        ThemeController(
            isDarkTheme = darkTheme,
            toggleTheme = { onToggleTheme?.invoke() },
            setDarkTheme = { onSetTheme?.invoke(it) }
        )
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalThemeController provides themeController) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

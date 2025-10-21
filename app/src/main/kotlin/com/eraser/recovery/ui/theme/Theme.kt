package com.eraser.recovery.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light Color Scheme
 * Matches Flutter app's light theme
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = SecondaryDark,
    
    tertiary = Accent,
    onTertiary = Color.White,
    tertiaryContainer = AccentLight,
    onTertiaryContainer = AccentDark,
    
    error = Error,
    onError = Color.White,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorDark,
    
    background = BackgroundLight,
    onBackground = TextPrimary,
    
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextSecondary,
    
    outline = BorderLight,
    outlineVariant = DividerLight,
)

/**
 * Dark Color Scheme
 * Matches Flutter app's dark theme
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.Black,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    
    secondary = SecondaryLight,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = SecondaryLight,
    
    tertiary = AccentLight,
    onTertiary = Color.Black,
    tertiaryContainer = AccentDark,
    onTertiaryContainer = AccentLight,
    
    error = ErrorLight,
    onError = Color.Black,
    errorContainer = ErrorDark,
    onErrorContainer = ErrorLight,
    
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,
    
    outline = BorderDark,
    outlineVariant = DividerDark,
)

/**
 * Eraser Theme
 * 
 * Main theme composable for the app.
 * Supports light/dark mode and Material You dynamic colors on Android 12+.
 * 
 * Research findings from Qustodio, Net Nanny, Bark:
 * - Support system theme preference
 * - Use Material 3 for modern design
 * - Implement edge-to-edge display
 * - Match status bar colors to theme
 */
@Composable
fun EraserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled by default to match Flutter app colors
    content: @Composable () -> Unit
) {
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
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


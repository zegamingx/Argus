package com.argus.agi.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ArgusColorScheme = darkColorScheme(
    primary = ArgusCyanPrimary,
    onPrimary = ArgusDarkBg,
    primaryContainer = ArgusSurfaceVariant,
    onPrimaryContainer = ArgusCyanGlow,
    secondary = ArgusBlueAccent,
    onSecondary = ArgusDarkBg,
    secondaryContainer = ArgusSapphire,
    onSecondaryContainer = ArgusTextPrimary,
    background = ArgusDarkBg,
    onBackground = ArgusTextPrimary,
    surface = ArgusSurface,
    onSurface = ArgusTextPrimary,
    surfaceVariant = ArgusSurfaceVariant,
    onSurfaceVariant = ArgusTextSecondary,
    error = ArgusError,
    onError = ArgusDarkBg
)

@Composable
fun ArgusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = ArgusColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

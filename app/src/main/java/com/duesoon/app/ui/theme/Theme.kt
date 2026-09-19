package com.duesoon.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.duesoon.app.domain.model.AccentPalette
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = LightSurface, 
    onBackground = DarkPrimaryText,
    onSurface = DarkPrimaryText,
    onSurfaceVariant = DarkSecondaryText,
    outlineVariant = DarkDivider,
    error = Error
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightSurface, 
    onBackground = LightPrimaryText,
    onSurface = LightPrimaryText,
    onSurfaceVariant = LightSecondaryText,
    outlineVariant = LightDivider,
    error = Error
)

@Composable
fun DueSoonTheme(
    accentPalette: AccentPalette = AccentPalette.INDIGO,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val accentColor = Color(accentPalette.primaryHex)
    val colorScheme = when {
        darkTheme -> DarkColorScheme.copy(
            primary = accentColor,
            onPrimary = LightSurface,
            primaryContainer = accentColor.copy(alpha = 0.2f),
            secondary = accentColor
        )
        else -> LightColorScheme.copy(
            primary = accentColor,
            onPrimary = LightSurface,
            primaryContainer = accentColor.copy(alpha = 0.2f),
            secondary = accentColor
        )
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

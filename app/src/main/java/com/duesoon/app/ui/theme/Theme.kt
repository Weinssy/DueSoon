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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.duesoon.app.domain.model.AccentPalette

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

private val CampusPaperLightColorScheme = lightColorScheme(
    primary = CampusPaperColors.LightTerracotta,
    onPrimary = Color.White,
    primaryContainer = CampusPaperColors.LightTerracottaTint,
    onPrimaryContainer = CampusPaperColors.LightTerracottaHover,
    secondary = CampusPaperColors.LightSage,
    onSecondary = Color.White,
    secondaryContainer = CampusPaperColors.LightSageTint,
    onSecondaryContainer = CampusPaperColors.LightSageHover,
    tertiary = CampusPaperColors.LightSage,
    onTertiary = Color.White,
    tertiaryContainer = CampusPaperColors.LightSageTint,
    onTertiaryContainer = CampusPaperColors.LightSageHover,
    background = CampusPaperColors.LightPaper,
    onBackground = CampusPaperColors.LightInk,
    surface = CampusPaperColors.LightCard,
    onSurface = CampusPaperColors.LightInk,
    surfaceVariant = CampusPaperColors.LightInset,
    onSurfaceVariant = CampusPaperColors.LightMuted,
    surfaceContainerLowest = CampusPaperColors.LightCard,
    surfaceContainerLow = CampusPaperColors.LightInset,
    surfaceContainer = CampusPaperColors.LightInset,
    outline = CampusPaperColors.LightRuleStrong,
    outlineVariant = CampusPaperColors.LightRule,
    error = Color(0xFF9E221B)
)

private val CampusPaperDarkColorScheme = darkColorScheme(
    primary = CampusPaperColors.DarkTerracotta,
    onPrimary = Color(0xFF2B0B06),
    primaryContainer = CampusPaperColors.DarkTerracottaContainer,
    onPrimaryContainer = CampusPaperColors.DarkTerracottaOnContainer,
    secondary = CampusPaperColors.DarkSage,
    onSecondary = Color(0xFF06251C),
    secondaryContainer = CampusPaperColors.DarkSageContainer,
    onSecondaryContainer = CampusPaperColors.DarkSageOnContainer,
    tertiary = CampusPaperColors.DarkSage,
    onTertiary = Color(0xFF06251C),
    tertiaryContainer = CampusPaperColors.DarkSageContainer,
    onTertiaryContainer = CampusPaperColors.DarkSageOnContainer,
    background = CampusPaperColors.DarkPaper,
    onBackground = CampusPaperColors.DarkInk,
    surface = CampusPaperColors.DarkCard,
    onSurface = CampusPaperColors.DarkInk,
    surfaceVariant = CampusPaperColors.DarkInset,
    onSurfaceVariant = CampusPaperColors.DarkMuted,
    surfaceContainerLowest = CampusPaperColors.DarkCard,
    surfaceContainerLow = CampusPaperColors.DarkInset,
    surfaceContainer = CampusPaperColors.DarkInset,
    outline = CampusPaperColors.DarkRuleStrong,
    outlineVariant = CampusPaperColors.DarkRule,
    error = Color(0xFFF87171)
)

@Composable
fun DueSoonTheme(
    accentPalette: AccentPalette = AccentPalette.INDIGO,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val accentColor = Color(accentPalette.primaryHex)
    val colorScheme = when {
        accentPalette == AccentPalette.CAMPUS_PAPER && darkTheme -> CampusPaperDarkColorScheme
        accentPalette == AccentPalette.CAMPUS_PAPER && !darkTheme -> CampusPaperLightColorScheme
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

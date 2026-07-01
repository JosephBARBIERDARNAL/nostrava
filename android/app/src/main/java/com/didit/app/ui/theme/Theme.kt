package com.didit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Light mode only — the app has no dark theme, matching the current web UI.
private val DiditColorScheme = lightColorScheme(
    primary = DiditColors.Brand,
    onPrimary = DiditColors.BrandForeground,
    secondary = DiditColors.Accent,
    onSecondary = DiditColors.AccentForeground,
    background = DiditColors.Background,
    onBackground = DiditColors.Foreground,
    surface = DiditColors.Card,
    onSurface = DiditColors.CardForeground,
    error = DiditColors.Destructive,
    onError = DiditColors.DestructiveForeground,
    outline = DiditColors.Border,
)

@Composable
fun DiditTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DiditColorScheme,
        typography = DiditTypography,
        content = content,
    )
}

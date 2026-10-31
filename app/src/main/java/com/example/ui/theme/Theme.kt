package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TreeGuardColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = ForestAbyssBg,
    primaryContainer = ForestCardElevated,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricCyan,
    onSecondary = ForestAbyssBg,
    secondaryContainer = CyanDim,
    onSecondaryContainer = TextPrimary,
    tertiary = WarningAmber,
    onTertiary = ForestAbyssBg,
    background = ForestAbyssBg,
    onBackground = TextPrimary,
    surface = ForestSecondaryBg,
    onSurface = TextPrimary,
    surfaceVariant = ForestCardBg,
    onSurfaceVariant = TextSecondary,
    outline = ForestBorderSubtle,
    error = PriorityCriticalRed,
    onError = Color.White
)

@Composable
fun TreeGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TreeGuardColorScheme,
        typography = Typography,
        content = content
    )
}

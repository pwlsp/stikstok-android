package com.example.tikstok.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PresentationColorScheme = darkColorScheme(
    primary = Copper,
    onPrimary = Color.White,
    primaryContainer = Copper,
    onPrimaryContainer = Color.White,
    secondary = Copper,
    onSecondary = Color.White,
    secondaryContainer = Copper,
    onSecondaryContainer = Color.White,
    tertiary = Copper,
    background = AppBackground,
    onBackground = AppOnSurface,
    surface = AppSurface,
    onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = AppOnSurfaceVariant,
    surfaceContainerLowest = AppSurfaceLowest,
    surfaceContainerLow = AppSurfaceLow,
    surfaceContainer = AppSurface,
    surfaceContainerHigh = AppSurfaceHigh,
    surfaceContainerHighest = AppSurfaceHighest,
    outline = AppOutline,
    outlineVariant = AppOutlineVariant,
)

@Composable
fun TikStokTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PresentationColorScheme,
        typography = Typography,
        content = content,
    )
}

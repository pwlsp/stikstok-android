package com.example.tikstok.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fixed dark scheme matching the deck. Dynamic (Material You) color is intentionally off so the
// copper accent is the single brand color everywhere instead of the wallpaper's tint.
private val PresentationColorScheme = darkColorScheme(
    primary = Copper,
    onPrimary = Color.White,
    primaryContainer = Copper,
    onPrimaryContainer = Color.White,
    secondary = Copper,
    onSecondary = Color.White,
    // Drives the "selected" look of FilterChips and the quick-amount chips.
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

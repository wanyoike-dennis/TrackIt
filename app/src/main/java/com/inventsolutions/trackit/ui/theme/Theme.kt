package com.inventsolutions.trackit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    // Use the deeper brand green for readable white-on-green controls.
    primary = TrackItEmeraldDark,
    onPrimary = TrackItSurfaceLight,
    primaryContainer = TrackItEmeraldTint,
    onPrimaryContainer = TrackItNavy,
    secondary = TrackItNavy,
    onSecondary = TrackItSurfaceLight,
    secondaryContainer = TrackItSurfaceVariantLight,
    onSecondaryContainer = TrackItNavy,
    tertiary = TrackItAmber,
    onTertiary = TrackItNavy,
    tertiaryContainer = TrackItAmberTint,
    onTertiaryContainer = TrackItNavy,
    error = TrackItExpense,
    onError = TrackItSurfaceLight,
    errorContainer = TrackItExpenseTintLight,
    onErrorContainer = TrackItNavy,
    background = TrackItBackgroundLight,
    onBackground = TrackItTextLight,
    surface = TrackItSurfaceLight,
    onSurface = TrackItTextLight,
    surfaceVariant = TrackItSurfaceVariantLight,
    onSurfaceVariant = TrackItTextMutedLight,
    outline = TrackItOutlineLight,
    outlineVariant = TrackItSurfaceVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = TrackItEmeraldLight,
    onPrimary = TrackItNavy,
    primaryContainer = TrackItEmeraldDark,
    onPrimaryContainer = TrackItSurfaceLight,
    secondary = TrackItTextDark,
    onSecondary = TrackItNavy,
    secondaryContainer = TrackItNavySurface,
    onSecondaryContainer = TrackItTextDark,
    tertiary = TrackItAmber,
    onTertiary = TrackItNavy,
    tertiaryContainer = TrackItNavySurface,
    onTertiaryContainer = TrackItAmberTint,
    error = Color(0xFFFF8A8A),
    onError = TrackItNavy,
    errorContainer = TrackItExpenseTintDark,
    onErrorContainer = TrackItTextDark,
    background = TrackItBackgroundDark,
    onBackground = TrackItTextDark,
    surface = TrackItSurfaceDark,
    onSurface = TrackItTextDark,
    surfaceVariant = TrackItSurfaceVariantDark,
    onSurfaceVariant = TrackItTextMutedDark,
    outline = TrackItOutlineDark,
    outlineVariant = TrackItNavySurface
)

@Composable
fun TrackItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

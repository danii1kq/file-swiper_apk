package com.danni.fileswiper.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Palette tokens extracted from the Stitch designs ("Eco Clean Storage",
// project 10189997976204885052). Primary = eco green, tertiary = icy blue
// for media/size accents, error = destructive red for trash actions.

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF006C4B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9DF3D4),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6358),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8F0E7),
    onSecondaryContainer = Color(0xFF2B322D),
    tertiary = Color(0xFF3D6374),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FD),
    onTertiaryContainer = Color(0xFF001F2A),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF4FBF3),
    onBackground = Color(0xFF161D18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF161D18),
    surfaceVariant = Color(0xFFE2EAE2),
    onSurfaceVariant = Color(0xFF3F4943),
    outline = Color(0xFF6F7A72),
    outlineVariant = Color(0xFFDDE5DC)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF74DBAC),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF005234),
    onPrimaryContainer = Color(0xFF9DF3D4),
    secondary = Color(0xFF9CA8A0),
    onSecondary = Color(0xFF1D251F),
    secondaryContainer = Color(0xFF2E3A32),
    onSecondaryContainer = Color(0xFFDFE4DF),
    tertiary = Color(0xFFA5CCE0),
    onTertiary = Color(0xFF003444),
    tertiaryContainer = Color(0xFF00212E),
    onTertiaryContainer = Color(0xFFC1E8FD),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101412),
    onBackground = Color(0xFFDFE4DF),
    surface = Color(0xFF1D251F),
    onSurface = Color(0xFFDFE4DF),
    surfaceVariant = Color(0xFF273129),
    onSurfaceVariant = Color(0xFF9CA8A0),
    outline = Color(0xFF68766E),
    outlineVariant = Color(0xFF333F36)
)

internal fun lightScheme(): ColorScheme = LightColors
internal fun darkScheme(): ColorScheme = DarkColors

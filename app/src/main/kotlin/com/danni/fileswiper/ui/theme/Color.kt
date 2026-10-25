package com.danni.fileswiper.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Ranked "green clean" palette: primary = eco green, tertiary = icy blue (media),
// error = destructive red for trash actions.

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF006C4B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF2D3),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6358),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8DB),
    onSecondaryContainer = Color(0xFF0D2018),
    tertiary = Color(0xFF3D6374),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FB),
    onTertiaryContainer = Color(0xFF001F2B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9FBF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFF9FBF8),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DF),
    onSurfaceVariant = Color(0xFF3F4943),
    outline = Color(0xFF6F7B73),
    outlineVariant = Color(0xFFBEC9C1)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF74DBAC),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005234),
    onPrimaryContainer = Color(0xFF9CF2D3),
    secondary = Color(0xFFB2CCC0),
    onSecondary = Color(0xFF1D352C),
    secondaryContainer = Color(0xFF354C42),
    onSecondaryContainer = Color(0xFFD0E8DB),
    tertiary = Color(0xFFA5CBE0),
    onTertiary = Color(0xFF053547),
    tertiaryContainer = Color(0xFF234E61),
    onTertiaryContainer = Color(0xFFC1E8FB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101412),
    onBackground = Color(0xFFDFE4DF),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFDFE4DF),
    surfaceVariant = Color(0xFF3F4944),
    onSurfaceVariant = Color(0xFFBEC9C1),
    outline = Color(0xFF89938C),
    outlineVariant = Color(0xFF3F4944)
)

internal fun lightScheme(): ColorScheme = LightColors
internal fun darkScheme(): ColorScheme = DarkColors

package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreenPrimary,
    onPrimary = Color(0xFF003821),
    primaryContainer = IslamicGreen,
    onPrimaryContainer = Color(0xFF9CF7C4),
    secondary = DarkGreenSecondary,
    onSecondary = Color(0xFF003823),
    tertiary = DarkGold,
    onTertiary = Color(0xFF452B00),
    background = DarkBackground,
    onBackground = Color(0xFFE1E3DF),
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF3F4942),
    onSurfaceVariant = Color(0xFFC0C9C0)
)

private val LightColorScheme = lightColorScheme(
    primary = LightGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF7C4),
    onPrimaryContainer = Color(0xFF002111),
    secondary = LightGreenSecondary,
    onSecondary = Color.White,
    tertiary = LightGold,
    onTertiary = Color.White,
    background = LightSand,
    onBackground = Color(0xFF191D1A),
    surface = Color.White,
    onSurface = Color(0xFF191D1A),
    surfaceVariant = Color(0xFFDDE5DE),
    onSurfaceVariant = Color(0xFF414943)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

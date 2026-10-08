package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ChromeBlueDark,
    onPrimary = Color(0xFF041E49),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    surface = ChromeSurfaceDark,
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = ChromeSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFC4C7C5),
    background = ChromeBackgroundDark,
    onBackground = Color(0xFFE3E3E3),
    outline = Color(0xFF444746)
)

private val LightColorScheme = lightColorScheme(
    primary = ChromeBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    surface = ChromeSurfaceLight,
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = ChromeSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF444746),
    background = ChromeBackgroundLight,
    onBackground = Color(0xFF1F1F1F),
    outline = Color(0xFF74777F)
)

@Composable
fun DevChromeTheme(
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

package com.example.kisandost.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = KisanGreenLight,
    secondary = KisanGreen,
    tertiary = KisanGreenDark,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = KisanWhite,
    onSecondary = KisanWhite,
    onTertiary = KisanWhite,
    onBackground = KisanWhite,
    onSurface = KisanWhite
)

private val LightColorScheme = lightColorScheme(
    primary = KisanGreen,
    secondary = KisanGreenDark,
    tertiary = KisanGreenLight,
    background = KisanBackground,
    surface = KisanSurface,
    onPrimary = KisanOnGreen,
    onSecondary = KisanOnGreen,
    onTertiary = KisanOnGreen,
    onBackground = KisanOnSurface,
    onSurface = KisanOnSurface
)

@Composable
fun KisanDostTheme(
    darkTheme: Boolean = false, // Force light theme for high contrast
    // Dynamic color disabled for consistent green theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
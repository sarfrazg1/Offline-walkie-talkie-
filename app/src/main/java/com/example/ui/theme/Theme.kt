package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RadioColorScheme = darkColorScheme(
    primary = PttGreen,
    onPrimary = Color.Black,
    primaryContainer = PttGreenDark,
    onPrimaryContainer = Color.White,
    secondary = RadioReceiveCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D5A),
    onSecondaryContainer = Color.White,
    tertiary = RadioAmber,
    onTertiary = Color.Black,
    background = RadioDarkBackground,
    onBackground = TextPrimary,
    surface = RadioDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = RadioCardSurface,
    onSurfaceVariant = TextSecondary,
    error = RadioBusyRed,
    onError = Color.White,
    outline = RadioSurfaceHighlight
)

@Composable
fun WalkieTalkieTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RadioColorScheme,
        typography = Typography,
        content = content
    )
}

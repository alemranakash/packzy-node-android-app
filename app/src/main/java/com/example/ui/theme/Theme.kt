package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricGreen,
    onPrimary = CosmicBlack,
    secondary = SoftSlateGray,
    onSecondary = TextWhite,
    tertiary = Emerald400,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = SurfaceCard,
    onSurface = TextWhite,
    error = ErrorRed,
    onError = TextWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for Midnight Cyberpunk vibe
    dynamicColor: Boolean = false, // Disable dynamic colors so our brand shines through
    content: @Composable () -> Unit,
) {
    // We enforce our premium dark cyber slate color scheme
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}


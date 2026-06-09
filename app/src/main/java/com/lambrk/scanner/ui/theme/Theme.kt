package com.lambrk.scanner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary             = NavyPrimaryNight,
    onPrimary           = OnNavyPrimaryNight,
    primaryContainer    = NavyPrimaryNightLight,
    onPrimaryContainer  = OnNavyPrimaryNight,
    secondary           = AccentBlueDark,        // bright blue accent for dark
    onSecondary         = Color(0xFF0D1B2A),
    secondaryContainer  = AccentBlue.copy(alpha = 0.2f),
    onSecondaryContainer= AccentBlueDark,
    background          = BackgroundDark,
    surface             = SurfaceDark,
    onSurface           = OnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary             = NavyPrimary,
    onPrimary           = OnNavyPrimary,
    primaryContainer    = NavyPrimaryLight,
    onPrimaryContainer  = OnNavyPrimary,
    secondary           = AccentBlue,            // bright blue accent for light
    onSecondary         = Color.White,
    secondaryContainer  = AccentBlueLight,
    onSecondaryContainer= NavyPrimary,
    background          = BackgroundLight,
    surface             = SurfaceLight,
    onSurface           = OnSurfaceLight
)

@Composable
fun ScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

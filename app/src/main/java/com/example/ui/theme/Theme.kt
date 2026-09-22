package com.example.ui.theme

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
    primary = CyanNeon,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = CyanNeon,
    secondary = VioletNeon,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3B1A5C),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = EmeraldGlow,
    onTertiary = Color.Black,
    background = CyberNavyDark,
    onBackground = Color(0xFFF1F5F9),
    surface = CyberNavySurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = CyberNavySurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = CyberNavyBorder,
    error = CoralAlert
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00687A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB5EAFF),
    onPrimaryContainer = Color(0xFF001F26),
    secondary = Color(0xFF704AB2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECDDFE),
    onSecondaryContainer = Color(0xFF280056),
    tertiary = Color(0xFF006D44),
    onTertiary = Color.White,
    background = CyberSlateLight,
    onBackground = TextDark,
    surface = CyberSlateSurface,
    onSurface = TextDark,
    surfaceVariant = CyberSlateSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = CyberSlateBorder,
    error = CoralAlert
)

@Composable
fun SweatyTheme(
    darkTheme: Boolean = true, // Voice assistants look premier in dark mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep alias for compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = SweatyTheme(darkTheme, dynamicColor, content)

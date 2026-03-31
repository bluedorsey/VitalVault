package com.example.personalhealthcareapp.ui.theme

import android.app.Activity
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
    primary = TealLight,
    secondary = TealVeryLight,
    tertiary = TealDark,
    background = Color(0xFF1E2120),
    surface = Color(0xFF262C2A),
    onPrimary = TealDark,
    onBackground = BackgroundClay,
    onSurface = WhiteCore,
    onSurfaceVariant = TextLight
)

private val LightColorScheme = lightColorScheme(
    primary = TealDark,
    secondary = TealLight,
    tertiary = TealVeryLight,
    background = BackgroundClay,
    surface = WhiteCore,
    onPrimary = WhiteCore,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = TextLight
)

@Composable
fun PersonalHealthCareAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
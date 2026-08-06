package com.scs3311.smart_home_monitoring_app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TealLight,
    onPrimary = BackgroundDark,
    secondary = AmberAccent,
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = Color(0xFFE8EAED),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = Color(0xFFB0BEC5)
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color(0xFFFFFFFF),
    secondary = TealDark,
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F7FA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1F2E),
    onSurface = Color(0xFF1A1F2E),
    surfaceVariant = Color(0xFFE0F2F1),
    onSurfaceVariant = Color(0xFF455A64)
)

@Composable
fun SmarthomemonitoringappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

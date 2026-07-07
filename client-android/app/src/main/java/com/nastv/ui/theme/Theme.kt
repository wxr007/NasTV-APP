package com.nastv.ui.theme

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
    primary = NasTVPrimary,
    secondary = NasTVSecondary,
    tertiary = Pink80,
    background = NasTVBackground,
    surface = NasTVSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = NasTVTextPrimary,
    onSurface = NasTVTextPrimary,
    error = NasTVError
)

// TV端专用颜色方案 - 金黄色
private val TvDarkColorScheme = darkColorScheme(
    primary = NasTVPrimaryGold,
    secondary = NasTVSecondaryGold,
    tertiary = Pink80,
    background = NasTVBackground,
    surface = NasTVSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = NasTVTextPrimary,
    onSurface = NasTVTextPrimary,
    error = NasTVError
)

private val LightColorScheme = lightColorScheme(
    primary = NasTVPrimary,
    secondary = NasTVSecondary,
    tertiary = Pink40,
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    error = NasTVError
)

@Composable
fun NasTVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isTv: Boolean = false,
    content: @Composable () -> Unit
) {
    // TV 端使用金黄色暗色主题
    val colorScheme = when {
        isTv -> TvDarkColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme && !isTv
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

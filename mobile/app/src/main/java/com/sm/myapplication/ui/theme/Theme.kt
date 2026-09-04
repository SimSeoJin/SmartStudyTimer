package com.sm.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = White,
    primaryContainer = BgGreenLight,
    onPrimaryContainer = GreenDark,

    secondary = GreenDark,
    onSecondary = White,
    secondaryContainer = GreenLighter,
    onSecondaryContainer = Black,

    tertiary = PurpleAccent,
    onTertiary = White,

    background = White,
    onBackground = Black,

    surface = White,
    onSurface = Black,
    surfaceVariant = BgGray,
    onSurfaceVariant = Black50,

    outline = Gray,
    outlineVariant = GrayLight,
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgGreenLight.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}

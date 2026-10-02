package com.sm.myapplication.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = lightColorScheme(
    primary = AccentGreen,
    onPrimary = CardWhite,
    primaryContainer = AccentTint,
    onPrimaryContainer = AccentInk,

    secondary = AccentInk,
    onSecondary = CardWhite,
    secondaryContainer = AccentTint,
    onSecondaryContainer = AccentInk,

    tertiary = SystemBlue,
    onTertiary = CardWhite,

    background = GroupedBg,
    onBackground = LabelPrimary,

    surface = CardWhite,
    onSurface = LabelPrimary,
    surfaceVariant = FillGray,
    onSurfaceVariant = LabelSecondary,

    error = Destructive,
    onError = CardWhite,

    outline = Separator,
    outlineVariant = Separator,
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 밝은 배경 위의 어두운 상태바 아이콘 (순공모드에서는 FocusStatusBar가 뒤집는다)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * 순공모드처럼 어두운 화면에서만 상태바 아이콘을 밝게 바꾸고,
 * 화면을 벗어나면 원래대로 되돌린다.
 */
@Composable
fun DarkStatusBarEffect() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = true }
    }
}

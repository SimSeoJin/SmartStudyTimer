package com.sm.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

/** 어두운 화면(순공모드)용 상태바. 화면을 벗어나면 밝은 화면용(어두운 아이콘)으로 되돌린다. */
@Composable
fun DarkStatusBarEffect() {
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(view) {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            window.statusBarColor = FocusBg.toArgb()
            controller.isAppearanceLightStatusBars = false
            onDispose {
                // 되돌리지 않으면 흰 배경 위에 흰 아이콘이 남아 시간·배터리가 안 보였다
                window.statusBarColor = BgGreenLight.toArgb()
                controller.isAppearanceLightStatusBars = true
            }
        }
    }
}

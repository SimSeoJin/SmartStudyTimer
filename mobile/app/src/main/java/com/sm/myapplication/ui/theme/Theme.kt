package com.sm.myapplication.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val AppColorScheme = lightColorScheme(
    primary = PartyRed,
    onPrimary = Paper,
    primaryContainer = PartyTint,
    onPrimaryContainer = PartyRedDark,

    secondary = Ink,
    onSecondary = Paper,
    secondaryContainer = PaperDeep,
    onSecondaryContainer = Ink,

    tertiary = ApprovedGreen,
    onTertiary = Paper,

    background = PaperShade,
    onBackground = Ink,

    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = LabelSecondary,
    surfaceContainerHigh = Paper,

    outline = Separator,
    outlineVariant = Separator,
)

/** 포스터 인쇄물처럼 모서리를 거의 세우지 않는다. */
object PosterShape {
    val Small = RoundedCornerShape(3.dp)
    val Medium = RoundedCornerShape(4.dp)
    val Large = RoundedCornerShape(6.dp)
}

private val AppShapes = Shapes(
    extraSmall = PosterShape.Small,
    small = PosterShape.Small,
    medium = PosterShape.Medium,
    large = PosterShape.Large,
    extraLarge = PosterShape.Large,
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PaperShade.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun DarkStatusBarEffect() {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = FocusBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
}

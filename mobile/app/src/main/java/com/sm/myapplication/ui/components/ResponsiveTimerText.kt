package com.sm.myapplication.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
fun ResponsiveTimerText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    maxFontSize: Float = 48f,
    minFontSize: Float = 28f,
    style: TextStyle = TextStyle().copy(fontSize = maxFontSize.sp)
) {
    var fontSize by remember { mutableStateOf(maxFontSize.sp) }

    Text(
        text = text,
        modifier = modifier.onSizeChanged { },
        color = color,
        style = style.copy(fontSize = fontSize),
        maxLines = 1,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize.value > minFontSize) {
                fontSize = (fontSize.value * 0.9f).coerceAtLeast(minFontSize).sp
            }
        }
    )
}

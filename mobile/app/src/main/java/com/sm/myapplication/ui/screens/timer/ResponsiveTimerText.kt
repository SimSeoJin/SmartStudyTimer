package com.sm.myapplication.ui.screens.timer

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.myapplication.ui.theme.LabelPrimary
import com.sm.myapplication.ui.theme.TabularFigures

/**
 * 항상 한 줄에 들어오는 타이머 텍스트.
 * 실제 가용 폭에 맞춰 글자 크기를 재기 때문에 기기 폭이 달라도 잘리지 않는다.
 */
@Composable
fun ResponsiveTimerText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: Float = 56f,
    minFontSize: Float = 28f,
    color: Color = LabelPrimary,
    horizontalPadding: Int = 20,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding.dp),
    ) {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val availablePx = with(density) { maxWidth.toPx() }

        val fontSize = remember(text, availablePx, maxFontSize, minFontSize) {
            findFittingFontSize(text, availablePx, textMeasurer, maxFontSize, minFontSize)
        }

        Text(
            text = text,
            style = timerStyle(fontSize),
            color = color,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun timerStyle(fontSize: Float) = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = fontSize.sp,
    letterSpacing = (-fontSize * 0.03f).sp,
    fontFeatureSettings = TabularFigures,
)

private fun findFittingFontSize(
    text: String,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    maxFontSize: Float,
    minFontSize: Float,
): Float {
    var low = minFontSize
    var high = maxFontSize

    repeat(12) {
        val mid = (low + high) / 2f
        val width = textMeasurer.measure(
            AnnotatedString(text),
            style = timerStyle(mid),
            maxLines = 1,
        ).size.width
        if (width <= availableWidthPx) low = mid else high = mid
    }
    return low.coerceIn(minFontSize, maxFontSize)
}

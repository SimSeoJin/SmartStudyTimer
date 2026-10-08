package com.sm.myapplication.ui.components

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalDate

/** 년·월·일을 각각 위아래로 스크롤해서 고르는 휠. 월/년이 바뀌면 그 달의 말일에 맞춰 일 범위를 조정한다. */
@Composable
fun DateWheelPicker(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    val thisYear = LocalDate.now().year
    val minYear = minOf(thisYear - 1, date.year)
    val maxYear = maxOf(thisYear + 10, date.year)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Wheel(
            min = minYear, max = maxYear, value = date.year,
            format = { "${it}년" }, modifier = Modifier.weight(1.3f),
        ) { y -> onDateChange(date.withYear(y)) }
        Wheel(
            min = 1, max = 12, value = date.monthValue,
            format = { "${it}월" }, modifier = Modifier.weight(1f),
        ) { m -> onDateChange(date.withMonth(m)) }
        Wheel(
            min = 1, max = date.lengthOfMonth(), value = date.dayOfMonth,
            format = { "${it}일" }, modifier = Modifier.weight(1f),
        ) { d -> onDateChange(date.withDayOfMonth(d)) }
    }
}

/** 시·분을 스크롤해서 고르는 휠(24시간제). */
@Composable
fun TimeWheelPicker(minutes: Int, onMinutesChange: (Int) -> Unit) {
    val hour = minutes / 60
    val minute = minutes % 60
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Wheel(
            min = 0, max = 23, value = hour,
            format = { "${it}시" }, modifier = Modifier.weight(1f),
        ) { h -> onMinutesChange(h * 60 + minute) }
        Wheel(
            min = 0, max = 59, value = minute,
            format = { "${it}분" }, modifier = Modifier.weight(1f),
        ) { m -> onMinutesChange(hour * 60 + m) }
    }
}

@Composable
private fun Wheel(
    min: Int,
    max: Int,
    value: Int,
    format: (Int) -> String,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    // factory는 한 번만 실행되므로 리스너가 첫 구성 때의 값을 붙잡고 있으면 다른 휠에서 고른 값이 되돌아간다.
    val latestOnChange by rememberUpdatedState(onChange)
    AndroidView(
        modifier = modifier.height(150.dp),
        factory = { ctx ->
            NumberPicker(ctx).apply {
                wrapSelectorWheel = false
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                setOnValueChangedListener { _, _, new -> latestOnChange(new) }
            }
        },
        update = { picker ->
            // setFormatter는 처음 그려지는 값에 적용되지 않으므로(안드로이드 알려진 문제) displayedValues로 단위를 붙인다.
            // 범위를 바꾸기 전에는 displayedValues를 먼저 비워야 한다.
            if (picker.minValue != min || picker.maxValue != max || picker.displayedValues == null) {
                picker.displayedValues = null
                picker.minValue = min
                picker.maxValue = max
                picker.displayedValues = Array(max - min + 1) { format(min + it) }
            }
            if (picker.value != value) picker.value = value
        },
    )
}

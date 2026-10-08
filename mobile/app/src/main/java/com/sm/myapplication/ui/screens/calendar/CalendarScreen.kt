package com.sm.myapplication.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.data.entity.StudySessionEntity
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.ui.components.InsetCard
import com.sm.myapplication.ui.components.RoundCheck
import com.sm.myapplication.ui.components.RowDivider
import com.sm.myapplication.ui.components.SectionHeader
import com.sm.myapplication.ui.components.SummaryHeader
import com.sm.myapplication.ui.components.formatDuration
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.screens.home.formatClock
import com.sm.myapplication.ui.theme.AccentInk
import com.sm.myapplication.ui.theme.AccentGreen
import com.sm.myapplication.ui.theme.AccentTint
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.Destructive
import com.sm.myapplication.ui.theme.FillGray
import com.sm.myapplication.ui.theme.Green300
import com.sm.myapplication.ui.theme.GroupedBg
import com.sm.myapplication.ui.theme.LabelPrimary
import com.sm.myapplication.ui.theme.LabelSecondary
import com.sm.myapplication.ui.theme.LabelTertiary
import com.sm.myapplication.ui.theme.SystemBlue
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    val monthTotalMs = state.dayTotalsMs.values.sum()
    val studiedDays = state.dayTotalsMs.count { it.value > 0 }
    val averageMs = if (studiedDays > 0) monthTotalMs / studiedDays else 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SummaryHeader(
            summary = "이번 달 누적",
            highlight = if (monthTotalMs > 0) formatKoreanDuration(monthTotalMs) else "0분",
            sub = if (studiedDays > 0) {
                "공부한 날 ${studiedDays}일 · 하루 평균 ${formatKoreanDuration(averageMs)}"
            } else {
                "이번 달 기록이 아직 없어요"
            },
        )

        MonthCard(
            yearMonth = state.yearMonth,
            totalsByDay = state.dayTotalsMs,
            ddayEpochDay = state.ddayEpochDay,
            today = today,
            selected = state.selectedDate,
            onPrev = viewModel::prevMonth,
            onNext = viewModel::nextMonth,
            onSelect = viewModel::selectDate,
        )

        val isToday = state.selectedDate == today
        SectionHeader(
            text = state.selectedDate.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)) +
                if (isToday) " · 오늘" else ""
        )

        if (state.selectedDaySessions.isEmpty()) {
            InsetCard {
                Text(
                    "이 날의 공부 기록이 없어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LabelSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            InsetCard {
                state.selectedDaySessions.forEachIndexed { index, session ->
                    SessionRow(session)
                    if (index != state.selectedDaySessions.lastIndex) RowDivider(inset = 50.dp)
                }
            }
        }

        if (state.ddayEpochDay == state.selectedDate.toEpochDay()) {
            SectionHeader("D-Day")
            InsetCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Destructive))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = state.ddayLabel.ifBlank { "D-Day" },
                        style = MaterialTheme.typography.titleSmall,
                        color = LabelPrimary,
                    )
                }
            }
        }

        if (state.selectedDayTodos.isNotEmpty()) {
            SectionHeader("일정")
            InsetCard {
                state.selectedDayTodos.forEachIndexed { index, todo ->
                    TodoRow(todo)
                    if (index != state.selectedDayTodos.lastIndex) RowDivider(inset = 48.dp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** 모드는 한 줄, 시간은 오른쪽에 두 줄(구간 / 총 시간)로 정렬한다. */
@Composable
private fun SessionRow(session: StudySessionEntity) {
    val isPure = session.mode == StudyMode.PURE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (isPure) FillGray else AccentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPure) Icons.Rounded.DarkMode else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (isPure) LabelPrimary else AccentInk,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (isPure) "순공 모드" else "기본 모드",
            style = MaterialTheme.typography.titleSmall,
            color = LabelPrimary,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${formatTime(session.startTime)} – ${formatTime(session.endTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = LabelSecondary,
            )
            Text(
                text = formatDuration(session.durationMs),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AccentInk,
            )
        }
    }
}

@Composable
private fun TodoRow(todo: TodoEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundCheck(checked = todo.isDone)
        Spacer(Modifier.width(10.dp))
        Text(
            text = todo.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (todo.isDone) LabelSecondary else LabelPrimary,
            textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
        )
        todo.timeMinutes?.let {
            Text(formatClock(it), style = MaterialTheme.typography.bodySmall, color = LabelTertiary)
        }
    }
}

@Composable
private fun MonthCard(
    yearMonth: YearMonth,
    totalsByDay: Map<Long, Long>,
    ddayEpochDay: Long?,
    today: LocalDate,
    selected: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .padding(horizontal = 10.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = yearMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)),
                style = MaterialTheme.typography.titleLarge,
                color = LabelPrimary,
            )
            Spacer(Modifier.weight(1f))
            ArrowButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "이전 달", onPrev)
            Spacer(Modifier.width(6.dp))
            ArrowButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "다음 달", onNext)
        }

        Row(Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { index, day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = when (index) {
                        0 -> Destructive
                        6 -> SystemBlue
                        else -> LabelSecondary
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        // 공부량 점의 진하기를 나누는 기준: 그 달 최대 기록의 절반
        val maxMs = totalsByDay.values.maxOrNull() ?: 0L
        val firstDay = yearMonth.atDay(1)
        val startOffset = firstDay.dayOfWeek.value % 7 // 일요일 시작
        val daysInMonth = yearMonth.lengthOfMonth()
        val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

        var cellIndex = 0
        while (cellIndex < totalCells) {
            Row(Modifier.fillMaxWidth()) {
                repeat(7) {
                    val dayNum = cellIndex - startOffset + 1
                    if (dayNum in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayNum)
                        val ms = totalsByDay[date.toEpochDay()] ?: 0L
                        DayCell(
                            day = dayNum,
                            studyMs = ms,
                            strong = maxMs > 0 && ms >= maxMs / 2,
                            isToday = date == today,
                            isDDay = date.toEpochDay() == ddayEpochDay,
                            isSelected = date == selected,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(date) },
                        )
                    } else {
                        Box(Modifier.weight(1f).height(40.dp))
                    }
                    cellIndex++
                }
            }
        }
    }
}

@Composable
private fun ArrowButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(GroupedBg)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = AccentInk, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DayCell(
    day: Int,
    studyMs: Long,
    strong: Boolean,
    isToday: Boolean,
    isDDay: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> AccentGreen
                            isDDay -> Destructive
                            else -> Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected || isToday || isDDay) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = when {
                        isSelected || isDDay -> CardWhite
                        isToday -> AccentInk
                        else -> LabelPrimary
                    },
                )
            }
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            studyMs <= 0L -> Color.Transparent
                            strong -> AccentGreen
                            else -> Green300
                        }
                    )
            )
        }
    }
}

private fun formatTime(epochMs: Long): String =
    java.time.Instant.ofEpochMilli(epochMs)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

private fun formatKoreanDuration(ms: Long): String {
    val totalMin = ms / 60000
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> "${h}시간 ${m}분"
        h > 0 -> "${h}시간"
        else -> "${m}분"
    }
}

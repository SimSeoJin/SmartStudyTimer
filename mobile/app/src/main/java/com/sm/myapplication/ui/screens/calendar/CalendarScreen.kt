package com.sm.myapplication.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
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
import com.sm.myapplication.ui.components.formatStudyDuration
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.screens.home.formatClock
import com.sm.myapplication.ui.theme.AccentInk
import com.sm.myapplication.ui.theme.AccentTint
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.Destructive
import com.sm.myapplication.ui.theme.Green500
import com.sm.myapplication.ui.theme.Green700
import com.sm.myapplication.ui.theme.Green900
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
            highlight = formatStudyDuration(monthTotalMs),
            sub = if (studiedDays > 0) {
                "공부한 날 ${studiedDays}일 · 하루 평균 ${formatStudyDuration(averageMs)}"
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

        // 기록이 없는 날을 골라도 볼 거리가 있도록, 보고 있는 달의 요약을 아래에 둔다
        SectionHeader("${state.yearMonth.monthValue}월 기록")
        MonthSummaryCard(
            totalsByDay = state.dayTotalsMs,
            pureMs = state.pureMs,
            basicMs = state.basicMs,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MonthSummaryCard(totalsByDay: Map<Long, Long>, pureMs: Long, basicMs: Long) {
    val totalMs = pureMs + basicMs
    InsetCard {
        if (totalMs <= 0L) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Visibility, contentDescription = null, tint = Green700, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "공부하면 날짜 칸이 초록색으로 채워지고, 많이 한 날일수록 진해져요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LabelSecondary,
                )
            }
            return@InsetCard
        }

        // 순공 / 일반 비율 막대
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)) {
            val pureRatio = pureMs.toFloat() / totalMs
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("순공 비율", style = MaterialTheme.typography.titleSmall, color = LabelPrimary)
                Spacer(Modifier.weight(1f))
                Text(
                    "${(pureRatio * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentInk,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(HeatLevel2),
            ) {
                if (pureRatio > 0f) {
                    Box(Modifier.weight(pureRatio.coerceAtLeast(0.02f)).fillMaxHeight().background(Green500))
                }
                if (pureRatio < 1f) Spacer(Modifier.weight((1f - pureRatio).coerceAtLeast(0.02f)))
            }
            Spacer(Modifier.height(8.dp))
            // 분 단위로 따로 버림하면 합이 누적 시간과 1분씩 어긋나므로(6분+3분≠10분),
            // 일반 시간은 "누적(분) − 순공(분)"으로 보여준다.
            val basicShownMs = (totalMs / 60_000 - pureMs / 60_000) * 60_000
            Row {
                LegendDot(Green500, "순공 ${formatStudyDuration(pureMs)}")
                Spacer(Modifier.width(14.dp))
                LegendDot(HeatLevel2, "일반 ${formatStudyDuration(if (basicShownMs > 0) basicShownMs else basicMs)}")
            }
        }

        RowDivider(inset = 16.dp)

        val best = totalsByDay.maxByOrNull { it.value }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            MonthStat(
                label = "가장 많이 한 날",
                value = best?.let { LocalDate.ofEpochDay(it.key).format(DateTimeFormatter.ofPattern("M월 d일")) } ?: "-",
                sub = best?.let { formatStudyDuration(it.value) },
                modifier = Modifier.weight(1f),
            )
            MonthStat(
                label = "최장 연속",
                value = "${longestStreak(totalsByDay)}일",
                sub = "공부한 날이 이어진 기간",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = LabelSecondary)
    }
}

@Composable
private fun MonthStat(label: String, value: String, sub: String?, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LabelSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = LabelPrimary)
        if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = LabelTertiary)
    }
}

/** 그 달 안에서 기록이 있는 날이 가장 길게 이어진 일수 */
private fun longestStreak(totalsByDay: Map<Long, Long>): Int {
    val days = totalsByDay.filter { it.value > 0 }.keys.sorted()
    var best = 0
    var run = 0
    var prev: Long? = null
    for (d in days) {
        run = if (prev != null && d == prev + 1) run + 1 else 1
        if (run > best) best = run
        prev = d
    }
    return best
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
                .background(if (isPure) BgGreenLight else AccentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPure) Icons.Rounded.Visibility else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (isPure) Green700 else AccentInk,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (isPure) "순공 모드" else "일반 타이머",
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

        // 칸 색 진하기 기준: 그 달 최대 기록 대비 비율
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
                            level = studyLevel(ms, maxMs),
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

/** 공부량 단계: 0(기록 없음) ~ 3(그 달 최대의 2/3 이상) */
private fun studyLevel(ms: Long, maxMs: Long): Int = when {
    ms <= 0L || maxMs <= 0L -> 0
    ms * 3 >= maxMs * 2 -> 3
    ms * 3 >= maxMs -> 2
    else -> 1
}

// 잔디처럼 공부를 많이 한 날일수록 진한 초록
private val HeatLevel1 = Color(0xFFE2F2D5)
private val HeatLevel2 = Color(0xFFB3DC93)

@Composable
private fun DayCell(
    day: Int,
    level: Int,
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
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    when {
                        isDDay -> Destructive
                        level == 3 -> Green500
                        level == 2 -> HeatLevel2
                        level == 1 -> HeatLevel1
                        else -> Color.Transparent
                    }
                )
                // 선택한 날은 채움 대신 테두리로 표시해 공부량 색을 가리지 않는다
                .then(if (isSelected) Modifier.border(1.5.dp, Green900, RoundedCornerShape(9.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected || isToday || isDDay) FontWeight.Bold else FontWeight.Normal,
                ),
                color = when {
                    isDDay || level == 3 -> CardWhite
                    isToday -> Green700
                    else -> LabelPrimary
                },
            )
        }
    }
}

private fun formatTime(epochMs: Long): String =
    java.time.Instant.ofEpochMilli(epochMs)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))


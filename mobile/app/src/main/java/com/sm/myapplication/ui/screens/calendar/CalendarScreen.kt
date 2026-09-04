package com.sm.myapplication.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sm.myapplication.data.entity.StudySessionEntity
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.Black50
import com.sm.myapplication.ui.theme.GreenPrimary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        // 헤더 (#F0F7E7)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(61.dp)
                .background(BgGreenLight),
        )

        // 월/년 + 좌우 화살표
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ArrowBox(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = viewModel::prevMonth)
            Text(
                text = state.yearMonth.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)),
                color = Black50,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
            )
            ArrowBox(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, onClick = viewModel::nextMonth)
        }

        // 요일 헤더
        WeekDayHeader()

        // 캘린더 그리드
        MonthGrid(
            yearMonth = state.yearMonth.toString(),
            yearMonthObj = state.yearMonth,
            totalsByDay = state.dayTotalsMs,
            today = today,
            selected = state.selectedDate,
            onSelect = viewModel::selectDate,
        )

        Spacer(Modifier.height(16.dp))

        // 선택된 날짜의 일정/세션 카드
        if (state.selectedDayTodos.isNotEmpty() || state.selectedDaySessions.isNotEmpty()) {
            SelectedDayCard(
                date = state.selectedDate,
                todos = state.selectedDayTodos,
                sessions = state.selectedDaySessions,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ArrowBox(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF222B45))
    }
}

@Composable
private fun WeekDayHeader() {
    val days = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        days.forEach { d ->
            Text(
                text = d,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Black50,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun MonthGrid(
    yearMonth: String,
    yearMonthObj: java.time.YearMonth,
    totalsByDay: Map<Long, Long>,
    today: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val firstDay = yearMonthObj.atDay(1)
    // 일요일을 한 주 시작으로
    val startOffset = (firstDay.dayOfWeek.value % 7) // SUN=0, MON=1, ..., SAT=6
    val daysInMonth = yearMonthObj.lengthOfMonth()
    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        var cellIndex = 0
        while (cellIndex < totalCells) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) {
                    val dayNum = cellIndex - startOffset + 1
                    if (dayNum in 1..daysInMonth) {
                        val date = yearMonthObj.atDay(dayNum)
                        val totalMs = totalsByDay[date.toEpochDay()] ?: 0L
                        DayCell(
                            day = dayNum,
                            studyMs = totalMs,
                            isToday = date == today,
                            isSelected = date == selected,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(date) },
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).height(64.dp))
                    }
                    cellIndex++
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    studyMs: Long,
    isToday: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(64.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) BgGreenLight else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = day.toString(),
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = if (isToday) GreenPrimary else Black50,
            )
            if (isToday) {
                Spacer(Modifier.size(2.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary),
                )
            }
        }
        if (studyMs > 0) {
            Spacer(Modifier.size(2.dp))
            Text(
                text = formatStudy(studyMs),
                fontSize = 8.sp,
                color = Black50,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SelectedDayCard(
    date: LocalDate,
    todos: List<TodoEntity>,
    sessions: List<StudySessionEntity>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)),
            fontWeight = FontWeight.Bold,
            color = Black50,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(8.dp))

        if (sessions.isNotEmpty()) {
            Text("공부 세션", fontSize = 12.sp, color = Black50, fontWeight = FontWeight.Medium)
            sessions.forEach { s ->
                Text(
                    text = "${formatTime(s.startTime)} - ${formatTime(s.endTime)} (${formatStudy(s.durationMs)})",
                    fontSize = 13.sp,
                    color = Color(0xFF1B1A1C),
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        if (todos.isNotEmpty()) {
            Text("일정", fontSize = 12.sp, color = Black50, fontWeight = FontWeight.Medium)
            todos.forEach { t ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(t.title, fontSize = 14.sp, color = Color(0xFF1B1A1C), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private fun formatStudy(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec / 60) % 60
    val s = totalSec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun formatTime(epochMs: Long): String {
    val time = java.time.Instant.ofEpochMilli(epochMs)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

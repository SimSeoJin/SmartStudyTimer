package com.sm.myapplication.ui.screens.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sm.myapplication.data.dao.DailyTotal
import com.sm.myapplication.data.entity.StudySessionEntity
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.data.repository.AppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val dayTotalsMs: Map<Long, Long> = emptyMap(),
    val selectedDayTodos: List<TodoEntity> = emptyList(),
    val selectedDaySessions: List<StudySessionEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository.get(app)

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    private val _selected = MutableStateFlow(LocalDate.now())

    private val monthlyFlow = _yearMonth.flatMapLatest { ym ->
        val first = ym.atDay(1).toEpochDay()
        val last = ym.atEndOfMonth().toEpochDay()
        repo.observeMonthlyTotals(first, last).map { list ->
            list.associate { it.day to it.total }
        }
    }

    private val selectedTodosFlow = _selected.flatMapLatest { repo.observeTodosByDate(it.toEpochDay()) }
    private val selectedSessionsFlow = _selected.flatMapLatest { repo.observeSessionsByDate(it.toEpochDay()) }

    val state = combine(
        _yearMonth, _selected, monthlyFlow, selectedTodosFlow, selectedSessionsFlow
    ) { ym, sel, totals, todos, sessions ->
        CalendarUiState(
            yearMonth = ym,
            selectedDate = sel,
            dayTotalsMs = totals,
            selectedDayTodos = todos,
            selectedDaySessions = sessions,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun prevMonth() { _yearMonth.value = _yearMonth.value.minusMonths(1) }
    fun nextMonth() { _yearMonth.value = _yearMonth.value.plusMonths(1) }
    fun selectDate(d: LocalDate) { _selected.value = d }
}

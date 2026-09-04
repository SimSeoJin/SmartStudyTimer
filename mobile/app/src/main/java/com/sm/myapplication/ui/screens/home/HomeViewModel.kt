package com.sm.myapplication.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val todayTodos: List<TodoEntity> = emptyList(),
    val ddayEpochDay: Long? = null,
    val ddayLabel: String = "기말 고사",
    val todayStudyMs: Long = 0L,
    val tierName: String = "다이아",
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository.get(app)

    init {
        viewModelScope.launch { repo.cleanupAbnormalSessions() }
    }

    private val today: Long
        get() = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    val state = combine(
        repo.observeTodosByDate(today),
        repo.ddayEpochDay,
        repo.ddayLabel,
        repo.observeTodaySessionMs(today),
    ) { todos, dday, label, ms ->
        HomeUiState(
            todayTodos = todos.take(4),
            ddayEpochDay = dday,
            ddayLabel = label,
            todayStudyMs = ms,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun toggleTodo(todo: TodoEntity) {
        viewModelScope.launch { repo.toggleTodo(todo.id, !todo.isDone) }
    }

    fun daysUntilDDay(): Int? {
        val dday = state.value.ddayEpochDay ?: return null
        return (dday - today).toInt()
    }
}

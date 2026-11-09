package com.sm.myapplication.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.data.repository.AppRepository
import com.sm.myapplication.network.dto.tierDisplayName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val todayTodos: List<TodoEntity> = emptyList(),
    val todoTotal: Int = 0,
    val todoDone: Int = 0,
    val ddayEpochDay: Long? = null,
    val ddayLabel: String = "기말 고사",
    val todayStudyMs: Long = 0L,
    val tierName: String = "-",
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository.get(app)

    // 서버 랭킹 기준 내 티어(표시용 한글). 미로그인·네트워크 실패면 "-".
    private val tierName = MutableStateFlow("-")

    init {
        viewModelScope.launch { repo.cleanupAbnormalSessions() }
    }

    fun refreshTier() {
        viewModelScope.launch {
            tierName.value = when (val tier = repo.fetchMyTier()) {
                null -> "-"
                AppRepository.NO_RANK -> "기록 없음"
                else -> tierDisplayName(tier)
            }
        }
    }

    private val today: Long
        get() = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    val state = combine(
        repo.observeTodosByDate(today),
        repo.ddayEpochDay,
        repo.ddayLabel,
        repo.observeTodaySessionMs(today),
        tierName,
    ) { todos, dday, label, ms, tier ->
        HomeUiState(
            todayTodos = todos.take(4),
            todoTotal = todos.size,
            todoDone = todos.count { it.isDone },
            ddayEpochDay = dday,
            ddayLabel = label,
            todayStudyMs = ms,
            tierName = tier,
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

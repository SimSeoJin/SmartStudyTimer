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
    /** 배지 이미지용 서버 티어명(Diamond 등). 티어가 없으면 null */
    val tierServerName: String? = null,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository.get(app)

    // 서버 랭킹 기준 내 티어(서버 이름). 최근 7일 기록이 없으면 NO_RANK, 미로그인·네트워크 실패면 null.
    private val tier = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { repo.cleanupAbnormalSessions() }
    }

    fun refreshTier() {
        viewModelScope.launch { tier.value = repo.fetchMyTier() }
    }

    private val today: Long
        get() = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    val state = combine(
        repo.observeTodosByDate(today),
        repo.ddayEpochDay,
        repo.ddayLabel,
        repo.observeTodaySessionMs(today),
        tier,
    ) { todos, dday, label, ms, tier ->
        HomeUiState(
            todayTodos = todos.take(4),
            todoTotal = todos.size,
            todoDone = todos.count { it.isDone },
            ddayEpochDay = dday,
            ddayLabel = label,
            todayStudyMs = ms,
            tierName = when (tier) {
                null -> "-"
                AppRepository.NO_RANK -> "기록 없음"
                else -> tierDisplayName(tier)
            },
            tierServerName = tier?.takeIf { it != AppRepository.NO_RANK },
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

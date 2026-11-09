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
import kotlinx.coroutines.flow.map
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

data class WeeklyDay(val epochDay: Long, val totalMs: Long)

data class WeeklyUiState(
    /** 6일 전 → 오늘 순서 */
    val days: List<WeeklyDay> = emptyList(),
    /** 오늘(또는 어제)까지 연속으로 공부한 날 수 */
    val streakDays: Int = 0,
)

/** 연속 공부일을 셀 때 거슬러 올라가는 최대 일수 */
private const val STREAK_LOOKBACK_DAYS = 60L

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

    /** 홈 '최근 7일' 카드: 기기에 저장된 모든 모드의 기록(Room) 기준 */
    val weekly = repo.observeMonthlyTotals(today - (STREAK_LOOKBACK_DAYS - 1), today)
        .map { totals ->
            val byDay = totals.associate { it.day to it.total }
            val days = (6 downTo 0).map { offset ->
                val day = today - offset
                WeeklyDay(day, byDay[day] ?: 0L)
            }
            // 오늘 아직 공부 전이면 어제부터 센다(오늘 하루가 끝나기 전엔 연속 기록이 끊긴 게 아니므로)
            var cursor = if ((byDay[today] ?: 0L) > 0L) today else today - 1
            var streak = 0
            while ((byDay[cursor] ?: 0L) > 0L) {
                streak++
                cursor--
            }
            WeeklyUiState(days = days, streakDays = streak)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyUiState())

    fun toggleTodo(todo: TodoEntity) {
        viewModelScope.launch { repo.toggleTodo(todo.id, !todo.isDone) }
    }

    fun daysUntilDDay(): Int? {
        val dday = state.value.ddayEpochDay ?: return null
        return (dday - today).toInt()
    }
}

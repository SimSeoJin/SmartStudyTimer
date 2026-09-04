package com.sm.myapplication.ui.screens.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sm.myapplication.data.entity.TodoCategory
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodoListUiState(
    val activeTodos: List<TodoEntity> = emptyList(),
    val completedTodos: List<TodoEntity> = emptyList(),
    val today: LocalDate = LocalDate.now(),
)

class TodoListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository.get(app)

    val state = repo.observeTodosByDate(LocalDate.now().toEpochDay())
        .map { todos ->
            TodoListUiState(
                activeTodos = todos.filter { !it.isDone },
                completedTodos = todos.filter { it.isDone },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodoListUiState())

    fun toggle(todo: TodoEntity) {
        viewModelScope.launch { repo.toggleTodo(todo.id, !todo.isDone) }
    }

    fun delete(todo: TodoEntity) {
        viewModelScope.launch { repo.deleteTodo(todo) }
    }
}

class TodoAddViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository.get(app)

    fun save(
        title: String,
        category: TodoCategory,
        dateEpochDay: Long,
        timeMinutes: Int?,
        memo: String,
        onDone: () -> Unit,
    ) {
        if (title.isBlank()) { onDone(); return }
        viewModelScope.launch {
            repo.addTodo(
                TodoEntity(
                    title = title.trim(),
                    category = category,
                    dateEpochDay = dateEpochDay,
                    timeMinutes = timeMinutes,
                    memo = memo,
                )
            )
            onDone()
        }
    }
}

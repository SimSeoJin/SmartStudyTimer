package com.sm.myapplication.ui.screens.login

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sm.myapplication.data.repository.AppRepository
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository.get(app)

    var state = mutableStateOf(LoginUiState())
        private set

    fun login(id: String, password: String, onSuccess: () -> Unit) {
        if (state.value.isLoading) return
        state.value = LoginUiState(isLoading = true)
        viewModelScope.launch {
            repo.login(id, password)
                .onSuccess {
                    state.value = LoginUiState(isLoading = false)
                    onSuccess()
                }
                .onFailure { e ->
                    state.value = LoginUiState(isLoading = false, errorMessage = e.message)
                }
        }
    }
}

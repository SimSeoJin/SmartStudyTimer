package com.sm.myapplication.ui.screens.login

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sm.myapplication.data.repository.ApiException
import com.sm.myapplication.data.repository.AppRepository
import kotlinx.coroutines.launch
import java.io.IOException

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository.get(app)

    var state = mutableStateOf(LoginUiState())
        private set

    fun onKakaoTokenReceived(accessToken: String, onSuccess: () -> Unit) =
        login(onSuccess) { repo.loginWithKakao(accessToken) }

    fun onKakaoLoginFailed(message: String?) = showSdkError("카카오", message)

    fun onNaverTokenReceived(accessToken: String, onSuccess: () -> Unit) =
        login(onSuccess) { repo.loginWithNaver(accessToken) }

    fun onNaverLoginFailed(message: String?) = showSdkError("네이버", message)

    fun onGoogleTokenReceived(accessToken: String, onSuccess: () -> Unit) =
        login(onSuccess) { repo.loginWithGoogle(accessToken) }

    fun onGoogleLoginFailed(message: String?) = showSdkError("구글", message)

    fun onDevLogin(testUserName: String, onSuccess: () -> Unit) =
        login(onSuccess) { repo.devLogin(testUserName) }

    /** 오류 문구는 잠깐 보여 주고 지운다(화면이 계속 빨간 글씨로 남지 않게). */
    fun clearError() {
        if (state.value.errorMessage != null) state.value = state.value.copy(errorMessage = null)
    }

    private fun login(onSuccess: () -> Unit, call: suspend () -> Result<Unit>) {
        if (state.value.isLoading) return
        state.value = LoginUiState(isLoading = true)
        viewModelScope.launch {
            call()
                .onSuccess {
                    state.value = LoginUiState(isLoading = false)
                    onSuccess()
                }
                .onFailure { e ->
                    Log.w(TAG, "로그인 실패", e)
                    state.value = LoginUiState(isLoading = false, errorMessage = userMessage(e))
                }
        }
    }

    // SDK가 주는 메시지는 영어 원문이라 화면에는 보여 주지 않고 로그로만 남긴다.
    private fun showSdkError(provider: String, rawMessage: String?) {
        Log.w(TAG, "$provider 로그인 실패: $rawMessage")
        state.value = LoginUiState(isLoading = false, errorMessage = "$provider 로그인에 실패했어요. 잠시 후 다시 시도해 주세요.")
    }

    private fun userMessage(e: Throwable): String = when (e) {
        // 서버가 돌려준 안내 문구는 그대로 보여 준다.
        is ApiException -> e.message ?: GENERIC_ERROR
        is IOException -> "서버에 연결할 수 없어요. 인터넷 연결을 확인해 주세요."
        else -> GENERIC_ERROR
    }

    companion object {
        private const val TAG = "LoginViewModel"
        private const val GENERIC_ERROR = "로그인 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요."
    }
}

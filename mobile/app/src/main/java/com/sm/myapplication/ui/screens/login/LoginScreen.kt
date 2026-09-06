package com.sm.myapplication.ui.screens.login

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.Black50
import com.sm.myapplication.ui.theme.KakaoYellow

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.state

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 헤더
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(BgGreenLight),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Black50)
                }
                Spacer(Modifier.weight(1f))
                Text("로그인", color = Black50, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.weight(1f))

        val errorMessage = uiState.errorMessage
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFD32F2F),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }

        // 카카오 로그인
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KakaoYellow)
                .clickable {
                    if (uiState.isLoading) return@clickable

                    val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                        if (token != null) {
                            viewModel.onKakaoTokenReceived(token.accessToken, onSuccess = onLoginSuccess)
                        } else if (error != null) {
                            // 사용자가 취소한 경우는 에러로 표시하지 않음
                            val cancelled = error is ClientError && error.reason == ClientErrorCause.Cancelled
                            if (!cancelled) viewModel.onKakaoLoginFailed(error.message)
                        }
                    }

                    if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                        UserApiClient.instance.loginWithKakaoTalk(context, callback = callback)
                    } else {
                        UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Chat, contentDescription = null, tint = Color(0xFF3C1E1E), modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                if (uiState.isLoading) "로그인 중..." else "카카오계정으로 로그인",
                color = Color(0xFF3C1E1E),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}

package com.sm.myapplication.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.theme.AccentGreen
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.Destructive
import com.sm.myapplication.ui.theme.KakaoLabel
import com.sm.myapplication.ui.theme.KakaoYellow
import com.sm.myapplication.ui.theme.LabelPrimary
import com.sm.myapplication.ui.theme.LabelSecondary
import com.sm.myapplication.ui.theme.LabelTertiary

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CardWhite)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Icon(
            imageVector = Icons.Rounded.Timer,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(84.dp),
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = "SmartStudyTimer",
            style = MaterialTheme.typography.headlineMedium,
            color = LabelPrimary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "공부 시간을 기록하고\n나만의 페이스를 만들어 보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = LabelSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1.4f))

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Destructive,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            )
        }

        KakaoButton(
            loading = uiState.isLoading,
            onClick = {
                if (uiState.isLoading) return@KakaoButton

                val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                    if (token != null) {
                        viewModel.onKakaoTokenReceived(token.accessToken, onSuccess = onLoginSuccess)
                    } else if (error != null) {
                        // 사용자가 직접 취소한 경우는 에러로 표시하지 않음
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
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "계속하면 이용약관과 개인정보 처리방침에\n동의하는 것으로 간주됩니다",
            style = MaterialTheme.typography.labelSmall,
            color = LabelTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun KakaoButton(loading: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(KakaoYellow)
            .pressable(enabled = !loading, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KakaoSymbol()
        Spacer(Modifier.size(8.dp))
        Text(
            text = if (loading) "로그인 중…" else "카카오로 계속하기",
            style = MaterialTheme.typography.labelLarge,
            color = KakaoLabel,
        )
    }
}

/** 카카오 말풍선 심볼 (브랜드 가이드의 단색 형태). */
@Composable
private fun KakaoSymbol() {
    Box(
        modifier = Modifier.size(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = androidx.compose.ui.graphics.Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(0f, h * 0.06f, w, h * 0.78f))
                moveTo(w * 0.28f, h * 0.64f)
                lineTo(w * 0.20f, h * 0.99f)
                lineTo(w * 0.52f, h * 0.72f)
                close()
            }
            drawPath(path, color = KakaoLabel)
        }
    }
}

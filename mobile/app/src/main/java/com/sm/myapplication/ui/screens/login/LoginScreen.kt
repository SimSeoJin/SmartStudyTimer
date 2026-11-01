package com.sm.myapplication.ui.screens.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.sm.myapplication.BuildConfig
import com.sm.myapplication.R
import com.sm.myapplication.ui.theme.Destructive
import com.sm.myapplication.ui.theme.Ink
import com.sm.myapplication.ui.theme.LabelSecondary
import com.sm.myapplication.ui.theme.Paper
import com.sm.myapplication.ui.theme.PaperShade
import com.sm.myapplication.ui.theme.PartyRed
import com.sm.myapplication.ui.theme.PosterShape
import com.sm.myapplication.ui.theme.Separator

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.state

    // 구글은 최초 1회 동의 화면이 필요해서, 동의 결과를 받을 런처를 미리 등록해 둔다.
    val googleConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        // 사용자가 동의 화면을 닫은 경우는 에러로 표시하지 않음
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        try {
            val token = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(result.data).accessToken
            if (token != null) {
                viewModel.onGoogleTokenReceived(token, onSuccess = onLoginSuccess)
            } else {
                viewModel.onGoogleLoginFailed(null)
            }
        } catch (e: Exception) {
            viewModel.onGoogleLoginFailed(e.message)
        }
    }
    var devTestUserName by remember { mutableStateOf("testuser") }

    // 배지 주변으로 2초마다 한 번씩 퍼졌다 사라지는 핑 파동
    val pulseTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperShade),
    ) {
        // 포스터 위아래를 두르는 굵은 먹색·빨간 띠
        Column(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            Box(Modifier.fillMaxWidth().height(10.dp).background(PartyRed))
            Box(Modifier.fillMaxWidth().height(3.dp).background(Ink))
        }
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Ink))
            Box(Modifier.fillMaxWidth().height(10.dp).background(PartyRed))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(PosterShape.Small).background(Paper),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Ink)
                }
                Spacer(Modifier.weight(1f))
                Text("출입 등록", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(40.dp))
            }

            Spacer(Modifier.weight(1f))

            // 브랜드 + 로그인, 화면 중앙에 한 덩어리로 배치
            // 앱 아이콘과 같은 순공부 포스터 배지. 뒤로 2초마다 빨간 경보 파동이 퍼진다.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .scale(pulseScale)
                        .clip(PosterShape.Large)
                        .background(PartyRed.copy(alpha = pulseAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(PosterShape.Large)
                        .background(PartyRed),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(144.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "순공부",
                color = Ink,
                fontWeight = FontWeight.Black,
                fontSize = 34.sp,
                letterSpacing = 6.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "딴짓은 패배다 · 순공은 자유다 · 기록은 영원하다",
                color = PartyRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "순공부는 동지의\n순수한 공부 시간만을 인정합니다",
                color = LabelSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 32.dp),
            )

            Spacer(Modifier.height(40.dp))

            val errorMessage = uiState.errorMessage
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Destructive,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
                )
            }

            if (BuildConfig.DEBUG) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(54.dp),
                    shape = PosterShape.Medium,
                    color = Ink,
                    shadowElevation = 6.dp,
                    onClick = {
                        if (uiState.isLoading) return@Surface
                        viewModel.onDevLogin("admin", onSuccess = onLoginSuccess)
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            if (uiState.isLoading) "출입 확인 중..." else "관리자 로그인",
                            color = Paper,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (BuildConfig.DEV_MODE) {
                Text(
                    text = "[개발 모드] 테스트 계정으로 로그인",
                    color = LabelSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp),
                )

                TextField(
                    value = devTestUserName,
                    onValueChange = { devTestUserName = it },
                    placeholder = { Text("사용자명 (예: testuser)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(48.dp),
                    shape = PosterShape.Small,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Paper,
                        unfocusedContainerColor = Paper,
                        focusedIndicatorColor = PartyRed,
                        unfocusedIndicatorColor = Separator,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(54.dp),
                    shape = PosterShape.Medium,
                    color = PartyRed,
                    shadowElevation = 6.dp,
                    onClick = {
                        if (uiState.isLoading || devTestUserName.isBlank()) return@Surface
                        viewModel.onDevLogin(devTestUserName, onSuccess = onLoginSuccess)
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            if (uiState.isLoading) "출입 확인 중..." else "테스트 로그인",
                            color = Paper,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // 카카오 로그인
                    SocialCircleButton(
                        enabled = !uiState.isLoading,
                        onClick = {
                            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                                if (token != null) {
                                    viewModel.onKakaoTokenReceived(token.accessToken, onSuccess = onLoginSuccess)
                                } else if (error != null) {
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
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_login_kakao),
                            contentDescription = "카카오 로그인",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // 네이버 로그인
                    SocialCircleButton(
                        enabled = !uiState.isLoading,
                        onClick = {
                            val callback = object : OAuthLoginCallback {
                                override fun onSuccess() {
                                    viewModel.onNaverTokenReceived(NaverIdLoginSDK.getAccessToken().orEmpty(), onSuccess = onLoginSuccess)
                                }

                                override fun onFailure(httpStatus: Int, message: String) {
                                    viewModel.onNaverLoginFailed(NaverIdLoginSDK.getLastErrorDescription() ?: message)
                                }

                                override fun onError(errorCode: Int, message: String) {
                                    onFailure(errorCode, message)
                                }
                            }

                            NaverIdLoginSDK.authenticate(context, callback)
                        },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_login_naver),
                            contentDescription = "네이버 로그인",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // 구글 로그인
                    SocialCircleButton(
                        enabled = !uiState.isLoading,
                        borderColor = Color(0xFFDADCE0),
                        onClick = {
                            val request = AuthorizationRequest.builder()
                                .setRequestedScopes(listOf(Scope("profile"), Scope("email")))
                                .build()

                            Identity.getAuthorizationClient(context).authorize(request)
                                .addOnSuccessListener { result ->
                                    val pendingIntent = result.pendingIntent
                                    if (result.hasResolution() && pendingIntent != null) {
                                        googleConsentLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                                    } else {
                                        val token = result.accessToken
                                        if (token != null) {
                                            viewModel.onGoogleTokenReceived(token, onSuccess = onLoginSuccess)
                                        } else {
                                            viewModel.onGoogleLoginFailed(null)
                                        }
                                    }
                                }
                                .addOnFailureListener { e -> viewModel.onGoogleLoginFailed(e.message) }
                        },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_login_google),
                            contentDescription = "구글 로그인",
                            modifier = Modifier.fillMaxSize().padding(15.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

// 동그란 소셜 로그인 버튼. 로딩 중에는 흐리게 표시하고 클릭을 막는다.
@Composable
private fun SocialCircleButton(
    enabled: Boolean,
    onClick: () -> Unit,
    borderColor: Color? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(58.dp)
            .alpha(if (enabled) 1f else 0.5f),
        shape = CircleShape,
        color = Color.White,
        border = borderColor?.let { BorderStroke(1.dp, it) },
        shadowElevation = 4.dp,
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

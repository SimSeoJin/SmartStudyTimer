package com.sm.myapplication.ui.screens.puremode

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.data.repository.AppRepository
import com.sm.myapplication.ui.components.NavBar
import com.sm.myapplication.ui.components.ResponsiveTimerText
import com.sm.myapplication.ui.components.StatusPill
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.screens.timer.TimerController
import com.sm.myapplication.ui.screens.timer.TimerPauseCause
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.DarkStatusBarEffect
import com.sm.myapplication.ui.theme.FocusBg
import com.sm.myapplication.ui.theme.FocusCameraBottom
import com.sm.myapplication.ui.theme.FocusCameraTop
import com.sm.myapplication.ui.theme.FocusGreen
import com.sm.myapplication.ui.theme.FocusRed
import com.sm.myapplication.ui.theme.FocusSurface
import com.sm.myapplication.ui.theme.LabelSecondary

@Composable
fun PureModeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by TimerController.state.collectAsStateWithLifecycle()
    var cameraOn by remember { mutableStateOf(true) }
    var screenCovered by remember { mutableStateOf(false) }

    DarkStatusBarEffect()

    LaunchedEffect(Unit) {
        if (!state.isRunning && state.elapsedMs == 0L) {
            TimerController.start(StudyMode.PURE, autoRun = false)
        }
    }

    LaunchedEffect(cameraOn) {
        if (cameraOn) TimerController.onCameraOn() else TimerController.onCameraOff()
    }

    val finish: () -> Unit = {
        TimerController.stop(AppRepository.get(context.applicationContext as Application))
        onBack()
    }
    BackHandler(onBack = finish)

    if (screenCovered) {
        ScreenCoveredOverlay(onTap = { screenCovered = false })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        NavBar(
            title = "순공모드",
            titleColor = CardWhite,
            // TODO(서버 연동): 순위는 랭킹 API 연결 전까지 표시용 값
            actionLabel = "55위",
            actionColor = FocusGreen,
            onAction = {},
        )

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            StatusPill(
                text = pureModeStatusLabel(state.isRunning, state.pauseCause),
                dark = true,
                active = state.isRunning,
            )
        }

        Spacer(Modifier.height(12.dp))

        if (cameraOn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(FocusCameraTop, FocusCameraBottom)))
                    .border(0.5.dp, CardWhite.copy(alpha = 0.14f), RoundedCornerShape(22.dp)),
            ) {
                FrontCameraPreview(modifier = Modifier.fillMaxSize())

                // 얼굴을 맞출 위치를 알려주는 가이드 링
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(width = 150.dp, height = 195.dp)
                        .border(
                            width = 1.5.dp,
                            color = if (state.isRunning) FocusGreen.copy(alpha = 0.75f) else CardWhite.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(percent = 48),
                        )
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))

        ResponsiveTimerText(
            text = formatElapsedDigits(state.elapsedMs),
            maxFontSize = if (cameraOn) 48f else 56f,
            minFontSize = 28f,
            color = CardWhite,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "✋ 손바닥 = 시작 · ✊ 주먹 = 일시정지",
            style = MaterialTheme.typography.bodySmall,
            color = LabelSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!cameraOn) Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkPillButton(
                    label = if (cameraOn) "카메라 끄기" else "카메라 켜기",
                    modifier = Modifier.weight(1f),
                    onClick = { cameraOn = !cameraOn },
                )
                DarkPillButton(
                    label = "화면 가리기",
                    modifier = Modifier.weight(1f),
                    onClick = { screenCovered = true },
                )
            }
            DarkPillButton(
                label = "순공모드 종료",
                destructive = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = finish,
            )
        }
    }
}

@Composable
private fun DarkPillButton(
    label: String,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(if (destructive) FocusRed.copy(alpha = 0.16f) else FocusSurface)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (destructive) FocusRed else CardWhite,
        )
    }
}

@Composable
private fun ScreenCoveredOverlay(onTap: () -> Unit) {
    DarkStatusBarEffect()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusBg)
            .pressable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "화면을 터치하면 돌아갑니다",
            style = MaterialTheme.typography.bodyLarge,
            color = CardWhite.copy(alpha = 0.22f),
        )
    }
}

/** TimerController의 내부 상태를 화면에 보여줄 짧은 문구로 옮긴다. */
private fun pureModeStatusLabel(running: Boolean, cause: TimerPauseCause): String = when {
    running -> "공부 중 · 자동 기록"
    cause == TimerPauseCause.WAITING_FACE -> "얼굴 감지 대기 중"
    cause == TimerPauseCause.AUTO_FACE_LOST -> "자리 비움 · 일시정지"
    cause == TimerPauseCause.MANUAL_GESTURE -> "일시정지 · 손바닥을 펴면 재개"
    cause == TimerPauseCause.CAMERA_OFF -> "카메라 꺼짐 · 일시정지"
    cause == TimerPauseCause.USER -> "일시정지"
    else -> "준비 중"
}

private fun formatElapsedDigits(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(totalSec / 3600, (totalSec / 60) % 60, totalSec % 60)
}

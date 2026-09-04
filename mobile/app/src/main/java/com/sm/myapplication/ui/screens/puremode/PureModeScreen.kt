package com.sm.myapplication.ui.screens.puremode

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.data.repository.AppRepository
import com.sm.myapplication.ui.screens.timer.TimerController
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.BgGreenTint
import com.sm.myapplication.ui.theme.Black50
import com.sm.myapplication.ui.theme.Gray
import com.sm.myapplication.ui.theme.PureModeBg

@Composable
fun PureModeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by TimerController.state.collectAsStateWithLifecycle()
    var cameraOn by remember { mutableStateOf(true) }
    var screenCovered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!state.isRunning && state.elapsedMs == 0L) {
            TimerController.start(StudyMode.PURE, autoRun = false)
        }
    }

    LaunchedEffect(cameraOn) {
        if (cameraOn) {
            TimerController.onCameraOn()
        } else {
            TimerController.onCameraOff()
        }
    }

    val finish: () -> Unit = {
        TimerController.stop(AppRepository.get(context.applicationContext as Application))
        onBack()
    }
    BackHandler(onBack = finish)

    if (screenCovered) {
        // pure_mode_screenoff
        ScreenCoveredOverlay(onTap = { screenCovered = false })
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp)
        ) {
            // 헤더 + "순공모드" 타이틀
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .background(BgGreenLight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "순공모드",
                    color = Black50,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (cameraOn) {
                Spacer(Modifier.height(14.dp))

                // 카메라 박스: 고정 520dp 대신 남은 공간에 맞게 자동 조절
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Gray)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FrontCameraPreview(modifier = Modifier.fillMaxSize())
                }

                Spacer(Modifier.height(12.dp))

                // 카메라 아래에 타이머를 항상 표시
                TimerLine(elapsedMs = state.elapsedMs)

                Spacer(Modifier.height(12.dp))
            } else {
                // camoff
                Spacer(Modifier.height(28.dp))
                TimerLine(elapsedMs = state.elapsedMs, big = true)
                Spacer(Modifier.height(14.dp))
                Text(
                    "나의 순위는 현재 55위 입니다.",
                    color = Black50,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }

        // 하단 종료 + 보조 버튼들
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 순공모드 종료 버튼 (가운데에 일시정지 ‖)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(BgGreenTint)
                    .clickable(onClick = finish)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Black50,
                )
                Spacer(Modifier.width(8.dp))
                Text("순공모드 종료", color = Black50, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    label = if (cameraOn) "카메라 끄기" else "카메라 켜기",
                    modifier = Modifier.weight(1f),
                    onClick = { cameraOn = !cameraOn },
                )
                PillButton(
                    label = "화면 가리기",
                    modifier = Modifier.weight(1f),
                    onClick = { screenCovered = true },
                )
            }
        }
    }
}

@Composable
private fun TimerLine(elapsedMs: Long, big: Boolean = false) {
    Text(
        text = formatElapsedDigits(elapsedMs),
        fontFamily = FontFamily.Monospace,
        fontSize = if (big) 56.sp else 40.sp,
        color = Color.Black,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun PillButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(BgGreenTint)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Black50, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
private fun ScreenCoveredOverlay(onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureModeBg)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "화면을 터치하면 이전화면으로 돌아갑니다",
            color = Color.White.copy(alpha = 0.21f),
            fontSize = 16.sp,
        )
    }
}

private fun formatElapsedDigits(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec / 60) % 60
    val s = totalSec % 60
    return "%02d : %02d : %02d".format(h, m, s)
}

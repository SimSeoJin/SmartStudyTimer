package com.sm.myapplication.ui.screens.timer

import android.app.Application
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.data.repository.AppRepository
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.Black50
import com.sm.myapplication.ui.theme.GreenLight
import com.sm.myapplication.ui.theme.GreenPrimary

@Composable
fun BasicModeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by TimerController.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (!state.isRunning && state.elapsedMs == 0L) {
            TimerController.start(StudyMode.BASIC)
        }
    }

    val finish: () -> Unit = {
        TimerController.stop(AppRepository.get(context.applicationContext as Application))
        onBack()
    }
    BackHandler(onBack = finish)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 연두 헤더
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .background(BgGreenLight),
            )

            Spacer(Modifier.height(20.dp))

            // 큰 타이머
            Text(
                text = formatElapsedDigits(state.elapsedMs),
                fontFamily = FontFamily.Monospace,
                fontSize = 56.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "나의 순위는 현재 55위 입니다.",
                fontSize = 16.sp,
                color = Black50,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(40.dp))

            // 가운데 일시정지 / 재생 큰 버튼 (glow + ‖ 또는 ▶)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 외부 glow
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(GreenLight.copy(alpha = 0.45f), Color.Transparent),
                            )
                        )
                )
                // 내부 glow
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(GreenPrimary.copy(alpha = 0.25f), Color.Transparent),
                            )
                        )
                )

                // 일시정지/재생 표시
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clickable { TimerController.pauseResume() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isRunning) {
                        // 일시정지 (‖) - 두 개의 둥근 막대
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 32.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 32.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "재생",
                            tint = Color.White,
                            modifier = Modifier.size(100.dp),
                        )
                    }
                }
            }
        }

        // 우상단 종료 버튼
        Text(
            text = "종료",
            color = Black50,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .clickable(onClick = finish),
        )
    }
}

internal fun formatElapsedDigits(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec / 60) % 60
    val s = totalSec % 60
    return "%02d : %02d : %02d".format(h, m, s)
}

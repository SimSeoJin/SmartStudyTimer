package com.sm.myapplication.ui.screens.timer

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sm.myapplication.data.datastore.AppPreferences
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.data.repository.AppRepository
import com.sm.myapplication.ui.components.GhostButton
import com.sm.myapplication.ui.components.NavBar
import com.sm.myapplication.ui.components.StatusPill
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.screens.setting.NotificationHelper
import com.sm.myapplication.ui.theme.AccentGreen
import com.sm.myapplication.ui.theme.Destructive
import com.sm.myapplication.ui.theme.GroupedBg
import com.sm.myapplication.ui.theme.LabelSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun BasicModeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by TimerController.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!state.isRunning && state.elapsedMs == 0L) {
            TimerController.start(StudyMode.BASIC)
        }
    }

    val finish: () -> Unit = {
        val duration = formatElapsedDigits(state.elapsedMs)
        scope.launch {
            val prefs = AppPreferences(context.applicationContext)
            val enabled = prefs.notificationsEnabled.first()
            TimerController.stop(AppRepository.get(context.applicationContext as Application))
            if (enabled) NotificationHelper.notifyStudyFinished(context, duration)
            onBack()
        }
    }
    BackHandler(onBack = finish)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        NavBar(
            title = "기본 모드",
            actionLabel = "종료",
            actionColor = Destructive,
            onAction = finish,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))

            StatusPill(
                text = if (state.isRunning) "공부 중" else "일시정지됨",
                active = state.isRunning,
            )

            Spacer(Modifier.height(18.dp))

            ResponsiveTimerText(
                text = formatElapsedDigits(state.elapsedMs),
                maxFontSize = 56f,
                minFontSize = 28f,
            )

            Spacer(Modifier.height(10.dp))

            // TODO(서버 연동): 순위는 랭킹 API 연결 전까지 표시용 값
            Text(
                text = "현재 55위",
                style = MaterialTheme.typography.bodyMedium,
                color = LabelSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .pressable { TimerController.pauseResume() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isRunning) "일시정지" else "재개",
                    tint = AccentGreen,
                    modifier = Modifier.size(110.dp),
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (state.isRunning) "일시정지" else "이어서 하기",
                style = MaterialTheme.typography.bodyMedium,
                color = LabelSecondary,
            )
        }

        GhostButton(
            text = "세션 종료하고 저장",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            onClick = finish,
        )
    }
}

internal fun formatElapsedDigits(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(totalSec / 3600, (totalSec / 60) % 60, totalSec % 60)
}

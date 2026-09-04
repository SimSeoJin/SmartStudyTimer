package com.sm.myapplication.ui.screens.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.BgGreenTint
import com.sm.myapplication.ui.theme.Black50
import com.sm.myapplication.ui.theme.GreenLight
import com.sm.myapplication.ui.theme.GreenPrimary

@Composable
fun MainHomeScreen(
    onStartBasicMode: () -> Unit,
    onStartPureMode: () -> Unit,
    onOpenTodos: () -> Unit,
    onOpenDDay: () -> Unit,
    onOpenTier: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val daysLeft = viewModel.daysUntilDDay()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        // 상단 연두 헤더
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(BgGreenLight),
        )

        Spacer(Modifier.height(10.dp))

        // 메인 타이머 영역 (큰 ▶ + 정보 카드 겹침)
        TimerStack(
            elapsedMs = state.todayStudyMs,
            onStart = onStartBasicMode,
        )

        Spacer(Modifier.height(12.dp))

        // 순공모드로 시작하기 버튼
        PureModeStartButton(onClick = onStartPureMode)

        Spacer(Modifier.height(16.dp))

        // D-Day + 티어 좌우 카드
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DDayCard(
                label = state.ddayLabel,
                daysLeft = daysLeft,
                onEdit = onOpenDDay,
                modifier = Modifier.weight(1f),
            )
            TierCard(
                tierName = state.tierName,
                onHelp = onOpenTier,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(14.dp))

        // 오늘 할 일 카드
        TodoMiniCard(
            todos = state.todayTodos,
            onAdd = onOpenTodos,
            onTodoToggle = viewModel::toggleTodo,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TimerStack(elapsedMs: Long, onStart: () -> Unit) {
    val timeText = formatElapsed(elapsedMs)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 큰 ▶ 재생 버튼 (위쪽)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .size(220.dp)
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            // 외부 글로우
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GreenLight.copy(alpha = 0.55f), Color.Transparent),
                        )
                    )
            )
            // 내부 진한 글로우
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GreenPrimary.copy(alpha = 0.55f), Color.Transparent),
                        )
                    )
            )
            // 흰색 큰 ▶
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "시작",
                tint = Color.White,
                modifier = Modifier
                    .size(150.dp)
                    .padding(start = 14.dp),
            )
        }

        // 아래쪽 정보 카드 (큰 버튼과 겹침)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.72f)
                .clickable(onClick = onStart),
            color = Color.White,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = timeText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = Color(0xFF333333),
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "START",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun PureModeStartButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 60.dp)
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(50),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Black50,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "순공모드로 시작하기",
                color = Black50,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun DDayCard(
    label: String,
    daysLeft: Int?,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalBar,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "나의 디데이",
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BgGreenLight)
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("수정", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Black50)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (daysLeft != null) {
                Text(
                    "'$label'까지",
                    color = Black50,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${daysLeft}일",
                        color = GreenPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "남았습니다",
                        color = Black50,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            } else {
                Text("디데이 설정", color = Black50, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TierCard(
    tierName: String,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Spa,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "나의 티어",
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = "도움말",
                    tint = Black50,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onHelp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "현재 나의 티어는",
                color = Black50,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "'$tierName'",
                    color = GreenPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "입니다",
                    color = Black50,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun TodoMiniCard(
    todos: List<TodoEntity>,
    onAdd: () -> Unit,
    onTodoToggle: (TodoEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.WbSunny,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "오늘의 할 일",
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "추가",
                    tint = Black50,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onAdd),
                )
            }
            Spacer(Modifier.height(8.dp))

            if (todos.isEmpty()) {
                Text(
                    "오늘 할 일이 없어요. + 로 추가하세요.",
                    color = Black50,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            } else {
                todos.forEach { todo ->
                    TodoMiniRow(todo = todo, onToggle = { onTodoToggle(todo) })
                }
            }
        }
    }
}

@Composable
private fun TodoMiniRow(todo: TodoEntity, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (todo.isDone) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (todo.isDone) GreenPrimary else Color(0xFFBDBDBD),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = todo.title,
            color = Color.Black,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec / 60) % 60
    val s = totalSec % 60
    return "%02d : %02d : %02d".format(h, m, s)
}

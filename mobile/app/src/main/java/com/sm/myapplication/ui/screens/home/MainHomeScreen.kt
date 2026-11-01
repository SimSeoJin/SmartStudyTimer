package com.sm.myapplication.ui.screens.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.ui.components.InitialAvatar
import com.sm.myapplication.ui.components.InsetCard
import com.sm.myapplication.ui.components.RoundCheck
import com.sm.myapplication.ui.components.RowDivider
import com.sm.myapplication.ui.components.SummaryHeader
import com.sm.myapplication.ui.components.formatHms
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.theme.AccentInk
import com.sm.myapplication.ui.theme.PartyTint
import com.sm.myapplication.ui.theme.Paper
import com.sm.myapplication.ui.theme.Ink
import com.sm.myapplication.ui.theme.PartyRed
import com.sm.myapplication.ui.theme.PosterShape
import com.sm.myapplication.ui.theme.Separator
import com.sm.myapplication.ui.theme.PaperShade
import com.sm.myapplication.ui.theme.LabelPrimary
import com.sm.myapplication.ui.theme.LabelSecondary
import com.sm.myapplication.ui.theme.LabelTertiary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MainHomeScreen(
    userName: String,
    onStartBasicMode: () -> Unit,
    onStartPureMode: () -> Unit,
    onOpenTodos: () -> Unit,
    onOpenDDay: () -> Unit,
    onOpenTier: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val daysLeft = viewModel.daysUntilDDay()
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))
    val remaining = state.todoTotal - state.todoDone

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperShade)
            .verticalScroll(rememberScrollState()),
    ) {
        SummaryHeader(
            summary = dateText,
            sub = when {
                state.todoTotal == 0 -> "동지, 오늘의 과업을 등록하십시오"
                remaining == 0 -> "오늘 과업 완수. 당은 만족합니다"
                else -> "남은 과업 ${remaining}건"
            },
            trailing = {
                InitialAvatar(
                    name = userName,
                    size = 36.dp,
                    modifier = Modifier.pressable(onClick = onOpenProfile),
                )
            },
        )

        TimerHeroCard(
            elapsedMs = state.todayStudyMs,
            onStartBasic = onStartBasicMode,
            onStartPure = onStartPureMode,
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label = "D-DAY",
                value = daysLeft?.let { if (it >= 0) "D-$it" else "D+${-it}" } ?: "미설정",
                valueColor = if (daysLeft != null) AccentInk else LabelTertiary,
                caption = daysLeft?.let { state.ddayLabel },
                actionText = if (daysLeft != null) "수정" else "날짜 정하기",
                onAction = onOpenDDay,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "나의 계급",
                value = state.tierName,
                valueColor = LabelPrimary,
                caption = null,
                actionText = "계급표 보기 ›",
                onAction = onOpenTier,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(10.dp))

        TodoMiniCard(
            todos = state.todayTodos,
            done = state.todoDone,
            total = state.todoTotal,
            onAdd = onOpenTodos,
            onTodoToggle = viewModel::toggleTodo,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TimerHeroCard(
    elapsedMs: Long,
    onStartBasic: () -> Unit,
    onStartPure: () -> Unit,
) {
    // 먹색 선전 벽보. 왼쪽 위 빨간 딱지와 아래 표어로 포스터 느낌을 낸다.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(PosterShape.Large)
            .background(Ink)
            .padding(18.dp),
    ) {
        Text(
            "순공부 공보",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = Paper,
            modifier = Modifier
                .clip(PosterShape.Small)
                .background(PartyRed)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "오늘의 순공 실적",
            style = MaterialTheme.typography.bodySmall,
            color = Paper.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatHms(elapsedMs),
            style = MaterialTheme.typography.displaySmall,
            color = Paper,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "순공부가 동지를 지켜보고 있습니다",
            style = MaterialTheme.typography.labelMedium,
            color = PartyTint,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(PosterShape.Small)
                    .background(Paper)
                    .pressable(onClick = onStartBasic),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = AccentInk,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("기본 모드", color = AccentInk, style = MaterialTheme.typography.titleSmall)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(PosterShape.Small)
                    .background(PartyRed)
                    .pressable(onClick = onStartPure),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = Paper,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text("순공모드", color = Paper, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    caption: String?,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(PosterShape.Medium)
            .background(Paper)
            .border(1.dp, Separator, PosterShape.Medium)
            .pressable(onClick = onAction)
            .padding(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LabelSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = valueColor,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (caption != null) {
                Text(
                    "$caption · ",
                    style = MaterialTheme.typography.labelSmall,
                    color = LabelSecondary,
                )
            }
            Text(
                actionText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AccentInk,
            )
        }
    }
}

@Composable
private fun TodoMiniCard(
    todos: List<TodoEntity>,
    done: Int,
    total: Int,
    onAdd: () -> Unit,
    onTodoToggle: (TodoEntity) -> Unit,
) {
    InsetCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("오늘의 과업", style = MaterialTheme.typography.titleMedium, color = LabelPrimary)
            if (total > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "$done/$total",
                    style = MaterialTheme.typography.bodySmall,
                    color = LabelSecondary,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(PosterShape.Small)
                    .background(PartyTint)
                    .pressable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "과업 등록",
                    tint = AccentInk,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (todos.isEmpty()) {
            Text(
                "아직 하달된 과업이 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = LabelSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp),
            )
        } else {
            Spacer(Modifier.height(2.dp))
            todos.forEachIndexed { index, todo ->
                TodoMiniRow(todo = todo, onToggle = { onTodoToggle(todo) })
                if (index != todos.lastIndex) RowDivider(inset = 48.dp)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun TodoMiniRow(todo: TodoEntity, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundCheck(checked = todo.isDone)
        Spacer(Modifier.width(10.dp))
        Text(
            text = todo.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (todo.isDone) LabelSecondary else LabelPrimary,
            textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
        )
        todo.timeMinutes?.let { mins ->
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatClock(mins),
                style = MaterialTheme.typography.bodySmall,
                color = LabelTertiary,
            )
        }
    }
}

internal fun formatClock(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val suffix = if (h < 12) "am" else "pm"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d%s".format(h12, m, suffix)
}

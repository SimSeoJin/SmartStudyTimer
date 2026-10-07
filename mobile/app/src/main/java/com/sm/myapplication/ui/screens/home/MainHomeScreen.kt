package com.sm.myapplication.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
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
import com.sm.myapplication.ui.theme.AccentTint
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.Green500
import com.sm.myapplication.ui.theme.Green700
import com.sm.myapplication.ui.theme.Green900
import com.sm.myapplication.ui.theme.GroupedBg
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
            .background(GroupedBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SummaryHeader(
            summary = dateText,
            sub = when {
                state.todoTotal == 0 -> "오늘의 할 일을 추가해 보세요"
                remaining == 0 -> "오늘 할 일을 모두 끝냈어요 🎉"
                else -> "할 일 ${remaining}개가 남았어요"
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
                label = "나의 티어",
                value = state.tierName,
                valueColor = LabelPrimary,
                caption = null,
                actionText = "기준표 보기 ›",
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Green500, Green700, Green900),
                )
            )
            .padding(18.dp),
    ) {
        Text(
            "오늘 누적 공부 시간",
            style = MaterialTheme.typography.bodySmall,
            color = CardWhite.copy(alpha = 0.88f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatHms(elapsedMs),
            style = MaterialTheme.typography.displaySmall,
            color = CardWhite,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(CardWhite)
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
                Text("시작하기", color = AccentInk, style = MaterialTheme.typography.titleSmall)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(CardWhite.copy(alpha = 0.2f))
                    .pressable(onClick = onStartPure),
                contentAlignment = Alignment.Center,
            ) {
                Text("순공모드", color = CardWhite, style = MaterialTheme.typography.titleSmall)
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
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
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
            Text("오늘의 할 일", style = MaterialTheme.typography.titleMedium, color = LabelPrimary)
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
                    .clip(CircleShape)
                    .background(AccentTint)
                    .pressable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "할 일 추가",
                    tint = AccentInk,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (todos.isEmpty()) {
            Text(
                "아직 등록한 할 일이 없어요.",
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
